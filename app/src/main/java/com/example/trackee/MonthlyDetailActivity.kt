package com.example.trackee

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MonthlyDetailActivity : AppCompatActivity() {

    private lateinit var mainContainer: LinearLayout
    private var apakahAdaDataYangDihapus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            val backgroundGradasi = ContextCompat.getDrawable(context, R.drawable.bg_gradient_default)
            if (backgroundGradasi != null) {
                background = backgroundGradasi
            } else {
                setBackgroundColor(Color.parseColor("#1AFFFFFF"))
            }
        }

        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scrollView.addView(mainContainer)
        setContentView(scrollView)

        val txtHeader = TextView(this).apply {
            text = "Isi Folder: ${HistoryActivity.namaBulanTerpilih}"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 48)
        }
        mainContainer.addView(txtHeader)

        // TANGANI BACK GESTURE DENGAN ANDROIDX MODERN (SOLUSI DEPRECATED ERROR)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (apakahAdaDataYangDihapus) {
                    setResult(RESULT_OK) // Kirim sinyal sukses kembali ke pembuka
                }
                finish()
            }
        })

        muatIsiMisiHarian()
    }

    private fun muatIsiMisiHarian() {
        if (mainContainer.childCount > 1) {
            mainContainer.removeViews(1, mainContainer.childCount - 1)
        }

        val listMisi = HistoryActivity.dataRiwayatBulanTerpilih

        if (listMisi.isEmpty()) {
            val txtKosong = TextView(this).apply {
                text = "Folder ini kosong."
                setTextColor(Color.parseColor("#757575"))
                gravity = Gravity.CENTER
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(0, 150, 0, 0)
            }
            mainContainer.addView(txtKosong)
            return
        }

        for (data in listMisi) {
            val cardView = CardView(this).apply {
                radius = 36f
                cardElevation = 2f
                setCardBackgroundColor(Color.parseColor("#FFE5E7"))
                setContentPadding(40, 40, 40, 40)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 32)
                layoutParams = params
            }

            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val txtTitle = TextView(this).apply {
                text = "Misi: ${data.object_name} (${data.status})"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#2E1A47"))
            }

            val txtNotes = TextView(this).apply {
                text = data.notes ?: "Tidak ada catatan"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, 10, 0, 12)
                setTextColor(Color.parseColor("#880E4F"))
            }

            val buttonContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val btnDelete = Button(this).apply {
                text = "Delete"
                setTextColor(Color.parseColor("#A11111"))
                setBackgroundColor(Color.TRANSPARENT)
                setAllCaps(false)
                setOnClickListener { konfirmasiHapusHarian(data.id) }
            }

            buttonContainer.addView(btnDelete)
            innerLayout.addView(txtTitle)
            innerLayout.addView(txtNotes)
            innerLayout.addView(buttonContainer)
            cardView.addView(innerLayout)

            mainContainer.addView(cardView)
        }
    }

    private fun konfirmasiHapusHarian(idData: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Riwayat")
            .setMessage("Yakin ingin menghapus item riwayat harian ini?")
            .setPositiveButton("Hapus") { dialog, _ ->
                // DIKEMBALIKAN KE Callback<CommonResponse> AGAR TIDAK ERROR TYPE MISMATCH
                RetrofitClient.instance.deleteHistory(idData).enqueue(object : Callback<CommonResponse> {
                    override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MonthlyDetailActivity, "Riwayat dihapus", Toast.LENGTH_SHORT).show()

                            HistoryActivity.dataRiwayatBulanTerpilih = HistoryActivity.dataRiwayatBulanTerpilih.filter { it.id != idData }

                            apakahAdaDataYangDihapus = true
                            setResult(RESULT_OK) // Beri tahu Activity sebelumnya bahwa ada perubahan

                            dialog.dismiss()
                            muatIsiMisiHarian()
                        } else {
                            Toast.makeText(this@MonthlyDetailActivity, "Gagal menghapus data dari server", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                        // Antisipasi jika server sukses hapus di DB tapi response json bermasalah
                        HistoryActivity.dataRiwayatBulanTerpilih = HistoryActivity.dataRiwayatBulanTerpilih.filter { it.id != idData }
                        apakahAdaDataYangDihapus = true
                        setResult(RESULT_OK)

                        Toast.makeText(this@MonthlyDetailActivity, "Tampilan diperbarui.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        muatIsiMisiHarian()
                    }
                })
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}