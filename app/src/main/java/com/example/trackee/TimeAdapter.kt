package com.example.trackee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimeAdapter(private val items: List<String>) :
    RecyclerView.Adapter<TimeAdapter.TimeViewHolder>() {

    private var centerPosition = -1

    class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tvNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_picker, parent, false)
        return TimeViewHolder(v)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.tv.text = items[position]

        holder.itemView.scaleX = 0.8f
        holder.itemView.scaleY = 0.8f
        holder.tv.alpha = 0.4f

        if (position == centerPosition) {
            holder.itemView.scaleX = 1.25f
            holder.itemView.scaleY = 1.25f
            holder.tv.alpha = 1f
        }
    }

    fun updateCenterPosition(position: Int) {
        centerPosition = position
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}
