package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.BuildConfig
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.local.NotificationRepository
import com.ajyra.amarhishab.data.local.ReleaseNote
import com.ajyra.amarhishab.data.local.ReleaseNotesRepository
import com.ajyra.amarhishab.data.repository.GitHubUpdateRepository
import com.ajyra.amarhishab.model.AppNotification
import com.ajyra.amarhishab.model.NotificationType
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val sessionManager: EncryptedSessionManager,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

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

    fun checkForUpdates(
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
                        // Trigger an in-app notification about the new update
                        notificationRepository.addNotification(
                            AppNotification(
                                id = "update_${info.latestVersionCode}",
                                titleEn = "New Update Available: v${info.latestVersion}",
                                titleBn = "নতুন আপডেট পাওয়া গেছে: v${info.latestVersion}",
                                messageEn = "Version ${info.latestVersion} is available with new improvements. Tap to download.",
                                messageBn = "আমার হিসাব এর নতুন সংস্করণ ${info.latestVersion} এখন ডাউনলোডের জন্য প্রস্তুত।",
                                type = NotificationType.UPDATE,
                                isRead = false,
                                actionUrl = info.downloadUrl
                            ),
                            showSystemNotification = true
                        )
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

    fun dismissUpdateDialog() {
        _updateCheckState.value = UpdateCheckState.Idle
    }
}
