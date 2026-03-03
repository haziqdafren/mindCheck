package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Sleep log entity for Phase 2
 * Tracks sleep duration and quality
 */
@Entity(
    tableName = "sleep_logs",
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
data class SleepLogEntity(
    @PrimaryKey
    val sleepId: String = UUID.randomUUID().toString(),
    val userId: String,
    val waktuTidur: String,                // "22:00" format HH:mm
    val waktuBangun: String,               // "06:00"
    val durasiJam: Float,                  // 8.0
    val kategoriDurasi: String,            // "Kurang dari 5 jam", "5-6 jam", "7-8 jam", "Lebih dari 8 jam"
    val kualitasTidur: Int,                // 1-5 (stars)
    val tanggal: Long,
    val createdAt: Long = System.currentTimeMillis()
)
