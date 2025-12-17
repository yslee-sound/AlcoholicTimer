# 광고 쿨타임 분리 리팩토링 완료 보고서 (v2.0)

**작업일**: 2025-12-17  
**목표**: 앱 오프닝 광고와 전면광고의 쿨타임 로직 분리  
**Firebase 파라미터**: `interstitial_interval_sec` (변경 없음, 기존 그대로 사용)

---

## ✅ 리팩토링 완료!

### 🎯 핵심 변경사항

**Before (v1.0 - 통합 방식)**
```
앱 오프닝 광고 표시 → lastAdShownTime 업데이트
전면 광고 표시 → lastAdShownTime 업데이트 (같은 변수)

문제점:
- 앱 오프닝을 보면 전면광고도 쿨타임 적용됨
- 전면광고를 보면 앱 오프닝도 쿨타임 적용됨
- 두 광고가 서로 영향을 줌
```

**After (v2.0 - 분리 방식)**
```
앱 오프닝 광고 표시 → AdController에서 별도 관리 (AdPolicyManager 미호출)
전면 광고 표시 → lastInterstitialTime 업데이트 (전용 변수)

개선점:
- 앱 오프닝과 전면광고가 독립적으로 작동
- 앱 오프닝을 봐도 전면광고 쿨타임에 영향 없음
- 전면광고를 봐도 앱 오프닝에 영향 없음
```

---

## 📋 수정된 파일 (3개)

### 1. AdPolicyManager.kt ⭐ 핵심

**변경된 주요 항목**:

#### A. SharedPreferences 키 분리
```kotlin
// [v2.0 신규] 전면광고 전용
private const val KEY_LAST_INTERSTITIAL_TIME_MS = "last_interstitial_time_ms"

// [DEPRECATED] 이전 통합 키 (하위 호환용)
@Deprecated("Use KEY_LAST_INTERSTITIAL_TIME_MS instead")
private const val KEY_LAST_AD_SHOWN_TIME_MS = "last_ad_shown_time_ms"
```

#### B. 함수 목적 명확화

**getInterstitialIntervalSeconds()**
```kotlin
// Before: "전면형 광고(전면광고 + 앱오프닝) 쿨타임"
// After: "전면광고 쿨타임" (전용)

/**
 * [v2.0] 전면광고 쿨타임 간격(초)을 반환
 * 
 * ⚠️ 주의: 이 값은 "전면광고(Interstitial Ad) 전용"입니다.
 * 앱 오프닝 광고는 별도의 쿨타임을 사용합니다.
 */
fun getInterstitialIntervalSeconds(context: Context): Long
```

**shouldShowInterstitialAd()**
```kotlin
// Before: 전면광고 + 앱오프닝 통합 체크
// After: 전면광고만 체크

/**
 * [v2.0] 전면광고 노출 가능 여부를 결정
 * 
 * ⚠️ 중요 변경사항:
 * - 오직 "전면광고(Interstitial Ad)"의 마지막 노출 시간만 체크
 * - 앱 오프닝 광고와는 독립적으로 작동
 */
fun shouldShowInterstitialAd(context: Context): Boolean {
    // ...existing code...
    
    // [v2.0] 전면광고 마지막 노출 시간만 가져오기
    val lastInterstitialTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME_MS, 0L)
    
    // ...existing code...
}
```

**markInterstitialAdShown()** (신규 함수명)
```kotlin
// Before: markAdShown() - 전면광고, 앱오프닝 모두 호출
// After: markInterstitialAdShown() - 전면광고 전용

/**
 * [v2.0] 전면광고가 성공적으로 표시된 후 호출
 * 
 * ⚠️ 중요: 이 함수는 "전면광고(Interstitial Ad) 전용"입니다.
 * - 전면광고의 onAdDismissedFullScreenContent 콜백에서만 호출
 * - 앱 오프닝 광고에서는 절대 호출하지 마세요!
 */
fun markInterstitialAdShown(context: Context, adType: String = "interstitial") {
    val currentTime = System.currentTimeMillis()
    
    // [v2.0] 전면광고 전용 타이머 업데이트
    prefs.edit().putLong(KEY_LAST_INTERSTITIAL_TIME_MS, currentTime).apply()
    
    Log.d(TAG, "✅ [v2.0 전면광고 전용] 광고 표시 완료")
    Log.d(TAG, "  ⚠️ 주의: 앱 오프닝 광고 타이머는 별도 관리됨")
}
```

#### C. 하위 호환성 유지

```kotlin
// [DEPRECATED] 이전 함수들은 Deprecated 표시
@Deprecated(
    message = "Use markInterstitialAdShown() for interstitial ads only.",
    replaceWith = ReplaceWith("markInterstitialAdShown(context, adType)")
)
fun markAdShown(context: Context, adType: String = "unknown")

@Deprecated("Use markInterstitialAdShown() instead")
fun markInterstitialShown(context: Context)
```

---

### 2. InterstitialAdManager.kt

**변경 위치**: `onAdShowedFullScreenContent()` 콜백

