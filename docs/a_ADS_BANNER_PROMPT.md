# 변경 이력 (Banner Ads)
- 2025-10-25 v1.2.0
  - **배너 상단 헤어라인(구분선) 추가**: 모든 배너 광고 컨테이너 상단에 얇은 구분선 적용
    - 두께: `AppBorder.Hairline` (약 0.5dp)
    - 색상: `#E0E0E0` (은은한 회색)
    - 적용 위치: BaseActivity, StandardScreen, DetailActivity, AddRecordActivity 등 모든 배너 컨테이너
    - 목적: 배너 영역을 콘텐츠와 시각적으로 명확히 구분하여 UX 개선
- 2025-10-25 v1.2.0
  - **배너 상단 헤어라인(구분선) 추가**: 모든 배너 광고 컨테이너 상단에 얇은 구분선 적용
    - 두께: `AppBorder.Hairline` (약 0.5dp)
    - 색상: `#E0E0E0` (은은한 회색)
    - 적용 위치: BaseActivity, StandardScreen, DetailActivity, AddRecordActivity 등 모든 배너 컨테이너
- 2025-10-25 v1.2.0
  - **배너 상단 헤어라인(구분선) 추가**: 모든 배너 광고 컨테이너 상단에 얇은 구분선 적용
    - 두께: `AppBorder.Hairline` (약 0.5dp)
    - 색상: `#E0E0E0` (은은한 회색)
- 2025-10-25 v1.2.0
  - **배너 상단 헤어라인(구분선) 추가**: 모든 배너 광고 컨테이너 상단에 얇은 구분선 적용
    - 두께: `AppBorder.Hairline` (약 0.5dp)
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

