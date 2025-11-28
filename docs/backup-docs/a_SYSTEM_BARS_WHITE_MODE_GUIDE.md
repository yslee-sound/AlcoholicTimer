# 시스템 바(상태바/네비게이션바) 화이트 라이트 모드 가이드

버전: v1.0.0 (2025-10-25)

## 변경 이력
- 2025-10-25 v1.0.0
  - 최초 작성: 상태바가 회색으로 보이거나 최초 실행 시 하단 3버튼이 사라지는 문제의 근본 원인과 해결책을 정리
  - 단일 소스(테마 XML)로 시스템 바를 관리하는 원칙 수립
  - Compose 인셋/오버레이 중복으로 생기는 상단 회색 스트립 이슈 해결 패턴 포함
  - API 레벨별 테마 설정 샘플과 QA 체크리스트 제공

---

## TL;DR(요약)
- 시스템 바(상태바/네비게이션바) 색/아이콘은 **XML 테마 한 곳**에서만 관리하세요.
- 상태바/네비게이션바 배경은 흰색, 아이콘은 어두운 색(라이트)로 고정합니다.
- Android 10+(API 29+)의 대비 스크림(enforce contrast)을 꺼서 회색 오버레이를 방지합니다.
- Compose에서 상단 인셋을 **중복 적용하지 말고**, 상태바 영역을 덮는 오버레이도 제거합니다.
- 필요하지 않다면 Edge‑to‑Edge를 쓰지 말고(기본값 유지), 사용하는 경우에만 인셋을 정확히 적용하세요.

---

## 근본 원인
1) 설정 충돌(중복 소스)
- 다음과 같이 여러 레이어에서 시스템 바를 각각 만질 때, 최초 프레임/전환 시점에 값이 뒤섞여 예측 불가능한 UI가 나옵니다.
  - 테마 XML(values, values‑v29, values‑v31 등)
  - Kotlin 코드(Theme.kt SideEffect, BaseActivity.onCreate, StartActivity.onCreate 등)

2) 인셋/오버레이 중복(Compose 레이아웃)
- Window가 상태바 아래로 콘텐츠를 내리지 않는 기본 모드에서, TopAppBar에 statusBarsPadding 또는 상단 오버레이를 추가해 **여분의 상단 갭**이 생김 → 그 갭을 회색 surfaceVariant 배경이 채워서 ‘상단 회색 줄’처럼 보임.

3) Android 10+ 대비 스크림
- 일부 Pixel/순정 계열에서 대비 보장을 위해 상태바/네비게이션바 위에 회색 스크림을 덧입힘 → 흰색 의도가 흐려짐.

---

## 해결 원칙(단일 소스 전략)
- 시스템 바 색/아이콘은 **테마 XML**만 진실의 원천(Source of Truth)으로 삼고, **코드에서 다시 덮어쓰지 않습니다**.
- Compose 레이아웃에서는 **중복 인셋과 오버레이를 제거**하여 회색 갭을 없앱니다.

---

## 테마 XML 샘플(복붙 가이드)

아래를 각 API 레벨 리소스에 맞춰 추가/수정하세요.

### 1) 기본값(values/themes.xml)
```xml
<style name="Theme.YourApp" parent="@android:style/Theme.DeviceDefault.NoActionBar">
    <item name="android:windowFullscreen">false</item>
    <!-- 흰색 배경 고정 -->
    <item name="android:statusBarColor">@android:color/white</item>
    <item name="android:navigationBarColor">@android:color/white</item>
    <!-- 전환 깜빡임 방지/상단 오버레이 제거 -->
    <item name="android:windowBackground">@android:color/white</item>
    <item name="android:windowContentOverlay">@null</item>
</style>

<!-- 스플래시 테마(Pre‑31 백포트) -->
<style name="Theme.YourApp.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@android:color/white</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/splash_app_icon</item>
    <item name="postSplashScreenTheme">@style/Theme.YourApp</item>
    <!-- 스플래시 동안도 흰색 유지 -->
    <item name="android:statusBarColor">@android:color/white</item>
    <item name="android:navigationBarColor">@android:color/white</item>
</style>
```

### 2) API 23+(values‑v23/themes.xml)
```xml
<style name="Theme.YourApp" parent="@android:style/Theme.DeviceDefault.NoActionBar">
    <item name="android:windowFullscreen">false</item>
    <item name="android:statusBarColor">@android:color/white</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowBackground">@android:color/white</item>
    <item name="android:windowContentOverlay">@null</item>
</style>
```

### 3) API 27+(values‑v27/themes.xml) — 네비게이션바 라이트 아이콘
```xml
<style name="Theme.YourApp" parent="@android:style/Theme.DeviceDefault.NoActionBar">
    <item name="android:navigationBarColor">@android:color/white</item>
    <item name="android:windowLightNavigationBar">true</item>
</style>
```

### 4) API 29+(values‑v29/themes.xml) — 대비 스크림 끄기
```xml
<style name="Theme.YourApp" parent="@android:style/Theme.DeviceDefault.NoActionBar">
    <item name="android:statusBarColor">@android:color/white</item>
    <item name="android:navigationBarColor">@android:color/white</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowLightNavigationBar">true</item>
    <item name="android:enforceStatusBarContrast">false</item>
    <item name="android:enforceNavigationBarContrast">false</item>
    <item name="android:windowBackground">@android:color/white</item>
    <item name="android:windowContentOverlay">@null</item>
</style>
```

