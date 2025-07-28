package com.example.alcoholictimer.utils

object Constants {
    // 테스트 모드 설정 (true: 초 단위, false: 일 단위)
    const val TEST_MODE = true

    // 초 단위 테스트 모드 (1초 = 1일 개념)
    const val SECOND_TEST_MODE = true

    // 프로그레스바 초 단위 테스트 모드
    const val PROGRESS_TEST_MODE = true

    // 시간 변환 상수
    const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24  // 일 단위 (1일 = 24시간)
    const val MINUTE_IN_MILLIS = 1000L * 60         // 분 단위 (1분 = 60초)
    const val SECOND_IN_MILLIS = 1000L              // 초 단위

    // 사용할 시간 단위
    val TIME_UNIT_MILLIS = when {
        SECOND_TEST_MODE -> SECOND_IN_MILLIS  // 초 단위 테스트 (1초 = 1일)
        TEST_MODE -> MINUTE_IN_MILLIS         // 분 단위 테스트 (1분 = 1일)
        else -> DAY_IN_MILLIS                 // 실제 일 단위 (1일 = 24시간)
    }

    // 프로그레스바용 시간 단위
    val PROGRESS_TIME_UNIT_MILLIS = if (PROGRESS_TEST_MODE) SECOND_IN_MILLIS else TIME_UNIT_MILLIS

    // 단위 텍스트
    val TIME_UNIT_TEXT = when {
        SECOND_TEST_MODE -> "초"  // 초 단위 테스트 시 표시 텍스트
        TEST_MODE -> "분"        // 분 단위 테스트 시 표시 텍스트
        else -> "일"            // 실제 운영 시 표시 텍스트
    }

    val PROGRESS_TIME_UNIT_TEXT = if (PROGRESS_TEST_MODE) "초" else TIME_UNIT_TEXT
}
