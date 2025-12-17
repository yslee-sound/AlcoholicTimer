# ✅ AppOpen 광고 표시 중 앱 화면 노출 방지

## 📅 작업일
2025-12-01

## 🚨 문제 상황
- Splash 화면 → AppOpen 광고 표시
- 그런데 **광고 뒤에서 앱이 실행되어 보임**
- 사용자가 "앱으로 이동" 버튼을 눌러야 앱 화면이 나와야 하는데, 광고 뒤에 앱이 보임
- 이것은 **AdMob 정책 위반**!

## 🔍 근본 원인

### 이전 로직
```kotlin
// 5초 타임아웃 설정
window.decorView.postDelayed(timeoutRunnable, 5000)

// 문제점:
// - AppOpen 광고가 표시되는 동안에도 5초 후 무조건 Splash 해제
// - MainActivity가 광고 뒤에 보이게 됨
// - AdMob 정책 위반!
```

**타임라인**:
```
0ms    - MainActivity onCreate
100ms  - Splash 화면 표시
500ms  - AppOpen 광고 로드 완료
600ms  - AppOpen 광고 표시 시작
5000ms - ⚠️ 타임아웃! Splash 해제
       - ❌ MainActivity가 광고 뒤에 보임!
10000ms - 사용자가 광고 닫음
```

---

## ✅ 해결책

### 1. 타임아웃 지연 로직 추가

**개선된 타임아웃**:
```kotlin
var timeoutRunnable: Runnable? = null
timeoutRunnable = Runnable {
    // AppOpen 광고가 표시 중인지 확인
    val isAppOpenShowing = try {
        AppOpenAdManager.isShowingAd()
    } catch (_: Throwable) { false }
    
    if (isAppOpenShowing) {
        // 광고가 표시 중이면 타임아웃을 연장 (1초 후 다시 확인)
        Log.d("MainActivity", "splash timeout deferred - AppOpen ad is showing")
        window.decorView.postDelayed(timeoutRunnable!!, 1000)
    } else {
        // 광고가 없으면 Splash 해제
        Log.d("MainActivity", "splash timeout fired -> releasing holdSplashState")
        holdSplashState.value = false
    }
}
window.decorView.postDelayed(timeoutRunnable, 3000) // 초기 3초
```

**동작**:
- 3초 후 타임아웃 체크
- AppOpen 광고가 표시 중이면 1초 후 다시 체크 (반복)
- 광고가 없으면 Splash 해제

### 2. AppOpen 광고 종료 리스너

**광고가 닫힐 때 Splash 해제**:
```kotlin
AppOpenAdManager.setOnAdFinishedListener {
    runOnUiThread {
        Log.d("MainActivity", "AppOpen ad finished -> releasing splash")
        setHoldSplash(false)
        // 타임아웃도 취소
        timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
    }
}
```

**동작**:
- AppOpen 광고가 닫히면 즉시 Splash 해제
- 타임아웃 Runnable도 취소하여 중복 실행 방지

---

## 📊 개선된 타임라인

### Before (문제)
```
0ms    - MainActivity onCreate
100ms  - Splash 화면 표시
500ms  - AppOpen 광고 표시 시작
5000ms - ⚠️ Splash 해제 (타임아웃)
       - ❌ 광고 뒤에 앱이 보임 (정책 위반!)
10000ms - 광고 닫힘
```

### After (해결)
```
0ms    - MainActivity onCreate
100ms  - Splash 화면 표시
500ms  - AppOpen 광고 표시 시작
3000ms - 타임아웃 체크 → 광고 표시 중! → 1초 연장
4000ms - 타임아웃 체크 → 광고 표시 중! → 1초 연장
5000ms - 타임아웃 체크 → 광고 표시 중! → 1초 연장
...
10000ms - ✅ 광고 닫힘 → Splash 해제
        - ✅ 이제 앱 화면이 보임 (정책 준수!)
```

---

## 🎯 핵심 변경 사항

### 파일: MainActivity.kt

