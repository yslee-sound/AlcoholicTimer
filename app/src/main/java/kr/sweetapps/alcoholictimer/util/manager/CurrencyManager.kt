package kr.sweetapps.alcoholictimer.util.manager

import android.content.Context
import kr.sweetapps.alcoholictimer.R
import java.util.Locale

/**
 * 통화 관리 및 포맷팅 유틸리티
 *
 * 사용자가 선택한 통화로 금액을 변환하고 포맷팅합니다.
 * 모든 금액은 내부적으로 KRW로 저장되며, 표시 시에만 변환됩니다.
 */
object CurrencyManager {
    private const val AUTO_CURRENCY_CODE = "AUTO"

    /**
     * 지원하는 통화 목록
     * Phase 1-3에서 지원하는 9개 통화 (PHP 추가)
     */
    val supportedCurrencies = listOf(
        CurrencyOption("KRW", "₩", R.string.currency_krw, 1.0, 2),
        // JPY: 1 JPY = 10 KRW (10원 = 1엔)
        CurrencyOption("JPY", "¥", R.string.currency_jpy, 10.0, 2),
        CurrencyOption("USD", "$", R.string.currency_usd, 1300.0, 2),
        CurrencyOption("EUR", "€", R.string.currency_eur, 1400.0, 2),
        CurrencyOption("MXN", "MX$", R.string.currency_mxn, 75.0, 2),
        CurrencyOption("CNY", "¥", R.string.currency_cny, 180.0, 2),
        CurrencyOption("BRL", "R$", R.string.currency_brl, 250.0, 2),
        // IDR: 1 KRW = 11.8 IDR (2025-12-24 기준)
        CurrencyOption("IDR", "Rp", R.string.currency_idr, 0.085, 2),
        // PHP: 1 PHP = 23 KRW (2026-01-02 기준)
        CurrencyOption("PHP", "₱", R.string.currency_php, 23.0, 2)
    )

    /**
     * 금액을 사용자 선택 통화로 포맷팅
     *
     * [REFACTORED] 조건부 스마트 포맷팅 (2026-01-02)
     * - IDR: 축약형 표시 (1.5jt, 500rb 등, Rp 기호 없음)
     * - 기타 통화: 전체 숫자 표시 (1,000.00 등, 기호 없음)
     * - UI에서 통화 코드를 별도로 표시
     *
     * @param amountInKRW 원화 기준 금액
     * @param context Context
     * @return 포맷팅된 숫자 문자열 (예: "1,000.00", "1.5jt", "10.50")
     */
    fun formatMoney(amountInKRW: Double, context: Context): String {
        val currency = getSelectedCurrency(context)
        val converted = amountInKRW / currency.rate

        // IDR: 축약형 포맷 사용 (Rp 기호 없음)
        if (currency.code == "IDR") {
            return formatCompactIDR(converted)
        }

        // 기타 통화: 전체 숫자 + 천 단위 콤마 + 소수점 (기호 없음)
        val decimals = currency.decimalPlaces
        return String.format(Locale.getDefault(), "%,.${decimals}f", converted)
    }

    /**
     * 금액을 소수점 없이 포맷팅
     *
     * [REFACTORED] 조건부 스마트 포맷팅 (2026-01-02)
     * - IDR: 축약형 표시 (1.5jt, 500rb 등, Rp 기호 없음)
     * - 기타 통화: 정수 표시 (1,000 등, 기호 없음)
     * - UI에서 통화 코드를 별도로 표시
     *
     * @param amountInKRW 원화 기준 금액
     * @param context Context
     * @return 포맷팅된 정수 문자열 (예: "1,000", "1.5jt", "2,206")
     */
    fun formatMoneyNoDecimals(amountInKRW: Double, context: Context): String {
        val currency = getSelectedCurrency(context)
        val converted = amountInKRW / currency.rate

        // IDR: 축약형 포맷 사용 (Rp 기호 없음)
        if (currency.code == "IDR") {
            return formatCompactIDR(converted)
        }

        // 기타 통화: 소수점 없이, 천 단위 콤마만 (기호 없음)
        return String.format(Locale.getDefault(), "%,.0f", converted)
    }

    /**
     * 사용자가 선택한 통화 가져오기
     *
     * @param context Context
     * @return 선택된 CurrencyOption
     */
    fun getSelectedCurrency(context: Context): CurrencyOption {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currencyCode = prefs.getString("currency", null)
        val explicit = prefs.getBoolean("currency_explicit", false)

        // [DEBUG] 설정 상태 로그 (2026-01-02)
        android.util.Log.d("CurrencyManager", "📊 currencyCode: $currencyCode")
        android.util.Log.d("CurrencyManager", "📊 explicit: $explicit")

        if (!explicit) {
            android.util.Log.d("CurrencyManager", "✅ Using system default (explicit=false)")
            return getDefaultCurrency()
        }

        if (currencyCode == null || currencyCode == AUTO_CURRENCY_CODE) {
            android.util.Log.d("CurrencyManager", "✅ Using system default (currencyCode=AUTO or null)")
            return getDefaultCurrency()
        }

        android.util.Log.d("CurrencyManager", "✅ Using explicit currency: $currencyCode")
        return supportedCurrencies.find { it.code == currencyCode }
            ?: getDefaultCurrency()
    }

