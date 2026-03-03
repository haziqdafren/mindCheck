package com.mindcheck.app.data.local.dao

import androidx.room.*
import com.mindcheck.app.data.local.entity.MoodTriggerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodTriggerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodTrigger(trigger: MoodTriggerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodTriggers(triggers: List<MoodTriggerEntity>)

    @Delete
    suspend fun deleteMoodTrigger(trigger: MoodTriggerEntity)

    @Query("SELECT * FROM mood_triggers WHERE moodId = :moodId")
    suspend fun getTriggersByMoodId(moodId: String): List<MoodTriggerEntity>

    @Query("SELECT * FROM mood_triggers WHERE moodId = :moodId")
    fun getTriggersByMoodIdFlow(moodId: String): Flow<List<MoodTriggerEntity>>

    @Query("DELETE FROM mood_triggers WHERE moodId = :moodId")
    suspend fun deleteTriggersByMoodId(moodId: String)
}
