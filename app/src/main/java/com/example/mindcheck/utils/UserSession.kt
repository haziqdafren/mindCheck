package com.mindcheck.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * UserSession utility for managing user authentication state and access control
 * Provides centralized methods for checking guest mode and restricting features
 */
object UserSession {

    private const val PREFS_NAME = "mindcheck_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_IS_GUEST_MODE = "is_guest_mode"
    private const val KEY_GUEST_SCREENING_COUNT = "guest_screening_count"
    private const val GUEST_USER_ID = "guest"

    /**
     * Check if current user is in guest mode
     */
    fun isGuestMode(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_GUEST_MODE, false) ||
               prefs.getString(KEY_CURRENT_USER_ID, null) == GUEST_USER_ID
    }

    /**
     * Check if user is fully authenticated (not guest)
     */
    fun isAuthenticated(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !isGuestMode(context)
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_CURRENT_USER_ID, null)
    }

    /**
     * Get guest screening count (limit to 1 for guests)
     */
    fun getGuestScreeningCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_GUEST_SCREENING_COUNT, 0)
    }

    /**
     * Increment guest screening count
     */
    fun incrementGuestScreeningCount(context: Context) {
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_GUEST_SCREENING_COUNT, 0)
        prefs.edit().putInt(KEY_GUEST_SCREENING_COUNT, currentCount + 1).apply()
    }

    /**
     * Check if guest has exceeded screening limit
     */
    fun hasGuestExceededScreeningLimit(context: Context): Boolean {
        return isGuestMode(context) && getGuestScreeningCount(context) >= 1
    }

    /**
     * Feature Access Control
     */
    enum class Feature {
        SCREENING_HISTORY,    // View past screening results
        MOOD_TRACKING,        // Log and track moods
        JOURNAL,              // Personal journal entries
        GOALS,                // Set and track goals
        ANALYTICS,            // View analytics dashboard
        UNLIMITED_SCREENING,  // Multiple screenings
        PROFILE_CUSTOMIZATION // Customize profile
    }

    /**
     * Check if user can access a specific feature
     */
    fun canAccessFeature(context: Context, feature: Feature): Boolean {
        if (!isGuestMode(context)) {
            return true // Authenticated users can access everything
        }

        // Guest mode restrictions
        return when (feature) {
            Feature.SCREENING_HISTORY,
            Feature.MOOD_TRACKING,
            Feature.JOURNAL,
            Feature.GOALS,
            Feature.ANALYTICS,
            Feature.UNLIMITED_SCREENING,
            Feature.PROFILE_CUSTOMIZATION -> false
        }
    }

    /**
     * Get feature restriction message for guests
     */
    fun getFeatureRestrictionMessage(feature: Feature): String {
        return when (feature) {
            Feature.SCREENING_HISTORY -> "Buat akun untuk menyimpan dan melihat riwayat screening kamu"
            Feature.MOOD_TRACKING -> "Mood tracking tersedia untuk member. Buat akun gratis untuk mulai tracking!"
            Feature.JOURNAL -> "Jurnal pribadi tersedia untuk member. Daftar sekarang untuk mulai menulis!"
            Feature.GOALS -> "Target wellness tersedia untuk member. Daftar untuk mulai mencapai target kamu!"
            Feature.ANALYTICS -> "Analytics lengkap tersedia untuk member. Buat akun untuk melihat progress kamu!"
            Feature.UNLIMITED_SCREENING -> "Mode tamu hanya dapat screening 1 kali. Buat akun untuk screening unlimited!"
            Feature.PROFILE_CUSTOMIZATION -> "Personalisasi profil tersedia untuk member. Daftar sekarang!"
        }
    }

    /**
     * Get feature restriction title for guests
     */
    fun getFeatureRestrictionTitle(feature: Feature): String {
        return when (feature) {
            Feature.SCREENING_HISTORY -> "Riwayat Screening"
            Feature.MOOD_TRACKING -> "Mood Tracking"
            Feature.JOURNAL -> "Jurnal Pribadi"
            Feature.GOALS -> "Target Wellness"
            Feature.ANALYTICS -> "Analytics"
            Feature.UNLIMITED_SCREENING -> "Screening Unlimited"
            Feature.PROFILE_CUSTOMIZATION -> "Personalisasi Profil"
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
