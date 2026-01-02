package kr.sweetapps.alcoholictimer.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * 모든 이벤트 로깅 실행을 담당하는 싱글톤 클래스
 */
object AnalyticsManager {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    private fun log(name: String, block: Bundle.() -> Unit = {}) {
        if (!::firebaseAnalytics.isInitialized) return

        val bundle = Bundle().apply(block)
        // Debug log to make event emissions visible in logcat during development
        try {
            Log.d("AnalyticsManager", "logEvent: $name -> ${bundleToString(bundle)}")
        } catch (_: Throwable) {
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    // Helper to print bundle contents for logging
    private fun bundleToString(bundle: Bundle): String {
        val sb = StringBuilder("{")
        for (key in bundle.keySet()) {
            if (sb.length > 1) sb.append(", ")
            sb.append(key).append("=").append(bundle.get(key))
        }
        sb.append("}")
        return sb.toString()
    }

    /**
     * [NEW] User Property 설정 (2025-12-31)
     *
     * Firebase에 사용자 속성을 저장하여 대시보드에서 세분화된 분석 가능
     *
     * @param propertyName 속성 이름
     * @param value 속성 값
     */
    fun setUserProperty(propertyName: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(propertyName, value)
            Log.d("AnalyticsManager", "✅ User Property Set: $propertyName = $value")
        } catch (e: Exception) {
            Log.e("AnalyticsManager", "❌ Failed to set user property: $propertyName", e)
        }
    }

    // AdRevenue: 광고 수익 발생
    fun logAdRevenue(value: Double, currency: String, adType: String) = log(AnalyticsEvents.AD_REVENUE) {
        putDouble(AnalyticsParams.VALUE, value)
        putString(AnalyticsParams.CURRENCY, currency)
        putString(AnalyticsParams.AD_TYPE, adType)
    }

    // TimerStart: 금주 타이머 시작
    fun logTimerStart(targetDays: Int, hadActiveGoal: Boolean, startTs: Long) = log(AnalyticsEvents.TIMER_START) {
        putInt(AnalyticsParams.TARGET_DAYS, targetDays)
        putBoolean(AnalyticsParams.HAD_ACTIVE_GOAL, hadActiveGoal)
        putLong(AnalyticsParams.START_TS, startTs)
    }

    // TimerEnd: 사용자가 타이머 중지 (중도 포기)
    fun logTimerEnd(targetDays: Int, actualDays: Int, reason: String, startTs: Long, endTs: Long) = log(AnalyticsEvents.TIMER_END) {
        putInt(AnalyticsParams.TARGET_DAYS, targetDays)
        putInt(AnalyticsParams.ACTUAL_DAYS, actualDays)
        putString(AnalyticsParams.FAIL_REASON, reason)
        putLong(AnalyticsParams.START_TS, startTs)
        putLong(AnalyticsParams.END_TS, endTs)
    }

    // TimerFinish: 금주 목표 달성
    fun logTimerFinish(targetDays: Int, actualDays: Int, startTs: Long, endTs: Long) = log(AnalyticsEvents.TIMER_FINISH) {
        putInt(AnalyticsParams.TARGET_DAYS, targetDays)
        putInt(AnalyticsParams.ACTUAL_DAYS, actualDays)
        putLong(AnalyticsParams.START_TS, startTs)
        putLong(AnalyticsParams.END_TS, endTs)
    }

    // AdImpression: 광고 노출
    fun logAdImpression(adType: String) = log(AnalyticsEvents.AD_IMPRESSION) {
        putString(AnalyticsParams.AD_TYPE, adType)
    }

    // AdClick: 광고 클릭
    fun logAdClick(adType: String) = log(AnalyticsEvents.AD_CLICK) {
        putString(AnalyticsParams.AD_TYPE, adType)
    }

    // [REMOVED] ViewRecords, ChangeRecordView, ViewRecordDetail 삭제 (2026-01-02)
    // - 너무 세분화되어 데이터 노이즈만 발생
    // - screen_view로 충분히 대체 가능

    // [NEW] DiarySave: 일기 저장 (2026-01-02)
    fun logDiarySave(
        mood: String,
        contentLength: Int,
        hasImage: Boolean,
        dayCount: Int
    ) = log(AnalyticsEvents.DIARY_SAVE) {
        putString(AnalyticsParams.MOOD, mood)
        putInt(AnalyticsParams.CONTENT_LENGTH, contentLength)
        putBoolean(AnalyticsParams.HAS_IMAGE, hasImage)
        putInt(AnalyticsParams.DAY_COUNT, dayCount)
    }


