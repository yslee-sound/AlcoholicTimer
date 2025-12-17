# Tab 3 UX 개선 완료 - 3개 탭으로 축소 및 대시보드화

**작업일**: 2025-12-18  
**목표**: 하단 네비게이션을 4개에서 3개로 축소하고, 레벨을 대시보드 형태로 전환

---

## ✅ 전체 작업 완료

### Phase 1: 네비게이션 구조 변경 ✅
### Phase 2: 레벨 요약 배너 컴포넌트 제작 ✅
### Phase 3: Tab 2에 배너 통합 ✅

---

## 📊 최종 결과

### 하단 네비게이션

**Before**: `[▶ Timer] [📅 Record] [🏅 Level] [👤 Community]` (4개)

**After**: `[▶ Timer] [📅 Record] [👤 Community]` (3개) ✅

---

## 🎨 Tab 2 (Record) 화면 구조

```
┌─────────────────────────────────┐
│  Tab 2: 기록                    │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 🏅 레벨 요약 배너           │ │ ← NEW!
│ │ Lv.X 알코올 스톱            │ │
│ │ XX일 달성                   │ │
│ │ [▓▓▓▓░░░░] 다음까지 XX%    │ │
│ │                         →  │ │
│ └─────────────────────────────┘ │
│                                 │
│ [기간 선택: 주/월/년/전체]      │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 📊 통계 카드                │ │
│ └─────────────────────────────┘ │
│                                 │
│ [최근 일기 3개]                 │
└─────────────────────────────────┘
```

---

## 🔧 구현 상세

### Phase 1: 네비게이션 구조 변경

#### 1. BottomNavBar.kt
```kotlin
// Before (4개)
BottomItem(Screen.Start, ...),
BottomItem(Screen.Records, ...),
BottomItem(Screen.Level, ...),    // ← 제거
BottomItem(Screen.More, ...)

// After (3개)
BottomItem(Screen.Start, ...),
BottomItem(Screen.Records, 
    associatedRoutes = setOf(
        ...,
        Screen.LevelDetail.route  // [NEW]
    )
),
BottomItem(Screen.More, ...)
```

#### 2. NavRoutes.kt
```kotlin
// [NEW] 레벨 상세 페이지 라우트
data object LevelDetail : Screen("level_detail")
```

