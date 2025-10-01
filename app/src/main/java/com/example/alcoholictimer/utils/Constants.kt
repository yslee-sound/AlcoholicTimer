package com.example.alcoholictimer.utils

import android.content.Context
import androidx.core.content.edit

object Constants {
    // SharedPreferences 관련 상수
    const val PREFS_NAME = "AlcoholicTimerPrefs"
    const val USER_SETTINGS_PREFS = "user_settings"  // 사용자 설정용 SharedPreferences
    const val PREF_KEY_TEST_MODE = "test_mode"
    const val PREF_TEST_MODE = "test_mode"  // 호환성 유지
    const val PREF_START_TIME = "start_time"
    const val PREF_TARGET_DAYS = "target_days"
    const val PREF_RECORDS = "records"

    // 타이머 완료 여부 및 기록 리스트 키
    const val PREF_TIMER_COMPLETED = "timer_completed"
    const val PREF_SOBRIETY_RECORDS = "sobriety_records"

    // 사용자 설정 키
    const val PREF_SELECTED_COST = "selected_cost"
    const val PREF_SELECTED_FREQUENCY = "selected_frequency"
    const val PREF_SELECTED_DURATION = "selected_duration"
    const val PREF_SETTINGS_INITIALIZED = "settings_initialized"

    // 설정 기본값
    const val DEFAULT_COST = "저"
    const val DEFAULT_FREQUENCY = "주 1회 이하"
    const val DEFAULT_DURATION = "짧음"

    // 테스트 모드 상수 (레벨 계산용)
    const val TEST_MODE_REAL = 0    // 실제 모드 (레벨 계산: 1일 = 24시간)
    const val TEST_MODE_MINUTE = 1  // 분 단위 테스트 모드 (레벨 계산: 1분 = 1일)
    const val TEST_MODE_SECOND = 2  // 초 단위 테스트 모드 (레벨 계산: 1초 = 1일)

    // 현재 선택된 테스트 모드 (기본값: 실제 모드)
    var currentTestMode = TEST_MODE_REAL

    // 테스트 모드 상태 (런타임에 변경 가능)
    val isTestMode: Boolean
        get() = currentTestMode != TEST_MODE_REAL

    val isSecondTestMode: Boolean
        get() = currentTestMode == TEST_MODE_SECOND

    val isMinuteTestMode: Boolean
        get() = currentTestMode == TEST_MODE_MINUTE

    // 시간 변환 상수
    const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24  // 일 단위 (1일 = 24시간)
    const val MINUTE_IN_MILLIS = 1000L * 60         // 분 단위 (1분 = 60초)
    const val SECOND_IN_MILLIS = 1000L              // 초 단위

    // 기타 상수
    const val RESULT_SCREEN_DELAY = 2000            // 결과 화면 전환 지연 시간 (2초)
    const val DEFAULT_VALUE = 2000                  // 기본값 2000
    const val DEFAULT_HANGOVER_HOURS = 5            // 기본 숙취 시간(시간)

    // 레벨 계산용 시간 단위 (테스트 모드에 따라 동적으로 결정)
    val LEVEL_TIME_UNIT_MILLIS: Long
        get() = when (currentTestMode) {
            TEST_MODE_SECOND -> SECOND_IN_MILLIS  // 초 단위 테스트 (1초 = 1일)
            TEST_MODE_MINUTE -> MINUTE_IN_MILLIS  // 분 단위 테스트 (1분 = 1일)
            else -> DAY_IN_MILLIS                 // 실제 일 단위 (1일 = 24시간)
        }

    // 레벨 계산용 시간 단위 텍스트 (테스트 모드에 따라 동적으로 결정)
    val LEVEL_TIME_UNIT_TEXT: String
        get() = when (currentTestMode) {
            TEST_MODE_SECOND -> "초"
            TEST_MODE_MINUTE -> "분"
            else -> "일"
        }

    // 상태 문자열
    const val STATUS_COMPLETED = "완료"

    // current_indicator 키 생성 함수 (세션별 분리)
    fun keyCurrentIndicator(startTime: Long): String = "current_indicator_${startTime}"

    /**
     * 레벨 계산용 일수 계산 함수
     * 테스트 모드에 따라 다른 시간 단위로 계산
     */
    fun calculateLevelDays(elapsedTimeMillis: Long): Int {
        return (elapsedTimeMillis / LEVEL_TIME_UNIT_MILLIS).toInt()
    }

    /**
     * 레벨 계산용 일수 계산 함수 (Float)
     * 테스트 모드에 따라 다른 시간 단위로 계산
     */
    fun calculateLevelDaysFloat(elapsedTimeMillis: Long): Float {
        return (elapsedTimeMillis / LEVEL_TIME_UNIT_MILLIS.toFloat())
    }

    // 앱 시작 시 설정 불러오기
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentTestMode = prefs.getInt(PREF_KEY_TEST_MODE, TEST_MODE_REAL)
    }

    /**
     * 테스트 모드를 업데이트하는 함수 (TestActivity 등에서 호출)
     */
    fun updateTestMode(mode: Int) {
        currentTestMode = when (mode) {
            TEST_MODE_SECOND, TEST_MODE_MINUTE, TEST_MODE_REAL -> mode
            else -> TEST_MODE_REAL // 잘못된 값이면 기본값으로 설정
        }
    }

    /**
     * 사용자 설정값을 초기화하는 함수 (앱 최초 실행 시 1회)
     */
    fun initializeUserSettings(context: Context) {
        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val isInitialized = sharedPref.getBoolean(PREF_SETTINGS_INITIALIZED, false)

        if (!isInitialized) {
            sharedPref.edit {
                putString(PREF_SELECTED_COST, DEFAULT_COST)
                putString(PREF_SELECTED_FREQUENCY, DEFAULT_FREQUENCY)
                putString(PREF_SELECTED_DURATION, DEFAULT_DURATION)
                putBoolean(PREF_SETTINGS_INITIALIZED, true)
            }
        }
    }

    /**
     * 사용자 설정값을 가져오는 함수
     */
    fun getUserSettings(context: Context): Triple<String, String, String> {
        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val cost = sharedPref.getString(PREF_SELECTED_COST, DEFAULT_COST) ?: DEFAULT_COST
        val frequency = sharedPref.getString(PREF_SELECTED_FREQUENCY, DEFAULT_FREQUENCY) ?: DEFAULT_FREQUENCY
        val duration = sharedPref.getString(PREF_SELECTED_DURATION, DEFAULT_DURATION) ?: DEFAULT_DURATION
        return Triple(cost, frequency, duration)
    }
}
