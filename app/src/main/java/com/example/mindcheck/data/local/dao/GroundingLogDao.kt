package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.GroundingLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroundingLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroundingLog(log: GroundingLogEntity): Long

    @Update
    suspend fun updateGroundingLog(log: GroundingLogEntity)

    @Delete
    suspend fun deleteGroundingLog(log: GroundingLogEntity)

    @Query("SELECT * FROM grounding_logs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGroundingLogsByUser(userId: String): Flow<List<GroundingLogEntity>>

    @Query("SELECT COUNT(*) FROM grounding_logs WHERE userId = :userId AND selesai = :completed")
    suspend fun getGroundingLogCount(userId: String, completed: Boolean): Int
}
