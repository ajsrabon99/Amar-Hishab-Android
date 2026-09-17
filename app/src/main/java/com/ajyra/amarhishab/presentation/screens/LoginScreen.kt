package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.R
import com.ajyra.amarhishab.presentation.viewmodel.AuthUiState
import com.ajyra.amarhishab.presentation.viewmodel.AuthViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    isBengali: Boolean,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToVerifyEmail: (email: String) -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Dialog state for forgot password
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetStep by remember { mutableStateOf(1) }
    var resetCode by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onLoginSuccess()
            }
            is AuthUiState.Error -> {
                val msg = if (isBengali) state.messageBn else state.messageEn
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
            else -> {}
        }
    }

    val isLoading = uiState is AuthUiState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Official Amar Hishab Brand Logo
            Image(
                painter = painterResource(id = R.drawable.amar_hishab_icon),
                contentDescription = "Amar Hishab Logo",
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isBengali) "আমার হিসাব" else "Amar Hishab",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isBengali) "আপনার নিরাপদ আর্থিক হিসাব খাতা" else "Your Personal Financial Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isBengali) "সাইন ইন করুন" else "Sign In",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Text(
                        text = if (isBengali) "ইউজারনেম বা ইমেইল এবং পাসওয়ার্ড দিন" else "Enter your username/email and password",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 20.dp),
                        textAlign = TextAlign.Start
                    )

                    // Error banner
                    if (uiState is AuthUiState.Error) {
                        val errorState = uiState as AuthUiState.Error
                        val errorMsg = if (isBengali) errorState.messageBn else errorState.messageEn
                        val isEmailNotVerified = errorMsg.contains("verify", ignoreCase = true) ||
                                errorState.messageEn.contains("verified", ignoreCase = true) ||
                                errorState.messageEn.contains("VERIFIED", ignoreCase = true)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("error_banner"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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

                                if (isEmailNotVerified) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            authViewModel.clearError()
                                            onNavigateToVerifyEmail(identifier.trim())
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(
                                            text = if (isBengali) "ইমেইল ভেরিফাই করুন →" else "Verify Email Now →",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Identifier input
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = { Text(if (isBengali) "ইউজারনেম বা ইমেইল" else "Username or Email") },
                        placeholder = { Text(if (isBengali) "যেমন: srabon বা user@example.com" else "e.g. srabon or user@example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
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
                            .testTag("identifier_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = { Text(if (isBengali) "পাসওয়ার্ড" else "Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isLoading && identifier.isNotBlank() && password.isNotBlank()) {
                                    authViewModel.loginWithPassword(identifier, password, onLoginSuccess)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isBengali) "পাসওয়ার্ড ভুলে গেছেন?" else "Forgot Password?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clickable {
                                    resetEmail = if (identifier.contains("@")) identifier else ""
                                    resetStep = 1
                                    showResetPasswordDialog = true
                                }
                                .padding(vertical = 4.dp)
                                .testTag("forgot_password_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.loginWithPassword(identifier, password, onLoginSuccess)
                        },
                        enabled = !isLoading && identifier.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
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
                                text = if (isBengali) "সাইন ইন" else "Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action links: Register & Verify Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onNavigateToRegister,
                            modifier = Modifier.testTag("register_button")
                        ) {
                            Text(
                                text = if (isBengali) "নতুন অ্যাকাউন্ট?" else "Create Account",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        TextButton(
                            onClick = {
                                onNavigateToVerifyEmail(identifier.trim())
                            },
                            modifier = Modifier.testTag("verify_code_button")
                        ) {
                            Text(
                                text = if (isBengali) "ইমেইল ভেরিফাই কোড" else "Verify Code",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBengali)
                        "নিরাপদ ও এনক্রিপ্ট করা হিসাব রক্ষণাবেক্ষণ"
                    else
                        "Secure encrypted financial tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // ================= FORGOT PASSWORD DIALOG =================
    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            title = {
                Text(
                    text = if (isBengali) "পাসওয়ার্ড রিসেট" else "Reset Password",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (resetStep == 1) {
                        Text(
                            text = if (isBengali)
                                "আপনার নিবন্ধিত ইমেইল ঠিকানা দিন:"
                            else
                                "Enter your registered email address:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text(if (isBengali) "ইমেইল" else "Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = if (isBengali)
                                "ইমেইলে পাঠানো কোড ও নতুন পাসওয়ার্ড দিন:"
                            else
                                "Enter the code sent to your email and your new password:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = resetCode,
                            onValueChange = { if (it.length <= 6) resetCode = it },
                            label = { Text(if (isBengali) "৬ ডিজিটের কোড" else "6-Digit Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = resetNewPassword,
                            onValueChange = { resetNewPassword = it },
                            label = { Text(if (isBengali) "নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)" else "New Password (min 8 chars)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetStep == 1) {
                            if (resetEmail.isNotBlank()) {
                                authViewModel.requestPasswordReset(resetEmail) { success, msg ->
                                    if (success) {
                                        resetStep = 2
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        } else {
                            if (resetEmail.isNotBlank() && resetCode.isNotBlank() && resetNewPassword.isNotBlank()) {
                                authViewModel.confirmPasswordReset(resetEmail, resetCode, resetNewPassword) { success, msg ->
                                    if (success) {
                                        showResetPasswordDialog = false
                                        identifier = resetEmail
                                        password = resetNewPassword
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(
                        if (resetStep == 1) {
                            if (isBengali) "কোড পাঠান" else "Send Code"
                        } else {
                            if (isBengali) "পাসওয়ার্ড আপডেট করুন" else "Update Password"
                        }
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetPasswordDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
