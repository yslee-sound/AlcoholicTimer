package kr.sweetapps.alcoholictimer.analytics

/**
 * 📄 모든 이벤트명, 파라미터명을 상수로 정의하는 파일
 */
object AnalyticsEvents {
    const val AD_REVENUE = "ad_revenue"
    const val TIMER_START = "timer_start"
    const val TIMER_END = "timer_end"
    const val TIMER_FINISH = "timer_finish"
    const val TIMER_GIVE_UP = "timer_give_up" // [NEW] 타이머 포기 (2025-12-31)
    const val SESSION_START = "session_start" // [NEW] 세션 시작 (2025-12-31)
    const val LEVEL_UP = "level_up" // [NEW] 레벨 업 (2025-12-31)
    const val SCREEN_VIEW = "screen_view" // [NEW] 화면 전환 (2025-12-31)
    const val COMMUNITY_POST = "community_post" // [NEW] 커뮤니티 글 작성 (2025-12-31)
    const val SETTINGS_CHANGE = "settings_change" // [NEW] 설정 변경 (2025-12-31)
    const val AD_IMPRESSION = "ad_impression"
    const val AD_CLICK = "ad_click"
    const val VIEW_RECORDS = "view_records"
    const val CHANGE_RECORD_VIEW = "change_record_view"
    const val VIEW_RECORD_DETAIL = "view_record_detail"
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
    const val VIEW_TYPE = "view_type"
    const val CURRENT_LEVEL = "current_level"
    const val RECORD_ID = "record_id"
    // [NEW] timer_give_up용 파라미터 (2025-12-31)
    const val QUIT_REASON = "quit_reason"
    const val QUIT_TS = "quit_ts"
    const val PROGRESS_PERCENT = "progress_percent"
    // [NEW] session_start용 파라미터 (2025-12-31)
    const val IS_FIRST_SESSION = "is_first_session"
    const val DAYS_SINCE_INSTALL = "days_since_install"
    const val TIMER_STATUS = "timer_status"
    // [NEW] level_up용 파라미터 (2025-12-31)
    const val OLD_LEVEL = "old_level"
    const val NEW_LEVEL = "new_level"
    const val TOTAL_DAYS = "total_days"
    const val LEVEL_NAME = "level_name"
    const val ACHIEVEMENT_TS = "achievement_ts"
    // [NEW] screen_view용 파라미터 (2025-12-31)
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val PREVIOUS_SCREEN = "previous_screen"
    // [NEW] community_post용 파라미터 (2025-12-31)
    const val POST_TYPE = "post_type"
    const val HAS_IMAGE = "has_image"
    const val CONTENT_LENGTH = "content_length"
    const val TAG_TYPE = "tag_type"
    const val USER_LEVEL = "user_level"
    const val DAYS = "days"
    // [NEW] settings_change용 파라미터 (2025-12-31)
    const val SETTING_TYPE = "setting_type"
    const val OLD_VALUE = "old_value"
    const val NEW_VALUE = "new_value"
}
