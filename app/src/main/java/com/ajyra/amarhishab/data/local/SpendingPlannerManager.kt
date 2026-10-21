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

    fun getMonthlyLimit(workspace: Workspace): Double {
        val key = when (workspace) {
            Workspace.PERSONAL -> KEY_LIMIT_PERSONAL
            Workspace.BUSINESS -> KEY_LIMIT_BUSINESS
        }
        val defaultVal = if (workspace == Workspace.PERSONAL) 30000.0 else 50000.0
        return prefs.getString(key, defaultVal.toString())?.toDoubleOrNull() ?: defaultVal
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
                    tx.date.startsWith(curYearMonthPrefix)
        }

        val monthlyLimit = getMonthlyLimit(workspace)
        val totalSpent = monthExpenses.sumOf { it.amount }
        val remainingBudget = (monthlyLimit - totalSpent).coerceAtLeast(0.0)
        val percentageUsed = if (monthlyLimit > 0.0) ((totalSpent / monthlyLimit) * 100.0).coerceIn(0.0, 100.0) else 0.0

        // Remaining days including today
        val remainingDays = (totalDaysInMonth - currentDayOfMonth + 1).coerceAtLeast(1)

        // Recommended Daily Spending = Remaining Monthly Budget / Remaining Days
        val recommendedDailySpending = if (remainingDays > 0 && remainingBudget > 0.0) {
            remainingBudget / remainingDays
        } else {
            0.0
        }

        // 2. Today's actual expense
        val todayExpenses = monthExpenses.filter { it.date == todayStr }
        val todayActualSpending = todayExpenses.sumOf { it.amount }
        val todayDifference = todayActualSpending - recommendedDailySpending

        // 3. Status determination with centralized thresholds
        val todayStatus = when {
            recommendedDailySpending <= 0.0 && todayActualSpending > 0.0 -> DailySpendingStatus.RED_ALERT
            todayActualSpending <= recommendedDailySpending -> DailySpendingStatus.NORMAL
            todayActualSpending < recommendedDailySpending * 1.25 -> DailySpendingStatus.ORANGE_WARNING
            else -> DailySpendingStatus.RED_ALERT
        }

        // 4. Check and trigger Daily Notification (At most ONE per day!)
        checkAndTriggerDailyAlertNotification(todayStatus, recommendedDailySpending, todayActualSpending, workspace, isBengali)

        // 5. Category Breakdown
        val categoryManager = CategoryManager.getInstance(context)
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
                    difference = dayExpenses - recommendedDailySpending,
                    status = dayStatus
                )
            )
        }

        // 7. Data-driven Financial Insight
        val insight = generateFinancialInsight(
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
            isRecurringLimit = isRecurringLimitEnabled(workspace)
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
        highestCategory: String?,
        categoryBreakdown: List<CategorySpendingItem>,
        totalSpent: Double,
        monthlyLimit: Double,
        remainingBudget: Double,
        todayStatus: DailySpendingStatus,
        isBengali: Boolean
    ): String {
        val categoryManager = CategoryManager.getInstance(context)
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
    }
}
