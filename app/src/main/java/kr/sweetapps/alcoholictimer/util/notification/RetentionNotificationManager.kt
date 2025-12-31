package kr.sweetapps.alcoholictimer.util.notification

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 리텐션 알림 예약 관리 클래스
 *
 * 그룹별 알림 예약, 취소, 재예약 로직 관리
 *
 * @since 2025-12-31
 */
object RetentionNotificationManager {

    // ============================================================
    // [TEST MODE] 알림 발송 테스트용 시간 단축 (2025-12-31)
    // ============================================================
    private const val TEST_MODE = true  // ⚠️ 배포 시 false로 변경 필수!

    // 실제 지연 시간 (시간 단위)
    private const val DELAY_24H = 24L   // 1일
    private const val DELAY_72H = 72L   // 3일
    private const val DELAY_168H = 168L // 7일
    private const val DELAY_720H = 720L // 30일

    // 테스트 지연 시간 (초 단위)
    private const val TEST_DELAY_24H = 10L   // 10초
    private const val TEST_DELAY_72H = 20L   // 20초
    private const val TEST_DELAY_168H = 30L  // 30초

    // Work Request Tags (취소 시 사용)
    private const val TAG_GROUP_A = "notification_group_a"
    private const val TAG_GROUP_B = "notification_group_b"
    private const val TAG_GROUP_C = "notification_group_c"

    // Notification IDs
    private const val NOTIFICATION_ID_GROUP_A_1 = 1001
    private const val NOTIFICATION_ID_GROUP_A_2 = 1002
    private const val NOTIFICATION_ID_GROUP_A_3 = 1003
    private const val NOTIFICATION_ID_GROUP_B_3D = 1004
    private const val NOTIFICATION_ID_GROUP_B_7D = 1005
    private const val NOTIFICATION_ID_GROUP_B_30D = 1006
    private const val NOTIFICATION_ID_GROUP_C = 1007

    /**
     * 그룹 A: 신규 유저 알림 예약
     *
     * [UPDATED] 3번의 법칙 적용 (2025-12-31)
     * - 1차: 24시간 후
     * - 2차: 1차 발송 2일 후 (72시간 = 3일차)
     * - 3차: 2차 발송 4일 후 (168시간 = 7일차)
     *
     * @param context Context
     */
    fun scheduleGroupANotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 기존 그룹 A 알림 취소
        cancelGroupANotifications(context)

        // 1차: 24시간 후 (TEST: 10초)
        scheduleNotificationWithTestMode(
            context = context,
            delayHours = DELAY_24H,
            testDelaySeconds = TEST_DELAY_24H,
            group = NotificationWorker.GROUP_NEW_USER,
            title = RetentionMessages.GroupA.TITLE_1,
            message = RetentionMessages.GroupA.MESSAGE_1,
            notificationId = NOTIFICATION_ID_GROUP_A_1,
            tag = TAG_GROUP_A
        )

        // 2차: 72시간 후 (TEST: 20초)
        scheduleNotificationWithTestMode(
            context = context,
            delayHours = DELAY_72H,
            testDelaySeconds = TEST_DELAY_72H,
            group = NotificationWorker.GROUP_NEW_USER,
            title = RetentionMessages.GroupA.TITLE_2,
            message = RetentionMessages.GroupA.MESSAGE_2,
            notificationId = NOTIFICATION_ID_GROUP_A_2,
            tag = TAG_GROUP_A
        )

        // 3차: 168시간 후 (TEST: 30초)
        scheduleNotificationWithTestMode(
            context = context,
            delayHours = DELAY_168H,
            testDelaySeconds = TEST_DELAY_168H,
            group = NotificationWorker.GROUP_NEW_USER,
            title = RetentionMessages.GroupA.TITLE_3,
            message = RetentionMessages.GroupA.MESSAGE_3,
            notificationId = NOTIFICATION_ID_GROUP_A_3,
            tag = TAG_GROUP_A
        )

        // [NEW] User Property 설정 (2025-12-31)
        kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.setUserProperty("retention_group", "group_a_new_user")

