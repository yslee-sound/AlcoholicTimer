# 스플래시 이식 가이드 (API 21 ~ 35+ / Compose 대응)

목표
- 모든 API에서 스플래시 아이콘이 끊김 없이 처음부터 끝까지 보이고, 자연스러운 페이드 인/아웃으로 본 화면으로 전환되도록 통일합니다.

핵심 전략
1) Android 12+(API 31+)
   - 시스템 SplashScreen 사용(`installSplashScreen`).
   - 종료 시 220ms 페이드 아웃 + scale 1.05 적용(`setOnExitAnimationListener`).
   - `setKeepOnScreenCondition`으로 최소 800ms 유지.
   - 시스템 스플래시 아이콘 잘림 방지를 위해 인셋 리소스 사용(`@drawable/splash_app_icon_inset`).
   - 메인 테마 `android:windowBackground = @android:color/white` 고정.

2) Android 11-(API 30-)
   - 메인 테마 `android:windowBackground = @drawable/splash_screen` 통일(값/`values-v23`/`values-v29`).
   - 런처 액티비티의 `setContent`를 최소 표시시간만큼 지연하여 windowBackground가 충분히 노출되도록 합니다.
   - Compose 오버레이(AnimatedVisibility: fadeIn/fadeOut + scaleIn/scaleOut)로 체크/초기화 대기 중에도 동일한 스플래시를 유지합니다.
   - 오버레이 종료 시 `window.setBackgroundDrawable(null)`로 잔상/깜빡임 방지.

3) 공통
   - 오버레이/시스템 스플래시 모두 아이콘은 `@drawable/splash_app_icon` 기준(필요 시 인셋 리소스 활용).
   - 최소 표시시간 800ms, 애니메이션 220ms.

수정 파일 요약
- `app/src/main/java/.../StartActivity.kt`
  - `installSplashScreen()` + `setOnExitAnimationListener`(31+)
  - `setKeepOnScreenCondition`로 최소 800ms 유지(31+)
  - API<31에서는 첫 렌더 지연 + Compose 오버레이 표시
  - 오버레이 종료 시 `window.setBackgroundDrawable(null)`
- `app/src/main/res/values/themes.xml`, `.../values-v23/themes.xml`, `.../values-v29/themes.xml`
  - 메인 테마 `android:windowBackground = @drawable/splash_screen`로 통일(31 미만)
- `app/src/main/res/values-v31/themes.xml`
  - 시스템 스플래시 아이콘 인셋: `android:windowSplashScreenAnimatedIcon = @drawable/splash_app_icon_inset`
  - 메인 테마 `android:windowBackground = @android:color/white`
- `app/src/main/res/drawable/splash_screen.xml`
  - 흰 배경 + 중앙 아이콘 레이어
- `app/src/main/res/drawable-anydpi-v26/splash_app_icon_inset.xml`
  - 아이콘 사방 24dp 인셋(필요 시 28~32dp)

이식 절차 (Step-by-Step)
1) 아이콘 리소스 준비
   - `drawable`에 앱 로고를 `splash_app_icon`으로 준비(PNG 또는 Vector).
   - 12+ 원형 잘림 방지를 위해 `drawable-anydpi-v26`에 `splash_app_icon_inset.xml` 추가.

2) 테마 반영
   - 31 미만(모든 values, `values-v23`, `values-v29`): 메인 테마 `android:windowBackground = @drawable/splash_screen`.
   - 31 이상(`values-v31`): 스플래시 테마에서 `android:windowSplashScreenAnimatedIcon = @drawable/splash_app_icon_inset` 지정, 메인 테마 배경은 흰색.

3) 스플래시 레이어 추가
   - `drawable/splash_screen.xml`: 흰 배경 + 중앙 아이콘.

4) 런처 액티비티 수정
   - `installSplashScreen()` 설치.
   - 31+: `setOnExitAnimationListener`로 220ms 페이드/스케일 아웃.
   - 31+: `setKeepOnScreenCondition`로 최소 800ms 보장.
   - <31: 첫 렌더를 남은 시간만큼 지연, Compose 오버레이(AnimatedVisibility) 표시.
   - 오버레이 종료 시 `window.setBackgroundDrawable(null)` 호출.

5) 검증(콜드 스타트 기준)
   - API 30 / 31 / 34+ 각 버전에서 확인.
   - 다크/라이트 모드 모두 확인(배경은 항상 흰색으로 유지되는지).
   - 전환 시 로고 잔상/깜빡임/두 번 보임 현상 여부 확인.

트러블슈팅
- 31+에서 본화면 직전 로고가 잠깐 다시 보임: 메인 테마 배경을 `@android:color/white`로 유지하고, 31+에서는 Compose 오버레이를 비활성화했는지 확인.
- 아이콘 원형 컨테이너 잘림: 인셋 24dp → 28~32dp로 확대.
- 프레임 깜빡임: 오버레이 종료 시점에 `window.setBackgroundDrawable(null)` 호출 누락 여부 확인.

애니메이션/시간 파라미터
- 최소 표시시간: 600~1200ms 권장(기본 800ms)
- 오버레이 enter/exit: 220ms, scale 0.98 → 1.02(200~300ms로 조정 가능)
- 시스템 스플래시 exit: 220ms, scale 1.05(1.02~1.08로 조정 가능)

