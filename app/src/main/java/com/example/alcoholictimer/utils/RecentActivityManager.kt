package com.example.alcoholictimer.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.alcoholictimer.models.RecentActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 최근 활동을 관리하는 매니저 클래스
 * 금주 완료/중단 기록을 최대 5개까지 저장하고 관리합니다.
 */
object RecentActivityManager {
    private const val PREFS_NAME = "recent_activities"
    private const val KEY_ACTIVITIES = "activities_json"

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
    }

    private fun getPrefs(): SharedPreferences? {
        return context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 완료된 금주 활동을 저장합니다
     */
    fun saveCompletedActivity(startTime: Long, endTime: Long, targetDays: Int, testMode: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.format(Date(startTime))
        val endDate = dateFormat.format(Date(endTime))

        // 실제 지속 시간 계산 (테스트 모드에 따라)
        val actualDurationHours = ((endTime - startTime) / (1000 * 60 * 60)).toInt()

        // 절약 금액 계산 (예시: 하루 2만원 기준)
        val savedMoney = when (testMode) {
            Constants.TEST_MODE_SECOND -> targetDays * 2 // 초 단위 테스트: 초당 2만원
            Constants.TEST_MODE_MINUTE -> targetDays * 2 // 분 단위 테스트: 분당 2만원
            else -> targetDays * 2 // 실제 모드: 일당 2만원
        }

        val activity = RecentActivity(
            startDate = startDate,
            endDate = endDate,
            title = "금주 챌린지",
            duration = targetDays,
            hours = actualDurationHours,
            isSuccess = true,
            savedMoney = savedMoney,
            testMode = testMode
        )

        addRecentActivity(activity)
    }

    /**
     * 중단된 금주 활동을 저장합니다
     */
    fun saveStoppedActivity(startTime: Long, endTime: Long, testMode: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.format(Date(startTime))
        val endDate = dateFormat.format(Date(endTime))

        // 실제 지속 시간 계산
        val actualDurationHours = ((endTime - startTime) / (1000 * 60 * 60)).toInt()
        val actualDurationDays = when (testMode) {
            Constants.TEST_MODE_SECOND -> ((endTime - startTime) / 1000).toInt() // 초 단위
            Constants.TEST_MODE_MINUTE -> ((endTime - startTime) / (1000 * 60)).toInt() // 분 단위
            else -> ((endTime - startTime) / (1000 * 60 * 60 * 24)).toInt() // 일 단위
        }

        val activity = RecentActivity(
            startDate = startDate,
            endDate = endDate,
            title = "금주 시도",
            duration = actualDurationDays,
            hours = actualDurationHours,
            isSuccess = false,
            savedMoney = 0,
            testMode = testMode
        )

        addRecentActivity(activity)
    }

    /**
     * 최근 활동을 추가합니다 (최대 5개 유지)
     */
    private fun addRecentActivity(activity: RecentActivity) {
        val activities = getRecentActivities().toMutableList()
        activities.add(0, activity) // 최신 활동을 맨 앞에 추가

        // 최대 5개만 유지
        while (activities.size > 5) {
            activities.removeLast()
        }

        saveActivities(activities)
    }

    /**
     * 저장된 최근 활동 목록을 반환합니다
     */
    fun getRecentActivities(): List<RecentActivity> {
        val prefs = getPrefs() ?: return emptyList()
        val jsonString = prefs.getString(KEY_ACTIVITIES, "[]") ?: "[]"

        return try {
            RecentActivity.fromJsonArray(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 활동 목록을 저장합니다
     */
    private fun saveActivities(activities: List<RecentActivity>) {
        val prefs = getPrefs() ?: return
        val jsonString = RecentActivity.toJsonArray(activities)

        prefs.edit()
            .putString(KEY_ACTIVITIES, jsonString)
            .apply()
    }

    /**
     * 모든 최근 활동을 삭제합니다
     */
    fun clearRecentActivities() {
        val prefs = getPrefs() ?: return
        prefs.edit()
            .remove(KEY_ACTIVITIES)
            .apply()
    }
}
