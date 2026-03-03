package com.mindcheck.app.domain.model

/**
 * Represents a single screening question with its configuration
 */
sealed class ScreeningQuestion(
    val id: String,
    val question: String,
    val description: String? = null,
    val emoji: String? = null
) {
    // Question 1: Gender
    data class Gender(
        val options: List<String> = listOf("Laki-laki", "Perempuan")
    ) : ScreeningQuestion(
        id = "gender",
        question = "Jenis kelamin kamu?",
        emoji = "👤"
    )

    // Question 2: Age
    data class Age(
        val minAge: Int = 17,
        val maxAge: Int = 30
    ) : ScreeningQuestion(
        id = "age",
        question = "Berapa usia kamu?",
        description = "Masukkan usia dalam tahun",
        emoji = "🎂"
    )

    // Question 3: Academic Pressure (1-5 scale)
    data class AcademicPressure(
        val min: Int = 1,
        val max: Int = 5,
        val labels: Map<Int, String> = mapOf(
            1 to "Sangat Rendah",
            2 to "Rendah",
            3 to "Sedang",
            4 to "Tinggi",
            5 to "Sangat Tinggi"
        )
    ) : ScreeningQuestion(
        id = "academic_pressure",
        question = "Seberapa besar tekanan akademik yang kamu rasakan?",
        description = "1 = Sangat Rendah, 5 = Sangat Tinggi",
        emoji = "📚"
    )

    // Question 4: Study Satisfaction (1-5 scale)
    data class StudySatisfaction(
        val min: Int = 1,
        val max: Int = 5,
        val labels: Map<Int, String> = mapOf(
            1 to "Sangat Tidak Puas",
            2 to "Tidak Puas",
            3 to "Cukup Puas",
            4 to "Puas",
            5 to "Sangat Puas"
        )
    ) : ScreeningQuestion(
        id = "study_satisfaction",
        question = "Seberapa puas kamu dengan studi kamu saat ini?",
        description = "1 = Sangat Tidak Puas, 5 = Sangat Puas",
        emoji = "✨"
    )

    // Question 5: Sleep Duration
    data class SleepDuration(
        val options: List<String> = listOf(
            "Kurang dari 5 jam",
            "5-6 jam",
            "7-8 jam",
            "Lebih dari 8 jam"
        )
    ) : ScreeningQuestion(
        id = "sleep_duration",
        question = "Berapa lama kamu tidur setiap malam?",
        description = "Rata-rata dalam 1 minggu terakhir",
        emoji = "😴"
    )

    // Question 6: Dietary Habits
    data class DietaryHabits(
        val options: List<String> = listOf(
            "Tidak Sehat",
            "Cukup",
            "Sehat"
        )
    ) : ScreeningQuestion(
        id = "dietary_habits",
        question = "Bagaimana pola makan kamu?",
        description = "Pilih yang paling sesuai",
        emoji = "🍽️"
    )

    // Question 7: Suicidal Thoughts (sensitive)
    data class SuicidalThoughts(
        val options: List<String> = listOf(
            "Tidak Pernah",
            "Pernah"
        )
    ) : ScreeningQuestion(
        id = "suicidal_thoughts",
        question = "Apakah kamu pernah memiliki pikiran untuk menyakiti diri sendiri?",
        description = "Informasi ini bersifat rahasia dan hanya untuk screening",
        emoji = "💭"
    )

    // Question 8: Study Hours
    data class StudyHours(
        val min: Int = 0,
        val max: Int = 16
    ) : ScreeningQuestion(
        id = "study_hours",
        question = "Berapa jam kamu belajar per hari?",
        description = "Rata-rata jam belajar harian",
        emoji = "⏰"
    )

    // Question 9: Financial Stress (1-5 scale)
    data class FinancialStress(
        val min: Int = 1,
        val max: Int = 5,
        val labels: Map<Int, String> = mapOf(
            1 to "Tidak Ada Stress",
            2 to "Sedikit Stress",
            3 to "Sedang",
            4 to "Cukup Stress",
            5 to "Sangat Stress"
        )
    ) : ScreeningQuestion(
        id = "financial_stress",
        question = "Seberapa besar stress finansial yang kamu rasakan?",
        description = "1 = Tidak Ada, 5 = Sangat Stress",
        emoji = "💰"
    )

    // Question 10: Family History
    data class FamilyHistory(
        val options: List<String> = listOf(
            "Tidak",
            "Ya"
        )
    ) : ScreeningQuestion(
        id = "family_history",
        question = "Apakah ada riwayat depresi dalam keluarga?",
        description = "Orang tua atau saudara kandung",
        emoji = "👨‍👩‍👧‍👦"
    )
}

/**
 * Container for all screening questions in order
 */
object ScreeningQuestions {
    val questions: List<ScreeningQuestion> = listOf(
        ScreeningQuestion.Gender(),
        ScreeningQuestion.Age(),
        ScreeningQuestion.AcademicPressure(),
        ScreeningQuestion.StudySatisfaction(),
        ScreeningQuestion.SleepDuration(),
        ScreeningQuestion.DietaryHabits(),
        ScreeningQuestion.SuicidalThoughts(),
        ScreeningQuestion.StudyHours(),
        ScreeningQuestion.FinancialStress(),
        ScreeningQuestion.FamilyHistory()
    )

    val totalQuestions: Int = questions.size
}

/**
 * User's answers to screening questions
 */
data class ScreeningAnswers(
    var gender: String? = null,
    var age: Int? = null,
    var academicPressure: Int? = null,
    var studySatisfaction: Int? = null,
    var sleepDuration: String? = null,
    var dietaryHabits: String? = null,
    var suicidalThoughts: String? = null,
    var studyHours: Int? = null,
    var financialStress: Int? = null,
    var familyHistory: String? = null
) {
    /**
     * Check if all required fields are answered
     */
    fun isComplete(): Boolean {
        return gender != null &&
                age != null &&
                academicPressure != null &&
                studySatisfaction != null &&
                sleepDuration != null &&
                dietaryHabits != null &&
                suicidalThoughts != null &&
                studyHours != null &&
                financialStress != null &&
                familyHistory != null
    }

    /**
     * Get current progress percentage
     */
    fun getProgress(): Float {
        val answeredCount = listOf(
            gender, age, academicPressure, studySatisfaction, sleepDuration,
            dietaryHabits, suicidalThoughts, studyHours, financialStress, familyHistory
        ).count { it != null }

        return answeredCount.toFloat() / 10f
    }

    /**
     * Convert to API request format
     */
    fun toApiRequest(): Map<String, Any> {
        return mapOf(
            "gender" to (gender ?: ""),
            "age" to (age ?: 0),
            "academic_pressure" to (academicPressure ?: 0),
            "study_satisfaction" to (studySatisfaction ?: 0),
            "sleep_duration" to (sleepDuration ?: ""),
            "dietary_habits" to (dietaryHabits ?: ""),
            "suicidal_thoughts" to (suicidalThoughts ?: ""),
            "study_hours" to (studyHours ?: 0),
            "financial_stress" to (financialStress ?: 0),
            "family_history" to (familyHistory ?: "")
        )
    }
}
