package com.example.trackee

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.trackee.databinding.ActivityHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    companion object {
        var dataRiwayatBulanTerpilih: List<HistoryResponse> = listOf()
        var namaBulanTerpilih: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        muatDaftarRiwayat()
    }

    override fun onResume() {
        super.onResume()
        // Memastikan data selalu segar saat user kembali dari halaman detail
        muatDaftarRiwayat()
    }

    private fun muatDaftarRiwayat() {
        // PERBAIKAN PENTING: Bersihkan total kontainer UI agar tidak terjadi duplikasi Card saat di-refresh
        binding.containerRiwayat.removeAllViews()

        binding.txtTotalMisi.text = "0"
        binding.txtMisiSelesai.text = "0"

        val tanggalHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Ambil User ID Aktif dari SharedPreferences
        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val activeUserId = (sharedPref.getString("USER_ID", "0") ?: "0").toInt()

        Log.d("TrackeeHistory", "Memuat data history untuk User ID: $activeUserId")

        // BAGIAN 1: Hitung total semua misi milik user aktif (Dari tabel Habits)
        RetrofitClient.instance.getHabits(activeUserId, tanggalHariIni).enqueue(object : Callback<List<HabitModel>> {
            override fun onResponse(call: Call<List<HabitModel>>, response: Response<List<HabitModel>>) {
                if (response.isSuccessful && response.body() != null) {
                    val totalMisiDariToday = response.body()!!.size
                    binding.txtTotalMisi.text = totalMisiDariToday.toString()
                }
            }
            override fun onFailure(call: Call<List<HabitModel>>, t: Throwable) {
                // Biarkan default 0 jika gagal
            }
        })

        // BAGIAN 2: Ambil data riwayat milik user aktif & hitung selesai
        RetrofitClient.instance.getHistories(activeUserId).enqueue(object : Callback<List<HistoryResponse>> {
            override fun onResponse(call: Call<List<HistoryResponse>>, response: Response<List<HistoryResponse>>) {
                if (response.isSuccessful) {
                    val listDataAll = response.body()

                    if (listDataAll != null && listDataAll.isNotEmpty()) {
                        // Filter data di sisi Android untuk memastikan hanya milik user yang sedang login
                        val listData = listDataAll.filter { it.user_id == activeUserId }

                        if (listData.isEmpty()) {
                            binding.txtMisiSelesai.text = "0"
                            tampilkanPesanKosong("Belum ada folder riwayat deteksi objek.")
                            return
                        }

                        val totalSelesaiGlobal = listData.size
                        binding.txtMisiSelesai.text = totalSelesaiGlobal.toString()

                        val formatSDFTarget = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

                        // Daftar format tanggal untuk mengantisipasi fleksibilitas database Laravel
                        val formatSumberDaftar = arrayOf(
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        )

                        // Mengelompokkan data berdasarkan Bulan dan Tahun
                        val grupBerdasarkanBulan = listData.groupBy { dataItem ->
                            var namaGrupBulan = "Juni 2026"
                            val tglMentah = dataItem.date ?: dataItem.created_at ?: ""

                            for (sdf in formatSumberDaftar) {
                                try {
                                    if (tglMentah.isNotEmpty()) {
                                        val objekTanggal = sdf.parse(tglMentah)
                                        if (objekTanggal != null) {
                                            namaGrupBulan = formatSDFTarget.format(objekTanggal)
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Lanjut ke format berikutnya jika format ini gagal
                                }
                            }
                            namaGrupBulan
                        }

                        // Render folder bulan ke UI secara bersih
                        binding.containerRiwayat.removeAllViews() // Proteksi ganda sebelum me-render ulang loop
                        for ((bulanTahun, daftarRiwayatBulanIni) in grupBerdasarkanBulan) {
                            tampilkanFolderBulan(bulanTahun, daftarRiwayatBulanIni)
                        }

                    } else {
                        binding.txtMisiSelesai.text = "0"
                        tampilkanPesanKosong("Belum ada folder riwayat deteksi objek.")
                    }
                } else {
                    Log.e("TrackeeHistory", "Respon gagal dari server: ${response.code()}")
                    Toast.makeText(this@HistoryActivity, "Gagal mengambil data dari server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<HistoryResponse>>, t: Throwable) {
                Log.e("TrackeeHistory", "Koneksi Error total: ${t.message}")
                Toast.makeText(this@HistoryActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanFolderBulan(bulanTahun: String, listDataBulan: List<HistoryResponse>) {
        val konteks = this@HistoryActivity
        val totalSelesaiBulanIni = listDataBulan.size

        val kalimatFeedback = when {
            totalSelesaiBulanIni >= 20 -> "Luar biasa! Kamu konsisten banget di bulan $bulanTahun. Pertahankan prestasimu dan terus melangkah maju! 🔥"
            totalSelesaiBulanIni in 10..19 -> "Progres yang bagus! Kamu sudah berusaha keras di bulan $bulanTahun. Yuk, sedikit lagi makin konsisten! 💪"
            else -> "Bulan yang cukup berat ya? Gak apa-apa, setiap bulan adalah awal yang baru. Tetap semangat, Trackee bersamamu! ❤️"
        }

        val cardView = CardView(konteks).apply {
            radius = 36f
            cardElevation = 2f
            setCardBackgroundColor(Color.parseColor("#FFE5E7"))
            setContentPadding(44, 44, 44, 44)

            setOnClickListener {
                dataRiwayatBulanTerpilih = listDataBulan
                namaBulanTerpilih = bulanTahun
                val intent = Intent(konteks, MonthlyDetailActivity::class.java)
                konteks.startActivity(intent)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 40)
            layoutParams = params
        }

        val innerLayout = LinearLayout(konteks).apply {
            orientation = LinearLayout.VERTICAL
        }

        val txtFolderTitle = TextView(konteks).apply {
            text = bulanTahun
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#2E1A47"))
        }

        val txtRingkasan = TextView(konteks).apply {
            text = "✨ Misi Selesai Bulan Ini: $totalSelesaiBulanIni"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 10, 0, 10)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#880E4F"))
        }

        val txtFeedback = TextView(konteks).apply {
            text = kalimatFeedback
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 4, 0, 16)
            setTypeface(null, android.graphics.Typeface.ITALIC)
            setTextColor(Color.parseColor("#3E3238"))
        }

        val buttonContainer = LinearLayout(konteks).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnDeleteMonth = Button(konteks).apply {
            text = "Hapus Bulan Ini"
            setTextColor(Color.parseColor("#A11111"))
            setBackgroundColor(Color.TRANSPARENT)
            setAllCaps(false)
            setOnClickListener {
                it.cancelPendingInputEvents()
                konfirmasiHapusSatuBulan(listDataBulan)
            }
        }

        buttonContainer.addView(btnDeleteMonth)
        innerLayout.addView(txtFolderTitle)
        innerLayout.addView(txtRingkasan)
        innerLayout.addView(txtFeedback)
        innerLayout.addView(buttonContainer)

        cardView.addView(innerLayout)
        binding.containerRiwayat.addView(cardView)
    }

    private fun konfirmasiHapusSatuBulan(listDataBulan: List<HistoryResponse>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hapus Seluruh Bulan")
        builder.setMessage("Apakah kamu yakin ingin menghapus seluruh riwayat misi di bulan ini?")

        builder.setPositiveButton("Hapus Semua") { dialog, _ ->
            val totalData = listDataBulan.size
            var requestSelesaiCounter = 0

            if (totalData == 0) {
                dialog.dismiss()
                return@setPositiveButton
            }

            for (data in listDataBulan) {
                RetrofitClient.instance.deleteHistory(data.id).enqueue(object : Callback<CommonResponse> {
                    override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                        requestSelesaiCounter++
                        if (requestSelesaiCounter == totalData) {
                            Toast.makeText(this@HistoryActivity, "Seluruh riwayat bulan ini berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            muatDaftarRiwayat()
                        }
                    }

                    override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                        requestSelesaiCounter++
                        if (requestSelesaiCounter == totalData) {
                            Toast.makeText(this@HistoryActivity, "Proses selesai dengan penyesuaian tampilan.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            muatDaftarRiwayat()
                        }
                    }
                })
            }
        }
        builder.setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun tampilkanPesanKosong(pesan: String) {
        val txtKosong = TextView(this).apply {
            text = pesan
            gravity = Gravity.CENTER
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 120, 0, 0)
            setTextColor(Color.parseColor("#888888"))
        }
        binding.containerRiwayat.addView(txtKosong)
    }
}