package com.example.trackee

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarAdapter(
    private val dates: List<Date>,
    private var selectedDate: Date,
    private val onDateClickListener: (Date) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val sdfDayName = SimpleDateFormat("EEE", Locale("id", "ID")) // Format nama hari (Sen, Sel, Rab)
    private val sdfDayNum = SimpleDateFormat("dd", Locale.getDefault())    // Format angka tanggal (01, 02)
    private val sdfCheck = SimpleDateFormat("yyyyMMdd", Locale.getDefault()) // Pembanding tanggal unik

    class CalendarViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val cardCalendar: CardView = v.findViewById(R.id.cardCalendar)
        val calendarContainer: LinearLayout = v.findViewById(R.id.calendarContainer)
        val tvDayName: TextView = v.findViewById(R.id.tvDayName)
        val tvDayNumber: TextView = v.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_date, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = dates[position]

        holder.tvDayName.text = sdfDayName.format(date).uppercase(Locale.getDefault())
        holder.tvDayNumber.text = sdfDayNum.format(date)

        // Cek apakah item tanggal ini merupakan tanggal yang sedang aktif dipilih oleh user
        val isSelected = sdfCheck.format(date) == sdfCheck.format(selectedDate)

        if (isSelected) {
            // =========================================================================
            // TEMPAT PERUBAHAN: State AKTIF diubah menjadi Ungu Pastel Matching (#937BB7)
            // =========================================================================
            holder.cardCalendar.setCardBackgroundColor(Color.parseColor("#937BB7"))
            holder.tvDayName.setTextColor(Color.WHITE) // Diubah ke Putih agar teks hari kontras & estetik
            holder.tvDayNumber.setTextColor(Color.WHITE) // Angka tanggal diubah ke Putih bersih
            holder.cardCalendar.elevation = 4f
        } else {
            // Tampilan pasif default (Transparan menyatu dengan background gradient utama)
            holder.cardCalendar.setCardBackgroundColor(Color.TRANSPARENT)
            holder.tvDayName.setTextColor(Color.parseColor("#B0A6C7"))
            holder.tvDayNumber.setTextColor(Color.parseColor("#4A347D"))
            holder.cardCalendar.elevation = 0f
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedDate
            selectedDate = date

            // Ambil index untuk melacak tanggal mana yang harus di-refresh tampilannya
            val prevIndex = indexDariTanggal(previousSelected)
            if (prevIndex != -1) notifyItemChanged(prevIndex)
            notifyItemChanged(position)

            // Jalankan fungsi callback aksi milik TodayActivity
            onDateClickListener(date)
        }
    }

    override fun getItemCount(): Int = dates.size

    // =========================================================================
    // TAMBAHAN BARU: Dipanggil otomatis oleh TodayActivity saat kalender digeser
    // =========================================================================
    fun updateSelectedDate(newDate: Date) {
        // Jangan refresh kalau tanggalnya masih sama
        if (sdfCheck.format(selectedDate) == sdfCheck.format(newDate)) return

        val previousSelected = selectedDate
        selectedDate = newDate

        // Cari index item lama dan baru berdasarkan format yyyyMMdd agar akurat
        val prevIndex = indexDariTanggal(previousSelected)
        val newIndex = indexDariTanggal(newDate)

        // Refresh komponen yang berubah saja demi menghemat performa UI render
        if (prevIndex != -1) notifyItemChanged(prevIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    // Fungsi utilitas internal untuk mencari index tanggal secara presisi tanpa terpengaruh objek milidetik jam
    private fun indexDariTanggal(targetDate: Date): Int {
        val targetString = sdfCheck.format(targetDate)
        return dates.indexOfFirst { sdfCheck.format(it) == targetString }
    }
}