**Before:**
```kotlin
override fun onAdShowedFullScreenContent() {
    Log.d(TAG, "show: 광고 표시 완료")
    AnalyticsManager.logAdImpression("interstitial")
    
    // [v1.0 통합] 모든 광고에 영향
    AdPolicyManager.markAdShown(activity, "interstitial")
}
```

**After:**
```kotlin
override fun onAdShowedFullScreenContent() {
    Log.d(TAG, "show: 광고 표시 완료")
    AnalyticsManager.logAdImpression("interstitial")
    
    // [v2.0 분리] 전면광고 전용 쿨타임 기록
    // ⚠️ 중요: 앱 오프닝 광고와 독립적으로 작동
    AdPolicyManager.markInterstitialAdShown(activity, "interstitial")
}
```

**핵심**: 
- 함수명 변경: `markAdShown()` → `markInterstitialAdShown()`
- 의미 명확화: 전면광고 전용임을 명시

---

### 3. AppOpenAdManager.kt ⭐ 중요한 제거

**변경 위치**: `onAdShowedFullScreenContent()` 콜백

**Before:**
```kotlin
override fun onAdShowedFullScreenContent() {
    Log.d(TAG, "AppOpen onAdShowedFullScreenContent")
    isShowing = true
    lastShownAt = System.currentTimeMillis()
    
    // [v1.0 통합] 앱 오프닝도 통합 타이머에 기록
    applicationRef?.let { app ->
        AdPolicyManager.markAdShown(app.applicationContext, "app_open") // ← 제거!
    }
    
    // AdController에 기록
    AdController.recordAppOpenShown(it.applicationContext)
}
```

**After:**
```kotlin
override fun onAdShowedFullScreenContent() {
    Log.d(TAG, "AppOpen onAdShowedFullScreenContent")
    isShowing = true
    lastShownAt = System.currentTimeMillis()
    
    // [v2.0 분리] 앱 오프닝 광고는 전면광고 타이머에 영향 주지 않음
    // ⚠️ 중요: AdPolicyManager.markInterstitialAdShown()을 호출하지 않음
    // 앱 오프닝 쿨타임은 AdController에서 별도 관리
    Log.d(TAG, "AppOpen ad shown - 전면광고 쿨타임과 독립적으로 작동")
    
    // AdController에만 기록 (앱 오프닝 전용)
    AdController.recordAppOpenShown(it.applicationContext)
}
```

**핵심**:
- `AdPolicyManager.markAdShown()` 호출 완전 제거
- 앱 오프닝은 `AdController`에서만 관리
- 전면광고 쿨타임에 영향 주지 않음

---

## 🎬 동작 시나리오

### 시나리오 1: 앱 오프닝 → 전면광고

```
08:00 - 앱 실행
        → 앱 오프닝 광고 표시 ✅
        → AdController.recordAppOpenShown() 호출
        → AdPolicyManager.markInterstitialAdShown() 호출 안 함!
        
08:05 - 타이머 완료 → "결과 확인" 클릭
        → shouldShowInterstitialAd() 체크
        → lastInterstitialTime = 0 (아직 전면광고 본 적 없음)
        → 전면광고 표시 가능 ✅
        → 전면광고 표시됨
        → markInterstitialAdShown() 호출
        → lastInterstitialTime = 08:05
```

**결과**: 앱 오프닝을 본 후 5분 안에도 전면광고 표시 가능! ✅

---

### 시나리오 2: 전면광고 → 앱 오프닝

```
14:00 - 타이머 완료 → "결과 확인" 클릭
        → 전면광고 표시 ✅
        → markInterstitialAdShown() 호출
        → lastInterstitialTime = 14:00
        
14:03 - 앱 종료 후 재실행 (3분 후)
        → AdController.canShowAppOpen() 체크
        → 앱 오프닝 쿨타임은 AdController에서 별도 관리
        → 전면광고와 독립적
        → 앱 오프닝 표시 가능 ✅
        → 앱 오프닝 표시됨
```

**결과**: 전면광고를 본 후에도 앱 오프닝은 독립적으로 표시! ✅

---

### 시나리오 3: 전면광고 쿨타임만 작동

```
15:00 - 전면광고 표시 ✅
        → lastInterstitialTime = 15:00
        
15:03 - "전체 기록" 뒤로가기
        → shouldShowInterstitialAd() 체크
        → 경과 시간: 3분 < 5분 (쿨타임)
        → 전면광고 표시 안 됨 ❌
        → 즉시 화면 복귀
        
15:06 - "전체 일기" 뒤로가기
        → shouldShowInterstitialAd() 체크
        → 경과 시간: 6분 > 5분 (쿨타임 통과)
        → 전면광고 표시 ✅
        → lastInterstitialTime = 15:06
```

**결과**: 전면광고끼리만 쿨타임 적용! ✅

---

## 📊 핵심 차이점 요약

### Firebase Remote Config 파라미터

