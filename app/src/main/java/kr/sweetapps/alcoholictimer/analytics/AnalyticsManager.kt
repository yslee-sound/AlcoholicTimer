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

    // ViewRecords: 과거 금주 기록 조회
    fun logViewRecords() = log(AnalyticsEvents.VIEW_RECORDS) {}

    // ChangeRecordView: 기록 보기 화면에서 주/월/년 기준 변경
    fun logChangeRecordView(viewType: String, currentLevel: Int) = log(AnalyticsEvents.CHANGE_RECORD_VIEW) {
        putString(AnalyticsParams.VIEW_TYPE, viewType)
        putInt(AnalyticsParams.CURRENT_LEVEL, currentLevel)
    }

    // ViewRecordDetail: 특정 기록 상세 조회
    fun logViewRecordDetail(recordId: String) = log(AnalyticsEvents.VIEW_RECORD_DETAIL) {
        putString(AnalyticsParams.RECORD_ID, recordId)
    }
}
