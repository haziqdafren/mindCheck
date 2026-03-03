package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Task completion entity for Phase 2
 * Tracks when tasks are completed
 */
@Entity(
    tableName = "task_completions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class TaskCompletionEntity(
    @PrimaryKey
    val completionId: String = UUID.randomUUID().toString(),
    val taskId: String,
    val tanggalSelesai: Long,
    val catatan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
