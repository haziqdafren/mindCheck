package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskCompletion(completion: TaskCompletionEntity): Long

    @Delete
    suspend fun deleteTaskCompletion(completion: TaskCompletionEntity)

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId ORDER BY tanggalSelesai DESC")
    fun getCompletionsByTaskId(taskId: String): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND tanggalSelesai >= :startDate AND tanggalSelesai <= :endDate")
    suspend fun getCompletionsByDateRange(taskId: String, startDate: Long, endDate: Long): List<TaskCompletionEntity>

    @Query("SELECT COUNT(*) FROM task_completions WHERE taskId = :taskId")
    suspend fun getCompletionCount(taskId: String): Int
}
