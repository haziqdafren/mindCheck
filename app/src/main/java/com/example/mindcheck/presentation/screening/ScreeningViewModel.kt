package com.mindcheck.app.presentation.screening

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindcheck.app.data.remote.api.MindCheckApiService
import com.mindcheck.app.data.remote.dto.PredictionRequest
import com.mindcheck.app.data.remote.dto.PredictionResponse
import com.mindcheck.app.domain.model.ScreeningAnswers
import com.mindcheck.app.domain.model.ScreeningQuestion
import com.mindcheck.app.domain.model.ScreeningQuestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel untuk mengelola kuesioner screening dan komunikasi dengan API ML
class ScreeningViewModel(
    private val apiService: MindCheckApiService,
    private val screeningRepository: com.mindcheck.app.data.repository.ScreeningRepository
) : ViewModel() {

    // Daftar semua pertanyaan PHQ-9
    val questions: List<ScreeningQuestion> = ScreeningQuestions.questions

    // Index pertanyaan yang sedang ditampilkan
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Jawaban pengguna untuk semua pertanyaan
    private val _answers = MutableStateFlow(ScreeningAnswers())
    val answers: StateFlow<ScreeningAnswers> = _answers.asStateFlow()

    // Persentase progress penyelesaian kuesioner
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // Hasil prediksi dari ML model (Flask API)
    private val _predictionResult: MutableStateFlow<PredictionResponse?> = MutableStateFlow(null)
    val predictionResult: StateFlow<PredictionResponse?> = _predictionResult.asStateFlow()

    // Status loading saat menunggu API response
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Pesan error jika API call gagal
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // DON'T clear results in init - this causes issues when ResultFragment accesses the ViewModel
        // Results will be cleared when user starts a new screening via startNewScreening()
        Log.d("ScreeningViewModel", "ViewModel initialized. Current predictionResult: ${_predictionResult.value}")
    }

    /**
     * Get current question
     */
    fun getCurrentQuestion(): ScreeningQuestion {
        return questions[_currentQuestionIndex.value]
    }

    /**
     * Move to next question
     */
    fun nextQuestion() {
        if (_currentQuestionIndex.value < questions.size - 1) {
            _currentQuestionIndex.value += 1
            updateProgress()
        }
    }

    /**
     * Move to previous question
     */
    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
            updateProgress()
        }
    }

    /**
     * Check if current question is answered
     */
    fun isCurrentQuestionAnswered(): Boolean {
        val currentQuestion = getCurrentQuestion()
        val currentAnswers = _answers.value

        return when (currentQuestion) {
            is ScreeningQuestion.Gender -> currentAnswers.gender != null
            is ScreeningQuestion.Age -> currentAnswers.age != null
            is ScreeningQuestion.AcademicPressure -> currentAnswers.academicPressure != null
            is ScreeningQuestion.StudySatisfaction -> currentAnswers.studySatisfaction != null
            is ScreeningQuestion.SleepDuration -> currentAnswers.sleepDuration != null
            is ScreeningQuestion.DietaryHabits -> currentAnswers.dietaryHabits != null
            is ScreeningQuestion.SuicidalThoughts -> currentAnswers.suicidalThoughts != null
            is ScreeningQuestion.StudyHours -> currentAnswers.studyHours != null
            is ScreeningQuestion.FinancialStress -> currentAnswers.financialStress != null
            is ScreeningQuestion.FamilyHistory -> currentAnswers.familyHistory != null
        }
    }

    /**
     * Save answer for current question
     */
    fun saveAnswer(answer: Any) {
        val currentQuestion = getCurrentQuestion()
        val updatedAnswers = _answers.value.copy()

        when (currentQuestion) {
            is ScreeningQuestion.Gender -> updatedAnswers.gender = answer as String
            is ScreeningQuestion.Age -> updatedAnswers.age = answer as Int
            is ScreeningQuestion.AcademicPressure -> updatedAnswers.academicPressure = answer as Int
            is ScreeningQuestion.StudySatisfaction -> updatedAnswers.studySatisfaction = answer as Int
            is ScreeningQuestion.SleepDuration -> updatedAnswers.sleepDuration = answer as String
            is ScreeningQuestion.DietaryHabits -> updatedAnswers.dietaryHabits = answer as String
            is ScreeningQuestion.SuicidalThoughts -> updatedAnswers.suicidalThoughts = answer as String
            is ScreeningQuestion.StudyHours -> updatedAnswers.studyHours = answer as Int
            is ScreeningQuestion.FinancialStress -> updatedAnswers.financialStress = answer as Int
            is ScreeningQuestion.FamilyHistory -> updatedAnswers.familyHistory = answer as String
        }

        _answers.value = updatedAnswers
        updateProgress()

        Log.d("ScreeningViewModel", "Saved answer: $answer for question: ${currentQuestion.question}")
        Log.d("ScreeningViewModel", "Is answered now: ${isCurrentQuestionAnswered()}")
    }

    /**
     * Get current answer for the question
     */
    fun getCurrentAnswer(): Any? {
        val currentQuestion = getCurrentQuestion()
        val currentAnswers = _answers.value

        return when (currentQuestion) {
            is ScreeningQuestion.Gender -> currentAnswers.gender
            is ScreeningQuestion.Age -> currentAnswers.age
            is ScreeningQuestion.AcademicPressure -> currentAnswers.academicPressure
            is ScreeningQuestion.StudySatisfaction -> currentAnswers.studySatisfaction
            is ScreeningQuestion.SleepDuration -> currentAnswers.sleepDuration
            is ScreeningQuestion.DietaryHabits -> currentAnswers.dietaryHabits
            is ScreeningQuestion.SuicidalThoughts -> currentAnswers.suicidalThoughts
            is ScreeningQuestion.StudyHours -> currentAnswers.studyHours
            is ScreeningQuestion.FinancialStress -> currentAnswers.financialStress
            is ScreeningQuestion.FamilyHistory -> currentAnswers.familyHistory
        }
    }

    /**
     * Update progress based on answered questions
     */
    private fun updateProgress() {
        _progress.value = _answers.value.getProgress()
    }

    /**
     * Submit screening answers to API
     */
    fun submitScreening() {
        if (!_answers.value.isComplete()) {
            _errorMessage.value = "Mohon jawab semua pertanyaan terlebih dahulu"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val request = PredictionRequest(
                    gender = _answers.value.gender!!,
                    age = _answers.value.age!!,
                    academic_pressure = _answers.value.academicPressure!!,
                    study_satisfaction = _answers.value.studySatisfaction!!,
                    sleep_duration = _answers.value.sleepDuration!!,
                    dietary_habits = _answers.value.dietaryHabits!!,
                    suicidal_thoughts = _answers.value.suicidalThoughts!!,
                    study_hours = _answers.value.studyHours!!,
                    financial_stress = _answers.value.financialStress!!,
                    family_history = _answers.value.familyHistory!!
                )

                val response = apiService.predict(request)
                Log.d("ScreeningViewModel", "API Response received: prediction=${response.prediction}, confidence=${response.confidence}, risk=${response.riskLevel}, advice count=${response.advice.size}")
                _predictionResult.value = response
                Log.d("ScreeningViewModel", "Stored result in StateFlow. Current value: ${_predictionResult.value}")

                // Save to local database and sync to cloud
                val saveResult = screeningRepository.saveScreeningResult(_answers.value, response)
                if (saveResult.isSuccess) {
                    Log.d("ScreeningViewModel", "Screening saved to database successfully with ID: ${saveResult.getOrNull()}")
                } else {
                    Log.e("ScreeningViewModel", "Failed to save screening to database: ${saveResult.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e("ScreeningViewModel", "API call failed", e)
                _errorMessage.value = "Gagal terhubung ke server: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset screening
     */
    fun resetScreening() {
        _currentQuestionIndex.value = 0
        _answers.value = ScreeningAnswers()
        _progress.value = 0f
        _predictionResult.value = null
        _errorMessage.value = null
    }

    /**
     * Start new screening - clears all previous data including results
     * Call this when user starts a new screening from dashboard
     */
    fun startNewScreening() {
        _currentQuestionIndex.value = 0
        _answers.value = ScreeningAnswers()
        _progress.value = 0f
        _predictionResult.value = null  // Clear old results to prevent caching
        _errorMessage.value = null
        _isLoading.value = false
        Log.d("ScreeningViewModel", "Started new screening - cleared all data including previous results")
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Is this the first question?
     */
    fun isFirstQuestion(): Boolean {
        return _currentQuestionIndex.value == 0
    }

    /**
     * Is this the last question?
     */
    fun isLastQuestion(): Boolean {
        return _currentQuestionIndex.value == questions.size - 1
    }

    /**
     * Get progress text (e.g., "1 dari 10")
     */
    fun getProgressText(): String {
        return "${_currentQuestionIndex.value + 1} dari ${questions.size}"
    }

    /**
     * Get progress percentage as integer
     */
    fun getProgressPercentage(): Int {
        return (_progress.value * 100).toInt()
    }
}