- **배너 상단 구분선**: 모든 배너 컨테이너 상단에 헤어라인(`AppBorder.Hairline`, #E0E0E0) 적용하여 콘텐츠와 광고 영역을 명확히 구분합니다.
- 전역 기본: 배너 위 전역 간격은 0dp(BANNER_TOP_GAP=0.dp). 광고 영역은 "화면 최하단 + 내비/IME 인셋 고려"만 담당합니다.
- 화면별 완충 여백: "딱 달라붙음"을 완화하고 싶을 때만 화면 콘텐츠 쪽에 소량의 내부 하단 패딩(권장 6~12dp, 보통 8dp)을 추가합니다.
- 필요 시 파라미터 사용: 특정 화면에서만 배너 위에 여백을 주려면 `BaseScreen(bannerTopGap = X.dp)`를 사용하되, 전역 기본은 0dp로 유지합니다.
- 인셋 일관성: 배너 컨테이너는 내비/IME 인셋을 자체 처리, 콘텐츠 안전 패딩(LocalSafeContentPadding)은 배너가 있을 때 내비 인셋과 전역 추가여백을 중복 적용하지 않습니다.

---

# 적용 레시피 (패턴 모음)
## 배너 컨테이너 기본 패턴 (헤어라인 포함)

**BaseActivity/BaseScreen 사용 시** (자동 적용됨):
```kotlin
// BaseScreen 내부에서 자동으로 헤어라인 렌더링
BaseScreen(bottomAd = { AdmobBanner() }) { /* content */ }
```

**커스텀 레이아웃에서 배너를 직접 배치할 때**:
```kotlin
Column(modifier = Modifier.fillMaxSize()) {
    // 상단 콘텐츠 영역
    Box(modifier = Modifier.weight(1f)) { /* content */ }
    
    // 배너 상단 헤어라인
    HorizontalDivider(
        thickness = AppBorder.Hairline,
        color = Color(0xFFE0E0E0)
    )
    
    // 배너 컨테이너
    Surface(color = Color.White) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = effectiveBottom)
                .heightIn(min = LayoutConstants.BANNER_MIN_HEIGHT),
            contentAlignment = Alignment.Center
        ) { AdmobBanner() }
    }
}
```

**StandardScreenWithBottomButton 패턴**:
```kotlin
// 하단 버튼이 있는 화면에서도 헤어라인 자동 적용
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

2) 스크롤 목록(LazyColumn) — 배너 위 최소 여백 8dp
```kotlin
LazyColumn(
    /* ... */,
    contentPadding = PaddingValues(bottom = 8.dp)
) { /* items */ }
```

3) 스크롤 컬럼(Column + verticalScroll) — 배너 위 최소 여백 8dp
```kotlin
Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(LocalSafeContentPadding.current)
        .padding(bottom = 8.dp) // 콘텐츠 내부 하단 완충
) { /* children */ }
```

4) 스크롤이 불필요할 때(내용이 짧음) — 남는 높이를 채우면서 8dp 보장
```kotlin
val viewportH by remember { mutableStateOf(0) }
val contentH = /* 측정값 합산 */
val filler = (viewportH - contentH).coerceAtLeast(0)
- **헤어라인 구분선**: 모든 배너 상단에 얇은 회색 선(#E0E0E0)이 표시되는가?
  - BaseScreen 사용 화면 (설정, 앱 정보, 프로필 편집 등)
  - StandardScreenWithBottomButton 사용 화면 (레벨, 금주 진행, 종료 확인 등)
  - 커스텀 레이아웃 화면 (기록 상세, 기록 추가 등)
val fillerDp = with(LocalDensity.current) { filler.toDp() } + 8.dp
- 디자인 토큰: `core/ui/AppBorder.kt` (Hairline 두께 정의)
- **헤어라인 구분선**: 모든 배너 상단에 `AppBorder.Hairline` (#E0E0E0) 적용으로 광고 영역을 시각적으로 명확히 구분
- 전역 갭 0dp, 화면별 보강(6~12dp) — 이 원칙만 지키면 어떤 앱에도 안정적으로 재사용 가능합니다
- 배너는 배너 역할(고정/인셋/구분선), 여백은 화면이 책임(가벼운 완충)합니다
- BaseScreen/StandardScreen 사용 시 헤어라인 자동 적용, 커스텀 레이아웃에서는 HorizontalDivider를 배너 위에 수동 배치
  - `core/ui/BaseActivity.kt` (BaseScreen: 배너 슬롯, 인셋 처리, bannerTopGap 파라미터, **헤어라인 렌더링**)
  - `core/ui/StandardScreen.kt` (StandardScreenWithBottomButton: 하단 버튼 화면용, **헤어라인 렌더링**)
- 커스텀 배너 배치 예시:
  - `feature/detail/DetailActivity.kt` (직접 Column 구성, **헤어라인 수동 추가**)
  - `feature/addrecord/AddRecordActivity.kt` (Scaffold + 고정 배너, **헤어라인 수동 추가**)
```

- 헤어라인이 배너 폭 전체에 걸쳐 균일하게 표시되는가? (화면 좌우 여백 없이)
5) 특정 화면만 배너 위 간격을 직접 주고 싶을 때
```kotlin
BaseScreen(bottomAd = { AdmobBanner() }, bannerTopGap = 8.dp) { /* content */ }
```

권장 우선순위: 콘텐츠 내부 패딩(2,3,4) → (필요 시) 화면별 bannerTopGap(5) → 전역 기본은 0 유지.

---

# 화면별 적용 예시 (본 앱)

- 금주 레벨 화면: Column + verticalScroll, 콘텐츠 내부 하단 패딩 8dp 적용 → 맨 아래 카드가 배너와 자연스럽게 분리됨.
- 설정 화면: LazyColumn의 `contentPadding.bottom = 8.dp` + (스크롤 불필요 시) filler + 8dp → 스크롤 여부와 무관하게 항상 8dp 확보.
- 전역 배너 간격: 0dp → 배너 자체 높이/인셋만 반영, 여백은 화면이 책임.

---

# 마이그레이션 가이드

- 종전 전역 간격(12/16dp)을 쓰던 앱 → BANNER_TOP_GAP=0.dp로 낮춘 후, 화면 성격에 따라 6~12dp를 콘텐츠에만 추가.
- BaseScreen 사용 시
  - 이미 `LocalSafeContentPadding`을 적용했다면 “콘텐츠 쪽 하단 패딩”만 얹으면 됨.
  - 기존에 `.padding(bottom = xx.dp)`와 전역 인셋이 중복된다면 중복 제거.
- 비‑BaseScreen 앱
  - 배너가 고정인 레이아웃에서: 배너 위 Spacer 제거(0dp), 콘텐츠 컨테이너 쪽에만 여백 추가.

---

# 트러블슈팅

- 배너 위에 회색 띠가 크게 남는다
  - 원인: 전역 안전 패딩/인셋 + 수동 bottom 패딩이 중복.
  - 조치: 콘텐츠 루트에서는 `LocalSafeContentPadding`만, 추가 여백이 필요하면 별도의 “시각적 완충(6~12dp)”만 더한다.

- 스크롤 끝에서 배너와 아이템이 붙는다
  - LazyColumn: `contentPadding.bottom = 8.dp` 추가.
  - Column + verticalScroll: `.padding(bottom = 8.dp)` 추가.
  - 내용이 짧아 스크롤이 없을 땐 filler Spacer에 `+ 8.dp` 더하기.

- 키보드(IME) 열릴 때 배너/콘텐츠가 겹친다
  - BaseScreen의 인셋 계산을 사용(IME 열릴 때만 IME inset 반영). 별도 수동 인셋을 중복하지 말 것.

---

# QA 체크리스트 (Cross‑App)
- 배너 영역 고정(Anchored Adaptive): 회전/해상도 변화에서도 광고 높이가 안정적인가?
- 전역 배너 위 간격은 0dp인가? 화면별 완충(6~12dp)은 과도하지 않고 일관적인가?
- IME 열림/닫힘, 내비 바 유무에서 콘텐츠/배너가 겹치지 않는가?
- 테스트 광고 라벨(디버그)이 정상 노출되는가?

---

# 참고 구현 파일 (본 저장소)
- 전역 상수: `core/ui/LayoutConstants.kt` (BANNER_TOP_GAP=0.dp, BANNER_MIN_HEIGHT)
- 공통 레이아웃: `core/ui/BaseActivity.kt` (BaseScreen: 배너 슬롯, 인셋 처리, bannerTopGap 파라미터)
- 화면 예시: `feature/level/LevelActivity.kt`(8dp 내부 패딩), `feature/settings/SettingsActivity.kt`(LazyColumn bottom 8dp)

---

# 요약
- 전역 갭 0dp, 화면별 보강(6~12dp) — 이 원칙만 지키면 어떤 앱에도 안정적으로 재사용 가능합니다. 배너는 배너 역할(고정/인셋), 여백은 화면이 책임(가벼운 완충)합니다.
