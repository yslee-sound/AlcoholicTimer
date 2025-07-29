package com.example.alcoholictimer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LevelHistoryAdapter(private val historyList: List<LevelHistoryItem>) :
    RecyclerView.Adapter<LevelHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAchievement: TextView = itemView.findViewById(R.id.tvAchievement)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_level_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvDate.text = item.date
        holder.tvAchievement.text = item.achievement
    }

    override fun getItemCount() = historyList.size
}

data class LevelHistoryItem(
    val date: String,
    val achievement: String
)