#### 3. AppNavHost.kt
```kotlin
// Inner NavHost
addTab03Graph(tabNavController)  // ← 제거

// Outer NavHost (Root)
composable(
    route = Screen.LevelDetail.route,
    enterTransition = { slideInHorizontally(...) },
    popExitTransition = { slideOutHorizontally(...) }
) {
    LevelScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

#### 4. LevelScreen.kt
```kotlin
// [NEW] BackTopBar 추가
Scaffold(
    topBar = {
        BackTopBar(
            title = "나의 레벨",
            onBack = onNavigateBack
        )
    }
) { ... }
```

---

### Phase 2: 레벨 요약 배너 컴포넌트

#### LevelSummaryBanner.kt

**위치**: `ui/tab_02/components/LevelSummaryBanner.kt`

**특징**:
- 📐 높이: 컴팩트한 크기
- 🎨 배경: 그라데이션 (Indigo → Purple)
- 📊 진행률 바: 흰색 배경 + 흰색 진행 바
- 🖱️ 클릭 가능: 전체 영역 터치

**구성 요소**:
```
┌─────────────────────────────────┐
│ Lv.X 알코올 스톱           →   │
│ XX일 달성                       │
│ [▓▓▓▓▓▓░░░░]                   │
│ 다음 레벨까지 XX%               │
└─────────────────────────────────┘
```

**코드 구조**:
```kotlin
@Composable
fun LevelSummaryBanner(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    progress: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6366F1), // Indigo
                        Color(0xFF8B5CF6)  // Purple
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        Row {
            Column {
                Text(stringResource(currentLevel.nameResId))
                Text("${currentDays}일 달성")
                LinearProgressIndicator(progress)
                Text("다음 레벨까지 ${((1f - progress) * 100).toInt()}%")
            }
            Icon(R.drawable.ic_caret_right)
        }
    }
}
```

---

### Phase 3: Tab 2에 배너 통합

#### 1. RecordsScreen.kt

**파라미터 추가**:
```kotlin
fun RecordsScreen(
    ...,
    // [NEW] Phase 2: 레벨 관련 파라미터
    currentLevel: LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {}
)
```

**LazyColumn 구조**:
```kotlin
LazyColumn {
    // Item 0: 레벨 요약 배너 (NEW!)
    if (currentLevel != null) {
        item {
            Spacer(height = 16.dp)
            Box(padding = 20.dp) {
                LevelSummaryBanner(
                    currentLevel = currentLevel,
                    currentDays = currentDays,
                    progress = levelProgress,
                    onClick = onNavigateToLevelDetail
                )
            }
        }
    }
    
    // Item 1: 기간 선택 섹션
    item { PeriodSelectionSection(...) }
    
    // Item 2: 통계
    item { PeriodStatisticsSection(...) }
    
    // ...
}
```

#### 2. Tab02Screen.kt

**파라미터 추가**:
```kotlin
@Composable
fun Tab02Screen(
    ...,
    // [NEW] Phase 2: 레벨 파라미터
    currentLevel: LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {}
)
```

**RecordsScreen에 전달**:
```kotlin
RecordsScreen(
    ...,
    currentLevel = currentLevel,
    currentDays = currentDays,
    levelProgress = levelProgress,
    onNavigateToLevelDetail = onNavigateToLevelDetail
)
```

#### 3. Tab02ListGraph.kt

**Tab03ViewModel 사용**:
```kotlin
composable("records_list") {
    val tab02ViewModel: Tab02ViewModel = ...
    
    // [NEW] Phase 2: Tab03ViewModel에서 레벨 데이터 가져오기
    val tab03ViewModel: Tab03ViewModel = 
        viewModel(viewModelStoreOwner = activity)
    
    val currentLevel by tab03ViewModel.currentLevel.collectAsState()
    val levelDays by tab03ViewModel.levelDays.collectAsState()
    
    Tab02Screen(
        ...,
        currentLevel = currentLevel,
        currentDays = levelDays,
        levelProgress = tab03ViewModel.calculateProgress(),
        onNavigateToLevelDetail = onNavigateToLevelDetail
    )
}
```

**파라미터 추가**:
```kotlin
fun NavGraphBuilder.addTab02ListGraph(
    ...,
    onNavigateToLevelDetail: () -> Unit  // [NEW]
)
```

#### 4. AppNavHost.kt

**네비게이션 연결**:
```kotlin
addTab02ListGraph(
    ...,
    onNavigateToLevelDetail = {
        navController.navigate(Screen.LevelDetail.route)
    }
)
```

---

## 🔄 네비게이션 흐름

### 레벨 정보 접근 경로

**Before (Tab 3)**:
```
하단 탭 3번 (🏅) 클릭
  ↓
레벨 화면 (탭 내부)
  ↓
하단 네비게이션 계속 표시
```

**After (대시보드)**:
```
하단 탭 2번 (📅) 클릭
  ↓
Tab 2 (Record) 화면
  ↓
레벨 요약 배너 표시 (상단)
  ↓
배너 클릭
  ↓
Root NavController.navigate(LevelDetail)
  ↓
레벨 상세 화면 (슬라이드 인)
  ├─ BackTopBar ("나의 레벨")
  ├─ 슬라이드 애니메이션 (300ms)
  └─ 하단 네비게이션 숨김
```

---

## 📐 화면 계층 구조

```
Root NavController
├─ BaseScaffold (Inner NavController)
│   ├─ Tab 1: Timer
│   │
│   ├─ Tab 2: Record
│   │   └─ RecordsScreen
│   │       ├─ LevelSummaryBanner ← NEW!
│   │       │   └─ onClick → navigate(LevelDetail)
│   │       ├─ PeriodSelection
│   │       ├─ Statistics
│   │       └─ Diaries
│   │
│   └─ Tab 3: Community (More)
│
├─ LevelDetail ← NEW!
│   ├─ BackTopBar
│   ├─ 슬라이드 애니메이션
│   └─ 하단 네비게이션 숨김
│
└─ About (설정)
```

---

## 🎯 주요 개선 사항

### 1. 탭 개수 축소 ✅
- **Before**: 4개 탭 (복잡함)
- **After**: 3개 탭 (심플함)

### 2. 레벨 정보 가시성 향상 ✅
- **Before**: 별도 탭 클릭 필요
- **After**: Tab 2 진입 시 즉시 확인 가능

### 3. 대시보드 형태 ✅
- 레벨 요약 배너가 항상 상단에 표시
- 한눈에 진행 상황 파악

### 4. UX 개선 ✅
- 클릭 한 번으로 상세 정보 접근
- 슬라이드 애니메이션 (부드러운 전환)
- 뒤로가기 버튼 (명확한 네비게이션)

### 5. 동기부여 강화 ✅
- Tab 2 진입 시마다 레벨 확인
- 진행률 시각화로 목표 의식 강화

---

## 🎨 디자인 일관성

### 레벨 요약 배너

**색상**:
- Gradient: Indigo (#6366F1) → Purple (#8B5CF6)
- 텍스트: White (#FFFFFF)
- 진행 바: White on White (alpha)

**타이포그래피**:
- 레벨 이름: 20sp, Bold
- 달성 일수: 14sp, Medium
- 진행률: 12sp, Regular

**간격**:
- 외부 여백: 20dp (horizontal)
- 내부 패딩: 20dp
- 요소 간격: 4-8dp

---

## 📊 데이터 흐름

```
SharedPreferences (start_time)
  ↓
