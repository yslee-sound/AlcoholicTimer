# 통화 현지화 구현 가이드

**작성일**: 2025-10-28  
**버전**: 1.0  
**상태**: Phase 1 준비 완료

---

## 📋 목차
1. [개요](#1-개요)
2. [문제 정의](#2-문제-정의)
3. [해결 방안](#3-해결-방안)
4. [구현 가이드](#4-구현-가이드)
5. [테스트 시나리오](#5-테스트-시나리오)
6. [FAQ](#6-faq)

---

## 1. 개요

### 1.1 배경
AlcoholicTimer는 금주/절주 시 절약한 금액을 표시하는 기능이 있습니다. 다국어 지원을 확장하면서 각 국가/언어권마다 사용하는 통화가 다른 문제가 발생했습니다.

**예시:**
- 🇰🇷 한국어: "10,000원 절약"
- 🇯🇵 일본어: "1,000엔 절약" (1엔 = 10원)
- 🇺🇸 영어: "$10 saved" (1달러 = 1,300원)
- 🇪🇸 스페인어: "€8 ahorrados" (1유로 = 1,400원)

### 1.2 목표
- ✅ 사용자가 자신의 통화를 선택할 수 있도록 설정 제공
- ✅ 첫 실행 시 국가/언어 기반 자동 감지
- ✅ 모든 금액 계산은 KRW 기준으로 통일
- ✅ 환율 관리 단순화 (고정 환율, 분기별 업데이트)

---

## 2. 문제 정의

### 2.1 스페인어권의 통화 다양성

| 국가 | 언어 | 통화 | 기호 | 환율 (KRW 기준) |
|------|------|------|------|-----------------|
| 스페인 | es-ES | 유로 (EUR) | € | 1,400원 |
| 멕시코 | es-MX | 페소 (MXN) | MX$ | 75원 |
| 아르헨티나 | es-AR | 페소 (ARS) | ARS$ | 3원 |
| 콜롬비아 | es-CO | 페소 (COP) | COL$ | 0.3원 |
| 칠레 | es-CL | 페소 (CLP) | CLP$ | 1.5원 |

**문제점:**
- 언어 코드(`es`)만으로는 통화 결정 불가
- 국가 코드(`es-MX`, `es-AR`)도 완벽하지 않음 (여행자, 해외 거주자)
- 하드코딩 시 유지보수 어려움

### 2.2 기존 구현의 한계

```kotlin
// 기존 코드 (문제점)
fun formatMoney(amountInKRW: Double): String {
    val locale = Locale.getDefault()
    return when (locale.language) {
        "ko" -> "${amountInKRW.toInt()}원"
        "ja" -> "¥${(amountInKRW / 10).toInt()}"
        "en" -> "$${(amountInKRW / 1300).toInt()}"
        else -> "$${(amountInKRW / 1300).toInt()}"  // 폴백
    }
}
```

**한계:**
1. 스페인어 사용자가 어떤 통화를 쓰는지 알 수 없음
2. 사용자가 원하는 통화로 변경 불가
3. 환율 변경 시 코드 수정 필요

---

## 3. 해결 방안

### 3.1 추천 접근법: 사용자 선택 + 자동 감지

```
┌─────────────────────────────────────┐
│   첫 실행 시                        │
│   ┌─────────────────────────────┐   │
│   │ 로케일 감지                 │   │
│   │ Locale.getDefault()         │   │
│   └────────┬────────────────────┘   │
│            │                         │
│            ▼                         │
│   ┌─────────────────────────────┐   │
│   │ 국가 코드 우선              │   │
│   │ locale.country              │   │
│   │ - KR → KRW                  │   │
│   │ - JP → JPY                  │   │
│   │ - US → USD                  │   │
│   └────────┬────────────────────┘   │
│            │                         │
│            ▼                         │
│   ┌─────────────────────────────┐   │
│   │ 언어 기반 폴백              │   │
│   │ locale.language             │   │
│   │ - es → EUR (스페인어 기본)  │   │
│   │ - en → USD                  │   │
│   └────────┬────────────────────┘   │
│            │                         │
│            ▼                         │
│   ┌─────────────────────────────┐   │
│   │ SharedPreferences 저장      │   │
│   │ "currency" = "KRW"          │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────┐
│   이후 실행                         │
│   ┌─────────────────────────────┐   │
│   │ 저장된 설정 사용            │   │
│   │ prefs.getString("currency") │   │
│   └─────────────────────────────┘   │
│            │                         │
│            ▼                         │
│   ┌─────────────────────────────┐   │
│   │ 설정 화면에서 변경 가능     │   │
│   │ - 통화 목록 표시            │   │
│   │ - RadioButton 선택          │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 3.2 데이터 모델

```kotlin
/**
 * 통화 옵션 데이터 클래스
 * 
 * @param code ISO 4217 통화 코드 (예: "KRW", "USD")
 * @param symbol 통화 기호 (예: "₩", "$")
 * @param nameResId 통화 이름 리소스 ID (다국어 지원)
 * @param rate KRW 대비 환율 (예: USD = 1300.0 → 1달러 = 1300원)
 * @param decimalPlaces 소수점 자리수 (원/엔 = 0, 달러/유로 = 2)
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameResId: Int,
    val rate: Double,
    val decimalPlaces: Int = 0
)
```

### 3.3 환율 관리 전략

#### 옵션 A: 고정 환율 (Phase 1-3 권장 ✅)

**장점:**
- 구현 간단
- 오프라인 동작
- 외부 의존성 없음

**단점:**
- 환율 변동 미반영
- 정확도 다소 낮음

**업데이트 방법:**
```kotlin
// 분기별 1회 코드 업데이트
private const val KRW_TO_JPY = 0.1      // 2025-Q4
private const val KRW_TO_USD = 1300.0   // 2025-Q4
private const val KRW_TO_EUR = 1400.0   // 2025-Q4
// ...

// Release Notes에 명시
// "환율 업데이트 (2025-Q4 기준): 1달러 = 1,300원"
```

#### 옵션 B: API 연동 (Phase 4+ 장기 과제)

**장점:**
- 실시간 환율 반영
- 정확도 높음

**단점:**
- 인터넷 연결 필요
- API 키 관리
- 무료 API 요청 제한

**추천 API:**
- [ExchangeRate-API](https://www.exchangerate-api.com/) - 무료 플랜 1,500 요청/월
- [Fixer.io](https://fixer.io/) - 무료 플랜 100 요청/월
- [Open Exchange Rates](https://openexchangerates.org/) - 무료 플랜 1,000 요청/월

**구현 예시:**
```kotlin
// Phase 4+ (선택 사항)
suspend fun updateExchangeRates(context: Context) {
    try {
        val response = api.getLatestRates(base = "KRW")
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("rates_json", gson.toJson(response.rates))
            .putLong("rates_updated", System.currentTimeMillis())
            .apply()
    } catch (e: Exception) {
        // 실패 시 고정 환율 사용
        Log.w("CurrencyManager", "Failed to update rates, using fallback")
    }
}
```

---

## 4. 구현 가이드

### 4.1 파일 구조

```
app/src/main/java/com/sweetapps/alcoholictimer/
├── core/
│   └── util/
│       ├── CurrencyManager.kt          (새로 생성)
│       └── FormatUtils.kt              (수정)
└── feature/
    └── settings/
        └── SettingsActivity.kt         (수정)

app/src/main/res/
├── values/
│   └── strings.xml                     (통화 이름 추가)
├── values-en/
│   └── strings.xml
└── values-ja/
    └── strings.xml
```

### 4.2 Step 1: CurrencyManager.kt 생성

```kotlin
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
     */
    val supportedCurrencies = listOf(
        CurrencyOption("KRW", "₩", R.string.currency_krw, 1.0, 0),
        CurrencyOption("JPY", "¥", R.string.currency_jpy, 0.1, 0),
        CurrencyOption("USD", "$", R.string.currency_usd, 1300.0, 2),
        CurrencyOption("EUR", "€", R.string.currency_eur, 1400.0, 2),
        CurrencyOption("MXN", "MX$", R.string.currency_mxn, 75.0, 0),
        CurrencyOption("CNY", "¥", R.string.currency_cny, 180.0, 2),
        CurrencyOption("BRL", "R$", R.string.currency_brl, 250.0, 2)
    )
    
    /**
     * 금액을 사용자 선택 통화로 포맷팅
     * 
     * @param amountInKRW 원화 기준 금액
     * @param context Context
     * @return 포맷팅된 문자열 (예: "¥1,000", "$10.00")
     */
    fun formatMoney(amountInKRW: Double, context: Context): String {
        val currency = getSelectedCurrency(context)
        val converted = amountInKRW / currency.rate
        
        return when (currency.code) {
            "KRW" -> {
                // 한국어: 숫자 + 원 (예: 10,000원)
                String.format(Locale.getDefault(), "%,.0f%s", converted, currency.symbol)
            }
            "JPY", "MXN" -> {
                // 엔화, 멕시코 페소: 기호 + 정수 (예: ¥1,000, MX$100)
                String.format(Locale.getDefault(), "%s%,.0f", currency.symbol, converted)
            }
            else -> {
                // 달러, 유로 등: 기호 + 소수점 2자리 (예: $10.00, €8.50)
                String.format(
                    Locale.getDefault(), 
                    "%s%,.${currency.decimalPlaces}f", 
                    currency.symbol, 
                    converted
                )
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
```

### 4.3 Step 2: FormatUtils.kt 수정

```kotlin
// FormatUtils.kt의 formatMoney 함수를 CurrencyManager 사용으로 변경

@JvmStatic
fun formatMoney(context: Context, amountInWon: Double): String {
    return CurrencyManager.formatMoney(amountInWon, context)
}
```

### 4.4 Step 3: strings.xml 추가

```xml
<!-- values/strings.xml -->
<string name="settings_currency">통화</string>
<string name="currency_krw">대한민국 원 (₩)</string>
<string name="currency_jpy">일본 엔 (¥)</string>
<string name="currency_usd">미국 달러 ($)</string>
<string name="currency_eur">유로 (€)</string>
<string name="currency_mxn">멕시코 페소 (MX$)</string>
<string name="currency_cny">중국 위안 (¥)</string>
<string name="currency_brl">브라질 헤알 (R$)</string>

<!-- values-en/strings.xml -->
<string name="settings_currency">Currency</string>
<string name="currency_krw">Korean Won (₩)</string>
<string name="currency_jpy">Japanese Yen (¥)</string>
<string name="currency_usd">US Dollar ($)</string>
<string name="currency_eur">Euro (€)</string>
<string name="currency_mxn">Mexican Peso (MX$)</string>
<string name="currency_cny">Chinese Yuan (¥)</string>
<string name="currency_brl">Brazilian Real (R$)</string>

<!-- values-ja/strings.xml -->
<string name="settings_currency">通貨</string>
<string name="currency_krw">韓国ウォン (₩)</string>
<string name="currency_jpy">日本円 (¥)</string>
<string name="currency_usd">米ドル ($)</string>
<string name="currency_eur">ユーロ (€)</string>
<string name="currency_mxn">メキシコペソ (MX$)</string>
<string name="currency_cny">中国元 (¥)</string>
<string name="currency_brl">ブラジルレアル (R$)</string>
```

### 4.5 Step 4: SettingsActivity.kt에 통화 선택 추가

```kotlin
// SettingsActivity.kt

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    // ...existing code...
    
    var selectedCurrency by remember { 
        mutableStateOf(CurrencyManager.getSelectedCurrency(context).code) 
    }
    
    Column(/*...*/) {
        // ...existing settings...
        
        SectionDivider()
        
        SettingsSection(
            title = stringResource(R.string.settings_currency), 
            titleColor = colorResource(id = R.color.color_indicator_money)
        ) {
            SettingsCurrencyGroup(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { newCurrency ->
                    selectedCurrency = newCurrency
                    CurrencyManager.saveCurrency(context, newCurrency)
                }
            )
        }
    }
}

@Composable
fun SettingsCurrencyGroup(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CurrencyManager.supportedCurrencies.forEach { currency ->
            SettingsOptionItem(
                isSelected = selectedCurrency == currency.code,
                label = "${stringResource(currency.nameResId)} ${currency.symbol}",
                onSelected = { onCurrencySelected(currency.code) }
            )
        }
    }
}
```

### 4.6 Step 5: MainActivity에서 초기화

```kotlin
// MainActivity.kt (또는 StartActivity.kt)

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 첫 실행 시 기본 통화 설정
    CurrencyManager.initializeDefaultCurrency(this)
    
    // ...existing code...
}
```

---

## 5. 테스트 시나리오

### 5.1 자동 감지 테스트

| 테스트 케이스 | 로케일 설정 | 예상 결과 |
|---------------|-------------|-----------|
| TC-01 | 한국어 (ko-KR) | KRW 선택됨 |
| TC-02 | 일본어 (ja-JP) | JPY 선택됨 |
| TC-03 | 영어-미국 (en-US) | USD 선택됨 |
| TC-04 | 스페인어-스페인 (es-ES) | EUR 선택됨 |
| TC-05 | 스페인어-멕시코 (es-MX) | MXN 선택됨 |
| TC-06 | 중국어 (zh-CN) | CNY 선택됨 |
| TC-07 | 포르투갈어-브라질 (pt-BR) | BRL 선택됨 |
| TC-08 | 독일어 (de-DE) | EUR 선택됨 |

**테스트 방법:**
```kotlin
@Test
fun `로케일에 따라 올바른 기본 통화 선택`() {
    // Given
    val context = mockContext()
    Locale.setDefault(Locale.JAPAN)
    
    // When
    CurrencyManager.initializeDefaultCurrency(context)
    val selected = CurrencyManager.getSelectedCurrency(context)
    
    // Then
    assertEquals("JPY", selected.code)
}
```

### 5.2 포맷팅 테스트

| 금액 (KRW) | 통화 | 예상 출력 |
|-----------|------|-----------|
| 10,000원 | KRW | "10,000₩" |
| 10,000원 | JPY | "¥1,000" |
| 10,000원 | USD | "$7.69" |
| 10,000원 | EUR | "€7.14" |
| 100,000원 | MXN | "MX$1,333" |

**테스트 방법:**
```kotlin
@Test
fun `원화를 엔화로 올바르게 변환`() {
    // Given
    val context = mockContext()
    saveCurrency(context, "JPY")
    
    // When
    val result = CurrencyManager.formatMoney(10000.0, context)
    
    // Then
    assertEquals("¥1,000", result)
}
```

### 5.3 사용자 시나리오 테스트

**시나리오 1: 스페인어 사용자 (멕시코)**
```
1. 앱 설치 (로케일: es-MX)
2. 첫 실행
   → 기본 통화: MXN (멕시코 페소) 자동 설정
3. 금주 시작
4. 1일 경과, 음주 비용 5만원 설정
   → 표시: "MX$667 절약" (50,000 / 75)
5. 설정에서 통화를 EUR(유로)로 변경
   → 표시: "€35.71 ahorrados"
```

**시나리오 2: 일본어 사용자 (여행 중)**
```
1. 로케일: ja-JP
2. 기본 통화: JPY
   → 표시: "¥5,000 節約"
3. 한국 여행 중 설정에서 KRW로 변경
   → 표시: "50,000₩ 節約"
4. 귀국 후 JPY로 다시 변경
```

---

## 6. FAQ

### Q1: 왜 API 연동이 아닌 고정 환율을 사용하나요?

**A:** Phase 1-3에서는 다음 이유로 고정 환율을 권장합니다:
- ✅ 오프라인 동작 가능
- ✅ 구현 단순화
- ✅ 외부 의존성 없음
- ✅ 절약 금액은 "대략적 추정"이므로 정확도 덜 중요

환율 변동이 심한 통화(아르헨티나 페소 등)는 Phase 3 이후 API 연동 고려

### Q2: 사용자가 환율을 직접 입력할 수 있나요?

**A:** Phase 1에서는 미지원. Phase 4+ 기능으로 검토 중:
```kotlin
// 향후 계획
data class CurrencyOption(
    // ...existing...
    val customRate: Double? = null  // 사용자 지정 환율
)
```

### Q3: 통화 기호가 중복되는 경우(¥, $)는 어떻게 구분하나요?

**A:** 설정 화면에서 통화 이름과 함께 표시:
- 일본 엔 (¥)
- 중국 위안 (¥)
- 미국 달러 ($)
- 멕시코 페소 (MX$)

### Q4: 새로운 통화를 추가하려면?

**A:** `CurrencyManager.kt`의 `supportedCurrencies` 리스트에 추가:
```kotlin
CurrencyOption("ARS", "ARS$", R.string.currency_ars, 3.0, 2)
```

그리고 strings.xml에 이름 추가:
```xml
<string name="currency_ars">Argentine Peso (ARS$)</string>
```

### Q5: 환율은 얼마나 자주 업데이트하나요?

**A:** 분기별 1회 (3개월마다) 코드 수정으로 업데이트:
- Q1: 1월 1일
- Q2: 4월 1일
- Q3: 7월 1일
- Q4: 10월 1일

Release Notes에 "환율 업데이트 (2025-Q4 기준)" 명시

### Q6: 기존 사용자는 어떻게 되나요?

**A:** 
- 저장된 설정 없음 → 자동 감지 후 저장
- 기존 금액 데이터(KRW 저장) → 변환 없이 계속 사용
- 통화만 변경 가능, 과거 데이터는 불변

---

## 7. 체크리스트

### Phase 1 구현 전

- [ ] `CurrencyManager.kt` 생성
- [ ] `FormatUtils.kt` 수정
- [ ] strings.xml 7개 언어 통화 이름 추가
- [ ] `SettingsActivity.kt` 통화 선택 UI 추가
- [ ] `MainActivity.kt`에서 초기화 호출
- [ ] 단위 테스트 작성 (자동 감지, 포맷팅)
- [ ] UI 테스트 (7개 통화 전환)
- [ ] 에뮬레이터 로케일 변경 테스트

### Phase 1 출시 전

- [ ] 전체 언어(한국어, 영어, 일본어) 통화 표시 확인
- [ ] Release Notes에 "통화 선택 기능 추가" 명시
- [ ] Play Store 스크린샷에 설정 화면 포함
- [ ] 환율 출처 명시 (앱 정보 또는 설정 화면)

---

## 8. 참고 자료

### 8.1 환율 정보 출처
- [한국은행 환율 정보](https://www.bok.or.kr/portal/main/main.do)
- [Google Finance](https://www.google.com/finance)
- [XE Currency Converter](https://www.xe.com/)

### 8.2 통화 코드 표준
- [ISO 4217 Currency Codes](https://www.iso.org/iso-4217-currency-codes.html)

### 8.3 Android Localization
- [Android Developers - Localize your app](https://developer.android.com/guide/topics/resources/localization)
- [NumberFormat - Android Developers](https://developer.android.com/reference/java/text/NumberFormat)

---

**최종 업데이트**: 2025-10-28  
**작성자**: AlcoholicTimer 개발팀  
**다음 검토일**: 2026-01-01 (Q1 환율 업데이트)

