package com.example.trackee

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    // 1. LOGIKA UTAMA: Berjalan otomatis saat jam alarm menyala tepat waktu
    override fun onReceive(context: Context, intent: Intent) {
        val namaKebiasaan = intent.getStringExtra("NAMA_KEBIASAAN") ?: "Kebiasaan Baru"
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
        val misiBenda = intent.getStringExtra("MISI_BENDA") ?: "Bottle"
        val historyId = intent.getIntExtra("HISTORY_ID", -1)

        Log.d("TrackeeAlarm", "Alarm terpicu untuk habit: $namaKebiasaan, History ID: $historyId")

        // =========================================================================
        // PERBAIKAN UTAMA 1: NYALAKAN BACKGROUND SERVICE UNTUK MEMUTAR SUARA ALARM
        // =========================================================================
        val serviceIntent = Intent(context, AlarmBackgroundService::class.java).apply {
            putExtra("RINGTONE_URI", ringtoneUri)
        }

        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("TrackeeAlarm", "Gagal menjalankan AlarmBackgroundService", e)
        }

        // =========================================================================
        // PERBAIKAN UTAMA 2: LANGSUNG BUKA MISSIONACTIVITY (SUARA TETAP BUNYI)
        // =========================================================================
        val intentMisi = Intent(context, MissionActivity::class.java).apply {
            // FLAG_ACTIVITY_NEW_TASK wajib digunakan karena memulai Activity dari luar konteks Activity (BroadcastReceiver)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAMA_KEBIASAAN", namaKebiasaan)
            putExtra("MISI_BENDA", misiBenda)
            putExtra("HISTORY_ID", historyId)
        }

        // Jalankan activity secara paksa ke layar depan HP
        context.startActivity(intentMisi)
    }

    // 2. FUNGSI STATIC HELPER: Dipanggil dari AddHabitActivity untuk mendaftarkan alarm ke OS Android
    companion object {
        fun pasangAlarmSistem(
            context: Context,
            calendarPengingat: Calendar,
            namaKebiasaan: String,
            ringtoneUri: String?, // Data URI Ringtone kustom
            misiBenda: String     // Target objek foto (Bottle, Book, dll)
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Proteksi Android 12+ mengenai aturan SCHEDULE_EXACT_ALARM
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intentIzin = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intentIzin.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intentIzin)
                    Toast.makeText(context, "Mohon berikan izin alarm tepat waktu agar pengingat berfungsi.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val intentToReceiver = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("NAMA_KEBIASAAN", namaKebiasaan)
                putExtra("RINGTONE_URI", ringtoneUri)
                putExtra("MISI_BENDA", misiBenda)
            }

            // Gunakan ID unik dari hashcode nama agar antar habit tidak saling menimpa hancur
            val requestCodeUnique = namaKebiasaan.hashCode()

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeUnique,
                intentToReceiver,
                pendingIntentFlags
            )

            // Jika jam yang dipilih sudah lewat dari waktu detik ini, majukan ke esok hari
            if (calendarPengingat.before(Calendar.getInstance())) {
                calendarPengingat.add(Calendar.DATE, 1)
            }

            // Setel alarm presisi tinggi (RTC_WAKEUP) agar menembus mode sleep HP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendarPengingat.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendarPengingat.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}