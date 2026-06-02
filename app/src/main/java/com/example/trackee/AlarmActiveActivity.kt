package com.example.trackee

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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

class AlarmActiveActivity : AppCompatActivity() {

    private lateinit var tvHabitName: TextView
    private lateinit var tvMissionInstruction: TextView
    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var cameraExecutor: ExecutorService

    private var mediaPlayer: MediaPlayer? = null
    private var targetMisi: String = "Object"
    private var namaKebiasaan: String = ""

    private var historyId: Int = -1
    private var habitId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm_active)

        tvHabitName = findViewById(R.id.tv_active_habit_title)
        tvMissionInstruction = findViewById(R.id.tv_mission_instruction)
        previewView = findViewById(R.id.view_finder)
        btnCapture = findViewById(R.id.btn_capture)

        cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

        namaKebiasaan = intent.getStringExtra("NAMA_KEBIASAAN") ?: "Habit"
        targetMisi = intent.getStringExtra("MISI_BENDA") ?: "Object"
        val ringtoneUriStr = intent.getStringExtra("RINGTONE_URI")

        historyId = intent.getIntExtra("HISTORY_ID", -1)
        habitId = intent.getIntExtra("HABIT_ID", -1)

        tvHabitName.text = namaKebiasaan
        tvMissionInstruction.text = "MISI BEREAKSI: Arahkan kamera ke objek: $targetMisi"

        putarRingtone(ringtoneUriStr)
        mulaiKameraX()

        btnCapture.setOnClickListener {
            verifikasiMisiObjek()
        }
    }

    private fun putarRingtone(uriStr: String?) {
        try {
            mediaPlayer = MediaPlayer().apply {
                if (!uriStr.isNullOrEmpty()) {
                    setDataSource(this@AlarmActiveActivity, Uri.parse(uriStr))
                } else {
                    val defaultAlarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    setDataSource(this@AlarmActiveActivity, defaultAlarmUri)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memputar ringtone alarm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mulaiKameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("TrackEeCamera", "Gagal mengikat komponen KameraX", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun verifikasiMisiObjek() {
        val bitmapToScan: Bitmap? = previewView.bitmap

        if (bitmapToScan == null) {
            Toast.makeText(this, "Kamera belum siap atau tidak merespon.", Toast.LENGTH_SHORT).show()
            return
        }

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmapToScan, 0)

        btnCapture.isEnabled = false
        btnCapture.text = "Memeriksa..."

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                var misiBerhasil = false
                var namaObjekTerdeteksi = "Tidak Dikenal"
                var skorAkurasiFloat = 0.0f

                for (obj in detectedObjects) {
                    for (label in obj.labels) {
                        val labelText = label.text.lowercase()
                        val targetSederhana = targetMisi.lowercase()

                        namaObjekTerdeteksi = label.text
                        skorAkurasiFloat = label.confidence

                        if (labelText.contains(targetSederhana) || targetSederhana.contains(labelText)) {
                            misiBerhasil = true
                            break
                        }
                    }
                    if (misiBerhasil) break
                }

                if (misiBerhasil) {
                    hentikanAlarm()
                    Toast.makeText(this, "Misi Berhasil! Objek $targetMisi Ditemukan.", Toast.LENGTH_LONG).show()

                    val tanggalHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val habitIdFix = if (habitId != -1) habitId else historyId

                    val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
                    val activeUserId = (sharedPref.getString("USER_ID", "0") ?: "0").toInt()

                    RetrofitClient.instance.createHistory(
                        userId = activeUserId,
                        objectName = namaKebiasaan,
                        status = "Selesai",
                        notes = "Misi foto sukses via deteksi AI kamera",
                        detectedLabel = namaObjekTerdeteksi,
                        confidenceScore = skorAkurasiFloat
                    ).enqueue(object : Callback<CommonResponse> {
                        override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {

                            val apiService = RetrofitClient.instance
                            // =========================================================================
                            // FIX: Mengirim skorAkurasiFloat secara langsung, tidak di-convert ke String
                            // =========================================================================
                            apiService.storeImageDetection(
                                habitId = habitIdFix,
                                date = tanggalHariIni,
                                detectedLabel = namaObjekTerdeteksi,
                                confidenceScore = skorAkurasiFloat,
                                imagePath = "assets/detections/alarm_captured.jpg",
                                historyId = historyId
                            ).enqueue(object : Callback<CommonResponse> {
                                override fun onResponse(call: Call<CommonResponse>, innerResponse: Response<CommonResponse>) {
                                    pindahKeHalamanSukses()
                                }

                                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                                    pindahKeHalamanSukses()
                                }
                            })
                        }

                        override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                            pindahKeHalamanSukses()
                        }
                    })

                } else {
                    btnCapture.isEnabled = true
                    btnCapture.text = "VERIFIKASI OBJEK"
                    val persentaseTeks = (skorAkurasiFloat * 100).toInt()
                    Toast.makeText(this, "Gagal! Terdeteksi: $namaObjekTerdeteksi ($persentaseTeks%). Cari: $targetMisi", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                btnCapture.isEnabled = true
                btnCapture.text = "VERIFIKASI OBJEK"
                Toast.makeText(this, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pindahKeHalamanSukses() {
        val intentSukses = Intent(this@AlarmActiveActivity, MissionSuccessActivity::class.java).apply {
            putExtra("NAMA_KEBIASAAN", namaKebiasaan)
            putExtra("MISI_BENDA", targetMisi)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intentSukses)
        finish()
    }

    private fun hentikanAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hentikanAlarm()
        cameraExecutor.shutdown()
    }
}