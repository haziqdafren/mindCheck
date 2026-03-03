package com.mindcheck.app.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.MainActivity
import com.mindcheck.app.databinding.ActivitySplashBinding
import com.mindcheck.app.presentation.auth.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Splash Screen Activity
 * Shows app branding and handles initial routing
 * OPTIMIZED: All I/O operations run on background thread
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDuration = 800L // 800ms - reduced for faster startup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Check session and navigate asynchronously
        lifecycleScope.launch {
            delay(splashDuration)
            navigateToNextScreen()
        }
    }

    /**
     * Check user session and navigate - ALL I/O happens on background thread
     */
    private suspend fun navigateToNextScreen() {
        val startTime = System.currentTimeMillis()

        // Read SharedPreferences on IO thread (not main thread!)
        val (isLoggedIn, currentUserId) = withContext(Dispatchers.IO) {
            val prefs = getSharedPreferences("mindcheck_prefs", MODE_PRIVATE)
            val loggedIn = prefs.getBoolean("is_logged_in", false)
            val userId = prefs.getString("current_user_id", null)
            Pair(loggedIn, userId)
        }

        val checkTime = System.currentTimeMillis() - startTime
        Timber.d("Session check completed in ${checkTime}ms")

        // Navigate on main thread
        val intent = if (isLoggedIn && currentUserId != null) {
            Timber.d("User logged in, navigating to MainActivity")
            Intent(this, MainActivity::class.java)
        } else {
            Timber.d("No user session, navigating to LoginActivity")
            Intent(this, LoginActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
