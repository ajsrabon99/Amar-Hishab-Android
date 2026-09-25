package com.ajyra.amarhishab.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ajyra.amarhishab.R
import com.ajyra.amarhishab.model.AppNotification
import com.ajyra.amarhishab.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class NotificationRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        createNotificationChannel()
        loadNotifications()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Amar Hishab Updates & Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for expense reminders, monthly recaps, and app updates"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun loadNotifications() {
        val jsonString = prefs.getString(KEY_NOTIFICATIONS_JSON, null)
        val list = mutableListOf<AppNotification>()
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AppNotification(
                            id = obj.getString("id"),
                            titleEn = obj.getString("titleEn"),
                            titleBn = obj.getString("titleBn"),
                            messageEn = obj.getString("messageEn"),
                            messageBn = obj.getString("messageBn"),
                            timestamp = obj.getLong("timestamp"),
                            type = try {
                                NotificationType.valueOf(obj.getString("type"))
                            } catch (e: Exception) {
                                NotificationType.SYSTEM
                            },
                            isRead = obj.optBoolean("isRead", false),
                            actionUrl = obj.optString("actionUrl").takeIf { it.isNotBlank() }
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors, load default seed
            }
        }

        if (list.isEmpty()) {
            // Seed initial helpful notifications for user
            val initial = listOf(
                AppNotification(
                    id = "seed_1",
                    titleEn = "Welcome to Amar Hishab v1.0.0",
                    titleBn = "আমার হিসাব v১.০.০ এ স্বাগতম",
                    messageEn = "Your modern personal finance companion is ready. Track cash, bKash, Nagad, and bank transactions securely offline.",
                    messageBn = "আপনার আধুনিক ফিন্যান্স সঙ্গী এখন প্রস্তুত। ক্যাশ, বিকাশ, নগদ ও ব্যাংকের সব হিসাব নিরাপদে রাখুন।",
                    timestamp = System.currentTimeMillis() - 3600000L * 2,
                    type = NotificationType.SYSTEM,
                    isRead = false
                ),
                AppNotification(
                    id = "seed_2",
                    titleEn = "Daily Expense Reminder Enabled",
                    titleBn = "দৈনিক খরচ এন্ট্রি রিমাইন্ডার সক্রিয়",
                    messageEn = "Keep your finances accurate! Remember to record today's spending at the end of the day.",
                    messageBn = "প্রতিদিনের খরচ নিয়মিত যুক্ত করুন যাতে মাস শেষে সঠিক হিসাব পাওয়া যায়।",
                    timestamp = System.currentTimeMillis() - 3600000L * 24,
                    type = NotificationType.EXPENSE_REMINDER,
                    isRead = false
                )
            )
            list.addAll(initial)
            saveList(list)
        }

        _notifications.value = list
        _unreadCount.value = list.count { !it.isRead }
    }

    private fun saveList(list: List<AppNotification>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("titleEn", item.titleEn)
                put("titleBn", item.titleBn)
                put("messageEn", item.messageEn)
                put("messageBn", item.messageBn)
                put("timestamp", item.timestamp)
                put("type", item.type.name)
                put("isRead", item.isRead)
                put("actionUrl", item.actionUrl ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS_JSON, array.toString()).apply()
        _notifications.value = list
        _unreadCount.value = list.count { !it.isRead }
    }

    fun addNotification(notification: AppNotification, showSystemNotification: Boolean = true) {
        val current = _notifications.value.toMutableList()
        // Prevent duplicate IDs
        current.removeAll { it.id == notification.id }
        current.add(0, notification)
        saveList(current)

        if (showSystemNotification) {
            postSystemNotification(notification)
        }
    }

    fun markAsRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveList(updated)
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        saveList(updated)
    }

    fun clearAll() {
        saveList(emptyList())
    }

    fun deleteNotification(id: String) {
        val updated = _notifications.value.filter { it.id != id }
        saveList(updated)
    }

    fun postSystemNotification(notification: AppNotification) {
        try {
            val session = EncryptedSessionManager.getInstance(context)
            if (!session.areNotificationsEnabled()) return

            val isBengali = session.getSavedLanguage() == "bn"
            val title = if (isBengali) notification.titleBn else notification.titleEn
            val content = if (isBengali) notification.messageBn else notification.messageEn

            val intent = Intent(context, com.ajyra.amarhishab.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "settings")
                putExtra("section", "community")
                if (notification.actionUrl != null) {
                    data = android.net.Uri.parse(notification.actionUrl)
                }
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notification.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Permission not granted yet on Android 13+
        } catch (e: Exception) {
            // Ignore system notification errors
        }
    }

    fun triggerTelegramCommunityNotificationOnce() {
        val hasPrompted = prefs.getBoolean(KEY_PROMPTED_TELEGRAM, false)
        if (!hasPrompted) {
            prefs.edit().putBoolean(KEY_PROMPTED_TELEGRAM, true).apply()
            val notification = AppNotification(
                id = "telegram_community_invite",
                titleEn = "Join Amar Hishab Community",
                titleBn = "আমার হিসাব কমিউনিটিতে যুক্ত হন",
                messageEn = "Join our Telegram community for the latest updates & news.",
                messageBn = "সর্বশেষ আপডেট ও খবরের জন্য আমাদের টেলিগ্রাম কমিউনিটিতে যুক্ত হন।",
                timestamp = System.currentTimeMillis(),
                type = NotificationType.SYSTEM,
                isRead = false,
                actionUrl = "amarhishab://settings/community"
            )
            addNotification(notification, showSystemNotification = true)
        }
    }

    companion object {
        const val CHANNEL_ID = "amar_hishab_channel_general"
        private const val PREF_NAME = "amar_hishab_notifications"
        private const val KEY_NOTIFICATIONS_JSON = "notifications_json"
        private const val KEY_PROMPTED_TELEGRAM = "key_prompted_telegram_community"

        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
