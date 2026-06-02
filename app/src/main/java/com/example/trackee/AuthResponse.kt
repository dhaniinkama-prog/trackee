package com.example.trackee

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String,

    // PERBAIKAN: Tambahkan ini agar variabel access_token dikenali oleh Android Studio!
    @SerializedName("access_token") val access_token: String?,

    // Cadangan pembacaan data user
    @SerializedName("user") val user: UserModel?,
    @SerializedName("data") val dataUser: UserModel?
)

data class UserModel(
    // ID dibuat String agar aman dan fleksibel
    @SerializedName("id") val id: String,

    // DIKUNCI KE "name" sesuai bawaan asli database Laravel kamu
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String
)