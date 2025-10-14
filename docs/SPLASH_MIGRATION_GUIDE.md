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

## 내부 내비게이션에서 스플래시 생략(API 30-)
문제: 드로어 등 내부 라우팅으로 런처(Start) 화면을 열 때, API 30-에서는 테마의 `windowBackground`(스플래시 레이어)가 다시 보여 깜빡임처럼 보일 수 있습니다.

해결 방법(권장 패턴)
1) 내부 이동 시 인텐트에 플래그를 추가합니다.
```kotlin
// Drawer 등에서 StartActivity로 이동
startActivity(Intent(this, StartActivity::class.java).apply {
    putExtra("skip_splash", true)
})
overridePendingTransition(0, 0)
```
2) StartActivity에서 플래그를 읽어 API 31 미만일 때 스플래시 지연/오버레이를 건너뜁니다.
```kotlin
val skipSplash = intent.getBooleanExtra("skip_splash", false)
val usesComposeOverlay = Build.VERSION.SDK_INT < 31 && !skipSplash
val initialRemain = if (skipSplash) 0L else computedRemain

if (Build.VERSION.SDK_INT < 31) {
    if (skipSplash) {
        // 테마 windowBackground(스플래시 레이어)를 즉시 덮고 첫 프레임 직후 제거
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        launchContent()
        window.decorView.post { window.setBackgroundDrawable(null) }
    } else {
        Handler(Looper.getMainLooper()).postDelayed({ launchContent() }, remain)
    }
} else {
    launchContent()
}
```
효과: 콜드 스타트에서는 스플래시가 정상 노출되고, 내부 네비게이션에서는 스플래시가 전혀 보이지 않습니다.

## Start 화면 워터마크(배경 장식) 패턴
목적: Start(금주 설정) 화면의 빈 영역에 앱 정체성을 드러내는 워터마크(연한 아이콘)를 표시합니다.

레이어링 원칙
- “배경 색상” 위, “실제 콘텐츠” 아래 레이어에 그려야 합니다. 공용 스크린 컴포저블이 배경을 덮어씌운다면, 별도의 장식 슬롯을 제공하세요.

예시 A: 공용 스크린에 슬롯 추가
```kotlin
@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    backgroundDecoration: @Composable BoxScope.() -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        backgroundDecoration() // 배경 위, 콘텐츠 아래
        // ... top/bottom 레이아웃 ...
    }
}
```
Start 화면에서 슬롯 사용
```kotlin
StandardScreenWithBottomButton(
    topContent = { /* 카드/입력 UI */ },
    bottomButton = { /* 시작 버튼 */ },
    backgroundDecoration = {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val base = if (maxWidth < maxHeight) maxWidth else maxHeight
            val iconSize = base * 0.70f // 기본 70% (이식 시 0.35~0.80 권장)
            Image(
                painter = painterResource(R.drawable.splash_app_icon),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(iconSize).alpha(0.12f)
            )
        }
    }
)
```
예시 B: 슬롯이 없다면 레이어 순서 보장
- 최상단 Box에서: 배경 색상 → 워터마크 → 실제 콘텐츠 순으로 배치.

튜닝 범위(권장)
- 크기 비율: 0.35f ~ 0.80f (기본 0.70f)
- 투명도(alpha): 0.08 ~ 0.16 (기본 0.12)
- 위치: 중앙 고정(필요 시 `offset(y = 24.dp)` 등으로 미세 조정)

## 변경 이력(요약)
- 2025-10-14
  - 내부 네비게이션(API 30-) 스플래시 재등장 방지: `skip_splash` 플래그 도입, Start에서 지연/오버레이 생략 + 배경 즉시 덮기/첫 프레임 이후 제거.
  - Start 화면 워터마크 추가: 공용 스크린에 `backgroundDecoration` 슬롯 추가, 기본 크기 70%, alpha 0.12.
