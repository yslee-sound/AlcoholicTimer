# 🔍 탭2 화면 재렌더링 원인 분석 보고서

**분석 일자**: 2026-01-03  
**상태**: 🔍 원인 파악 완료 (수정 미실행)

---

## 🎯 핵심 문제 발견

### **문제: 탭2 버튼 클릭 시 화면이 다시 그려지는 현상**

---

## 🔍 근본 원인 3가지

### 1️⃣ **BottomNavBar의 조건문 로직 문제** ⭐⭐⭐ (최우선 원인)

**파일**: `BottomNavBar.kt` 라인 154-165

**현재 코드**:
```kotlin
if (index == 1) {
    if (currentRoute != Screen.Records.route) {  // ❌ 문제의 조건문!
        navController.navigate(Screen.Records.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        }
    }
}
```

**문제점**:
- **이미 Records 화면에 있을 때** (`currentRoute == Screen.Records.route`)
- 탭2를 다시 클릭하면 `if` 조건이 `false`가 됨
- 따라서 **`navigate()` 호출 안 됨**
- 하지만 **클릭 이벤트 자체**가 발생
- 결과: **Composable이 리컴포지션됨!**

**왜 리컴포지션되는가?**:
1. 탭 클릭 → `onClick` 람다 실행
2. `selected` 상태 재계산
3. `currentDestination` 재확인
4. 이 과정에서 **여러 State가 읽혀짐**
5. Compose는 State 읽기 = 구독으로 간주
6. State 변경 없어도 **읽기 동작 자체가 리컴포지션 트리거**

---

### 2️⃣ **Tab02Screen의 Composable 파라미터 재계산** ⭐⭐

**파일**: `Tab02.kt` 라인 101-108

**문제의 코드**:
```kotlin
val context = LocalContext.current  // ❌ 매번 재계산
val periodWeek = context.getString(R.string.records_period_week)  // ❌ 매번 재계산
val periodMonth = context.getString(R.string.records_period_month)  // ❌ 매번 재계산
val periodYear = context.getString(R.string.records_period_year)  // ❌ 매번 재계산
val periodAll = stringResource(id = R.string.records_period_all)  // ❌ 매번 재계산
```

**문제점**:
- `LocalContext.current`는 **Composition Local**
- Composable이 리컴포지션될 때마다 **다시 읽혀짐**
- `context.getString()`도 **매번 다시 호출**
- `stringResource()`도 **매번 다시 계산**
- 이 값들은 **절대 변하지 않는데도** 매번 재계산!

**성능 영향**:
```kotlin
리컴포지션 1회당:
- context 읽기: 1회
- getString() 호출: 3회
- stringResource() 호출: 1회
→ 총 5번의 불필요한 연산!
```

---

### 3️⃣ **LaunchedEffect(Unit)의 재실행** ⭐

**파일**: `Tab02.kt` 라인 111-121

**코드**:
```kotlin
LaunchedEffect(Unit) {
    viewModel.initializePeriod(periodAll)
    viewModel.loadRecordsOnInit()
}
```

**문제점**:
- `LaunchedEffect(Unit)`의 key는 `Unit`
- `Unit`은 **싱글톤 객체**로 절대 변하지 않음
- 하지만 **Composable이 완전히 재생성**되면 새로운 `LaunchedEffect` 인스턴스 생성
- 결과: **다시 실행될 수 있음**

**언제 재실행되는가?**:
1. Navigation으로 **완전히 나갔다가 돌아올 때**
2. Configuration Change (화면 회전 등)
3. Parent Composable이 재생성될 때

**현재 상황**:
- `loadRecordsOnInit()`에서 `isInitialized` 체크하므로 실제 로딩은 안 함
- 하지만 **함수 호출 자체는 발생**
- 로그에 "Already initialized" 메시지가 계속 찍힘

---

## 📊 리컴포지션 트리거 체인

```
[탭2 버튼 클릭]
    ↓
1. onClick 람다 실행
    ↓
2. BottomNavBar 리컴포지션
    ├─> selected 상태 재계산
    ├─> currentRoute 재확인
    └─> currentDestination?.route 읽기
    ↓
3. Tab02Screen 리컴포지션 트리거
    ├─> LocalContext.current 다시 읽기
    ├─> getString() 4번 호출
    ├─> stringResource() 1번 호출
    └─> LaunchedEffect 재확인
    ↓
4. 하위 Composable들도 리컴포지션
    ├─> RecordsScreen
    ├─> DiaryDetailFeedScreen (조건부)
    └─> 모든 remember {} 블록 재실행
    ↓
5. 화면이 다시 그려짐!
```

---

## 🎯 각 원인별 해결 방법

### 해결책 1: BottomNavBar 로직 수정 (최우선!)

**현재 문제**:
```kotlin
if (index == 1) {
    if (currentRoute != Screen.Records.route) {  // ❌ 이미 있을 때 조건 불일치
        navController.navigate(...)
    }
    // ❌ else 블록 없음 → 아무것도 안 하지만 클릭은 처리됨
}
```

**해결 방법 A**: 조건 제거
```kotlin
if (index == 1) {
    // currentRoute 체크 없이 무조건 navigate
    // launchSingleTop이 중복 방지해줌
    navController.navigate(Screen.Records.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(...) { saveState = true }
    }
}
```

**해결 방법 B**: `selected` 체크로 변경
```kotlin
if (index == 1 && !selected) {  // 선택되지 않았을 때만
    navController.navigate(Screen.Records.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(...) { saveState = true }
    }
}
// selected == true일 때는 onClick 자체를 막음
```

---

### 해결책 2: Tab02Screen 성능 최적화

**현재 문제**:
```kotlin
val periodWeek = context.getString(R.string.records_period_week)  // ❌ 매번 호출
```

