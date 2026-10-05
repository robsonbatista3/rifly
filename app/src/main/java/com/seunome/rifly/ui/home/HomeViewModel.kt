package com.seunome.rifly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.repository.AuthRepository
import com.seunome.rifly.data.repository.RaffleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val raffleRepository = RaffleRepository()
    private val authRepository = AuthRepository()

    private val _raffles = MutableStateFlow<List<Raffle>>(emptyList())
    val raffles: StateFlow<List<Raffle>> = _raffles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRaffles()
    }

    fun loadRaffles() {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _raffles.value = emptyList()
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = raffleRepository.getMyRaffles(userId)
            result.fold(
                onSuccess = { list ->
                    _raffles.value = list
                },
                onFailure = {
                    _raffles.value = emptyList()
                }
            )
            _isLoading.value = false
        }
    }
}
