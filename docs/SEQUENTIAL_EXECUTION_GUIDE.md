# 앱 오프닝 광고 순차적 실행 가이드 (Sequential Execution Guide)

## 📌 문제 상황

기존 MainActivity에서 **UMP 동의 확인**과 **앱 오프닝 광고 로직**이 비동기로 섞여 실행되면서, 광고가 표시되기 전에 메인 화면으로 넘어가는 문제가 발생했습니다.

### 기존 구조의 문제점

```kotlin
// ❌ 문제가 있는 구조
override fun onCreate() {
    // 타임아웃 시작 (4초)
    postDelayed(timeoutRunnable, 4000)
    
    // UMP 비동기 시작
    umpConsentManager.gatherConsent { canInitialize ->
        // 광고 SDK 초기화 (언제 끝날지 모름)
        MobileAds.initialize()
        
        // 광고 로드 시작 (언제 끝날지 모름)
        AppOpenAdManager.preload()
    }
    
    // ❌ 즉시 UI 렌더링 시작 (UMP/광고 완료를 기다리지 않음)
    setContent { ... }
}
```

**결과**: 타임아웃이 먼저 발동하거나, UI가 먼저 렌더링되어 스플래시가 조기 해제됨

---

## ✅ 해결: 순차적 실행 구조 (Sequential Execution)

### 핵심 원칙

**반드시 이전 단계가 완료된 후에만 다음 단계를 실행합니다.**

```
1단계: UMP 동의 확인
  ↓ (콜백 완료 대기)
2단계: 광고 SDK 초기화 및 광고 로드
  ↓ (광고 로드 완료 대기)
3단계: 광고 표시
  ↓ (사용자가 광고 닫기 대기)
4단계: 메인 액티비티 진입
```

