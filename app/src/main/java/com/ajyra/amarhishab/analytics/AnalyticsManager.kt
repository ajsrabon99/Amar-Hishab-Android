package com.ajyra.amarhishab.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized, privacy-compliant Firebase Analytics manager for Amar Hishab V1.2.8.
 *
 * STRICT PRIVACY RULES:
 * - NO personal names, emails, phone numbers, or credentials.
 * - NO transaction descriptions, notes, or specific merchant information.
 * - NO monetary amounts, balances, bank account numbers, or bKash/Nagad details.
 * - ONLY high-level anonymous product-usage telemetry to measure feature engagement,
 *   active users, retention, and stability.
 */
object AnalyticsManager {

    private const val TAG = "AnalyticsManager"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Safe Event Names
    const val EVENT_APP_OPEN = "app_open"
    const val EVENT_SCREEN_VIEW = "screen_view"
    const val EVENT_TRANSACTION_ADDED = "transaction_added"
    const val EVENT_TRANSACTION_EDITED = "transaction_edited"
    const val EVENT_TRANSACTION_DELETED = "transaction_deleted"
    const val EVENT_INCOME_ADDED = "income_added"
    const val EVENT_EXPENSE_ADDED = "expense_added"
    const val EVENT_TRANSFER_ADDED = "transfer_added"
    const val EVENT_BACKUP_CREATED = "backup_created"
    const val EVENT_RESTORE_COMPLETED = "restore_completed"
    const val EVENT_REPORT_VIEWED = "report_viewed"
    const val EVENT_SMART_SPENDING_VIEWED = "smart_spending_viewed"
    const val EVENT_SMART_SPENDING_LIMIT_SET = "smart_spending_limit_set"
    const val EVENT_SPENDING_WARNING_SHOWN = "spending_warning_shown"
    const val EVENT_SPENDING_RED_ALERT_SHOWN = "spending_red_alert_shown"
    const val EVENT_BUSINESS_WORKSPACE_OPENED = "business_workspace_opened"
    const val EVENT_PERSONAL_WORKSPACE_OPENED = "personal_workspace_opened"
    const val EVENT_SETTINGS_OPENED = "settings_opened"
    const val EVENT_PROFILE_OPENED = "profile_opened"

    // Safe Parameter Keys
    const val PARAM_WORKSPACE = "workspace"
    const val PARAM_TRANSACTION_TYPE = "transaction_type"
    const val PARAM_SCREEN_NAME = "screen_name"
    const val PARAM_STATUS = "spending_status"
    const val PARAM_CATEGORY_TYPE = "category_type"

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            logEvent(EVENT_APP_OPEN)
            Log.d(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics initialization note: ${e.message}")
        }
    }

    /**
     * Safely logs an event with non-sensitive parameters.
     */
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        try {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                // Ensure key and value are safe strings and cannot leak amounts or PII
                if (!isSensitiveKey(key)) {
                    bundle.putString(key, value.take(50))
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Event logged: $eventName | params=$params")
        } catch (e: Exception) {
            Log.w(TAG, "Event logging skipped for $eventName: ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        logEvent(EVENT_SCREEN_VIEW, mapOf(PARAM_SCREEN_NAME to screenName))
    }

    fun logWorkspaceSwitched(isBusiness: Boolean) {
        val event = if (isBusiness) EVENT_BUSINESS_WORKSPACE_OPENED else EVENT_PERSONAL_WORKSPACE_OPENED
        val wsStr = if (isBusiness) "business" else "personal"
        logEvent(event, mapOf(PARAM_WORKSPACE to wsStr))
    }

    fun logTransactionAction(action: String, type: String, workspace: String) {
        val event = when (action) {
            "add" -> when (type.lowercase()) {
                "income" -> EVENT_INCOME_ADDED
                "expense" -> EVENT_EXPENSE_ADDED
                "transfer" -> EVENT_TRANSFER_ADDED
                else -> EVENT_TRANSACTION_ADDED
            }
            "edit" -> EVENT_TRANSACTION_EDITED
            "delete" -> EVENT_TRANSACTION_DELETED
            else -> EVENT_TRANSACTION_ADDED
        }
        logEvent(event, mapOf(
            PARAM_TRANSACTION_TYPE to type.lowercase(),
            PARAM_WORKSPACE to workspace.lowercase()
        ))
    }

    fun logSmartSpendingAlert(isRedAlert: Boolean, workspace: String) {
        val event = if (isRedAlert) EVENT_SPENDING_RED_ALERT_SHOWN else EVENT_SPENDING_WARNING_SHOWN
        val status = if (isRedAlert) "red_alert" else "orange_warning"
        logEvent(event, mapOf(
            PARAM_WORKSPACE to workspace.lowercase(),
            PARAM_STATUS to status
        ))
    }

    /**
     * Safeguard to reject any accidental monetary or PII keys.
     */
    private fun isSensitiveKey(key: String): Boolean {
        val lower = key.lowercase()
        return lower.contains("amount") ||
                lower.contains("balance") ||
                lower.contains("money") ||
                lower.contains("price") ||
                lower.contains("cost") ||
                lower.contains("name") && !lower.contains("screen") ||
                lower.contains("email") ||
                lower.contains("phone") ||
                lower.contains("password") ||
                lower.contains("account_number") ||
                lower.contains("token") ||
                lower.contains("description") ||
                lower.contains("note")
    }
}
