package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.MoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(moodLog: MoodLogEntity): Long

    @Update
    suspend fun updateMoodLog(moodLog: MoodLogEntity)

    @Delete
    suspend fun deleteMoodLog(moodLog: MoodLogEntity)

    @Query("SELECT * FROM mood_logs WHERE moodId = :moodId")
    suspend fun getMoodLogById(moodId: String): MoodLogEntity?

    @Query("SELECT * FROM mood_logs WHERE userId = :userId ORDER BY tanggal DESC")
    fun getMoodLogsByUser(userId: String): Flow<List<MoodLogEntity>>

    @Query("SELECT * FROM mood_logs WHERE userId = :userId AND tanggal >= :startDate AND tanggal <= :endDate ORDER BY tanggal DESC")
    fun getMoodLogsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<MoodLogEntity>>

    @Query("SELECT * FROM mood_logs WHERE userId = :userId ORDER BY tanggal DESC LIMIT 1")
    suspend fun getLatestMoodLog(userId: String): MoodLogEntity?

    @Query("SELECT COUNT(*) FROM mood_logs WHERE userId = :userId AND tanggal >= :startDate")
    suspend fun getMoodLogCountSince(userId: String, startDate: Long): Int

    // Optimized queries with LIMIT for better performance
    @Query("SELECT * FROM mood_logs WHERE userId = :userId ORDER BY tanggal DESC LIMIT :limit")
    suspend fun getRecentMoodLogs(userId: String, limit: Int = 30): List<MoodLogEntity>

    @Query("SELECT COUNT(*) FROM mood_logs WHERE userId = :userId")
    suspend fun getMoodLogCount(userId: String): Int
}
