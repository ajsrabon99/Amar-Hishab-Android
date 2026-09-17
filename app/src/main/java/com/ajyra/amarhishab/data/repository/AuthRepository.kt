package com.ajyra.amarhishab.data.repository

import android.util.Log
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.AmarHishabApiService
import com.ajyra.amarhishab.network.ApiClient
import com.ajyra.amarhishab.network.LoginRequestDto
import com.ajyra.amarhishab.network.NetworkErrorParser
import com.ajyra.amarhishab.network.NetworkResult
import com.ajyra.amarhishab.network.PasswordResetRequestDto
import com.ajyra.amarhishab.network.RegisterRequestDto
import com.ajyra.amarhishab.network.ResendCodeRequestDto
import com.ajyra.amarhishab.network.VerifyCodeRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: AmarHishabApiService,
    private val sessionManager: EncryptedSessionManager,
    private val financeRepository: FinanceRepository
) {
    val isAuthenticated: StateFlow<Boolean> = sessionManager.isAuthenticated

    fun getCurrentUser(): User? = sessionManager.getCurrentUser()

    fun hasToken(): Boolean = !sessionManager.getAuthToken().isNullOrBlank()

    suspend fun validateSessionOnStartup(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.getAuthToken()
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No session token present on startup.")
            sessionManager.clearSession()
            return@withContext false
        }
        try {
            Log.d(TAG, "Validating token with GET /api/v1/auth/me/ ...")
            val response = apiService.getMe()
            val userDto = response.user
            if (userDto != null && !userDto.email.isNullOrBlank()) {
                val email = userDto.email
                val username = userDto.username?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                val givenName = userDto.firstName.orEmpty()
                val familyName = userDto.lastName.orEmpty()
                val displayName = listOfNotNull(givenName.ifBlank { null }, familyName.ifBlank { null })
                    .joinToString(" ")
                    .trim()
                    .ifBlank { username }

                val user = User(
                    id = userDto.id?.takeIf { it.isNotBlank() } ?: email,
                    email = email,
                    username = username,
                    firstName = givenName,
                    lastName = familyName,
                    displayName = displayName,
                    avatarUrl = userDto.avatar,
                    isVerified = userDto.isVerified ?: true
                )

                sessionManager.saveAuthSession(
                    token = token,
                    sessionId = sessionManager.getSessionId(),
                    user = user
                )
                Log.d(TAG, "Startup session validated for ${user.username}")

                try {
                    financeRepository.syncWithBackend()
                } catch (e: Exception) {
                    Log.w(TAG, "Post-startup sync note: ${e.message}")
                }
                true
            } else {
                Log.w(TAG, "GET /api/v1/auth/me/ returned empty user, clearing token.")
                sessionManager.clearSession()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Token validation failed on startup: ${e.message}. Clearing token.")
            sessionManager.clearSession()
            false
        }
    }

    suspend fun loginWithPassword(identifier: String, password: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting password authentication for user identifier: $identifier")
            val req = LoginRequestDto(
                identifier = identifier.trim(),
                email = if (identifier.contains("@")) identifier.trim() else null,
                username = if (!identifier.contains("@")) identifier.trim() else null,
                password = password
            )
            val response = apiService.loginWithPassword(req)

            val token = response.token ?: response.key
            if (token.isNullOrBlank()) {
                return@withContext NetworkResult.Error(
                    messageEn = response.message ?: "Server responded without an authentication token.",
                    messageBn = "সার্ভার থেকে প্রমাণীকরণ টোকেন পাওয়া যায়নি।"
                )
            }

            val userDto = response.user
            val email = userDto?.email?.takeIf { it.isNotBlank() } ?: if (identifier.contains("@")) identifier else "user@amarhishab.app"
            val username = userDto?.username?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            val givenName = userDto?.firstName.orEmpty()
            val familyName = userDto?.lastName.orEmpty()
            val displayName = listOfNotNull(givenName.ifBlank { null }, familyName.ifBlank { null })
                .joinToString(" ")
                .trim()
                .ifBlank { username }

            val user = User(
                id = userDto?.id?.takeIf { it.isNotBlank() } ?: email,
                email = email,
                username = username,
                firstName = givenName,
                lastName = familyName,
                displayName = displayName,
                avatarUrl = userDto?.avatar,
                isVerified = userDto?.isVerified ?: true
            )

            sessionManager.saveAuthSession(
                token = token,
                sessionId = response.sessionId,
                user = user
            )

            Log.d(TAG, "First-party authentication succeeded, token saved for ${user.username}")

            // Trigger background sync after login
            try {
                financeRepository.syncWithBackend()
            } catch (e: Exception) {
                Log.w(TAG, "Post-login sync deferred: ${e.message}")
            }

            NetworkResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            NetworkErrorParser.parse(e, isLoginAttempt = true)
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String = password
    ): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val req = RegisterRequestDto(
                username = username.trim(),
                email = email.trim(),
                password = password,
                confirmPassword = confirmPassword
            )
            val response = apiService.register(req)
            val msg = response.message ?: "Account created! A verification code has been sent to your email."
            NetworkResult.Success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}")
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun verifyEmail(identifier: String, code: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            val req = VerifyCodeRequestDto(
                identifier = identifier.trim(),
                code = code.trim()
            )
            val response = apiService.verifyEmail(req)
            val token = response.token ?: response.key
            if (token.isNullOrBlank()) {
                return@withContext NetworkResult.Error(
                    messageEn = response.message ?: "Verification failed: No authentication token received.",
                    messageBn = "ভেরিফিকেশন ব্যর্থ: প্রমাণীকরণ টোকেন পাওয়া যায়নি।"
                )
            }

            val userDto = response.user
            val email = userDto?.email?.takeIf { it.isNotBlank() } ?: if (identifier.contains("@")) identifier else "user@amarhishab.app"
            val username = userDto?.username?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            val givenName = userDto?.firstName.orEmpty()
            val familyName = userDto?.lastName.orEmpty()
            val displayName = listOfNotNull(givenName.ifBlank { null }, familyName.ifBlank { null })
                .joinToString(" ")
                .trim()
                .ifBlank { username }

            val user = User(
                id = userDto?.id?.takeIf { it.isNotBlank() } ?: email,
                email = email,
                username = username,
                firstName = givenName,
                lastName = familyName,
                displayName = displayName,
                avatarUrl = userDto?.avatar,
                isVerified = true
            )

            sessionManager.saveAuthSession(
                token = token,
                sessionId = response.sessionId,
                user = user
            )

            try {
                financeRepository.syncWithBackend()
            } catch (e: Exception) {
                Log.w(TAG, "Post-verification sync deferred: ${e.message}")
            }

            NetworkResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed: ${e.message}")
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun resendVerificationCode(identifier: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val req = ResendCodeRequestDto(identifier = identifier.trim())
            val response = apiService.resendCode(req)
            val msg = response.message ?: "Verification code resent to your email."
            NetworkResult.Success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Resend code failed: ${e.message}")
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun requestPasswordReset(email: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val req = PasswordResetRequestDto(email = email.trim())
            val response = apiService.resetPassword(req)
            val msg = response.message ?: "If an account with that email exists, a reset code was sent."
            NetworkResult.Success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset request failed: ${e.message}")
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val req = PasswordResetRequestDto(
                email = email.trim(),
                code = code.trim(),
                newPassword = newPassword
            )
            val response = apiService.resetPassword(req)
            val msg = response.message ?: "Password reset successfully."
            NetworkResult.Success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset confirmation failed: ${e.message}")
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun logout(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val currentToken = sessionManager.getAuthToken()
        try {
            ApiClient.executeBackendLogout(currentToken)
        } catch (e: Exception) {
            Log.w(TAG, "Server logout note: ${e.message}")
        }
        try {
            sessionManager.clearSession()
            // Financial records are preserved locally in Room database
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local session: ${e.message}")
        }
        NetworkResult.Success(Unit)
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
