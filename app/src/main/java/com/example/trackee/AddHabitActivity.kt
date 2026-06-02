package com.example.trackee

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddHabitActivity : AppCompatActivity() {

    private lateinit var etHabitName: EditText
    private lateinit var etDailyGoal: EditText
    private lateinit var tvHours: TextView
    private lateinit var tvMinutes: TextView
    private lateinit var btnChangeReminder: Button
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

    private lateinit var btnSelectRingtone: Button
    private lateinit var tvRingtoneName: TextView

    private lateinit var rgFrequencyType: RadioGroup
    private lateinit var layoutSpecificDays: LinearLayout
    private lateinit var layoutFlexibleCount: LinearLayout
    private lateinit var etFlexibleCount: EditText

    private lateinit var toggleSun: ToggleButton
    private lateinit var toggleMon: ToggleButton
    private lateinit var toggleTue: ToggleButton
    private lateinit var toggleWed: ToggleButton
    private lateinit var toggleThu: ToggleButton
    private lateinit var toggleFri: ToggleButton
    private lateinit var toggleSat: ToggleButton

    private var selectedRingtoneUri: String? = null
    private var habitPosition: Int = -1
    private var habitId: Int = -1

    private var tanggalAktifDariIntent: String = ""
    private var stringBanyakJam: String = "07:30"

    private val calendarPengingat: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedRingtoneUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(this, uri)
                tvRingtoneName.text = ringtone.getTitle(this)
            } else {
                tvRingtoneName.text = "Suara Default"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_habit)

        etHabitName = findViewById(R.id.et_habit_name)
        etDailyGoal = findViewById(R.id.et_daily_goal)
        tvHours = findViewById(R.id.tv_hours)
        tvMinutes = findViewById(R.id.tv_minutes)
        btnChangeReminder = findViewById(R.id.btn_change_reminder)
        btnSave = findViewById(R.id.btn_save_habit)
        btnBack = findViewById(R.id.btn_close)
        btnSelectRingtone = findViewById(R.id.btn_select_ringtone)
        tvRingtoneName = findViewById(R.id.tv_ringtone_name)

        rgFrequencyType = findViewById(R.id.rg_frequency_type)
        layoutSpecificDays = findViewById(R.id.layout_specific_days)
        layoutFlexibleCount = findViewById(R.id.layout_flexible_count)
        etFlexibleCount = findViewById(R.id.et_flexible_count)

        toggleSun = findViewById(R.id.toggle_sun)
        toggleMon = findViewById(R.id.toggle_mon)
        toggleTue = findViewById(R.id.toggle_tue)
        toggleWed = findViewById(R.id.toggle_wed)
        toggleThu = findViewById(R.id.toggle_thu)
        toggleFri = findViewById(R.id.toggle_fri)
        toggleSat = findViewById(R.id.toggle_sat)

        rgFrequencyType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_every_day -> {
                    layoutSpecificDays.visibility = View.GONE
                    layoutFlexibleCount.visibility = View.GONE
                }
                R.id.rb_specific_days -> {
                    layoutSpecificDays.visibility = View.VISIBLE
                    layoutFlexibleCount.visibility = View.GONE
                }
                R.id.rb_flexible_week -> {
                    layoutSpecificDays.visibility = View.GONE
                    layoutFlexibleCount.visibility = View.VISIBLE
                }
            }
        }

        val sdfDefault = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tanggalAktifDariIntent = intent.getStringExtra("EXTRA_TANGGAL_AKTIF") ?: sdfDefault.format(Date())

        if (intent.hasExtra("EXTRA_HABIT_NAME") || intent.getIntExtra("EXTRA_HABIT_ID", -1) != -1) {
            habitId = intent.getIntExtra("EXTRA_HABIT_ID", -1)
            val habitName = intent.getStringExtra("EXTRA_HABIT_NAME")
            val habitTarget = intent.getStringExtra("EXTRA_HABIT_TARGET")

            stringBanyakJam = intent.getStringExtra("EXTRA_HABIT_HOURS_MINUTES") ?: "07:30"
            selectedRingtoneUri = intent.getStringExtra("EXTRA_HABIT_RINGTONE")
            habitPosition = intent.getIntExtra("HABIT_POSITION", -1)

            etHabitName.setText(habitName)
            etDailyGoal.setText(habitTarget)

            val freqType = intent.getStringExtra("EXTRA_HABIT_FREQUENCY_TYPE") ?: "every_day"
            when (freqType) {
                "every_day" -> rgFrequencyType.check(R.id.rb_every_day)
                "specific_days" -> {
                    rgFrequencyType.check(R.id.rb_specific_days)
                    val specDays = intent.getStringExtra("EXTRA_HABIT_SPECIFIC_DAYS") ?: ""
                    toggleSun.isChecked = specDays.contains("Sun")
                    toggleMon.isChecked = specDays.contains("Mon")
                    toggleTue.isChecked = specDays.contains("Tue")
                    toggleWed.isChecked = specDays.contains("Wed")
                    toggleThu.isChecked = specDays.contains("Thu")
                    toggleFri.isChecked = specDays.contains("Fri")
                    toggleSat.isChecked = specDays.contains("Sat")
                }
                "flexible_week" -> {
                    rgFrequencyType.check(R.id.rb_flexible_week)
                    val flexCount = intent.getIntExtra("EXTRA_HABIT_FLEXIBLE_COUNT", 3)
                    etFlexibleCount.setText(flexCount.toString())
                }
            }

            try {
                val jamBersih = if (stringBanyakJam.contains(",")) stringBanyakJam.split(",")[0] else stringBanyakJam
                val bagianJamMenit = when {
                    jamBersih.contains("T") -> jamBersih.split("T")[1].split(":")
                    jamBersih.contains(" ") -> jamBersih.split(" ")[1].split(":")
                    else -> jamBersih.split(":")
                }

                val jamFormat = bagianJamMenit[0].trim()
                val menitFormat = bagianJamMenit[1].trim()

                tvHours.text = jamFormat
                tvMinutes.text = menitFormat

                calendarPengingat.set(Calendar.HOUR_OF_DAY, jamFormat.toInt())
                calendarPengingat.set(Calendar.MINUTE, menitFormat.toInt())
                stringBanyakJam = "$jamFormat:$menitFormat"
            } catch (e: Exception) {
                Log.e("TrackeeJam", "Gagal mengurai waktu bawaan: $stringBanyakJam", e)
                tvHours.text = "07"
                tvMinutes.text = "30"
                stringBanyakJam = "07:30"
            }

            if (selectedRingtoneUri != null) {
                try {
                    val ringtone = RingtoneManager.getRingtone(this, Uri.parse(selectedRingtoneUri))
                    tvRingtoneName.text = ringtone.getTitle(this)
                } catch (e: Exception) {
                    tvRingtoneName.text = "Suara Default"
                }
            }

            btnSave.text = "Perbarui Kebiasaan"
        }

        btnSelectRingtone.setOnClickListener {
            val intentPicker = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih Suara Alarm")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri?.let { Uri.parse(it) })
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            }
            ringtonePickerLauncher.launch(intentPicker)
        }

        btnChangeReminder.setOnClickListener { tampilkanTimePicker() }
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { simpanKeDatabaseLaravel() }
    }

    private fun tampilkanTimePicker() {
        val jamSekarang = calendarPengingat.get(Calendar.HOUR_OF_DAY)
        val menitSekarang = calendarPengingat.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, jamTerpilih, menitTerpilih ->
            val jamFormat = String.format("%02d", jamTerpilih)
            val menitFormat = String.format("%02d", menitTerpilih)
            stringBanyakJam = "$jamFormat:$menitFormat"

            tvHours.text = jamFormat
            tvMinutes.text = menitFormat

            calendarPengingat.set(Calendar.HOUR_OF_DAY, jamTerpilih)
            calendarPengingat.set(Calendar.MINUTE, menitTerpilih)

            Toast.makeText(this, "Waktu alarm aktif: $stringBanyakJam", Toast.LENGTH_SHORT).show()
        }, jamSekarang, menitSekarang, true).show()
    }

    private fun simpanKeDatabaseLaravel() {
        val inputName = etHabitName.text.toString().trim()
        val inputTarget = etDailyGoal.text.toString().trim()

        if (inputName.isEmpty() || inputTarget.isEmpty()) {
            Toast.makeText(this, "Nama & Target tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        val frequencyType = when (rgFrequencyType.checkedRadioButtonId) {
            R.id.rb_every_day -> "every_day"
            R.id.rb_specific_days -> "specific_days"
            R.id.rb_flexible_week -> "flexible_week"
            else -> "every_day"
        }

        var specificDaysResult: String? = null
        if (frequencyType == "specific_days") {
            val daysList = mutableListOf<String>()
            if (toggleSun.isChecked) daysList.add("Sun")
            if (toggleMon.isChecked) daysList.add("Mon")
            if (toggleTue.isChecked) daysList.add("Tue")
            if (toggleWed.isChecked) daysList.add("Wed")
            if (toggleThu.isChecked) daysList.add("Thu")
            if (toggleFri.isChecked) daysList.add("Fri")
            if (toggleSat.isChecked) daysList.add("Sat")

            if (daysList.isEmpty()) {
                Toast.makeText(this, "Pilih minimal satu hari untuk opsi Hari Tertentu!", Toast.LENGTH_SHORT).show()
                return
            }
            specificDaysResult = daysList.joinToString(",")
        }

        var flexibleCountResult: Int? = null
        if (frequencyType == "flexible_week") {
            val countText = etFlexibleCount.text.toString().trim()
            flexibleCountResult = countText.toIntOrNull() ?: 3
            if (flexibleCountResult < 1 || flexibleCountResult > 7) {
                Toast.makeText(this, "Target hari fleksibel harus bernilai 1-7 hari!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val savedUserIdString = sharedPref.getString("USER_ID", "0") ?: "0"
        val activeUserId = savedUserIdString.toIntOrNull() ?: 0

        if (activeUserId == 0) {
            Toast.makeText(this, "Sesi user tidak valid. Silakan relogin!", Toast.LENGTH_SHORT).show()
            return
        }

        // =========================================================================
        // PERBAIKAN: LOGIKA BARU PENGOCOKAN TARGET MISI BENDA SECARA ACAK
        // Diambil secara acak dari kumpulan label objek riil bawaan ML Kit
        // =========================================================================
        val daftarBendaRandom = listOf(
            "Bottle",       // Botol Minum / Tumbler
            "Cup",          // Cangkir / Gelas Kopi / Mug
            "Book",         // Buku / Novel / Catatan
            "Fashion good", // Baju / Handuk / Kain Olahraga
            "Home good",    // Sisir / Kotak Pensil / Benda rumah umum
            "Tableware"     // Piring / Mangkok / Sendok di meja
        )
        val misiBenda = daftarBendaRandom.random()
        // =========================================================================

        btnSave.isEnabled = false
        btnSave.text = "Menyimpan ke Server..."

        val apiService = RetrofitClient.instance

        // PERBAIKAN: Pemotongan string jam dilakukan dengan lebih aman dan bersih
        val jamBersihUntukDateTime = stringBanyakJam.split(",")[0].trim()
        val alarmDatetimeResult = "$tanggalAktifDariIntent $jamBersihUntukDateTime:00"

        if (habitId != -1) {
            apiService.updateHabit(
                id = habitId,
                name = inputName,
                target = inputTarget,
                frequencyType = frequencyType,
                specificDays = specificDaysResult,
                flexibleCount = flexibleCountResult,
                alarmDatetime = alarmDatetimeResult,
                hoursAndMinutes = stringBanyakJam,
                ringtoneUri = selectedRingtoneUri,
                missionTarget = misiBenda
            ).enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    btnSave.isEnabled = true
                    btnSave.text = "Perbarui Kebiasaan"

                    if (response.isSuccessful || response.code() == 200 || response.code() == 201) {
                        Toast.makeText(this@AddHabitActivity, "Kebiasaan berhasil diperbarui!", Toast.LENGTH_SHORT).show()

                        val updatedHabitTemplate = HabitModel(
                            id = habitId,
                            userId = activeUserId,
                            name = inputName,
                            target = inputTarget,
                            alarmDatetime = alarmDatetimeResult,
                            hoursAndMinutes = stringBanyakJam,
                            missionTarget = misiBenda,
                            frequencyType = frequencyType,
                            specificDays = specificDaysResult,
                            flexibleCount = flexibleCountResult,
                            ringtoneUri = selectedRingtoneUri
                        )
                        try {
                            AlarmHelper.setBanyakAlarmDariDatabase(this@AddHabitActivity, updatedHabitTemplate)
                        } catch (e: Exception) {
                            Log.e("TrackeeAlarm", "Gagal memperbarui alarm lokal", e)
                        }

                        val intentBalik = Intent().apply {
                            putExtra("HABIT_POSITION", habitPosition)
                        }
                        setResult(RESULT_OK, intentBalik)
                        finish()
                    } else {
                        Log.e("TrackeeUpdate", "Error Server: Code ${response.code()}")
                        Toast.makeText(this@AddHabitActivity, "Gagal memperbarui data di server.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    btnSave.text = "Perbarui Kebiasaan"
                    Log.e("TrackEeApiError", "Koneksi Update API Gagal", t)
                    Toast.makeText(this@AddHabitActivity, "Gagal terhubung dengan server Laravel!", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            apiService.createHabit(
                userId = activeUserId,
                name = inputName,
                target = inputTarget,
                frequencyType = frequencyType,
                specificDays = specificDaysResult,
                flexibleCount = flexibleCountResult,
                alarmDatetime = alarmDatetimeResult,
                hoursAndMinutes = stringBanyakJam,
                ringtoneUri = selectedRingtoneUri,
                missionTarget = misiBenda
            ).enqueue(object : Callback<HabitModel> {
                override fun onResponse(call: Call<HabitModel>, response: Response<HabitModel>) {
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan"

                    if (response.isSuccessful && response.body() != null) {
                        val habitHasilDatabase = response.body()!!

                        // PERBAIKAN UTAMA: Mengisi properti properti alarmDatetime sebelum diproses oleh AlarmHelper
                        habitHasilDatabase.alarmDatetime = alarmDatetimeResult

                        AlarmHelper.setBanyakAlarmDariDatabase(this@AddHabitActivity, habitHasilDatabase)
                        Toast.makeText(this@AddHabitActivity, "Kebiasaan baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()

                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@AddHabitActivity, "Gagal memproses respons dari server.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<HabitModel>, t: Throwable) {
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan"
                    Log.e("TrackEeApiError", "Koneksi Create API Gagal", t)
                    Toast.makeText(this@AddHabitActivity, "Gagal terhubung dengan server Laravel!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}