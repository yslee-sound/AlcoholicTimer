package com.example.alcoholictimer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LevelAdapter : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    private var levelList = listOf<Level>()
    private var currentDays = 0

    fun updateLevels(levels: List<Level>, days: Int) {
        levelList = levels
        currentDays = days
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levelList[position]
        holder.bind(level, currentDays)
    }

    override fun getItemCount(): Int = levelList.size

    class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewLevelBadge: View = itemView.findViewById(R.id.viewLevelBadge)
        private val tvLevelName: TextView = itemView.findViewById(R.id.tvLevelName)
        private val tvLevelColor: TextView = itemView.findViewById(R.id.tvLevelColor)
        private val tvLevelRange: TextView = itemView.findViewById(R.id.tvLevelRange)
        private val tvLevelDescription: TextView = itemView.findViewById(R.id.tvLevelDescription)
        private val tvCurrentIndicator: TextView = itemView.findViewById(R.id.tvCurrentIndicator)

        fun bind(level: Level, currentDays: Int) {
            // 배지 색상 설정
            viewLevelBadge.setBackgroundColor(Color.parseColor(level.color))

            // 레벨명 설정
            tvLevelName.text = level.name

            // 색상명 설정
            tvLevelColor.text = level.colorName

            // 레벨 범위 설정
            val rangeText = if (level.maxDays == Int.MAX_VALUE) {
                "${level.minDays}일 이상"
            } else {
                "${level.minDays}~${level.maxDays}일"
            }
            tvLevelRange.text = rangeText

            // 레벨 설명 설정
            tvLevelDescription.text = level.description

            // 현재 달성한 레벨인지 확인
            val isAchieved = currentDays >= level.minDays
            val isCurrentLevel = currentDays >= level.minDays && currentDays <= level.maxDays

            // 현재 레벨 표시
            if (isCurrentLevel) {
                tvCurrentIndicator.visibility = View.VISIBLE
                tvCurrentIndicator.setTextColor(Color.parseColor(level.color))
            } else {
                tvCurrentIndicator.visibility = View.GONE
            }

            if (isAchieved) {
                // 달성한 레벨 - 정상 색상 표시
                tvLevelName.setTextColor(Color.parseColor("#333333"))
                tvLevelColor.setTextColor(Color.parseColor("#666666"))
                tvLevelRange.setTextColor(Color.parseColor("#666666"))
                tvLevelDescription.setTextColor(Color.parseColor("#999999"))
                itemView.alpha = 1.0f

                // 현재 레벨인 경우 강조
                if (isCurrentLevel) {
                    tvLevelName.setTextColor(Color.parseColor(level.color))
                    tvLevelName.textSize = 17f
                } else {
                    tvLevelName.textSize = 16f
                }
            } else {
                // 미달성 레벨 - 흐리게 표시
                tvLevelName.setTextColor(Color.parseColor("#CCCCCC"))
                tvLevelColor.setTextColor(Color.parseColor("#CCCCCC"))
                tvLevelRange.setTextColor(Color.parseColor("#CCCCCC"))
                tvLevelDescription.setTextColor(Color.parseColor("#CCCCCC"))
                viewLevelBadge.setBackgroundColor(Color.parseColor("#E0E0E0"))
                itemView.alpha = 0.6f
                tvLevelName.textSize = 16f
            }
        }
    }
}
