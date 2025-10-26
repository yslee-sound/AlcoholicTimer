 # 구글 애드몹 앱 오프닝 광고 구현 프롬프트

## 📌 문서 정보

### 버전 이력
| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0.0 | 2025-01-26 | AI Assistant | 초기 작성 - AlcoholicTimer 앱 기반 |
| | | | - AppOpenAdManager 클래스 구현 |
| | | | - 콜드 스타트 보호 기능 |
| | | | - 일일 제한 및 쿨다운 정책 |
| 1.0.3 | 2025-10-26 | AI Assistant | 버그 수정 - 경쟁 조건(Race Condition) 해결 |
| | | | - Handler를 사용한 지연 광고 표시 (200ms) |
| | | | - 빠른 홈 버튼 전환 시 안정성 개선 |
| | | | - 예약된 광고 표시 취소 로직 추가 |
| 1.1.0 | 2025-10-26 | AI Assistant | 광고 표시 안정성 대폭 개선 |
| | | | - isAdFullyLoaded 플래그 추가 (완전 로드 추적) |
| | | | - Activity 안정화 시간 증가 (500ms) |
| | | | - 광고 표시 일관성 향상 (10번 중 2번 → 안정적) |
| | | | - 비정상 광고 즉시 재로드 (2초 대기 제거) |
| | | | - 디버그 토스트 최적화 (핵심 정보만 표시) |

### 문서 목적
이 문서는 **동일한 Base 구조를 사용하는 안드로이드 앱**에 구글 애드몹 앱 오프닝 광고를 적용하기 위한 완전한 가이드입니다.

### 적용 대상
- Android Kotlin 기반 앱
- Jetpack Compose 사용
- MobileAds SDK 이미 통합된 앱
- Application 클래스 존재하는 앱

### 주요 개선 사항 (v1.1.0)
- **광고 완전 로드 추적**: `isAdFullyLoaded` 플래그로 광고가 완전히 준비된 상태만 표시
- **Activity 안정화 대기**: 500ms 딜레이로 화면 전환 완료 후 광고 표시
- **일관성 향상**: 광고 로드 성공 시 안정적으로 표시 (이전: 10번 중 2번 → 개선: 거의 매번)
- **비정상 광고 처리**: 500ms 미만 광고는 즉시 재로드 (2초 대기 제거)
- **디버그 효율성**: 핵심 토스트만 표시 (광고 로드 완료, 표시 중, 오류)

---

## 🎯 프롬프트: 앱 오프닝 광고 구현

다음 지시사항에 따라 구글 애드몹 앱 오프닝 광고를 구현해주세요.

### 전제 조건
1. **기존 AdMob 설정 확인**
   - `com.google.android.gms:play-services-ads` 의존성이 있는지 확인
   - `MainApplication` 또는 `{ProjectName}Application` 클래스에서 `MobileAds.initialize()` 호출 여부 확인

2. **프로젝트 구조 파악**
   - `{packageName}/core/ads/` 디렉토리 존재 여부 확인
   - 없으면 생성 필요

---

## 📝 구현 단계

### STEP 1: 의존성 추가

**파일**: `app/build.gradle.kts`

**위치**: `dependencies` 블록 내부

**추가할 코드**:
```kotlin
// Lifecycle Process (앱 오프닝 광고용)
implementation("androidx.lifecycle:lifecycle-process:2.9.4")
```

**설명**: 
- ProcessLifecycleOwner를 사용하기 위한 라이브러리입니다.
- 앱의 포그라운드/백그라운드 전환을 감지합니다.

---

### STEP 2: BuildConfig 필드 추가

**파일**: `app/build.gradle.kts`

**위치**: `buildTypes` 블록 내부

#### Debug 빌드타입에 추가:
```kotlin
getByName("debug") {
    // ... 기존 코드 ...
    
    // 앱 오프닝 테스트 유닛ID (Google 제공 테스트 ID)
    buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
}
```

#### Release 빌드타입에 추가:
```kotlin
release {
    // ... 기존 코드 ...
    
    // 앱 오프닝 광고 유닛ID: 실제 배포용 유닛ID로 교체 필요
    buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-YOUR_PUB_ID/REPLACE_WITH_REAL_APP_OPEN\"")
}
```

**주의**: `YOUR_PUB_ID`와 `REPLACE_WITH_REAL_APP_OPEN`을 실제 AdMob 광고 유닛 ID로 교체하세요.

---

