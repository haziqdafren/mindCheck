package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Grounding log entity for Phase 2
 * Tracks grounding exercise completion
 */
@Entity(
    tableName = "grounding_logs",
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
data class GroundingLogEntity(
    @PrimaryKey
    val logId: String = UUID.randomUUID().toString(),
    val userId: String,
    val selesai: Boolean = false,
    val durasiDetik: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
