package com.mindcheck.app.data.remote.dto

data class PredictionRequest(
    val gender: String,
    val age: Int,
    val academic_pressure: Int,
    val study_satisfaction: Int,
    val sleep_duration: String,
    val dietary_habits: String,
    val suicidal_thoughts: String,
    val study_hours: Int,
    val financial_stress: Int,
    val family_history: String
)
