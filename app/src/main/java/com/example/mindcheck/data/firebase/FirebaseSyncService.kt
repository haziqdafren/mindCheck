package com.mindcheck.app.data.firebase

import android.content.Context
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Service to sync data between local Room database and Firebase Firestore
 * Handles bidirectional sync for all user data
 */
class FirebaseSyncService(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) {

    private val db = AppDatabase.getDatabase(context)

    /**
     * Sync all user data to Firestore (upload)
     */
    suspend fun syncToFirestore(userId: String): Result<Unit> {
        return try {
            Timber.d("Starting sync to Firestore for user: $userId")

            // Sync user data
            val user = db.userDao().getUserById(userId)
            if (user != null) {
                val userData = mapOf(
                    "userId" to user.userId,
                    "nama" to user.nama,
                    "email" to user.email,
                    "gender" to (user.gender ?: ""),
                    "usia" to (user.usia ?: 0),
                    "universitas" to (user.universitas ?: ""),
                    "createdAt" to user.createdAt,
                    "updatedAt" to user.updatedAt
                )
                firestoreRepository.saveUserData(userId, userData)
            }

            // Sync screenings
            val screenings = db.screeningDao().getLatestScreening(userId)
            if (screenings != null) {
                val data = screeningToMap(screenings)
                firestoreRepository.saveDocument(
                    FirestoreRepository.COLLECTION_SCREENINGS,
                    screenings.screeningId,
                    data
                )
            }

            // Sync mood logs
            val moodLogs = db.moodLogDao().getRecentMoodLogs(userId, 100)
            moodLogs.forEach { moodLog ->
                val data = moodLogToMap(moodLog)
                firestoreRepository.saveDocument(
                    FirestoreRepository.COLLECTION_MOOD_LOGS,
                    moodLog.moodId,
                    data
                )
            }

            // Sync sleep logs
            val sleepLogs = db.sleepLogDao().getRecentSleepLogs(userId, 100)
            sleepLogs.forEach { sleepLog ->
                val data = sleepLogToMap(sleepLog)
                firestoreRepository.saveDocument(
                    FirestoreRepository.COLLECTION_SLEEP_LOGS,
                    sleepLog.sleepId,
                    data
                )
            }

            // Sync gratitude entries
            val gratitudeEntries = db.gratitudeEntryDao().getRecentGratitudeEntries(userId, 100)
            gratitudeEntries.forEach { entry ->
                val data = gratitudeEntryToMap(entry)
                firestoreRepository.saveDocument(
                    FirestoreRepository.COLLECTION_GRATITUDE_ENTRIES,
                    entry.entryId,
                    data
                )
            }

            // Sync goals
            val goals = db.goalDao().getRecentGoals(userId, 50)
            goals.forEach { goal ->
                val data = goalToMap(goal)
                firestoreRepository.saveDocument(
                    FirestoreRepository.COLLECTION_GOALS,
                    goal.goalId,
                    data
                )
            }

            Timber.d("Sync to Firestore completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error syncing to Firestore")
            Result.failure(e)
        }
    }

    /**
     * Sync all user data from Firestore (download)
     */
    suspend fun syncFromFirestore(userId: String): Result<Unit> {
        return try {
            Timber.d("Starting sync from Firestore for user: $userId")

            // Sync user data
            val userDataResult = firestoreRepository.getUserData(userId)
            userDataResult.getOrNull()?.let { userData ->
                // User data is already in Room from registration/login
                Timber.d("User data synced from Firestore")
            }

            // Sync mood logs
            val moodLogsResult = firestoreRepository.getUserDocuments(
                FirestoreRepository.COLLECTION_MOOD_LOGS,
                userId
            )
            moodLogsResult.getOrNull()?.forEach { data ->
                val moodLog = mapToMoodLog(data)
                db.moodLogDao().insertMoodLog(moodLog)
            }

            // Sync sleep logs
            val sleepLogsResult = firestoreRepository.getUserDocuments(
                FirestoreRepository.COLLECTION_SLEEP_LOGS,
                userId
            )
            sleepLogsResult.getOrNull()?.forEach { data ->
                val sleepLog = mapToSleepLog(data)
                db.sleepLogDao().insertSleepLog(sleepLog)
            }

            // Sync gratitude entries
            val gratitudeResult = firestoreRepository.getUserDocuments(
                FirestoreRepository.COLLECTION_GRATITUDE_ENTRIES,
                userId
            )
            gratitudeResult.getOrNull()?.forEach { data ->
                val entry = mapToGratitudeEntry(data)
                db.gratitudeEntryDao().insertGratitudeEntry(entry)
            }

            // Sync goals
            val goalsResult = firestoreRepository.getUserDocuments(
                FirestoreRepository.COLLECTION_GOALS,
                userId
            )
            goalsResult.getOrNull()?.forEach { data ->
                val goal = mapToGoal(data)
                db.goalDao().insertGoal(goal)
            }

            Timber.d("Sync from Firestore completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error syncing from Firestore")
            Result.failure(e)
        }
    }

