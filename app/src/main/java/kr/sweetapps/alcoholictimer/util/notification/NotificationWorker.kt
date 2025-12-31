package kr.sweetapps.alcoholictimer.util.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.main.MainActivity
import kr.sweetapps.alcoholictimer.util.manager.RetentionPreferenceManager

/**
 * WorkManager Worker for Retention Notifications
 *
 * 예약된 시간에 실행되어 사용자 상태를 체크하고 조건에 맞으면 알림 발송
 *
 * @since 2025-12-31
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        // Worker Input Data Keys
        const val KEY_NOTIFICATION_GROUP = "notification_group"
        const val KEY_NOTIFICATION_TITLE = "notification_title"
        const val KEY_NOTIFICATION_MESSAGE = "notification_message"
        const val KEY_NOTIFICATION_ID = "notification_id"

        // Notification Groups
        const val GROUP_NEW_USER = "group_new_user"      // 그룹 A: 신규 유저
        const val GROUP_ACTIVE_USER = "group_active_user" // 그룹 B: 활성 유저
        const val GROUP_RESTING_USER = "group_resting_user" // 그룹 C: 휴식 유저
    }

    override fun doWork(): Result {
        return try {
            val group = inputData.getString(KEY_NOTIFICATION_GROUP) ?: return Result.failure()
            val title = inputData.getString(KEY_NOTIFICATION_TITLE) ?: "ZERO"
            val message = inputData.getString(KEY_NOTIFICATION_MESSAGE) ?: "금주를 시작해보세요!"
            val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0)

            android.util.Log.d("NotificationWorker", "🔔 Worker started - Group: $group, ID: $notificationId")

            // [NEW] 방해 금지 시간 체크 (22:00 ~ 10:00) (2025-12-31)
            if (isDoNotDisturbTime()) {
                android.util.Log.d("NotificationWorker", "🌙 Do Not Disturb time - notification postponed")
                // 1시간 후 재시도
                return Result.retry()
            }

            // 상태 체크: 조건에 맞지 않으면 알림 발송하지 않음
            if (!shouldShowNotification(group)) {
                android.util.Log.d("NotificationWorker", "⏭️ Notification skipped - condition not met for group: $group")
                return Result.success()
            }

            // 알림 발송
            sendNotification(title, message, notificationId)

            // 재시도 카운트 증가 (그룹 A만)
            if (group == GROUP_NEW_USER) {
                val currentCount = RetentionPreferenceManager.incrementRetryCount(applicationContext)
                android.util.Log.d("NotificationWorker", "📊 Retry count incremented: $currentCount")
            }

            // [NEW] Firebase Analytics 이벤트 전송 준비 (2025-12-31)
            logNotificationSent(group, notificationId)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "❌ Worker failed", e)
            Result.failure()
        }
    }

    /**
     * [NEW] 방해 금지 시간 체크 (22:00 ~ 10:00) (2025-12-31)
     *
     * @return true: 방해 금지 시간대, false: 알림 가능 시간대
     */
    private fun isDoNotDisturbTime(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        // 22:00 ~ 23:59 또는 00:00 ~ 09:59
        return currentHour >= 22 || currentHour < 10
    }

    /**
     * 그룹별 조건 체크
     *
     * [UPDATED] 안전 장치 강화 및 상세 로그 추가 (2025-12-31)
     *
     * @param group 알림 그룹 (A/B/C)
     * @return true: 알림 발송 가능, false: 발송하지 않음
     */
    private fun shouldShowNotification(group: String): Boolean {
        val prefs = RetentionPreferenceManager
        val isTimerRunning = prefs.isTimerRunning(applicationContext)
        val retryCount = prefs.getRetryCount(applicationContext)

        val result = when (group) {
            GROUP_NEW_USER -> {
                // 그룹 A: 신규 유저
                // 조건: 타이머가 실행 중이 아니고, 재시도 횟수가 3회 미만
                val shouldShow = !isTimerRunning && retryCount < 3
                android.util.Log.d("NotificationWorker", "[Group A Check] isTimerRunning=$isTimerRunning, retryCount=$retryCount → shouldShow=$shouldShow")
                shouldShow
            }

            GROUP_ACTIVE_USER -> {
                // 그룹 B: 활성 유저 (타이머 실행 중)
                // 조건: 타이머가 실행 중
                val shouldShow = isTimerRunning
                android.util.Log.d("NotificationWorker", "[Group B Check] isTimerRunning=$isTimerRunning → shouldShow=$shouldShow")
                shouldShow
            }

            GROUP_RESTING_USER -> {
                // 그룹 C: 휴식 유저 (타이머 종료/포기)
                // 조건: 타이머가 실행 중이 아님
                val shouldShow = !isTimerRunning
                android.util.Log.d("NotificationWorker", "[Group C Check] isTimerRunning=$isTimerRunning → shouldShow=$shouldShow")
                shouldShow
            }

            else -> {
                android.util.Log.w("NotificationWorker", "⚠️ Unknown group: $group")
                false
            }
        }

        // [NEW] 안전 장치: 예약 시점과 현재 상태가 모순되는지 최종 확인 (2025-12-31)
        if (!result) {
            android.util.Log.w("NotificationWorker", "🛡️ Safety Check: Notification blocked due to condition mismatch (group=$group)")
        }

        return result
    }

    /**
     * 알림 발송
     *
     * [UPDATED] 딥링크 정보 추가 (2025-12-31)
     *
     * @param title 알림 제목
     * @param message 알림 메시지
     * @param notificationId 알림 ID
     */
    private fun sendNotification(title: String, message: String, notificationId: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // [NEW] 딥링크 정보 추가 (2025-12-31)
        val group = inputData.getString(KEY_NOTIFICATION_GROUP) ?: GROUP_NEW_USER
        val targetScreen = kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.getTargetScreen(group)
        val showBadgeAnimation = kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.shouldShowBadgeAnimation(group)

        // 앱 실행 Intent with Deep Link
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_SCREEN_ROUTE, targetScreen)
            putExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_GROUP_TYPE, group)
            putExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_SHOW_BADGE_ANIMATION, showBadgeAnimation)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌드
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannelManager.CHANNEL_ID_RETENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 알림 표시
        notificationManager.notify(notificationId, notification)

        android.util.Log.d("NotificationWorker", "✅ Notification sent - ID: $notificationId, Title: $title, Target: $targetScreen")
    }

    /**
     * [NEW] Firebase Analytics 이벤트 로깅 (2025-12-31)
     * 향후 ad_impression과 연동 가능
     *
     * @param group 알림 그룹
     * @param notificationId 알림 ID
     */
    private fun logNotificationSent(group: String, notificationId: Int) {
        try {
            // TODO: 향후 AnalyticsManager.logNotificationSent() 구현
            android.util.Log.d("NotificationWorker", "📊 [Analytics] Notification sent - Group: $group, ID: $notificationId")

            // 예시: ad_impression과 유사한 방식으로 로깅
            // AnalyticsManager.logEvent("notification_sent") {
            //     putString("group", group)
            //     putInt("notification_id", notificationId)
            // }
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "Failed to log analytics", e)
        }
    }
}

