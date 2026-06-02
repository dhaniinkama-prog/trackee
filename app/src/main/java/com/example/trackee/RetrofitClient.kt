package com.example.trackee

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://detonate-elk-cane.ngrok-free.dev/api/"

    val instance: ApiService by lazy {
        // 1. Buat OkHttpClient untuk menyuntikkan header ngrok bypass & konfigurasi timeout
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBaru = chain.request().newBuilder()
                    // PENTING: Header ini memaksa ngrok untuk langsung melempar data ke API tanpa halaman warning
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(requestBaru)
            }
            .build()

        // 2. Bangun Retrofit menggunakan OkHttpClient di atas
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Masukkan client di sini
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}