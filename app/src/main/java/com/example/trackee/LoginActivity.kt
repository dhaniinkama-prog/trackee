package com.example.trackee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val checkRemember = findViewById<CheckBox>(R.id.checkRemember)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textSignup = findViewById<TextView>(R.id.textSignup)

        // LOGIKA KUSTOM MATA PASSWORD (LOGIN)
        val layoutPassword = inputPassword.parent.parent as? TextInputLayout
        if (layoutPassword != null) {
            layoutPassword.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            inputPassword.hint = "Enter Password"

            layoutPassword.setEndIconOnClickListener {
                if (inputPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                inputPassword.setSelection(inputPassword.text.length)
            }
        }

        textSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        buttonLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            buttonLogin.isEnabled = false
            buttonLogin.text = "Loading..."

            val params = HashMap<String, String>()
            params["email"] = email
            params["password"] = password

            // Memanggil fungsi loginUser
            RetrofitClient.instance.loginUser(params).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    buttonLogin.isEnabled = true
                    buttonLogin.text = "LOGIN"

                    if (response.isSuccessful && response.body() != null) {
                        val authData = response.body()!!

                        // Menggunakan Elvis Operator untuk backup .user atau .dataUser dari model
                        val userProfile = authData.user ?: authData.dataUser

                        if (userProfile != null) {
                            val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
                            val editor = sharedPref.edit()

                            editor.putBoolean("IS_LOGGED_IN", true)
                            editor.putString("USER_EMAIL", email)

                            // Id dikonversi ke String & variabel nama disesuaikan ke nameUser milik model timmu
                            editor.putString("USER_ID", userProfile.id.toString())
                            editor.putString("USER_NAME", userProfile.name)

                            // FIXED: Menggunakan access_token yang valid dan terdeteksi di model kamu
                            val token = authData.access_token ?: ""
                            editor.putString("USER_TOKEN", token)

                            // =========================================================================
                            // PENAMBAHAN FLAG: Menandai bahwa pengguna ini masuk lewat jalur LOGIN
                            // (User lama, tidak perlu kuesioner lagi)
                            // =========================================================================
                            editor.putBoolean("IS_RETURNING_USER", true)

                            editor.apply()

                            Toast.makeText(this@LoginActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                            // Alur lancar: Masuk ke WelcomeActivity terlebih dahulu
                            val intent = Intent(this@LoginActivity, WelcomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Data user tidak ditemukan dalam respon server.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Login Gagal! Akun tidak cocok.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    buttonLogin.isEnabled = true
                    buttonLogin.text = "LOGIN"
                    Toast.makeText(this@LoginActivity, "Koneksi ke server gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}