    /**
     * Auto-sync in background
     */
    fun startAutoSync(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            syncToFirestore(userId)
        }
    }

    // Helper methods to convert entities to maps
    private fun screeningToMap(screening: ScreeningEntity): Map<String, Any> {
        return mapOf(
            "screeningId" to screening.screeningId,
            "userId" to screening.userId,
            "tingkatRisiko" to screening.tingkatRisiko,
            "skorPrediksi" to screening.skorPrediksi,
            "tanggal" to screening.tanggal,
            "rekomendasi" to screening.rekomendasi
        )
    }

    private fun moodLogToMap(moodLog: MoodLogEntity): Map<String, Any> {
        return mapOf(
            "moodId" to moodLog.moodId,
            "userId" to moodLog.userId,
            "tingkatMood" to moodLog.tingkatMood,
            "catatan" to (moodLog.catatan ?: ""),
            "tanggal" to moodLog.tanggal
        )
    }

    private fun sleepLogToMap(sleepLog: SleepLogEntity): Map<String, Any> {
        return mapOf(
            "sleepId" to sleepLog.sleepId,
            "userId" to sleepLog.userId,
            "waktuTidur" to sleepLog.waktuTidur,
            "waktuBangun" to sleepLog.waktuBangun,
            "durasiJam" to sleepLog.durasiJam,
            "kategoriDurasi" to sleepLog.kategoriDurasi,
            "kualitasTidur" to sleepLog.kualitasTidur,
            "tanggal" to sleepLog.tanggal
        )
    }

    private fun gratitudeEntryToMap(entry: GratitudeEntryEntity): Map<String, Any> {
        return mapOf(
            "entryId" to entry.entryId,
            "userId" to entry.userId,
            "item1" to entry.item1,
            "item2" to entry.item2,
            "item3" to entry.item3,
            "tanggal" to entry.tanggal
        )
    }

    private fun goalToMap(goal: GoalEntity): Map<String, Any> {
        return mapOf(
            "goalId" to goal.goalId,
            "userId" to goal.userId,
            "judul" to goal.judul,
            "deskripsi" to goal.deskripsi,
            "kategori" to goal.kategori,
            "tanggalMulai" to goal.tanggalMulai,
            "tanggalTarget" to goal.tanggalTarget,
            "status" to goal.status,
            "createdAt" to goal.createdAt
        )
    }

    // Helper methods to convert maps to entities
    private fun mapToMoodLog(data: Map<String, Any>): MoodLogEntity {
        return MoodLogEntity(
            moodId = data["moodId"] as String,
            userId = data["userId"] as String,
            tingkatMood = data["tingkatMood"] as String,
            catatan = data["catatan"] as? String ?: "",
            tanggal = (data["tanggal"] as Number).toLong()
        )
    }

    private fun mapToSleepLog(data: Map<String, Any>): SleepLogEntity {
        return SleepLogEntity(
            sleepId = data["sleepId"] as String,
            userId = data["userId"] as String,
            waktuTidur = data["waktuTidur"] as String,
            waktuBangun = data["waktuBangun"] as String,
            durasiJam = (data["durasiJam"] as Number).toFloat(),
            kategoriDurasi = data["kategoriDurasi"] as String,
            kualitasTidur = (data["kualitasTidur"] as Number).toInt(),
            tanggal = (data["tanggal"] as Number).toLong()
        )
    }

    private fun mapToGratitudeEntry(data: Map<String, Any>): GratitudeEntryEntity {
        return GratitudeEntryEntity(
            entryId = data["entryId"] as String,
            userId = data["userId"] as String,
            item1 = data["item1"] as String,
            item2 = data["item2"] as? String ?: "",
            item3 = data["item3"] as? String ?: "",
            tanggal = (data["tanggal"] as Number).toLong()
        )
    }

    private fun mapToGoal(data: Map<String, Any>): GoalEntity {
        return GoalEntity(
            goalId = data["goalId"] as String,
            userId = data["userId"] as String,
            judul = data["judul"] as String,
            deskripsi = data["deskripsi"] as String,
            kategori = data["kategori"] as String,
            tanggalMulai = (data["tanggalMulai"] as Number).toLong(),
            tanggalTarget = (data["tanggalTarget"] as Number).toLong(),
            status = data["status"] as String,
            createdAt = (data["createdAt"] as Number).toLong()
        )
    }
}
