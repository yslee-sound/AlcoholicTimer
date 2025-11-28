# Flat UI + Window Insets/IME 통합 가이드

> 문서 버전
> - 버전: v1.0.0
> - 최근 업데이트: 2025-10-24
> - 변경 요약: 플랫 UI(헤어라인 + 0dp)와 Insets/IME 정책을 하나의 문서로 통합. "하단 여백 소유자 단일화" 원칙과 화면 타입별 결정 트리(A/B/C)를 중심으로 일관된 구현 기준 정리. 금지 조합/체크리스트/스니펫/수용 기준 포함.

## 현재 프로젝트 적용 현황
- A) 하단 버튼 화면: `StandardScreenWithBottomButton`에서 `effectiveBottom = max(WindowInsets.navigationBars, WindowInsets.ime)`만으로 버튼 패딩/콘텐츠 예약을 처리. Start/Run/Quit 화면에 적용됨.
- B) 입력(IME) 화면: `NicknameEditActivity`에 권장안(선택 1) 적용 완료. `bottom = max(navBars, IME) + 16.dp`를 단 한 번만 적용하며 `imePadding()`은 사용하지 않음.
- C) 일반 스크롤/리스트 화면: `RecordsScreen`, `SettingsScreen` 등에서 스크롤 컨테이너에 `bottom = WindowInsets.navigationBars + 16.dp`를 단 한 번만 적용.
- 금지 조합 준수: imePadding + (navBars + 16dp) 혼용 및 부모/자식/Spacer 중복 하단 여백 미사용.

목표
- 전 앱 화면의 외곽 스타일(Flat: elevation 0dp + Hairline border)과 Window Insets/IME 처리(Edge-to-Edge, 입력/하단 버튼 대응)를 단일 규칙으로 정리한다.
- 3버튼/제스처 내비, 키보드(IME) 등장/퇴장, 드로어 등 다양한 상황에서도 하단 여백 과다·겹침 없이 일관된 레이아웃을 유지한다.

핵심 원칙
- 하단 여백 소유자 단일화: 같은 화면에서 "하단 여백"은 단 한 곳에서만 계산·적용한다.
- 전역 루트는 수평만: 전역(root) 컨테이너는 WindowInsets.safeDrawing.only(Horizontal)만 적용하고, 하단은 화면/컴포넌트가 소유한다.
- 스타일과 레이아웃 분리: 스타일(헤어라인, 0dp, 톤)은 어디서든 동일하고, 하단 여백은 화면 유형에 따라 계산 주체만 달라진다.

디자인 토큰(요약)
- Elevation
  - AppElevation.CARD = 0.dp
  - AppElevation.CARD_HIGH = 2.dp // 강조 원형 버튼
- Border
  - AppBorder.Hairline = 0.75.dp
- Color
  - color_border_light (라이트: #E5E8EC, 다크: outlineVariant 등 저대비 아웃라인 매핑)

공통 카드(AppCard) 기본값
- elevation = AppElevation.CARD (0.dp)
- border = BorderStroke(AppBorder.Hairline, colorResource(R.color.color_border_light))
- shape/interaction은 제품 정책 유지

화면 타입별 결정 트리(하단 여백 소유자)
- A) 하단 고정 버튼 화면
  - 소유자: 버튼 화면 레이아웃(예: StandardScreenWithBottomButton)
  - 계산: effectiveBottom = max(WindowInsets.navigationBars, WindowInsets.ime)
  - 처리: 콘텐츠 하단 예약과 버튼 패딩을 effectiveBottom 기반으로만 적용
  - 금지: 스크롤 컨테이너에 추가 bottom 패딩/Spacer 금지
- B) 입력(IME) 화면(하단 버튼 없음)
  - 선택 1(권장): bottom = max(navBars, IME) + 16.dp를 한 번만 적용(imePadding 혼용 금지)
  - 선택 2(간단): imePadding 단독 적용(별도의 nav+16 미적용)
- C) 일반 스크롤/리스트 화면(IME/하단 버튼 없음)
  - 소유자: 스크롤 컨테이너(Column/LazyColumn)
  - 계산: bottom = WindowInsets.navigationBars + 16.dp
  - 적용: 단 한 번만 적용(부모/자식/Spacer 중복 금지)

전역 인셋 정책
- Edge-to-Edge: WindowCompat.setDecorFitsSystemWindows(window, false) (onCreate 1회)
- 상단(statusBars): AppBar(TopAppBar) 컨테이너에만 적용
- 수평: 전역 루트에 WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
- 하단: 전역 루트에 적용하지 않음(화면 유형 A/B/C 중 해당 소유자가 처리)

