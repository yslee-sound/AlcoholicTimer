package kr.sweetapps.alcoholictimer.util.utils

import android.content.Context
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.util.CurrencyManager
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
        // Format numeric part with requested decimals and append localized unit strings
        val hourUnit = context.getString(R.string.unit_hour)
        val dayUnit = context.getString(R.string.unit_day)
        val formattedHours = String.Companion.format(Locale.getDefault(), "%.${decimals}f", hoursRounded)
        return if (dayInt == 0) {
            // e.g., "1.2시간"
            "$formattedHours$hourUnit"
        } else {
            // e.g., "1일 1.2시간"
            "$dayInt$dayUnit $formattedHours$hourUnit"
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

    /**
     * Format hours with adaptive decimals: if value < 1 -> 2 decimals, else -> 1 decimal.
     * Returns numeric string only (no unit).
     */
    @JvmStatic
    fun formatHoursValue(hours: Double, locale: Locale = Locale.getDefault()): String {
        val safe = if (hours.isNaN() || hours.isInfinite()) 0.0 else hours.coerceAtLeast(0.0)
        val decimals = if (safe < 1.0 && safe > 0.0) 2 else 1
        return String.format(locale, "%.${decimals}f", safe)
    }

    /**
     * Format hours and append localized unit (e.g., "시간"). Uses R.string.unit_hour for unit.
     */
    @JvmStatic
    fun formatHoursWithUnit(context: Context, hours: Double): String {
        val num = formatHoursValue(hours, Locale.getDefault())
        return "$num${context.getString(R.string.unit_hour)}"
    }

    /**
     * Format hours and append localized unit with fixed decimals
     */
    @JvmStatic
    fun formatHoursWithUnitFixed(context: Context, hours: Double, decimals: Int = 1): String {
        val safe = if (hours.isNaN() || hours.isInfinite()) 0.0 else hours.coerceAtLeast(0.0)
        val scale = 10.0.pow(decimals)
        val rounded = round(safe * scale) / scale
        return String.Companion.format(
            Locale.getDefault(), "%.${decimals}f%s", rounded, context.getString(
                R.string.unit_hour))
    }

    @JvmStatic
    fun daysToDayHourStringFixed(context: Context, days: Double, decimals: Int = 1): String {
        return daysToDayHourString(context, days, decimals)
    }
}