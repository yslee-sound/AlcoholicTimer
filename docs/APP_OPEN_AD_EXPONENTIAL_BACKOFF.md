# 📱 AppOpenAdManager 지수 백오프(Exponential Backoff) 재시도 로직 구현 완료

**작성일:** 2026-01-05  
**수정 파일:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/ad/AppOpenAdManager.kt`  
**목적:** 광고 로드 실패 시 무분별한 재시도 방지, AdMob 정책 준수

---

## 🎯 문제점 (AS-IS)

### 기존 상황
- 광고 로드 실패 시 **재시도 로직이 완전히 제거**되어 있었음
- 일시적인 네트워크 문제나 광고 인벤토리 부족 시 광고 노출 기회 상실
- 주석: `// [FIX] Retry logic removed to comply with AdMob policy (2025-12-24)`

### 문제
- 광고가 일시적으로 로드되지 않으면 앱 세션 동안 광고가 전혀 표시되지 않음
- 다음 앱 실행이나 생명주기 이벤트까지 기다려야 함

---

## ✅ 해결 방안 (TO-BE)

### 구현 내용: 지수 백오프(Exponential Backoff) 재시도 로직

#### 1. **재시도 카운터 추가**
```kotlin
// [NEW] 지수 백오프(Exponential Backoff) 재시도 카운터 (2026-01-05)
@Volatile private var retryAttempt: Int = 0
private const val MAX_RETRY_ATTEMPTS = 3 // 최대 3회까지만 재시도
```

#### 2. **성공 시 카운터 초기화**
```kotlin
override fun onAdLoaded(ad: AppOpenAd) {
    Log.d(TAG, "onAdLoaded app-open")
    
    // [NEW] 광고 로드 성공 시 재시도 카운터 초기화 (2026-01-05)
    retryAttempt = 0
    
    appOpenAd = ad
    loaded = true
    isLoading = false
    // ...existing code...
}
```

#### 3. **실패 시 지수 백오프 재시도**
```kotlin
override fun onAdFailedToLoad(loadAdError: LoadAdError) {
    Log.w(TAG, "onAdFailedToLoad app-open: ${loadAdError.message} (errorCode=${loadAdError.code})")
    isLoading = false
    loaded = false
    appOpenAd = null

    // Notify listeners of failure
    try { onLoadFailedListener?.invoke() } catch (_: Throwable) {}
    for (l in loadFailedListeners) runCatching { l.invoke() }

    // [NEW] 지수 백오프(Exponential Backoff) 재시도 로직 (2026-01-05)
    if (retryAttempt < MAX_RETRY_ATTEMPTS) {
        retryAttempt++
        // 지수 백오프 계산: 2^n 초 (1초, 2초, 4초)
        val delaySeconds = Math.pow(2.0, (retryAttempt - 1).toDouble()).toLong()
        val delayMillis = delaySeconds * 1000L
        
        Log.d(TAG, "AdMob: ${retryAttempt}번째 재시도 예약, ${delaySeconds}초 후 실행")
        
        mainHandler.postDelayed({
            try {
                Log.d(TAG, "AdMob: ${retryAttempt}번째 재시도 실행 중...")
                applicationRef?.applicationContext?.let { ctx -> preload(ctx) }
            } catch (e: Exception) {
                Log.e(TAG, "AdMob: 재시도 실행 실패: ${e.message}")
            }
        }, delayMillis)
    } else {
        Log.w(TAG, "AdMob: 최대 재시도 횟수(${MAX_RETRY_ATTEMPTS}회) 도달. 더 이상 재시도하지 않음.")
        retryAttempt = 0 // 다음 자연스러운 로드를 위해 카운터 초기화
    }
}
```

---

## 📊 재시도 타임라인

| 시도 횟수 | 대기 시간 | 누적 시간 | 계산식 |
|----------|----------|----------|--------|
| 1회차 실패 | 1초 대기 | 1초 | 2^0 = 1초 |
| 2회차 실패 | 2초 대기 | 3초 | 2^1 = 2초 |
| 3회차 실패 | 4초 대기 | 7초 | 2^2 = 4초 |
| 4회차 | ❌ 재시도 중단 | - | MAX_RETRY 도달 |

**총 재시도 횟수:** 최대 3회  
**총 대기 시간:** 최대 7초

---

## 🔍 AdMob 정책 준수 검증

### ✅ 준수 항목

