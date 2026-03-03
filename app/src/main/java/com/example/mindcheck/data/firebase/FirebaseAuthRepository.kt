package com.mindcheck.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Repository for Firebase Authentication operations
 * Handles user authentication, registration, and session management
 */
class FirebaseAuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Get currently logged in Firebase user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Register new user with email and password
     */
    suspend fun registerUser(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            // Update user profile with name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            Timber.d("Firebase user registered: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Firebase registration error")
            Result.failure(e)
        }
    }

    /**
     * Login user with email and password
     */
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Login failed")

            Timber.d("Firebase user logged in: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Firebase login error")
            Result.failure(e)
        }
    }

    /**
     * Logout current user
     */
    fun logoutUser() {
        auth.signOut()
        Timber.d("Firebase user logged out")
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Timber.d("Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending password reset email")
            Result.failure(e)
        }
    }

    /**
     * Delete current user account
     */
    suspend fun deleteUser(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            user.delete().await()
            Timber.d("Firebase user deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting user")
            Result.failure(e)
        }
    }

    /**
     * Update user email
     */
    suspend fun updateUserEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            user.updateEmail(newEmail).await()
            Timber.d("User email updated to: $newEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating email")
            Result.failure(e)
        }
    }

    /**
     * Update user password
     */
    suspend fun updateUserPassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            user.updatePassword(newPassword).await()
            Timber.d("User password updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating password")
            Result.failure(e)
        }
    }

    /**
     * Get Firebase user ID (UID)
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
}