### STEP 3: AppOpenAdManager 클래스 생성

**파일 생성**: `app/src/main/java/{packageName}/core/ads/AppOpenAdManager.kt`

**전체 코드**:
```kotlin
package {packageName}.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import {packageName}.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App Open Ad Manager
 * 
 * 앱이 백그라운드에서 포그라운드로 전환될 때 전면 광고를 표시합니다.
 * - 콜드 스타트 시에는 표시하지 않음 (스플래시 화면과의 충돌 방지)
 * - 일일 노출 횟수 제한 및 쿨다운 적용
 * - v1.1.0: 광고 완전 로드 추적 및 안정성 개선
 */
class AppOpenAdManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var isAdFullyLoaded = false  // 광고가 완전히 로드되어 표시 가능한 상태
    
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    
    // 콜드 스타트 플래그 (앱 프로세스 시작 후 첫 foreground 전환)
    private var isColdStart = true
    
    // 포그라운드 전환 시 광고 표시를 한 번만 시도하기 위한 플래그
    private var shouldShowAdOnResume = false
    
    // Handler for delayed ad display
    private val handler = Handler(Looper.getMainLooper())
    private var adShowRunnable: Runnable? = null

    companion object {
        private const val TAG = "AppOpenAdManager"
        
        // Google 테스트 앱 오프닝 광고 ID
        private const val GOOGLE_TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        
        // 광고 유효 시간 (4시간)
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L
        
        // 정책 기본값 (필요에 따라 조정 가능)
        private const val DEFAULT_DAILY_CAP = 5  // 일일 최대 5회
        private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L  // 5분 쿨다운
        
        // Activity 안정화 대기 시간 (500ms)
        private const val AD_SHOW_DELAY_MS = 500L
        
        // SharedPreferences
        private const val PREFS = "ad_prefs"
        private const val KEY_LAST_SHOWN_MS = "app_open_last_shown_ms"
        private const val KEY_DAILY_COUNT = "app_open_daily_count"
        private const val KEY_DAILY_DAY = "app_open_daily_day"
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun currentUnitId(): String {
        val id: String = BuildConfig.ADMOB_APP_OPEN_UNIT_ID
        return if (id.isBlank() || id.contains("REPLACE_WITH_REAL_APP_OPEN")) {
            GOOGLE_TEST_APP_OPEN_ID
        } else {
            id
        }
    }

    /** 광고가 유효한지 확인 (4시간 이내 로드된 광고 + 완전 로드) */
    private fun isAdAvailable(): Boolean {
        val available = appOpenAd != null && wasLoadTimeLessThanNHoursAgo() && isAdFullyLoaded
        Log.d(TAG, "isAdAvailable: $available (ad=${appOpenAd != null}, timeValid=${wasLoadTimeLessThanNHoursAgo()}, fullyLoaded=$isAdFullyLoaded)")
        return available
    }

    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = Date().time - loadTime
        return dateDifference < AD_TIMEOUT_MS
    }

    private fun currentDayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun getPrefs() = application.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    private data class PolicyState(
        val dailyCount: Int,
        val dayKey: String,
        val lastShownMs: Long
    )

    private fun readPolicyState(): PolicyState {
        val sp = getPrefs()
        val day = sp.getString(KEY_DAILY_DAY, null)
        val today = currentDayKey()
        val count = if (day == today) sp.getInt(KEY_DAILY_COUNT, 0) else 0
        val lastMs = sp.getLong(KEY_LAST_SHOWN_MS, 0L)
        return PolicyState(count, today, lastMs)
    }

    private fun writePolicyState(update: PolicyState) {
        getPrefs().edit {
            putString(KEY_DAILY_DAY, update.dayKey)
            putInt(KEY_DAILY_COUNT, update.dailyCount)
            putLong(KEY_LAST_SHOWN_MS, update.lastShownMs)
        }
    }

    private fun passesPolicy(): Pair<Boolean, String?> {
        val state = readPolicyState()
        
        // 일일 노출 횟수 제한
        if (state.dailyCount >= DEFAULT_DAILY_CAP) {
            return false to "dailycap"
        }
        
        // 쿨다운 체크
        val now = System.currentTimeMillis()
        val since = now - state.lastShownMs
        if (state.lastShownMs > 0L && since < DEFAULT_COOLDOWN_MS) {
            return false to "cooldown"
        }
        
        return true to null
    }

    private fun recordShown() {
        val prev = readPolicyState()
        val newState = prev.copy(
            dailyCount = prev.dailyCount + 1,
            lastShownMs = System.currentTimeMillis()
        )
        writePolicyState(newState)
    }

    private fun isPolicyBypassed(): Boolean = BuildConfig.DEBUG

    /** 광고 로드 */
    fun loadAd() {
        // 이미 로딩 중이면 스킵
        if (isLoadingAd) {
            Log.d(TAG, "Skipping load: already loading")
            return
        }
        
        // 유효한 광고가 이미 있으면 스킵
        if (isAdAvailable()) {
            Log.d(TAG, "Skipping load: valid ad already available")
            return
        }

        isLoadingAd = true
        isAdFullyLoaded = false  // 로딩 시작하면 플래그 초기화
        val request = AdRequest.Builder().build()
        val unitId = currentUnitId()
        
        Log.d(TAG, "Loading app open ad with unitId=$unitId (debug=${BuildConfig.DEBUG})")
        
        AppOpenAd.load(
            application,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    isAdFullyLoaded = true  // 광고가 완전히 로드되어 표시 가능
                    Log.d(TAG, "App open ad loaded successfully at ${Date()} - Ready to show")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    isAdFullyLoaded = false
                    Log.w(TAG, "App open ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                }
            }
        )
    }

    /** 광고 표시 */
    private fun showAdIfAvailable(activity: Activity) {
        // 이미 광고 표시 중이면 스킵 (이중 체크)
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing")
            return
        }

        // 광고가 없으면 새로 로드 (이중 체크)
        if (!isAdAvailable()) {
            Log.d(TAG, "Ad is not available, loading new ad")
            loadAd()
            return
        }

        // 디버그 모드가 아닐 때만 정책 체크
        if (!isPolicyBypassed()) {
            val (pass, reason) = passesPolicy()
            if (!pass) {
                Log.d(TAG, "Blocked by policy: $reason")
                return
            }
        } else {
            Log.d(TAG, "Policy bypassed (debug): showing ad")
        }

        val ad = appOpenAd ?: return

        // 광고 표시 시작 시간 기록
        val adShowStartTime = System.currentTimeMillis()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                val showDelay = System.currentTimeMillis() - adShowStartTime
                isShowingAd = true
                Log.d(TAG, "App open ad showed full screen content (delay: ${showDelay}ms)")
                
                // 디버그 모드가 아닐 때만 노출 기록
                if (!isPolicyBypassed()) {
                    recordShown()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                val totalDisplayTime = System.currentTimeMillis() - adShowStartTime
                appOpenAd = null
                isAdFullyLoaded = false
                isShowingAd = false
                Log.d(TAG, "App open ad dismissed (total display time: ${totalDisplayTime}ms)")
                
                // 광고가 비정상적으로 빨리 닫혔는지 체크
                if (totalDisplayTime < 500) {
                    Log.w(TAG, "Ad dismissed too quickly (${totalDisplayTime}ms), marking ad as invalid")
                }
                
                // 즉시 다음 광고 로드
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isAdFullyLoaded = false
                isShowingAd = false
                Log.w(TAG, "App open ad failed to show: ${adError.message} (code: ${adError.code})")
                
                // 실패한 광고는 즉시 재로드
                loadAd()
            }
        }

        ad.show(activity)
    }

    /** 앱이 포그라운드로 전환될 때 호출 (DefaultLifecycleObserver) */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        
        Log.d(TAG, "ProcessLifecycle.onStart() - isColdStart=$isColdStart")
        
        // 이전에 예약된 광고 표시 작업 취소
        cancelPendingAdShow()
        
        // 콜드 스타트인 경우 플래그만 해제하고 광고를 표시하지 않음
        if (isColdStart) {
            Log.d(TAG, "First onStart after cold start - resetting flag, will NOT show ad")
            isColdStart = false
            shouldShowAdOnResume = false
            return
        }
        
        // 콜드 스타트가 아닌 경우, Activity가 Resume될 때 광고를 표시하도록 플래그 설정
        Log.d(TAG, "Not cold start - setting flag to show ad on next activity resume")
        shouldShowAdOnResume = true
    }
    
    /** 앱이 백그라운드로 전환될 때 호출 */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "ProcessLifecycle.onStop()")
        shouldShowAdOnResume = false
        cancelPendingAdShow()
    }
    
    /** 예약된 광고 표시 작업 취소 */
    private fun cancelPendingAdShow() {
        adShowRunnable?.let {
            handler.removeCallbacks(it)
            Log.d(TAG, "Cancelled pending ad show")
        }
        adShowRunnable = null
    }

    /** 콜드 스타트 플래그 리셋 (Application.onCreate에서 호출) */
    fun resetColdStart() {
        isColdStart = true
    }

    // ActivityLifecycleCallbacks 구현
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
            
            // 플래그가 있든 없든 무조건 체크
            if (shouldShowAdOnResume) {
                Log.d(TAG, "Activity resumed with shouldShowAdOnResume=true, checking ad availability")
                shouldShowAdOnResume = false
                
                // 광고 표시 전 상태 확인
                if (isShowingAd) {
                    Log.d(TAG, "Ad is already showing, skipping")
                    return
                }
                
                if (!isAdAvailable()) {
                    Log.d(TAG, "Ad is not available, loading new ad")
                    loadAd()
                    return
                }
                
                // Activity가 완전히 안정될 때까지 짧은 딜레이 후 광고 표시
                adShowRunnable = Runnable {
                    Log.d(TAG, "Delayed ad show triggered")
                    currentActivity?.let { act ->
                        if (!isShowingAd) {
                            showAdIfAvailable(act)
                        } else {
                            Log.d(TAG, "Ad already showing, skipping")
                        }
                    }
                    adShowRunnable = null
                }
                handler.postDelayed(adShowRunnable!!, AD_SHOW_DELAY_MS)
            } else {
                // 플래그가 없는 경우 - 정상적인 상황들:
                // 1. 콜드 스타트 직후 (이미 onStart에서 처리함)
                // 2. 앱 내부 네비게이션 (다른 Activity 복귀)
                // 3. 화면 회전 등
                Log.d(TAG, "Activity resumed but shouldShowAdOnResume=false (normal for cold start or internal navigation)")
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // 사용자가 빠르게 나간 경우 예약된 광고 표시 취소
        if (!isShowingAd) {
            cancelPendingAdShow()
        }
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
```

