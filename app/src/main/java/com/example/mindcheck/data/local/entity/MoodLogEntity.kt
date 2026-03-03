package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Mood log entity for Phase 2
 * Tracks daily mood with notes and triggers
 */
@Entity(
    tableName = "mood_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["tanggal"])]
)
data class MoodLogEntity(
    @PrimaryKey
    val moodId: String = UUID.randomUUID().toString(),
    val userId: String,
    val tingkatMood: String,               // "Sangat Rendah", "Rendah", "Biasa", "Baik", "Sangat Baik"
    val catatan: String = "",
    val tanggal: Long,                     // Date as timestamp
    val createdAt: Long = System.currentTimeMillis()
)
