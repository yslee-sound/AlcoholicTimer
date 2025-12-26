package kr.sweetapps.alcoholictimer.util.utils

import android.content.Context
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager
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
    // [MODIFIED] 시간 부분 소수점 없이 정수로 표시 (2025-12-24)
    // [MODIFIED] 한국어 로케일 공백 제거 (2025-12-26)
    @JvmStatic
    fun daysToDayHourString(context: Context, days: Double, decimals: Int = 2): String {
        val safeDays = if (days.isNaN() || days.isInfinite()) 0.0 else days.coerceAtLeast(0.0)
        var dayInt = floor(safeDays).toInt()
        val frac = safeDays - dayInt
        val hoursRaw = frac * 24.0
        // [MODIFIED] 반올림하여 정수로 표시
        var hoursRounded = round(hoursRaw)
        if (hoursRounded >= 24.0) {
            dayInt += 1
            hoursRounded = 0.0
        }
        // Format numeric part with requested decimals and append localized unit strings
        val hourUnit = context.getString(R.string.unit_hour)
        val dayUnit = context.getString(R.string.unit_day)
        // [MODIFIED] 소수점 제거
        val formattedHours = String.Companion.format(Locale.getDefault(), "%.0f", hoursRounded)

        // [NEW] 한국어 로케일 체크 (2025-12-26)
        val isKorean = Locale.getDefault().language == "ko"

        return if (dayInt == 0) {
            // 한국어: "1시간" / 그 외: "1 시간"
            if (isKorean) "$formattedHours$hourUnit" else "$formattedHours $hourUnit"
        } else {
            // 한국어: "1일 8시간" (일/시간 사이는 띄움) / 그 외: "1 일 8 시간"
            if (isKorean) {
                "$dayInt$dayUnit $formattedHours$hourUnit"
            } else {
                "$dayInt $dayUnit $formattedHours $hourUnit"
            }
        }
    }

    /**
     * 금액을 로케일에 따라 포맷팅
     * 사용자가 선택한 통화로 표시합니다.
     *
     * [UPDATED] 인도네시아 로케일 자동 감지 및 축약형 포맷 적용 (2025-12-26)
     * - 인도네시아 로케일(ID/in)인 경우: formatCompactRupiah() 사용 (예: Rp1,5jt)
     * - 그 외 국가: 기존 CurrencyManager 사용 (예: ¥10,000)
     *
     * @param context Context
     * @param amountInWon 원화 기준 금액
     * @return 포맷팅된 통화 문자열
     */
    @JvmStatic
    fun formatMoney(context: Context, amountInWon: Double): String {
        val locale = Locale.getDefault()
        // 인도네시아 로케일 감지 (국가 코드 ID 또는 언어 코드 in)
        if (locale.country.equals("ID", ignoreCase = true) || locale.language.equals("in", ignoreCase = true)) {
            return formatCompactRupiah(amountInWon)
        }
        // 그 외 국가는 기존 방식 유지
        return CurrencyManager.formatMoney(amountInWon, context)
    }

    /**
     * Format hours with adaptive decimals: if value < 1 -> 2 decimals, else -> 1 decimal.
     * Returns numeric string only (no unit).
     * [MODIFIED] 소수점 없이 정수로 표시 (2025-12-24)
     */
    @JvmStatic
    fun formatHoursValue(hours: Double, locale: Locale = Locale.getDefault()): String {
        val safe = if (hours.isNaN() || hours.isInfinite()) 0.0 else hours.coerceAtLeast(0.0)
        // [MODIFIED] 소수점 제거 - 반올림하여 정수로 표시
        return String.format(locale, "%.0f", round(safe))
    }

    /**
     * Format hours and append localized unit (e.g., "시간"). Uses R.string.unit_hour for unit.
     * [MODIFIED] 한국어 로케일 공백 제거 (2025-12-26)
     */
    @JvmStatic
    fun formatHoursWithUnit(context: Context, hours: Double): String {
        val num = formatHoursValue(hours, Locale.getDefault())
        val isKorean = Locale.getDefault().language == "ko"
        // 한국어: "8시간" / 그 외: "8 시간"
        return if (isKorean) "$num${context.getString(R.string.unit_hour)}" else "$num ${context.getString(R.string.unit_hour)}"
    }

    /**
     * Format hours and append localized unit with fixed decimals
     * [MODIFIED] 소수점 없이 정수로 표시 (2025-12-24)
     * [MODIFIED] 한국어 로케일 공백 제거 (2025-12-26)
     */
    @JvmStatic
    fun formatHoursWithUnitFixed(context: Context, hours: Double, decimals: Int = 1): String {
        val safe = if (hours.isNaN() || hours.isInfinite()) 0.0 else hours.coerceAtLeast(0.0)
        // [MODIFIED] 소수점 제거 - 반올림하여 정수로 표시
        val rounded = round(safe)
        val isKorean = Locale.getDefault().language == "ko"
        // 한국어: "429시간" / 그 외: "429 시간"
        val format = if (isKorean) "%.0f%s" else "%.0f %s"
        return String.Companion.format(
            Locale.getDefault(), format, rounded, context.getString(R.string.unit_hour))
    }

    @JvmStatic
    fun daysToDayHourStringFixed(context: Context, days: Double, decimals: Int = 1): String {
        return daysToDayHourString(context, days, decimals)
    }

    /**
     * 일수를 소수점 1자리로 포맷하고 단위를 붙여서 반환
     *
     * 숫자와 단위 사이에 공백이 자동으로 포함됩니다.
     *
     * @param context Context
     * @param days 일수
     * @return 포맷된 문자열 (예: "42.7 Hari", "3.5 Days", "100.1일")
     *
     * [NEW] 일수 포맷팅 중앙화 (2025-12-26)
     * [MODIFIED] 한국어 로케일 공백 제거 (2025-12-26)
     */
    @JvmStatic
    fun formatDaysWithUnit(context: Context, days: Double): String {
        val safe = if (days.isNaN() || days.isInfinite()) 0.0 else days.coerceAtLeast(0.0)
        val isKorean = Locale.getDefault().language == "ko"
        // 한국어: "100.1일" / 그 외: "100.1 Days"
        val format = if (isKorean) "%.1f%s" else "%.1f %s"
        return String.format(
            Locale.getDefault(),
            format,
            safe,
            context.getString(R.string.unit_day)
        )
    }

    /**
     * 인도네시아 루피아(IDR) 축약 포맷터
     *
     * 큰 금액을 간결하게 표시하기 위한 포맷터입니다.
     *
     * **예시:**
     * - 494035 -> "Rp494rb" (rb = ribu = 천)
     * - 1500000 -> "Rp1,5jt" (jt = juta = 백만)
     * - 2340000000 -> "Rp2,3M" (M = miliar = 십억)
     *
     * @param amount 루피아 금액
     * @return 축약된 루피아 문자열
     *
     * [NEW] 인도네시아 로케일 전용 축약 포맷 (2025-12-26)
     */
    @JvmStatic
    fun formatCompactRupiah(amount: Double): String {
        // 1. 음수 처리
        if (amount < 0) return "-" + formatCompactRupiah(-amount)

        // 2. 단위 기준 설정
        val thousand = 1000.0
        val million = 1000000.0
        val billion = 1000000000.0

        // [FIX] deprecated Locale 생성자 대신 forLanguageTag 사용
        val indonesiaLocale = Locale.forLanguageTag("in-ID")

        // 3. 포맷팅 (소수점 1자리까지, 끝에 0이면 제거)
        // [UPDATED] 숫자와 단위 사이 공백 추가 (2025-12-26)
        return when {
            amount >= billion -> {
                val value = amount / billion
                String.format(indonesiaLocale, "Rp%.1f M", value).replace(",0 M", " M")
            }
            amount >= million -> {
                val value = amount / million
                String.format(indonesiaLocale, "Rp%.1f jt", value).replace(",0 jt", " jt")
            }
            amount >= thousand -> {
                val value = amount / thousand
                // 천 단위는 보통 소수점을 잘 안 씁니다 (선택 사항)
                String.format(indonesiaLocale, "Rp%.0f rb", value)
            }
            else -> {
                // 작은 숫자는 그대로 표시 (천 단위 구분 기호 포함)
                String.format(indonesiaLocale, "Rp%,.0f", amount)
            }
        }
    }

    /**
     * 큰 숫자(칼로리 등)를 간결하게 표시하기 위한 범용 포맷터
     *
     * 레이아웃이 깨지는 것을 방지하기 위해 100만 이상은 축약 표시합니다.
     *
     * **인도네시아 로케일:**
     * - 1,000,000 미만: "11.521" (천 단위 구분)
     * - 1,000,000 이상: "1,2jt" (백만), "2,3M" (십억)
     *
     * **기타 로케일:**
     * - 천 단위 구분 기호만 적용: "11,521"
     *
     * @param context Context
     * @param amount 숫자 값
     * @return 포맷된 문자열
     *
     * [NEW] 전역 숫자 축약 포맷터 (2025-12-26)
     */
    @JvmStatic
    fun formatCompactNumber(context: Context, amount: Double): String {
        // 음수 처리
        if (amount < 0) return "-" + formatCompactNumber(context, -amount)

        val locale = Locale.getDefault()
        val isIndonesia = locale.country.equals("ID", ignoreCase = true) ||
                         locale.language.equals("in", ignoreCase = true)

        if (isIndonesia) {
            val million = 1000000.0
            val billion = 1000000000.0
            val indonesiaLocale = Locale.forLanguageTag("in-ID")

            // [UPDATED] 숫자와 단위 사이 공백 추가 (2025-12-26)
            return when {
                amount >= billion -> {
                    // 십억 단위: "2,3 M"
                    val value = amount / billion
                    String.format(indonesiaLocale, "%.1f M", value).replace(",0 M", " M")
                }
                amount >= million -> {
                    // 백만 단위: "1,2 jt"
                    val value = amount / million
                    String.format(indonesiaLocale, "%.1f jt", value).replace(",0 jt", " jt")
                }
                else -> {
                    // 100만 미만: "11.521" (천 단위 구분 기호)
                    String.format(indonesiaLocale, "%,.0f", amount)
                }
            }
        } else {
            // 기타 로케일: 천 단위 구분 기호만 적용
            return String.format(locale, "%,.0f", amount)
        }
    }
}