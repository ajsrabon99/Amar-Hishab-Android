package com.ajyra.amarhishab.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajyra.amarhishab.model.SavingsGoal

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: String,
    val description: String? = null,
    val iconCategory: String? = null,
    val isActiveOnDashboard: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SavingsGoal = SavingsGoal(
        id = id,
        name = name,
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        targetDate = targetDate,
        description = description,
        iconCategory = iconCategory,
        isActiveOnDashboard = isActiveOnDashboard,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: SavingsGoal): SavingsGoalEntity = SavingsGoalEntity(
            id = domain.id,
            name = domain.name,
            targetAmount = domain.targetAmount,
            savedAmount = domain.savedAmount,
            targetDate = domain.targetDate,
            description = domain.description,
            iconCategory = domain.iconCategory,
            isActiveOnDashboard = domain.isActiveOnDashboard,
            createdAt = domain.createdAt
        )
    }
}
