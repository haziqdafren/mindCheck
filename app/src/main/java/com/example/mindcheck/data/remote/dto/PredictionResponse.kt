package com.mindcheck.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    val prediction: Int,                           // 0 = No Depression, 1 = Depression
    @SerializedName("prediction_label")
    val predictionLabel: String,                   // "Tidak Ada Depresi" or "Risiko Depresi Terdeteksi"
    val confidence: Double,
    @SerializedName("risk_level")
    val riskLevel: String,
    val advice: List<String>
)
