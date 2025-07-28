package com.example.alcoholictimer.utils

object Constants {
    // 테스트 모드 설정 (true: 분 단위, false: 일 단위)
    const val TEST_MODE = true

    // 프로그레스바 초 단위 테스트 모드
    const val PROGRESS_TEST_MODE = true

    // 시간 변환 상수
    const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24  // 일 단위 (1일 = 24시간)
    const val MINUTE_IN_MILLIS = 1000L * 60         // 분 단위 (1분 = 60초)
    const val SECOND_IN_MILLIS = 1000L              // 초 단위

    // 사용할 시간 단위
    val TIME_UNIT_MILLIS = if (TEST_MODE) MINUTE_IN_MILLIS else DAY_IN_MILLIS

    // 프로그레스바용 시간 단위 (초 단위 테스트를 위함)
    val PROGRESS_TIME_UNIT_MILLIS = if (PROGRESS_TEST_MODE) SECOND_IN_MILLIS else TIME_UNIT_MILLIS

    // 단위 텍스트
    val TIME_UNIT_TEXT = if (TEST_MODE) "분" else "일"
    val PROGRESS_TIME_UNIT_TEXT = if (PROGRESS_TEST_MODE) "초" else TIME_UNIT_TEXT
}
