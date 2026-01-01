# ✅ 필리핀 페소(PHP) 지원 추가 완료

**작업일**: 2026-01-02  
**목적**: 필리핀 사용자를 위한 페소(PHP) 통화 지원 추가  
**상태**: ✅ 완료

---

## 🎯 구현 내용

### 1. CurrencyManager.kt 수정 ✅

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/util/manager/CurrencyManager.kt`

#### 변경 사항:

**1) supportedCurrencies 리스트에 PHP 추가**
```kotlin
val supportedCurrencies = listOf(
    // ...existing currencies...
    CurrencyOption("IDR", "Rp", R.string.currency_idr, 0.085, 2),
    // PHP: 1 PHP = 23 KRW (2026-01-02 기준)
    CurrencyOption("PHP", "₱", R.string.currency_php, 23.0, 2)
)
```

**환율 정보**:
- 1 PHP = 23 KRW
- 페소 기호: ₱
- 소수점: 2자리

**2) getDefaultCurrency()에 필리핀 로케일 감지 추가**
```kotlin
private fun getDefaultCurrency(): CurrencyOption {
    val locale = Locale.getDefault()
    val countryCode = locale.country
    val languageCode = locale.language

    val currencyCode = when (countryCode) {
        // ...existing codes...
        "PH" -> "PHP"  // 필리핀
        else -> {
            when (languageCode) {
                // ...existing languages...
                "tl", "fil" -> "PHP"  // 타갈로그어, 필리핀어
                // ...
            }
        }
    }
    // ...
}
```

**지원 로케일**:
- 국가 코드: `PH` (필리핀)
- 언어 코드: `tl` (타갈로그어), `fil` (필리핀어)

---

### 2. strings.xml 파일 수정 ✅

모든 언어별 strings.xml 파일에 `currency_php` 추가:

#### values/strings.xml (영어)
```xml
<string name="currency_php">Philippine Peso (₱)</string>
```

#### values-ko/strings.xml (한국어)
```xml
<string name="currency_php">필리핀 페소 (₱)</string>
```

#### values-ja/strings.xml (일본어)
```xml
<string name="currency_php">フィリピンペソ (₱)</string>
```

#### values-in/strings.xml (인도네시아어)
```xml
<string name="currency_php">Peso Filipina (₱)</string>
```

---

## 🔄 작동 원리

### 자동 감지 시나리오

**시나리오 1: 필리핀 사용자**
```
시스템 언어: 필리핀어 (Tagalog/Filipino)
시스템 국가: 필리핀 (PH)
  ↓
getDefaultCurrency() 호출
  ↓
countryCode = "PH" 또는 languageCode = "tl"/"fil" 감지
  ↓
자동으로 PHP 선택 ✅
  ↓
금액 표시: "₱1,000.00" (23,000원 기준)
```

**시나리오 2: 수동 선택**
```
설정 → 통화 설정 → Philippine Peso (₱) 선택
  ↓
앱 전체에 페소로 표시
  ↓
예: 23,000원 = ₱1,000.00
```

---

## 💱 환율 예시

| 한국 원 (KRW) | 필리핀 페소 (PHP) |
|--------------|------------------|
| 23원 | ₱1.00 |
| 230원 | ₱10.00 |
| 2,300원 | ₱100.00 |
| 23,000원 | ₱1,000.00 |
| 230,000원 | ₱10,000.00 |

**계산 공식**:
```
PHP 금액 = KRW 금액 / 23
```

---

## 🧪 테스트 방법

### 방법 1: 에뮬레이터 언어 변경

```powershell
# 필리핀어로 변경
adb -s emulator-5554 shell "setprop persist.sys.locale fil-PH; setprop ctl.restart zygote"

