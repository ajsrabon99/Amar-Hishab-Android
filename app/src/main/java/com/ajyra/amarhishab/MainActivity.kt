package com.ajyra.amarhishab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.presentation.navigation.AppNavigation
import com.ajyra.amarhishab.presentation.viewmodel.AuthViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ViewModelFactory
import com.ajyra.amarhishab.ui.theme.AmarHishabTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        ViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = EncryptedSessionManager.getInstance(applicationContext)

        setContent {
            val themeMode by sessionManager.themeMode.collectAsState()
            AmarHishabTheme(themeSetting = themeMode) {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
