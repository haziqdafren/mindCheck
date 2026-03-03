package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Screening entity matching ERD specification
 * Stores depression screening results and AI predictions
 */
@Entity(
    tableName = "screenings",
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
data class ScreeningEntity(
    @PrimaryKey
    val screeningId: String = UUID.randomUUID().toString(),
    val userId: String,

    // Screening answers (matching ERD fields)
    val tekananAkademik: Float,            // Academic pressure score
    val kepuasanStudi: Float,              // Study satisfaction score
    val durasiTidur: String,               // "Kurang dari 5 jam/5-6 jam/6-7 jam/7-8 jam/Lebih dari 8 jam"
    val polaMakan: String,                 // "Sehat/Cukup/Tidak Sehat"
    val pikiranBunuhDiri: String,          // "Tidak Pernah/Jarang/Kadang-kadang/Sering"
    val jamBelajar: Int,                   // Study hours per day
    val stresKeuangan: Float,              // Financial stress score

    // Prediction results
    val skorPrediksi: Float,               // Prediction score from AI model
    val tingkatRisiko: String,             // "Rendah/Sedang/Tinggi"
    val rekomendasi: String,               // Recommendations text

    // Metadata
    val tanggal: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