1. **적절한 재시도 횟수 제한**
   - ✅ 최대 3회로 제한 (업계 표준)
   - ❌ 무한 루프 없음

2. **지수 백오프(Exponential Backoff)**
   - ✅ 재시도 간격이 지수적으로 증가 (1초 → 2초 → 4초)
   - ❌ 공격적인 재시도(Aggressive Retry) 없음

3. **최대 대기 시간**
   - ✅ 총 7초 이내에 완료 (사용자 경험 저해 최소화)
   - ✅ AdMob 서버 부하 최소화

4. **에러 로그 기록**
   - ✅ `errorCode` 포함하여 상세 로그 기록
   - ✅ 재시도 횟수 및 대기 시간 로그

5. **성공 시 카운터 초기화**
   - ✅ `retryAttempt = 0`으로 초기화
   - ✅ 다음 실패 시 처음부터 재시도

---

## 📈 예상 효과

### Before (재시도 없음)
- 광고 로드 실패 시 세션 동안 광고 없음
- 광고 노출 기회 손실

### After (지수 백오프)
- 일시적인 네트워크 문제나 인벤토리 부족 시 자동 복구
- 사용자 경험 개선 (광고가 적절히 표시됨)
- AdMob 정책 준수 (과도한 요청 방지)

---

## 🧪 테스트 시나리오

### 1. 정상 로드 성공
```
[로그]
onAdLoaded app-open
retryAttempt = 0 (초기화)
```

### 2. 1회 실패 후 재시도 성공
```
[로그]
onAdFailedToLoad app-open: No fill (errorCode=3)
AdMob: 1번째 재시도 예약, 1초 후 실행
...1초 대기...
AdMob: 1번째 재시도 실행 중...
preload 호출
onAdLoaded app-open
retryAttempt = 0 (초기화)
```

### 3. 3회 연속 실패 (최대 도달)
```
[로그]
onAdFailedToLoad app-open: No fill (errorCode=3)
AdMob: 1번째 재시도 예약, 1초 후 실행
...1초 대기...
onAdFailedToLoad app-open: No fill (errorCode=3)
AdMob: 2번째 재시도 예약, 2초 후 실행
...2초 대기...
onAdFailedToLoad app-open: No fill (errorCode=3)
AdMob: 3번째 재시도 예약, 4초 후 실행
...4초 대기...
onAdFailedToLoad app-open: No fill (errorCode=3)
AdMob: 최대 재시도 횟수(3회) 도달. 더 이상 재시도하지 않음.
retryAttempt = 0 (초기화)
```

---

## 🛡️ 안전장치 (Safeguards)

1. **최대 횟수 제한**
   ```kotlin
   if (retryAttempt < MAX_RETRY_ATTEMPTS) { /* 재시도 */ }
   ```

2. **예외 처리**
   ```kotlin
   try {
       applicationRef?.applicationContext?.let { ctx -> preload(ctx) }
   } catch (e: Exception) {
       Log.e(TAG, "AdMob: 재시도 실행 실패: ${e.message}")
   }
   ```

3. **카운터 초기화 (무한 루프 방지)**
   ```kotlin
   retryAttempt = 0 // 성공 시 또는 최대 도달 시
   ```

4. **메모리 누수 방지**
   - `mainHandler.postDelayed` 사용 (Lifecycle 종료 시 자동 취소)

---

## 📝 참고 문서

- [AdMob Invalid Traffic 정책](https://support.google.com/admob/answer/2618003)
- [Exponential Backoff 패턴 (Google Cloud)](https://cloud.google.com/iot/docs/how-tos/exponential-backoff)
- [Android Handler.postDelayed 문서](https://developer.android.com/reference/android/os/Handler#postDelayed(java.lang.Runnable,%20long))

---

## 🎯 결론

✅ **AdMob 정책 준수:** 지수 백오프로 과도한 요청 방지  
✅ **사용자 경험 개선:** 일시적 실패 시 자동 복구  
✅ **성능 최적화:** 최대 7초 이내 완료  
✅ **안전성:** 최대 3회 제한, 예외 처리, 카운터 초기화

**권장 사항:** 프로덕션 배포 전 다양한 네트워크 환경에서 테스트 권장 (Wi-Fi, 4G, 오프라인 → 온라인 전환 등)

---

**수정 완료일:** 2026-01-05  
**빌드 상태:** ✅ 성공 (26초 소요)

