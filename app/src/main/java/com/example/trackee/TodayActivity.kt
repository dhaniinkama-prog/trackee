package com.example.trackee

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodayActivity : AppCompatActivity() {

    private val contohHabit = mutableListOf<HabitModel>()
    private lateinit var adapter: HabitAdapter
    private lateinit var imgHeaderAvatar: ImageView

    private lateinit var calendarAdapter: CalendarAdapter
    private val listTanggalKalender = mutableListOf<Date>()
    private var tanggalDipilihSaatIni: Date = Calendar.getInstance().time

    private lateinit var viewDimOverlay: View
    private lateinit var cardSidebar: CardView

    private var rectEditInstance: RectF? = null
    private var rectDeleteInstance: RectF? = null

    private val editHabitLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val dataIntent = result.data
            val isDeleted = dataIntent?.getBooleanExtra("IS_DELETED", false) ?: false
            val deletedPosition = dataIntent?.getIntExtra("HABIT_POSITION", -1) ?: -1

            if (isDeleted && deletedPosition != -1 && deletedPosition < contohHabit.size) {
                contohHabit.removeAt(deletedPosition)
                adapter.notifyItemRemoved(deletedPosition)
                adapter.notifyItemRangeChanged(deletedPosition, contohHabit.size)
                Toast.makeText(this, "Kebiasaan berhasil dihapus!", Toast.LENGTH_SHORT).show()
            } else {
                ambilDataHabitBasedOnDate(tanggalDipilihSaatIni)
            }
        }
    }

    private val addHabitLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            ambilDataHabitBasedOnDate(tanggalDipilihSaatIni)
        }
    }

    private var swipeButtonsShowedState = SwipeState.GONE
    private var currentViewHolder: RecyclerView.ViewHolder? = null

    enum class SwipeState {
        GONE, RIGHT_VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("IS_LOGGED_IN", false)

        if (!isLoggedIn) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_today)

        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val rvHabits = findViewById<RecyclerView>(R.id.rvHabits)
        val btnAddNew = findViewById<Button>(R.id.btnAddNew)
        val btnAddIcon = findViewById<ImageView>(R.id.btnAddIcon)

        val btnGoToProfile = findViewById<CardView>(R.id.btnGoToProfile)
        imgHeaderAvatar = findViewById(R.id.imgHeaderAvatar)

        val menuDashboardView = LayoutInflater.from(this).inflate(R.layout.menu_dashboard, null)
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        rootLayout.addView(menuDashboardView)

        viewDimOverlay = menuDashboardView.findViewById(R.id.viewDimOverlay)
        cardSidebar = menuDashboardView.findViewById(R.id.cardSidebar)

        val menuProfile = menuDashboardView.findViewById<LinearLayout>(R.id.menu_profile)
        val menuHistory = menuDashboardView.findViewById<LinearLayout>(R.id.menu_history)
        val menuLogout = menuDashboardView.findViewById<LinearLayout>(R.id.menu_logout)

        viewDimOverlay.visibility = View.GONE
        cardSidebar.visibility = View.GONE

        initCalendarHorizontal()

        rvHabits.layoutManager = LinearLayoutManager(this)
        adapter = HabitAdapter(this, contohHabit, { tanggalDipilihSaatIni }, { ambilDataHabitBasedOnDate(tanggalDipilihSaatIni) })
        rvHabits.adapter = adapter

        ambilDataHabitBasedOnDate(tanggalDipilihSaatIni)

        val swipeGesture = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val currentPos = viewHolder.adapterPosition
                if (currentPos == RecyclerView.NO_POSITION || currentPos >= contohHabit.size) return

                val currentItem = contohHabit[currentPos]

                val builder = android.app.AlertDialog.Builder(this@TodayActivity)
                builder.setTitle("Hapus Habit")
                builder.setMessage("Apakah Anda yakin ingin menghapus habit '${currentItem.name}' beserta seluruh riwayatnya?")

                builder.setPositiveButton("Hapus") { dialog, _ ->

                    // 1. BACKUP DATA UNTUK JAGA-JAGA JIKA SERVER EROR (ROLLBACK)
                    val backupItem = currentItem

                    // 2. INSTANT UI DELETE: Hapus langsung dari layar HP demi kenyamanan visual user
                    contohHabit.removeAt(currentPos)
                    adapter.notifyItemRemoved(currentPos)
                    adapter.notifyItemRangeChanged(currentPos, contohHabit.size)
                    Toast.makeText(this@TodayActivity, "Habit berhasil dihapus!", Toast.LENGTH_SHORT).show()

                    // 3. JALANKAN OPERASI ASINKRONUS KE BACKEND LARAVEL
                    RetrofitClient.instance.deleteHabit(backupItem.id).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                        override fun onResponse(
                            call: retrofit2.Call<okhttp3.ResponseBody>,
                            response: retrofit2.Response<okhttp3.ResponseBody>
                        ) {
                            if (!response.isSuccessful) {
                                Log.e("TrackeeDelete", "Server gagal menghapus data dengan kode eror: ${response.code()}")
                                Toast.makeText(this@TodayActivity, "Gagal sinkronisasi data dengan server", Toast.LENGTH_SHORT).show()

                                // Terjadi masalah di server, panggil ulang data asli (Rollback Visual)
                                ambilDataHabitBasedOnDate(tanggalDipilihSaatIni)
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                            Log.e("TrackeeDelete", "Koneksi Gagal total ke internet: ${t.message}")
                            Toast.makeText(this@TodayActivity, "Koneksi terputus! Mengembalikan data.", Toast.LENGTH_SHORT).show()

                            // Internet mati, kembalikan item yang terhapus (Rollback Visual)
                            ambilDataHabitBasedOnDate(tanggalDipilihSaatIni)
                        }
                    })
                    dialog.dismiss()
                }

                builder.setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                    // Kembalikan posisi swipe ke semula jika user klik batal
                    adapter.notifyItemChanged(currentPos)
                }

                builder.setCancelable(false)
                builder.show()
            }
            override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
                if (swipeButtonsShowedState == SwipeState.RIGHT_VISIBLE) return 0
                return super.convertToAbsoluteDirection(flags, layoutDirection)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                var currentDX = dX
                val itemView = viewHolder.itemView
                val buttonWidth = 160 * recyclerView.context.resources.displayMetrics.density

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (swipeButtonsShowedState != SwipeState.GONE) {
                        currentDX = Math.min(dX, -buttonWidth)
                        setTouchListener(recyclerView, viewHolder, currentDX, buttonWidth)
                    } else {
                        setTouchHelperListener(recyclerView, viewHolder, dX, buttonWidth)
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, currentDX, dY, actionState, isCurrentlyActive)
                    drawButtons(c, itemView, currentDX, buttonWidth)
                    currentViewHolder = viewHolder
                }
            }
        }

        ItemTouchHelper(swipeGesture).attachToRecyclerView(rvHabits)

        val openAddHabitAction = {
            val sdfQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val intent = Intent(this, AddHabitActivity::class.java).apply {
                putExtra("EXTRA_TANGGAL_AKTIF", sdfQuery.format(tanggalDipilihSaatIni))
            }
            addHabitLauncher.launch(intent)
        }
        btnAddNew.setOnClickListener { openAddHabitAction() }
        btnAddIcon.setOnClickListener { openAddHabitAction() }

        btnGoToProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnMenu.setOnClickListener {
            viewDimOverlay.visibility = View.VISIBLE
            cardSidebar.visibility = View.VISIBLE
        }

        viewDimOverlay.setOnClickListener {
            viewDimOverlay.visibility = View.GONE
            cardSidebar.visibility = View.GONE
        }

        menuProfile.setOnClickListener {
            viewDimOverlay.visibility = View.GONE
            cardSidebar.visibility = View.GONE
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        menuHistory.setOnClickListener {
            viewDimOverlay.visibility = View.GONE
            cardSidebar.visibility = View.GONE
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        menuLogout.setOnClickListener {
            viewDimOverlay.visibility = View.GONE
            cardSidebar.visibility = View.GONE

            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadProfileAvatar()
    }

    private fun initCalendarHorizontal() {
        val rvCalendar = findViewById<RecyclerView>(R.id.rvCalendar)
        val textToday = findViewById<TextView>(R.id.txtToday)

        listTanggalKalender.clear()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -14)

        for (i in 0..28) {
            listTanggalKalender.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val layoutManagerKalender = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCalendar.layoutManager = layoutManagerKalender

        calendarAdapter = CalendarAdapter(listTanggalKalender, tanggalDipilihSaatIni) { date ->
            updateHalamanBerdasarkanTanggal(date, textToday)
        }

        rvCalendar.adapter = calendarAdapter
        rvCalendar.scrollToPosition(14)

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvCalendar)

        rvCalendar.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManagerKalender)
                    if (centerView != null) {
                        val posisiTengah = layoutManagerKalender.getPosition(centerView)
                        if (posisiTengah != RecyclerView.NO_POSITION) {
                            val tanggalTerpilihSelesaiGeser = listTanggalKalender[posisiTengah]
                            updateHalamanBerdasarkanTanggal(tanggalTerpilihSelesaiGeser, textToday)
                            calendarAdapter.updateSelectedDate(tanggalTerpilihSelesaiGeser)
                        }
                    }
                }
            }
        })

        val formatHeaderAwal = SimpleDateFormat("EEEE, dd MMMM", Locale("id", "ID"))
        textToday.text = formatHeaderAwal.format(tanggalDipilihSaatIni)
    }

    private fun updateHalamanBerdasarkanTanggal(date: Date, textViewHeader: TextView) {
        if (tanggalDipilihSaatIni != date) {
            tanggalDipilihSaatIni = date
            val formatHeader = SimpleDateFormat("EEEE, dd MMMM", Locale("id", "ID"))
            textViewHeader.text = formatHeader.format(date)

            ambilDataHabitBasedOnDate(date)
        }
    }

    private fun ambilDataHabitBasedOnDate(date: Date) {
        val sdfQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tanggalString = sdfQuery.format(date)

        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val activeUserId = (sharedPref.getString("USER_ID", "0") ?: "0").toInt()

        Log.d("TrackEeSync", "Meminta data habit untuk User ID: $activeUserId pada Tanggal: $tanggalString")

        RetrofitClient.instance.getHabits(activeUserId, tanggalString).enqueue(object : Callback<List<HabitModel>> {
            override fun onResponse(call: Call<List<HabitModel>>, response: Response<List<HabitModel>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val daftarHabitDariServer = response.body()!!

                    RetrofitClient.instance.getHistories(userId = activeUserId).enqueue(object : Callback<List<HistoryResponse>> {
                        override fun onResponse(historyCall: Call<List<HistoryResponse>>, historyResponse: Response<List<HistoryResponse>>) {
                            if (historyResponse.isSuccessful && historyResponse.body() != null) {
                                val listHistory = historyResponse.body()!!

                                val objekTerdeteksiHariIni = listHistory.filter { historyItem ->
                                    val tglCreated = historyItem.created_at ?: ""
                                    tglCreated.startsWith(tanggalString)
                                }.map { it.object_name?.lowercase(Locale.getDefault()) ?: "" }

                                Log.d("TrackEeSync", "Objek foto terdeteksi hari ini di DB: $objekTerdeteksiHariIni")

                                for (habit in daftarHabitDariServer) {
                                    val targetMisiHabit = habit.missionTarget?.lowercase(Locale.getDefault()) ?: ""
                                    val namaHabitAsli = habit.name?.lowercase(Locale.getDefault()) ?: ""

                                    val apakahMisiSuksesFoto = objekTerdeteksiHariIni.any { objekFoto ->
                                        (targetMisiHabit.isNotEmpty() && objekFoto.contains(targetMisiHabit)) ||
                                                (targetMisiHabit.isNotEmpty() && targetMisiHabit.contains(objekFoto)) ||
                                                namaHabitAsli.contains(objekFoto)
                                    }

                                    if (apakahMisiSuksesFoto) {
                                        Log.d("TrackEeSync", "MATCH! Otomatis mencentang habit: ${habit.name}")
                                        habit.isChecked = true
                                    }
                                }
                            }

                            tampilkanKeAdapter(daftarHabitDariServer)
                        }

                        override fun onFailure(historyCall: Call<List<HistoryResponse>>, t: Throwable) {
                            tampilkanKeAdapter(daftarHabitDariServer)
                        }
                    })

                } else {
                    val userGoal = sharedPref.getString("USER_GOAL", "DEFAULT")
                    contohHabit.clear()
                    buatHabitRekomendasiOtomatis(userGoal)
                }
            }

            override fun onFailure(call: Call<List<HabitModel>>, t: Throwable) {
                val userGoal = sharedPref.getString("USER_GOAL", "DEFAULT")
                contohHabit.clear()
                buatHabitRekomendasiOtomatis(userGoal)
            }
        })
    }

    private fun sampleKondisiSuksesHapus(currentPos: Int) {
        if (currentPos != RecyclerView.NO_POSITION && currentPos < contohHabit.size) {
            Log.d("TrackeeDeleteSukses", "INFO: Berhasil menghapus item pada posisi: $currentPos dari list.")

            // Panggil fungsi internal adapter yang baru kita buat
            adapter.removeItem(currentPos)

            Toast.makeText(this@TodayActivity, "Habit berhasil dihapus!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sampleKondisiUpdateStatusCheck(habitId: Int, isChecked: Boolean) {
        val index = contohHabit.indexOfFirst { it.id == habitId }
        if (index != -1) {
            contohHabit[index].isChecked = isChecked
            adapter.notifyItemChanged(index)
        }
    }

    private fun tampilkanKeAdapter(daftarHabit: List<HabitModel>) {
        contohHabit.clear()
        contohHabit.addAll(daftarHabit)

        adapter.clearCheckedStatusMap()
        adapter.notifyDataSetChanged()

        for (habit in daftarHabit) {
            try {
                AlarmHelper.setBanyakAlarmDariDatabase(this@TodayActivity, habit)
            } catch (e: Exception) {
                Log.e("TrackeeAlarm", "Gagal mendaftarkan alarm internal untuk ${habit.name}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileAvatar()
        ambilDataHabitBasedOnDate(tanggalDipilihSaatIni)
    }

    private fun loadProfileAvatar() {
        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val savedImageUriString = sharedPref.getString("USER_AVATAR", null)

        if (!savedImageUriString.isNullOrEmpty()) {
            Glide.with(this)
                .load(savedImageUriString)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .circleCrop()
                .into(imgHeaderAvatar)
        } else {
            imgHeaderAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, buttonWidth: Float) {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (swipeButtonsShowedState == SwipeState.RIGHT_VISIBLE) {
                    checkButtonClick(event, viewHolder)
                }
                recyclerView.setOnTouchListener(null)
                swipeButtonsShowedState = SwipeState.GONE
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }
            false
        }
    }

    private fun setTouchHelperListener(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, buttonWidth: Float) {
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (dX <= -buttonWidth) {
                    swipeButtonsShowedState = SwipeState.RIGHT_VISIBLE
                }
                if (swipeButtonsShowedState != SwipeState.GONE) {
                    setTouchListener(recyclerView, viewHolder, dX, buttonWidth)
                }
            }
            false
        }
    }

    private fun checkButtonClick(event: MotionEvent, viewHolder: RecyclerView.ViewHolder) {
        val pos = viewHolder.adapterPosition
        if (pos == RecyclerView.NO_POSITION || pos >= contohHabit.size) return

        val currentItem = contohHabit[pos]

        if (rectEditInstance != null && rectEditInstance!!.contains(event.x, event.y)) {
            val sdfQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val rawWaktu = when {
                !currentItem.hoursAndMinutes.isNullOrEmpty() && currentItem.hoursAndMinutes != "null" -> currentItem.hoursAndMinutes
                !currentItem.alarmDatetime.isNullOrEmpty() && currentItem.alarmDatetime != "null" -> currentItem.alarmDatetime
                else -> "07:30"
            }

            var jamBersihKirim = "07:30"
            if (rawWaktu != null && rawWaktu != "Tidak Aktif") {
                try {
                    jamBersihKirim = if (rawWaktu.contains(",")) rawWaktu.split(",")[0] else rawWaktu
                    if (jamBersihKirim.contains("T")) {
                        val splitT = jamBersihKirim.split("T")[1]
                        jamBersihKirim = "${splitT.split(":")[0]}:${splitT.split(":")[1]}"
                    } else if (jamBersihKirim.contains(" ")) {
                        val splitSpasi = jamBersihKirim.split(" ")[1]
                        jamBersihKirim = "${splitSpasi.split(":")[0]}:${splitSpasi.split(":")[1]}"
                    }
                } catch (e: Exception) {
                    jamBersihKirim = "07:30"
                }
            }

            val intent = Intent(this, AddHabitActivity::class.java).apply {
                putExtra("EXTRA_HABIT_ID", currentItem.id)
                putExtra("EXTRA_HABIT_NAME", currentItem.name ?: "")
                putExtra("EXTRA_HABIT_TARGET", currentItem.target ?: "")
                putExtra("EXTRA_HABIT_HOURS_MINUTES", jamBersihKirim)
                putExtra("EXTRA_HABIT_RINGTONE", currentItem.ringtoneUri)
                putExtra("EXTRA_HABIT_FREQUENCY_TYPE", currentItem.frequencyType ?: "every_day")
                putExtra("EXTRA_HABIT_SPECIFIC_DAYS", currentItem.specificDays)
                putExtra("EXTRA_HABIT_FLEXIBLE_COUNT", currentItem.flexibleCount ?: 3)
                putExtra("EXTRA_TANGGAL_AKTIF", sdfQuery.format(tanggalDipilihSaatIni))
                putExtra("HABIT_POSITION", pos)
            }
            editHabitLauncher.launch(intent)
        }
        else if (rectDeleteInstance != null && rectDeleteInstance!!.contains(event.x, event.y)) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hapus Habit")
                .setMessage("Yakin ingin menghapus '${currentItem.name}'?")
                .setPositiveButton("Hapus") { _, _ ->

                    // Jika ID lokal rekomendasi (<= 0), langsung hapus dari layar
                    if (currentItem.id <= 0) {
                        val currentPos = viewHolder.adapterPosition
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < contohHabit.size) {
                            contohHabit.removeAt(currentPos)
                            adapter.notifyItemRemoved(currentPos)
                            adapter.notifyItemRangeChanged(currentPos, contohHabit.size)
                            Toast.makeText(this@TodayActivity, "Rekomendasi lokal dihapus", Toast.LENGTH_SHORT).show()
                        }
                        return@setPositiveButton
                    }

                    // Panggil API ke Laravel
                    RetrofitClient.instance.deleteHabit(currentItem.id).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                        override fun onResponse(call: retrofit2.Call<okhttp3.ResponseBody>, response: retrofit2.Response<okhttp3.ResponseBody>) {
                            val currentPos = viewHolder.adapterPosition

                            if (response.isSuccessful) {
                                sampleKondisiSuksesHapus(currentPos)
                            } else {
                                Log.e("TrackeeDelete", "Error Server: Code ${response.code()}")
                                Toast.makeText(this@TodayActivity, "Gagal menghapus habit dari server (Code: ${response.code()})", Toast.LENGTH_SHORT).show()

                                if (currentPos != RecyclerView.NO_POSITION) {
                                    adapter.notifyItemChanged(currentPos)
                                }
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                            val currentPos = viewHolder.adapterPosition
                            Log.e("TrackeeDelete", "Koneksi Gagal: ${t.message}")
                            Toast.makeText(this@TodayActivity, "Koneksi error, gagal menghapus data.", Toast.LENGTH_SHORT).show()

                            if (currentPos != RecyclerView.NO_POSITION) {
                                adapter.notifyItemChanged(currentPos)
                            }
                        }
                    })
                }
                .setNegativeButton("Batal") { _, _ ->
                    val currentPos = viewHolder.adapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(currentPos)
                    }
                }
                .show()
        }
    } // Tanda kurung kurawal checkButtonClick penutup aman di sini!

    private fun drawButtons(c: Canvas, itemView: View, dX: Float, buttonWidth: Float) {
        if (dX >= 0) return

        val p = Paint()
        val rightButtonLeft = itemView.right - buttonWidth
        val density = itemView.context.resources.displayMetrics.density
        val marginBottomPx = 12 * density
        val corners = 24f

        val rectEdit = RectF(rightButtonLeft, itemView.top.toFloat(), rightButtonLeft + (buttonWidth / 2), itemView.bottom.toFloat() - marginBottomPx)
        p.color = Color.parseColor("#2980B9")
        c.drawRoundRect(rectEdit, corners, corners, p)

        p.color = Color.WHITE
        p.textSize = 14 * density
        p.isAntiAlias = true
        p.textAlign = Paint.Align.CENTER
        val textHeight = p.descent() - p.ascent()
        val textOffset = textHeight / 2 - p.descent()
        c.drawText("EDIT", rectEdit.centerX(), rectEdit.centerY() + textOffset, p)

        val rectDelete = RectF(rightButtonLeft + (buttonWidth / 2), itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat() - marginBottomPx)
        p.color = Color.parseColor("#C0392B")
        c.drawRoundRect(rectDelete, corners, corners, p)

        p.color = Color.WHITE
        c.drawText("DELETE", rectDelete.centerX(), rectDelete.centerY() + textOffset, p)

        rectEditInstance = rectEdit
        rectDeleteInstance = rectDelete
    }

    private fun buatHabitRekomendasiOtomatis(goal: String?) {
        val sharedPref = getSharedPreferences("TrackeePref", Context.MODE_PRIVATE)
        val activeUserId = (sharedPref.getString("USER_ID", "0") ?: "0").toInt()

        // ID diatur ke 0 agar terdeteksi sebagai rekomendasi lokal offline
        when (goal) {
            "MINUM", "DRINK" -> {
                contohHabit.add(HabitModel(id = 0, userId = activeUserId, name = "Minum air 500ml setelah bangun tidur", target = "2.0 Liter", hoursAndMinutes = "07:30", ringtoneUri = null, missionTarget = "bottle"))
                contohHabit.add(HabitModel(id = 0, userId = activeUserId, name = "Sedia botol minum 2 Liter di meja kerja", target = "2.0 Liter", hoursAndMinutes = "11:00", ringtoneUri = null, missionTarget = "bottle"))
            }
            "TIDUR", "SLEEP" -> {
                contohHabit.add(HabitModel(id = 0, userId = activeUserId, name = "Matikan layar gadget pada jam 21.30", target = "8 Jam", hoursAndMinutes = "21:30", ringtoneUri = null, missionTarget = "bed"))
            }
            else -> {
                contohHabit.add(HabitModel(id = 0, userId = activeUserId, name = "Minum Air Putih", target = "2 Liter", hoursAndMinutes = "07:30", ringtoneUri = null, missionTarget = "bottle"))
            }
        }
        adapter.notifyDataSetChanged()
    }

    class HabitAdapter(
        private val context: Context,
        private val list: MutableList<HabitModel>, // <<< UBAH JADI MutableList
        private val getTanggalAktif: () -> Date,
        private val triggerDataRefresh: () -> Unit
    ) : RecyclerView.Adapter<HabitAdapter.ViewHolder>() {

        private val checkedStatusMap = mutableMapOf<Int, Boolean>()

        // >>> TAMBAHKAN FUNGSI INI DI DALAM HABITADAPTER <<<
        fun removeItem(position: Int) {
            if (position >= 0 && position < list.size) {
                list.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, list.size)
            }
        }

        fun clearCheckedStatusMap() {
            checkedStatusMap.clear()
        }

        // ... ke bawahnya tetap sama (ViewHolder, onCreateViewHolder, onBindViewHolder) ...

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.txtHabitName)
            val txtHabitProgress: TextView = v.findViewById(R.id.txtHabitProgress)
            val cbHabitDone: CheckBox = v.findViewById(R.id.cbHabitDone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_habit_text, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val habit = list[position]

            holder.title.text = habit.name ?: "Tanpa Nama"
            holder.txtHabitProgress.visibility = View.VISIBLE

            val rawWaktu: String? = when {
                !habit.hoursAndMinutes.isNullOrEmpty() && habit.hoursAndMinutes != "null" -> habit.hoursAndMinutes
                !habit.alarmDatetime.isNullOrEmpty() && habit.alarmDatetime != "null" -> habit.alarmDatetime
                else -> "07:30"
            }

            var jamBersih = "07:30"

            if (rawWaktu != null && rawWaktu != "Tidak Aktif") {
                try {
                    if (rawWaktu.contains("T")) {
                        val splitT = rawWaktu.split("T")[1]
                        val splitKolon = splitT.split(":")
                        jamBersih = "${splitKolon[0]}:${splitKolon[1]}"
                    } else if (rawWaktu.contains(" ")) {
                        val splitSpasi = rawWaktu.split(" ")[1]
                        val splitKolon = splitSpasi.split(":")
                        jamBersih = "${splitKolon[0]}:${splitKolon[1]}"
                    } else if (rawWaktu.contains(":")) {
                        val splitKolon = rawWaktu.split(":")
                        jamBersih = "${splitKolon[0]}:${splitKolon[1]}"
                    } else {
                        jamBersih = rawWaktu
                    }
                } catch (e: Exception) {
                    jamBersih = "07:30"
                }
            }

            holder.txtHabitProgress.text = "${habit.target ?: ""} • Pengingat: $jamBersih"

            holder.cbHabitDone.setOnCheckedChangeListener(null)

            val statusFinalCentang = checkedStatusMap[habit.id] ?: habit.isChecked
            holder.cbHabitDone.isChecked = statusFinalCentang

            holder.cbHabitDone.setOnCheckedChangeListener { _, isChecked ->
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    val targetHabit = list[currentPos]

                    checkedStatusMap[targetHabit.id] = isChecked
                    targetHabit.isChecked = isChecked

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val tanggalTargetDinamis = sdf.format(getTanggalAktif())

                    RetrofitClient.instance.toggleChecklist(
                        habitId = targetHabit.id,
                        date = tanggalTargetDinamis,
                        isChecked = if (isChecked) 1 else 0
                    ).enqueue(object : Callback<CommonResponse> {
                        override fun onResponse(
                            call: Call<CommonResponse>,
                            response: Response<CommonResponse>
                        ) {
                            if (response.isSuccessful) {
                                if (isChecked) {
                                    Toast.makeText(context, "${targetHabit.name} berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Riwayat dicabut, data terhapus dari Laravel!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Server menolak perubahan status.", Toast.LENGTH_LONG).show()
                                checkedStatusMap[targetHabit.id] = !isChecked
                                targetHabit.isChecked = !isChecked
                                notifyItemChanged(currentPos)
                            }
                        }

                        override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                            Toast.makeText(context, "Gagal sinkronisasi data ke server.", Toast.LENGTH_SHORT).show()
                            checkedStatusMap[targetHabit.id] = !isChecked
                            targetHabit.isChecked = !isChecked
                            notifyItemChanged(currentPos)
                        }
                    })
                }
            }
        }

        override fun getItemCount(): Int = list.size
    }
}