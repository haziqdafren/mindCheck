package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(sleepLog: SleepLogEntity): Long

    @Update
    suspend fun updateSleepLog(sleepLog: SleepLogEntity)

    @Delete
    suspend fun deleteSleepLog(sleepLog: SleepLogEntity)

    @Query("SELECT * FROM sleep_logs WHERE sleepId = :sleepId")
    suspend fun getSleepLogById(sleepId: String): SleepLogEntity?

    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY tanggal DESC")
    fun getSleepLogsByUser(userId: String): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE userId = :userId AND tanggal >= :startDate AND tanggal <= :endDate ORDER BY tanggal DESC")
    fun getSleepLogsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY tanggal DESC LIMIT 1")
    suspend fun getLatestSleepLog(userId: String): SleepLogEntity?

    @Query("SELECT AVG(durasiJam) FROM sleep_logs WHERE userId = :userId AND tanggal >= :startDate")
    suspend fun getAverageSleepDuration(userId: String, startDate: Long): Float?

    @Query("SELECT AVG(kualitasTidur) FROM sleep_logs WHERE userId = :userId AND tanggal >= :startDate")
    suspend fun getAverageSleepQuality(userId: String, startDate: Long): Float?

    // Optimized queries with LIMIT for better performance
    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY tanggal DESC LIMIT :limit")
    suspend fun getRecentSleepLogs(userId: String, limit: Int = 30): List<SleepLogEntity>

    @Query("SELECT COUNT(*) FROM sleep_logs WHERE userId = :userId")
    suspend fun getSleepLogCount(userId: String): Int
}
