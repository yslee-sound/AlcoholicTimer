# 변경 이력 (Banner Ads)
- 2025-10-25 v1.3.0
  - 배너 높이 “초기 고정 예약” 도입: Anchored Adaptive 배너의 예측 높이를 사용해 광고 미노출 시점부터 동일 높이의 흰 영역을 예약하여 레이아웃 점프 제거
  - 공용 함수 제공: `predictAnchoredBannerHeightDp()` (Compose) — 회전/해상도 변경 시 자동 재계산, 실패 시 50dp 폴백
  - 적용: BaseScreen, StandardScreenWithBottomButton, DetailActivity, AddRecordActivity 등 모든 배너 컨테이너
  - 문서: 재사용 가능한 패턴/체크리스트/트러블슈팅 업데이트
- 2025-10-25 v1.2.0
  - 배너 상단 헤어라인(구분선) 추가: 모든 배너 광고 컨테이너 상단에 얇은 구분선 적용
    - 두께: `AppBorder.Hairline` (약 0.5dp)
    - 색상: `#E0E0E0` (은은한 회색)
    - 적용 위치: BaseActivity, StandardScreen, DetailActivity, AddRecordActivity 등 모든 배너 컨테이너
    - 목적: 배너 영역을 콘텐츠와 시각적으로 명확히 구분하여 UX 개선
- 2025-10-24 v1.1.4
  - 전역 배너 위 간격(BANNER_TOP_GAP)을 0dp로 확정. 불필요한 전역 갭 제거, 화면별로 필요한 곳만 `bannerTopGap` 또는 콘텐츠 내부 패딩으로 보완.
- 2025-10-24 v1.1.3
  - (취소됨) 전역 기본 간격 16dp 상향. 실 사용 결과, 전역 갭이 불필요하여 철회하고 0dp로 회귀(v1.1.4).
- 2025-10-24 v1.1.2
  - BaseScreen에 `bannerTopGap` 파라미터 추가: 화면별로 배너 위 간격을 조절 가능. 기본값은 `LayoutConstants.BANNER_TOP_GAP`.
  - BaseScreen: 배너가 있을 때 콘텐츠 쪽 `bottomExtra`와 내비 인셋을 제거, IME 표시시에만 IME 높이 반영.
  - 일부 화면(예: 레벨 화면): 콘텐츠 내부 하단 패딩(예: 8dp)으로 촘촘함 완화.
- 2025-10-24 v1.1.1
  - 설정 화면(SettingsActivity) 하단 여백 과다 이슈 수정: BaseScreen이 제공하는 LocalSafeContentPadding과 별도 수동 bottom 패딩(8dp) 중복 제거.
  - 가이드 보강: BaseScreen + bottomAd 조합에서는 콘텐츠 영역에 별도의 하단 패딩을 추가하지 말 것(Do/Don't 섹션 추가).
- 2025-10-10 v1.0.0
  - 초안 작성: 배너 의존성, 기본 배치 가이드, QA 체크리스트.

---

# 최종 정책 요약 (Cross‑App Reusable)

- 배너 높이 예약: “Anchored Adaptive 예측 높이”로 컨테이너 height를 고정해 처음부터 동일 높이의 흰 영역을 확보(레이아웃 점프 제거).
- 배너 상단 구분선: 모든 배너 컨테이너 상단에 헤어라인(`AppBorder.Hairline`, #E0E0E0) 적용하여 콘텐츠와 광고 영역을 명확히 구분.
- 전역 기본: 배너 위 전역 간격은 0dp(BANNER_TOP_GAP=0.dp). 광고 영역은 "화면 최하단 + 내비/IME 인셋 고려"만 담당.
- 화면별 완충 여백: "딱 달라붙음"을 완화하고 싶을 때만 화면 콘텐츠 쪽에 소량의 내부 하단 패딩(권장 6~12dp, 보통 8dp) 추가.
- 인셋 일관성: 배너 컨테이너는 내비/IME 인셋을 자체 처리, 콘텐츠 안전 패딩(LocalSafeContentPadding)은 배너가 있을 때 내비 인셋과 전역 추가여백을 중복 적용하지 않음.

---

# 재사용 가능한 구현 (Compose)

## Anchored Adaptive 예측 높이 함수
```kotlin
@Composable
fun predictAnchoredBannerHeightDp(): Dp {
    val context = LocalContext.current
    val conf = LocalConfiguration.current
    val density = LocalDensity.current
    val availableWidthDp = conf.screenWidthDp // 컨테이너가 화면 풀폭일 때
    return try {
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
        with(density) { adSize.getHeightInPixels(context).toDp() }.coerceAtLeast(50.dp)
    } catch (_: Throwable) {
        50.dp // 폴백(전화기 기준 최소)
    }
}
```

## 배너 컨테이너 레이아웃(기본)
```kotlin
val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
val effectiveBottom = maxOf(navBottom, imeBottom)
val predictedBannerH = predictAnchoredBannerHeightDp()

// 배너 상단 헤어라인
HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))

// 배너 컨테이너 — height "고정"
Surface(color = Color.White) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = effectiveBottom)
            .height(predictedBannerH),
        contentAlignment = Alignment.Center
    ) { AdmobBanner() }
}
```

## 버튼이 있는 화면(Standard/FAB) — 예약 높이 반영
```kotlin
val reservedBannerH = if (showBanner) predictAnchoredBannerHeightDp() else 0.dp
val reservedBottom = fabHalf + gap + reservedBannerH + effectiveBottom

Column(modifier = Modifier.padding(bottom = reservedBottom)) { /* 콘텐츠 */ }
// 하단 정렬된 배너 컨테이너는 위의 "기본"과 동일
```

## (옵션) 뷰 시스템(ViewGroup) 힌트
```kotlin
val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
val hPx = adSize.getHeightInPixels(context).coerceAtLeast(dpToPx(50))
container.layoutParams = container.layoutParams.apply { height = hPx }
```

---

# 적용 레시피 (패턴 모음)
## BaseActivity/BaseScreen 사용 시 (자동)
```kotlin
BaseScreen(bottomAd = { AdmobBanner() }) { /* content */ }
```

## 커스텀 레이아웃에서 직접 배치
```kotlin
Column(Modifier.fillMaxSize()) {
    Box(Modifier.weight(1f)) { /* content */ }
    HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))
    Surface(color = Color.White) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = effectiveBottom)
                .height(predictAnchoredBannerHeightDp()),
            contentAlignment = Alignment.Center
        ) { AdmobBanner() }
    }
}
```

## StandardScreenWithBottomButton 패턴
```kotlin
StandardScreenWithBottomButton(
    topContent = { /* cards */ },
    bottomButton = { /* FAB */ },
    bottomAd = { AdmobBanner() }
)
```

## 콘텐츠 여백 패턴
1) 전역 기본(배너 위 간격 없음)
```kotlin
BaseScreen(bottomAd = { AdmobBanner() }) { /* content */ }
```
2) LazyColumn — 배너 위 최소 여백 8dp
```kotlin
LazyColumn(/*...*/, contentPadding = PaddingValues(bottom = 8.dp)) { /* items */ }
```
3) Column + verticalScroll — 배너 위 최소 여백 8dp
```kotlin
Column(Modifier.verticalScroll(rememberScrollState()).padding(LocalSafeContentPadding.current).padding(bottom = 8.dp)) { /* children */ }
```
4) 스크롤 불필요(내용 짧음) — 남는 높이 채우면서 8dp 보장
```kotlin
val fillerDp = with(LocalDensity.current) { (viewportH - contentH).coerceAtLeast(0).toDp() } + 8.dp
```
5) 특정 화면만 배너 위 간격 필요
```kotlin
BaseScreen(bottomAd = { AdmobBanner() }, bannerTopGap = 8.dp) { /* content */ }
```

