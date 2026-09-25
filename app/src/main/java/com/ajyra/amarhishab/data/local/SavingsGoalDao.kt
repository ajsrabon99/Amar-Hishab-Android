package com.ajyra.amarhishab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    suspend fun getAllGoalsList(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE isActiveOnDashboard = 1 LIMIT 1")
    fun getActiveDashboardGoal(): Flow<SavingsGoalEntity?>

    @Query("UPDATE savings_goals SET isActiveOnDashboard = 0")
    suspend fun clearActiveDashboardGoals()

    @Query("UPDATE savings_goals SET isActiveOnDashboard = 1 WHERE id = :id")
    suspend fun setActiveGoalById(id: String)

    @Transaction
    suspend fun setActiveDashboardGoal(id: String) {
        clearActiveDashboardGoals()
        setActiveGoalById(id)
    }

    @Query("UPDATE savings_goals SET savedAmount = savedAmount + :amount WHERE id = :id")
    suspend fun addMoneyToGoal(id: String, amount: Double)

    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun getCount(): Int
}
