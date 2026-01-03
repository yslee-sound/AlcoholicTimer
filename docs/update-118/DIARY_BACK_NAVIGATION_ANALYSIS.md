# 🔍 일기 작성 화면 뒤로가기 버그 분석

**분석 일자**: 2026-01-03  
**문제**: 일기 작성 화면에서 취소(뒤로가기) 시 무조건 일기 목록으로 이동

---

## 📋 확인해야 할 파일

### 1️⃣ **Tab02DetailGraph.kt** ⭐⭐⭐ (핵심 파일!)

**경로**: `app/src/main/java/kr/sweetapps/alcoholictimer/ui/main/navigation/Tab02DetailGraph.kt`

**문제 위치**:

#### A. 신규 일기 작성 (라인 366~378)
```kotlin
composable(
    route = Screen.DiaryWrite.route,
    ...
) {
    DiaryWriteScreen(
        selectedDate = selectedDate,
        onDismiss = {
            // ❌ 문제: 무조건 AllDiary로 강제 이동!
            navController.popBackStack()
            navController.navigate(Screen.AllDiary.route) {  // ❌ 여기가 문제!
                launchSingleTop = true
            }
        }
    )
}
```

#### B. 일기 수정 (라인 409~421)
```kotlin
composable(
    route = Screen.DiaryDetail.route,
    ...
) {
    DiaryWriteScreen(
        diaryId = diaryIdLong,
        onDismiss = {
            // ❌ 문제: 수정 후에도 AllDiary로 강제 이동!
            navController.popBackStack()
            navController.navigate(Screen.AllDiary.route) {  // ❌ 여기가 문제!
                launchSingleTop = true
            }
        }
    )
}
```

**버그 원인**:
- `onDismiss` 콜백에서 **무조건 `AllDiary`로 navigate** 호출
- `popBackStack()`만 호출하면 이전 화면으로 돌아가는데
- 강제로 `navigate(Screen.AllDiary.route)` 추가 호출

---

### 2️⃣ **DiaryWriteScreen.kt** ⭐ (참고)

**경로**: `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_02/screens/DiaryWriteScreen.kt`

**확인 사항**:
- 라인 43~51: `onDismiss` 파라미터 정의
- 라인 317~321: 저장 완료 후 `onDismiss()` 호출
- 라인 327~330: 취소 버튼 클릭 시 `onDismiss()` 호출

**현재 동작**:
```kotlin
@Composable
fun DiaryWriteScreen(
    diaryId: Long? = null,
    selectedDate: Long? = null,
    onDismiss: () -> Unit = {}  // 상위에서 주입받는 콜백
) {
    // ...
    
    // 저장 완료 시
    onPost = {
        onDismiss()  // 상위 콜백 호출
    }
    
    // 취소 시
    onDismiss = {
        onDismiss()  // 상위 콜백 호출
    }
}
```

**문제**: `DiaryWriteScreen` 자체는 문제없음. 상위에서 주입하는 `onDismiss` 콜백이 문제!

---

## 🎯 진입 경로 분석

### 시나리오 1: Records 화면 → 일기 작성
```
RecordsScreen
    ↓ (날짜 클릭)
onNavigateToDiaryWrite(selectedDate)
    ↓
Tab02DetailGraph.DiaryWrite
    ↓ (취소 클릭)
onDismiss()
    ├─> popBackStack()
    └─> ❌ navigate(AllDiary)  // 강제!
    ↓
결과: Records로 돌아가야 하는데 AllDiary로 감!
```

### 시나리오 2: AllDiary → 일기 작성
```
AllDiary (일기 목록)
    ↓ (FAB 클릭)
onNavigateToDiaryWrite(null)
    ↓
Tab02DetailGraph.DiaryWrite
    ↓ (취소 클릭)
onDismiss()
    ├─> popBackStack()
    └─> navigate(AllDiary)  // 이미 AllDiary였음
    ↓
결과: 정상 (우연히 맞음)
```

### 시나리오 3: DiaryDetail → 일기 수정
```
DiaryDetailFeedScreen
    ↓ (수정 클릭)
onNavigateToDiaryDetail(diaryId)
    ↓
Tab02DetailGraph.DiaryDetail
    ↓ (취소 클릭)
onDismiss()
    ├─> popBackStack()
    └─> ❌ navigate(AllDiary)  // 강제!
    ↓
결과: DiaryDetail로 돌아가야 하는데 AllDiary로 감!
```

