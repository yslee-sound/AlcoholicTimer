# 📱 HorizontalPager 기반 다중 타이머 구조 구현 완료

**작성일:** 2026-01-05  
**작업자:** 안드로이드 Compose UI 전문 개발자  
**목적:** 단일 타이머 화면을 좌우 스와이프 가능한 다중 타이머 구조로 변경

---

## 🎯 구현 목표

✅ **완료된 항목:**
1. ✅ 사용자가 메인 타이머 카드를 좌우로 스와이프 가능
2. ✅ 마지막 페이지에 **새로운 타이머 추가 '+' 카드** 표시
3. ✅ 타이머는 최대 **3개**까지만 생성 가능
4. ✅ 카드 하단에 현재 페이지 위치를 알려주는 **인디케이터(Indicator)** 추가

---

## 📊 구현 내용

### 1️⃣ ViewModel 데이터 구조 변경

**파일:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/viewmodel/Tab01ViewModel.kt`

#### 추가된 항목:

```kotlin
// [NEW] 타이머 데이터 클래스
data class TimerData(
    val id: Int,
    val name: String,
    val startTime: Long,
    val targetDays: Float,
    val isCompleted: Boolean
)

// [NEW] 다중 타이머 리스트 상태 (최대 3개)
private val _timers = MutableStateFlow<List<TimerData>>(emptyList())
val timers: StateFlow<List<TimerData>> = _timers.asStateFlow()

// [NEW] 현재 선택된 타이머 인덱스
private val _currentTimerIndex = MutableStateFlow(0)
val currentTimerIndex: StateFlow<Int> = _currentTimerIndex.asStateFlow()
```

#### 추가된 함수:

1. **`initializeTimerList()`**
   - 기본 '금주 타이머' 1개를 생성하여 리스트 초기화

2. **`addNewTimer()`**
   - 새로운 타이머 추가 (최대 3개 제한)
   - TODO: 향후 DB 연동 필요

3. **`setCurrentTimerIndex(index: Int)`**
   - 현재 선택된 타이머 인덱스 변경

---

### 2️⃣ RunScreen UI 구조 변경

**파일:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/screens/RunScreen.kt`

#### 주요 변경사항:

##### A. Import 추가
```kotlin
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
```

##### B. Pager 상태 초기화
```kotlin
// [NEW] 타이머 리스트 상태 구독
val timers by viewModel.timers.collectAsState()
val currentTimerIndex by viewModel.currentTimerIndex.collectAsState()

// [NEW] Pager 상태 초기화
val showAddButton = timers.size < 3
val pageCount = timers.size + if (showAddButton) 1 else 0
val pagerState = rememberPagerState(
    initialPage = currentTimerIndex,
    pageCount = { pageCount }
)

// [NEW] Pager 페이지 변경 감지
LaunchedEffect(pagerState.currentPage) {
    if (pagerState.currentPage < timers.size) {
        viewModel.setCurrentTimerIndex(pagerState.currentPage)
    }
}
```

##### C. HorizontalPager 구조
```kotlin
HorizontalPager(
    state = pagerState,
    contentPadding = PaddingValues(horizontal = 0.dp),
    pageSpacing = 16.dp,
    modifier = Modifier.fillMaxWidth()
) { page ->
    if (page < timers.size) {
        // [기존 타이머 카드]
        ExistingTimerCard(...)
    } else {
        // [새 타이머 추가 카드]
        AddTimerCard(
            onClick = { viewModel.addNewTimer() }
        )
    }
}
```

##### D. 인디케이터 추가
```kotlin
// [NEW] Pager 인디케이터
if (pageCount > 1) {
    PagerIndicator(
        pageCount = pageCount,
        currentPage = pagerState.currentPage
    )
}
```

---

### 3️⃣ 새로운 Composable 함수

#### A. **ExistingTimerCard()** - 기존 타이머 카드
```kotlin
@Composable
private fun ExistingTimerCard(
    timerData: TimerData,
    displayElapsedMillis: Long,
    targetDays: Float,
    elapsedDaysFloat: Float,
    remainingDays: Int,
    progressTimeText: String,
    progress: Float,
    modifier: Modifier = Modifier
)
```

**특징:**
- 기존 타이머 카드 UI를 그대로 유지
- 배경 이미지 (bg9), 경과 일수, 진행률 바 포함
- 모래시계 아이콘 + 남은 일수 표시

---