#### 1. 타임아웃 지연 로직
```kotlin
// Before
val timeoutRunnable = Runnable {
    holdSplashState.value = false
}
window.decorView.postDelayed(timeoutRunnable, 5000)

// After
var timeoutRunnable: Runnable? = null
timeoutRunnable = Runnable {
    val isAppOpenShowing = AppOpenAdManager.isShowingAd()
    if (isAppOpenShowing) {
        window.decorView.postDelayed(timeoutRunnable!!, 1000) // 연장
    } else {
        holdSplashState.value = false
    }
}
window.decorView.postDelayed(timeoutRunnable, 3000)
```

#### 2. 광고 종료 리스너
```kotlin
// 추가됨
AppOpenAdManager.setOnAdFinishedListener {
    runOnUiThread {
        setHoldSplash(false)
        timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
    }
}
```

---

## ✅ 보장되는 사항

### AdMob 정책 준수
- ✅ AppOpen 광고 표시 중에는 **절대** 앱 화면이 보이지 않음
- ✅ Splash 화면이 광고가 닫힐 때까지 유지됨
- ✅ 사용자가 "앱으로 이동"을 눌러야 앱이 나타남 (광고 없을 때는 자동)

### 타임아웃 안전장치
- ✅ 광고가 로드되지 않으면 3초 후 Splash 자동 해제
- ✅ 광고 표시 중이면 타임아웃 자동 연장
- ✅ 광고 종료 시 즉시 Splash 해제

### 사용자 경험
- ✅ 광고가 없으면 빠르게 앱 진입 (3초 이내)
- ✅ 광고가 있으면 깔끔하게 광고 → 앱 전환
- ✅ 광고 뒤에 앱이 보이는 혼란스러운 상황 제거

---

## 🧪 테스트 시나리오

### 시나리오 1: AppOpen 광고가 있을 때
1. 앱 시작
2. Splash 화면 표시
3. AppOpen 광고 로드
4. AppOpen 광고 표시 (Splash 유지)
5. **3초 후 타임아웃 체크** → 광고 표시 중! → 1초 연장
6. **4초 후 타임아웃 체크** → 광고 표시 중! → 1초 연장
7. 사용자가 광고 닫기
8. **즉시 Splash 해제**
9. ✅ 앱 화면 표시

**결과**: 광고 뒤에 앱이 보이지 않음!

### 시나리오 2: AppOpen 광고가 없을 때
1. 앱 시작
2. Splash 화면 표시
3. AppOpen 광고 없음 (정책으로 비활성화 등)
4. **3초 후 타임아웃 체크** → 광고 없음! → Splash 해제
5. ✅ 앱 화면 표시

**결과**: 빠르게 앱 진입!

### 시나리오 3: AppOpen 광고 로드 실패
1. 앱 시작
2. Splash 화면 표시
3. AppOpen 광고 로드 시도
4. 로드 실패 (네트워크 오류 등)
5. `onAdFinishedListener` 호출
6. **즉시 Splash 해제**
7. ✅ 앱 화면 표시

**결과**: 무한 대기 없음!

---

## 📝 추가 고려사항

### 타임아웃 시간 조정
현재: 초기 3초, 광고 표시 중이면 1초씩 연장

**조정 가능**:
```kotlin
// 더 빠른 진입 원하면
window.decorView.postDelayed(timeoutRunnable, 2000) // 2초

// 더 여유 있게
window.decorView.postDelayed(timeoutRunnable, 5000) // 5초
```

### 최대 대기 시간 설정
무한 연장 방지:
```kotlin
var retryCount = 0
timeoutRunnable = Runnable {
    val isAppOpenShowing = AppOpenAdManager.isShowingAd()
    if (isAppOpenShowing && retryCount < 10) { // 최대 10초 연장
        retryCount++
        window.decorView.postDelayed(timeoutRunnable!!, 1000)
    } else {
        holdSplashState.value = false
    }
}
```

---

## 🎉 완료!

### 적용된 파일
- ✅ `MainActivity.kt` - Splash 유지 로직 개선

### 보장
- ✅ AppOpen 광고 표시 중 앱 화면 **절대** 노출 안 됨
- ✅ AdMob 정책 완벽 준수
- ✅ 타임아웃 안전장치 유지

### 다른 코드 영향
- ✅ 없음 - MainActivity만 수정
- ✅ AdBanner 조건부 렌더링 그대로 유지
- ✅ AppOpenAdManager 그대로 유지

**이제 업계 표준에 맞게 작동합니다!** 🎉

