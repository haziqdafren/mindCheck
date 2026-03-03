package com.mindcheck.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mindcheck.app.data.local.dao.*
import com.mindcheck.app.data.local.entity.*

// Database utama aplikasi menggunakan Room (SQLite) untuk penyimpanan lokal
@Database(
    entities = [
        UserEntity::class,           // Tabel users
        ScreeningEntity::class,      // Tabel hasil screening
        MoodLogEntity::class,         // Tabel catatan mood
        MoodTriggerEntity::class,     // Tabel pemicu mood
        SleepLogEntity::class,        // Tabel log tidur
        GratitudeEntryEntity::class,  // Tabel jurnal rasa syukur
        GoalEntity::class,            // Tabel target/tujuan
        TaskEntity::class,            // Tabel tugas untuk target
        TaskCompletionEntity::class,  // Tabel penyelesaian tugas
        EmergencyLogEntity::class,    // Tabel log bantuan darurat
        GroundingLogEntity::class     // Tabel log teknik grounding
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAO untuk operasi database users dan screening
    abstract fun userDao(): UserDao
    abstract fun screeningDao(): ScreeningDao

    // DAO untuk fitur mood, sleep, dan gratitude
    abstract fun moodLogDao(): MoodLogDao
    abstract fun moodTriggerDao(): MoodTriggerDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun gratitudeEntryDao(): GratitudeEntryDao

    // DAO untuk fitur goals dan tasks
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao

    // DAO untuk fitur emergency
    abstract fun emergencyLogDao(): EmergencyLogDao
    abstract fun groundingLogDao(): GroundingLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton pattern untuk akses database
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindcheck_database"
                )
                    .fallbackToDestructiveMigration()  // Untuk development - akan hapus dan buat ulang DB jika schema berubah
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)  // Penulisan lebih cepat
                    .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))  // Query paralel
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return getDatabase(context)
        }
    }
}
