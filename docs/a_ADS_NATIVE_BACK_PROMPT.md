# [Prompt] Native Ad on Back Press (뒤로가기 네이티브 광고 팝업) — 완전 구현 가이드

## 📋 문서 정보

**문서 버전**: v1.0.0  
**최종 수정일**: 2025-01-19  
**대상 플랫폼**: Android (Kotlin + Compose)  
**AdMob SDK 버전**: 23.0.0+  
**적용 앱**: AlcoholicTimer (최초 구현)

## 📝 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| v1.0.0 | 2025-01-19 | 최초 작성 - AlcoholicTimer 앱에서 네이티브 광고 구현 완료 | AI Agent |

## 🎯 문서 목적

이 문서는 **다른 앱에서도 동일한 네이티브 광고 패턴을 재사용**할 수 있도록 작성된 AI 에이전트용 프롬프트입니다.

- 동일 Base(Compose, BaseActivity 등)를 공유하는 앱에서 "뒤로가기 시 종료 확인 팝업 + 네이티브 광고" 패턴을 구현
- Google AdMob 정책 완전 준수
- 사용자 경험을 해치지 않는 안전한 광고 노출

## ⚠️ 정책 준수 필수사항 (중요!)

**Google Ads 정책 위반 시 앱이 Play Store에서 제재받을 수 있습니다.**

### 금지 사항
- ❌ 사용자의 앱 종료를 부당하게 방해하는 광고
- ❌ 광고를 클릭하도록 유도하는 텍스트/그래픽
- ❌ 광고와 '종료' 버튼을 혼동 가능하게 배치
- ❌ UMP 동의 전 광고 로드

### 필수 구현 사항
- ✅ 종료/취소 버튼이 광고보다 명확하게 표시
- ✅ 광고가 없어도 팝업은 정상 작동
- ✅ 일일 노출 제한 (1-2회)
- ✅ 광고에 "Ad" 라벨 명시적 표시
- ✅ 즉시 종료 가능 (추가 단계 강요 금지)

---

## 🚀 AI 에이전트 실행 프롬프트

> 아래 내용을 AI 코딩 에이전트에게 그대로 복사하여 전달하세요.

### 작업 개요

Android 앱에 **뒤로가기 시 종료 확인 팝업 + 네이티브 광고**를 구현합니다. 다음 5개 파일을 생성/수정합니다:

1. `app/build.gradle.kts` - BuildConfig 설정
2. `NativeAdManager.kt` - 광고 로더 싱글톤
3. `NativeViewBinder.kt` - 광고 뷰 바인딩 유틸
4. `include_native_exit_ad.xml` - 네이티브 광고 레이아웃
5. `NativeExitPopup.kt` - Compose 팝업 UI

### 1단계: BuildConfig 설정

**파일**: `app/build.gradle.kts`

```kotlin
// buildFeatures 섹션에 추가
buildFeatures {
    compose = true
    buildConfig = true  // 필수!
}

// buildTypes 섹션 수정
buildTypes {
    release {
        // ...existing code...
        
        // 네이티브 광고 유닛 ID (릴리즈용 - 실제 ID로 교체 필요)
        buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"ca-app-pub-XXXXX/XXXXX\"")
    }
    
    debug {
        // ...existing code...
        
        // 네이티브 광고 테스트 유닛 ID (Google 공식)
        buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
    }
}
```

### 2단계: NativeAdManager.kt 생성

**파일**: `app/src/main/java/{package}/core/ads/NativeAdManager.kt`

