package com.ajyra.amarhishab.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ajyra.amarhishab.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EncryptedSessionManager(private val context: Context) {

    // Dedicated encrypted storage for sensitive authentication tokens and session credentials.
    // Strictly NO fallback to plaintext SharedPreferences for security credentials.
    private val securePrefs: SharedPreferences? by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Secure storage initialization failed, attempting single recovery: ${e.message}")
            try {
                // Remove potentially corrupted encrypted pref file and recreate
                context.deleteSharedPreferences(PREF_FILE_SECURE)
                createEncryptedPrefs()
            } catch (recoveryEx: Exception) {
                Log.e(TAG, "Secure storage recovery failed. Plaintext fallback is strictly prohibited: ${recoveryEx.message}")
                null
            }
        }
    }

    // Standard SharedPreferences used EXCLUSIVELY for non-sensitive local UI preferences (language, theme)
    private val settingsPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE_SETTINGS, Context.MODE_PRIVATE)
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE_SECURE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _isAuthenticated = MutableStateFlow(hasValidSession())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _language = MutableStateFlow(getSavedLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    private val _themeMode = MutableStateFlow(getSavedTheme())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun hasValidSession(): Boolean {
        val prefs = securePrefs ?: return false
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        val sessionId = prefs.getString(KEY_SESSION_ID, null)
        return (!token.isNullOrBlank()) || (!sessionId.isNullOrBlank())
    }

    fun saveAuthSession(
        token: String? = null,
        sessionId: String? = null,
        user: User? = null,
        isGoogle: Boolean = true
    ) {
        val prefs = securePrefs
            ?: throw SecurityException("Encrypted storage unavailable. Storing credentials in unencrypted storage is prohibited.")

        prefs.edit().apply {
            if (!token.isNullOrBlank()) putString(KEY_AUTH_TOKEN, token) else remove(KEY_AUTH_TOKEN)
            if (!sessionId.isNullOrBlank()) putString(KEY_SESSION_ID, sessionId) else remove(KEY_SESSION_ID)
            putBoolean(KEY_IS_GOOGLE, isGoogle)
            if (user != null) {
                putString(KEY_USER_ID, user.id)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USER_NAME, user.displayName.ifBlank { user.email })
                if (user.avatarUrl != null) {
                    putString(KEY_USER_AVATAR, user.avatarUrl)
                }
            }
            apply()
        }
        _isAuthenticated.value = true
    }

    fun getAuthToken(): String? = securePrefs?.getString(KEY_AUTH_TOKEN, null)

    fun getSessionId(): String? = securePrefs?.getString(KEY_SESSION_ID, null)

    fun getCurrentUser(): User? {
        val prefs = securePrefs ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        return User(
            id = prefs.getString(KEY_USER_ID, "") ?: "",
            email = email,
            displayName = prefs.getString(KEY_USER_NAME, "") ?: "",
            avatarUrl = prefs.getString(KEY_USER_AVATAR, null),
            isGoogleUser = prefs.getBoolean(KEY_IS_GOOGLE, false),
            isVerified = true
        )
    }

    fun isGoogleAuth(): Boolean = securePrefs?.getBoolean(KEY_IS_GOOGLE, false) ?: false

    fun clearSession() {
        securePrefs?.edit()?.apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_SESSION_ID)
            remove(KEY_IS_GOOGLE)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_AVATAR)
            apply()
        }
        _isAuthenticated.value = false
    }

    fun setLanguage(lang: String) {
        settingsPrefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang
    }

    fun getSavedLanguage(): String = settingsPrefs.getString(KEY_LANGUAGE, "bn") ?: "bn"

    fun setTheme(theme: String) {
        settingsPrefs.edit().putString(KEY_THEME, theme).apply()
        _themeMode.value = theme
    }

    fun getSavedTheme(): String = settingsPrefs.getString(KEY_THEME, "system") ?: "system"

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean = settingsPrefs.getBoolean(KEY_NOTIFICATIONS, true)

    fun setExpenseReminders(enabled: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_REMINDERS, enabled).apply()
    }

    fun areExpenseRemindersEnabled(): Boolean = settingsPrefs.getBoolean(KEY_REMINDERS, true)

    fun setMonthlySummaryEnabled(enabled: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_MONTHLY_SUMMARY, enabled).apply()
    }

    fun isMonthlySummaryEnabled(): Boolean = settingsPrefs.getBoolean(KEY_MONTHLY_SUMMARY, true)

    fun setAppLockEnabled(enabled: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    }

    fun isAppLockEnabled(): Boolean = settingsPrefs.getBoolean(KEY_APP_LOCK, false)

    companion object {
        private const val TAG = "EncryptedSessionManager"
        private const val PREF_FILE_SECURE = "amar_hishab_secure_prefs"
        private const val PREF_FILE_SETTINGS = "amar_hishab_settings"

        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_IS_GOOGLE = "is_google"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_MONTHLY_SUMMARY = "monthly_summary"
        private const val KEY_APP_LOCK = "app_lock"

        @Volatile
        private var INSTANCE: EncryptedSessionManager? = null

        fun getInstance(context: Context): EncryptedSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = EncryptedSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
