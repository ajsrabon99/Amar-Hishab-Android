package com.ajyra.amarhishab.model

data class User(
    val id: String = "",
    val email: String,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isGoogleUser: Boolean = false,
    val isVerified: Boolean = true
) {
    val name: String
        get() = displayName.ifBlank { username.ifBlank { email } }
}
