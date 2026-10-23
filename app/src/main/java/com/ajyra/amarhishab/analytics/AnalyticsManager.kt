package com.ajyra.amarhishab.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized, production-grade Firebase Analytics manager for Amar Hishab V1.2.9.
 *
 * PRIVACY RULES (CRITICAL):
 * - Strictly NO transaction amounts, balances, currency figures, or numeric money values.
 * - Strictly NO transaction descriptions, merchant names, notes, or category custom descriptions.
 * - Strictly NO user names, email addresses, phone numbers, account numbers, or credentials.
 * - Strictly NO custom user IDs or device tracking parameters.
 * - Allowed parameters ONLY: "transaction_type", "workspace", "screen_name".
 */
object AnalyticsManager {

    private const val TAG = "AnalyticsManager"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Safe Event Names requested by V1.2.9
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

    // Safe Parameter Keys (Alphanumeric and underscores only, <= 40 chars)
    const val PARAM_WORKSPACE = "workspace"
    const val PARAM_TRANSACTION_TYPE = "transaction_type"
    const val PARAM_SCREEN_NAME = "screen_name"

    fun initialize(context: Context) {
        try {
            val analytics = FirebaseAnalytics.getInstance(context)
            analytics.setAnalyticsCollectionEnabled(true)
            firebaseAnalytics = analytics
            logEvent(EVENT_APP_OPEN)
            if (com.ajyra.amarhishab.BuildConfig.DEBUG) {
                Log.d(TAG, "Firebase Analytics initialized successfully")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics initialization warning: ${e.message}")
        }
    }

    /**
     * Safely logs an event with sanitized non-PII parameters.
     */
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        try {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                if (!isSensitiveKey(key) && isAllowedParameter(key)) {
                    bundle.putString(key, value.take(50))
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Logged event: $eventName | params=$params")
        } catch (e: Exception) {
            Log.w(TAG, "Event logging skipped for $eventName: ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName.take(50))
                putString(PARAM_SCREEN_NAME, screenName.take(50))
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Screen view logged: $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log screen view for $screenName: ${e.message}")
        }
    }

    fun logWorkspaceSwitched(isBusiness: Boolean) {
        val event = if (isBusiness) EVENT_BUSINESS_WORKSPACE_OPENED else EVENT_PERSONAL_WORKSPACE_OPENED
        val wsStr = if (isBusiness) "business" else "personal"
        logEvent(event, mapOf(PARAM_WORKSPACE to wsStr))
    }

    fun logTransactionAction(action: String, type: String, workspace: String) {
        val cleanType = type.lowercase()
        val cleanWs = workspace.lowercase()

        when (action.lowercase()) {
            "add" -> {
                // Primary transaction added event
                logEvent(EVENT_TRANSACTION_ADDED, mapOf(
                    PARAM_TRANSACTION_TYPE to cleanType,
                    PARAM_WORKSPACE to cleanWs
                ))
                // Specific type event
                when (cleanType) {
                    "income" -> logEvent(EVENT_INCOME_ADDED, mapOf(
                        PARAM_TRANSACTION_TYPE to "income",
                        PARAM_WORKSPACE to cleanWs
                    ))
                    "expense" -> logEvent(EVENT_EXPENSE_ADDED, mapOf(
                        PARAM_TRANSACTION_TYPE to "expense",
                        PARAM_WORKSPACE to cleanWs
                    ))
                    "transfer" -> logEvent(EVENT_TRANSFER_ADDED, mapOf(
                        PARAM_TRANSACTION_TYPE to "transfer",
                        PARAM_WORKSPACE to cleanWs
                    ))
                }
            }
            "edit" -> {
                logEvent(EVENT_TRANSACTION_EDITED, mapOf(
                    PARAM_TRANSACTION_TYPE to cleanType,
                    PARAM_WORKSPACE to cleanWs
                ))
            }
            "delete" -> {
                logEvent(EVENT_TRANSACTION_DELETED, mapOf(
                    PARAM_TRANSACTION_TYPE to cleanType,
                    PARAM_WORKSPACE to cleanWs
                ))
            }
        }
    }

    fun logSmartSpendingAlert(isRedAlert: Boolean, workspace: String) {
        val event = if (isRedAlert) EVENT_SPENDING_RED_ALERT_SHOWN else EVENT_SPENDING_WARNING_SHOWN
        logEvent(event, mapOf(PARAM_WORKSPACE to workspace.lowercase()))
    }

    private fun isAllowedParameter(key: String): Boolean {
        return key == PARAM_WORKSPACE || key == PARAM_TRANSACTION_TYPE || key == PARAM_SCREEN_NAME || key == FirebaseAnalytics.Param.SCREEN_NAME
    }

    /**
     * Strict defense against any sensitive or personal information.
     */
    private fun isSensitiveKey(key: String): Boolean {
        val lower = key.lowercase()
        return lower.contains("amount") ||
                lower.contains("balance") ||
                lower.contains("money") ||
                lower.contains("price") ||
                lower.contains("cost") ||
                lower.contains("user") ||
                lower.contains("email") ||
                lower.contains("phone") ||
                lower.contains("password") ||
                lower.contains("account_number") ||
                lower.contains("token") ||
                lower.contains("description") ||
                lower.contains("note")
    }
}
