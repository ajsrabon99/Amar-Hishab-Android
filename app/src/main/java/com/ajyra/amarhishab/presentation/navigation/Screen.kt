package com.ajyra.amarhishab.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleEn: String,
    val titleBn: String,
    val icon: ImageVector? = null
) {
    object Login : Screen("login", "Login", "লগইন", null)
    object Register : Screen("register", "Register", "রেজিস্ট্রেশন", null)
    object VerifyEmail : Screen("verify_email?email={email}", "Verify Email", "ইমেইল ভেরিফাই", null) {
        fun createRoute(email: String = ""): String = if (email.isNotBlank()) "verify_email?email=$email" else "verify_email"
    }
    object Home : Screen("home", "Home", "হোম", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", "লেনদেন", Icons.Default.ReceiptLong)
    object Reports : Screen("reports", "Reports", "রিপোর্ট", Icons.Default.Assessment)
    object Profile : Screen("profile", "Profile", "প্রোফাইল", Icons.Default.Person)

    object AddTransaction : Screen("add_transaction/{isExpense}", "Add", "যুক্ত করুন", null) {
        fun createRoute(isExpense: Boolean): String = "add_transaction/$isExpense"
    }

    object Transfer : Screen("transfer", "Transfer", "ট্রান্সফার", null)
}
