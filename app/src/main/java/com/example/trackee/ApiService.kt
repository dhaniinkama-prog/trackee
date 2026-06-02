package com.example.trackee

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Field
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ApiService {

    // =========================================================================
    // === AUTENTIKASI ===
    // =========================================================================
    @FormUrlEncoded
    @POST("login")
    fun loginUser(@FieldMap params: Map<String, String>): Call<AuthResponse>

    @FormUrlEncoded
    @POST("register")
    fun registerUser(@FieldMap params: Map<String, String>): Call<AuthResponse>

    // =========================================================================
    // === CRUD HISTORY TRACKEE ===
    // =========================================================================

    @GET("history")
    fun getHistories(
        @Query("user_id") userId: Int
    ): Call<List<HistoryResponse>>

    @FormUrlEncoded
    @POST("history")
    fun createHistory(
        @Field("user_id") userId: Int,
        @Field("object_name") objectName: String,
        @Field("status") status: String,
        @Field("notes") notes: String,
        @Field("detected_label") detectedLabel: String?,
        @Field("confidence_score") confidenceScore: Float?
    ): Call<CommonResponse>

    @FormUrlEncoded
    @PUT("history/{id}")
    fun updateHistory(
        @Path("id") id: Int,
        @Field("notes") notes: String
    ): Call<CommonResponse>

    @DELETE("history/{id}")
    fun deleteHistory(
        @Path("id") id: Int
    ): Call<CommonResponse>

    // =========================================================================
    // === CRUD HABITS ===
    // =========================================================================

    @GET("habits")
    fun getHabits(
        @Query("user_id") userId: Int,
        @Query("date") date: String
    ): Call<List<HabitModel>>

    @FormUrlEncoded
    @POST("habits")
    fun createHabit(
        @Field("user_id") userId: Int,
        @Field("name") name: String,
        @Field("target") target: String,
        @Field("frequency_type") frequencyType: String,
        @Field("specific_days") specificDays: String?,
        @Field("flexible_count") flexibleCount: Int?,
        @Field("alarm_datetime") alarmDatetime: String?,
        @Field("hours_and_minutes") hoursAndMinutes: String?,
        @Field("ringtone_uri") ringtoneUri: String?,
        @Field("mission_target") missionTarget: String?
    ): Call<HabitModel>

    @FormUrlEncoded
    @PUT("habits/{id}")
    fun updateHabit(
        @Path("id") id: Int,
        @Field("name") name: String,
        @Field("target") target: String,
        @Field("frequency_type") frequencyType: String,
        @Field("specific_days") specificDays: String?,
        @Field("flexible_count") flexibleCount: Int?,
        @Field("alarm_datetime") alarmDatetime: String?,
        @Field("hours_and_minutes") hoursAndMinutes: String?,
        @Field("ringtone_uri") ringtoneUri: String?,
        @Field("mission_target") missionTarget: String?
    ): Call<CommonResponse>

    // FIX UTAMA HAPUS: Menggunakan POST rute alternatif kebal blokir ngrok/provider
    @POST("habits/delete/{id}")
    fun deleteHabit(
        @Path("id") id: Int
    ): Call<ResponseBody>

    // =========================================================================
    // === REKOMENDASI KUESIONER ===
    // =========================================================================
    @FormUrlEncoded
    @POST("habits/rekomendasi")
    fun kirimRekomendasiKuesioner(
        @Field("user_id") userId: Int,
        @Field("category") category: String
    ): Call<CommonResponse>

    // =========================================================================
    // === PENGATURAN AKUN USER ===
    // =========================================================================

    @FormUrlEncoded
    @POST("user/change-password")
    fun changePassword(
        @Field("user_id") userId: Int,
        @Field("old_password") oldPassword: String,
        @Field("new_password") newPassword: String
    ): Call<CommonResponse>

    @Multipart
    @POST("user/upload-profile")
    fun uploadProfilePicture(
        @Part("user_id") userId: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<com.google.gson.JsonObject>

    // =========================================================================
    // === TOTAL SINKRONISASI CHECKLIST & IMAGE VERIFICATION ===
    // =========================================================================

    // FIX UTAMA: Jika kamu mengirimkan file gambar mentah lewat Kamera/Gallery Android ke Laravel,
    // gunakan rute @Multipart ini agar fungsi storeImageDetection di Laravel tidak mengembalikan error null.
    @Multipart
    @POST("habits/detect-image")
    fun storeImageDetectionMultipart(
        @Part("habit_id") habitId: RequestBody,
        @Part("date") date: RequestBody,
        @Part("detected_label") detectedLabel: RequestBody,
        @Part("confidence_score") confidenceScore: RequestBody,
        @Part("history_id") historyId: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Call<CommonResponse>

    // Rute alternatif FormUrlEncoded jika Android hanya mengirimkan string path/URL gambar murni tanpa file fisik
    @FormUrlEncoded
    @POST("habits/detect-image")
    fun storeImageDetection(
        @Field("habit_id") habitId: Int,
        @Field("date") date: String,
        @Field("detected_label") detectedLabel: String,
        @Field("confidence_score") confidenceScore: Float,
        @Field("image_path") imagePath: String?,
        @Field("history_id") historyId: Int?
    ): Call<CommonResponse>

    @FormUrlEncoded
    @POST("habits/toggle-checklist")
    fun toggleChecklist(
        @Field("habit_id") habitId: Int,
        @Field("date") date: String,
        @Field("is_checked") isChecked: Int
    ): Call<CommonResponse>
}