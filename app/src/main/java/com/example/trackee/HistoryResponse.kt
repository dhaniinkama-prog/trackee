package com.example.trackee

import com.google.gson.annotations.SerializedName

data class HistoryResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val user_id: Int,

    @SerializedName("habit_id") // Tambahkan ini karena riwayat merujuk ke id habit tertentu
    val habit_id: Int?,

    @SerializedName("object_name")
    val object_name: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("date")
    val date: String?,

    @SerializedName("detected_label")
    val detected_label: String?,

    @SerializedName("confidence_score")
    val confidence_score: Double?, // Menggunakan Double agar lebih aman menangkap nilai desimal JSON

    @SerializedName("created_at")
    val created_at: String?,

    @SerializedName("updated_at")
    val updated_at: String?
)