### 5) API 31+(values‑v31/themes.xml)
```xml
<style name="Theme.YourApp" parent="@android:style/Theme.DeviceDefault.NoActionBar">
    <item name="android:statusBarColor">@android:color/white</item>
    <item name="android:navigationBarColor">@android:color/white</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowLightNavigationBar">true</item>
    <item name="android:enforceStatusBarContrast">false</item>
    <item name="android:enforceNavigationBarContrast">false</item>
    <item name="android:windowBackground">@android:color/white</item>
    <item name="android:windowContentOverlay">@null</item>
</style>

<style name="Theme.YourApp.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@android:color/white</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/splash_app_icon</item>
    <item name="postSplashScreenTheme">@style/Theme.YourApp</item>
    <item name="android:statusBarColor">@android:color/white</item>
    <item name="android:navigationBarColor">@android:color/white</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowLightNavigationBar">true</item>
    <item name="android:enforceStatusBarContrast">false</item>
    <item name="android:enforceNavigationBarContrast">false</item>
</style>
```

> 팁: 하위/상위 버전 리소스가 **중복/누락 없이 계층적으로 덮어쓰는지** 반드시 확인하세요.

---

## Compose 레이아웃 체크리스트(중복 인셋/오버레이 제거)
- "상단 회색 줄"이 보이면 대부분 아래 두 항목 중 하나입니다.
  1) TopAppBar에 `Modifier.windowInsetsPadding(WindowInsets.statusBars)`(또는 statusBarsPadding)를 **클래식 모드**에서 쓰고 있다.
  2) 상태바 높이만큼 덮는 흰색/투명 오버레이 Box를 올려두었다.

해결:
- 클래식 모드(권장): `WindowCompat.setDecorFitsSystemWindows(window, true)`를 유지하고, TopAppBar에는 **추가 상단 패딩/오버레이를 적용하지 않습니다**. 루트 컨테이너 Surface/Scaffold는 `color = Color.White`로.
- Edge‑to‑Edge를 정말 써야 한다면: `setDecorFitsSystemWindows(window, false)` + TopAppBar에만 `statusBarsPadding()`을 적용하고, 상태바 배경을 의도적으로 흰색으로 유지하려면 레이아웃 상단에 흰색 바를 정확히 **한 번만** 깔아야 합니다(중복 금지).

---

## 코드에서 시스템 바 조작 금지(필요 시 최소화)
- 아래와 같은 코드는 **가능하면 제거**하세요.
  - `window.statusBarColor = ...`, `window.navigationBarColor = ...`
  - `WindowInsetsControllerCompat(...).isAppearanceLightStatusBars = ...`
  - `WindowCompat.setDecorFitsSystemWindows(window, ...)`
- 이유: 테마 XML과 충돌하여 최초 프레임/전환 타이밍에서 잔상이 남습니다.
- 정말 필요할 때(테마 토글 기능 등)만 코드로 바꾸되, 전체 앱에서 한 곳(예: BaseActivity)에서 일괄 처리하고 테마 XML과 합이 맞는지 검증하세요.

---

## QA 체크리스트
- [ ] 첫 실행(스플래시 → 첫 화면)에서 상태바가 ‘완전 흰색’인가?
- [ ] TopAppBar와 상태바 사이에 얇은 회색 띠가 전혀 보이지 않는가?
- [ ] 하단 3버튼이 즉시 흰 배경 위 검은 아이콘으로 보이는가?
- [ ] 회전/다크모드/다양한 내비 모드(3버튼, 제스처)에서 일관되는가?
- [ ] Android 10+ 기기에서 대비 스크림(회색 오버레이)이 전혀 보이지 않는가?
- [ ] 스플래시/런타임 전환 구간에서도 색상 점프가 없는가?

---

## 트러블슈팅
- 상단 회색 띠가 남음
  - TopAppBar의 `statusBarsPadding()`/오버레이를 제거했는지 다시 확인
  - 루트 배경이 회색(surfaceVariant)인데 상단 갭이 생기지 않게 인셋 중복 제거
- 하단 3버튼이 보였다/사라졌다 반복
  - values‑v27 이상에서 `windowLightNavigationBar=true`와 `navigationBarColor=white`가 있는지 확인
- Pixel에서만 회색 느낌
  - v29/31에 `enforceStatusBarContrast=false`, `enforceNavigationBarContrast=false` 누락 여부 확인
- 코드에서 시스템바를 다시 덮어씀
  - Theme.kt SideEffect, BaseActivity.onCreate, 특정 Activity.onCreate에서의 윈도우 조작 제거

---

## 적용 순서(다른 앱에 이식)
1) 테마 XML부터 정리: values/v23/v27/v29/v31에 본문 샘플대로 추가
2) 코드 레벨 윈도우 조작(테마와 충돌하는 부분) 제거
3) Compose 레이아웃에서 상단 인셋/오버레이 중복 제거
4) 완전 제거/재설치 후 첫 실행으로 검증

---

## 부록: 모드 선택 가이드
- 클래식(권장): 상태바/네비게이션바 모두 흰색, 콘텐츠는 시스템바 밖에서 시작
- Edge‑to‑Edge: 콘텐츠가 시스템바 아래로 확장됨. 이 경우 인셋 적용을 **정확히 한 번**만.

---

## 참고
- 이 저장소 적용 사례: BaseActivity의 상단 오버레이/중복 인셋 제거, 테마 XML로 단일화, v29/31 대비 스크림 해제
- Pixel 7 Pro, Android 10+에서 재현되던 회색 이슈가 본 가이드로 해결됨

