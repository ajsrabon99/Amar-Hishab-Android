package com.ajyra.amarhishab.model

import com.ajyra.amarhishab.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

enum class GoalStatus {
    IN_PROGRESS,
    COMPLETED,
    OVERDUE
}

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDate: String, // yyyy-MM-dd
    val description: String? = null,
    val iconCategory: String? = "general",
    val isActiveOnDashboard: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = max(0.0, targetAmount - savedAmount)

    val progressPercentage: Double
        get() = if (targetAmount > 0.0) {
            (savedAmount / targetAmount * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

    val isCompleted: Boolean
        get() = savedAmount >= targetAmount

    fun getDaysRemaining(todayDateStr: String = DateUtils.todayDateString()): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = sdf.parse(todayDateStr) ?: Date()
            val target = sdf.parse(targetDate) ?: today
            val diffMillis = target.time - today.time
            ceil(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toInt()
        } catch (_: Exception) {
            0
        }
    }

    fun getStatus(todayDateStr: String = DateUtils.todayDateString()): GoalStatus {
        return when {
            isCompleted -> GoalStatus.COMPLETED
            getDaysRemaining(todayDateStr) < 0 -> GoalStatus.OVERDUE
            else -> GoalStatus.IN_PROGRESS
        }
    }

    fun getRequiredDaily(daysRemaining: Int): Double {
        if (isCompleted || remainingAmount <= 0.0) return 0.0
        val days = max(1, daysRemaining)
        return remainingAmount / days
    }

    fun getRequiredWeekly(daysRemaining: Int): Double {
        if (isCompleted || remainingAmount <= 0.0) return 0.0
        val weeks = max(1.0, daysRemaining / 7.0)
        return remainingAmount / weeks
    }

    fun getRequiredMonthly(daysRemaining: Int): Double {
        if (isCompleted || remainingAmount <= 0.0) return 0.0
        val months = max(1.0, daysRemaining / 30.4375)
        return remainingAmount / months
    }
}

data class SmartSavingSuggestion(
    val category: String,
    val currentMonthlyExpense: Double,
    val suggestedReductionPercentage: Int,
    val potentialMonthlySavings: Double,
    val reasonEn: String,
    val reasonBn: String,
    val isEssential: Boolean = false
)

data class SmartSavingsPlan(
    val goalId: String,
    val goalName: String,
    val requiredMonthly: Double,
    val suggestions: List<SmartSavingSuggestion>,
    val totalPotentialMonthlySavings: Double,
    val remainingRequiredMonthly: Double
)
