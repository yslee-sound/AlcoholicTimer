package kr.sweetapps.alcoholictimer.util.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.sweetapps.alcoholictimer.util.manager.RetentionPreferenceManager

/**
 * 기기 재부팅 수신 BroadcastReceiver
 *
 * BOOT_COMPLETED를 수신하여 예약된 알림을 재등록
 *
 * @since 2025-12-31
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            android.util.Log.d("BootCompletedReceiver", "🔄 Device rebooted - re-scheduling notifications")

            // 사용자 상태에 따라 알림 재등록
            reScheduleNotifications(context)
        }
    }

    /**
     * 사용자 상태에 따라 알림 재등록
     *
     * @param context Context
     */
    private fun reScheduleNotifications(context: Context) {
        val prefs = RetentionPreferenceManager
        val isTimerRunning = prefs.isTimerRunning(context)
        val startTime = prefs.getStartTime(context)
        val lastEndTime = prefs.getLastEndTime(context)
        val retryCount = prefs.getRetryCount(context)

        when {
            // Case 1: 타이머 실행 중 → 그룹 B 재등록
            isTimerRunning && startTime > 0 -> {
                android.util.Log.d("BootCompletedReceiver", "✅ Timer is running - re-scheduling Group B")
                RetentionNotificationManager.scheduleGroupBNotifications(context, startTime)
            }

            // Case 2: 최근 종료/포기 24시간 이내 → 그룹 C 재등록
            lastEndTime > 0 && System.currentTimeMillis() - lastEndTime < 24 * 60 * 60 * 1000 -> {
                android.util.Log.d("BootCompletedReceiver", "✅ Recently stopped - re-scheduling Group C")
                RetentionNotificationManager.scheduleGroupCNotifications(context)
            }

            // Case 3: 신규 유저 (재시도 횟수 3회 미만) → 그룹 A 재등록
            retryCount < 3 -> {
                android.util.Log.d("BootCompletedReceiver", "✅ New user - re-scheduling Group A")
                RetentionNotificationManager.scheduleGroupANotifications(context)
            }

            else -> {
                android.util.Log.d("BootCompletedReceiver", "ℹ️ No notifications to re-schedule")
            }
        }
    }
}

