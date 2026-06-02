package com.example.trackee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MissionSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mission_success)

        val txtSukses = findViewById<TextView>(R.id.txtSuksesMisi)
        val btnHome = findViewById<Button>(R.id.btnKembaliHome)

        // Mengambil nama objek/kebiasaan yang berhasil dideteksi dari intent sebelumnya
        val namaKebiasaan = intent.getStringExtra("NAMA_KEBIASAAN") ?: "Kebiasaan"
        txtSukses.text = "Misi $namaKebiasaan\nSelesai Dilaksanakan!"

        // [OTOMATIS JALAN] Kirim data hasil deteksi objek ini ke API Laravel via Ngrok
        simpanHasilDeteksiKeLaravel(namaKebiasaan)

        // Ketika tombol diklik, arahkan kembali ke MainActivity (Home)
        btnHome.setOnClickListener {
            val intentHome = Intent(this, MainActivity::class.java)
            // FLAG_ACTIVITY_CLEAR_TOP memastikan semua halaman camera & popup tadi dihancurkan dari memori
            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intentHome)
            finish()
        }
    }

    // ==========================================================
    // FUNGSI API POST: MENYIMPAN RIWAYAT DETEKSI KE DATABASE
    // ==========================================================
    private fun simpanHasilDeteksiKeLaravel(namaObjek: String) {

        // =========================================================================
        // FIX: Ambil activeUserId dari SharedPreferences agar data history terikat ke pemiliknya
        // =========================================================================
        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val activeUserId = (sharedPref.getString("USER_ID", "0") ?: "0").toInt()

        // PERBAIKAN UTAMA: Memasukkan userId ke parameter pertama createHistory
        RetrofitClient.instance.createHistory(
            userId = activeUserId, // <-- FIX UTAMA
            objectName = namaObjek,
            status = "Selesai",
            notes = "Berhasil dideteksi via Kamera AI Trackee",
            detectedLabel = null,
            confidenceScore = null
        ).enqueue(object : Callback<CommonResponse> {

            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                if (response.isSuccessful) {
                    // Muncul notifikasi kalau data sukses masuk ke localhost/hosting Laravel via Ngrok
                    Toast.makeText(this@MissionSuccessActivity, "Riwayat tersimpan ke Laravel!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MissionSuccessActivity, "Laravel menolak menyimpan data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                // Muncul jika koneksi internet mati atau URL Ngrok tidak sinkron
                Toast.makeText(this@MissionSuccessActivity, "Gagal simpan ke server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}