package com.example.alcoholictimer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alcoholictimer.R
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.Constants

/**
 * 최근 활동 목록을 표시하는 RecyclerView 어댑터
 */
class RecentActivityAdapter(
    private var activities: List<RecentActivity>,
    private val onItemClick: (RecentActivity) -> Unit = {}
) : RecyclerView.Adapter<RecentActivityAdapter.RecentActivityViewHolder>() {

    /**
     * ViewHolder 클래스
     */
    class RecentActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivActivityIcon: ImageView = itemView.findViewById(R.id.ivActivityIcon)
        val tvEndDate: TextView = itemView.findViewById(R.id.tvEndDate)
        val tvActivityTitle: TextView = itemView.findViewById(R.id.tvActivityTitle)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvResult: TextView = itemView.findViewById(R.id.tvResult)
        val tvSavedMoney: TextView = itemView.findViewById(R.id.tvSavedMoney)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return RecentActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentActivityViewHolder, position: Int) {
        val activity = activities[position]

        // 종료일 표시
        holder.tvEndDate.text = activity.endDate

        // 활동 제목 표시
        holder.tvActivityTitle.text = activity.title

        // 지속 시간 표시 (테스트 모드에 따른 단위)
        val timeUnit = when (activity.testMode) {
            Constants.TEST_MODE_SECOND -> "초"
            Constants.TEST_MODE_MINUTE -> "분"
            else -> "일"
        }

        val durationText = if (activity.hours > 0) {
            "${activity.duration}${timeUnit} ${activity.hours}시간"
        } else {
            "${activity.duration}${timeUnit}"
        }
        holder.tvDuration.text = durationText

        // 성공/실패 표시
        if (activity.isSuccess) {
            holder.tvResult.text = "성공"
            holder.tvResult.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            holder.ivActivityIcon.setImageResource(android.R.drawable.ic_menu_agenda) // 기본 아이콘 사용
        } else {
            holder.tvResult.text = "실패"
            holder.tvResult.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            holder.ivActivityIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // 취소 아이콘 사용
        }

        // 절약 금액 표시
        if (activity.savedMoney > 0) {
            holder.tvSavedMoney.text = "${activity.savedMoney}만원"
            holder.tvSavedMoney.visibility = View.VISIBLE
        } else {
            holder.tvSavedMoney.visibility = View.GONE
        }

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener { onItemClick(activity) }
    }

    override fun getItemCount(): Int = activities.size

    /**
     * 데이터 업데이트 메서드
     */
    fun updateData(newActivities: List<RecentActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}