---

## ✅ 해결 방법

### 간단한 수정: `popBackStack()`만 호출

**Tab02DetailGraph.kt 수정**:

#### 신규 작성 (라인 366~378)
```kotlin
// [수정 전]
onDismiss = {
    onRefreshCounterIncrement()
    navController.popBackStack()
    navController.navigate(Screen.AllDiary.route) {  // ❌ 삭제!
        launchSingleTop = true
    }
}

// [수정 후]
onDismiss = {
    onRefreshCounterIncrement()
    navController.popBackStack()  // ✅ 이것만!
}
```

#### 일기 수정 (라인 409~421)
```kotlin
// [수정 전]
onDismiss = {
    onRefreshCounterIncrement()
    navController.popBackStack()
    navController.navigate(Screen.AllDiary.route) {  // ❌ 삭제!
        launchSingleTop = true
    }
}

// [수정 후]
onDismiss = {
    onRefreshCounterIncrement()
    navController.popBackStack()  // ✅ 이것만!
}
```

---

## 🔬 `popBackStack()`의 작동 원리

**Navigation BackStack**:
```
[Records] → [DiaryWrite]
            ↑ (현재 위치)
```

**`popBackStack()` 호출 시**:
```
[Records] ← (여기로 돌아감)
```

**`popBackStack()` + `navigate(AllDiary)` 호출 시**:
```
[Records] ← (잠깐 돌아감)
    ↓
[Records] → [AllDiary] ← (강제 이동!)
```

**교훈**: `popBackStack()`만으로 충분! 추가 navigate는 불필요!

---

## 💡 왜 이런 코드가 있었을까?

**추측**:
```kotlin
// [CRITICAL] 신규 작성 후 피드 화면(AllDiary)으로 이동 (2025-12-27)
// [NEW] 피드 목록 화면으로 강제 이동하여 저장된 일기를 최신순으로 확인
```

**의도**:
- 일기 저장 후 **최신 일기를 보여주기 위해** AllDiary로 이동
- "저장된 일기를 즉시 확인"하게 하려는 UX 의도

**문제**:
- **취소 시에도 동일한 로직** 실행
- 구분 없이 **무조건 AllDiary로 이동**

---

## 🎯 개선된 로직 (선택적)

### 옵션 1: 단순 수정 (권장)
```kotlin
onDismiss = {
    onRefreshCounterIncrement()
    navController.popBackStack()  // 이전 화면으로
}
```

### 옵션 2: 저장 성공 시에만 AllDiary 이동
```kotlin
// DiaryWriteScreen에서 구분 필요
onDismiss = { wasSaved: Boolean ->
    onRefreshCounterIncrement()
    if (wasSaved) {
        navController.popBackStack()
        navController.navigate(Screen.AllDiary.route) {
            launchSingleTop = true
        }
    } else {
        navController.popBackStack()  // 취소는 이전 화면으로
    }
}
```

**하지만**: 옵션 1이 더 간단하고 자연스러움!

---

## 📋 수정해야 할 위치 정리

**파일**: `Tab02DetailGraph.kt`

**위치 1**: 라인 366~378 (신규 작성)
- `onDismiss` 콜백 내부
- `navController.navigate(Screen.AllDiary.route) { ... }` 제거

**위치 2**: 라인 409~421 (일기 수정)
- `onDismiss` 콜백 내부
- `navController.navigate(Screen.AllDiary.route) { ... }` 제거

**총 2곳 수정!**

---

## 🎉 기대 효과

### Before (버그)
```
Records → DiaryWrite → (취소) → AllDiary ❌
DiaryDetail → DiaryWrite → (취소) → AllDiary ❌
```

### After (수정)
```
Records → DiaryWrite → (취소) → Records ✅
DiaryDetail → DiaryWrite → (취소) → DiaryDetail ✅
AllDiary → DiaryWrite → (취소) → AllDiary ✅
```

**자연스러운 네비게이션 복원!**

---

## 🚀 결론

**확인해야 할 핵심 파일**: 
- ✅ **`Tab02DetailGraph.kt`** (라인 366~421)

**수정 내용**:
- `onDismiss` 콜백에서 `navigate(Screen.AllDiary.route)` 호출 제거
- `popBackStack()`만 남기기

**예상 소요 시간**: 5분 (2줄 삭제 x 2곳)

---

**분석 완료!** 🎊

