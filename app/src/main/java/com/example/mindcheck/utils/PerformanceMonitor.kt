package com.mindcheck.app.utils

import android.os.SystemClock
import timber.log.Timber

/**
 * Performance monitoring utility to track and log operation times
 * Helps identify performance bottlenecks during development
 */
object PerformanceMonitor {

    private val startTimes = mutableMapOf<String, Long>()

    /**
     * Start tracking an operation
     * @param operationName Unique identifier for the operation
     */
    fun start(operationName: String) {
        startTimes[operationName] = SystemClock.elapsedRealtime()
        Timber.v("[PERF] Started: $operationName")
    }

    /**
     * End tracking and log the duration
     * @param operationName Must match the name used in start()
     * @param warnThresholdMs Log warning if operation took longer than this (default: 100ms)
     */
    fun end(operationName: String, warnThresholdMs: Long = 100) {
        val startTime = startTimes.remove(operationName)
        if (startTime == null) {
            Timber.w("[PERF] No start time found for: $operationName")
            return
        }

        val duration = SystemClock.elapsedRealtime() - startTime

        when {
            duration > warnThresholdMs -> Timber.w("[PERF] ⚠️ Slow ($duration ms): $operationName")
            duration > warnThresholdMs / 2 -> Timber.i("[PERF] ⏱️ Moderate ($duration ms): $operationName")
            else -> Timber.d("[PERF] ✅ Fast ($duration ms): $operationName")
        }
    }

    /**
     * Measure and log execution time of a suspending function
     */
    suspend fun <T> measureSuspend(
        operationName: String,
        warnThresholdMs: Long = 100,
        block: suspend () -> T
    ): T {
        start(operationName)
        return try {
            block()
        } finally {
            end(operationName, warnThresholdMs)
        }
    }

    /**
     * Measure and log execution time of a regular function
     */
    fun <T> measure(
        operationName: String,
        warnThresholdMs: Long = 100,
        block: () -> T
    ): T {
        start(operationName)
        return try {
            block()
        } finally {
            end(operationName, warnThresholdMs)
        }
    }

    /**
     * Clear all tracked operations (useful for cleanup)
     */
    fun clear() {
        startTimes.clear()
    }
}
