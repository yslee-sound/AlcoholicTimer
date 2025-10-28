package com.sweetapps.alcoholictimer.core.util

import android.content.Context
import com.sweetapps.alcoholictimer.R
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

object FormatUtils {
    private const val WON_TO_DOLLAR_RATE = 1000.0 // 1,000원 = 1달러
    private const val WON_TO_YEN_RATE = 10.0 // 10원 = 1엔

    @JvmStatic
    fun daysToDayHourString(days: Double, decimals: Int = 2, locale: Locale = Locale.getDefault()): String {
        val safeDays = if (days.isNaN() || days.isInfinite()) 0.0 else days.coerceAtLeast(0.0)
        var dayInt = floor(safeDays).toInt()
        val frac = safeDays - dayInt
        val hoursRaw = frac * 24.0
        val scale = 10.0.pow(decimals)
        var hoursRounded = round(hoursRaw * scale) / scale
        if (hoursRounded >= 24.0) {
            dayInt += 1
            hoursRounded = 0.0
        }
        return if (dayInt == 0) {
            String.format(locale, "%.${decimals}f시간", hoursRounded)
        } else {
            String.format(locale, "%d일 %.${decimals}f시간", dayInt, hoursRounded)
        }
    }

    // Context를 받는 다국어 버전
    @JvmStatic
    fun daysToDayHourString(context: Context, days: Double, decimals: Int = 2): String {
        val safeDays = if (days.isNaN() || days.isInfinite()) 0.0 else days.coerceAtLeast(0.0)
        var dayInt = floor(safeDays).toInt()
        val frac = safeDays - dayInt
        val hoursRaw = frac * 24.0
        val scale = 10.0.pow(decimals)
        var hoursRounded = round(hoursRaw * scale) / scale
        if (hoursRounded >= 24.0) {
            dayInt += 1
            hoursRounded = 0.0
        }
        return if (dayInt == 0) {
            context.getString(R.string.unit_life_hours_only, hoursRounded)
        } else {
            context.getString(R.string.unit_life_days_hours, dayInt, hoursRounded)
        }
    }

    /**
     * 금액을 로케일에 따라 포맷팅
     * 한국어: 원화 (₩1,000원)
     * 영어: 달러 ($1.00)
     * 일본어: 엔화 (¥100)
     */
    @JvmStatic
    fun formatMoney(context: Context, amountInWon: Double): String {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "ko" -> {
                // 한국어: 원화 표시
                context.getString(R.string.unit_won_format, amountInWon)
            }
            "ja" -> {
                // 일본어: 엔화로 변환 (10원 = 1엔)
                val amountInYen = amountInWon / WON_TO_YEN_RATE
                context.getString(R.string.unit_won_format, amountInYen)
            }
            else -> {
                // 영어 등: 달러로 변환 (소수점 2자리)
                val amountInDollars = amountInWon / WON_TO_DOLLAR_RATE
                String.format(locale, "$%,.2f", amountInDollars)
            }
        }
    }

    /**
     * 금액을 로케일에 따라 계산 (내부 계산용)
     * 영어 환경에서는 달러 기준으로, 일본어에서는 엔화 기준으로 계산하기 위해 사용
     */
    @JvmStatic
    fun convertMoneyForDisplay(amountInWon: Double): Double {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "ko" -> amountInWon
            "ja" -> amountInWon / WON_TO_YEN_RATE
            else -> amountInWon / WON_TO_DOLLAR_RATE
        }
    }
}