**주의**: `{packageName}`을 실제 패키지명으로 교체하세요.

---

### STEP 4: MainApplication 클래스 수정

**파일**: `app/src/main/java/{packageName}/MainApplication.kt` (또는 `{ProjectName}Application.kt`)

**추가할 코드**:

1. **import 문 추가**:
```kotlin
import {packageName}.core.ads.AppOpenAdManager
```

2. **클래스 레벨 변수 추가**:
```kotlin
class MainApplication : Application() {
    private lateinit var appOpenAdManager: AppOpenAdManager
    
    // ... 기존 코드 ...
}
```

3. **onCreate() 메서드 수정**:
```kotlin
override fun onCreate() {
    super.onCreate()
    
    // ... 기존 광고 설정 코드 ...
    
    MobileAds.initialize(this) { initStatus ->
        android.util.Log.d("MainApplication", "MobileAds initialized: $initStatus")
        
        // MobileAds 초기화 완료 후 앱 오프닝 광고 로드
        appOpenAdManager.loadAd()
    }
    
    // 앱 오프닝 광고 매니저 초기화
    appOpenAdManager = AppOpenAdManager(this)
    appOpenAdManager.resetColdStart()
    
    // ... 기존 코드 계속 ...
}
```

**중요**: `MobileAds.initialize()` 콜백 내부에 `appOpenAdManager.loadAd()` 호출을 추가하고, 그 아래에 매니저 초기화 코드를 배치하세요.

