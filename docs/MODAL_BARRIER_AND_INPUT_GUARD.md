# Modal Barrier & Input Guard Guide (Compose)

목표
- 드로어/모달이 열려 있거나 애니메이션 중일 때, 배경의 모든 입력(탭/더블탭/롱프레스/스크롤/플링)이 절대 전달되지 않도록 보장합니다.
- 드로어 닫힘 직후에도 짧은 그레이스 기간을 둬 클릭 스루(click-through)가 발생하지 않도록 방지합니다.
- 다이얼로그의 바깥 터치/백 버튼 동작을 정책(canDismiss)에 맞게 명시적으로 제어합니다.

성공 기준(Acceptance Criteria)
- 스크림 영역 탭 시: 드로어는 닫히지만 배경 onClick/onLongClick/scroll 콜백은 호출되지 않습니다.
- 애니메이션 중/닫힘 직후: 입력이 배경으로 전달되지 않습니다(특히 Up 이벤트 유출 방지).
- 접근성: 드로어/모달이 열려 있으면 배경 포커스 이동이 차단됩니다. (필요시 포커스 트랩 적용)

구현 개요
1) Drawer(ModalNavigationDrawer) 입력 가드
- 드로어 상태를 `snapshotFlow`로 관찰하여 입력 가드를 활성화합니다.
- 활성 조건: 드로어가 열림 상태이거나, 애니메이션 중이거나, 닫힘 직후 그레이스 타임.
- 포인터 입력은 전면 오버레이에서 `pointerInput` 루프로 전부 소비합니다.

핵심 스니펫
```kotlin
// imports
import androidx.compose.ui.input.pointer.*

// 상태
var drawerInputGuardActive by remember { mutableStateOf(false) }
val drawerGuardGraceMs = 200L
LaunchedEffect(drawerState) {
    snapshotFlow { Triple(drawerState.isAnimationRunning, drawerState.currentValue, drawerState.targetValue) }
        .collect { (isAnimating, current, target) ->
            if (isAnimating || current != DrawerValue.Closed || target != DrawerValue.Closed) {
                drawerInputGuardActive = true
            } else {
                // 닫힘 직후 잠시 입력을 소비하여 클릭 스루 방지
                drawerInputGuardActive = true
                delay(drawerGuardGraceMs)
                drawerInputGuardActive = false
            }
        }
}

// 오버레이: 활성 시 모든 포인터 이벤트 소비
if (drawerInputGuardActive /* || 전역 입력 락 등 필요 조건 병합 */) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(drawerInputGuardActive) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    )
}
```

권장 옵션
- 그레이스 기간: 150~250ms 범위에서 기기별 체감에 따라 조정(기본 200ms).
- 필요 시 글로벌 입력 락(CompositionLocal)과 조건 병합: 비동기 네비게이션/애니메이션 경계에서 안정성 ↑

2) Dialog(예: AppUpdateDialog) 모달 배리어
- 바깥 터치/백 버튼으로 닫힘 여부를 `canDismiss` 정책으로 제어합니다.

```kotlin
Dialog(
    onDismissRequest = { if (canDismiss) onDismiss() },
    properties = DialogProperties(
        dismissOnBackPress = canDismiss,
        dismissOnClickOutside = canDismiss
    )
) { /* ... */ }
```

접근성(Accessibility)
- TalkBack 활성화 시 드로어/모달이 열리면 배경 포커스 이동이 불가능해야 합니다.
- 스크림(배리어)에 의미 있는 접근성 액션(예: 닫기)을 제공하거나, 닫기 액션은 명시적 버튼으로 제공합니다.

QA 체크리스트
- Show taps / Pointer location 켜고 다음 제스처에서 배경 반응이 없는지 확인:
  - 단일 탭, 빠른 더블탭, 길게 누르기, 수평·수직 스크롤, 매우 느린 탭(Down-지연-Up)
- 드로어 열기 → 스크림 탭으로 닫기 → 닫힘 직후 200ms 내 입력이 배경으로 전달되지 않음
- 스크롤 가능한 리스트/지도 화면에서도 동일하게 배경 움직임 없음 확인
- canDismiss=false 다이얼로그: 바깥 터치·백 버튼으로 닫히지 않음

마이그레이션 가이드(다른 앱에 적용)
1) Base 화면 컴포저블에 입력 가드 상태/로직 추가(위 스니펫 참조)
2) 드로어가 있는 화면에서 블러/스크림 뒤 전면 오버레이로 포인터 입력 소비
3) 다이얼로그 구현에 `DialogProperties` 적용
4) QA 시나리오 자동화 또는 수동 체크리스트 수행

문제 해결 팁
- 클릭만 막히고 스크롤이 먹는다면: `clickable` 대신 `pointerInput`로 전 이벤트를 소비하도록 전환
- 드문 클릭 스루 발생: 그레이스 기간을 소폭 확대(최대 250~300ms) 후 재검증
- Compose 버전 차이: `consume()`가 경고 시 `consumeAllChanges()`로 대체 고려

버전
- 본 정책은 1.0.1 이후 기본 가이드로 채택됩니다(CHANGELOG 참조).
