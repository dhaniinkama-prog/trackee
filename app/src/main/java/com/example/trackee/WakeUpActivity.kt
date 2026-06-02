package com.example.trackee

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class WakeUpActivity : AppCompatActivity() {

    private lateinit var hourPicker: RecyclerView
    private lateinit var minutePicker: RecyclerView

    private lateinit var hourAdapter: TimeAdapter
    private lateinit var minuteAdapter: TimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_time)

        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)

        val btnContinue = findViewById<Button>(R.id.buttonContinue)
        btnContinue.setOnClickListener {
            // Sesuai alur baru: Setelah isi jam bangun, lanjut ke QuestionActivity
            val intent = Intent(this, QuestionActivity::class.java)
            startActivity(intent)
            finish() // Ditutup supaya tidak menumpuk di backstack
        }

        // Generate angka jam & menit
        val hours = (0..23).map { String.format("%02d", it) }
        val minutes = (0..59).map { String.format("%02d", it) }

        // Adapter
        hourAdapter = TimeAdapter(hours)
        minuteAdapter = TimeAdapter(minutes)

        setupWheel(hourPicker, hourAdapter)
        setupWheel(minutePicker, minuteAdapter)
    }

    private fun setupWheel(rv: RecyclerView, adapter: TimeAdapter) {
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rv)

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(recyclerView.layoutManager)
                    if (centerView != null) {
                        val position = recyclerView.getChildAdapterPosition(centerView)
                        adapter.updateCenterPosition(position)
                    }
                }
            }
        })

        rv.post {
            val centerView = snapHelper.findSnapView(rv.layoutManager)
            if (centerView != null) {
                val pos = rv.getChildAdapterPosition(centerView)
                adapter.updateCenterPosition(pos)
            }
        }
    }
}