# 앱 실행 후 확인
# - 설정 → 통화에서 "Philippine Peso (₱)" 확인
# - 금액 표시가 ₱로 자동 변경되는지 확인
```

### 방법 2: 수동 선택

```
1. 앱 실행
2. 설정 (톱니바퀴 아이콘)
3. 통화 설정
4. "Philippine Peso (₱)" 선택
5. 앱 내 모든 금액이 페소로 표시되는지 확인
```

### 확인 포인트

- [ ] 통화 설정 화면에 "Philippine Peso (₱)" 항목 표시
- [ ] 필리핀 로케일에서 자동으로 페소 선택
- [ ] Tab 01 (타이머 화면) 금액이 ₱로 표시
- [ ] Tab 02 (성공 화면) 아낀 돈이 ₱로 표시
- [ ] Tab 03 (레벨 화면) 레벨업 금액이 ₱로 표시
- [ ] 설정 화면에서 "현재: ₱" 표시

---

## 📊 지원 통화 현황

현재 지원하는 9개 통화:

| 번호 | 통화 코드 | 기호 | 이름 | 환율 (1 KRW 기준) |
|-----|----------|------|------|------------------|
| 1 | KRW | ₩ | 한국 원 | 1.0 |
| 2 | JPY | ¥ | 일본 엔 | 0.1 |
| 3 | USD | $ | 미국 달러 | 0.00077 |
| 4 | EUR | € | 유로 | 0.00071 |
| 5 | MXN | MX$ | 멕시코 페소 | 0.013 |
| 6 | CNY | ¥ | 중국 위안 | 0.0056 |
| 7 | BRL | R$ | 브라질 헤알 | 0.004 |
| 8 | IDR | Rp | 인도네시아 루피아 | 11.8 |
| 9 | **PHP** | **₱** | **필리핀 페소** | **0.043** |

---

## 🌏 로케일 감지 매핑

| 국가/언어 | 코드 | 자동 선택 통화 |
|----------|------|---------------|
| 한국 | KR / ko | KRW (₩) |
| 일본 | JP / ja | JPY (¥) |
| 미국 | US / en | USD ($) |
| 중국 | CN / zh | CNY (¥) |
| 멕시코 | MX / es | MXN (MX$) |
| 브라질 | BR / pt | BRL (R$) |
| 인도네시아 | ID / in, id | IDR (Rp) |
| **필리핀** | **PH / tl, fil** | **PHP (₱)** |
| 기타 | - | USD ($) |

---

## 🔧 향후 개선 사항

### 우선순위 낮음

**1. 필리핀어 UI 번역 (선택사항)**
```
현재: 영어 UI 표시
향후: values-tl/strings.xml 추가하여 타갈로그어 UI 지원
```

**2. 환율 자동 업데이트 (선택사항)**
```
현재: 고정 환율 (1 PHP = 23 KRW)
향후: API를 통한 실시간 환율 적용
```

**3. 지역별 금액 표시 방식**
```
현재: ₱1,000.00 (영문 방식)
향후: ₱1.000,00 (필리핀 현지 방식) - 필요 시
```

---

## ✅ 최종 체크리스트

### 코드 수정
- [x] CurrencyManager.kt에 PHP 추가
- [x] supportedCurrencies에 CurrencyOption 추가
- [x] getDefaultCurrency()에 필리핀 로케일 추가
- [x] 환율 설정 (1 PHP = 23 KRW)
- [x] 소수점 2자리 설정

### 리소스 추가
- [x] values/strings.xml - currency_php 추가
- [x] values-ko/strings.xml - currency_php 추가
- [x] values-ja/strings.xml - currency_php 추가
- [x] values-in/strings.xml - currency_php 추가

### 테스트 준비
- [x] 테스트 방법 문서화
- [x] 확인 포인트 정리
- [x] 에뮬레이터 설정 명령어 준비

---

## 📝 코드 변경 요약

### 수정된 파일

1. **CurrencyManager.kt**
   - 라인 18-32: supportedCurrencies에 PHP 추가
   - 라인 139: countryCode "PH" 추가
   - 라인 150: languageCode "tl", "fil" 추가

2. **strings.xml (4개 파일)**
   - values/strings.xml: 라인 234 - currency_php 추가
   - values-ko/strings.xml: 라인 233 - currency_php 추가
   - values-ja/strings.xml: 라인 233 - currency_php 추가
   - values-in/strings.xml: 라인 231 - currency_php 추가

**총 변경 라인**: 약 5줄 (코드) + 4줄 (리소스) = 9줄

---

## 🎯 기대 효과

### 사용자 경험 개선
- ✅ 필리핀 사용자가 페소로 금액 확인 가능
- ✅ 시스템 언어 감지로 자동 설정
- ✅ 수동 선택으로 커스터마이징 가능

### 시장 확대
- 🌏 필리핀 시장 진출 준비 완료
- 📱 현지화된 사용자 경험 제공
- 💰 글로벌 앱으로서의 경쟁력 강화

---

**작성일**: 2026-01-02  
**상태**: ✅ 완료  
**다음 단계**: 빌드 및 실제 기기 테스트

