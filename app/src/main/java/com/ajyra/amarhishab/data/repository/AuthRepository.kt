package com.ajyra.amarhishab.data.repository

import android.util.Log
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.AmarHishabApiService
import com.ajyra.amarhishab.network.ApiClient
import com.ajyra.amarhishab.network.NetworkErrorParser
import com.ajyra.amarhishab.network.NetworkResult
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

    suspend fun exchangeAuthCode(authCode: String): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exchanging authorization code with backend...")
            val authResult = ApiClient.executeAuthCodeExchange(authCode)

            val userDto = authResult.userDto
            val email = userDto?.email?.takeIf { it.isNotBlank() } ?: "user@amarhishab.app"
            val givenName = userDto?.firstName.orEmpty()
            val familyName = userDto?.lastName.orEmpty()
            val displayName = listOfNotNull(givenName.ifBlank { null }, familyName.ifBlank { null })
                .joinToString(" ")
                .trim()
                .ifBlank { userDto?.username?.ifBlank { null } ?: email.substringBefore("@") }

            val user = User(
                id = userDto?.id?.takeIf { it.isNotBlank() } ?: authResult.sessionId ?: email,
                email = email,
                username = userDto?.username ?: email.substringBefore("@"),
                firstName = givenName,
                lastName = familyName,
                displayName = displayName,
                avatarUrl = userDto?.avatar,
                isGoogleUser = true,
                isVerified = userDto?.isVerified ?: true
            )

            sessionManager.saveAuthSession(
                token = authResult.token,
                sessionId = authResult.sessionId,
                user = user,
                isGoogle = true
            )

            Log.d(TAG, "Authorization exchange succeeded, session saved for ${user.email}")

            // Trigger background sync after login
            try {
                financeRepository.syncWithBackend()
            } catch (e: Exception) {
                Log.w(TAG, "Post-login sync deferred: ${e.message}")
            }

            NetworkResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Code exchange failed: ${e.message}")
            NetworkErrorParser.parse(e, isLoginAttempt = true)
        }
    }

    suspend fun logout(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val currentSessionId = sessionManager.getSessionId()
        try {
            ApiClient.executeBackendLogout(currentSessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Server logout note: ${e.message}")
        }
        try {
            sessionManager.clearSession()
            // Preserve user financial records on logout (do not call clearLocalData())
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local session: ${e.message}")
        }
        NetworkResult.Success(Unit)
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
