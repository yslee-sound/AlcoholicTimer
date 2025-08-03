package com.example.alcoholictimer.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.alcoholictimer.models.RecentActivity
import java.text.SimpleDateFormat
import java.util.*

object RecordManager {
    private const val PREFS_NAME = "recent_activities"
    private const val KEY_ACTIVITIES = "activities"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addActivity(activity: RecentActivity) {
        // 간단한 문자열 형태로 저장
        val activities = getActivities().toMutableList()
        activities.add(0, activity) // 최신 활동을 맨 앞에 추가

        // 최대 5개만 유지
        while (activities.size > 5) {
            activities.removeLast()
        }

        // 간단한 문자열 저장 방식 사용
        val activitiesString = activities.joinToString("|") {
            "${it.startDate},${it.endDate},${it.duration},${it.isCompleted}"
        }
        prefs.edit().putString(KEY_ACTIVITIES, activitiesString).apply()
    }

    fun getActivities(): List<RecentActivity> {
        val activitiesString = prefs.getString(KEY_ACTIVITIES, "") ?: ""
        if (activitiesString.isEmpty()) return emptyList()

        return activitiesString.split("|").mapNotNull { activityData ->
            val parts = activityData.split(",")
            if (parts.size == 4) {
                RecentActivity(
                    startDate = parts[0],
                    endDate = parts[1],
                    duration = parts[2].toIntOrNull() ?: 0,
                    isCompleted = parts[3].toBoolean()
                )
            } else null
        }
    }

    fun clearActivities() {
        prefs.edit().remove(KEY_ACTIVITIES).apply()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
