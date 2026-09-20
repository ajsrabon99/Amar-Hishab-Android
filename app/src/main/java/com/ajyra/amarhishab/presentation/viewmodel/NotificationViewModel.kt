package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.ajyra.amarhishab.data.local.NotificationRepository
import com.ajyra.amarhishab.model.AppNotification
import kotlinx.coroutines.flow.StateFlow

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.notifications
    val unreadCount: StateFlow<Int> = notificationRepository.unreadCount

    fun markAsRead(id: String) {
        notificationRepository.markAsRead(id)
    }

    fun markAllAsRead() {
        notificationRepository.markAllAsRead()
    }

    fun clearAll() {
        notificationRepository.clearAll()
    }

    fun deleteNotification(id: String) {
        notificationRepository.deleteNotification(id)
    }
}
