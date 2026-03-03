package com.mindcheck.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Repository for Cloud Firestore operations
 * Handles data sync between local Room database and cloud Firestore
 */
class FirestoreRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        // Firestore collection names
        const val COLLECTION_USERS = "users"
        const val COLLECTION_SCREENINGS = "screenings"
        const val COLLECTION_MOOD_LOGS = "mood_logs"
        const val COLLECTION_SLEEP_LOGS = "sleep_logs"
        const val COLLECTION_GRATITUDE_ENTRIES = "gratitude_entries"
        const val COLLECTION_GOALS = "goals"
        const val COLLECTION_TASKS = "tasks"
    }

    /**
     * Save or update user data to Firestore
     */
    suspend fun saveUserData(userId: String, userData: Map<String, Any>): Result<Unit> {
        return try {
            db.collection(COLLECTION_USERS)
                .document(userId)
                .set(userData, SetOptions.merge())
                .await()

            Timber.d("User data saved to Firestore: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving user data to Firestore")
            Result.failure(e)
        }
    }

    /**
     * Get user data from Firestore
     */
    suspend fun getUserData(userId: String): Result<Map<String, Any>?> {
        return try {
            val document = db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            Result.success(document.data)
        } catch (e: Exception) {
            Timber.e(e, "Error getting user data from Firestore")
            Result.failure(e)
        }
    }

    /**
     * Save document to collection
     */
    suspend fun saveDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any>
    ): Result<Unit> {
        return try {
            db.collection(collection)
                .document(documentId)
                .set(data, SetOptions.merge())
                .await()

            Timber.d("Document saved to $collection: $documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving document to $collection")
            Result.failure(e)
        }
    }

    /**
     * Get document from collection
     */
    suspend fun getDocument(
        collection: String,
        documentId: String
    ): Result<Map<String, Any>?> {
        return try {
            val document = db.collection(collection)
                .document(documentId)
                .get()
                .await()

            Result.success(document.data)
        } catch (e: Exception) {
            Timber.e(e, "Error getting document from $collection")
            Result.failure(e)
        }
    }

    /**
     * Get all documents from a collection for a specific user
     */
    suspend fun getUserDocuments(
        collection: String,
        userId: String
    ): Result<List<Map<String, Any>>> {
        return try {
            val querySnapshot = db.collection(collection)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val documents = querySnapshot.documents.mapNotNull { it.data }

            Timber.d("Retrieved ${documents.size} documents from $collection for user $userId")
            Result.success(documents)
        } catch (e: Exception) {
            Timber.e(e, "Error getting documents from $collection")
            Result.failure(e)
        }
    }

    /**
     * Delete document from collection
     */
    suspend fun deleteDocument(collection: String, documentId: String): Result<Unit> {
        return try {
            db.collection(collection)
                .document(documentId)
                .delete()
                .await()

            Timber.d("Document deleted from $collection: $documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting document from $collection")
            Result.failure(e)
        }
    }

    /**
     * Delete all user data from Firestore
     */
    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        return try {
            // Delete user document
            db.collection(COLLECTION_USERS).document(userId).delete().await()

            // Delete from each collection
            val collections = listOf(
                COLLECTION_SCREENINGS,
                COLLECTION_MOOD_LOGS,
                COLLECTION_SLEEP_LOGS,
                COLLECTION_GRATITUDE_ENTRIES,
                COLLECTION_GOALS,
                COLLECTION_TASKS
            )

            collections.forEach { collection ->
                val documents = db.collection(collection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                documents.forEach { document ->
                    document.reference.delete().await()
                }
            }

            Timber.d("All user data deleted from Firestore: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting all user data from Firestore")
            Result.failure(e)
        }
    }

    /**
     * Batch save multiple documents to a collection
     */
    suspend fun batchSaveDocuments(
        collection: String,
        documents: List<Pair<String, Map<String, Any>>>
    ): Result<Unit> {
        return try {
            val batch = db.batch()

            documents.forEach { (documentId, data) ->
                val docRef = db.collection(collection).document(documentId)
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()

            Timber.d("Batch saved ${documents.size} documents to $collection")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error batch saving documents to $collection")
            Result.failure(e)
        }
    }
}
