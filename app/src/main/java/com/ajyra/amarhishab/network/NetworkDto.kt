package com.ajyra.amarhishab.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "token") val token: String? = null,
    @Json(name = "key") val key: String? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthExchangeRequestDto(
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class AuthExchangeResponseDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "key") val key: String? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequestDto(
    @Json(name = "id_token") val idToken: String
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email") val email: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountBalancesDto(
    @Json(name = "cash") val cash: Double? = 0.0,
    @Json(name = "bkash") val bkash: Double? = 0.0,
    @Json(name = "nagad") val nagad: Double? = 0.0,
    @Json(name = "bank") val bank: Double? = 0.0,
    @Json(name = "total") val total: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String,
    @Json(name = "account") val account: String,
    @Json(name = "to_account") val toAccount: String? = null,
    @Json(name = "date") val date: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DashboardResponseDto(
    @Json(name = "balances") val balances: AccountBalancesDto? = null,
    @Json(name = "monthly_income") val monthlyIncome: Double? = 0.0,
    @Json(name = "monthly_expense") val monthlyExpense: Double? = 0.0,
    @Json(name = "monthly_savings") val monthlySavings: Double? = 0.0,
    @Json(name = "savings_rate") val savingsRate: Double? = 0.0,
    @Json(name = "recent_transactions") val recentTransactions: List<TransactionDto>? = null
)

@JsonClass(generateAdapter = true)
data class CreateTransactionRequestDto(
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String,
    @Json(name = "account") val account: String,
    @Json(name = "to_account") val toAccount: String? = null,
    @Json(name = "date") val date: String,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class TransferRequestDto(
    @Json(name = "from_account") val fromAccount: String,
    @Json(name = "to_account") val toAccount: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class AppVersionResponseDto(
    @Json(name = "latest_version") val latestVersion: String,
    @Json(name = "latest_version_code") val latestVersionCode: Int,
    @Json(name = "minimum_supported_version") val minSupportedVersion: String,
    @Json(name = "update_required") val updateRequired: Boolean,
    @Json(name = "release_notes") val releaseNotes: String,
    @Json(name = "download_url") val downloadUrl: String
)
