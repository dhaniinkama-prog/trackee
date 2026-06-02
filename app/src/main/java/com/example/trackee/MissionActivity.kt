package com.example.trackee

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mission)

        // Inisialisasi komponen view dari layout activity_mission.xml
        val txtJudul = findViewById<TextView>(R.id.txtJudulMisi)
        val btnMulaiCari = findViewById<Button>(R.id.btnSelesaiMisi)
        val btnCloseMisi = findViewById<ImageButton>(R.id.btnCloseMisi)

        // Menangkap data yang dikirim oleh AlarmReceiver
        val namaKebiasaan = intent.getStringExtra("NAMA_KEBIASAAN") ?: "Kebiasaan"
        val historyId = intent.getIntExtra("HISTORY_ID", -1)

        // PERBAIKAN: Tangkap target objek (misal: Bottle, Cup, Book) agar kamera tahu apa yang dicari
        val misiBenda = intent.getStringExtra("MISI_BENDA") ?: "Bottle"

        txtJudul.text = "Waktunya melakukan:\n$namaKebiasaan"

        // Ketika tombol "Buka Kamera Misi" diklik, pindah ke halaman kamera deteksi objek
        btnMulaiCari.setOnClickListener {
            val intentKamera = Intent(this, DetectObjectActivity::class.java).apply {
                putExtra("NAMA_KEBIASAAN", namaKebiasaan)
                putExtra("HISTORY_ID", historyId)

                // DISELARASKAN: Menggunakan key "MISSION_TARGET" agar terbaca sempurna oleh DetectObjectActivity
                putExtra("MISSION_TARGET", misiBenda)
            }
            startActivity(intentKamera)
            finish() // Menutup halaman pembuka ini agar user tidak bisa kembali (back) ke sini lagi
        }

        // Ketika tombol silang (X) diklik, matikan ringtone alarm dan tutup halaman
        btnCloseMisi.setOnClickListener {
            // PERBAIKAN: Hentikan Service suara secara paksa agar alarm tidak bunyi terus setelah diclose
            stopService(Intent(this, AlarmBackgroundService::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Antisipasi jika user menekan tombol back bawaan HP, matikan juga suaranya
        stopService(Intent(this, AlarmBackgroundService::class.java))
    }
}