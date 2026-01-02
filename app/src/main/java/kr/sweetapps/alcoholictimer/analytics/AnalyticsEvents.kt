package kr.sweetapps.alcoholictimer.analytics

/**
 * 📄 모든 이벤트명, 파라미터명을 상수로 정의하는 파일
 */
object AnalyticsEvents {
    // [Group A] 수익화 (Money) 💰
    const val AD_REVENUE = "ad_revenue"
    const val AD_IMPRESSION = "ad_impression"
    const val AD_CLICK = "ad_click"

    // [Group B] 핵심 활동 (Core Action) 🔥
    const val TIMER_START = "timer_start"
    const val TIMER_END = "timer_end" // [DEPRECATED] 사용 안 함
    const val TIMER_FINISH = "timer_finish" // [DEPRECATED] 사용 안 함
    const val TIMER_GIVE_UP = "timer_give_up"
    const val DIARY_SAVE = "diary_save" // [NEW] 일기 저장 (2026-01-02)
    const val COMMUNITY_POST = "community_post" // [MODIFIED] 커뮤니티 글만 (일기 제외) (2026-01-02)

    // [Group C] 성장 (Growth) 🌱
    const val LEVEL_UP = "level_up"

    // [Group D] 앱 건강도 (Health) 🏥
    const val SESSION_START = "session_start"
    const val NOTIFICATION_OPEN = "notification_open"
    const val SETTINGS_CHANGE = "settings_change"

    // [REMOVED] 삭제된 이벤트 (2026-01-02)
    // - VIEW_RECORDS: screen_view로 대체 가능
    // - CHANGE_RECORD_VIEW: 너무 세분화, 불필요
    // - VIEW_RECORD_DETAIL: 너무 깊은 뎁스, 데이터 노이즈
}

object AnalyticsParams {
    // Common
    const val VALUE = "value"
    const val CURRENCY = "currency"

    // Ad related
    const val AD_TYPE = "ad_type"

    // Timer related
    const val TARGET_DAYS = "target_days"
    const val ACTUAL_DAYS = "actual_days"
    const val START_TS = "start_ts"
    const val END_TS = "end_ts"
    const val QUIT_REASON = "quit_reason"
    const val QUIT_TS = "quit_ts"
    const val PROGRESS_PERCENT = "progress_percent"
    const val HAD_ACTIVE_GOAL = "had_active_goal"
    const val FAIL_REASON = "fail_reason" // [DEPRECATED]

    // Session related
    const val IS_FIRST_SESSION = "is_first_session"
    const val DAYS_SINCE_INSTALL = "days_since_install"
    const val TIMER_STATUS = "timer_status"

    // Level related
    const val OLD_LEVEL = "old_level"
    const val NEW_LEVEL = "new_level"
    const val TOTAL_DAYS = "total_days"
    const val LEVEL_NAME = "level_name"
    const val ACHIEVEMENT_TS = "achievement_ts"

    // [NEW] Diary related (2026-01-02)
    const val MOOD = "mood" // 기분: "happy", "sad", "soso"
    const val CONTENT_LENGTH = "content_length"
    const val HAS_IMAGE = "has_image"
    const val DAY_COUNT = "day_count" // 금주 며칠차

    // [MODIFIED] Community related (2026-01-02)
    const val POST_TYPE = "post_type" // "challenge" (커뮤니티 전용)
    const val TAG_TYPE = "tag_type"
    const val USER_LEVEL = "user_level"
    const val DAYS = "days"

    // Settings related
    const val SETTING_TYPE = "setting_type"
    const val OLD_VALUE = "old_value"
    const val NEW_VALUE = "new_value"

    // Notification related
    const val NOTIFICATION_ID = "notification_id"
    const val GROUP_TYPE = "group_type"
    const val TARGET_SCREEN = "target_screen"
    const val OPEN_TS = "open_ts"

    // [REMOVED] 삭제된 파라미터 (2026-01-02)
    // - SCREEN_NAME, SCREEN_CLASS, PREVIOUS_SCREEN: screen_view 이벤트 제거로 불필요
    // - VIEW_TYPE, CURRENT_LEVEL, RECORD_ID: 세분화 이벤트 제거로 불필요
    // - SHARE_TARGET, CONTENT_TYPE: 공유 기능 없음 (2026-01-02)
}
