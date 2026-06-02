package com.example.trackee

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgProfileAvatar: ImageView
    private var selectedImageUri: Uri? = null
    private var activeUserId: Int = 0

    private val getImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            if (data != null && data.data != null) {
                selectedImageUri = data.data

                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(selectedImageUri!!, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Tampilkan gambar sementara menggunakan Glide agar aman
                Glide.with(this).load(selectedImageUri).into(imgProfileAvatar)

                // LANGSUNG UPLOAD KE SERVER SETELAH FOTO DIPILIH
                uploadFotoKeLaravel()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // =========================================================================
        // FIX SINKRONISASI ID: Menyamakan ID Kotlin dengan ID di file XML kamu
        // =========================================================================
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnLogout = findViewById<ImageView>(R.id.btnLogout)
        val btnChangePhoto = findViewById<TextView>(R.id.btnChangePhoto)
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar)

        val inputFullname = findViewById<EditText>(R.id.inputFullname)
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputOldPassword = findViewById<EditText>(R.id.inputOldPassword)
        val inputNewPassword = findViewById<EditText>(R.id.inputNewPassword)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        val sharedPref = getSharedPreferences("TrackeePref", MODE_PRIVATE)

        // Ambil User ID asli dari sesi login
        val savedUserIdString = sharedPref.getString("USER_ID", "0") ?: "0"
        activeUserId = savedUserIdString.toIntOrNull() ?: 0

        inputFullname.setText(sharedPref.getString("USER_NAME", ""))
        inputEmail.setText(sharedPref.getString("USER_EMAIL", ""))

        // Memuat Foto Profil Menggunakan URL internet dari SharedPreferences
        val savedAvatarStr = sharedPref.getString("USER_AVATAR", null)
        if (!savedAvatarStr.isNullOrEmpty()) {
            Glide.with(this)
                .load(savedAvatarStr)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .circleCrop()
                .into(imgProfileAvatar)
        }

        // Aksi ganti foto ketika teks atau area foto diklik
        btnChangePhoto.setOnClickListener {
            bukaGaleri()
        }
        imgProfileAvatar.setOnClickListener {
            bukaGaleri()
        }

        // Tombol kembali ke halaman sebelumnya
        btnBack.setOnClickListener { finish() }

        // Tombol logout session
        btnLogout.setOnClickListener {
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()
            Toast.makeText(this, "Berhasil Logout!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // PROSES GANTI PASSWORD ONLINE KETIKA KLIK SIMPAN
        buttonSave.setOnClickListener {
            val inputOld = inputOldPassword.text.toString().trim()
            val inputNew = inputNewPassword.text.toString().trim()

            if (inputOld.isNotEmpty() || inputNew.isNotEmpty()) {
                if (inputNew.length < 8) {
                    Toast.makeText(this, "Password baru minimal 8 karakter!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                buttonSave.isEnabled = false
                buttonSave.text = "Mengubah Password..."

                RetrofitClient.instance.changePassword(activeUserId, inputOld, inputNew)
                    .enqueue(object : Callback<CommonResponse> {
                        override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                            buttonSave.isEnabled = true
                            buttonSave.text = "Simpan Perubahan"

                            if (response.isSuccessful) {
                                Toast.makeText(this@ProfileActivity, "Password Berhasil Diperbarui di Server!", Toast.LENGTH_SHORT).show()
                                inputOldPassword.text.clear()
                                inputNewPassword.text.clear()
                            } else {
                                Toast.makeText(this@ProfileActivity, "Gagal Ganti Password. Cek password lama Anda!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                            buttonSave.isEnabled = true
                            buttonSave.text = "Simpan Perubahan"
                            Toast.makeText(this@ProfileActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                val editor = sharedPref.edit()
                editor.putString("USER_NAME", inputFullname.text.toString().trim())
                editor.putString("USER_EMAIL", inputEmail.text.toString().trim())
                editor.apply()
                Toast.makeText(this, "Profil lokal berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun bukaGaleri() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        getImageLauncher.launch(intent)
    }

    private fun uploadFotoKeLaravel() {
        if (selectedImageUri == null) return

        try {
            val inputStream = contentResolver.openInputStream(selectedImageUri!!) ?: return

            val file = File(cacheDir, "upload_avatar.jpg")
            val outputStream = FileOutputStream(file)

            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)

            outputStream.close()
            inputStream.close()

            val mediaTypeImage = MediaType.parse("image/jpeg")
            val requestFile = RequestBody.create(mediaTypeImage, file)
            val bodyImage = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val mediaTypeText = MediaType.parse("text/plain")
            val userIdBody = RequestBody.create(mediaTypeText, activeUserId.toString())

            Toast.makeText(this, "Mengunggah foto ke server...", Toast.LENGTH_SHORT).show()

            RetrofitClient.instance.uploadProfilePicture(userIdBody, bodyImage)
                .enqueue(object : Callback<JsonObject> {
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        if (response.isSuccessful && response.body() != null) {
                            val jsonResponse = response.body()!!

                            if (jsonResponse.has("profile_picture_url")) {
                                val urlFotoServer = jsonResponse.get("profile_picture_url").asString

                                val sharedPref = getSharedPreferences("TrackeePref", MODE_PRIVATE)
                                sharedPref.edit().putString("USER_AVATAR", urlFotoServer).apply()

                                Glide.with(this@ProfileActivity)
                                    .load(urlFotoServer)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .circleCrop()
                                    .into(imgProfileAvatar)

                                Toast.makeText(this@ProfileActivity, "Foto profil sinkron ke database cloud!", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                            } else {
                                Toast.makeText(this@ProfileActivity, "Foto terunggah, gagal memetakan URL data", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorRaw = response.errorBody()?.string()
                            Log.e("TrackeeUploadError", "Pesan Penolakan Server: $errorRaw")
                            Toast.makeText(this@ProfileActivity, "Gagal sinkron gambar ke server", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                        Log.e("TrackeeUploadError", "Gagal Total Koneksi", t)
                        Toast.makeText(this@ProfileActivity, "Koneksi server gagal untuk unggah foto", Toast.LENGTH_SHORT).show()
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memproses file foto", Toast.LENGTH_SHORT).show()
        }
    }
}