---

### STEP 5: Gradle 동기화

**실행 방법**:
- Android Studio: `File` → `Sync Project with Gradle Files`
- 터미널:
  ```bash
  ./gradlew --stop
  ./gradlew clean
  ```

---

### STEP 6: 테스트

#### 1. Debug 빌드로 기본 동작 테스트
1. 앱을 빌드하여 실행
2. **홈 버튼**을 눌러 백그라운드로 이동
3. **최근 앱 목록**에서 앱을 다시 선택
4. 테스트 광고가 표시되는지 확인

#### 2. 콜드 스타트 테스트
1. 앱을 완전히 종료 (최근 앱 목록에서 제거)
2. 앱 아이콘을 탭하여 재시작
3. 광고가 **표시되지 않아야** 함 ✅ (콜드 스타트 보호)
4. 다시 백그라운드 → 포그라운드 전환 시 광고 표시 확인

#### 3. Logcat 확인
```bash
adb logcat -s AppOpenAdManager
```

**예상 로그**:
- ✅ `Loading app open ad with unitId=...` - 광고 로드 시작
- ✅ `App open ad loaded successfully` - 로드 성공
- ⏭️ `Skipping ad on cold start` - 콜드 스타트 스킵
- 📺 `App open ad showed full screen content` - 광고 표시 성공
- 🔧 `Policy bypassed (debug): showing ad` - 디버그 모드