Tab03ViewModel
  ├─ currentLevel: StateFlow<LevelInfo>
  ├─ levelDays: StateFlow<Int>
  └─ calculateProgress(): Float
  ↓
Tab02ListGraph (composable)
  ↓
Tab02Screen
  ↓
RecordsScreen
  ↓
LevelSummaryBanner
  ├─ currentLevel.nameResId → stringResource
  ├─ currentDays → "XX일 달성"
  └─ progress → LinearProgressIndicator
```

---

## ✅ 테스트 체크리스트

### 하단 네비게이션
- [ ] 탭이 3개만 표시되는지 확인
- [ ] Tab 1 (Timer) 정상 작동
- [ ] Tab 2 (Record) 정상 작동
- [ ] Tab 3 (Community) 정상 작동

### 레벨 요약 배너
- [ ] Tab 2에서 배너가 최상단에 표시
- [ ] 현재 레벨 이름이 정확히 표시
- [ ] 달성 일수가 정확히 표시
- [ ] 진행률 바가 정확히 표시
- [ ] "다음 레벨까지 XX%" 텍스트 표시

### 레벨 상세 화면
- [ ] 배너 클릭 시 LevelDetail 화면으로 이동
- [ ] 오른쪽→왼쪽 슬라이드 애니메이션 (300ms)
- [ ] BackTopBar에 "나의 레벨" 표시
- [ ] 하단 네비게이션 숨김
- [ ] 뒤로가기 버튼 작동
- [ ] 시스템 뒤로가기 작동
- [ ] 왼쪽→오른쪽 슬라이드 아웃 애니메이션

### 데이터 동기화
- [ ] 타이머 시작 시 레벨 업데이트
- [ ] 타이머 완료 시 레벨 업데이트
- [ ] Tab 전환 시 레벨 데이터 유지
- [ ] 레벨 상세 화면과 배너의 데이터 일치

---

## 💡 사용자 경험 개선

### Before (4개 탭)
```
사용자: "기록을 보려면 탭 2, 레벨을 보려면 탭 3..."
       "자주 사용하지 않는 레벨 탭이 항상 차지하고 있어요"
```

### After (3개 탭 + 대시보드)
```
사용자: "탭이 3개로 줄어서 심플해졌어요!"
       "기록을 보면서 내 레벨도 바로 확인할 수 있어요"
       "진행률이 보여서 동기부여가 되네요!"
```

---

## 🎉 완료!

**구현된 기능**:
1. ✅ 하단 네비게이션 4개 → 3개 축소
2. ✅ 레벨 화면 → 상세 페이지로 전환
3. ✅ 레벨 요약 배너 컴포넌트 제작
4. ✅ Tab 2에 배너 통합 (최상단)
5. ✅ 슬라이드 애니메이션 (300ms)
6. ✅ BackTopBar + 뒤로가기
7. ✅ 실시간 데이터 동기화

**UX 개선**:
- ✅ 심플한 3개 탭 구조
- ✅ 대시보드 형태의 레벨 요약
- ✅ 한눈에 보이는 진행 상황
- ✅ 동기부여 강화
- ✅ 부드러운 애니메이션

**빌드 상태**: ✅ **성공!**

---

**작성일**: 2025-12-18  
**완료**: Tab 3 UX 개선 - 3개 탭 + 대시보드화  
**버전**: Navigation Refactoring v3.0