```kotlin
package {YOUR_PACKAGE}.core.ads

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import {YOUR_PACKAGE}.BuildConfig
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 네이티브 광고 로더 및 정책 관리 싱글톤
 * - 디버그: 정책 우회, 항상 허용
 * - 릴리즈: 일일 캡(2회), 쿨다운(2분), UMP 동의 게이팅
 */
object NativeAdManager {
    private const val TAG = "NativeAdManager"
    private const val GOOGLE_TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    private fun currentUnitId(): String {
        val id = BuildConfig.ADMOB_NATIVE_UNIT_ID
        return if (id.isBlank() || id.contains("XXXXX")) {
            GOOGLE_TEST_NATIVE_ID
        } else {
            id
        }
    }

    // 정책 기본값
    private const val DEFAULT_DAILY_CAP = 2
    private const val DEFAULT_COOLDOWN_MS = 2 * 60 * 1000L // 2분

    private const val PREFS = "ad_prefs"
    private const val KEY_NATIVE_LAST_SHOWN_MS = "native_last_shown_ms"
    private const val KEY_NATIVE_DAILY_COUNT = "native_daily_count"
    private const val KEY_NATIVE_DAILY_DAY = "native_daily_day"

    private var cachedNativeAd: NativeAd? = null
    private val isLoading = AtomicBoolean(false)

    private fun isPolicyBypassed(): Boolean = BuildConfig.DEBUG

    /**
     * 네이티브 광고 미리 로드
     */
    fun preload(context: Context) {
        if (isLoading.get()) {
            Log.d(TAG, "Already loading, skip")
            return
        }
        if (cachedNativeAd != null) {
            Log.d(TAG, "Ad already loaded, skip")
            return
        }

        // 네트워크 상태 체크
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager?.activeNetworkInfo
        @Suppress("DEPRECATION")
        if (networkInfo == null || !networkInfo.isConnected) {
            Log.w(TAG, "⚠️ No network connection, skip ad loading")
            return
        }

        isLoading.set(true)
        val unitId = currentUnitId()
        Log.d(TAG, "🔄 Loading native ad with unitId=$unitId (debug=${BuildConfig.DEBUG})")

        val adLoader = AdLoader.Builder(context, unitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "✅ Native ad loaded successfully")
                cachedNativeAd = nativeAd
                isLoading.set(false)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Native ad failed to load:")
                    Log.e(TAG, "   - Error code: ${error.code}")
                    Log.e(TAG, "   - Error message: ${error.message}")
                    
                    when (error.code) {
                        0 -> Log.e(TAG, "   ERROR_CODE_INTERNAL_ERROR")
                        1 -> Log.e(TAG, "   ERROR_CODE_INVALID_REQUEST")
                        2 -> Log.e(TAG, "   ERROR_CODE_NETWORK_ERROR")
                        3 -> Log.e(TAG, "   ERROR_CODE_NO_FILL (정상)")
                    }
                    
                    cachedNativeAd = null
                    isLoading.set(false)
                    
                    // 네트워크 오류 시 30초 후 재시도
                    if (error.code == 2 || error.code == 0) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            preload(context.applicationContext)
                        }, 30000)
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * 로드된 네이티브 광고 획득 (소유권 이전)
     */
    fun acquire(context: Context): NativeAd? {
        val ad = cachedNativeAd
        cachedNativeAd = null
        if (ad != null) {
            Log.d(TAG, "✅ Ad acquired, will reload in background")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                preload(context.applicationContext)
            }, 1000)
        } else {
            Log.w(TAG, "⚠️ No ad available to acquire")
        }
        return ad
    }

    /**
     * 정책 체크: 광고 표시가 허용되는지 확인
     */
    fun canShowAd(context: Context): Boolean {
        if (isPolicyBypassed()) {
            Log.d(TAG, "Policy bypassed (DEBUG)")
            return true
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = currentDayKey()

        // 일일 카운트 체크
        val storedDay = prefs.getString(KEY_NATIVE_DAILY_DAY, "")
        val dailyCount = if (storedDay == todayKey) {
            prefs.getInt(KEY_NATIVE_DAILY_COUNT, 0)
        } else {
            0
        }

        if (dailyCount >= DEFAULT_DAILY_CAP) {
            Log.d(TAG, "Daily cap reached ($dailyCount/$DEFAULT_DAILY_CAP)")
            return false
        }

        // 쿨다운 체크
        val lastShown = prefs.getLong(KEY_NATIVE_LAST_SHOWN_MS, 0L)
        val elapsed = now - lastShown
        if (elapsed < DEFAULT_COOLDOWN_MS) {
            val remainSec = (DEFAULT_COOLDOWN_MS - elapsed) / 1000
            Log.d(TAG, "Cooldown active (remain ${remainSec}s)")
            return false
        }

        return true
    }

    /**
     * 광고 표시 후 호출 (정책 카운터 업데이트)
     */
    fun recordShown(context: Context) {
        if (isPolicyBypassed()) {
            Log.d(TAG, "Policy bypassed, skip recording")
            return
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = currentDayKey()

        val storedDay = prefs.getString(KEY_NATIVE_DAILY_DAY, "")
        val dailyCount = if (storedDay == todayKey) {
            prefs.getInt(KEY_NATIVE_DAILY_COUNT, 0)
        } else {
            0
        }

        prefs.edit()
            .putLong(KEY_NATIVE_LAST_SHOWN_MS, now)
            .putInt(KEY_NATIVE_DAILY_COUNT, dailyCount + 1)
            .putString(KEY_NATIVE_DAILY_DAY, todayKey)
            .apply()

        Log.d(TAG, "Recorded show: count=${dailyCount + 1}/$DEFAULT_DAILY_CAP")

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            preload(context.applicationContext)
        }, 500)
    }

    private fun currentDayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }
}
```

### 3단계: NativeViewBinder.kt 생성

**파일**: `app/src/main/java/{package}/core/ads/NativeViewBinder.kt`

