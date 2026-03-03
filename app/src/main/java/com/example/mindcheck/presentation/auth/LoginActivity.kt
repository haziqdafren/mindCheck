package com.mindcheck.app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.firebase.FirebaseAuthRepository
import com.mindcheck.app.data.firebase.FirebaseSyncService
import com.mindcheck.app.databinding.ActivityLoginBinding
import com.mindcheck.app.presentation.onboarding.OnboardingActivity
import com.mindcheck.app.utils.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest

/**
 * Login Activity - Firebase Integrated
 * Handles user authentication with Firebase + local Room database
 * OPTIMIZED: All heavy operations tracked and run on background threads
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val firebaseAuth by lazy { FirebaseAuthRepository() }
    private val syncService by lazy { FirebaseSyncService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PerformanceMonitor.start("LoginActivity.onCreate")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PerformanceMonitor.end("LoginActivity.onCreate", warnThresholdMs = 50)

        // Hide action bar
        supportActionBar?.hide()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGuest.setOnClickListener {
            proceedAsGuest()
        }
    }

    private fun proceedAsGuest() {
        lifecycleScope.launch {
            // All SharedPreferences operations on IO thread to avoid StrictMode violation
            val onboardingCompleted = withContext(Dispatchers.IO) {
                val prefs = getSharedPreferences("mindcheck_prefs", MODE_PRIVATE)
                val success = prefs.edit()
                    .putString("current_user_id", "guest")
                    .putBoolean("is_logged_in", false)
                    .putBoolean("is_guest_mode", true)
                    .commit() // Use commit() for immediate synchronous write

                if (success) {
                    Timber.d("Guest session saved successfully")
                } else {
                    Timber.e("Failed to save guest session!")
                }

                // Check if onboarding completed
                prefs.getBoolean("onboarding_completed_guest", false)
            }

            // Navigate on main thread
            val intent = if (onboardingCompleted) {
                Intent(this@LoginActivity, com.mindcheck.app.MainActivity::class.java)
            } else {
                Intent(this@LoginActivity, OnboardingActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                showError("Email tidak boleh kosong")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Format email tidak valid")
                return false
            }
            password.isEmpty() -> {
                showError("Password tidak boleh kosong")
                return false
            }
        }
        return true
    }

    private fun performLogin(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            PerformanceMonitor.start("LoginActivity.performLogin")

            try {
                // Try Firebase login first
                val firebaseResult = PerformanceMonitor.measureSuspend("Firebase.login") {
                    firebaseAuth.loginUser(email, password)
                }

                if (firebaseResult.isSuccess) {
                    val firebaseUser = firebaseResult.getOrNull()!!
                    val userId = firebaseUser.uid

                    // Check if user exists in local Room database (on IO thread)
                    val user = withContext(Dispatchers.IO) {
                        PerformanceMonitor.start("Database.getUserById")
                        val existingUser = db.userDao().getUserById(userId)
                        PerformanceMonitor.end("Database.getUserById")

                        if (existingUser == null) {
                            // User doesn't exist in local DB, create entry
                            val userEntity = com.mindcheck.app.data.local.entity.UserEntity(
                                userId = userId,
                                email = firebaseUser.email ?: email,
                                password = hashPassword(password), // Keep for local backup
                                nama = firebaseUser.displayName ?: "User"
                            )
                            PerformanceMonitor.start("Database.insertUser")
                            db.userDao().insertUser(userEntity)
                            PerformanceMonitor.end("Database.insertUser")
                            userEntity
                        } else {
                            // Update last active
                            db.userDao().updateLastActive(userId, System.currentTimeMillis())
                            existingUser
                        }
                    }

                    // Sync data from Firestore to local DB (async, don't block login)
                    launch(Dispatchers.IO) {
                        PerformanceMonitor.measureSuspend("Firestore.syncFromFirestore", warnThresholdMs = 500) {
                            syncService.syncFromFirestore(userId)
                        }
                    }

                    // Save user session (async)
                    withContext(Dispatchers.IO) {
                        saveUserSession(userId, isFirebaseUser = true)
                    }

                    Timber.d("Firebase login successful for user: ${user.email}")

                    PerformanceMonitor.end("LoginActivity.performLogin", warnThresholdMs = 500)

                    navigateToNext(userId)
                } else {
                    // Firebase login failed, try local Room database (offline mode)
                    Timber.w("Firebase login failed, trying local database")
                    val hashedPassword = hashPassword(password)
                    val user = db.userDao().login(email, hashedPassword)

                    if (user != null) {
                        // Update last active
                        db.userDao().updateLastActive(user.userId, System.currentTimeMillis())

                        // Save user session (local only)
                        saveUserSession(user.userId, isFirebaseUser = false)

                        Timber.d("Local login successful for user: ${user.email}")

                        navigateToNext(user.userId)
                    } else {
                        showError("Email atau password salah")
                        binding.btnLogin.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Login error")
                showError("Terjadi kesalahan: ${e.message}")
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private suspend fun navigateToNext(userId: String) {
        // Read SharedPreferences on IO thread
        val onboardingCompleted = withContext(Dispatchers.IO) {
            val prefs = getSharedPreferences("mindcheck_prefs", MODE_PRIVATE)
            prefs.getBoolean("onboarding_completed_${userId}", false)
        }

        val intent = if (onboardingCompleted) {
            Intent(this@LoginActivity, com.mindcheck.app.MainActivity::class.java)
        } else {
            Intent(this@LoginActivity, OnboardingActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun saveUserSession(userId: String, isFirebaseUser: Boolean = true) {
        val prefs = getSharedPreferences("mindcheck_prefs", MODE_PRIVATE)
        val success = prefs.edit()
            .putString("current_user_id", userId)
            .putBoolean("is_logged_in", true)
            .putBoolean("is_guest_mode", false)  // IMPORTANT: Clear guest mode flag
            .putBoolean("is_firebase_user", isFirebaseUser)
            .commit() // Use commit() for immediate synchronous write

        if (success) {
            Timber.d("User session saved successfully: $userId (guest_mode=false)")
        } else {
            Timber.e("Failed to save user session!")
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