    /**
     * 로케일 기반 기본 통화 자동 감지
     *
     * @return 감지된 CurrencyOption
     */
    private fun getDefaultCurrency(): CurrencyOption {
        val locale = Locale.getDefault()
        val countryCode = locale.country
        val languageCode = locale.language

        // [DEBUG] 로케일 확인 로그 (2026-01-02)
        android.util.Log.d("CurrencyManager", "🌍 Locale: $locale")
        android.util.Log.d("CurrencyManager", "🌍 Country: $countryCode")
        android.util.Log.d("CurrencyManager", "🌍 Language: $languageCode")

        val currencyCode = when (countryCode) {
            "KR" -> "KRW"
            "JP" -> "JPY"
            "US" -> "USD"
            "CN" -> "CNY"
            "MX" -> "MXN"
            "BR" -> "BRL"
            "ID" -> "IDR"  // 인도네시아
            "PH" -> "PHP"  // 필리핀
            else -> {
                when (languageCode) {
                    "ko" -> "KRW"
                    "ja" -> "JPY"
                    "zh" -> "CNY"
                    "es" -> "EUR"
                    "pt" -> "BRL"
                    "in", "id" -> "IDR"  // 인도네시아어
                    "tl", "fil" -> "PHP"  // 타갈로그어, 필리핀어
                    "de", "fr" -> "EUR"
                    else -> "USD"
                }
            }
        }

        android.util.Log.d("CurrencyManager", "💰 Selected Currency: $currencyCode")

        return supportedCurrencies.find { it.code == currencyCode }
            ?: supportedCurrencies.first()
    }

    /**
     * 통화 설정 저장
     *
     * @param context Context
     * @param currencyCode 통화 코드 (예: "USD", "AUTO")
     * @param explicit 사용자가 명시적으로 선택했는지 여부 (true: 수동 선택, false: 시스템 설정)
     */
    fun saveCurrency(context: Context, currencyCode: String, explicit: Boolean = true) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("currency", currencyCode)
            .putBoolean("currency_explicit", explicit)
            .apply()
    }

    /**
     * 첫 실행 시 기본 통화 초기화
     * MainActivity.onCreate()에서 호출
     *
     * @param context Context
     */
    fun initializeDefaultCurrency(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.contains("currency")) {
            // [FIX] 시스템 설정 모드로 초기화 (explicit: false) (2026-01-02)
            saveCurrency(context, AUTO_CURRENCY_CODE, explicit = false)
        }
    }

    /**
     * 인도네시아 루피아(IDR) 축약형 포맷터
     *
     * 큰 금액을 간결하게 표시하기 위한 포맷터입니다.
     * Rp 기호는 포함하지 않으며, UI에서 통화 코드를 별도로 표시합니다.
     *
     * **예시:**
     * - 1,500,000 → "1.5jt" (jt = juta = 백만)
     * - 500,000 → "500rb" (rb = ribu = 천)
     * - 950 → "950"
     *
     * @param value IDR 금액
     * @return 축약된 문자열 (Rp 기호 없음)
     */
    private fun formatCompactIDR(value: Double): String {
        // 음수 처리
        if (value < 0) return "-" + formatCompactIDR(-value)

        return when {
            // 1,000,000 이상: 백만 단위 (jt = juta)
            value >= 1_000_000 -> {
                val millions = value / 1_000_000
                // 1.5jt, 2.3jt 형태 (소수점 1자리)
                if (millions >= 10) {
                    // 10jt 이상은 소수점 없이
                    String.format(Locale.getDefault(), "%.0fjt", millions)
                } else {
                    // 10jt 미만은 소수점 1자리
                    val formatted = String.format(Locale.getDefault(), "%.1fjt", millions)
                    // .0jt는 jt로 표시 (예: 1.0jt -> 1jt)
                    formatted.replace(".0jt", "jt")
                }
            }
            // 1,000 이상: 천 단위 (rb = ribu)
            value >= 1_000 -> {
                val thousands = value / 1_000
                // 500rb, 1.5rb 형태
                if (thousands >= 100) {
                    // 100rb 이상은 소수점 없이
                    String.format(Locale.getDefault(), "%.0frb", thousands)
                } else if (thousands % 1.0 == 0.0) {
                    // 정수면 소수점 없이
                    String.format(Locale.getDefault(), "%.0frb", thousands)
                } else {
                    // 소수점 1자리
                    String.format(Locale.getDefault(), "%.1frb", thousands)
                }
            }
            // 1,000 미만: 천 단위 콤마만
            else -> {
                String.format(Locale.getDefault(), "%,.0f", value)
            }
        }
    }
}

/**
 * 통화 옵션 데이터 클래스
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameResId: Int,
    val rate: Double,
    val decimalPlaces: Int = 2
)
