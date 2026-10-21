package com.ajyra.amarhishab.model

enum class DailySpendingStatus(val labelEn: String, val labelBn: String) {
    NORMAL("Normal", "স্বাভাবিক"),
    ORANGE_WARNING("Warning", "সতর্কতা"),
    RED_ALERT("High Alert", "জরুরি সতর্কতা")
}

data class DailySpendingRecord(
    val date: String,
    val recommendedAmount: Double,
    val actualAmount: Double,
    val difference: Double,
    val status: DailySpendingStatus
)

data class CategorySpendingItem(
    val category: String,
    val amount: Double,
    val percentage: Double
)

data class SmartSpendingSummary(
    val monthlyLimit: Double = 30000.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 30000.0,
    val percentageUsed: Double = 0.0,
    val remainingDays: Int = 30,
    val totalDaysInMonth: Int = 30,
    val recommendedDailySpending: Double = 1000.0,
    val todayActualSpending: Double = 0.0,
    val todayStatus: DailySpendingStatus = DailySpendingStatus.NORMAL,
    val todayDifference: Double = 0.0,
    val highestCategory: String? = null,
    val categoryBreakdown: List<CategorySpendingItem> = emptyList(),
    val dailyHistory: List<DailySpendingRecord> = emptyList(),
    val financialInsight: String? = null,
    val workspace: String = "PERSONAL",
    val isRecurringLimit: Boolean = true
)
