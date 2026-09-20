package com.ajyra.amarhishab.model

import java.util.UUID

enum class NotificationType {
    SYSTEM,
    UPDATE,
    EXPENSE_REMINDER,
    MONTHLY_SUMMARY,
    TRANSACTION
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val titleEn: String,
    val titleBn: String,
    val messageEn: String,
    val messageBn: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType = NotificationType.SYSTEM,
    val isRead: Boolean = false,
    val actionUrl: String? = null
)
