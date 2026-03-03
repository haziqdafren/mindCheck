package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.ScreeningEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Screening operations
 */
@Dao
interface ScreeningDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreening(screening: ScreeningEntity): Long

    @Update
    suspend fun updateScreening(screening: ScreeningEntity)

    @Delete
    suspend fun deleteScreening(screening: ScreeningEntity)

    @Query("SELECT * FROM screenings WHERE screeningId = :screeningId")
    suspend fun getScreeningById(screeningId: String): ScreeningEntity?

    @Query("SELECT * FROM screenings WHERE screeningId = :screeningId")
    fun getScreeningByIdFlow(screeningId: String): Flow<ScreeningEntity?>

    @Query("SELECT * FROM screenings WHERE userId = :userId ORDER BY tanggal DESC")
    fun getScreeningsByUser(userId: String): Flow<List<ScreeningEntity>>

    @Query("SELECT * FROM screenings WHERE userId = :userId ORDER BY tanggal DESC LIMIT 1")
    suspend fun getLatestScreening(userId: String): ScreeningEntity?

    @Query("SELECT * FROM screenings WHERE userId = :userId ORDER BY tanggal DESC LIMIT 1")
    fun getLatestScreeningFlow(userId: String): Flow<ScreeningEntity?>

    @Query("SELECT * FROM screenings ORDER BY tanggal DESC")
    fun getAllScreenings(): Flow<List<ScreeningEntity>>

    @Query("SELECT COUNT(*) FROM screenings WHERE userId = :userId")
    suspend fun getScreeningCount(userId: String): Int
}
