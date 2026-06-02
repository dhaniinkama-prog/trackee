package com.example.trackee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val inputFullname = findViewById<EditText>(R.id.inputFullname)
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val buttonSignup = findViewById<Button>(R.id.buttonSignup)
        val textGoToLogin = findViewById<TextView>(R.id.textGoToLogin)

        val layoutPassword = findViewById<TextInputLayout>(R.id.layoutPasswordSignup)

        // KEMBALI KE LOGIN: Menutup halaman signup, kembali ke login di belakangnya
        textGoToLogin.setOnClickListener {
            finish()
        }

        buttonSignup.setOnClickListener {
            val name = inputFullname.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            // Validasi Input
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) { // Disamakan dengan Laravel (minimal 8 karakter)
                Toast.makeText(this, "Password minimal 8 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Bungkus data ke dalam HashMap untuk dikirim ke @FieldMap registerUser
            val params = HashMap<String, String>()
            params["name"] = name
            params["email"] = email
            params["password"] = password

            Toast.makeText(this, "Mendaftarkan akun...", Toast.LENGTH_SHORT).show()

            // =========================================================================
            // HUBUNGKAN KE LARAVEL: Kirim data registrasi ke database MySQL
            // =========================================================================
            RetrofitClient.instance.registerUser(params).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {

                        // =========================================================================
                        // SIMPAN DATA KE SHAREDPREFS (Status Login & Nama untuk Welcome)
                        // =========================================================================
                        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("USER_NAME", name)              // Simpan nama untuk textview Welcome
                        editor.putBoolean("IS_RETURNING_USER", false)    // KUNCI: Pengguna Baru (Wajib Kuesioner)
                        editor.putBoolean("IS_LOGGED_IN", true)          // Tandai sudah login agar tidak tertendang
                        editor.apply()

                        Toast.makeText(this@SignupActivity, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()

                        // UBAH RUTE: Langsung bawa Pengguna Baru ke WelcomeActivity secara bersih
                        val intent = Intent(this@SignupActivity, WelcomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Jika email sudah terdaftar atau validasi Laravel gagal
                        Toast.makeText(this@SignupActivity, "Registrasi Gagal! Email mungkin sudah digunakan.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@SignupActivity, "Koneksi ke server gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}