### 구현 코드

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 스플래시 설정
    val holdSplashState = mutableStateOf(true)
    val splash = installSplashScreen()
    splash.setKeepOnScreenCondition { holdSplashState.value }
    
    // ============================================================
    // 4단계: 메인 진입 함수 (광고 완료 또는 실패 시만 호출)
    // ============================================================
    val proceedToMainActivity: () -> Unit = {
        runOnUiThread {
            Log.d("MainActivity", "메인 액티비티 진입")
            holdSplashState.value = false
            setContent { AppContent() }
        }
    }
    
    // ============================================================
    // 안전 타임아웃 (4초) - 광고 로딩 중이면 자동 연장
    // ============================================================
    var timeoutRunnable: Runnable? = null
    timeoutRunnable = Runnable {
        val isLoading = AppOpenAdManager.isLoading()
        val isShowing = AppOpenAdManager.isShowingAd()
        
        if (isLoading || isShowing) {
            // 광고 로딩/표시 중 - 타임아웃 연장
            Log.d("MainActivity", "Timeout deferred - ad is loading/showing")
            window.decorView.postDelayed(timeoutRunnable!!, 1000)
        } else {
            // 광고 없음 - 타임아웃 발동
            Log.w("MainActivity", "Timeout fired -> proceed to main")
            proceedToMainActivity()
        }
    }
    window.decorView.postDelayed(timeoutRunnable, 4000)
    
    // ============================================================
    // 1단계: UMP 동의 확인 시작
    // ============================================================
    Log.d("MainActivity", "단계 1: UMP 동의 확인 시작")
    
    val umpConsentManager = (application as MainApplication).umpConsentManager
    umpConsentManager.gatherConsent(this) { canInitializeAds ->
        Log.d("MainActivity", "단계 1 완료: canInitializeAds=$canInitializeAds")
        
        if (!canInitializeAds) {
            // 동의 없음 - 즉시 메인으로
            Log.w("MainActivity", "No consent -> proceed to main")
            proceedToMainActivity()
            return@gatherConsent
        }
        
        // ============================================================
        // 2단계: 광고 SDK 초기화 및 광고 로드
        // ============================================================
        Log.d("MainActivity", "단계 2: 광고 SDK 초기화 및 로드")
        
        try {
            // SDK 초기화
            MobileAds.initialize(this) {
                Log.d("MainActivity", "MobileAds initialized")
            }
            
            // 광고 로드 완료 리스너
            AppOpenAdManager.setOnAdLoadedListener {
                runOnUiThread {
                    Log.d("MainActivity", "단계 3: 광고 로드 완료 -> 표시 시도")
                    
                    if (!AppOpenAdManager.isLoaded()) {
                        Log.w("MainActivity", "Ad not loaded -> proceed to main")
                        proceedToMainActivity()
                        return@runOnUiThread
                    }
                    
                    // ============================================================
                    // 3단계: 광고 표시
                    // ============================================================
                    val shown = AppOpenAdManager.showIfAvailable(
                        this,
                        bypassRecentFullscreenSuppression = true
                    )
                    Log.d("MainActivity", "광고 표시 결과: shown=$shown")
                    
                    if (!shown) {
                        // 광고 표시 실패 - 메인으로
                        Log.w("MainActivity", "Ad failed to show -> proceed to main")
                        proceedToMainActivity()
                    }
                    // 광고 표시 성공 - onAdDismissed에서 메인으로 이동
                }
            }
            
            // 광고 로드 실패 리스너
            AppOpenAdManager.setOnAdLoadFailedListener {
                runOnUiThread {
                    Log.w("MainActivity", "Ad load failed -> proceed to main")
                    proceedToMainActivity()
                }
            }
            
            // 광고 닫힘 리스너 (사용자가 광고를 닫았을 때)
            AppOpenAdManager.setOnAdFinishedListener {
                runOnUiThread {
                    Log.d("MainActivity", "단계 4: 광고 닫힘 -> 메인 진입")
                    timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
                    proceedToMainActivity()
                }
            }
            
            // 광고 표시 시작 리스너 (타임아웃 취소)
            AppOpenAdManager.setOnAdShownListener {
                runOnUiThread {
                    Log.d("MainActivity", "광고 표시 시작 -> 타임아웃 취소")
                    timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
                }
            }
            
            // 광고 로드 시작
            Log.d("MainActivity", "광고 로드 시작")
            AppOpenAdManager.preload(this)
            
        } catch (t: Throwable) {
            Log.e("MainActivity", "광고 설정 실패", t)
            proceedToMainActivity()
        }
    }
    
    // ❌ 여기서 setContent()를 호출하면 안 됨!
    // ✅ proceedToMainActivity() 함수 내부에서만 호출
}
```

---

## 🔑 핵심 포인트

### 1. `proceedToMainActivity()` 함수

**이 함수만이 메인 화면으로 진입하는 유일한 경로입니다.**

호출 시점:
- ✅ UMP 동의 거부 시
- ✅ 광고 로드 실패 시
- ✅ 광고 표시 실패 시
- ✅ **사용자가 광고를 닫았을 때** (가장 중요!)
- ✅ 타임아웃 발동 시

❌ **절대 호출하면 안 되는 곳**:
- `onCreate()` 끝
- 타이머로 강제 호출

### 2. **🚨 무한 중첩 방지 (Critical!)**

#### 문제 1: 리스너 중복 실행

**문제**: `setOnAdLoadedListener`는 광고가 로드될 때마다 실행되므로, 백그라운드 복귀 시 광고가 다시 로드되면 리스너가 또 실행되어 **무한 중첩**이 발생합니다!

**해결책**: 플래그를 사용하여 **첫 광고 로드 시에만 실행**:

```kotlin
// 플래그 선언
@Volatile
private var hasHandledInitialAdLoad: Boolean = false

// 리스너 설정
AppOpenAdManager.setOnAdLoadedListener {
    runOnUiThread {
        // [중요] 첫 광고 로드 시에만 실행
        if (hasHandledInitialAdLoad) {
            Log.d("MainActivity", "광고 로드 완료 (이미 처리됨) - 스킵")
            return@runOnUiThread
        }
        hasHandledInitialAdLoad = true
        
        // 광고 표시 로직
        val shown = AppOpenAdManager.showIfAvailable(this)
        // ...
    }
}
```

#### 문제 2: 자동 표시와 수동 표시의 중첩

**문제**: `ProcessLifecycleOwner`의 **자동 표시**와 `setOnAdLoadedListener`의 **수동 표시**가 동시에 실행되어 광고가 **2개 중첩**됩니다!

**해결책**: `onCreate()` 시작 시 **자동 표시를 완전히 비활성화**:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // [중요] 자동 표시 비활성화 (수동으로 제어)
    AppOpenAdManager.setAutoShowEnabled(false)
    Log.d("MainActivity", "AppOpen auto-show DISABLED")
    
    // ... 나머지 로직
}
```

**최종 메인 진입 시 자동 표시 재활성화** (백그라운드 복귀 시 자동 표시):

