package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE goalId = :goalId")
    suspend fun getGoalById(goalId: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE goalId = :goalId")
    fun getGoalByIdFlow(goalId: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsByUser(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getGoalsByStatus(userId: String, status: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND kategori = :kategori ORDER BY createdAt DESC")
    fun getGoalsByCategory(userId: String, kategori: String): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND status = :status")
    suspend fun getGoalCountByStatus(userId: String, status: String): Int

    // Optimized queries with LIMIT for better performance
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentGoals(userId: String, limit: Int = 10): List<GoalEntity>

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId")
    suspend fun getGoalCount(userId: String): Int
}