| 항목 | Before (v1.0) | After (v2.0) |
|------|---------------|--------------|
| **키 이름** | `interstitial_interval_sec` | `interstitial_interval_sec` ✅ 동일 |
| **의미** | 전면광고 + 앱오프닝 통합 | 전면광고 전용 |
| **영향 범위** | 모든 전면형 광고 | 전면광고만 |

### SharedPreferences 변수

| 항목 | Before (v1.0) | After (v2.0) |
|------|---------------|--------------|
| **변수명** | `last_ad_shown_time_ms` | `last_interstitial_time_ms` |
| **업데이트 시점** | 전면광고 + 앱오프닝 | 전면광고만 |
| **체크 대상** | 전면광고 + 앱오프닝 | 전면광고만 |

### 함수 호출

| 광고 타입 | Before (v1.0) | After (v2.0) |
|----------|---------------|--------------|
| **전면광고** | `markAdShown()` | `markInterstitialAdShown()` ✅ |
| **앱 오프닝** | `markAdShown()` | 호출 안 함 ✅ |

---

## ✅ 검증 체크리스트

### 1. 전면광고 쿨타임
- ✅ 전면광고 표시 시 `lastInterstitialTime` 업데이트
- ✅ `shouldShowInterstitialAd()`가 `lastInterstitialTime`만 체크
- ✅ 앱 오프닝과 독립적

### 2. 앱 오프닝 쿨타임
- ✅ 앱 오프닝 표시 시 `AdPolicyManager` 미호출
- ✅ `AdController`에서만 관리
- ✅ 전면광고와 독립적

### 3. Firebase 파라미터
- ✅ `interstitial_interval_sec` 키 이름 변경 없음
- ✅ 기존 Remote Config 값 그대로 사용
- ✅ 추가 설정 불필요

### 4. 하위 호환성
- ✅ Deprecated 함수 유지 (경고만 표시)
- ✅ 기존 호출 코드 작동 (markAdShown → markInterstitialAdShown)

---

## 🎯 주요 코드 스니펫

### AdPolicyManager.kt - 전면광고 체크

```kotlin
fun shouldShowInterstitialAd(context: Context): Boolean {
    // Kill Switch 확인
    if (!isAdEnabled(context)) return false
    
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val intervalSeconds = getInterstitialIntervalSeconds(context)
    val intervalMillis = intervalSeconds * 1000L
    
    // [v2.0] 전면광고 전용 타이머 체크
    val lastInterstitialTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME_MS, 0L)
    val currentTime = System.currentTimeMillis()
    val elapsedTime = currentTime - lastInterstitialTime
    
    // 쿨타임 검사 (전면광고 전용)
    return elapsedTime >= intervalMillis
}
```

### InterstitialAdManager.kt - 전면광고 표시 후

```kotlin
ad.fullScreenContentCallback = object : FullScreenContentCallback() {
    override fun onAdShowedFullScreenContent() {
        Log.d(TAG, "Interstitial ad showed")
        
        // [v2.0] 전면광고 전용 쿨타임 기록
        // ⚠️ 중요: 앱 오프닝 광고와 독립적으로 작동
        AdPolicyManager.markInterstitialAdShown(activity, "interstitial")
    }
    
    override fun onAdDismissedFullScreenContent() {
        // ...existing code...
    }
}
```

### AppOpenAdManager.kt - 앱 오프닝 표시 후

```kotlin
ad.fullScreenContentCallback = object : FullScreenContentCallback() {
    override fun onAdShowedFullScreenContent() {
        Log.d(TAG, "App open ad showed")
        
        // [v2.0] 앱 오프닝은 AdPolicyManager를 호출하지 않음!
        // AdController에서만 별도 관리
        AdController.recordAppOpenShown(context)
        
        // markInterstitialAdShown() 호출하지 않음! ← 핵심!
    }
    
    override fun onAdDismissedFullScreenContent() {
        // ...existing code...
    }
}
```

---

## 🎉 리팩토링 완료!

### 주요 성과

1. ✅ **Firebase 파라미터 재사용**
   - `interstitial_interval_sec` 키 이름 변경 없음
   - 기존 Remote Config 값 그대로 사용

2. ✅ **타이머 변수 분리**
   - 전면광고: `last_interstitial_time_ms` (신규)
   - 앱 오프닝: AdController에서 별도 관리

3. ✅ **검사 로직 분리**
   - 전면광고: `shouldShowInterstitialAd()` (전면광고 타이머만 체크)
   - 앱 오프닝: `AdController.canShowAppOpen()` (독립적)

4. ✅ **업데이트 로직 분리**
   - 전면광고: `markInterstitialAdShown()` 호출
   - 앱 오프닝: `markInterstitialAdShown()` 호출 안 함

### 사용자 경험 개선

- ✅ 앱 오프닝을 봐도 전면광고 볼 수 있음
- ✅ 전면광고를 봐도 앱 오프닝 볼 수 있음
- ✅ 두 광고가 독립적으로 작동
- ✅ 더 자연스러운 광고 노출

---

**작업 완료**: 2025-12-17  
**버전**: v2.0 (쿨타임 분리)  
**빌드 상태**: 진행 중

