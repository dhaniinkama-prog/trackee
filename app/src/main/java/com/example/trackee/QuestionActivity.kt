package com.example.trackee

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class QuestionActivity : AppCompatActivity() {

    private lateinit var btnYes: Button
    private lateinit var btnNo: Button
    private lateinit var btnSkip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)

        btnYes = findViewById(R.id.buttonyes)
        btnNo = findViewById(R.id.buttonno)
        btnSkip = findViewById(R.id.buttonskip)

        // Ketika user pilih YES -> Pindah ke MenuActivity
        btnYes.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish() // Tutup QuestionActivity agar tidak menumpuk di backstack
        }

        // Ketika user pilih NO -> Pindah ke MenuActivity
        btnNo.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish() // Tutup QuestionActivity agar tidak menumpuk di backstack
        }

        // Ketika user pilih SKIP -> Pindah ke MenuActivity
        btnSkip.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish() // Tutup QuestionActivity agar tidak menumpuk di backstack
        }
    }
}