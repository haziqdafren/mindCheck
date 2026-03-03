package com.mindcheck.app.data.remote

import com.mindcheck.app.data.remote.api.MindCheckApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Client untuk koneksi ke Flask API (Machine Learning Server)
object RetrofitClient {

    // URL Flask API - ganti sesuai network yang digunakan
    // Gunakan IP Mac yang menjalankan Flask server
    private const val BASE_URL = "http://192.168.1.2:5001/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: MindCheckApiService = retrofit.create(MindCheckApiService::class.java)
}
