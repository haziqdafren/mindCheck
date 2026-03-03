package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.EmergencyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyLog(log: EmergencyLogEntity): Long

    @Update
    suspend fun updateEmergencyLog(log: EmergencyLogEntity)

    @Delete
    suspend fun deleteEmergencyLog(log: EmergencyLogEntity)

    @Query("SELECT * FROM emergency_logs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getEmergencyLogsByUser(userId: String): Flow<List<EmergencyLogEntity>>

    @Query("SELECT * FROM emergency_logs WHERE userId = :userId AND jenisLatihan = :type ORDER BY createdAt DESC")
    fun getEmergencyLogsByType(userId: String, type: String): Flow<List<EmergencyLogEntity>>

    @Query("SELECT COUNT(*) FROM emergency_logs WHERE userId = :userId AND createdAt >= :startDate")
    suspend fun getEmergencyLogCountSince(userId: String, startDate: Long): Int
}