예시 코드 모음(필요 부분만 발췌)

```kotlin
// StartActivity.kt (핵심 로직 요약)
class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = androidx.core.splashscreen.SplashScreen.installSplashScreen(this)
        val splashStart = android.os.SystemClock.uptimeMillis()
        val minShowMillis = 800L
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            splash.setOnExitAnimationListener { provider ->
                provider.iconView.animate()
                    .alpha(0f)
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(220)
                    .withEndAction { provider.remove() }
                    .start()
            }
            splash.setKeepOnScreenCondition {
                android.os.SystemClock.uptimeMillis() - splashStart < minShowMillis
            }
        }
        super.onCreate(savedInstanceState)

        val launchContent = {
            val elapsed = android.os.SystemClock.uptimeMillis() - splashStart
            val initialRemain = (minShowMillis - elapsed).coerceAtLeast(0L)
            val usesComposeOverlay = android.os.Build.VERSION.SDK_INT < 31
            setContent {
                val keepMin = remember { mutableStateOf(initialRemain > 0L) }
                LaunchedEffect(initialRemain) {
                    if (initialRemain > 0L) kotlinx.coroutines.delay(initialRemain); keepMin.value = false
                }
                val isChecking = remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(400); isChecking.value = false }

                val showOverlay = usesComposeOverlay && (keepMin.value || isChecking.value)
                LaunchedEffect(showOverlay) {
                    if (!showOverlay) window.setBackgroundDrawable(null)
                }

                androidx.compose.material3.MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        // 실제 화면 UI

                        // 스플래시 오버레이(30-): 페이드/스케일 연출
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showOverlay,
                            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                                    androidx.compose.animation.scaleIn(initialScale = 0.98f, animationSpec = androidx.compose.animation.core.tween(220)),
                            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220)) +
                                   androidx.compose.animation.scaleOut(targetScale = 1.02f, animationSpec = androidx.compose.animation.core.tween(220))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.splash_app_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(240.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT < 31) {
            val remain = (minShowMillis - (android.os.SystemClock.uptimeMillis() - splashStart)).coerceAtLeast(0L)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ launchContent() }, remain)
        } else {
            launchContent()
        }
    }
}
```

```xml
<!-- values/themes.xml (31 미만 공통) -->
<resources>
    <style name="Theme.YourApp" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:windowBackground">@drawable/splash_screen</item>
    </style>

    <!-- 백포트 호환 목적의 스플래시 테마 정의(선택) -->
    <style name="Theme.YourApp.Splash" parent="Theme.SplashScreen">
        <item name="postSplashScreenTheme">@style/Theme.YourApp</item>
        <item name="android:windowSplashScreenBackground">@android:color/white</item>
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/splash_app_icon</item>
    </style>
</resources>
```

```xml
<!-- values-v31/themes.xml (31+) -->
<resources>
    <style name="Theme.YourApp" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:windowBackground">@android:color/white</item>
    </style>

    <style name="Theme.YourApp.Splash" parent="Theme.SplashScreen">
        <item name="postSplashScreenTheme">@style/Theme.YourApp</item>
        <item name="android:windowSplashScreenBackground">@android:color/white</item>
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/splash_app_icon_inset</item>
        <item name="android:windowSplashScreenAnimationDuration">220</item>
    </style>
</resources>
```

```xml
<!-- drawable/splash_screen.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@android:color/white" />
    <item>
        <inset
            android:insetLeft="0dp"
            android:insetTop="0dp"
            android:insetRight="0dp"
            android:insetBottom="0dp">
            <bitmap
                android:src="@drawable/splash_app_icon"
                android:gravity="center"
                android:antialias="true"
                android:filter="true"
                android:dither="true" />
        </inset>
    </item>
</layer-list>
```

```xml
<!-- drawable-anydpi-v26/splash_app_icon_inset.xml (31+ 인셋) -->
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="24dp"
    android:insetTop="24dp"
    android:insetRight="24dp"
    android:insetBottom="24dp">
    <bitmap android:src="@drawable/splash_app_icon" />
</inset>
```

검증 체크리스트(요약)
- API 30/31/34+에서 콜드 스타트 시:
  - 아이콘이 시작부터 끝까지 끊김 없이 보이나?
  - 31+: 시스템 스플래시 종료 시 페이드/스케일 아웃이 220ms로 자연스러운가?
  - 30-: Compose 오버레이 페이드/스케일 인/아웃이 220ms로 동작하는가?
  - 오버레이 종료 후 잔상/깜빡임이 없는가(`window.setBackgroundDrawable(null)` 호출 확인)?
- 다크/라이트 모드: 배경이 항상 흰색으로 유지되는가?
- 특정 기기(노치, 라운드, 고주사율)에서 잘림/깜빡임 없는가?

부록: 조절 포인트
- 최소 표시시간: 800ms(앱 특성에 맞게 600~1200ms)
- 오버레이 애니메이션: 180~300ms, scale 0.98/1.02
- 시스템 스플래시 exit: 200~300ms, scale 1.02~1.08
- 인셋: 24dp → 28~32dp(원형 잘림 발생 시)