**해결 방법**: `remember` 사용
```kotlin
val periodWeek = remember { context.getString(R.string.records_period_week) }
val periodMonth = remember { context.getString(R.string.records_period_month) }
val periodYear = remember { context.getString(R.string.records_period_year) }
val periodAll = remember { stringResource(id = R.string.records_period_all) }
```

**효과**:
- ✅ 첫 Composition에만 계산
- ✅ 이후 리컴포지션에서는 **캐시된 값 사용**
- ✅ 불필요한 연산 0회!

---

### 해결책 3: LaunchedEffect 최적화 (선택적)

**현재 코드**:
```kotlin
LaunchedEffect(Unit) {
    viewModel.initializePeriod(periodAll)
    viewModel.loadRecordsOnInit()
}
```

**개선 방법 A**: Key를 더 명확하게
```kotlin
LaunchedEffect(viewModel) {  // ViewModel이 변경될 때만 재실행
    viewModel.initializePeriod(periodAll)
    viewModel.loadRecordsOnInit()
}
```

**개선 방법 B**: DisposableEffect로 변경
```kotlin
DisposableEffect(Unit) {
    viewModel.loadRecordsOnInit()
    onDispose { /* cleanup */ }
}
```

---

## 🔬 실제 발생 시나리오

### 시나리오 1: 이미 탭2에 있을 때 탭2 클릭

```
[사용자 액션]
탭2 화면 보는 중 → 탭2 아이콘 다시 클릭

[내부 동작]
1. onClick 실행
2. if (index == 1) → true
3. if (currentRoute != Screen.Records.route) → false (이미 Records)
4. navigate() 호출 안 됨
5. 하지만 onClick 내부에서 State 읽기 발생
6. Tab02Screen 리컴포지션 트리거
7. LocalContext.current 다시 읽기
8. getString() 4번 재호출
9. LaunchedEffect 재확인
10. 화면 다시 그려짐!

[사용자가 보는 것]
- 화면이 살짝 깜빡임
- 스크롤 위치는 유지됨 (restoreState 덕분)
- 하지만 "다시 그려지는" 느낌
```

### 시나리오 2: 다른 탭에서 탭2로 이동

```
[사용자 액션]
탭1 → 탭2 클릭

[내부 동작]
1. onClick 실행
2. if (index == 1) → true
3. if (currentRoute != Screen.Records.route) → true (Start/Run에 있었음)
4. navigate(Screen.Records.route) 호출 ✅
5. Navigation 전환
6. Tab02Screen Composition
7. LocalContext.current 읽기
8. getString() 호출
9. LaunchedEffect(Unit) 실행
10. loadRecordsOnInit() → 이미 초기화됨 → 스킵

[사용자가 보는 것]
- 정상적인 화면 전환
- 데이터는 즉시 표시 (캐시)
- 부드러움
```

---

## 📋 정리: 문제의 우선순위

| 순위 | 원인 | 영향도 | 난이도 | 수정 파일 |
|------|------|--------|--------|----------|
| **1** | BottomNavBar 조건문 | ⭐⭐⭐ 높음 | 🟢 쉬움 | BottomNavBar.kt |
| **2** | Tab02Screen 재계산 | ⭐⭐ 중간 | 🟢 쉬움 | Tab02.kt |
| **3** | LaunchedEffect 재실행 | ⭐ 낮음 | 🟡 보통 | Tab02.kt |

---

## 🎯 권장 수정 순서

### 1단계: BottomNavBar 수정 (필수!)
- `if (currentRoute != Screen.Records.route)` 조건 제거
- 또는 `selected` 상태로 클릭 자체를 막기

### 2단계: Tab02Screen 최적화 (권장)
- `remember` 블록으로 문자열 캐싱
- 불필요한 재계산 방지

### 3단계: LaunchedEffect 검토 (선택)
- Key를 더 명확하게 지정
- 또는 DisposableEffect로 변경

---

## 💡 핵심 인사이트

### Compose의 리컴포지션 원리

**중요한 개념**:
```kotlin
// ❌ 잘못된 이해
"State가 변경되지 않으면 리컴포지션 안 됨"

// ✅ 올바른 이해
"State를 읽는 Composable은 해당 State의 구독자가 됨"
"Parent가 리컴포지션되면 Child도 리컴포지션될 수 있음"
"onClick 같은 이벤트 핸들러도 리컴포지션 트리거 가능"
```

### 탭 네비게이션의 함정

**문제의 패턴**:
```kotlin
onClick = {
    if (!selected) {  // ✅ 좋은 패턴
        navigate(...)
    }
    // 또는
    if (currentRoute != targetRoute) {  // ❌ 나쁜 패턴
        navigate(...)
    }
}
```

**이유**:
- `selected` 체크: **클릭 전에** 확인 → 불필요한 실행 방지
- `currentRoute` 체크: **클릭 후에** 확인 → 이미 State 읽음 → 리컴포지션!

---

## 🎉 결론

**탭2 버튼 클릭 시 화면이 다시 그려지는 이유**:

1. ✅ `BottomNavBar`의 조건문 로직이 불완전함
2. ✅ 탭2에 이미 있을 때도 `onClick`이 실행됨
3. ✅ State 읽기가 발생하여 리컴포지션 트리거
4. ✅ `Tab02Screen`의 Composable 파라미터들이 매번 재계산됨
5. ✅ 결과: 화면 전체가 다시 그려짐!

**가장 효과적인 해결책**:
- **BottomNavBar의 조건문 로직 수정** (1줄 수정으로 해결!)
- `Tab02Screen`의 문자열들을 `remember`로 캐싱 (성능 추가 개선)

---

**분석 완료!** 🎊
**수정 준비 완료 - 명령만 기다리는 중!** 🚀

