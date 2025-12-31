package kr.sweetapps.alcoholictimer.util.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 알림 채널 관리 클래스
 *
 * Android 8.0 (API 26+)부터 필요한 Notification Channel 생성 및 관리
 *
 * @since 2025-12-31
 */
object NotificationChannelManager {

    // 채널 ID 상수
    const val CHANNEL_ID_RETENTION = "retention_notifications"
    const val CHANNEL_ID_ACHIEVEMENT = "achievement_notifications"

    // 채널 이름 및 설명
    private const val CHANNEL_NAME_RETENTION = "리텐션 알림"
    private const val CHANNEL_DESC_RETENTION = "타이머 리마인더 및 재도전 유도 알림"

    private const val CHANNEL_NAME_ACHIEVEMENT = "성취 알림"
    private const val CHANNEL_DESC_ACHIEVEMENT = "목표 달성 축하 및 레벨업 알림"

    /**
     * 모든 알림 채널 생성
     * Application.onCreate() 또는 MainActivity.onCreate()에서 호출
     *
     * @param context Application Context
     */
    fun createNotificationChannels(context: Context) {
        // Android 8.0 미만에서는 채널이 필요 없음
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 리텐션 알림 채널 (중요도: 높음)
        val retentionChannel = NotificationChannel(
            CHANNEL_ID_RETENTION,
            CHANNEL_NAME_RETENTION,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC_RETENTION
            enableLights(true)
            enableVibration(true)
        }

        // 2. 성취 알림 채널 (중요도: 높음)
        val achievementChannel = NotificationChannel(
            CHANNEL_ID_ACHIEVEMENT,
            CHANNEL_NAME_ACHIEVEMENT,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC_ACHIEVEMENT
            enableLights(true)
            enableVibration(true)
        }

        // 채널 등록
        notificationManager.createNotificationChannel(retentionChannel)
        notificationManager.createNotificationChannel(achievementChannel)

        android.util.Log.d("NotificationChannel", "✅ Notification channels created")
    }
}