    // [MODIFIED] CommunityPost: 커뮤니티 글 작성 (일기 제외) (2026-01-02)
    fun logCommunityPost(
        postType: String, // "challenge" only
        hasImage: Boolean,
        contentLength: Int,
        tagType: String?,
        userLevel: Int,
        days: Int
    ) = log(AnalyticsEvents.COMMUNITY_POST) {
        putString(AnalyticsParams.POST_TYPE, postType)
        putBoolean(AnalyticsParams.HAS_IMAGE, hasImage)
        putInt(AnalyticsParams.CONTENT_LENGTH, contentLength)
        tagType?.let { putString(AnalyticsParams.TAG_TYPE, it) }
        putInt(AnalyticsParams.USER_LEVEL, userLevel)
        putInt(AnalyticsParams.DAYS, days)
    }

    // [REMOVED] ScreenView 삭제 (2026-01-02)
    // - 모든 화면 전환을 추적하면 데이터가 너무 많아짐
    // - 핵심 화면만 선별적으로 추적하는 것이 효율적

    // [NEW] TimerGiveUp: 타이머 포기 (2025-12-31)
    fun logTimerGiveUp(
        targetDays: Int,
        actualDays: Int,
        quitReason: String,
        startTs: Long,
        quitTs: Long,
        progressPercent: Float
    ) = log(AnalyticsEvents.TIMER_GIVE_UP) {
        putInt(AnalyticsParams.TARGET_DAYS, targetDays)
        putInt(AnalyticsParams.ACTUAL_DAYS, actualDays)
        putString(AnalyticsParams.QUIT_REASON, quitReason)
        putLong(AnalyticsParams.START_TS, startTs)
        putLong(AnalyticsParams.QUIT_TS, quitTs)
        putFloat(AnalyticsParams.PROGRESS_PERCENT, progressPercent)
    }

    // [NEW] SessionStart: 세션 시작 (2025-12-31)
    fun logSessionStart(
        isFirstSession: Boolean,
        daysSinceInstall: Int,
        timerStatus: String
    ) = log(AnalyticsEvents.SESSION_START) {
        putBoolean(AnalyticsParams.IS_FIRST_SESSION, isFirstSession)
        putInt(AnalyticsParams.DAYS_SINCE_INSTALL, daysSinceInstall)
        putString(AnalyticsParams.TIMER_STATUS, timerStatus)
    }

    // [NEW] LevelUp: 레벨 업 달성 (2026-01-02)
    fun logLevelUp(
        oldLevel: Int,
        newLevel: Int,
        totalDays: Int,
        levelName: String
    ) = log(AnalyticsEvents.LEVEL_UP) {
        putInt(AnalyticsParams.OLD_LEVEL, oldLevel)
        putInt(AnalyticsParams.NEW_LEVEL, newLevel)
        putInt(AnalyticsParams.TOTAL_DAYS, totalDays)
        putString(AnalyticsParams.LEVEL_NAME, levelName)
        putLong(AnalyticsParams.ACHIEVEMENT_TS, System.currentTimeMillis())
    }

    // [REMOVED] ScreenView 삭제 (2026-01-02)

    // [NEW] SettingsChange: 설정 변경 (2025-12-31)
    fun logSettingsChange(
        settingType: String,
        oldValue: String?,
        newValue: String
    ) = log(AnalyticsEvents.SETTINGS_CHANGE) {
        putString(AnalyticsParams.SETTING_TYPE, settingType)
        oldValue?.let { putString(AnalyticsParams.OLD_VALUE, it) }
        putString(AnalyticsParams.NEW_VALUE, newValue)
    }

    // [NEW] NotificationOpen: 알림 클릭 (2025-12-31)
    fun logNotificationOpen(
        notificationId: Int,
        groupType: String,
        targetScreen: String
    ) = log(AnalyticsEvents.NOTIFICATION_OPEN) {
        putInt(AnalyticsParams.NOTIFICATION_ID, notificationId)
        putString(AnalyticsParams.GROUP_TYPE, groupType)
        putString(AnalyticsParams.TARGET_SCREEN, targetScreen)
        putLong(AnalyticsParams.OPEN_TS, System.currentTimeMillis())
    }
}
