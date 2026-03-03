package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.GratitudeEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GratitudeEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGratitudeEntry(entry: GratitudeEntryEntity): Long

    @Update
    suspend fun updateGratitudeEntry(entry: GratitudeEntryEntity)

    @Delete
    suspend fun deleteGratitudeEntry(entry: GratitudeEntryEntity)

    @Query("SELECT * FROM gratitude_entries WHERE entryId = :entryId")
    suspend fun getGratitudeEntryById(entryId: String): GratitudeEntryEntity?

    @Query("SELECT * FROM gratitude_entries WHERE userId = :userId ORDER BY tanggal DESC")
    fun getGratitudeEntriesByUser(userId: String): Flow<List<GratitudeEntryEntity>>

    @Query("SELECT * FROM gratitude_entries WHERE userId = :userId AND tanggal >= :startDate AND tanggal <= :endDate ORDER BY tanggal DESC")
    fun getGratitudeEntriesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<GratitudeEntryEntity>>

    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE userId = :userId AND tanggal >= :startDate")
    suspend fun getGratitudeCountSince(userId: String, startDate: Long): Int

    // Optimized queries with LIMIT for better performance
    @Query("SELECT * FROM gratitude_entries WHERE userId = :userId ORDER BY tanggal DESC LIMIT :limit")
    suspend fun getRecentGratitudeEntries(userId: String, limit: Int = 30): List<GratitudeEntryEntity>

    @Query("SELECT tanggal FROM gratitude_entries WHERE userId = :userId ORDER BY tanggal DESC LIMIT :limit")
    suspend fun getRecentGratitudeDates(userId: String, limit: Int = 60): List<Long>

    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE userId = :userId")
    suspend fun getGratitudeCount(userId: String): Int
}
