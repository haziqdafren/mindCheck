package com.mindcheck.app.data.mock

import android.content.Context
import android.content.SharedPreferences
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Initializes database with mock data on first app launch
 */
object DatabaseInitializer {

    private const val PREFS_NAME = "mindcheck_prefs"
    private const val KEY_DB_INITIALIZED = "db_initialized"
    private const val KEY_CURRENT_USER_ID = "current_user_id"

    /**
     * Initialize database with mock data if not already initialized
     */
    fun initializeIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isInitialized = prefs.getBoolean(KEY_DB_INITIALIZED, false)

        if (!isInitialized) {
            Timber.d("Initializing database with mock data...")
            initializeDatabase(context, prefs)
        } else {
            Timber.d("Database already initialized")
        }
    }

    private fun initializeDatabase(context: Context, prefs: SharedPreferences) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)

                // Create guest user to prevent FOREIGN KEY constraint failures in guest mode
                val guestUser = UserEntity(
                    userId = "guest",
                    email = "guest@mindcheck.app",
                    password = "", // No password for guest
                    nama = "Tamu"
                )

                // Insert guest user (ignore if already exists)
                try {
                    db.userDao().insertUser(guestUser)
                    Timber.d("Guest user created successfully")
                } catch (e: Exception) {
                    // Guest user might already exist, ignore
                    Timber.d("Guest user already exists or error: ${e.message}")
                }

                // Mark database as initialized
                prefs.edit().putBoolean(KEY_DB_INITIALIZED, true).apply()
                Timber.d("Database initialized (authentication system active)")
            } catch (e: Exception) {
                Timber.e(e, "Error initializing database")
            }
        }
    }

    /**
     * Generate mock data for a specific user (optional for demo/testing)
     */
    fun generateMockDataForUser(context: Context, userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)

                // Generate and insert mock data
                val moodLogs = MockDataProvider.generateMoodLogs(userId)
                moodLogs.forEach { db.moodLogDao().insertMoodLog(it) }

                val moodTriggers = MockDataProvider.generateMoodTriggers(moodLogs)
                db.moodTriggerDao().insertMoodTriggers(moodTriggers)

                val sleepLogs = MockDataProvider.generateSleepLogs(userId)
                sleepLogs.forEach { db.sleepLogDao().insertSleepLog(it) }

                val gratitudeEntries = MockDataProvider.generateGratitudeEntries(userId)
                gratitudeEntries.forEach { db.gratitudeEntryDao().insertGratitudeEntry(it) }

                val goals = MockDataProvider.generateGoals(userId)
                goals.forEach { db.goalDao().insertGoal(it) }

                val tasks = MockDataProvider.generateTasks(goals)
                db.taskDao().insertTasks(tasks)

                val emergencyLogs = MockDataProvider.generateEmergencyLogs(userId)
                emergencyLogs.forEach { db.emergencyLogDao().insertEmergencyLog(it) }

                // Add sample screening
                val screening = MockDataProvider.generateSampleScreening(userId)
                db.screeningDao().insertScreening(screening)

                Timber.d("Mock data generated for user: $userId")
            } catch (e: Exception) {
                Timber.e(e, "Error generating mock data")
            }
        }
    }

    /**
     * Get current user ID from preferences
     */
    fun getCurrentUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    /**
     * Clear all data and reinitialize (for testing)
     */
    fun resetDatabase(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.clearAllTables()
                Timber.d("Database cleared")

                // Reinitialize
                initializeIfNeeded(context)
            } catch (e: Exception) {
                Timber.e(e, "Error resetting database")
            }
        }
    }
}
