package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.AuthRepository
import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val messageEn: String, val messageBn: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isAuthenticated: StateFlow<Boolean> = authRepository.isAuthenticated

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(authRepository.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isCheckingSession = MutableStateFlow<Boolean>(authRepository.hasToken())
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    init {
        checkSessionStartup()
    }

    fun checkSessionStartup() {
        viewModelScope.launch {
            if (authRepository.hasToken()) {
                val isValid = authRepository.validateSessionOnStartup()
                if (isValid) {
                    _currentUser.value = authRepository.getCurrentUser()
                } else {
                    _currentUser.value = null
                }
            } else {
                _currentUser.value = null
            }
            _isCheckingSession.value = false
        }
    }

    fun loginWithPassword(
        identifier: String,
        password: String,
        onSuccess: (() -> Unit)? = null
    ) {
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(
                messageEn = "Please enter both username/email and password.",
                messageBn = "ইউজারনেম/ইমেইল এবং পাসওয়ার্ড উভয়ই লিখুন।"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.loginWithPassword(identifier, password)) {
                is NetworkResult.Success -> {
                    _currentUser.value = result.data
                    _uiState.value = AuthUiState.Success(result.data)
                    onSuccess?.invoke()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState.Error(
                        messageEn = result.messageEn,
                        messageBn = result.messageBn
                    )
                }
                NetworkResult.Loading -> {
                    _uiState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String = password,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.register(username, email, password, confirmPassword)) {
                is NetworkResult.Success -> {
                    _uiState.value = AuthUiState.Idle
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.messageEn, result.messageBn)
                    onComplete(false, result.messageEn)
                }
                NetworkResult.Loading -> {
                    _uiState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun verifyEmail(
        identifier: String,
        code: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.verifyEmail(identifier, code)) {
                is NetworkResult.Success -> {
                    _currentUser.value = result.data
                    _uiState.value = AuthUiState.Success(result.data)
                    onComplete(true, "Email verified successfully!")
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.messageEn, result.messageBn)
                    onComplete(false, result.messageEn)
                }
                NetworkResult.Loading -> {
                    _uiState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun resendVerificationCode(
        identifier: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = authRepository.resendVerificationCode(identifier)) {
                is NetworkResult.Success -> onComplete(true, result.data)
                is NetworkResult.Error -> onComplete(false, result.messageEn)
                NetworkResult.Loading -> {}
            }
        }
    }

    fun requestPasswordReset(
        email: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.requestPasswordReset(email)) {
                is NetworkResult.Success -> {
                    _uiState.value = AuthUiState.Idle
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.messageEn, result.messageBn)
                    onComplete(false, result.messageEn)
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    fun confirmPasswordReset(
        email: String,
        code: String,
        newPassword: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.confirmPasswordReset(email, code, newPassword)) {
                is NetworkResult.Success -> {
                    _uiState.value = AuthUiState.Idle
                    onComplete(true, result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.messageEn, result.messageBn)
                    onComplete(false, result.messageEn)
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    fun setError(messageEn: String, messageBn: String) {
        _uiState.value = AuthUiState.Error(messageEn, messageBn)
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
