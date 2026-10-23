package com.ajyra.amarhishab.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ajyra.amarhishab.analytics.AnalyticsManager
import com.ajyra.amarhishab.model.AppNotification
import com.ajyra.amarhishab.model.CategorySpendingItem
import com.ajyra.amarhishab.model.DailySpendingRecord
import com.ajyra.amarhishab.model.DailySpendingStatus
import com.ajyra.amarhishab.model.NotificationType
import com.ajyra.amarhishab.model.SmartSpendingSummary
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.model.Workspace
import com.ajyra.amarhishab.utils.CurrencyFormatter
import com.ajyra.amarhishab.utils.DateUtils
import java.util.Calendar

class SpendingPlannerManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private val notificationRepo: NotificationRepository by lazy {
        NotificationRepository(context)
    }

    fun isLimitConfigured(workspace: Workspace): Boolean {
        val key = when (workspace) {
            Workspace.PERSONAL -> KEY_LIMIT_PERSONAL
            Workspace.BUSINESS -> KEY_LIMIT_BUSINESS
        }
        return prefs.contains(key) && (prefs.getString(key, null)?.toDoubleOrNull() ?: 0.0) > 0.0
    }

    fun getMonthlyLimit(workspace: Workspace): Double {
        val key = when (workspace) {
            Workspace.PERSONAL -> KEY_LIMIT_PERSONAL
            Workspace.BUSINESS -> KEY_LIMIT_BUSINESS
        }
        return prefs.getString(key, "0.0")?.toDoubleOrNull() ?: 0.0
    }

    fun calculateSuggestedMonthlyLimit(allTransactions: List<Transaction>, workspace: Workspace): Double {
        val wsId = workspace.id
        // Exclude transfers strictly, include only actual expense transactions in active workspace
        val expenseTxs = allTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                    (tx.workspace.equals(wsId, ignoreCase = true) || tx.workspace.equals("ALL", ignoreCase = true)) &&
                    tx.toAccountCode == null
        }
        if (expenseTxs.isEmpty()) return 0.0

        val monthMap = mutableMapOf<String, Double>()
        for (tx in expenseTxs) {
            val ym = if (tx.date.length >= 7) tx.date.substring(0, 7) else ""
            if (ym.isNotEmpty()) {
                monthMap[ym] = (monthMap[ym] ?: 0.0) + tx.amount
            }
        }
        val totalSpentAllMonths = expenseTxs.sumOf { it.amount }
        val monthCount = monthMap.size.coerceAtLeast(1)
        val avgMonthlyExpense = totalSpentAllMonths / monthCount

        if (avgMonthlyExpense <= 0.0) return 0.0

        // Round up sensibly to nearest 500
        val rounded = Math.ceil(avgMonthlyExpense / 500.0) * 500.0
        return rounded.coerceAtLeast(1000.0)
    }

    fun setMonthlyLimit(workspace: Workspace, limit: Double) {
        val key = when (workspace) {
            Workspace.PERSONAL -> KEY_LIMIT_PERSONAL
            Workspace.BUSINESS -> KEY_LIMIT_BUSINESS
        }
        prefs.edit().putString(key, limit.toString()).apply()
        AnalyticsManager.logEvent(
            AnalyticsManager.EVENT_SMART_SPENDING_LIMIT_SET,
            mapOf(AnalyticsManager.PARAM_WORKSPACE to workspace.id.lowercase())
        )
    }

    fun isRecurringLimitEnabled(workspace: Workspace): Boolean {
        val key = when (workspace) {
            Workspace.PERSONAL -> KEY_RECURRING_PERSONAL
            Workspace.BUSINESS -> KEY_RECURRING_BUSINESS
        }
        return prefs.getBoolean(key, true)
    }

    fun setRecurringLimitEnabled(workspace: Workspace, enabled: Boolean) {
        val key = when (workspace) {
            Workspace.PERSONAL -> KEY_RECURRING_PERSONAL
            Workspace.BUSINESS -> KEY_RECURRING_BUSINESS
        }
        prefs.edit().putBoolean(key, enabled).apply()
    }

    /**
     * Dynamically computes the full Smart Spending Summary from active transactions.
     */
    fun computeSmartSpending(
        allTransactions: List<Transaction>,
        workspace: Workspace,
        isBengali: Boolean
    ): SmartSpendingSummary {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val curYearMonthPrefix = String.format("%04d-%02d", curYear, curMonth)
        val todayStr = DateUtils.todayDateString()

        // 1. Filter strictly EXPENSE transactions for the current calendar month and active workspace
        val monthExpenses = allTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                    (tx.workspace.equals(workspace.id, ignoreCase = true) || tx.workspace.equals("ALL", ignoreCase = true)) &&
                    tx.toAccountCode == null &&
                    tx.date.startsWith(curYearMonthPrefix)
        }

        val isConfigured = isLimitConfigured(workspace)
        val monthlyLimit = getMonthlyLimit(workspace)
        val suggestedLimit = if (!isConfigured) calculateSuggestedMonthlyLimit(allTransactions, workspace) else 0.0

        val totalSpent = monthExpenses.sumOf { it.amount }
        val remainingBudget = if (isConfigured && monthlyLimit > 0.0) (monthlyLimit - totalSpent).coerceAtLeast(0.0) else 0.0
        val percentageUsed = if (isConfigured && monthlyLimit > 0.0) ((totalSpent / monthlyLimit) * 100.0).coerceIn(0.0, 100.0) else 0.0

        // Remaining days including today
        val remainingDays = (totalDaysInMonth - currentDayOfMonth + 1).coerceAtLeast(1)

        // Recommended Daily Spending = Remaining Monthly Budget / Remaining Days (only if user set limit)
        val recommendedDailySpending = if (isConfigured && remainingDays > 0 && remainingBudget > 0.0) {
            remainingBudget / remainingDays
        } else {
            0.0
        }

        // 2. Today's actual expense
        val todayExpenses = monthExpenses.filter { it.date == todayStr }
        val todayActualSpending = todayExpenses.sumOf { it.amount }
        val todayDifference = if (isConfigured && recommendedDailySpending > 0.0) todayActualSpending - recommendedDailySpending else 0.0

        // 3. Status determination with centralized thresholds (only if configured)
        val todayStatus = when {
            !isConfigured || monthlyLimit <= 0.0 -> DailySpendingStatus.NORMAL
            recommendedDailySpending <= 0.0 && todayActualSpending > 0.0 -> DailySpendingStatus.RED_ALERT
            todayActualSpending <= recommendedDailySpending -> DailySpendingStatus.NORMAL
            todayActualSpending < recommendedDailySpending * 1.25 -> DailySpendingStatus.ORANGE_WARNING
            else -> DailySpendingStatus.RED_ALERT
        }

        // 4. Check and trigger Daily Notification (At most ONE per day if configured)
        if (isConfigured && monthlyLimit > 0.0) {
            checkAndTriggerDailyAlertNotification(todayStatus, recommendedDailySpending, todayActualSpending, workspace, isBengali)
        }

        // 5. Category Breakdown
        val categoryTotals = mutableMapOf<String, Double>()
        monthExpenses.forEach { tx ->
            val cat = tx.category.trim()
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + tx.amount
        }
        val categoryBreakdown = categoryTotals.map { (cat, amt) ->
            val pct = if (totalSpent > 0.0) (amt / totalSpent) * 100.0 else 0.0
            CategorySpendingItem(category = cat, amount = amt, percentage = pct)
        }.sortedByDescending { it.amount }

        val highestCategory = categoryBreakdown.firstOrNull()?.category

        // 6. Recent Days History (Up to past 7 days)
        val dailyHistory = mutableListOf<DailySpendingRecord>()
        val startDay = (currentDayOfMonth - 6).coerceAtLeast(1)
        for (day in currentDayOfMonth downTo startDay) {
            val dateStr = String.format("%04d-%02d-%02d", curYear, curMonth, day)
            val dayExpenses = monthExpenses.filter { it.date == dateStr }.sumOf { it.amount }
            val dayStatus = when {
                !isConfigured || monthlyLimit <= 0.0 -> DailySpendingStatus.NORMAL
                recommendedDailySpending <= 0.0 && dayExpenses > 0.0 -> DailySpendingStatus.RED_ALERT
                dayExpenses <= recommendedDailySpending -> DailySpendingStatus.NORMAL
                dayExpenses < recommendedDailySpending * 1.25 -> DailySpendingStatus.ORANGE_WARNING
                else -> DailySpendingStatus.RED_ALERT
            }
            dailyHistory.add(
                DailySpendingRecord(
                    date = dateStr,
                    recommendedAmount = recommendedDailySpending,
                    actualAmount = dayExpenses,
                    difference = if (isConfigured && recommendedDailySpending > 0.0) dayExpenses - recommendedDailySpending else 0.0,
                    status = dayStatus
                )
            )
        }

        // 7. Data-driven Financial Insight
        val insight = generateFinancialInsight(
            isConfigured = isConfigured,
            suggestedLimit = suggestedLimit,
            highestCategory = highestCategory,
            categoryBreakdown = categoryBreakdown,
            totalSpent = totalSpent,
            monthlyLimit = monthlyLimit,
            remainingBudget = remainingBudget,
            todayStatus = todayStatus,
            isBengali = isBengali
        )

        return SmartSpendingSummary(
            monthlyLimit = monthlyLimit,
            totalSpent = totalSpent,
            remainingBudget = remainingBudget,
            percentageUsed = percentageUsed,
            remainingDays = remainingDays,
            totalDaysInMonth = totalDaysInMonth,
            recommendedDailySpending = recommendedDailySpending,
            todayActualSpending = todayActualSpending,
            todayStatus = todayStatus,
            todayDifference = todayDifference,
            highestCategory = highestCategory,
            categoryBreakdown = categoryBreakdown,
            dailyHistory = dailyHistory,
            financialInsight = insight,
            workspace = workspace.id,
            isRecurringLimit = isRecurringLimitEnabled(workspace),
            isConfigured = isConfigured,
            suggestedLimit = suggestedLimit
        )
    }

    /**
     * Enforces the critical rule: At most ONE alert notification per day.
     */
    private fun checkAndTriggerDailyAlertNotification(
        status: DailySpendingStatus,
        recommended: Double,
        actual: Double,
        workspace: Workspace,
        isBengali: Boolean
    ) {
        if (status == DailySpendingStatus.NORMAL) return

        val todayStr = DateUtils.todayDateString()
        val lastAlertKey = "last_alert_${workspace.id.lowercase()}"
        val lastAlertDate = prefs.getString(lastAlertKey, null)

        // Strict suppression: Already alerted today for this workspace
        if (lastAlertDate == todayStr) return

        val recFmt = CurrencyFormatter.format(recommended, isBengali)
        val actFmt = CurrencyFormatter.format(actual, isBengali)

        val (titleEn, titleBn, msgEn, msgBn) = when (status) {
            DailySpendingStatus.ORANGE_WARNING -> {
                AnalyticsManager.logSmartSpendingAlert(isRedAlert = false, workspace = workspace.id)
                Quadruple(
                    "Spending Warning",
                    "খরচের সতর্কতা",
                    "Today's spending ($actFmt) is slightly above your recommended daily budget ($recFmt). Cut back tomorrow to maintain your monthly budget.",
                    "আজ আপনার খরচ একটু বেশি হয়েছে। আগামীকাল একটু কম খরচ করার চেষ্টা করুন যাতে মাসিক বাজেট ঠিক থাকে।"
                )
            }
            DailySpendingStatus.RED_ALERT -> {
                AnalyticsManager.logSmartSpendingAlert(isRedAlert = true, workspace = workspace.id)
                Quadruple(
                    "High Spending Alert",
                    "অতিরিক্ত খরচের জরুরি সতর্কতা",
                    "Today's spending ($actFmt) is significantly over your daily budget ($recFmt). Reduce spending tomorrow to protect your monthly plan.",
                    "আজ আপনার খরচ অনেক বেশি হয়েছে। আগামীকাল কম খরচ করে মাসিক বাজেট ঠিক রাখুন।"
                )
            }
            else -> return
        }

        val notification = AppNotification(
            id = "spending_alert_${workspace.id}_$todayStr",
            titleEn = titleEn,
            titleBn = titleBn,
            messageEn = msgEn,
            messageBn = msgBn,
            timestamp = System.currentTimeMillis(),
            type = NotificationType.EXPENSE_REMINDER,
            isRead = false
        )

        notificationRepo.addNotification(notification, showSystemNotification = true)
        prefs.edit().putString(lastAlertKey, todayStr).apply()
    }

    private fun generateFinancialInsight(
        isConfigured: Boolean,
        suggestedLimit: Double,
        highestCategory: String?,
        categoryBreakdown: List<CategorySpendingItem>,
        totalSpent: Double,
        monthlyLimit: Double,
        remainingBudget: Double,
        todayStatus: DailySpendingStatus,
        isBengali: Boolean
    ): String {
        val categoryManager = CategoryManager.getInstance(context)
        if (!isConfigured || monthlyLimit <= 0.0) {
            return if (suggestedLimit > 0.0) {
                val sugFmt = CurrencyFormatter.format(suggestedLimit, isBengali)
                if (isBengali)
                    "আপনার বিগত খরচের ভিত্তিতে প্রস্তাবিত মাসিক বাজেট $sugFmt। দৈনিক সুষম ব্যয়ের জন্য বাজেট নির্ধারণ করুন।"
                else
                    "Based on your actual expense history, a recommended monthly budget is $sugFmt. Set your limit to enable daily budget tracking."
            } else {
                if (isBengali)
                    "দৈনিক খরচের সঠিক নিয়ন্ত্রণ ও সাশ্রয়ী পরিকল্পনার জন্য একটি মাসিক ব্যয়ের সীমা নির্ধারণ করুন।"
                else
                    "Set a monthly spending limit to receive personalized daily recommendations and spending alerts."
            }
        }

        return when {
            remainingBudget <= 0.0 -> {
                if (isBengali)
                    "আপনার এই মাসের নির্ধারিত বাজেট অতিক্রান্ত হয়েছে। আগামী দিনগুলোতে অপচয় পরিহার করার পরামর্শ দেওয়া হলো।"
                else
                    "Your monthly spending budget has been fully consumed. Exercise caution on discretionary purchases for the rest of the month."
            }
            highestCategory != null && categoryBreakdown.isNotEmpty() -> {
                val topCat = categoryBreakdown.first()
                val topName = categoryManager.getCategoryDisplayName(topCat.category, isBengali)
                val pctStr = String.format("%.1f", topCat.percentage)
                if (isBengali)
                    "আপনার এই মাসের প্রধান ব্যয়ের খাত হলো \"$topName\", যা মোট ব্যয়ের ${CurrencyFormatter.toBengaliDigits(pctStr)}%।"
                else
                    "Your primary spending category this month is \"$topName\", representing $pctStr% of your total expenses."
            }
            todayStatus == DailySpendingStatus.NORMAL && totalSpent > 0.0 -> {
                if (isBengali)
                    "আজকের খরচের গতি আপনার নির্ধারিত মাসিক বাজেট অনুযায়ী পুরোপুরি সঠিক রয়েছে।"
                else
                    "Your daily spending pace today is aligned with your monthly financial plan."
            }
            else -> {
                if (isBengali)
                    "সঠিক পরিকল্পনা ও দৈনিক বাজেট অনুসরণে আর্থিক সঞ্চয় বৃদ্ধি পায়।"
                else
                    "Adhering to recommended daily spending fosters disciplined savings and long-term financial health."
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val PREF_NAME = "amar_hishab_spending_planner"
        private const val KEY_LIMIT_PERSONAL = "spending_limit_personal"
        private const val KEY_LIMIT_BUSINESS = "spending_limit_business"
        private const val KEY_RECURRING_PERSONAL = "spending_recurring_personal"
        private const val KEY_RECURRING_BUSINESS = "spending_recurring_business"

        @Volatile
        private var instance: SpendingPlannerManager? = null

        fun getInstance(context: Context): SpendingPlannerManager {
            return instance ?: synchronized(this) {
                instance ?: SpendingPlannerManager(context.applicationContext).also { instance = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        fun resetForTesting() {
            instance = null
        }
    }

    @androidx.annotation.VisibleForTesting
    fun clearLimitsForTesting() {
        prefs.edit().clear().commit()
    }
}
