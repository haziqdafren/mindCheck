package com.mindcheck.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * User entity matching ERD specification
 * Stores complete user information for personalization and screening
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String = UUID.randomUUID().toString(),
    val googleId: String? = null,
    val email: String,
    val password: String, // Hashed password
    val nama: String,
    val usia: Int? = null,
    val gender: String? = null, // "Laki-laki" or "Perempuan"
    val universitas: String? = null,
    val semester: Int? = null,
    val riwayatKeluarga: String? = null, // "Ya", "Tidak", or "Tidak Tahu"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
