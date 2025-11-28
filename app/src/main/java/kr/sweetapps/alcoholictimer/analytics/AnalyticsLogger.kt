package kr.sweetapps.alcoholictimer.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsLogger {
    private lateinit var fa: FirebaseAnalytics

    fun initialize(context: Context) {
        fa = FirebaseAnalytics.getInstance(context)
    }

    private fun log(name: String, block: Bundle.() -> Unit = {}) {
        if (!::fa.isInitialized) return
        val b = Bundle().apply(block)
        // Debug log to make event emissions visible in logcat during development
        try { Log.d("AnalyticsLogger", "logEvent: $name -> ${b.toString()}") } catch (_: Throwable) {}
        fa.logEvent(name, b)
    }

    // 핵심 이벤트 래퍼 (ANALYTICS_EVENTS.md 기준)
    fun timerStart(targetDays: Int, hadActive: Boolean, startTs: Long) = log("timer_start") {
        putInt("target_days", targetDays)
        putString("had_active_goal", hadActive.toString())
        putLong("start_ts", startTs)
    }

    fun timerComplete(targetDays: Int, actualDays: Int, startTs: Long, endTs: Long) = log("timer_complete") {
        putInt("target_days", targetDays)
        putInt("actual_days", actualDays)
        putLong("start_ts", startTs)
        putLong("end_ts", endTs)
        putString("success_type", "full")
    }

    fun timerFail(targetDays: Int, actualDays: Int, reason: String, startTs: Long, endTs: Long) = log("timer_fail") {
        putInt("target_days", targetDays)
        putInt("actual_days", actualDays)
        putString("fail_reason", reason)
        putLong("start_ts", startTs)
        putLong("end_ts", endTs)
    }

    fun viewRecords() = log("view_records") {}
    fun viewRecordDetail(recordId: String) = log("view_record_detail") { putString("record_id", recordId) }
    fun deleteRecords(deleteCount: Int) = log("delete_records") { putInt("delete_count", deleteCount) }

    fun changeIndicator(indicatorType: String) = log("change_indicator") { putString("indicator_type", indicatorType) }
    fun changeSettings(settingType: String) = log("change_settings") { putString("setting_type", settingType) }

    // 광고 관련 보조 이벤트 (AdManager에서도 PaidEventListener를 통해 수익 이벤트를 기록하고 있음)
    fun logAdRequest(adType: String) = log("ad_request") { putString("ad_type", adType) }
    fun logAdImpression(adType: String) = log("ad_impression") { putString("ad_type", adType) }
    fun logAdClick(adType: String) = log("ad_click") { putString("ad_type", adType) }
    fun logAdRevenue(adType: String, value: Double, currency: String) = log("ad_revenue") {
        putString("ad_type", adType)
        putDouble("value", value)
        putString("currency", currency)
    }
}
