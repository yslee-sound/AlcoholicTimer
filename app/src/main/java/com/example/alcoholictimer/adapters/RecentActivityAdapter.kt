package com.example.alcoholictimer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alcoholictimer.R
import com.example.alcoholictimer.models.RecentActivity
import java.text.SimpleDateFormat
import java.util.Locale

class RecentActivityAdapter(private val activities: List<RecentActivity>) :
    RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvEndDate)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvSavedMoney: TextView = view.findViewById(R.id.tvSavedMoney)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        holder.tvDate.text = activity.endDate
        holder.tvDuration.text = "${activity.duration}일"
        holder.tvStatus.text = if (activity.isCompleted) "성공" else "실패"

        // 하루 평균 음주 비용을 5000원으로 가정하고 절약 금액 계산
        val savedMoney = activity.duration * 5000
        val savedMoneyText = when {
            savedMoney >= 10000 -> String.format("%.1f만원", savedMoney / 10000.0)
            else -> "${savedMoney}원"
        }
        holder.tvSavedMoney.text = "${savedMoneyText} 절약"
    }

    override fun getItemCount() = activities.size
}
