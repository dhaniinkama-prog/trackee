package com.example.trackee

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {

    fun setBanyakAlarmDariDatabase(context: Context, habit: HabitModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. CEK JIKA USER MENGGUNAKAN ALARM SPESIFIK INDEPENDENT (alarmDatetime tidak null)
        if (!habit.alarmDatetime.isNullOrEmpty()) {
            setAlarmSpesifikTunggal(context, alarmManager, habit)
            return
        }

        val dataJamAman = habit.hoursAndMinutes ?: ""
        if (dataJamAman.isEmpty() || dataJamAman == "Tidak Aktif") {
            Log.w("TrackEeAlarm", "Habit '${habit.name}' diabaikan dari sistem pengingat karena jam kosong atau tidak aktif.")
            return
        }

        // Lakukan pemisahan string koma setelah dipastikan data tidak null atau kosong
        val listJam: List<String> = dataJamAman.split(",")

        listJam.forEachIndexed { index, waktu ->
            try {
                val waktuBersih = waktu.trim()
                if (waktuBersih.isEmpty()) return@forEachIndexed

                val bagian = waktuBersih.split(":")
                if (bagian.size < 2) return@forEachIndexed

                val jam = bagian[0].toInt()
                val menit = bagian[1].toInt()

                // Buat konfigurasi waktu target alarm terlebih dahulu
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, jam)
                    set(Calendar.MINUTE, menit)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    // Jika jam sudah lewat untuk hari ini, majukan ke esok hari secara aman
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                // PERBAIKAN UTAMA: Cek kecocokan hari dilakukan setelah object kalender disesuaikan waktunya
                if (!apakahHariIniHarusBunyi(calendar, habit)) {
                    Log.d("TrackEeAlarm", "Alarm ${habit.name} pukul $waktuBersih dilewati karena tidak cocok dengan jadwal hari rutinitas.")
                    return@forEachIndexed
                }

                // SINKRONISASI INTENT DATA DENGAN ALARMRECEIVER (Menggunakan HISTORY_ID sesuai alur baru)
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("HISTORY_ID", habit.id) // ID Habit dilempar sebagai inisiasi awal History ID
                    putExtra("NAMA_KEBIASAAN", habit.name)
                    putExtra("MISI_BENDA", habit.missionTarget)
                    putExtra("RINGTONE_URI", habit.ringtoneUri)
                }

                // Rumus ID Unik agar tidak saling menimpa antar multi-jam dalam satu habit
                val requestCode = habit.id * 100 + index

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Menggunakan kombinasi RTC_WAKEUP & AllowWhileIdle agar alarm menembus mode sleep/doze OS Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                Log.d("TrackEeAlarm", "Alarm Berhasil Dipasang: ${habit.name} pukul $waktuBersih (Mili: ${calendar.timeInMillis})")

            } catch (e: Exception) {
                Log.e("TrackEeAlarm", "Format jam corrupt atau salah ketik: $waktu", e)
            }
        }
    }

    private fun apakahHariIniHarusBunyi(calendar: Calendar, habit: HabitModel): Boolean {
        val tipeFrekuensi = habit.frequencyType ?: "every_day"
        if (tipeFrekuensi == "every_day" || tipeFrekuensi == "flexible_week") {
            return true
        }

        if (tipeFrekuensi == "specific_days" && !habit.specificDays.isNullOrEmpty()) {
            val sdfHari = SimpleDateFormat("EEE", Locale.US)
            val namaHariIni = sdfHari.format(calendar.time) // Menghasilkan "Mon", "Tue", "Wed", dll.
            val daftarHariPilihan = habit.specificDays!!.split(",")

            return daftarHariPilihan.any { it.trim().equals(namaHariIni, ignoreCase = true) }
        }

        return true
    }

    private fun setAlarmSpesifikTunggal(context: Context, alarmManager: AlarmManager, habit: HabitModel) {
        try {
            val rawData = habit.alarmDatetime

            // LOG INVESTIGASI: Mencetak isi asli variabel ke Logcat
            Log.w("TrackEeAlarm_DEBUG", "================= INVESTIGASI START =================")
            Log.w("TrackEeAlarm_DEBUG", "Nama Habit   : ${habit.name}")
            Log.w("TrackEeAlarm_DEBUG", "Data Mentah  : '$rawData'")
            Log.w("TrackEeAlarm_DEBUG", "Panjang teks : ${rawData?.length ?: 0} karakter")
            Log.w("TrackEeAlarm_DEBUG", "Apakah null  : ${rawData == null}")
            Log.w("TrackEeAlarm_DEBUG", "=====================================================")

            if (rawData.isNullOrEmpty()) {
                Log.e("TrackEeAlarm_DEBUG", "Proses dibatalkan karena data null atau kosong!")
                return
            }

            // Bersihkan spasi tak terlihat di ujung string
            val dateTimeStr = rawData.trim()

            var finalTimeInMillis: Long = 0
            var parseSuccess = false

            // STRATEGI 1: Gunakan parser bawaan modern java.time (Android Oreo ke atas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val instant = java.time.Instant.parse(dateTimeStr)
                    finalTimeInMillis = instant.toEpochMilli()
                    parseSuccess = true
                    Log.d("TrackEeAlarm_DEBUG", "Metode 1 (Instant.parse) -> BERHASIL")
                } catch (e: Exception) {
                    Log.e("TrackEeAlarm_DEBUG", "Metode 1 Gagal: ${e.message}")
                }
            }

            // STRATEGI 2: Jika Strategi 1 gagal / OS lama, gunakan SimpleDateFormat ISO lengkap
            if (!parseSuccess) {
                try {
                    val formatISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = formatISO.parse(dateTimeStr)
                    if (date != null) {
                        finalTimeInMillis = date.time
                        parseSuccess = true
                        Log.d("TrackEeAlarm_DEBUG", "Metode 2 (SimpleDateFormat ISO) -> BERHASIL")
                    }
                } catch (e: Exception) {
                    Log.e("TrackEeAlarm_DEBUG", "Metode 2 Gagal: ${e.message}")
                }
            }

            // STRATEGI 3: Pelapis Ekstrim (Potong manual string jika format milidetik berantakan)
            if (!parseSuccess) {
                try {
                    // Jika string mengandung 'T' dan '.', paksa potong bagian milidetiknya agar bersih
                    // Contoh: "2026-06-01T07:30:00.000000Z" -> diubah jadi "2026-06-01 07:30:00"
                    if (dateTimeStr.contains("T")) {
                        val tanggal = dateTimeStr.substringBefore("T")
                        val jamPenuh = dateTimeStr.substringAfter("T").substringBefore(".")
                        val gabungBersih = "$tanggal $jamPenuh" // Hasil: "2026-06-01 07:30:00"

                        val formatBersih = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val date = formatBersih.parse(gabungBersih)
                        if (date != null) {
                            finalTimeInMillis = date.time
                            parseSuccess = true
                            Log.d("TrackEeAlarm_DEBUG", "Metode 3 (Manual Substring Clean) -> BERHASIL. String hasil potong: $gabungBersih")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TrackEeAlarm_DEBUG", "Metode 3 Gagal: ${e.message}")
                }
            }

            // Jika semua taktik gagal, lempar error spesifik agar ketahuan alasannya
            if (!parseSuccess) {
                throw java.text.ParseException("Seluruh metode gagal mengurai teks: '$dateTimeStr'", 0)
            }

            val calendar = Calendar.getInstance().apply {
                timeInMillis = finalTimeInMillis
            }

            // Jika datetime spesifik ternyata sudah lewat, otomatis majukan 1 hari agar tidak hang
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("HISTORY_ID", habit.id)
                putExtra("NAMA_KEBIASAAN", habit.name)
                putExtra("MISI_BENDA", habit.missionTarget)
                putExtra("RINGTONE_URI", habit.ringtoneUri)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habit.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }

            Log.w("TrackEeAlarm_DEBUG", "Alarm Mandiri Sukses Dipasang untuk: ${habit.name}")
            Log.w("TrackEeAlarm_DEBUG", "================= INVESTIGASI END =================\n")
        } catch (e: Exception) {
            Log.e("TrackEeAlarm", "Gagal total menyetel alarm spesifik", e)
            Log.w("TrackEeAlarm_DEBUG", "================= INVESTIGASI END WITH ERROR =================\n")
        }
    }
}