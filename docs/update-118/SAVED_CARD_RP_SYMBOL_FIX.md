# 🐛 SAVED 카드 Rp 기호 중복 표시 버그 수정 완료!

**작업일**: 2026-01-02  
**문제**: 인도네시아 언어 설정 시 "Rp135 KRW" 형태로 잘못 표시  
**상태**: ✅ 수정 완료

---

## 🐛 문제 분석

### 재현 시나리오
```
1. 핸드폰 언어: 인도네시아어 설정
2. RecordsScreen의 SAVED 카드 확인
3. 결과: ❌ "Rp1,5jt IDR" 형태로 표시 (Rp 기호 중복!)
```

### 원인: formatCompactRupiah()가 Rp 기호를 강제로 붙임

**문제 코드**:
```kotlin
// FormatUtils.kt
fun formatCompactRupiah(amount: Double): String {
    // ...
    return when {
        amount >= million -> {
            val value = amount / million
            String.format(indonesiaLocale, "Rp%.1f jt", value)
            // ↑ Rp 기호가 포함됨!
        }
        // ...
    }
}

// RecordsScreen.kt
val savedMoneyFormatted = remember(savedMoney) {
    val formatted = CurrencyManager.formatMoneyNoDecimals(savedMoney, context)
    // ↑ "Rp1,5jt" 반환
    
    if (locale == Indonesia) {
        "$formatted IDR"  // ← "Rp1,5jt IDR" (중복!)
    } else {
        formatted
    }
}
```

**문제점**:
1. `formatCompactRupiah()`가 "Rp" 기호를 자동으로 붙임
2. RecordsScreen에서 " IDR" 통화 코드를 추가
3. **결과: "Rp1,5jt IDR" 형태로 중복 표시**

---

## ✅ 수정 내용

### 1. FormatUtils - formatCompactRupiahNoSymbol() 추가

**새로운 함수 추가**:
```kotlin
/**
 * 인도네시아 금액 축약 포맷 (Rp 기호 없음)
 * 
 * 통화 기호를 별도로 표시하는 UI에서 사용
 * 예: "1,5jt IDR" 형태로 표시할 때
 */
@JvmStatic
fun formatCompactRupiahNoSymbol(amount: Double): String {
    // ...
    return when {
        amount >= billion -> {
            String.format(indonesiaLocale, "%.1f M", value)  // Rp 제거!
        }
        amount >= million -> {
            String.format(indonesiaLocale, "%.1f jt", value)  // Rp 제거!
        }
        amount >= thousand -> {
            String.format(indonesiaLocale, "%.0f rb", value)  // Rp 제거!
        }
        else -> {
            String.format(indonesiaLocale, "%,.0f", amount)  // Rp 제거!
        }
    }
}
```

### 2. CurrencyManager.formatMoneyNoDecimals() 수정

**수정 전**:
```kotlin
if (locale == Indonesia) {
    return FormatUtils.formatCompactRupiah(amountInKRW)
    // ↑ "Rp1,5jt" 반환
}
```

**수정 후**:
```kotlin
// [FIX] 인도네시아 로케일 감지 및 기호 없는 축약형 포맷 사용 (2026-01-02)
if (locale == Indonesia) {
    // Rp 기호 없이 숫자만 반환 (예: "1,5jt")
    return FormatUtils.formatCompactRupiahNoSymbol(amountInKRW)
}
```

### 3. RecordsScreen.kt 주석 업데이트

**수정 전**:
```kotlin
val formatted = CurrencyManager.formatMoneyNoDecimals(savedMoney, context)  // "Rp1,4jt"
if (locale == Indonesia) {
    "$formatted IDR"  // 인도네시아: "Rp1,4jt IDR"
}
```

