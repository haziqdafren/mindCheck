package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Gratitude entry entity for Phase 2
 * Daily gratitude journal with 3 items
 */
@Entity(
    tableName = "gratitude_entries",
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
data class GratitudeEntryEntity(
    @PrimaryKey
    val entryId: String = UUID.randomUUID().toString(),
    val userId: String,
    val item1: String,
    val item2: String = "",
    val item3: String = "",
    val tanggal: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
