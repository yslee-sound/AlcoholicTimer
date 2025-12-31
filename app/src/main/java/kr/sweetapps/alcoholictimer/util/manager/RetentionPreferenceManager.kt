package kr.sweetapps.alcoholictimer.util.manager

import android.content.Context
import android.content.SharedPreferences

/**
 * 리텐션 시스템 데이터 관리 클래스
 *
 * 알림 로직 판단을 위한 SharedPreferences 관리
 *
 * @since 2025-12-31
 */
object RetentionPreferenceManager {

    private const val PREFS_NAME = "retention_prefs"

    // 키 상수 정의
    private const val KEY_TIMER_STATE = "timer_state"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_LAST_END_TIME = "last_end_time"
    private const val KEY_RETRY_COUNT = "retry_count"
    private const val KEY_NOTIFICATION_PERMISSION_SHOWN = "notification_permission_shown"

    /**
     * SharedPreferences 인스턴스 획득
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ==================== 타이머 상태 관리 ====================

    /**
     * 현재 타이머 실행 여부 저장
     * @param isRunning true: 타이머 실행 중, false: 정지
     */
    fun setTimerState(context: Context, isRunning: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TIMER_STATE, isRunning).apply()
    }

    /**
     * 현재 타이머 실행 여부 조회
     * @return true: 타이머 실행 중, false: 정지
     */
    fun isTimerRunning(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TIMER_STATE, false)
    }

    // ==================== 타이머 시간 관리 ====================

    /**
     * 타이머 시작 시각 저장
     * @param startTimeMillis 타이머 시작 시각 (UTC milliseconds)
     */
    fun setStartTime(context: Context, startTimeMillis: Long) {
        getPrefs(context).edit().putLong(KEY_START_TIME, startTimeMillis).apply()
    }

    /**
     * 타이머 시작 시각 조회
     * @return 타이머 시작 시각 (UTC milliseconds), 미설정 시 0
     */
    fun getStartTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_START_TIME, 0L)
    }

    /**
     * 최근 타이머 종료/포기 시각 저장
     * @param endTimeMillis 종료 시각 (UTC milliseconds)
     */
    fun setLastEndTime(context: Context, endTimeMillis: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_END_TIME, endTimeMillis).apply()
    }

    /**
     * 최근 타이머 종료/포기 시각 조회
     * @return 최근 종료 시각 (UTC milliseconds), 미설정 시 0
     */
    fun getLastEndTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_END_TIME, 0L)
    }

    // ==================== 재시도 카운트 관리 ====================

    /**
     * 신규 유저 대상 알림 발송 횟수 저장
     * @param count 발송 횟수 (0~3)
     */
    fun setRetryCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_RETRY_COUNT, count).apply()
    }

    /**
     * 신규 유저 대상 알림 발송 횟수 조회
     * @return 발송 횟수 (0~3)
     */
    fun getRetryCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_RETRY_COUNT, 0)
    }

    /**
     * 재시도 카운트 증가
     * @return 증가 후 카운트 값
     */
    fun incrementRetryCount(context: Context): Int {
        val current = getRetryCount(context)
        val newCount = (current + 1).coerceIn(0, 3)
        setRetryCount(context, newCount)
        return newCount
    }

    /**
     * 재시도 카운트 초기화
     */
    fun resetRetryCount(context: Context) {
        setRetryCount(context, 0)
    }

    // ==================== 권한 요청 상태 관리 ====================

    /**
     * 알림 권한 Pre-Permission 다이얼로그 표시 여부 저장
     * @param shown true: 이미 표시함, false: 미표시
     */
    fun setNotificationPermissionShown(context: Context, shown: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATION_PERMISSION_SHOWN, shown).apply()
    }

    /**
     * 알림 권한 Pre-Permission 다이얼로그 표시 여부 조회
     * @return true: 이미 표시함, false: 미표시
     */
    fun isNotificationPermissionShown(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATION_PERMISSION_SHOWN, false)
    }

    // ==================== 전체 데이터 초기화 ====================

    /**
     * 모든 리텐션 관련 데이터 초기화
     * (디버깅 또는 앱 재설정 시 사용)
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // ==================== 디버그용 메서드 ====================

    /**
     * 현재 저장된 모든 값 조회 (디버깅용)
     * @return 현재 상태 정보 문자열
     */
    fun getDebugInfo(context: Context): String {
        return buildString {
            appendLine("=== Retention Preferences ===")
            appendLine("Timer Running: ${isTimerRunning(context)}")
            appendLine("Start Time: ${getStartTime(context)}")
            appendLine("Last End Time: ${getLastEndTime(context)}")
            appendLine("Retry Count: ${getRetryCount(context)}")
            appendLine("Permission Shown: ${isNotificationPermissionShown(context)}")
        }
    }
}

