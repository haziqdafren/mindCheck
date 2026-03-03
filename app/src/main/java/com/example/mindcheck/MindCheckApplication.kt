package com.mindcheck.app

import android.app.Application
import android.os.StrictMode
import com.mindcheck.app.BuildConfig
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.mock.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Application class for MindCheck
 * Optimized for fast startup - all heavy operations run in background
 */
class MindCheckApplication : Application() {

    // Application-scoped coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Enable StrictMode in debug builds to catch performance issues
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Initialize Timber for logging (lightweight)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val startTime = System.currentTimeMillis()

        // Pre-warm database connection in background (reduces first-access latency)
        applicationScope.launch(Dispatchers.IO) {
            DatabaseInitializer.initializeIfNeeded(this@MindCheckApplication)

            // Pre-initialize database instance
            AppDatabase.getDatabase(this@MindCheckApplication)

            val initTime = System.currentTimeMillis() - startTime
            Timber.d("MindCheck Application initialized in ${initTime}ms")
        }

        Timber.d("MindCheck Application onCreate completed")
    }

    /**
     * Enable StrictMode to detect performance issues during development
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll() // Detect disk reads, disk writes, network, etc
                .penaltyLog() // Log violations
                .penaltyFlashScreen() // Flash screen on violation (visual indicator)
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll() // Detect leaks, closeable leaks, etc
                .penaltyLog()
                .build()
        )

        Timber.d("StrictMode enabled - watching for performance violations")
    }
}
