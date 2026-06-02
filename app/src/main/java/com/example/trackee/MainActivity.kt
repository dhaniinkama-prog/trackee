package com.example.trackee

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // JEMBATAN OTOMATIS: Begitu MainActivity dipanggil, langsung oper ke TodayActivity
        val intent = Intent(this, TodayActivity::class.java)
        startActivity(intent)

        // Langsung tutup MainActivity agar tidak mengendap di background layar
        finish()
    }
}