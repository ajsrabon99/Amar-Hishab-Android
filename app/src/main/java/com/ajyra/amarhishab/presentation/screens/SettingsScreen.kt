package com.ajyra.amarhishab.presentation.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import com.ajyra.amarhishab.presentation.components.AmarSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.ajyra.amarhishab.BuildConfig
import com.ajyra.amarhishab.R
import com.ajyra.amarhishab.data.local.ReleaseNotesRepository
import com.ajyra.amarhishab.data.repository.GitHubUpdateRepository
import com.ajyra.amarhishab.model.AppUpdateInfo
import com.ajyra.amarhishab.presentation.viewmodel.SettingsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.UpdateCheckState
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.FintechPrimary
import com.ajyra.amarhishab.utils.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val dailyReminder by viewModel.expenseReminders.collectAsState()
    val monthlyRecap by viewModel.monthlySummary.collectAsState()
    val biometricLock by viewModel.appLock.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Theme selector dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = if (isBengali) "থিম নির্বাচন করুন" else "Choose Theme",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf(
                        Triple("system", if (isBengali) "সিস্টেম ডিফল্ট" else "System Default", Icons.Default.Brightness4),
                        Triple("light", if (isBengali) "লাইট থিম" else "Light Mode", Icons.Default.LightMode),
                        Triple("dark", if (isBengali) "ডার্ক থিম" else "Dark Mode", Icons.Default.DarkMode)
                    ).forEach { (mode, label, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.setTheme(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = icon, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Release Notes dialog
    if (showReleaseNotesDialog) {
        val notes = ReleaseNotesRepository.releaseNotes
        AlertDialog(
            onDismissRequest = { showReleaseNotesDialog = false },
            title = {
                Text(
                    text = if (isBengali) "রিলিজ নোটস" else "Release Notes",
                    fontWeight = FontWeight.Bold
                )
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

    // Help & Support dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = if (isBengali) "সহায়তা ও তথ্য" else "Help & Support",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBengali)
                            "আমার হিসাব একটি সম্পূর্ণ অফলাইন-সক্ষম, ব্যক্তিগত আর্থিক হিসাব ব্যবস্থাপনা অ্যাপ্লিকেশন।"
                        else
                            "Amar Hishab is a complete offline-first personal finance tracking application."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Developer: AJ SRABON\nEmail: ashrafuzzamansrabon@gmail.com\nGitHub: github.com/${GitHubUpdateRepository.GITHUB_OWNER}/${GitHubUpdateRepository.GITHUB_REPO}",
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

    // Update Result Dialog
    when (val state = updateCheckState) {
        is UpdateCheckState.UpdateAvailable -> {
            val info = state.info
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                icon = {
                    Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = FintechPrimary)
                },
                title = {
                    Text(
                        text = if (isBengali) "নতুন আপডেট উপলব্ধ! (v${info.latestVersion})" else "Update Available! (v${info.latestVersion})",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (isBengali)
                                "আমার হিসাব v${info.latestVersion} সংস্করণ প্রস্তুত হয়েছে।"
                            else
                                "Amar Hishab v${info.latestVersion} is now available."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isBengali) "পরিবর্তনসমূহ:" else "What's New:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBengali)
                                "• ডাউনলোড সম্পন্ন হলে অ্যান্ড্রয়েড প্যাকেজ ইনস্টলার ইনস্টলেশন নিশ্চিত করতে বলবে।"
                            else
                                "• Android will prompt you to confirm download and package installation.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissUpdateDialog()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isBengali) "ডাউনলোড করুন (APK)" else "Download APK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(if (isBengali) "পরে" else "Later")
                    }
                }
            )
        }
        is UpdateCheckState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = {
                    Text(
                        text = if (isBengali) "অ্যাপ আপ-টু-ডেট" else "App is Up to Date",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (isBengali)
                                "আপনি আমার হিসাব এর সর্বশেষ সংস্করণ (v${state.versionName}) ব্যবহার করছেন।"
                            else
                                "You are currently running the latest version of Amar Hishab (v${state.versionName})."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "GitHub Releases: github.com/ajsrabon/amar-hishab",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(if (isBengali) "ঠিক আছে" else "OK")
                    }
                }
            )
        }
        is UpdateCheckState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = {
                    Text(
                        text = if (isBengali) "আপডেট চেক ব্যর্থ হয়েছে" else "Update Check Failed",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(text = if (isBengali) state.messageBn else state.messageEn)
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(if (isBengali) "ঠিক আছে" else "OK")
                    }
                }
            )
        }
        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "সেটিংস" else "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isBengali) "পেছনে যান" else "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen_scroll"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            item {
                SettingsSectionHeader(if (isBengali) "অ্যাপের রূপ (Appearance)" else "Appearance")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsOptionRow(
                            icon = when (themeMode) {
                                "dark" -> Icons.Default.DarkMode
                                "light" -> Icons.Default.LightMode
                                else -> Icons.Default.DarkMode
                            },
                            title = if (isBengali) "থিম" else "Theme",
                            subtitle = when (themeMode) {
                                "dark" -> if (isBengali) "ডার্ক মোড" else "Dark Mode"
                                "light" -> if (isBengali) "লাইট মোড" else "Light Mode"
                                else -> if (isBengali) "সিস্টেম ডিফল্ট" else "System Default"
                            },
                            onClick = { showThemeDialog = true }
                        )
                    }
                }
            }

            // Language Section
            item {
                SettingsSectionHeader(if (isBengali) "ভাষা (Language)" else "Language")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SettingsOptionRow(
                        icon = Icons.Default.Language,
                        title = if (isBengali) "অ্যাপের ভাষা" else "App Language",
                        subtitle = if (isBengali) "বর্তমান ভাষা: বাংলা" else "Current: English",
                        trailingContent = {
                            Button(
                                onClick = { viewModel.setLanguage(if (isBengali) "en" else "bn") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text(if (isBengali) "English" else "বাংলা")
                            }
                        }
                    )
                }
            }

            // Notifications Section
            item {
                SettingsSectionHeader(if (isBengali) "বিজ্ঞপ্তি ও রিমাইন্ডার" else "Notifications & Reminders")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsOptionRow(
                            icon = Icons.Default.NotificationsActive,
                            title = if (isBengali) "সব বিজ্ঞপ্তি" else "Push Notifications",
                            subtitle = if (isBengali) "সিস্টেম ও আপডেট বিজ্ঞপ্তি পান" else "Allow in-app and system alerts",
                            trailingContent = {
                                AmarSwitch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { viewModel.toggleNotifications(it) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            icon = Icons.Default.Alarm,
                            title = if (isBengali) "দৈনিক খরচ এন্ট্রি রিমাইন্ডার" else "Daily Expense Reminder",
                            subtitle = if (isBengali) "প্রতিদিন রাত ৯টায় রিমাইন্ডার" else "Daily reminder at 9:00 PM",
                            trailingContent = {
                                AmarSwitch(
                                    checked = dailyReminder,
                                    onCheckedChange = { viewModel.toggleExpenseReminders(it) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            icon = Icons.Default.Assessment,
                            title = if (isBengali) "মাসিক সারসংক্ষেপ রিপোর্ট" else "Monthly Summary Report",
                            subtitle = if (isBengali) "মাসের শুরুতে আগের মাসের হিসাব" else "Monthly financial recap",
                            trailingContent = {
                                AmarSwitch(
                                    checked = monthlyRecap,
                                    onCheckedChange = { viewModel.toggleMonthlySummary(it) }
                                )
                            }
                        )
                    }
                }
            }

            // Security & Privacy Section
            item {
                SettingsSectionHeader(if (isBengali) "নিরাপত্তা ও গোপনীয়তা" else "Security & Privacy")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SettingsOptionRow(
                        icon = Icons.Default.Fingerprint,
                        title = if (isBengali) "বায়োমেট্রিক অ্যাপ লক" else "Biometric App Lock",
                        subtitle = if (isBengali) "অ্যাপ খোলার সময় ফিঙ্গারপ্রিন্ট বা ডিভাইস লক চাইবে" else "Require fingerprint or PIN to open app",
                        trailingContent = {
                            AmarSwitch(
                                checked = biometricLock,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (BiometricHelper.isBiometricAvailable(context)) {
                                            val activity = context as? FragmentActivity
                                            if (activity != null) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = activity,
                                                    title = if (isBengali) "বায়োমেট্রিক লক নিশ্চিতকরণ" else "Confirm Biometric Lock",
                                                    subtitle = if (isBengali) "অ্যাপ সুরক্ষায় বায়োমেট্রিক সক্রিয় করতে প্রমাণীকরণ করুন" else "Authenticate to enable local app privacy lock",
                                                    onSuccess = {
                                                        viewModel.toggleAppLock(true)
                                                    },
                                                    onError = {}
                                                )
                                            } else {
                                                viewModel.toggleAppLock(true)
                                            }
                                        } else {
                                            viewModel.toggleAppLock(true)
                                        }
                                    } else {
                                        viewModel.toggleAppLock(false)
                                    }
                                }
                            )
                        }
                    )
                }
            }

            // App Updates Section
            item {
                SettingsSectionHeader(if (isBengali) "অ্যাপ আপডেট" else "App Updates")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsOptionRow(
                            icon = Icons.Default.SystemUpdate,
                            title = if (isBengali) "আপডেট চেক করুন (GitHub)" else "Check for Updates (GitHub)",
                            subtitle = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            trailingContent = {
                                if (updateCheckState is UpdateCheckState.Checking) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    TextButton(onClick = { viewModel.checkForUpdates() }) {
                                        Text(if (isBengali) "চেক করুন" else "Check Now", color = EmeraldPrimary)
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            icon = Icons.Default.NewReleases,
                            title = if (isBengali) "রিলিজ নোটস" else "Release Notes",
                            subtitle = if (isBengali) "ভার্সন ${BuildConfig.VERSION_NAME} এর নতুন ফিচারসমূহ" else "See what is new in version ${BuildConfig.VERSION_NAME}",
                            onClick = { showReleaseNotesDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            icon = Icons.Default.NotificationsActive,
                            title = if (isBengali) "আপডেট ডায়ালগ ও নোটিফিকেশন টেস্ট" else "Test Update Dialog & Flow",
                            subtitle = if (isBengali) "ইন-অ্যাপ বিজ্ঞপ্তি ও ডাউনলোড ফ্লো পরীক্ষা করুন" else "Preview update dialog & trigger notification",
                            onClick = { viewModel.testUpdateNotificationFlow() }
                        )
                    }
                }
            }

            // About & Developer Section
            item {
                SettingsSectionHeader(if (isBengali) "ডেভেলপার ও সহায়তা" else "About & Developer")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsOptionRow(
                            icon = Icons.Default.Person,
                            title = if (isBengali) "ডেভেলপার" else "Developer",
                            subtitle = "AJ SRABON",
                            onClick = null
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            icon = Icons.Default.Email,
                            title = if (isBengali) "সাপোর্ট ইমেইল" else "Support Email",
                            subtitle = "ashrafuzzamansrabon@gmail.com",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:ashrafuzzamansrabon@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Amar Hishab App Support")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:ashrafuzzamansrabon@gmail.com"))
                                        context.startActivity(fallback)
                                    } catch (e2: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            painter = painterResource(id = R.drawable.ic_facebook),
                            iconTint = Color.Unspecified,
                            title = "Facebook Profile",
                            subtitle = "Connect with AJ SRABON on Facebook",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/ashrafuzzaman.srabon")).apply {
                                        setPackage("com.facebook.katana")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/ashrafuzzaman.srabon"))
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        // Fallback
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        SettingsOptionRow(
                            painter = painterResource(id = R.drawable.ic_instagram),
                            iconTint = Color.Unspecified,
                            title = "Instagram Profile",
                            subtitle = "Follow AJ SRABON on Instagram",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/aj_srabon_")).apply {
                                        setPackage("com.instagram.android")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/aj_srabon_"))
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        // Fallback
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector? = null,
    painter: Painter? = null,
    iconTint: Color? = null,
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
                .size(40.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = iconTint ?: EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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
