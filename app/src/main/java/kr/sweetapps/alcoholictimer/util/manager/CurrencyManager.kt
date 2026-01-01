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
     * 통화별로 정의된 decimalPlaces를 사용합니다.
     *
     * [UPDATED] 인도네시아 로케일 자동 감지 및 축약형 포맷 적용 (2025-12-26)
     * - 인도네시아 로케일(ID/in)인 경우: FormatUtils.formatCompactRupiah() 사용
     * - 그 외 국가: 기존 방식 유지
     *
     * @param amountInKRW 원화 기준 금액
     * @param context Context
     * @return 포맷팅된 문자열 (예: "¥1,000.00", "$10.00", "Rp1,5jt")
     */
    fun formatMoney(amountInKRW: Double, context: Context): String {
        // [NEW] 인도네시아 로케일 감지 및 축약형 포맷 사용 (2025-12-26)
        val locale = Locale.getDefault()
        if (locale.country.equals("ID", ignoreCase = true) || locale.language.equals("in", ignoreCase = true)) {
            return kr.sweetapps.alcoholictimer.util.utils.FormatUtils.formatCompactRupiah(amountInKRW)
        }

        val currency = getSelectedCurrency(context)
        val converted = amountInKRW / currency.rate
        val decimals = currency.decimalPlaces

        return when (currency.code) {
            "KRW" -> {
                // 한국어: 숫자 + 원 기호(뒤에 붙음)
                String.format(Locale.getDefault(), "%,.${decimals}f%s", converted, currency.symbol)
            }
            else -> {
                // 모든 통화: 기호 + 소수점(decimals)
                String.format(Locale.getDefault(), "%s%,.${decimals}f", currency.symbol, converted)
            }
        }
    }

    /**
     * 금액을 소수점 없이 포맷합니다(예: 2206원 또는 $2,206).
     * 화면 요구사항으로 소수점 표기가 없어야 할 때 사용합니다.
     *
     * [UPDATED] 인도네시아 로케일 자동 감지 및 축약형 포맷 적용 (2025-12-26)
     * - 인도네시아 로케일(ID/in)인 경우: FormatUtils.formatCompactRupiah() 사용
     * - 그 외 국가: 기존 방식 유지
     */
    fun formatMoneyNoDecimals(amountInKRW: Double, context: Context): String {
        // [NEW] 인도네시아 로케일 감지 및 축약형 포맷 사용 (2025-12-26)
        val locale = Locale.getDefault()
        if (locale.country.equals("ID", ignoreCase = true) || locale.language.equals("in", ignoreCase = true)) {
            // FormatUtils.formatCompactRupiah를 사용하려면 import 필요
            return kr.sweetapps.alcoholictimer.util.utils.FormatUtils.formatCompactRupiah(amountInKRW)
        }

        val currency = getSelectedCurrency(context)
        val converted = amountInKRW / currency.rate

        return when (currency.code) {
            "KRW" -> {
                // 소수점 없이 표시하고 기호 추가
                String.format(Locale.getDefault(), "%,.0f%s", converted, currency.symbol)
            }
            else -> {
                // 다른 통화: 기호 + 소수점 없이
                String.format(Locale.getDefault(), "%s%,.0f", currency.symbol, converted)
            }
        }
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

        if (!explicit) {
            return getDefaultCurrency()
        }

        if (currencyCode == null || currencyCode == AUTO_CURRENCY_CODE) {
            return getDefaultCurrency()
        }

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

        return supportedCurrencies.find { it.code == currencyCode }
            ?: supportedCurrencies.first()
    }

    /**
     * 통화 설정 저장
     *
     * @param context Context
     * @param currencyCode 통화 코드 (예: "USD")
     */
    fun saveCurrency(context: Context, currencyCode: String) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("currency", currencyCode)
            .putBoolean("currency_explicit", true)
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
            saveCurrency(context, AUTO_CURRENCY_CODE)
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("currency_explicit", false)
                .apply()
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
