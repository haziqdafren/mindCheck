package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Task entity for Phase 2
 * Tasks associated with goals
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["goalId"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class TaskEntity(
    @PrimaryKey
    val taskId: String = UUID.randomUUID().toString(),
    val goalId: String,
    val judulTugas: String,
    val tipePengulangan: String,           // "Harian", "Mingguan", "Khusus"
    val hariPengulangan: String = "",      // "Senin,Rabu,Jumat" for weekly
    val selesai: Boolean = false,
    val waktuSelesai: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
