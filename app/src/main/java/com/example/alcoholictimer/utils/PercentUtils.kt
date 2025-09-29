package com.example.alcoholictimer.utils

import java.math.BigDecimal
import java.math.RoundingMode

object PercentUtils {
    /**
     * 소수 퍼센트 값을 0자리(정수)로 반올림(HALF_UP)하여 Int로 반환
     * 예) 4.6 -> 5, 4.4 -> 4
     */
    fun roundPercent(percentValue: Double): Int =
        BigDecimal(percentValue).setScale(0, RoundingMode.HALF_UP).toInt()

    /**
     * 0.0~1.0 비율 값을 받아 퍼센트(0~100) 정수로 반올림(HALF_UP)하여 반환
     * 예) 0.046 -> 5
     */
    fun roundPercentFromRatio(ratio: Double): Int = roundPercent(ratio * 100.0)
}