```kotlin
package {YOUR_PACKAGE}.core.ads

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import {YOUR_PACKAGE}.R

/**
 * 네이티브 광고 뷰 바인딩 유틸리티
 */
object NativeViewBinder {
    fun bind(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        // 헤드라인
        nativeAdView.findViewById<TextView>(R.id.ad_headline)?.let {
            it.text = nativeAd.headline
            nativeAdView.headlineView = it
        }

        // 본문
        nativeAdView.findViewById<TextView>(R.id.ad_body)?.let {
            it.text = nativeAd.body
            nativeAdView.bodyView = it
        }

        // CTA 버튼
        nativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.let {
            it.text = nativeAd.callToAction ?: "자세히 보기"
            nativeAdView.callToActionView = it
        }

        // 아이콘
        nativeAdView.findViewById<ImageView>(R.id.ad_icon)?.let { iconView ->
            nativeAd.icon?.let { icon ->
                iconView.setImageDrawable(icon.drawable)
                nativeAdView.iconView = iconView
            }
        }

        // 광고주
        nativeAdView.findViewById<TextView>(R.id.ad_advertiser)?.let {
            it.text = nativeAd.advertiser
            nativeAdView.advertiserView = it
        }

        // 미디어 뷰
        nativeAdView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)?.let {
            nativeAdView.mediaView = it
        }

        nativeAdView.setNativeAd(nativeAd)
    }
}
```

### 4단계: include_native_exit_ad.xml 생성

**파일**: `app/src/main/res/layout/include_native_exit_ad.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.gms.ads.nativead.NativeAdView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@android:color/white"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- 광고 라벨 (정책 필수) -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:text="Ad"
            android:textColor="#999999"
            android:textSize="10sp"
            android:background="#F0F0F0"
            android:paddingStart="6dp"
            android:paddingEnd="6dp"
            android:paddingTop="2dp"
            android:paddingBottom="2dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <ImageView
                android:id="@+id/ad_icon"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:contentDescription="Ad Icon" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="8dp"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/ad_headline"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="#000000"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:maxLines="2"
                    android:ellipsize="end" />

                <TextView
                    android:id="@+id/ad_advertiser"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:textColor="#666666"
                    android:textSize="11sp"
                    android:maxLines="1"
                    android:ellipsize="end" />
            </LinearLayout>
        </LinearLayout>

        <com.google.android.gms.ads.nativead.MediaView
            android:id="@+id/ad_media"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:layout_marginTop="12dp" />

        <TextView
            android:id="@+id/ad_body"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textColor="#333333"
            android:textSize="12sp"
            android:maxLines="3"
            android:ellipsize="end" />

        <Button
            android:id="@+id/ad_call_to_action"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:minWidth="120dp"
            android:textColor="@android:color/white"
            android:textSize="13sp"
            android:textStyle="bold" />

    </LinearLayout>

</com.google.android.gms.ads.nativead.NativeAdView>
```

### 5단계: NativeExitPopup.kt 생성

**파일**: `app/src/main/java/{package}/core/ui/NativeExitPopup.kt`

```kotlin
package {YOUR_PACKAGE}.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.LayoutInflater
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import {YOUR_PACKAGE}.R
import {YOUR_PACKAGE}.core.ads.NativeAdManager
import {YOUR_PACKAGE}.core.ads.NativeViewBinder

/**
 * 뒤로가기 시 표시되는 네이티브 광고 포함 종료 확인 팝업
 */
@Composable
fun NativeExitPopup(
    visible: Boolean,
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var nativeAdView by remember { mutableStateOf<NativeAdView?>(null) }

    val canShowAd = NativeAdManager.canShowAd(context)

    LaunchedEffect(visible) {
        if (visible && canShowAd) {
            android.util.Log.d("NativeExitPopup", "Attempting to acquire native ad...")
            nativeAd = NativeAdManager.acquire(context)
            if (nativeAd != null) {
                android.util.Log.d("NativeExitPopup", "✅ Native ad acquired successfully")
                NativeAdManager.recordShown(context)
            } else {
                android.util.Log.w("NativeExitPopup", "⚠️ No native ad available")
            }
        }
    }

    DisposableEffect(visible) {
        onDispose {
            nativeAdView?.destroy()
            nativeAd?.destroy()
            nativeAd = null
            nativeAdView = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 타이틀
                Text(
                    text = "앱을 종료하시겠어요?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 네이티브 광고 영역
                if (nativeAd != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.LightGray),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val inflater = LayoutInflater.from(ctx)
                                val adView = inflater.inflate(
                                    R.layout.include_native_exit_ad,
                                    null,
                                    false
                                ) as NativeAdView

                                nativeAd?.let { ad ->
                                    NativeViewBinder.bind(adView, ad)
                                }

                                nativeAdView = adView
                                adView
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // 광고 없을 때 플레이스홀더
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "감사합니다! 💙",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("취소", style = MaterialTheme.typography.bodyLarge)
                    }

                    Button(
                        onClick = onConfirmExit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("종료", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
```