#### B. **AddTimerCard()** - 타이머 추가 카드
```kotlin
@Composable
private fun AddTimerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**디자인 요구사항:**
- ✅ 기존 타이머 카드와 동일한 크기 (260.dp)
- ✅ 배경: 연한 회색 (0xFFF5F5F5)
- ✅ 테두리: 2dp, 0xFFE0E0E0
- ✅ 아이콘: 큰 `+` 아이콘 (64.dp, Icons.Default.Add)
- ✅ 문구: "Start a new timer" (Body style)
- ✅ 동작: 클릭 시 `viewModel.addNewTimer()` 호출

---

#### C. **PagerIndicator()** - 페이지 인디케이터
```kotlin
@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
)
```

**스타일:**
- ✅ 작은 원형 점(Dot) - 8.dp
- ✅ 활성 페이지: 진한 파란색 (0xFF1E40AF)
- ✅ 비활성 페이지: 연한 회색 (0xFFBDBDBD)
- ✅ 카드와 하단 콘텐츠 사이 배치 (50.dp 높이)

---

### 4️⃣ strings.xml 추가

**파일:** `app/src/main/res/values/strings.xml`

```xml
<!-- [NEW] Multiple Timers (2026-01-05) -->
<string name="add_new_timer_message">Start a new timer</string>
```

---

## 📐 UI 레이아웃 구조

```
Column (Vertical Scroll)
├─ Spacer (20dp)
├─ HorizontalPager ← [NEW]
│  ├─ Page 0: ExistingTimerCard (기본 타이머)
│  ├─ Page 1: ExistingTimerCard (타이머 2) [if exists]
│  ├─ Page 2: ExistingTimerCard (타이머 3) [if exists]
│  └─ Page N: AddTimerCard (+ 버튼) [if timers.size < 3]
├─ Spacer (12dp)
├─ PagerIndicator ← [NEW]
├─ Spacer (16dp)
├─ NativeAdItem (광고)
├─ Spacer (10dp)
├─ QuoteDisplay (명언)
├─ Spacer (26dp)
├─ ModernStopButtonSimple (포기 버튼)
└─ Spacer (100dp)
```

---

## 🔧 기술적 세부사항

### Pager 설정
| 속성 | 값 | 설명 |
|------|-----|------|
| `pageCount` | `timers.size + (showAddButton ? 1 : 0)` | 동적 페이지 수 |
| `initialPage` | `currentTimerIndex` | 마지막 선택된 페이지 |
| `contentPadding` | `0.dp` | 양옆 패딩 없음 |
| `pageSpacing` | `16.dp` | 카드 간격 |

### 제약사항
- ✅ 최대 타이머 개수: **3개**
- ✅ `if (_timers.value.size >= 3) { return }` 체크
- ✅ 3개 도달 시 '+' 카드 자동 숨김

---

## 🧪 테스트 시나리오

### 1. 초기 상태 (타이머 1개)
```
[타이머 1] [+ 버튼]
  ●  ○
```

### 2. 타이머 2개 추가 후
```
[타이머 1] [타이머 2] [+ 버튼]
  ●  ○  ○
```

### 3. 타이머 3개 (최대)
```
[타이머 1] [타이머 2] [타이머 3]
  ●  ○  ○
```
✅ '+' 카드 자동 숨김

---

## ✅ 빌드 결과

**상태:** 성공 🎉  
**소요 시간:** 12초  
**경고:** 없음 (기존 Deprecation 경고만 존재)

```
BUILD SUCCESSFUL in 12s
43 actionable tasks: 17 executed, 6 from cache, 20 up-to-date
```

---

## 🎯 다음 단계 (향후 구현 필요)

### 1. 데이터베이스 연동 ⏳
- Room Database에 `Timer` 테이블 생성
- `timers` 리스트를 DB에서 로드
- `addNewTimer()` → DB에 실제 저장

### 2. 타이머별 독립 실행 ⏳
- 각 타이머가 개별 `startTime`, `targetDays` 관리
- 페이지 변경 시 선택된 타이머 데이터 로드
- `TimerTimeManager`와 연동하여 독립 실행

### 3. 타이머 삭제 기능 ⏳
- 길게 누르기(Long Press) 또는 스와이프로 삭제
- 최소 1개 타이머는 유지 (삭제 불가)

### 4. 타이머 이름 편집 ⏳
- '+' 카드 클릭 시 이름 입력 다이얼로그 표시
- 기존 타이머 이름 클릭 시 편집 가능

---

## 📝 주의사항

### ⚠️ 현재 제한사항
1. **타이머 추가는 UI만 구현됨**
   - `addNewTimer()`가 메모리에만 저장
   - 앱 재시작 시 초기화됨
   - TODO: DB 연동 필요

2. **모든 타이머가 같은 시간 표시**
   - 현재는 첫 번째 타이머의 `elapsedMillis`를 공유
   - TODO: 각 타이머별 독립 시간 관리 필요

3. **포기 버튼은 첫 번째 타이머만 제어**
   - TODO: 현재 선택된 타이머만 제어하도록 변경 필요

---

## 📚 참고 사항

### Compose Pager 공식 문서
- [HorizontalPager](https://developer.android.com/reference/kotlin/androidx/compose/foundation/pager/package-summary#HorizontalPager(androidx.compose.foundation.pager.PagerState,androidx.compose.ui.Modifier,androidx.compose.foundation.layout.PaddingValues,androidx.compose.foundation.pager.PageSize,kotlin.Int,androidx.compose.ui.unit.Dp,androidx.compose.ui.Alignment.Vertical,androidx.compose.foundation.gestures.snapping.SnapFlingBehavior,kotlin.Boolean,kotlin.Boolean,kotlin.Function1,kotlin.Function2))
- [rememberPagerState](https://developer.android.com/reference/kotlin/androidx/compose/foundation/pager/package-summary#rememberPagerState(kotlin.Int,kotlin.Float,kotlin.Function0))

### 수정된 파일 목록
1. `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/viewmodel/Tab01ViewModel.kt`
   - 타이머 리스트 상태 추가
   - 타이머 추가 함수 구현

2. `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/screens/RunScreen.kt`
   - HorizontalPager 구조로 변경
   - 타이머 카드, 추가 카드, 인디케이터 Composable 추가

3. `app/src/main/res/values/strings.xml`
   - 타이머 추가 문구 추가

---

**구현 완료일:** 2026-01-05  
**다음 리뷰:** DB 연동 후 재검토 필요

