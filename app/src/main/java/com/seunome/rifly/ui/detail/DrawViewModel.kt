package com.seunome.rifly.ui.detail

import androidx.lifecycle.ViewModel
import com.seunome.rifly.data.model.Prize
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.model.Reservation
import com.seunome.rifly.data.repository.PrizeRepository
import com.seunome.rifly.data.repository.RaffleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.max

data class DrawState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val winnerNumbers: List<Int> = emptyList(),
    val winnerNames: List<String> = emptyList(),
    val winnerPhones: List<String> = emptyList()
) {
    val winnerNumber: Int? get() = winnerNumbers.firstOrNull()
    val winnerName: String? get() = winnerNames.firstOrNull()
    val winnerPhone: String? get() = winnerPhones.firstOrNull()
}

class DrawViewModel : ViewModel() {

    private val raffleRepository = RaffleRepository()
    private val prizeRepository = PrizeRepository()

    private val _state = MutableStateFlow(DrawState())
    val state: StateFlow<DrawState> = _state.asStateFlow()

    fun drawRandom(raffle: Raffle, confirmedReservations: List<Reservation>, prizes: List<Prize> = emptyList()): List<Int> {
        val confirmedNumbers = confirmedReservations.flatMap { it.numbers }
        if (confirmedNumbers.isEmpty()) {
            _state.value = DrawState(error = "Não há nenhuma reserva confirmada para realizar o sorteio.")
            return emptyList()
        }

        val countToPick = if (prizes.isNotEmpty()) prizes.size else 1
        val distinctNumbers = confirmedNumbers.distinct().shuffled()
        val winningNumbers = distinctNumbers.take(countToPick)

        buildDrawState(winningNumbers, confirmedReservations)
        return winningNumbers
    }

    fun drawByLottery(
        raffle: Raffle,
        confirmedReservations: List<Reservation>,
        lotteryResult: String,
        prizes: List<Prize> = emptyList()
    ): List<Int> {
        val confirmedNumbers = confirmedReservations.flatMap { it.numbers }
        if (confirmedNumbers.isEmpty()) {
            _state.value = DrawState(error = "Não há nenhuma reserva confirmada para realizar o sorteio.")
            return emptyList()
        }

        val cleanLottery = lotteryResult.replace(Regex("\\D"), "")
        if (cleanLottery.isBlank()) {
            _state.value = DrawState(error = "Informe um número válido da Loteria Federal.")
            return emptyList()
        }

        val countToPick = if (prizes.isNotEmpty()) prizes.size else 1
        val maxNumber = confirmedNumbers.maxOrNull() ?: raffle.totalNumbers
        val digitsCount = max(1, log10(maxNumber.toDouble()).toInt() + 1)

        val extractedDigits = if (cleanLottery.length >= digitsCount) {
            cleanLottery.takeLast(digitsCount)
        } else {
            cleanLottery
        }

        val extractedNumber = extractedDigits.toIntOrNull() ?: 0

        val firstWinner = if (extractedNumber in confirmedNumbers) {
            extractedNumber
        } else {
            confirmedNumbers.minByOrNull { kotlin.math.abs(it - extractedNumber) } ?: confirmedNumbers.random()
        }

        val winningNumbers = mutableListOf(firstWinner)
        val remaining = confirmedNumbers.distinct().filter { it != firstWinner }.shuffled()
        if (countToPick > 1) {
            winningNumbers.addAll(remaining.take(countToPick - 1))
        }

        buildDrawState(winningNumbers, confirmedReservations)
        return winningNumbers
    }

    private fun buildDrawState(winningNumbers: List<Int>, confirmedReservations: List<Reservation>) {
        val winnerNames = mutableListOf<String>()
        val winnerPhones = mutableListOf<String>()

        winningNumbers.forEach { winningNumber ->
            val winningReservation = confirmedReservations.find { winningNumber in it.numbers }
            val buyerName = winningReservation?.buyerName ?: "Comprador Desconhecido"
            val buyerPhone = winningReservation?.buyerPhone ?: ""

            winnerNames.add(buyerName)
            winnerPhones.add(buyerPhone)
        }

        _state.value = DrawState(
            isLoading = false,
            winnerNumbers = winningNumbers,
            winnerNames = winnerNames,
            winnerPhones = winnerPhones
        )
    }

    suspend fun persistDraw(
        raffle: Raffle,
        prizes: List<Prize>
    ): Result<Unit> {
        val currentState = _state.value
        val winningNumbers = currentState.winnerNumbers
        val winnerNames = currentState.winnerNames
        val winnerPhones = currentState.winnerPhones

        if (winningNumbers.isEmpty()) {
            return Result.failure(IllegalStateException("Sorteio não realizado ainda."))
        }

        return try {
            winningNumbers.forEachIndexed { index, winningNumber ->
                val buyerName = winnerNames.getOrElse(index) { "Desconhecido" }
                val buyerPhone = winnerPhones.getOrElse(index) { "" }

                if (prizes.isNotEmpty() && index < prizes.size) {
                    val prize = prizes[index]
                    prizeRepository.updatePrizeWinner(prize.id, winningNumber, buyerName, buyerPhone)
                }
            }

            val firstWinningNumber = winningNumbers.firstOrNull() ?: 0
            val firstWinnerName = winnerNames.firstOrNull() ?: "Não identificado"

            raffleRepository.updateRaffleDraw(raffle.id, "DRAWN", firstWinningNumber, firstWinnerName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