권장 우선순위: 콘텐츠 내부 패딩(2,3,4) → (필요 시) 화면별 bannerTopGap(5) → 전역 기본은 0 유지.

---

# 마이그레이션 가이드
- 기존 heightIn(min=50dp)만 사용하던 레이아웃 → `height(predictAnchoredBannerHeightDp())`로 교체.
- BaseScreen 사용 시: 콘텐츠 쪽 하단 패딩 중복 제거(내비 인셋/전역 여백 중복 주의).
- 비‑BaseScreen 앱: 배너 위 Spacer 제거(0dp), 콘텐츠 컨테이너에서만 6~12dp 완충.

---

# 트러블슈팅
- 광고 로딩 전/후에 레이아웃이 출렁임
  - 컨테이너 높이가 고정되어 있지 않은 경우 → 예측 높이로 height 고정.
- 회전 시 배너 높이가 들쭉날쭉
  - `LocalConfiguration.current`를 참조하여 재구성될 때 높이가 재계산되는지 확인.
- 배너 위에 회색 띠가 크게 남음
  - 전역 안전 패딩/인셋 + 수동 bottom 패딩 중복. 콘텐츠 루트는 `LocalSafeContentPadding` 위주로, 추가는 6~12dp만.
- 키보드(IME) 열릴 때 배너/콘텐츠 겹침
  - BaseScreen의 인셋 계산을 사용(IME 열릴 때만 IME inset 반영). 별도 수동 인셋을 중복하지 말 것.

---

# QA 체크리스트 (Cross‑App)
- [ ] 배너 컨테이너가 광고 전/후 동일 높이(예측값)로 유지되어 레이아웃 점프가 없는가?
- [ ] 회전/해상도 변경 시 높이가 자연스럽게 업데이트되는가?
- [ ] 전역 배너 위 간격은 0dp인가? 화면별 완충(6~12dp)은 과도하지 않은가?
- [ ] IME 열림/닫힘, 내비 바 유무에서 콘텐츠/배너가 겹치지 않는가?
- [ ] 테스트 광고 라벨(디버그)이 정상 노출되는가?

---

# 참고 구현 파일 (본 저장소)
- 전역 상수: `core/ui/LayoutConstants.kt` (BANNER_TOP_GAP=0.dp, BANNER_MIN_HEIGHT)
- 공용 예측 함수: `core/ui/StandardScreen.kt` (`predictAnchoredBannerHeightDp`)
- 공통 레이아웃: `core/ui/BaseActivity.kt` (BaseScreen: 배너 슬롯/인셋/헤어라인/예측 높이)
- 화면 예시: `feature/detail/DetailActivity.kt`, `feature/addrecord/AddRecordActivity.kt`

---

# 요약
- 전역 갭 0dp + 예측 높이 “초기 고정 예약” + 상단 헤어라인 = 흔들림 없는 안정적인 배너 UX. 배너는 배너 역할(고정/인셋/구분선), 여백은 화면이 책임(가벼운 완충).
