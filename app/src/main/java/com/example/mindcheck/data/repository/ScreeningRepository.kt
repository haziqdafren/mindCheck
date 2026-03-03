package com.mindcheck.app.data.repository

import android.content.Context
import com.mindcheck.app.data.firebase.FirebaseAuthRepository
import com.mindcheck.app.data.firebase.FirestoreRepository
import com.mindcheck.app.data.local.dao.ScreeningDao
import com.mindcheck.app.data.local.entity.ScreeningEntity
import com.mindcheck.app.data.remote.dto.PredictionResponse
import com.mindcheck.app.domain.model.ScreeningAnswers
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository for screening operations
 * Handles local database and cloud sync for screening results
 */
class ScreeningRepository(
    private val screeningDao: ScreeningDao,
    private val authRepository: FirebaseAuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val context: Context
) {

    /**
     * Get current user ID from Firebase Auth or SharedPreferences (fallback for guest mode)
     */
    private fun getCurrentUserId(): String? {
        // Try Firebase Auth first
        val firebaseUserId = authRepository.getUserId()
        if (firebaseUserId != null) return firebaseUserId

        // Fallback to SharedPreferences for guest mode
        val prefs = context.getSharedPreferences("mindcheck_prefs", Context.MODE_PRIVATE)
        val sharedPrefsUserId = prefs.getString("current_user_id", null)
        if (sharedPrefsUserId != null) return sharedPrefsUserId

        // Last resort: use "guest" as default user
        return "guest"
    }

    /**
     * Save screening result to local database and sync to cloud
     */
    suspend fun saveScreeningResult(
        answers: ScreeningAnswers,
        result: PredictionResponse
    ): Result<String> {
        return try {
            // Get current user ID
            val userId = getCurrentUserId()
                ?: throw Exception("User not logged in")

            // Create screening entity
            val screening = ScreeningEntity(
                userId = userId,
                tekananAkademik = answers.academicPressure?.toFloat() ?: 0f,
                kepuasanStudi = answers.studySatisfaction?.toFloat() ?: 0f,
                durasiTidur = answers.sleepDuration ?: "",
                polaMakan = answers.dietaryHabits ?: "",
                pikiranBunuhDiri = answers.suicidalThoughts ?: "",
                jamBelajar = answers.studyHours ?: 0,
                stresKeuangan = answers.financialStress?.toFloat() ?: 0f,
                skorPrediksi = (result.confidence * 100).toFloat(), // Convert to percentage (0.84 -> 84.0)
                tingkatRisiko = result.riskLevel,
                rekomendasi = result.advice.joinToString("\n• ", prefix = "• ") // Format as bullet list
            )

            // Save to local database
            val screeningId = screeningDao.insertScreening(screening)
            Timber.d("Screening saved to local database with ID: ${screening.screeningId}")

            // Sync to Firestore (background operation - don't block on failure)
            try {
                val screeningData = mapOf(
                    "screeningId" to screening.screeningId,
                    "userId" to screening.userId,
                    "tekananAkademik" to screening.tekananAkademik,
                    "kepuasanStudi" to screening.kepuasanStudi,
                    "durasiTidur" to screening.durasiTidur,
                    "polaMakan" to screening.polaMakan,
                    "pikiranBunuhDiri" to screening.pikiranBunuhDiri,
                    "jamBelajar" to screening.jamBelajar,
                    "stresKeuangan" to screening.stresKeuangan,
                    "skorPrediksi" to screening.skorPrediksi,
                    "tingkatRisiko" to screening.tingkatRisiko,
                    "rekomendasi" to screening.rekomendasi,
                    "tanggal" to screening.tanggal,
                    "createdAt" to screening.createdAt
                )

                firestoreRepository.saveDocument(
                    FirestoreRepository.COLLECTION_SCREENINGS,
                    screening.screeningId,
                    screeningData
                )
                Timber.d("Screening synced to Firestore")
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync screening to Firestore (non-fatal)")
            }

            Result.success(screening.screeningId)
        } catch (e: Exception) {
            Timber.e(e, "Error saving screening result")
            Result.failure(e)
        }
    }

    /**
     * Get all screenings for current user
     */
    fun getUserScreenings(): Flow<List<ScreeningEntity>> {
        val userId = getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return screeningDao.getScreeningsByUser(userId)
    }

    /**
     * Get latest screening for current user
     */
    suspend fun getLatestScreening(): ScreeningEntity? {
        val userId = getCurrentUserId() ?: return null
        return screeningDao.getLatestScreening(userId)
    }

    /**
     * Get screening by ID
     */
    suspend fun getScreeningById(screeningId: String): ScreeningEntity? {
        return screeningDao.getScreeningById(screeningId)
    }

    /**
     * Delete screening
     */
    suspend fun deleteScreening(screening: ScreeningEntity): Result<Unit> {
        return try {
            screeningDao.deleteScreening(screening)

            // Also delete from Firestore
            try {
                firestoreRepository.deleteDocument(
                    FirestoreRepository.COLLECTION_SCREENINGS,
                    screening.screeningId
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete screening from Firestore (non-fatal)")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting screening")
            Result.failure(e)
        }
    }

    /**
     * Get screening count for current user
     */
    suspend fun getScreeningCount(): Int {
        val userId = getCurrentUserId() ?: return 0
        return screeningDao.getScreeningCount(userId)
    }
}
