# Window Insets & IME 처리 가이드

목표: 3버튼/제스처 내비게이션과 다양한 키보드(IME) 환경에서 하단 여백 과다/겹침 없이 안정적인 레이아웃을 유지한다.

이 문서는 AlcoholicTimer 앱에 적용한 실제 수정 내역과, 동일한 베이스를 사용하는 다른 앱에 확장 적용할 수 있는 정책/체크리스트/스니펫을 제공한다.

---

## 1) 이번 리포지토리에 적용한 변경 내역 요약

변경 목적: 3버튼 내비 기기에서 "하단 여백 과도" 문제 제거, 입력 화면에서 키보드가 버튼/콘텐츠를 가리는 문제 예방.

핵심 원칙
- 전역(root 컨테이너)에서는 하단(bottom) safe area를 강제로 추가하지 않는다.
- 수평(horizontal) 인셋만 전역 적용하고, 상단(statusBars)은 AppBar에만 한정 적용한다.
- 입력 화면은 IME(키보드) 등장 시 겹침이 없도록 `imePadding()` 또는 IME 하단 인셋을 고려한 계산을 사용한다.

구체 수정
- app/src/main/java/com/example/alcoholictimer/feature/records/components/AllRecords.kt
  - 루트 Box의 `windowInsetsPadding(WindowInsets.safeDrawing.only(...))`에서 Bottom 제거 → 수평만 유지.
  - 빈 상태 컴포저블 이름 충돌 해결: `EmptyRecordsState` → `AllRecordsEmptyState`로 변경.
- app/src/main/java/com/example/alcoholictimer/core/ui/StandardScreen.kt
  - `StandardScreenWithBottomButton`에서 하단 버튼과 콘텐츠 이격 계산 시 네비게이션 바 높이(nav)와 IME 하단 높이(ime) 중 더 큰 값을 사용(effectiveBottom)하도록 개선.
  - `reservedBottom = (buttonSize/2) + extraGap + effectiveBottom`로 콘텐츠 하단을 예약하고, 버튼 패딩도 `effectiveBottom + 24.dp`로 조정.
- app/src/main/java/com/example/alcoholictimer/feature/profile/NicknameEditActivity.kt
  - 루트 Column에 `imePadding()` 추가. 키보드가 열릴 때 버튼이 가려지지 않도록 함.
- 전역 BaseScreen 정책(참고: BaseActivity.kt)
  - AppBar에는 `WindowInsets.statusBars`만 적용.
  - 콘텐츠 루트에는 `WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)`만 적용(하단 제외).

빌드/검증
- Debug Kotlin 컴파일 성공. 변경과 무관한 경고(Deprecated statusBarColor/navigationBarColor 등)만 존재.

---

## 2) 정책 및 베스트 프랙티스

- Edge-to-Edge 기본 설정
  - `WindowCompat.setDecorFitsSystemWindows(window, false)`를 onCreate에서 한 번만 설정.
- 전역 인셋 정책
  - 상단: AppBar(TopAppBar) 컨테이너에만 `WindowInsets.statusBars` 적용.
  - 하단: 전역 루트에서는 적용하지 않음. 하단 여백이 필요한 화면에서만 개별적으로 처리.
  - 수평: 전역 루트 컨테이너에 `WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)` 적용.
- 리스트/상세 화면
  - 루트에 하단 safeDrawing 금지. 스크롤 컨텐츠는 자체 스크롤 패딩으로 해결.
- 입력 화면(키보드 등장)
  - 단순: 루트 컨테이너에 `imePadding()`.
  - 하단 고정 버튼이 있는 화면: 네비게이션 바 vs IME 하단 인셋 중 큰 값 사용.
- 금지 패턴
  - 전역(모든 화면)에 `systemBarsPadding()` 또는 `safeDrawingPadding()` 하단을 무차별 적용.
  - 같은 화면에서 서로 다른 레벨의 컨테이너가 각각 하단 인셋을 중복 적용.

---

## 3) 스니펫 모음

1) 전역 BaseScreen 예시

```
// AppBar
Surface(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) { ... }

// 콘텐츠 루트(전역)
Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
```

2) 입력 화면(간단)

```
Column(Modifier.fillMaxSize().imePadding()) { ... }
```

3) 하단 고정 버튼이 있는 화면

```
val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
val effectiveBottom = if (navBarBottom > imeBottom) navBarBottom else imeBottom

// 콘텐츠 하단 예약
val buttonSize = 96.dp
val extraGap = 32.dp
val reservedBottom = (buttonSize / 2) + extraGap + effectiveBottom

Column(Modifier.padding(bottom = reservedBottom)) { ... }

// 버튼
Box(Modifier.align(Alignment.BottomCenter).padding(bottom = effectiveBottom + 24.dp)) { ... }
```

4) 리스트 화면(루트 하단 safeDrawing 금지)

```
Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
  LazyColumn(
    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
    // ...
  )
}
```

---

## 4) 리포 내 전수 점검 체크리스트

- [ ] `windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom ...))` 사용 제거
- [ ] 전역 루트는 수평만 적용, 하단은 금지
- [ ] AppBar 컨테이너에만 `WindowInsets.statusBars` 적용
- [ ] 입력 화면 루트에 `imePadding()` 적용
- [ ] 하단 고정 버튼 화면에서 IME/네비 비교 후 큰 값 사용
- [ ] insets 중복 적용 지점 제거(부모/자식 중복)
- [ ] Debug 빌드 컴파일 확인

검증 커맨드 예시(Windows, Debug Kotlin만 빠르게)

```
./gradlew.bat :app:compileDebugKotlin --console=plain -x lint -x test
```

---

## 5) 다른 앱으로의 이식 절차(요약)

1) 전역 설정 확인: DecorFitsSystemWindows(false) 적용 여부 확인.
2) 전역 BaseScreen 또는 공통 레이아웃에서 하단 safeDrawing 제거, 수평만 유지.
3) AppBar에만 statusBars 인셋 적용.
4) 입력 화면에는 imePadding 적용.
5) 하단 고정 버튼 화면은 IME/네비 합성 하단 여백 사용.
6) 전수 검색 키워드: `safeDrawing`, `systemBarsPadding`, `windowInsetsPadding`, `imePadding`, `navigationBarsPadding`, `statusBarsPadding`.
7) 빌드/스모크 테스트.

---

## 6) 트러블슈팅

- 3버튼 기기에서 여백이 남음: 해당 화면 루트에 Bottom 인셋이 남아있는지 확인(전역/로컬 중복 포함).
- 키보드가 버튼을 가림: 루트에 `imePadding()` 누락 또는 하단 버튼 패딩에서 IME 인셋 미반영.
- 특정 OEM 키보드에서 늦게 반영: 패딩이 애니메이션 중 갱신될 수 있으므로 실제 기기에서 재확인.

---

## 7) 변경 이력(요약)

- 2025-10-13: AlcoholicTimer에 전역 인셋 정책 정비, AllRecords 하단 인셋 제거, 하단 버튼/IME 대응 개선, 닉네임 편집 화면 imePadding 추가.

