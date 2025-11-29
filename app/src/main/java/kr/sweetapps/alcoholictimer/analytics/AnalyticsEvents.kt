package kr.sweetapps.alcoholictimer.analytics

/**
 * 📄 모든 이벤트명, 파라미터명을 상수로 정의하는 파일
 */
object AnalyticsEvents {
    const val AD_REVENUE = "ad_revenue"
    const val TIMER_START = "timer_start"
    const val TIMER_END = "timer_end"
    const val TIMER_FINISH = "timer_finish"
    const val AD_IMPRESSION = "ad_impression"
    const val AD_CLICK = "ad_click"
    const val VIEW_RECORDS = "view_records"
}

object AnalyticsParams {
    const val VALUE = "value"
    const val CURRENCY = "currency"
    const val AD_TYPE = "ad_type"
    const val TARGET_DAYS = "target_days"
    const val ACTUAL_DAYS = "actual_days"
    const val START_TS = "start_ts"
    const val END_TS = "end_ts"
    const val FAIL_REASON = "fail_reason"
    const val HAD_ACTIVE_GOAL = "had_active_goal"
}
