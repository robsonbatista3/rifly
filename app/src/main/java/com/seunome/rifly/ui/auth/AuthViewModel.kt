package com.seunome.rifly.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.rifly.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        _isAuthenticated.value = authRepository.isLoggedIn()
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success
                    _isAuthenticated.value = true
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "Erro ao fazer login.")
                }
            )
        }
    }

    fun register(name: String, email: String, password: String, phone: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.register(name, email, password, phone)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success
                    _isAuthenticated.value = true
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "Erro ao criar conta.")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isAuthenticated.value = false
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
