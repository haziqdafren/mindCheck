package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Goal entity for Phase 2
 * Wellness goals tracking
 */
@Entity(
    tableName = "goals",
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
data class GoalEntity(
    @PrimaryKey
    val goalId: String = UUID.randomUUID().toString(),
    val userId: String,
    val judul: String,
    val deskripsi: String,
    val kategori: String,                  // "Tidur", "Stres", "Olahraga", "Makanan", "Belajar", "Sosial", "Lainnya"
    val tanggalMulai: Long,
    val tanggalTarget: Long,
    val status: String,                    // "Aktif", "Selesai", "Dibatalkan"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
