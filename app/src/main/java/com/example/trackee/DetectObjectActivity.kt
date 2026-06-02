package com.example.trackee

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DetectObjectActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var txtInstruksi: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var namaKebiasaan: String = "Benda"
    private var historyId: Int = -1

    // VARIABEL BARU: Menyimpan target instruksi hasil kocokan dari database (Default: "Bottle")
    private var targetMisiBenda: String = "Bottle"

    // Status arah kamera (Default: Kamera Belakang)
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    // Status flag agar tidak terjadi pengiriman API berkali-kali secara bersamaan
    private var isApiProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_object)

        // =========================================================================
        // PERBAIKAN: MATIKAN SUARA ALARM BEGITU USER MASUK KE HALAMAN KAMERA
        // =========================================================================
        try {
            stopService(Intent(this, AlarmBackgroundService::class.java))
            Log.d("TrackeeCamera", "AlarmBackgroundService berhasil dimatikan.")
        } catch (e: Exception) {
            Log.e("TrackeeCamera", "Gagal mematikan service suara", e)
        }

        viewFinder = findViewById(R.id.viewFinder)
        txtInstruksi = findViewById(R.id.txtInstruksiKamera)
        val btnSimulasi = findViewById<Button>(R.id.btnSimulasiSukses)
        val btnFlipCamera = findViewById<CardView>(R.id.btnFlipCamera)

        namaKebiasaan = intent.getStringExtra("NAMA_KEBIASAAN") ?: "Benda"
        historyId = intent.getIntExtra("HISTORY_ID", -1)

        // MENANGKAP OPERAN DATA MISI BENDA DARI MISSION ACTIVITY
        targetMisiBenda = intent.getStringExtra("MISSION_TARGET") ?: "Bottle"

        // Menerjemahkan teks target ke bahasa Indonesia agar user tidak kebingungan saat membaca instruksi layar
        val namaBendaIndonesia = when (targetMisiBenda.lowercase()) {
            "bottle" -> "Botol Minum / Tumbler"
            "cup" -> "Cangkir / Gelas Kopi"
            "book" -> "Buku / Novel / Catatan"
            "fashion good" -> "Handuk / Pakaian / Kain"
            "home good" -> "Peralatan Rumah (Sisir/Kotak Pensil)"
            "tableware" -> "Piring / Mangkok / Sendok"
            else -> "Objek Terpilih"
        }

        // Mengubah Teks Instruksi secara dinamis sesuai benda acak yang ditargetkan sistem
        txtInstruksi.text = "Misi: Carilah [$namaBendaIndonesia]\nDekatkan kamera ke objek tersebut untuk verifikasi!"

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Logic klik untuk membalikkan kamera depan <-> belakang
        btnFlipCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }

        // Tombol bypass/simulasi
        btnSimulasi.setOnClickListener {
            misiBerhasil(detectedLabel = "$targetMisiBenda (Bypass)", confidenceScore = 1.0f)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // Setup ML Kit Object Detector ( STREAM_MODE agar deteksi real-time berjalan lancar )
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification() // Tetap dinyalakan untuk membaca label jika ada
                .build()
            val objectDetector = ObjectDetection.getClient(options)

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy, objectDetector)
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("DetectObject", "Gagal memulai kamera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy, objectDetector: com.google.mlkit.vision.objects.ObjectDetector) {
        if (isApiProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    if (detectedObjects.isNotEmpty()) {
                        val obj = detectedObjects.first()

                        // Mengambil label text bawaan ML Kit jika terdeteksi
                        val labelTeks = if (obj.labels.isNotEmpty()) obj.labels.first().text else "Unknown"
                        val skorAkurasi = if (obj.labels.isNotEmpty()) obj.labels.first().confidence else 0.0f

                        // Jika akurasi deteksi di atas 35%
                        if (skorAkurasi >= 0.35f) {
                            // COCOKKAN: Apakah teks label ML Kit mengandung kata kunci targetMisiBenda kita?
                            if (labelTeks.contains(targetMisiBenda, ignoreCase = true)) {
                                runOnUiThread {
                                    misiBerhasil(detectedLabel = labelTeks, confidenceScore = skorAkurasi)
                                }
                            } else {
                                Log.d("MLKit-Scan", "Melihat objek: $labelTeks, tapi misi mencari: $targetMisiBenda")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit-AI", "Gagal mendeteksi objek", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun misiBerhasil(detectedLabel: String, confidenceScore: Float) {
        if (isApiProcessing) return
        isApiProcessing = true

        // Hentikan penangkapan kamera agar tidak menembak API berulang kali
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(this).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) { /** Diabaikan **/ }

        Toast.makeText(this, "Memverifikasi hasil ke server...", Toast.LENGTH_SHORT).show()

        val defaultImagePath = "assets/detections/captured_object.jpg"

        // SINKRONISASI DATA: Dapatkan tanggal hari ini berformat YYYY-MM-DD untuk parameter ApiService
        val tanggalHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Menangkap data HABIT_ID yang dikirim dari halaman checklist sebelumnya (jika kosong gunakan historyId sebagai fallback)
        val habitIdDariIntent = intent.getIntExtra("HABIT_ID", historyId)

        // =========================================================================
        // PERBAIKAN: Masukkan confidenceScore secara langsung sebagai Float
        // Tidak perlu diubah ke String lagi agar match dengan ApiService interface kita.
        // =========================================================================
        RetrofitClient.instance.storeImageDetection(
            habitId = habitIdDariIntent,
            date = tanggalHariIni,
            detectedLabel = detectedLabel,
            confidenceScore = confidenceScore, // <-- SEKARANG MENGGUNAKAN FLOAT (FIXED ERROR)
            imagePath = defaultImagePath,
            historyId = historyId
        ).enqueue(object : Callback<CommonResponse> {
            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                if (response.isSuccessful) {
                    Log.d("API-DETECTION-POST", "Data deteksi acak sukses masuk database!")
                } else {
                    Log.e("API-DETECTION-POST", "Gagal menyimpan data, response code: ${response.code()}")
                }
                pindahKeHalamanSukses()
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                Log.e("API-DETECTION-POST", "Koneksi internet bermasalah: ${t.message}")
                pindahKeHalamanSukses()
            }
        })
    }

    private fun pindahKeHalamanSukses() {
        Toast.makeText(this, "🎉 BERHASIL! Misi $namaKebiasaan Selesai!", Toast.LENGTH_LONG).show()

        val intentSukses = Intent(this, MissionSuccessActivity::class.java).apply {
            putExtra("NAMA_KEBIASAAN", namaKebiasaan)
        }
        startActivity(intentSukses)
        finish()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Aplikasi butuh izin kamera untuk mendeteksi benda.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        try {
            stopService(Intent(this, AlarmBackgroundService::class.java))
        } catch (e: Exception) { /** Diabaikan **/ }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}