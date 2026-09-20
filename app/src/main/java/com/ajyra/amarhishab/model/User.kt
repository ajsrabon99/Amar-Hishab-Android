package com.ajyra.amarhishab.model

data class User(
    val id: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val avatarUri: String? = null,
    val isVerified: Boolean = true
) {
    val name: String
        get() = displayName.ifBlank { username.ifBlank { if (email.isNotBlank()) email else "User" } }
}