        val mode = if (TEST_MODE) "TEST MODE (10s, 20s, 30s)" else "24h, 72h, 168h"
        android.util.Log.d("RetentionNotification", "✅ Group A notifications scheduled - $mode")
    }

    /**
     * 그룹 B: 활성 유저 알림 예약
     *
     * 타이머 시작 시점에 3일, 7일, 30일 뒤 알림 예약
     *
     * @param context Context
     * @param startTimeMillis 타이머 시작 시각 (UTC milliseconds)
     */
    fun scheduleGroupBNotifications(context: Context, startTimeMillis: Long) {
        val workManager = WorkManager.getInstance(context)

        // 기존 그룹 B 알림 취소
        cancelGroupBNotifications(context)

        val now = System.currentTimeMillis()
        val elapsedHours = (now - startTimeMillis) / (60 * 60 * 1000)

        // 3일 알림 (이미 3일이 지났다면 스킵)
        if (elapsedHours < 72) {
            scheduleNotification(
                context = context,
                delayHours = 72 - elapsedHours,
                group = NotificationWorker.GROUP_ACTIVE_USER,
                title = RetentionMessages.GroupB.TITLE_3D,
                message = RetentionMessages.GroupB.MESSAGE_3D,
                notificationId = NOTIFICATION_ID_GROUP_B_3D,
                tag = TAG_GROUP_B
            )
        }

        // 7일 알림 (이미 7일이 지났다면 스킵)
        if (elapsedHours < 168) {
            scheduleNotification(
                context = context,
                delayHours = 168 - elapsedHours,
                group = NotificationWorker.GROUP_ACTIVE_USER,
                title = RetentionMessages.GroupB.TITLE_7D,
                message = RetentionMessages.GroupB.MESSAGE_7D,
                notificationId = NOTIFICATION_ID_GROUP_B_7D,
                tag = TAG_GROUP_B
            )
        }

        // 30일 알림 (이미 30일이 지났다면 스킵)
        if (elapsedHours < 720) {
            scheduleNotification(
                context = context,
                delayHours = 720 - elapsedHours,
                group = NotificationWorker.GROUP_ACTIVE_USER,
                title = RetentionMessages.GroupB.TITLE_30D,
                message = RetentionMessages.GroupB.MESSAGE_30D,
                notificationId = NOTIFICATION_ID_GROUP_B_30D,
                tag = TAG_GROUP_B
            )
        }

        // [NEW] User Property 설정 (2025-12-31)
        kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.setUserProperty("retention_group", "group_b_active_user")

        android.util.Log.d("RetentionNotification", "✅ Group B notifications scheduled (3d, 7d, 30d)")
    }

    /**
     * 그룹 C: 휴식 유저 알림 예약
     *
     * [UPDATED] D+1, D+3 알림 추가 (2025-12-31)
     * - D+1 (24시간 후): 재도전 유도
     * - D+3 (72시간 후): 간 회복 메시지
     *
     * @param context Context
     */
    fun scheduleGroupCNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 기존 그룹 C 알림 취소
        cancelGroupCNotifications(context)

        // D+1: 24시간 후 (TEST: 10초)
        scheduleNotificationWithTestMode(
            context = context,
            delayHours = DELAY_24H,
            testDelaySeconds = TEST_DELAY_24H,
            group = NotificationWorker.GROUP_RESTING_USER,
            title = RetentionMessages.GroupC.TITLE_D1,
            message = RetentionMessages.GroupC.MESSAGE_D1,
            notificationId = NOTIFICATION_ID_GROUP_C,
            tag = TAG_GROUP_C
        )

        // D+3: 72시간 후 (TEST: 20초)
        scheduleNotificationWithTestMode(
            context = context,
            delayHours = DELAY_72H,
            testDelaySeconds = TEST_DELAY_72H,
            group = NotificationWorker.GROUP_RESTING_USER,
            title = RetentionMessages.GroupC.TITLE_D3,
            message = RetentionMessages.GroupC.MESSAGE_D3,
            notificationId = NOTIFICATION_ID_GROUP_C + 1,
            tag = TAG_GROUP_C
        )

        // [NEW] User Property 설정 (2025-12-31)
        kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.setUserProperty("retention_group", "group_c_resting_user")

        val mode = if (TEST_MODE) "TEST MODE (10s, 20s)" else "24h, 72h"
        android.util.Log.d("RetentionNotification", "✅ Group C notifications scheduled - $mode")
    }

    /**
     * 그룹 A 알림 취소
     */
    fun cancelGroupANotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_GROUP_A)
        android.util.Log.d("RetentionNotification", "🗑️ Group A notifications cancelled")
    }

    /**
     * 그룹 B 알림 취소
     */
    fun cancelGroupBNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_GROUP_B)
        android.util.Log.d("RetentionNotification", "🗑️ Group B notifications cancelled")
    }

    /**
     * 그룹 C 알림 취소
     */
    fun cancelGroupCNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_GROUP_C)
        android.util.Log.d("RetentionNotification", "🗑️ Group C notifications cancelled")
    }

    /**
     * 모든 알림 취소
     */
    fun cancelAllNotifications(context: Context) {
        cancelGroupANotifications(context)
        cancelGroupBNotifications(context)
        cancelGroupCNotifications(context)
        android.util.Log.d("RetentionNotification", "🗑️ All notifications cancelled")
    }

    /**
     * [NEW] 즉시 알림 표시 (테스트용) (2025-12-31)
     *
     * WorkManager 예약 없이 즉시 알림을 표시
     * 알림 UI 및 채널 설정 테스트용
     *
     * @param context Context
     * @param title 알림 제목
     * @param message 알림 메시지
     */
    fun showImmediateTestNotification(context: Context, title: String, message: String) {
        android.util.Log.d("RetentionNotification", "🧪 TEST: Showing immediate notification")

        // 1. 채널 생성 확인
        NotificationChannelManager.createNotificationChannels(context)
        android.util.Log.d("RetentionNotification", "✅ Notification channel verified")

        // 2. 알림 표시
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // 앱 실행 Intent
        val intent = android.content.Intent(context, kr.sweetapps.alcoholictimer.ui.main.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            9999, // 테스트용 고유 ID
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌드
        val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_RETENTION)
            .setSmallIcon(kr.sweetapps.alcoholictimer.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 알림 표시
        notificationManager.notify(9999, notification)

        android.util.Log.d("RetentionNotification", "✅ TEST: Notification displayed - Title: $title")
    }

    /**
     * [NEW] 테스트 모드를 고려한 알림 예약 (2025-12-31)
     *
     * TEST_MODE가 true면 초 단위로, false면 시간 단위로 예약
     *
     * @param context Context
     * @param delayHours 실제 지연 시간 (시간 단위)
     * @param testDelaySeconds 테스트 지연 시간 (초 단위)
     * @param group 알림 그룹
     * @param title 알림 제목
     * @param message 알림 메시지
     * @param notificationId 알림 ID
     * @param tag WorkRequest 태그
     */
    private fun scheduleNotificationWithTestMode(
        context: Context,
        delayHours: Long,
        testDelaySeconds: Long,
        group: String,
        title: String,
        message: String,
        notificationId: Int,
        tag: String
    ) {
        val inputData = Data.Builder()
            .putString(NotificationWorker.KEY_NOTIFICATION_GROUP, group)
            .putString(NotificationWorker.KEY_NOTIFICATION_TITLE, title)
            .putString(NotificationWorker.KEY_NOTIFICATION_MESSAGE, message)
            .putInt(NotificationWorker.KEY_NOTIFICATION_ID, notificationId)
            .build()

        val workRequest = if (TEST_MODE) {
            // 테스트 모드: 초 단위로 예약
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(testDelaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag(tag)
                .build()
        } else {
            // 실제 모드: 시간 단위로 예약
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delayHours, TimeUnit.HOURS)
                .setInputData(inputData)
                .addTag(tag)
                .build()
        }

        WorkManager.getInstance(context).enqueue(workRequest)

        val delayInfo = if (TEST_MODE) {
            "Delay: ${testDelaySeconds}s (TEST MODE)"
        } else {
            "Delay: ${delayHours}h"
        }

        android.util.Log.d("RetentionNotification", "📅 Notification scheduled - Group: $group, $delayInfo, ID: $notificationId")
    }

    /**
     * 알림 예약 (내부 헬퍼 함수)
     *
     * @param context Context
     * @param delayHours 지연 시간 (시간 단위)
     * @param group 알림 그룹
     * @param title 알림 제목
     * @param message 알림 메시지
     * @param notificationId 알림 ID
     * @param tag WorkRequest 태그
     */
    private fun scheduleNotification(
        context: Context,
        delayHours: Long,
        group: String,
        title: String,
        message: String,
        notificationId: Int,
        tag: String
    ) {
        val inputData = Data.Builder()
            .putString(NotificationWorker.KEY_NOTIFICATION_GROUP, group)
            .putString(NotificationWorker.KEY_NOTIFICATION_TITLE, title)
            .putString(NotificationWorker.KEY_NOTIFICATION_MESSAGE, message)
            .putInt(NotificationWorker.KEY_NOTIFICATION_ID, notificationId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .setInputData(inputData)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        android.util.Log.d("RetentionNotification", "📅 Notification scheduled - Group: $group, Delay: ${delayHours}h, ID: $notificationId")
    }
}

