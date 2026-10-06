package com.seunome.rifly.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.repository.AuthRepository
import com.seunome.rifly.data.repository.NotificationRepository
import com.seunome.rifly.data.repository.RaffleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val raffleRepository = RaffleRepository()
    private val authRepository = AuthRepository()
    private val notificationRepository = NotificationRepository()

    private val _raffles = MutableStateFlow<List<Raffle>>(emptyList())
    val raffles: StateFlow<List<Raffle>> = _raffles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        loadRaffles()
        checkAdminStatus()
        viewModelScope.launch {
            while (isActive) {
                loadUnreadCount()
                delay(30000)
            }
        }
    }

    fun checkAdminStatus() {
        viewModelScope.launch {
            val admin = authRepository.isAdmin()
            Log.d("AdminDebug", "isAdmin carregado = $admin")
            _isAdmin.value = admin
        }
    }

    fun loadUnreadCount() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount(userId)
            result.getOrNull()?.let { count ->
                _unreadCount.value = count
            }
        }
    }

    fun loadRaffles() {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _raffles.value = emptyList()
            return
        }

        checkAdminStatus()

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
            loadUnreadCount()
        }
    }
}
