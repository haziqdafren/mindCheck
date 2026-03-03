package com.mindcheck.app.presentation.screening

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindcheck.app.data.firebase.FirebaseAuthRepository
import com.mindcheck.app.data.firebase.FirestoreRepository
import com.mindcheck.app.data.local.database.AppDatabase
import com.mindcheck.app.data.remote.api.MindCheckApiService
import com.mindcheck.app.data.repository.ScreeningRepository

/**
 * Factory for creating ScreeningViewModel with dependencies
 */
class ScreeningViewModelFactory(
    private val apiService: MindCheckApiService,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScreeningViewModel::class.java)) {
            // Create dependencies
            val database = AppDatabase.getDatabase(context)
            val screeningDao = database.screeningDao()
            val authRepository = FirebaseAuthRepository()
            val firestoreRepository = FirestoreRepository()
            val screeningRepository = ScreeningRepository(
                screeningDao,
                authRepository,
                firestoreRepository,
                context
            )

            return ScreeningViewModel(apiService, screeningRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
