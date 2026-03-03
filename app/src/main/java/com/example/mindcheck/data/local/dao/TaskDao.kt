package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getTasksByGoalId(goalId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE goalId IN (:goalIds) ORDER BY goalId, createdAt DESC")
    suspend fun getTasksByGoalIds(goalIds: List<String>): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId AND selesai = :completed ORDER BY createdAt DESC")
    fun getTasksByGoalIdAndStatus(goalId: String, completed: Boolean): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET selesai = :completed, waktuSelesai = :timestamp WHERE taskId = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean, timestamp: Long?)

    @Query("SELECT COUNT(*) FROM tasks WHERE goalId = :goalId AND selesai = :completed")
    suspend fun getTaskCountByStatus(goalId: String, completed: Boolean): Int

    @Query("DELETE FROM tasks WHERE goalId = :goalId")
    suspend fun deleteTasksByGoalId(goalId: String)
}
