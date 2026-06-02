package com.example.trackee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.trackee.databinding.ActivityMenuBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi binding
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Klik gambar Minum Air
        binding.ivDrink.setOnClickListener {
            simpanGoalDanPindah("drink") // Diubah ke huruf kecil agar cocok dengan backend Laravel
        }

        // Klik gambar Atur Tidur
        binding.ivSleep.setOnClickListener {
            simpanGoalDanPindah("sleep")
        }

        // Klik gambar Olahraga
        binding.ivSport.setOnClickListener {
            simpanGoalDanPindah("sport")
        }

        // Klik gambar Membaca
        binding.ivRead.setOnClickListener {
            simpanGoalDanPindah("read")
        }

        // =========================================================================
        // AKSI TOMBOL SKIP: Berpindah langsung ke halaman LoginActivity
        // =========================================================================
        binding.btnSkipMenu.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Tambahkan finish() jika kamu ingin menutup halaman menu kuesioner ini
            // sehingga user tidak bisa menekan tombol 'Back' untuk kembali ke sini.
            finish()
        }
    }

    /**
     * Fungsi untuk menyimpan preferensi, mengirim habit pilihan ke Laravel,
     * dan mengarahkannya langsung ke halaman TodayActivity
     */
    private fun simpanGoalDanPindah(goalType: String) {
        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putString("USER_GOAL", goalType.uppercase()) // Tetap simpan huruf besar di lokal jika dibutuhkan
        editor.putBoolean("IS_FIRST_RUN", false)
        editor.apply()

        // =========================================================================
        // PERBAIKAN UTAMA: Ambil String dari SharedPreferences lalu paksa ubah ke Int!
        // =========================================================================
        val userIdString = sharedPref.getString("USER_ID", "1") ?: "1"
        val userIdInt = (userIdString).toInt() // <--- Sekarang tipenya sudah resmi Int!

        // Tampilkan pesan loading singkat agar user tahu proses sedang berjalan
        Toast.makeText(this, "Menyiapkan habit kamu...", Toast.LENGTH_SHORT).show()

        // =========================================================================
        // HUBUNGKAN KE LARAVEL: Sekarang kirim userIdInt (Int), dijamin gak bentrok lagi!
        // =========================================================================
        RetrofitClient.instance.kirimRekomendasiKuesioner(userIdInt, goalType).enqueue(object : Callback<CommonResponse> {
            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                // Baik sukses ataupun gagal merespon dari server, kita arahkan user ke TodayActivity
                // agar aplikasi tidak macet/stuck di halaman menu kuesioner
                bukaTodayActivity()
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                // Tetap pindah ke halaman utama jika koneksi internet terputus
                bukaTodayActivity()
            }
        })
    }

    private fun bukaTodayActivity() {
        val intent = Intent(this, TodayActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Tutup MenuActivity agar tidak bisa di-back kembali
    }
}