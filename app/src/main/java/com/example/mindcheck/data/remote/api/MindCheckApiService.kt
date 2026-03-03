package com.mindcheck.app.data.remote.api

import com.mindcheck.app.data.remote.dto.PredictionRequest
import com.mindcheck.app.data.remote.dto.PredictionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MindCheckApiService {

    @GET("health")
    suspend fun healthCheck(): Map<String, String>

    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): PredictionResponse
}
