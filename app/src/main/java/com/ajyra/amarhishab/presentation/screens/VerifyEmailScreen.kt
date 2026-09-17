package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.R
import com.ajyra.amarhishab.presentation.viewmodel.AuthUiState
import com.ajyra.amarhishab.presentation.viewmodel.AuthViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreen(
    initialEmail: String = "",
    authViewModel: AuthViewModel,
    isBengali: Boolean,
    onVerificationSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var identifier by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableIntStateOf(0) }
    var isResending by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isLoading = uiState is AuthUiState.Loading

    // Cooldown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onVerificationSuccess()
        }
    }

    fun submitVerification() {
        validationError = null
        val trimmedIdentifier = identifier.trim()
        val trimmedCode = code.trim()

        if (trimmedIdentifier.isBlank()) {
            validationError = if (isBengali) "ইমেইল বা ইউজারনেম দিন।" else "Please enter your email or username."
            return
        }
        if (trimmedCode.length < 6) {
            validationError = if (isBengali) "৬ ডিজিটের ভেরিফিকেশন কোড লিখুন।" else "Please enter the full 6-digit verification code."
            return
        }

        focusManager.clearFocus()
        authViewModel.verifyEmail(
            identifier = trimmedIdentifier,
            code = trimmedCode
        ) { success, msg ->
            if (success) {
                onVerificationSuccess()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    fun handleResend() {
        if (resendCooldown > 0 || isResending) return
        val trimmedIdentifier = identifier.trim()
        if (trimmedIdentifier.isBlank()) {
            validationError = if (isBengali) "কোড পুনরায় পাঠাতে ইমেইল বা ইউজারনেম প্রয়োজন।" else "Please enter your email to resend the code."
            return
        }

        isResending = true
        authViewModel.resendVerificationCode(trimmedIdentifier) { success, msg ->
            isResending = false
            if (success) {
                resendCooldown = 60
                scope.launch {
                    val text = if (isBengali) "ভেরিফিকেশন কোড পুনরায় পাঠানো হয়েছে!" else "A new verification code has been sent!"
                    snackbarHostState.showSnackbar(text)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("verify_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Official Amar Hishab Brand Logo
            Image(
                painter = painterResource(id = R.drawable.amar_hishab_icon),
                contentDescription = "Amar Hishab Logo",
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isBengali) "ইমেইল যাচাইকরণ" else "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isBengali)
                    "আপনার ইমেইলে একটি ৬ ডিজিটের কোড পাঠানো হয়েছে। কোডটি নিচে লিখে নিশ্চিত করুন।"
                else
                    "A 6-digit verification code was sent to your email. Enter it below to complete setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            // Verify Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verify_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Local validation error banner
                    if (validationError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .testTag("verify_validation_error"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = validationError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { validationError = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Server-side error banner
                    if (uiState is AuthUiState.Error) {
                        val errorState = uiState as AuthUiState.Error
                        val errorMsg = if (isBengali) errorState.messageBn else errorState.messageEn
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .testTag("verify_server_error"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { authViewModel.clearError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Email/Identifier input
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            validationError = null
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = { Text(if (isBengali) "ইমেইল বা ইউজারনেম" else "Email or Username") },
                        placeholder = { Text("user@example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_identifier_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6-digit Code Input
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }.take(6)
                            code = digitsOnly
                            validationError = null
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                            if (digitsOnly.length == 6 && identifier.isNotBlank()) {
                                submitVerification()
                            }
                        },
                        label = { Text(if (isBengali) "৬ ডিজিটের কোড" else "6-Digit Code") },
                        placeholder = { Text("1 2 3 4 5 6") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitVerification() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_code_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    // Visual digit boxes representation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 0 until 6) {
                            val char = code.getOrNull(i)?.toString() ?: ""
                            val isCurrent = code.length == i
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (char.isNotEmpty()) EmeraldPrimary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) EmeraldPrimary
                                        else if (char.isNotEmpty()) EmeraldPrimary.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (char.isNotEmpty()) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = { submitVerification() },
                        enabled = !isLoading && identifier.isNotBlank() && code.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isBengali) "যাচাই সম্পন্ন করুন" else "Verify & Continue",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resend Code Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { handleResend() },
                            enabled = resendCooldown == 0 && !isResending && identifier.isNotBlank(),
                            modifier = Modifier.testTag("resend_code_button")
                        ) {
                            if (isResending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (resendCooldown == 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (resendCooldown > 0) {
                                    if (isBengali) "আবার পাঠান (${resendCooldown}s)" else "Resend in ${resendCooldown}s"
                                } else {
                                    if (isBengali) "কোড পুনরায় পাঠান" else "Resend Code"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (resendCooldown == 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Back to Login link
                    Text(
                        text = if (isBengali) "লগইন পেজে ফিরে যান" else "Back to Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clickable { onNavigateToLogin() }
                            .padding(8.dp)
                            .testTag("back_to_login_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
