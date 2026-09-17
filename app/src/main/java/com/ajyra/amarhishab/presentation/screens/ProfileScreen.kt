package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.BuildConfig
import com.ajyra.amarhishab.data.local.ReleaseNotesRepository
import com.ajyra.amarhishab.presentation.viewmodel.ProfileViewModel
import com.ajyra.amarhishab.presentation.viewmodel.UpdateCheckState
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    isBengali: Boolean,
    onLanguageToggle: () -> Unit,
    onLogout: () -> Unit
) {
    val user by profileViewModel.user.collectAsState()
    val isAuthenticated by profileViewModel.isAuthenticated.collectAsState()
    val updateCheckState by profileViewModel.updateCheckState.collectAsState()
    val snackbarMessage by profileViewModel.snackbarMessage.collectAsState()

    var dailyReminder by remember { mutableStateOf(true) }
    var monthlyRecap by remember { mutableStateOf(true) }
    var biometricLock by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Connect account form state
    var connectIdentifier by remember { mutableStateOf("") }
    var connectPassword by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.clearSnackbar()
        }
    }

    // Disconnect Website Account Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = {
                Text(if (isBengali) "অ্যাকাউন্ট সংযোগ বিচ্ছিন্ন করবেন?" else "Disconnect Website Account?")
            },
            text = {
                Text(
                    if (isBengali)
                        "এটি সার্ভার থেকে আপনার মোবাইল এক্সেস টোকেন বাতিল করবে। আপনার সংরক্ষিত স্থানীয় হিসাব сохран থাকবে।"
                    else
                        "This will revoke your mobile authorization token on the Amar Hishab server and clear session locally."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        profileViewModel.disconnectAccount(onDisconnectComplete = onLogout)
                    }
                ) {
                    Text(if (isBengali) "সংযোগ বিচ্ছিন্ন করুন" else "Disconnect Account", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Connect Account Dialog
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { if (!isConnecting) showConnectDialog = false },
            title = {
                Text(if (isBengali) "অমর হিসাব অ্যাকাউন্ট সংযোগ করুন" else "Connect Amar Hishab Account")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBengali)
                            "ওয়েবসাইট অ্যাকাউন্টের ইউজারনেম/ইমেইল এবং পাসওয়ার্ড দিন:"
                        else
                            "Enter your website username/email and password:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = connectIdentifier,
                        onValueChange = { connectIdentifier = it },
                        label = { Text(if (isBengali) "ইউজারনেম বা ইমেইল" else "Username or Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = connectPassword,
                        onValueChange = { connectPassword = it },
                        label = { Text(if (isBengali) "পাসওয়ার্ড" else "Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (connectIdentifier.isNotBlank() && connectPassword.isNotBlank()) {
                            isConnecting = true
                            profileViewModel.connectAccount(connectIdentifier, connectPassword) { success, err ->
                                isConnecting = false
                                if (success) {
                                    showConnectDialog = false
                                    connectPassword = ""
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(err ?: "Authentication failed")
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isConnecting && connectIdentifier.isNotBlank() && connectPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (isBengali) "সংযোগ করুন" else "Connect")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConnectDialog = false },
                    enabled = !isConnecting
                ) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Sign Out Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(if (isBengali) "লগআউট করতে চান?" else "Confirm Sign Out")
            },
            text = {
                Text(
                    if (isBengali)
                        "আপনি কি আপনার হিসাব অ্যাকাউন্ট থেকে লগআউট করতে চান?"
                    else
                        "Are you sure you want to sign out from your account?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        profileViewModel.logout(onLogoutComplete = onLogout)
                    }
                ) {
                    Text(if (isBengali) "লগআউট" else "Sign Out", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    if (showReleaseNotesDialog) {
        val notes = ReleaseNotesRepository.releaseNotes
        AlertDialog(
            onDismissRequest = { showReleaseNotesDialog = false },
            title = {
                Text(if (isBengali) "রিলিজ নোটস" else "Release Notes")
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (note in notes) {
                        Text(
                            text = "v${note.versionName} (${note.releaseDate})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val feats = if (isBengali) note.featuresBn else note.featuresEn
                        for (feat in feats) {
                            Text(
                                text = "• $feat",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReleaseNotesDialog = false }) {
                    Text(if (isBengali) "ঠিক আছে" else "Close")
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(if (isBengali) "সহায়তা ও তথ্য" else "Help & Support")
            },
            text = {
                Column {
                    Text(
                        text = if (isBengali)
                            "আমার হিসাব একটি নিরাপদ আর্থিক ট্র্যাকিং অ্যাপ। আপনার সকল ডেটা সম্পূর্ণ গোপনীয়ভাবে সংরক্ষিত থাকে।"
                        else
                            "Amar Hishab is a secure financial management app. Your financial records are encrypted and stored safely."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Email: support@amarhishab.app\nDeveloper: Ajyra Tech",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(if (isBengali) "ঠিক আছে" else "Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "প্রোফাইল ও সেটিংস" else "Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("profile_screen_scroll"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = user?.displayName?.ifBlank { user?.username } ?: (if (isBengali) "ব্যবহারকারী" else "User"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user?.email ?: "user@amarhishab.app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAuthenticated) {
                                        if (isBengali) "✓ সক্রিয় অ্যাকাউন্ট" else "✓ Active Account"
                                    } else {
                                        if (isBengali) "অফলাইন মোড" else "Offline Mode"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ================= SECTION: ACCOUNT (REQUIREMENT 9) =================
            item {
                SectionHeader(if (isBengali) "অ্যাকাউন্ট সংযোগ" else "Account")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        if (isAuthenticated && user != null) {
                            // Connected Account info
                            SettingsRow(
                                icon = Icons.Default.AccountCircle,
                                title = if (isBengali) "সংযুক্ত অ্যাকাউন্ট" else "Connected Account",
                                subtitle = "${user?.username} (${user?.email})"
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            SettingsRow(
                                icon = Icons.Default.CheckCircle,
                                title = if (isBengali) "সংযোগ স্ট্যাটাস" else "Connection Status",
                                subtitle = if (isBengali) "ওয়েবসাইট এবং মোবাইল সিঙ্ক সক্রিয়" else "Website & mobile sync active",
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isBengali) "সংযুক্ত" else "Connected",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            SettingsRow(
                                icon = Icons.Default.LinkOff,
                                title = if (isBengali) "ওয়েবসাইট অ্যাকাউন্ট ডিসকানেক্ট" else "Disconnect Website Account",
                                subtitle = if (isBengali) "সার্ভার টোকেন বাতিল করুন" else "Revoke server access token",
                                onClick = { showDisconnectDialog = true },
                                trailingContent = {
                                    Text(
                                        text = if (isBengali) "ডিসকানেক্ট" else "Disconnect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        } else {
                            // Not Connected state
                            SettingsRow(
                                icon = Icons.Default.Sync,
                                title = if (isBengali) "অমর হিসাব অ্যাকাউন্ট সংযোগ করুন" else "Connect Amar Hishab Account",
                                subtitle = if (isBengali) "ওয়েবসাইট অ্যাকাউন্টের সাথে সিঙ্ক করুন" else "Sync with your website account",
                                onClick = { showConnectDialog = true },
                                trailingContent = {
                                    Button(
                                        onClick = { showConnectDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text(if (isBengali) "সংযোগ করুন" else "Connect")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Preferences Section
            item {
                SectionHeader(if (isBengali) "পছন্দসমূহ" else "Preferences")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Language toggle
                        SettingsRow(
                            icon = Icons.Default.Language,
                            title = if (isBengali) "ভাষা (Language)" else "Language",
                            subtitle = if (isBengali) "বর্তমান: বাংলা" else "Current: English",
                            trailingContent = {
                                Button(
                                    onClick = onLanguageToggle,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text(if (isBengali) "English" else "বাংলা")
                                }
                            }
                        )
                    }
                }
            }

            // Notifications Section
            item {
                SectionHeader(if (isBengali) "নোটিফিকেশন ও রিমাইন্ডার" else "Notifications & Reminders")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Alarm,
                            title = if (isBengali) "দৈনিক খরচ এন্ট্রি রিমাইন্ডার" else "Daily Expense Reminder",
                            subtitle = if (isBengali) "প্রতিদিন রাত ৯টায় রিমাইন্ডার" else "Daily reminder at 9:00 PM",
                            trailingContent = {
                                Switch(
                                    checked = dailyReminder,
                                    onCheckedChange = { dailyReminder = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.Assessment,
                            title = if (isBengali) "মাসিক সারসংক্ষেপ রিপোর্ট" else "Monthly Summary Report",
                            subtitle = if (isBengali) "মাসের শুরুতে আগের মাসের হিসাব" else "Monthly financial recap",
                            trailingContent = {
                                Switch(
                                    checked = monthlyRecap,
                                    onCheckedChange = { monthlyRecap = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                                )
                            }
                        )
                    }
                }
            }

            // Security Section
            item {
                SectionHeader(if (isBengali) "নিরাপত্তা" else "Security")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Fingerprint,
                            title = if (isBengali) "বায়োমেট্রিক লক" else "Biometric App Lock",
                            subtitle = if (isBengali) "আঙ্গুলের ছাপ বা ফেস আনলক" else "Fingerprint or face unlock",
                            trailingContent = {
                                Switch(
                                    checked = biometricLock,
                                    onCheckedChange = { biometricLock = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                                )
                            }
                        )
                    }
                }
            }

            // About & Help Section
            item {
                SectionHeader(if (isBengali) "অ্যাপ সম্পর্কে" else "About & Support")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = if (isBengali) "অ্যাপ সংস্করণ" else "App Version",
                            subtitle = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            trailingContent = {
                                if (updateCheckState is UpdateCheckState.Checking) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    TextButton(onClick = { profileViewModel.checkForUpdates() }) {
                                        Text(if (isBengali) "আপডেট চেক" else "Check", color = EmeraldPrimary)
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.NewReleases,
                            title = if (isBengali) "নতুন কী আছে (রিলিজ নোট)" else "What's New (Release Notes)",
                            subtitle = if (isBengali) "ভার্সন ${BuildConfig.VERSION_NAME} এর ফিচারসমূহ" else "Version ${BuildConfig.VERSION_NAME} update details",
                            onClick = { showReleaseNotesDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.HelpOutline,
                            title = if (isBengali) "সহায়তা ও যোগাযোগ" else "Help & Support",
                            subtitle = if (isBengali) "সাপোর্ট টিমের সাথে যোগাযোগ" else "Contact technical support",
                            onClick = { showHelpDialog = true }
                        )
                    }
                }
            }

            // Logout Button
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_logout_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "সাইন আউট করুন" else "Sign Out",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
