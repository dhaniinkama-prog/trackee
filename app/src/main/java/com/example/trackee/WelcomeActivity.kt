package com.example.trackee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // 1. Ambil nama user & status rute dari SharedPreferences
        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val nameUser = sharedPref.getString("USER_NAME", "User")

        // Membaca flag: True jika user lama (dari Login), False jika user baru (dari Signup)
        val isReturningUser = sharedPref.getBoolean("IS_RETURNING_USER", false)

        // 2. Ikat ke TextView title berdasarkan ID asli XML kamu (welcomeTitle)
        val txtWelcomeTitle = findViewById<TextView>(R.id.welcomeTitle)
        if (txtWelcomeTitle != null) {
            txtWelcomeTitle.text = "Hai $nameUser"
        }

        // 3. Tombol Lanjut dengan Logika Pencabangan Rute Halaman
        findViewById<Button>(R.id.buttonContinue).setOnClickListener {
            if (isReturningUser) {
                // PENGGUNA LAMA (Jalur Login): Langsung bypass ke TodayActivity (Tanpa Kuesioner)
                val intentToday = Intent(this, TodayActivity::class.java)
                intentToday.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intentToday)
                finish()
            } else {
                // PENGGUNA BARU (Jalur Register/Signup): Jalankan alur kuesioner setelan awal
                val intentKuesioner = Intent(this, WakeUpActivity::class.java)
                startActivity(intentKuesioner)
                finish()
            }
        }
    }
}