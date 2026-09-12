package com.ajyra.amarhishab.presentation.viewmodel

import android.net.Uri
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

    fun handleAuthDeepLink(uri: Uri?) {
        if (uri == null) return
        val scheme = uri.scheme
        val host = uri.host
        val path = uri.path
        if (scheme == "amarhishab" && host == "auth" && path == "/callback") {
            val error = uri.getQueryParameter("error")
            val errorDesc = uri.getQueryParameter("error_description")
            if (!error.isNullOrBlank()) {
                val errEn = errorDesc?.ifBlank { null } ?: "Sign in was cancelled or failed ($error)."
                val errBn = "সাইন-ইন বাতিল বা ব্যর্থ হয়েছে ($error)।"
                _uiState.value = AuthUiState.Error(errEn, errBn)
                return
            }

            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                exchangeAuthCode(code)
            } else {
                _uiState.value = AuthUiState.Error(
                    messageEn = "Authentication callback did not contain an authorization code.",
                    messageBn = "প্রমাণীকরণ কলব্যাকে কোনো অনুমোদন কোড পাওয়া যায়নি।"
                )
            }
        }
    }

    fun exchangeAuthCode(authCode: String) {
        if (authCode.isBlank()) {
            _uiState.value = AuthUiState.Error(
                messageEn = "Authorization code is missing.",
                messageBn = "অনুমোদন কোড পাওয়া যায়নি।"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.exchangeAuthCode(authCode)) {
                is NetworkResult.Success -> {
                    _currentUser.value = result.data
                    _uiState.value = AuthUiState.Success(result.data)
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

    fun onAuthCancelled() {
        if (_uiState.value is AuthUiState.Loading) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
