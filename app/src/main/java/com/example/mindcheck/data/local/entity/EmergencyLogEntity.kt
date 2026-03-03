package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Emergency log entity for Phase 2
 * Tracks use of emergency coping tools
 */
@Entity(
    tableName = "emergency_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class EmergencyLogEntity(
    @PrimaryKey
    val logId: String = UUID.randomUUID().toString(),
    val userId: String,
    val jenisLatihan: String,              // "Pernapasan", "Grounding", "Afirmasi"
    val selesai: Boolean = false,
    val feedback: String = "",             // "Lebih Baik", "Sama", "Lebih Buruk"
    val createdAt: Long = System.currentTimeMillis()
)
