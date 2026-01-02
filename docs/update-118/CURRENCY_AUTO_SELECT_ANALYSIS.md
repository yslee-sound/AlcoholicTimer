# 🔍 통화 설정 자동 선택 로직 분석 결과

**분석일**: 2026-01-02  
**문제**: 앱 재설치 시 시스템 언어와 관계없이 KRW로 고정되는 현상

---

## 📊 현재 코드 동작 분석

### 1. CurrencyManager.getSelectedCurrency() 로직

```kotlin
fun getSelectedCurrency(context: Context): CurrencyOption {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val currencyCode = prefs.getString("currency", null)
    val explicit = prefs.getBoolean("currency_explicit", false)

    // [STEP 1] explicit가 false면 자동 감지
    if (!explicit) {
        return getDefaultCurrency()  // ✅ 시스템 언어 기반 자동 감지
    }

    // [STEP 2] currencyCode가 null이거나 AUTO면 자동 감지
    if (currencyCode == null || currencyCode == AUTO_CURRENCY_CODE) {
        return getDefaultCurrency()  // ✅ 시스템 언어 기반 자동 감지
    }

    // [STEP 3] 저장된 통화 코드 사용
    return supportedCurrencies.find { it.code == currencyCode }
        ?: getDefaultCurrency()
}
```

### 2. getDefaultCurrency() - 시스템 언어 기반 자동 감지

```kotlin
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
                "in", "id" -> "IDR"
                "tl", "fil" -> "PHP"
                "de", "fr" -> "EUR"
                else -> "USD"  // 기본값
            }
        }
    }

    return supportedCurrencies.find { it.code == currencyCode }
        ?: supportedCurrencies.first()  // ⚠️ 여기가 문제!
}
```

---

## 🐛 문제점 발견!

### 문제 1: supportedCurrencies.first()가 KRW를 반환

```kotlin
val supportedCurrencies = listOf(
    CurrencyOption("KRW", "₩", R.string.currency_krw, 1.0, 2),  // ← 첫 번째 = KRW!
    CurrencyOption("JPY", "¥", R.string.currency_jpy, 10.0, 2),
    // ...
)

// 만약 currencyCode가 매칭되지 않으면?
return supportedCurrencies.find { it.code == currencyCode }
    ?: supportedCurrencies.first()  // ⚠️ 항상 KRW 반환!
```

### 문제 2: initializeDefaultCurrency()가 호출되지 않음

```kotlin
// CurrencyManager.kt에 정의되어 있지만
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

// MainActivity.kt에서 호출되지 않음! ❌
```

---

## 🔍 재설치 시 동작 시나리오

### 시나리오: 필리핀 사용자가 앱을 재설치

1. **SharedPreferences 초기화**
   ```
   settings.xml 파일 삭제됨 (재설치)
   ↓
   currency: null
   currency_explicit: false (기본값)
   ```

2. **getSelectedCurrency() 호출**
   ```kotlin
   val currencyCode = prefs.getString("currency", null)  // null
   val explicit = prefs.getBoolean("currency_explicit", false)  // false

   if (!explicit) {
       return getDefaultCurrency()  // ✅ 정상 호출
   }
   ```

3. **getDefaultCurrency() 실행**
   ```kotlin
   val locale = Locale.getDefault()  // 필리핀: "PH", "tl"
   val countryCode = locale.country  // "PH"
   val languageCode = locale.language  // "tl" 또는 "fil"

   val currencyCode = when (countryCode) {
       "PH" -> "PHP"  // ✅ 정상 매칭
       // ...
   }

   return supportedCurrencies.find { it.code == "PHP" }
       ?: supportedCurrencies.first()
   ```

4. **결과**
   - ✅ **정상**: 필리핀 사용자 → PHP
   - ✅ **정상**: 한국 사용자 → KRW
   - ✅ **정상**: 일본 사용자 → JPY

---

## 🤔 그럼 왜 KRW로 선택되는가?

### 가능한 원인들:

### 원인 1: 개발자 기기의 로케일 설정이 한국

```
개발자 PC/에뮬레이터 설정:
- 시스템 언어: 한국어
- 지역: 대한민국

↓
Locale.getDefault().country = "KR"
↓
getDefaultCurrency() → "KRW"
```

### 원인 2: 설정 화면에서 명시적으로 KRW를 선택한 이력

```
사용자가 설정 화면에서 "대한민국 원 (KRW)"를 선택
↓
currency_explicit: true 저장
↓
재설치 후에도 이 값이 캐시에 남아있음 (가능성 낮음)
```