드로어(ModalNavigationDrawer) + IME
- 드로어 오픈 시작 시 즉시 포커스 해제 + 키보드 숨김
- 드로어 시트에 statusBarsPadding + navigationBarsPadding 적용
- 애니메이션 시작부터 입력 가드 활성화, 닫힘 직후 짧은 그레이스 타임 동안 입력 소비

금지 조합(반패턴)
- imePadding + (navigationBars + 16dp) 동시 적용
- 하단 고정 버튼 예약 로직 + 스크롤 컨테이너 bottom 패딩 동시 적용
- 부모 컨테이너 하단 인셋 + 자식 스크롤 하단 패딩 중복 적용
- 전역 루트에 systemBarsPadding()/safeDrawingPadding()의 하단을 무차별 적용
- 스크롤 컨테이너에 이미 bottom 패딩을 줬는데 말단 Spacer로 여백을 추가 확보

Compose 스니펫(요약)
- 전역 루트
```kotlin
// AppBar
Surface(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) { ... }
// 콘텐츠 루트(전역)
val rootModifier = Modifier.windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
)
```
- 일반 스크롤/리스트(C)
```kotlin
val topGap = 16.dp
val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
val bottomGap = navBottom + topGap
LazyColumn(
  modifier = Modifier.fillMaxSize(),
  contentPadding = PaddingValues(bottom = bottomGap)
) { /* items */ }
```
- 입력 화면(B) — 권장안
```kotlin
val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
val bottomGap = max(navBottom, imeBottom) + 16.dp
Column(Modifier.fillMaxSize().padding(bottom = bottomGap)) { /* content */ }
```
- 하단 버튼 화면(A)
```kotlin
val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
val effectiveBottom = max(navBottom, imeBottom)
val buttonSize = 96.dp
val extraGap = 32.dp
val reservedBottom = (buttonSize / 2) + extraGap + effectiveBottom
Column(Modifier.padding(bottom = reservedBottom)) { /* content */ }
Box(Modifier.align(Alignment.BottomCenter).padding(bottom = effectiveBottom + 24.dp)) { /* button */ }
```
- 드로어 + IME: 상태 변화 시 포커스/키보드 정리 및 시트 패딩, 입력 가드 패턴(상세 코드는 별첨 문서 참고 가능)

수용 기준(Acceptance Criteria)
- 전역 카드/컨테이너가 elevation 0dp + Hairline 테두리로 통일
- A/B/C 화면 규칙에 따라 하단 여백이 단일 소유자에서 한 번만 계산·적용됨(중복 없음)
- 3버튼 기기에서 콘텐츠가 시스템 바와 겹치지 않으며, 제스처 내비에서도 최소 16dp가 유지됨
- 입력/하단 버튼 화면에서 키보드/버튼 겹침이 발생하지 않음
- 드로어 오픈 시 포커스/키보드 정리 및 시트 패딩으로 가려짐/클릭 스루 없음

QA 체크리스트
- 일반 리스트 화면: 제스처/3버튼 모두 바닥 여백 적절, Spacer 중복 없음
- 입력 화면: imePadding과 bottom 패딩 혼용 없음, 키보드 시 과도한 띄움/겹침 없음
- 하단 버튼 화면: 스크롤 컨테이너에 추가 bottom 없음, 버튼/콘텐츠 간 이격 정상
- 드로어: 포커스 해제/키보드 숨김 동작, 시트 패딩/입력 가드 정상

마이그레이션 가이드(기존 개별 문서 → 통합)
- a_FLAT_UI_BASE_PROMPT.md: 하단 여백 규칙은 본 통합 문서의 A/B/C 원칙을 따른다.
- INSETS_AND_IME_GUIDE.md 및 a_INSETS_AND_IME_GUIDE.md: 일반 스크롤/리스트는 navBars + 16dp, 입력/하단 버튼은 max(nav, IME) 기반 단일 소유자로 정리.
- 혼용/중복을 제거한 뒤 스모크 테스트(3버튼/제스처, IME on/off, 드로어).

변경 이력
- v1.0.0 (2025-10-24)
  - 초판: 플랫 UI + Insets/IME 통합, 소유자 단일화/화면 타입 트리/금지 조합/스니펫/수용 기준/QA 체크 포함
