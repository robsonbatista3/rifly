package com.seunome.rifly.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.model.Notification
import com.seunome.rifly.data.repository.AuthRepository
import com.seunome.rifly.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository()
    private val authRepository = AuthRepository()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = notificationRepository.getNotifications(userId)
            result.fold(
                onSuccess = { list ->
                    _notifications.value = list
                },
                onFailure = {}
            )
            _isLoading.value = false
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
            load()
        }
    }

    fun markAllAsRead() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
            load()
        }
    }
}
