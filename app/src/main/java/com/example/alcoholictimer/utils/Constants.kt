package com.example.alcoholictimer.utils

import android.content.Context

object Constants {
    // SharedPreferences 관련 상수
    const val PREFS_NAME = "AlcoholicTimerPrefs"
    const val PREF_KEY_TEST_MODE = "test_mode"
    const val PREF_TEST_MODE = "test_mode"  // 호환성을 위해 추가
    const val PREF_START_TIME = "start_time"
    const val PREF_TARGET_DAYS = "target_days"
    const val PREF_RECORDS = "records"

    // 테스트 모드 상수
    const val TEST_MODE_REAL = 0    // 실제 모드 (1일 = 24시간)
    const val TEST_MODE_MINUTE = 1  // 분 단위 테스트 모드 (1일 = 1분)
    const val TEST_MODE_SECOND = 2  // 초 단위 테스트 모드 (1일 = 1초)

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

    // UI 관련 상수
    const val RESULT_SCREEN_DELAY = 2000             // 결과 화면 전환 지연 시간 (2초)

    // 기타 상수
    const val DEFAULT_VALUE = 2000                   // 기본값 2000

    // 사용할 시간 단위 (테스트 모드에 따라 동적으로 결정)
    val TIME_UNIT_MILLIS: Long
        get() = when (currentTestMode) {
            TEST_MODE_SECOND -> SECOND_IN_MILLIS  // 초 단위 테스트 (1초 = 1일)
            TEST_MODE_MINUTE -> MINUTE_IN_MILLIS  // 분 단위 테스트 (1분 = 1일)
            else -> DAY_IN_MILLIS                 // 실제 일 단위 (1일 = 24시간)
        }

    // 시간 단위 텍스트 (테스트 모드에 따라 동적으로 결정)
    val TIME_UNIT_TEXT: String
        get() = when (currentTestMode) {
            TEST_MODE_SECOND -> "초"
            TEST_MODE_MINUTE -> "분"
            else -> "일"
        }

    // 앱 시작 시 설정 불러오기
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentTestMode = prefs.getInt(PREF_KEY_TEST_MODE, TEST_MODE_REAL)
    }

    /**
     * 테스트 모드를 업데이트하는 함수
     * TestActivity에서 설정 저장 시 호출됩니다.
     */
    fun updateTestMode(mode: Int) {
        currentTestMode = when (mode) {
            TEST_MODE_SECOND, TEST_MODE_MINUTE, TEST_MODE_REAL -> mode
            else -> TEST_MODE_REAL // 잘못된 값이면 기본값으로 설정
        }
    }
}
