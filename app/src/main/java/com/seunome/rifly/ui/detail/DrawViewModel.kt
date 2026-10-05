package com.seunome.rifly.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.model.Reservation
import com.seunome.rifly.data.repository.RaffleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max

data class DrawState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val winnerNumber: Int? = null,
    val winnerName: String? = null,
    val winnerPhone: String? = null
)

sealed class DrawAnimationState {
    object Idle : DrawAnimationState()
    data class Rolling(val currentNumber: Int) : DrawAnimationState()
    data class Finished(val winningNumber: Int) : DrawAnimationState()
}

class DrawViewModel : ViewModel() {

    private val raffleRepository = RaffleRepository()

    private val _state = MutableStateFlow(DrawState())
    val state: StateFlow<DrawState> = _state.asStateFlow()

    private val _animation = MutableStateFlow<DrawAnimationState>(DrawAnimationState.Idle)
    val animation: StateFlow<DrawAnimationState> = _animation.asStateFlow()

    fun drawRandom(raffle: Raffle, confirmedReservations: List<Reservation>) {
        val confirmedNumbers = confirmedReservations.flatMap { it.numbers }
        if (confirmedNumbers.isEmpty()) {
            _state.value = DrawState(error = "Não há nenhuma reserva confirmada para realizar o sorteio.")
            return
        }

        _state.value = DrawState(isLoading = true)

        viewModelScope.launch {
            for (i in 1..30) {
                val randomSample = confirmedNumbers.random()
                _animation.value = DrawAnimationState.Rolling(randomSample)
                delay(100)
            }

            val winningNumber = confirmedNumbers.random()
            finishDraw(raffle, winningNumber, confirmedReservations)
        }
    }

    fun drawByLottery(raffle: Raffle, confirmedReservations: List<Reservation>, lotteryResult: String) {
        val confirmedNumbers = confirmedReservations.flatMap { it.numbers }
        if (confirmedNumbers.isEmpty()) {
            _state.value = DrawState(error = "Não há nenhuma reserva confirmada para realizar o sorteio.")
            return
        }

        val cleanLottery = lotteryResult.replace(Regex("\\D"), "")
        if (cleanLottery.isBlank()) {
            _state.value = DrawState(error = "Informe um número válido da Loteria Federal.")
            return
        }

        _state.value = DrawState(isLoading = true)

        viewModelScope.launch {
            val maxNumber = confirmedNumbers.maxOrNull() ?: raffle.totalNumbers
            val digitsCount = max(1, log10(maxNumber.toDouble()).toInt() + 1)

            val extractedDigits = if (cleanLottery.length >= digitsCount) {
                cleanLottery.takeLast(digitsCount)
            } else {
                cleanLottery
            }

            val extractedNumber = extractedDigits.toIntOrNull() ?: 0

            val winningNumber = if (extractedNumber in confirmedNumbers) {
                extractedNumber
            } else {
                confirmedNumbers.minByOrNull { kotlin.math.abs(it - extractedNumber) } ?: confirmedNumbers.random()
            }

            for (i in 1..20) {
                val sample = confirmedNumbers.random()
                _animation.value = DrawAnimationState.Rolling(sample)
                delay(100)
            }

            finishDraw(raffle, winningNumber, confirmedReservations)
        }
    }

    private suspend fun finishDraw(raffle: Raffle, winningNumber: Int, confirmedReservations: List<Reservation>) {
        val winningReservation = confirmedReservations.find { winningNumber in it.numbers }
        val buyerName = winningReservation?.buyerName ?: "Comprador Desconhecido"
        val buyerPhone = winningReservation?.buyerPhone ?: ""

        val updates = mapOf(
            "status" to "DRAWN",
            "winner_number" to winningNumber,
            "winner_name" to buyerName
        )

        val result = raffleRepository.updateRaffle(raffle.id, updates)
        result.fold(
            onSuccess = {
                _state.value = DrawState(
                    isLoading = false,
                    winnerNumber = winningNumber,
                    winnerName = buyerName,
                    winnerPhone = buyerPhone
                )
                _animation.value = DrawAnimationState.Finished(winningNumber)
            },
            onFailure = { ex ->
                _state.value = DrawState(
                    isLoading = false,
                    error = "Erro ao salvar resultado do sorteio: ${ex.localizedMessage}"
                )
                _animation.value = DrawAnimationState.Idle
            }
        )
    }

    fun resetAnimation() {
        _animation.value = DrawAnimationState.Idle
    }
}
