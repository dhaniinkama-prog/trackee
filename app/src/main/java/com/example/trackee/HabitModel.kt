package com.example.trackee

import com.google.gson.annotations.SerializedName

data class HabitModel(
    @SerializedName("id") var id: Int,
    @SerializedName("user_id") var userId: Int,
    @SerializedName("name") var name: String?,
    @SerializedName("target") var target: String?,

    // PERBAIKAN: Disamakan dengan $table->string('hours_and_minutes') di Laravel
    @SerializedName("hours_and_minutes") var hoursAndMinutes: String?,

    // PERBAIKAN: Disamakan dengan $table->text('ringtone_uri') di Laravel
    @SerializedName("ringtone_uri") var ringtoneUri: String? = null,

    // PERBAIKAN: Disamakan dengan $table->string('mission_target') di Laravel
    @SerializedName("mission_target") var missionTarget: String? = "bottle",

    // =========================================================================
    // FITUR FREKUENSI KALENDER & ALARM MANDIRI (SUDAH AMAN & COCOK)
    // =========================================================================
    @SerializedName("frequency_type") var frequencyType: String? = "every_day",
    @SerializedName("specific_days") var specificDays: String? = null,
    @SerializedName("flexible_count") var flexibleCount: Int? = null,
    @SerializedName("alarm_datetime") var alarmDatetime: String? = null,

    // =========================================================================
    // STATUS CHECKLIST UNTUK SINKRONISASI ADAPTER
    // =========================================================================
    @SerializedName("is_checked") var isChecked: Boolean = false
)