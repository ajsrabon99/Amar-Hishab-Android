package com.ajyra.amarhishab

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.presentation.navigation.AppNavigation
import com.ajyra.amarhishab.presentation.screens.AnimatedSplashScreen
import com.ajyra.amarhishab.presentation.screens.AppLockScreen
import com.ajyra.amarhishab.ui.theme.AmarHishabTheme
import com.ajyra.amarhishab.utils.BiometricHelper

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = EncryptedSessionManager.getInstance(applicationContext)

        setContent {
            val themeMode by sessionManager.themeMode.collectAsState()
            val language by sessionManager.language.collectAsState()
            val isBengali = language == "bn"

            var isSplashFinished by remember { mutableStateOf(false) }
            val isAppLockEnabled = remember { sessionManager.isAppLockEnabled() }
            var isUnlocked by remember { mutableStateOf(!isAppLockEnabled) }

            // Android 13+ Notification Permission Launcher
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                sessionManager.setNotificationsEnabled(isGranted)
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            fun triggerBiometricUnlock() {
                if (BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                    BiometricHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        title = if (isBengali) "আমার হিসাব আনলক করুন" else "Unlock Amar Hishab",
                        subtitle = if (isBengali) "বায়োমেট্রিক বা স্ক্রিন লক দিয়ে নিশ্চিত করুন" else "Confirm with biometric or device lock",
                        onSuccess = {
                            isUnlocked = true
                        },
                        onError = {
                            // User cancelled or failed; stay locked
                        }
                    )
                } else {
                    isUnlocked = true
                }
            }

            LaunchedEffect(isSplashFinished) {
                if (isSplashFinished && !isUnlocked) {
                    triggerBiometricUnlock()
                }
            }

            AmarHishabTheme(themeSetting = themeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = Pair(isSplashFinished, isUnlocked),
                        animationSpec = tween(350),
                        label = "app_root_crossfade"
                    ) { (splashDone, unlocked) ->
                        when {
                            !splashDone -> {
                                AnimatedSplashScreen(
                                    onSplashFinished = {
                                        isSplashFinished = true
                                    }
                                )
                            }
                            !unlocked -> {
                                AppLockScreen(
                                    isBengali = isBengali,
                                    onUnlockClick = {
                                        triggerBiometricUnlock()
                                    }
                                )
                            }
                            else -> {
                                AppNavigation()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
