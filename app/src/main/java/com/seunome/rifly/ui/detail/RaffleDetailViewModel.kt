package com.seunome.rifly.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.model.Reservation
import com.seunome.rifly.data.repository.RaffleRepository
import com.seunome.rifly.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RaffleDetailViewModel : ViewModel() {

    private val reservationRepository = ReservationRepository()
    private val raffleRepository = RaffleRepository()

    private val _raffle = MutableStateFlow<Raffle?>(null)
    val raffle: StateFlow<Raffle?> = _raffle.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val pendingReservations: StateFlow<List<Reservation>> = _reservations.map { list ->
        list.filter { it.status == "PENDING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val confirmedReservations: StateFlow<List<Reservation>> = _reservations.map { list ->
        list.filter { it.status == "CONFIRMED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val confirmedNumbers: StateFlow<Set<Int>> = _reservations.map { list ->
        list.filter { it.status == "CONFIRMED" }.flatMap { it.numbers }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val soldCount: StateFlow<Int> = confirmedNumbers.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val revenue: StateFlow<Double> = confirmedReservations.map { list ->
        list.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setup(raffle: Raffle) {
        _raffle.value = raffle
        refresh()
    }

    fun refresh() {
        val currentRaffle = _raffle.value ?: return
        _isLoading.value = true
        viewModelScope.launch {
            val result = reservationRepository.getReservationsByRaffle(currentRaffle.id)
            result.fold(
                onSuccess = { list ->
                    _reservations.value = list
                },
                onFailure = { ex ->
                    _message.value = "Erro ao carregar reservas: ${ex.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun confirm(reservation: Reservation) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = reservationRepository.confirmReservation(reservation.id)
            result.fold(
                onSuccess = {
                    _message.value = "Pagamento de ${reservation.buyerName} confirmado! ✅"
                    refresh()
                },
                onFailure = { ex ->
                    _message.value = "Erro ao confirmar pagamento: ${ex.localizedMessage}"
                    _isLoading.value = false
                }
            )
        }
    }

    fun cancel(reservation: Reservation) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = reservationRepository.cancelReservation(reservation.id)
            result.fold(
                onSuccess = {
                    _message.value = "Reserva de ${reservation.buyerName} cancelada"
                    refresh()
                },
                onFailure = { ex ->
                    _message.value = "Erro ao cancelar reserva: ${ex.localizedMessage}"
                    _isLoading.value = false
                }
            )
        }
    }

    fun getConfirmedReservations(): List<Reservation> {
        return confirmedReservations.value
    }

    fun getConfirmedNumbers(): List<Int> {
        return confirmedReservations.value.flatMap { it.numbers }
    }

    fun canDraw(): Boolean {
        val currentRaffle = _raffle.value
        return currentRaffle?.status == "ACTIVE" && confirmedReservations.value.isNotEmpty()
    }

    fun clearMessage() {
        _message.value = null
    }
}