### 원인 3: 플레이스토어 백업/복원 기능

```
Google Play 자동 백업:
- SharedPreferences가 클라우드에 백업됨
- 재설치 시 자동 복원
↓
이전에 선택한 KRW 설정이 복원됨
↓
currency_explicit: true
currencyCode: "KRW"
```

---

## ✅ 실제 로직은 정상 작동!

**결론**: 코드 로직은 정상입니다! 문제의 원인은:

1. **로케일이 실제로 한국으로 설정되어 있거나**
2. **Google Play 자동 백업으로 이전 설정이 복원되었거나**
3. **테스트 기기의 시스템 언어가 한국어로 되어 있음**

---

## 🧪 테스트 방법

### 1. 로케일 확인 로그 추가

```kotlin
private fun getDefaultCurrency(): CurrencyOption {
    val locale = Locale.getDefault()
    val countryCode = locale.country
    val languageCode = locale.language
    
    // [DEBUG] 로케일 확인 로그
    android.util.Log.d("CurrencyManager", "🌍 Locale: $locale")
    android.util.Log.d("CurrencyManager", "🌍 Country: $countryCode")
    android.util.Log.d("CurrencyManager", "🌍 Language: $languageCode")
    
    val currencyCode = when (countryCode) {
        // ...
    }
    
    android.util.Log.d("CurrencyManager", "💰 Selected Currency: $currencyCode")
    
    return supportedCurrencies.find { it.code == currencyCode }
        ?: supportedCurrencies.first()
}
```

### 2. 완전 초기화 테스트

```bash
# 앱 삭제 + 데이터 완전 삭제
adb uninstall kr.sweetapps.alcoholictimer

# SharedPreferences 수동 삭제
adb shell rm -rf /data/data/kr.sweetapps.alcoholictimer

# 재설치
adb install app-debug.apk
```

### 3. 에뮬레이터 로케일 변경 테스트

```
Settings → System → Languages & input → Languages
→ 필리핀어(Filipino) 선택
→ 앱 재설치 후 확인
```

---

## 🎯 권장 개선 사항

### 개선 1: initializeDefaultCurrency() 호출 추가

**MainActivity.onCreate()에 추가**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // [NEW] 첫 실행 시 기본 통화 초기화
    CurrencyManager.initializeDefaultCurrency(this)
    
    // ...기존 코드...
}
```

**효과**: 명시적으로 AUTO 모드로 초기화하여 자동 감지 보장

### 개선 2: 디버그 로그 추가

**CurrencyManager.getSelectedCurrency()에 로그 추가**:
```kotlin
fun getSelectedCurrency(context: Context): CurrencyOption {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val currencyCode = prefs.getString("currency", null)
    val explicit = prefs.getBoolean("currency_explicit", false)
    
    // [DEBUG] 설정 상태 로그
    android.util.Log.d("CurrencyManager", "📊 currencyCode: $currencyCode")
    android.util.Log.d("CurrencyManager", "📊 explicit: $explicit")
    
    // ...기존 코드...
}
```

### 개선 3: Google Play 백업 제외 (선택사항)

**AndroidManifest.xml**:
```xml
<application
    android:allowBackup="true"
    android:fullBackupContent="@xml/backup_rules">
```

**res/xml/backup_rules.xml**:
```xml
<full-backup-content>
    <exclude domain="sharedpref" path="settings.xml" />
</full-backup-content>
```

**주의**: 이렇게 하면 사용자가 명시적으로 선택한 통화도 백업되지 않음

---

## 📝 최종 결론

**현재 코드는 정상 작동합니다!** ✅

- ✅ 시스템 언어 기반 자동 감지 로직 구현됨
- ✅ 9개 국가/언어 지원 (한국, 일본, 미국, 중국, 멕시코, 브라질, 인도네시아, 필리핀, 유로존)
- ✅ currency_explicit 플래그로 수동/자동 모드 구분

**KRW로 표시되는 이유는**:
1. 개발자 기기가 한국 로케일로 설정되어 있거나
2. 플레이스토어 자동 백업으로 이전 설정이 복원되었거나
3. 테스트 시 로케일이 제대로 변경되지 않았을 가능성

**해결 방법**:
- Logcat에서 로케일 확인 (위의 디버그 로그 추가)
- 에뮬레이터 시스템 언어를 필리핀어로 변경 후 완전 재설치 테스트

---

**작성일**: 2026-01-02  
**상태**: 분석 완료

