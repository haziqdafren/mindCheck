package com.mindcheck.app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.local.entity.UserEntity
import com.mindcheck.app.data.firebase.FirebaseAuthRepository
import com.mindcheck.app.data.firebase.FirestoreRepository
import com.mindcheck.app.databinding.ActivityRegisterBinding
import com.mindcheck.app.presentation.onboarding.OnboardingActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest

/**
 * Register Activity - Firebase Integrated
 * Handles user registration with Firebase + local Room database
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val firebaseAuth by lazy { FirebaseAuthRepository() }
    private val firestoreRepository by lazy { FirestoreRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                performRegister(name, email, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish() // Go back to login
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        when {
            name.isEmpty() -> {
                showError("Nama tidak boleh kosong")
                return false
            }
            name.length < 2 -> {
                showError("Nama terlalu pendek")
                return false
            }
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
            password.length < 6 -> {
                showError("Password minimal 6 karakter")
                return false
            }
            password != confirmPassword -> {
                showError("Password tidak cocok")
                return false
            }
        }
        return true
    }

    private fun performRegister(name: String, email: String, password: String) {
        binding.btnRegister.isEnabled = false
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Check if email already exists in local database
                val existingUser = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    db.userDao().getUserByEmail(email)
                }

                if (existingUser != null) {
                    showError("Email sudah terdaftar")
                    binding.btnRegister.isEnabled = true
                    return@launch
                }

                // Try Firebase registration first
                val firebaseResult = firebaseAuth.registerUser(email, password, name)

                if (firebaseResult.isSuccess) {
                    // Firebase registration successful
                    val firebaseUser = firebaseResult.getOrNull()!!
                    val userId = firebaseUser.uid

                    // Create user in local Room database
                    val hashedPassword = hashPassword(password)
                    val user = UserEntity(
                        userId = userId, // Use Firebase UID as primary key
                        email = email,
                        password = hashedPassword,
                        nama = name,
                        gender = null,
                        usia = null,
                        universitas = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.userDao().insertUser(user)
                    }

                    // Save user data to Firestore - SYNCHRONOUS to ensure it completes
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val userData = mapOf(
                                "userId" to userId,
                                "nama" to name,
                                "email" to email,
                                "createdAt" to System.currentTimeMillis(),
                                "lastActive" to System.currentTimeMillis()
                            )
                            val result = firestoreRepository.saveUserData(userId, userData)
                            if (result.isSuccess) {
                                Timber.d("✅ User data saved to Firestore successfully")
                            } else {
                                Timber.w("⚠️ Firestore save failed: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "⚠️ Firestore save failed, but local registration succeeded")
                        }
                    }

                    // Save user session on IO thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        saveUserSession(userId, isFirebaseUser = true)
                    }

                    Timber.d("Firebase registration successful for user: $email")

                    // Navigate to onboarding
                    val intent = Intent(this@RegisterActivity, OnboardingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    // Firebase registration failed - FALLBACK to local-only registration
                    val error = firebaseResult.exceptionOrNull()
                    Timber.w(error, "Firebase registration failed, falling back to local registration")

                    // Check if it's a configuration error (not user error)
                    val isConfigError = error?.message?.contains("CONFIGURATION_NOT_FOUND") == true ||
                                       error?.message?.contains("AppCheckProvider") == true

                    if (isConfigError) {
                        // Firebase not configured properly - use local registration
                        Timber.i("Firebase not configured, creating local-only user")

                        val userId = java.util.UUID.randomUUID().toString()
                        val hashedPassword = hashPassword(password)
                        val user = UserEntity(
                            userId = userId,
                            email = email,
                            password = hashedPassword,
                            nama = name,
                            gender = null,
                            usia = null,
                            universitas = null,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            db.userDao().insertUser(user)
                        }

                        // Save user session on IO thread
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            saveUserSession(userId, isFirebaseUser = false)
                        }

                        Timber.d("Local registration successful for user: $email")

                        // Navigate to onboarding
                        val intent = Intent(this@RegisterActivity, OnboardingActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Show user-specific error
                        val errorMessage = when {
                            error?.message?.contains("email address is already in use") == true ->
                                "Email sudah terdaftar"
                            error?.message?.contains("network") == true ->
                                "Tidak ada koneksi internet. Silakan coba lagi."
                            else ->
                                "Pendaftaran gagal: ${error?.message}"
                        }

                        showError(errorMessage)
                        binding.btnRegister.isEnabled = true
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Registration error")
                showError("Terjadi kesalahan: ${e.message}")
                binding.btnRegister.isEnabled = true
            }
        }
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
