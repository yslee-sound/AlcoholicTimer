package kr.sweetapps.alcoholictimer.core.util

import android.content.Context
import kr.sweetapps.alcoholictimer.R
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

object FormatUtils {

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
     * 사용자가 선택한 통화로 표시합니다.
     *
     * @param context Context
     * @param amountInWon 원화 기준 금액
     * @return 포맷팅된 통화 문자열
     */
    @JvmStatic
    fun formatMoney(context: Context, amountInWon: Double): String {
        return CurrencyManager.formatMoney(amountInWon, context)
    }
}

