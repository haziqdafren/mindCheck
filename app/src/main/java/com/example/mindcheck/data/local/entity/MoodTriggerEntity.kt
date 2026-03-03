package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Mood trigger entity for Phase 2
 * Tracks what triggers/affects mood
 */
@Entity(
    tableName = "mood_triggers",
    foreignKeys = [
        ForeignKey(
            entity = MoodLogEntity::class,
            parentColumns = ["moodId"],
            childColumns = ["moodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moodId"])]
)
data class MoodTriggerEntity(
    @PrimaryKey
    val triggerId: String = UUID.randomUUID().toString(),
    val moodId: String,
    val kategoriPemicu: String,            // "Akademik", "Keuangan", "Tidur", "Kesehatan", "Keluarga", "Hubungan", "Lainnya"
    val createdAt: Long = System.currentTimeMillis()
)
