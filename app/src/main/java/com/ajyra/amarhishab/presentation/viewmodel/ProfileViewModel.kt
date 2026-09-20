package com.ajyra.amarhishab.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.BuildConfig
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.local.ReleaseNote
import com.ajyra.amarhishab.data.local.ReleaseNotesRepository
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.data.repository.GitHubUpdateRepository
import com.ajyra.amarhishab.model.AppUpdateInfo
import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckState()
    data class UpToDate(val versionName: String) : UpdateCheckState()
    data class Error(val messageEn: String, val messageBn: String) : UpdateCheckState()
}

class ProfileViewModel(
    private val financeRepository: FinanceRepository,
    private val sessionManager: EncryptedSessionManager
) : ViewModel() {

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val currentUser: StateFlow<User> = sessionManager.currentUser
    val language: StateFlow<String> = sessionManager.language
    val themeMode: StateFlow<String> = sessionManager.themeMode

    private val _notificationsEnabled = MutableStateFlow(sessionManager.areNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _expenseReminders = MutableStateFlow(sessionManager.areExpenseRemindersEnabled())
    val expenseReminders: StateFlow<Boolean> = _expenseReminders.asStateFlow()

    private val _monthlySummary = MutableStateFlow(sessionManager.isMonthlySummaryEnabled())
    val monthlySummary: StateFlow<Boolean> = _monthlySummary.asStateFlow()

    private val _appLock = MutableStateFlow(sessionManager.isAppLockEnabled())
    val appLock: StateFlow<Boolean> = _appLock.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    val releaseNotes: ReleaseNote = ReleaseNotesRepository.getLatest()

    fun updateProfile(name: String, email: String, phone: String) {
        sessionManager.saveUserProfile(name, email, phone)
        _snackbarMessage.value = "Profile updated successfully"
    }

    fun updateProfilePhoto(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            try {
                val savedUriString = withContext(Dispatchers.IO) {
                    val avatarFile = File(context.filesDir, "user_avatar.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        FileOutputStream(avatarFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    avatarFile.absolutePath
                }
                sessionManager.updateProfileAvatar(savedUriString)
                _snackbarMessage.value = "Profile photo updated"
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to update profile photo: ${e.message}"
            }
        }
    }

    fun setLanguage(lang: String) {
        sessionManager.setLanguage(lang)
    }

    fun setTheme(theme: String) {
        sessionManager.setTheme(theme)
    }

    fun toggleNotifications(enabled: Boolean) {
        sessionManager.setNotificationsEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    fun toggleExpenseReminders(enabled: Boolean) {
        sessionManager.setExpenseReminders(enabled)
        _expenseReminders.value = enabled
    }

    fun toggleMonthlySummary(enabled: Boolean) {
        sessionManager.setMonthlySummaryEnabled(enabled)
        _monthlySummary.value = enabled
    }

    fun toggleAppLock(enabled: Boolean) {
        sessionManager.setAppLockEnabled(enabled)
        _appLock.value = enabled
    }

    fun checkForUpdate(
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ) {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            when (val res = GitHubUpdateRepository.checkLatestRelease()) {
                is NetworkResult.Success -> {
                    val info = res.data
                    if (info.latestVersionCode > currentVersionCode) {
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(info)
                    } else {
                        _updateCheckState.value = UpdateCheckState.UpToDate(currentVersionName)
                    }
                }
                is NetworkResult.Error -> {
                    _updateCheckState.value = UpdateCheckState.Error(
                        messageEn = res.messageEn,
                        messageBn = res.messageBn
                    )
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun checkForUpdates() {
        checkForUpdate()
    }

    fun dismissUpdateDialog() {
        _updateCheckState.value = UpdateCheckState.Idle
    }
}