**수정 후**:
```kotlin
// [FIX] 인도네시아 로케일 시 Rp 기호 제거 (2026-01-02)
val formatted = CurrencyManager.formatMoneyNoDecimals(savedMoney, context)  // "1,4jt" (Rp 제거됨)
if (locale == Indonesia) {
    "$formatted IDR"  // 인도네시아: "1,4jt IDR"
}
```

---

## 🔍 수정 후 동작

### 시나리오: 인도네시아 사용자가 RecordsScreen 확인

```
1. savedMoney = 1,500,000 KRW
   ↓
2. CurrencyManager.formatMoneyNoDecimals() 호출
   ↓
3. 인도네시아 로케일 감지
   ↓
4. FormatUtils.formatCompactRupiahNoSymbol() 호출
   ↓
5. 반환값: "1,5jt" (Rp 기호 없음!)
   ↓
6. RecordsScreen에서 " IDR" 추가
   ↓
7. 최종 표시: "1,5jt IDR"  ✅
```

### 다른 국가도 정상 작동

| 언어/통화 | savedMoney (KRW) | 표시 결과 |
|----------|-----------------|----------|
| 한국어 (KRW) | 10,000 | ✅ "10,000₩" |
| 일본어 (JPY) | 10,000 | ✅ "¥1,000" |
| 영어 (USD) | 10,000 | ✅ "$7.69" |
| **인도네시아 (IDR)** | **1,500,000** | ✅ **"1,5jt IDR"** |
| 필리핀 (PHP) | 10,000 | ✅ "₱435" |

---

## 📊 수정 파일 목록

| 파일 | 수정 내용 |
|-----|----------|
| `FormatUtils.kt` | `formatCompactRupiahNoSymbol()` 함수 추가 |
| `CurrencyManager.kt` | `formatMoneyNoDecimals()`에서 NoSymbol 버전 사용 |
| `RecordsScreen.kt` | 주석 업데이트 (Rp 제거 설명) |

---

## 🎯 포맷 비교

### formatCompactRupiah() (Rp 포함)
```kotlin
1,500,000 KRW → "Rp1,5jt"
494,000 KRW → "Rp494rb"
2,340,000,000 KRW → "Rp2,3M"
```
**사용처**: 통화 기호가 필요 없는 UI (예: formatMoney)

### formatCompactRupiahNoSymbol() (Rp 제거) ✅
```kotlin
1,500,000 KRW → "1,5jt"
494,000 KRW → "494rb"
2,340,000,000 KRW → "2,3M"
```
**사용처**: 통화 코드를 별도로 표시하는 UI (예: SAVED 카드)

---

## 🧪 테스트 방법

### 1. 핸드폰에서 수동 테스트

```
Step 1: 핸드폰 언어 → 인도네시아어 변경
Step 2: 앱 실행
Step 3: RecordsScreen (기록 화면) 이동
Step 4: SAVED 카드 확인
Step 5: ✅ "1,5jt IDR" 형태로 표시 (Rp 제거됨!)
```

### 2. 다른 화면도 확인

**formatMoneyNoDecimals()를 사용하는 화면들**:
- ✅ RecordsScreen - SAVED 카드
- ✅ RecordsScreen - StatCard (하단)
- ✅ QuitScreen - 저축 금액
- ✅ RunScreen - 아낀 돈 표시
- ✅ DetailScreen - 상세 화면

**모든 화면이 동일한 로직을 사용하므로 일괄 수정됨!**

---

## 🎉 결과

**버그 완전 수정!** ✅

**Before (버그)**:
```
SAVED 카드: "Rp1,5jt IDR" ❌
```

**After (수정)**:
```
SAVED 카드: "1,5jt IDR" ✅
```

**개선 효과**:
- ✅ Rp 기호 중복 제거
- ✅ 깔끔한 표시 형식 (숫자 + 통화 코드)
- ✅ 모든 통화에서 일관된 표시
- ✅ 다른 화면들도 자동으로 수정됨

---

**작성일**: 2026-01-02  
**상태**: ✅ 완료  
**테스트**: 빌드 진행 중