```kotlin
val proceedToMainActivity = {
    // 메인 진입 후 자동 표시 재활성화
    AppOpenAdManager.setAutoShowEnabled(true)
    setContent { ... }
}
```

**효과**: 
- ✅ **앱 최초 실행 시**: 자동 표시 OFF → 수동 표시만 실행 → **광고 1개만 표시**
- ✅ **백그라운드 복귀 시**: 자동 표시 ON → `ProcessLifecycleOwner`가 자동 처리 → **광고 1개만 표시**
- ✅ **리스너 중복 실행 방지**: `hasHandledInitialAdLoad` 플래그로 첫 로드만 처리
- ✅ **중첩 완전 차단**: 자동 표시와 수동 표시가 절대 동시에 실행되지 않음

### 3. `setContent()` 호출 위치

```kotlin
// ❌ 잘못된 위치
override fun onCreate() {
    umpConsentManager.gatherConsent { ... }
    setContent { ... }  // 콜백 밖 - 즉시 실행됨!
}

// ✅ 올바른 위치
val proceedToMainActivity = {
    setContent { ... }  // 오직 여기서만!
}
```

### 4. 타임아웃 로직

```kotlin
// 광고 로딩/표시 중이면 무한 연장
if (isLoading || isShowing) {
    postDelayed(timeoutRunnable, 1000)  // 1초 후 재확인
} else {
    proceedToMainActivity()  // 타임아웃 발동
}
```

---

## 📊 실행 흐름 다이어그램

```
앱 시작
  ↓
onCreate() 진입
  ↓
스플래시 설정 (holdSplashState = true)
  ↓
타임아웃 4초 시작
  ↓
════════════════════════════════════════
1단계: UMP 동의 확인
════════════════════════════════════════
  ↓
gatherConsent() 시작
  ↓
⏱️ 대기 중... (비동기)
  ↓
✅ 콜백 완료: canInitializeAds=true
  ↓
════════════════════════════════════════
2단계: 광고 SDK 초기화 및 로드
════════════════════════════════════════
  ↓
MobileAds.initialize()
  ↓
AppOpenAdManager.preload() 시작
  ↓
⏱️ 대기 중... (광고 로드)
  ↓
✅ onAdLoaded 콜백
  ↓
════════════════════════════════════════
3단계: 광고 표시
════════════════════════════════════════
  ↓
AppOpenAdManager.showIfAvailable()
  ↓
✅ onAdShown 콜백 → 타임아웃 취소
  ↓
광고 표시 중...
  ↓
사용자가 광고 닫음 (X 버튼)
  ↓
✅ onAdDismissed 콜백
  ↓
════════════════════════════════════════
4단계: 메인 액티비티 진입
════════════════════════════════════════
  ↓
proceedToMainActivity() 호출
  ↓
holdSplashState = false
  ↓
setContent { AppContent() }
  ↓
메인 화면 표시 완료 ✅
```

---

## 🚨 AdMob 정책 준수 체크리스트

- [x] 광고가 **완전히 로드된 후**에만 표시
- [x] 광고 표시 중에는 **다른 화면으로 전환 금지**
- [x] 사용자가 **광고를 닫을 때까지** 대기
- [x] 광고 로드/표시 실패 시 **무한 대기 방지** (타임아웃)
- [x] 테스트 기기에서만 테스트 광고 표시

---

## 📝 로그 확인

정상 작동 시 예상 로그:

```
MainActivity: 단계 1: UMP 동의 확인 시작
MainActivity: 단계 1 완료: canInitializeAds=true
MainActivity: 단계 2: 광고 SDK 초기화 및 로드
MainActivity: MobileAds initialized
MainActivity: 광고 로드 시작
AppOpenAdManager: onAdLoaded app-open
MainActivity: 단계 3: 광고 로드 완료 -> 표시 시도
MainActivity: 광고 표시 결과: shown=true
AppOpenAdManager: onAdShowedFullScreenContent
MainActivity: 광고 표시 시작 -> 타임아웃 취소
[사용자가 광고 닫음]
AppOpenAdManager: onAdDismissedFullScreenContent
MainActivity: 단계 4: 광고 닫힘 -> 메인 진입
MainActivity: 메인 액티비티 진입
```

---

## 문서 버전

- **작성일**: 2025-12-02
- **적용 버전**: v1.0.0+
- **업데이트**: 순차적 실행 구조로 전면 재작성

