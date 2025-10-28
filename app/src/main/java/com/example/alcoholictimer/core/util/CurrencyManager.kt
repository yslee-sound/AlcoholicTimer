package com.sweetapps.alcoholictimer.core.util

import android.content.Context
import com.sweetapps.alcoholictimer.R
import java.util.Locale

/**
 * 통화 관리 및 포맷팅 유틸리티
 *
 * 사용자가 선택한 통화로 금액을 변환하고 포맷팅합니다.
 * 모든 금액은 내부적으로 KRW로 저장되며, 표시 시에만 변환됩니다.
 */
object CurrencyManager {

    /**
     * 지원하는 통화 목록
     * Phase 1-3에서 지원하는 7개 통화
     * 모든 통화 소수점 2자리 표시
     */
    val supportedCurrencies = listOf(
        CurrencyOption("KRW", "₩", R.string.currency_krw, 1.0, 2),
        CurrencyOption("JPY", "¥", R.string.currency_jpy, 0.1, 2),
        CurrencyOption("USD", "$", R.string.currency_usd, 1300.0, 2),
        CurrencyOption("EUR", "€", R.string.currency_eur, 1400.0, 2),
        CurrencyOption("MXN", "MX$", R.string.currency_mxn, 75.0, 2),
        CurrencyOption("CNY", "¥", R.string.currency_cny, 180.0, 2),
        CurrencyOption("BRL", "R$", R.string.currency_brl, 250.0, 2)
    )

    /**
     * 금액을 사용자 선택 통화로 포맷팅
     * 모든 통화는 소수점 2자리로 표시
     *
     * @param amountInKRW 원화 기준 금액
     * @param context Context
     * @return 포맷팅된 문자열 (예: "¥1,000.00", "$10.00")
     */
    fun formatMoney(amountInKRW: Double, context: Context): String {
        val currency = getSelectedCurrency(context)
        val converted = amountInKRW / currency.rate

        return when (currency.code) {
            "KRW" -> {
                // 한국어: 숫자 + 원 (예: 10,000.00원)
                String.format(Locale.getDefault(), "%,.2f%s", converted, currency.symbol)
            }
            else -> {
                // 모든 통화: 기호 + 소수점 2자리 (예: $10.00, €8.50, ¥1,000.00)
                String.format(Locale.getDefault(), "%s%,.2f", currency.symbol, converted)
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

        // 저장된 설정이 없으면 자동 감지
        if (currencyCode == null) {
            return getDefaultCurrency(context).also {
                saveCurrency(context, it.code)
            }
        }

        return supportedCurrencies.find { it.code == currencyCode }
            ?: supportedCurrencies.first()
    }

    /**
     * 로케일 기반 기본 통화 자동 감지
     *
     * @param context Context
     * @return 감지된 CurrencyOption
     */
    private fun getDefaultCurrency(context: Context): CurrencyOption {
        val locale = Locale.getDefault()
        val countryCode = locale.country
        val languageCode = locale.language

        // 1순위: 국가 코드 기반
        val currencyCode = when (countryCode) {
            "KR" -> "KRW"
            "JP" -> "JPY"
            "US" -> "USD"
            "CN" -> "CNY"
            "MX" -> "MXN"
            "BR" -> "BRL"
            else -> {
                // 2순위: 언어 코드 기반
                when (languageCode) {
                    "ko" -> "KRW"
                    "ja" -> "JPY"
                    "zh" -> "CNY"
                    "es" -> "EUR"  // 스페인어권 기본 = 유로
                    "pt" -> "BRL"  // 포르투갈어권 기본 = 헤알
                    "de", "fr" -> "EUR"
                    else -> "USD"  // 최종 폴백 = 달러
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
            val defaultCurrency = getDefaultCurrency(context)
            saveCurrency(context, defaultCurrency.code)
        }
    }
}

/**
 * 통화 옵션 데이터 클래스
 */
data class CurrencyOption(
    val code: String,           // ISO 4217 코드
    val symbol: String,         // 통화 기호
    val nameResId: Int,         // 이름 리소스 ID
    val rate: Double,           // KRW 기준 환율
    val decimalPlaces: Int = 0  // 소수점 자리수
)