---

### STEP 7: Release 배포 준비

#### 1. AdMob 콘솔에서 광고 유닛 생성
1. [AdMob 콘솔](https://apps.admob.com/) 접속
2. **앱 선택** → **광고 단위** → **광고 단위 추가**
3. **앱 오프닝 광고** 선택
4. 광고 단위 이름 입력 (예: "Main App Open")
5. **광고 단위 생성** 클릭
6. **광고 유닛 ID 복사** (예: `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`)

#### 2. build.gradle.kts 수정
```kotlin
release {
    // ... 기존 코드 ...
    
    buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY\"")
    //                                                            ↑ 여기를 실제 광고 유닛 ID로 교체
}
```

#### 3. Release 빌드 테스트
- 실제 기기에서 Release 빌드로 테스트
- 정책 동작 확인 (일일 5회 제한, 5분 쿨다운)

---

## 🔧 커스터마이징

### 광고 정책 조정

**파일**: `AppOpenAdManager.kt`

**수정 가능한 상수**:
```kotlin
companion object {
    // 일일 최대 노출 횟수 (기본: 5)
    private const val DEFAULT_DAILY_CAP = 5
    
    // 쿨다운 시간 밀리초 (기본: 5분)
    private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L
    
    // 광고 유효 시간 밀리초 (기본: 4시간)
    private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L
    
    // Activity 안정화 대기 시간 (기본: 500ms)
    private const val AD_SHOW_DELAY_MS = 500L
}
```

**예시 - 더 빈번한 노출**:
```kotlin
private const val DEFAULT_DAILY_CAP = 10  // 하루 10회
private const val DEFAULT_COOLDOWN_MS = 2 * 60 * 1000L  // 2분 쿨다운
```

**예시 - 덜 빈번한 노출**:
```kotlin
private const val DEFAULT_DAILY_CAP = 3  // 하루 3회
private const val DEFAULT_COOLDOWN_MS = 10 * 60 * 1000L  // 10분 쿨다운
```

---

## 📊 동작 흐름

```
1. 앱 시작 (콜드 스타트)
   ↓
2. MainApplication.onCreate()
   ↓
3. MobileAds.initialize() → 완료 콜백
   ↓
4. appOpenAdManager.loadAd() 호출
   ↓
5. 광고 로드 시작 (백그라운드)
   ↓
6. 사용자가 앱 사용 중...
   ↓
7. 홈 버튼 → 백그라운드 이동
   ↓
8. 최근 앱 목록 → 앱 선택 (포그라운드 복귀)
   ↓
9. ProcessLifecycleOwner.onStart() 감지
   ↓
10. showAdIfAvailable() 호출
    ↓
11. 정책 체크
    - 콜드 스타트? NO (첫 실행 이후)
    - 일일 제한? NO (5회 미만)
    - 쿨다운? NO (5분 경과)
    ↓
12. 광고 표시 ✅
    ↓
13. 노출 기록 저장
    ↓
14. 다음 광고 자동 프리로드
```

---

## 🛠️ 문제 해결

### 문제 1: 광고가 표시되지 않음

**원인 및 해결책**:
1. **콜드 스타트**
   - 증상: 앱 최초 실행 시 광고 안 나옴
   - 해결: 정상 동작입니다. 백그라운드 → 포그라운드로 테스트하세요.

2. **정책 블록**
   - 증상: Logcat에 `Blocked by policy: dailycap` 또는 `cooldown`
   - 해결: 
     - Debug 빌드는 정책 우회됩니다
     - Release 빌드는 정책 값 조정 또는 대기 필요

3. **광고 로드 실패**
   - 증상: Logcat에 `App open ad failed to load`
   - 해결: 
     - 네트워크 연결 확인
     - AdMob 계정 상태 확인
     - 광고 유닛 ID 확인

### 문제 2: Gradle 동기화 오류

**해결책**:
```bash
# 1. Gradle 데몬 중지
./gradlew --stop

# 2. 클린 빌드
./gradlew clean

# 3. Android Studio 재시작
```

### 문제 3: ProcessLifecycleOwner 오류

**원인**: `lifecycle-process` 의존성 누락

**해결책**:
1. `app/build.gradle.kts`에 의존성 추가 확인
2. Gradle 동기화 실행
3. 여전히 오류 시 Android Studio 재시작

### 문제 4: BuildConfig.ADMOB_APP_OPEN_UNIT_ID 오류

**원인**: BuildConfig 필드 미생성

**해결책**:
1. `buildConfigField` 추가 확인
2. `buildFeatures { buildConfig = true }` 확인
3. Gradle 동기화 후 Rebuild Project

---

## ✅ 배포 체크리스트

프로덕션 배포 전 확인 사항:

- [ ] `lifecycle-process` 의존성 추가 완료
- [ ] `AppOpenAdManager.kt` 파일 생성 완료 (패키지명 수정)
- [ ] `build.gradle.kts`에 BuildConfig 필드 추가 (Debug/Release)
- [ ] `MainApplication.kt` 수정 완료
- [ ] Gradle 동기화 성공
- [ ] Debug 빌드로 기본 동작 테스트 완료
- [ ] 콜드 스타트 보호 동작 확인
- [ ] AdMob 콘솔에서 앱 오프닝 광고 유닛 생성
- [ ] Release 빌드에 실제 광고 유닛 ID 설정
- [ ] 실제 기기에서 Release 빌드 테스트
- [ ] 정책 값 검토 및 조정 (필요 시)
- [ ] Logcat 로그 확인으로 정상 동작 검증
- [ ] 다른 전면 광고(Interstitial)와의 충돌 테스트
- [ ] 사용자 경험 테스트 (과도한 광고 노출 여부)
- [ ] AdMob 정책 준수 확인

---

## 📚 참고 자료

### Google 공식 문서
- [AdMob - App Open Ads](https://developers.google.com/admob/android/app-open)
- [ProcessLifecycleOwner](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner)
- [AdMob 정책](https://support.google.com/admob/answer/6128543)

### 관련 파일
- 원본 구현 앱: AlcoholicTimer
- 패키지: `com.sweetapps.alcoholictimer`
- Base 구조: Jetpack Compose + MobileAds SDK

---

## 💡 추가 팁

### 1. 사용자 경험 최적화
- 일일 제한을 너무 높게 설정하지 마세요 (권장: 3-5회)
- 쿨다운은 최소 5분 이상 권장
- 콜드 스타트 보호는 **반드시 유지**하세요

### 2. 성능 최적화
- 광고는 백그라운드에서 미리 로드됩니다
- 4시간 후 자동 만료로 메모리 관리
- 광고 표시 실패 시 자동 재로드

### 3. 디버깅
- Debug 빌드는 정책을 우회하여 테스트가 용이합니다
- Logcat 필터: `AppOpenAdManager`
- 모든 주요 이벤트는 로그로 기록됩니다

### 4. 다른 광고와의 조화
- Interstitial 광고와 동시에 표시되지 않도록 `isShowingAd` 플래그로 제어
- 필요 시 광고 간 우선순위 로직 추가 고려

### 5. 라이프사이클 동기화 (중요!)
- **ProcessLifecycle.onStart()**는 앱이 포그라운드로 올 때 호출되지만, Activity가 완전히 준비되기 전일 수 있습니다
- **shouldShowAdOnResume 플래그**를 사용하여 실제 광고 표시는 `onActivityResumed()`에서 수행합니다
- 이를 통해 광고가 짧게 보이다가 사라지는 문제를 방지합니다
- Activity가 완전히 준비된 후에만 광고를 표시하여 안정적인 UX를 제공합니다

---

## 🔄 버전별 호환성

| 항목 | 최소 버전 | 권장 버전 | 비고 |
|------|----------|----------|------|
| Android SDK | 21 (Lollipop) | 26+ | |
| Kotlin | 1.8.0 | 2.0+ | |
| play-services-ads | 22.0.0 | 23.4.0+ | |
| lifecycle-process | 2.5.0 | 2.9.4+ | |
| Compose | - | 최신 | 선택사항 |

---

## 📞 지원

문제 발생 시:
1. Logcat 로그 확인 (`AppOpenAdManager` 필터)
2. Gradle 동기화 및 클린 빌드 시도
3. AdMob 콘솔에서 광고 유닛 상태 확인
4. Google AdMob 지원팀 문의

---

**문서 끝** - 이 프롬프트를 AI 어시스턴트에게 제공하면 동일한 구현을 다른 앱에도 적용할 수 있습니다.