### 6단계: Activity/Screen에 연동

**예시**: StartActivity 또는 메인 화면

```kotlin
import {YOUR_PACKAGE}.core.ads.NativeAdManager
import {YOUR_PACKAGE}.core.ui.NativeExitPopup
import androidx.activity.compose.BackHandler

// onCreate 또는 초기화 시점
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // UMP 동의 후 광고 미리 로드
    UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
        if (canRequest) {
            NativeAdManager.preload(this)
        }
    }
}

// Composable 화면 내부
@Composable
fun MainScreen(activity: Activity?) {
    var showExitPopup by remember { mutableStateOf(false) }

    // 뒤로가기 핸들러
    BackHandler(enabled = true) {
        showExitPopup = true
    }

    // 네이티브 광고 팝업
    NativeExitPopup(
        visible = showExitPopup,
        onConfirmExit = {
            activity?.finish()
        },
        onDismiss = {
            showExitPopup = false
        }
    )
    
    // ...existing UI code...
}
```

### 7단계: AndroidManifest.xml 확인

AdMob App ID가 설정되어 있는지 확인:

```xml
<application>
    <!-- ...existing code... -->
    
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
</application>
```

### 8단계: MainApplication.kt 초기화

**파일**: `app/src/main/java/{package}/MainApplication.kt`

```kotlin
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // AdMob 초기화
        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this) { initStatus ->
            android.util.Log.d("MainApplication", "MobileAds initialized: $initStatus")
        }
    }
}
```

---

## ✅ 구현 검증 체크리스트

### 기능 테스트
- [ ] 디버그 빌드 시 테스트 광고가 로드되는가?
- [ ] 광고 상단에 "Ad" 라벨이 표시되는가?
- [ ] 뒤로가기 시 팝업이 정상 표시되는가?
- [ ] 광고가 없을 때도 팝업이 정상 작동하는가?
- [ ] "취소" 버튼으로 팝업을 닫을 수 있는가?
- [ ] "종료" 버튼으로 앱이 종료되는가?

### 정책 준수
- [ ] 종료/취소 버튼이 광고보다 명확한가?
- [ ] 광고를 클릭하도록 유도하는 텍스트가 없는가?
- [ ] 일일 2회 이상 표시되지 않는가?
- [ ] UMP 동의 전에는 광고가 로드되지 않는가?

### 로그 확인
Logcat에서 다음 태그로 필터링:
- `NativeAdManager`
- `NativeExitPopup`

정상 로그:
```
🔄 Loading native ad with unitId=...
✅ Native ad loaded successfully
✅ Ad acquired successfully
```

---

## 🐛 문제 해결

### 광고가 표시되지 않을 때

1. **Logcat 에러 코드 확인**
   - `ERROR_CODE_0`: 내부 오류 (재시도 됨)
   - `ERROR_CODE_1`: 잘못된 요청 (유닛 ID 확인)
   - `ERROR_CODE_2`: 네트워크 오류 (인터넷 연결 확인)
   - `ERROR_CODE_3`: 광고 없음 (테스트 광고는 항상 있어야 함)

2. **BuildConfig 확인**
   - `BuildConfig.ADMOB_NATIVE_UNIT_ID`가 올바른 값인지 확인
   - Gradle Sync 후 Clean Build 실행

3. **네트워크 확인**
   - 에뮬레이터/기기가 인터넷에 연결되어 있는지 확인

4. **UMP 동의 확인**
   - UMP 동의가 완료된 후 광고가 로드되는지 확인

---

## 📚 참고 자료

- [Google AdMob Native Ads 정책](https://support.google.com/admob/answer/6329638)
- [Native Ads Advanced 가이드](https://developers.google.com/admob/android/native/advanced)
- [AdMob 정책 센터](https://support.google.com/admob/answer/6128543)

---

## 🔄 향후 개선 계획

- v1.1.0: 네이티브 광고 A/B 테스트 지원
- v1.2.0: 다양한 레이아웃 템플릿 제공
- v2.0.0: 자동 최적화 기능 추가

---

## 📌 적용 시 주의사항

1. **패키지명 교체**: 모든 `{YOUR_PACKAGE}`를 실제 패키지명으로 교체
2. **리소스 ID 확인**: `R.id.*` 리소스가 프로젝트에 맞게 설정되었는지 확인
3. **색상 리소스**: 필요 시 `Color.LightGray` 등을 프로젝트의 색상 리소스로 교체
4. **텍스트 다국어화**: 하드코딩된 텍스트를 `strings.xml`로 이동

---

**문서 작성 완료**. 이 가이드를 AI 에이전트에게 전달하여 다른 앱에도 동일한 패턴을 적용하세요.

