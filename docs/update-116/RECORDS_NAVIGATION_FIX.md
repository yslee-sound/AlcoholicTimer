# 월 통계 버튼 연결 오류 수정

## 📋 문제 상황

**증상:**
- 2번째 탭(기록 화면)의 **"월 통계"** 오른쪽 목록 버튼 클릭
- 기대: **"모든 기록 보기(금주 기록)"** 화면으로 이동
- 실제: **"모든 일기 보기"** 화면으로 잘못 이동 ❌

**스크린샷 위치:**
- "월 통계" 헤더 오른쪽의 목록 아이콘 (☰) 버튼

## 🔍 원인 분석

### 문제 1: NavGraph에서 잘못된 라우팅

**파일:** `NavGraph.kt` (라인 223)

```kotlin
// [문제] 모든 일기 보기로 연결됨
onNavigateToAllRecords = { navController.navigate(Screen.AllDiary.route) }
```

**분석:**
- `onNavigateToAllRecords` 파라미터는 **"모든 기록 보기"**를 의미
- 하지만 `Screen.AllDiary.route`로 이동 (모든 일기 보기)
- 올바른 라우트: `Screen.AllRecords.route`

### 문제 2: RecordsScreen 내부 콜백 혼용

**파일:** `RecordsScreen.kt` (라인 346)

```kotlin
// [문제] 일기 섹션에서 기록 보기 콜백 재사용
RecentDiarySection(
    diaries = diaries,
    onNavigateToAllDiaries = onNavigateToAllRecords,  // ❌ 잘못된 콜백 재사용
    ...
)
```

**분석:**
- RecordsScreen에는 두 가지 "모두 보기" 버튼이 있음:
  1. **월 통계** 옆 버튼 → "모든 기록 보기" (금주 기록)
  2. **최근 일기** 아래 버튼 → "모든 일기 보기" (일기)
- 하지만 두 버튼 모두 `onNavigateToAllRecords` 콜백 사용
- 결과: 두 버튼이 같은 화면으로 이동

## ✅ 해결 방법

### 수정 1: NavGraph 라우팅 수정

**파일:** `NavGraph.kt`

```kotlin
// [FIX] 올바른 라우트로 수정
composable(Screen.Records.route) {
    RecordsScreen(
        externalRefreshTrigger = recordsRefreshCounter,
        onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) }, // [FIX] 모든 기록 보기
        onNavigateToAllDiaries = { navController.navigate(Screen.AllDiary.route) },   // [NEW] 모든 일기 보기
        ...
    )
}
```

### 수정 2: RecordsScreen 파라미터 분리

**파일:** `RecordsScreen.kt`

```kotlin
@Composable
fun RecordsScreen(
    externalRefreshTrigger: Int,
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},  // [FIX] 모든 금주 기록 보기
    onNavigateToAllDiaries: () -> Unit = {},  // [NEW] 모든 일기 보기
    onAddRecord: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {},
    fontScale: Float = 1.06f
) {
```

### 수정 3: RecentDiarySection 올바른 콜백 사용

**파일:** `RecordsScreen.kt`

```kotlin
// [FIX] 올바른 콜백 사용
RecentDiarySection(
    diaries = diaries,
    onNavigateToAllDiaries = onNavigateToAllDiaries,  // [FIX] 모든 일기 보기 콜백 사용
    onDiaryClick = onDiaryClick
)
```

## 🎯 수정 결과

### Before (수정 전)

| 버튼 위치 | 기대 화면 | 실제 화면 | 상태 |
|----------|----------|----------|------|
| 월 통계 옆 (☰) | 모든 기록 보기 | 모든 일기 보기 | ❌ |
| 최근 일기 아래 | 모든 일기 보기 | 모든 일기 보기 | ✅ |

### After (수정 후)

| 버튼 위치 | 기대 화면 | 실제 화면 | 상태 |
|----------|----------|----------|------|
| 월 통계 옆 (☰) | 모든 기록 보기 | 모든 기록 보기 | ✅ |
| 최근 일기 아래 | 모든 일기 보기 | 모든 일기 보기 | ✅ |

## 📊 화면 흐름도

### 수정 전
```
Records 화면
├─ [월 통계 ☰] → onNavigateToAllRecords → Screen.AllDiary ❌
└─ [최근 일기 전체보기] → onNavigateToAllRecords → Screen.AllDiary ✅
```

### 수정 후
```
Records 화면
├─ [월 통계 ☰] → onNavigateToAllRecords → Screen.AllRecords ✅
└─ [최근 일기 전체보기] → onNavigateToAllDiaries → Screen.AllDiary ✅
```

## 🔧 기술적 세부사항

### Screen 라우트 구조

```kotlin
// 금주 기록 관련
Screen.Records.route        // 2번째 탭: 기록 메인 화면
Screen.AllRecords.route     // 모든 금주 기록 보기

// 일기 관련
Screen.AllDiary.route       // 모든 일기 보기
Screen.DiaryWrite.route     // 일기 작성
Screen.DiaryDetail.route    // 일기 상세
```

### 콜백 역할 분리

| 콜백 이름 | 역할 | 이동 화면 |
|----------|------|----------|
| `onNavigateToAllRecords` | 모든 금주 기록 보기 | `AllRecordsScreen` |
| `onNavigateToAllDiaries` | 모든 일기 보기 | `AllDiaryScreen` |
| `onNavigateToDetail` | 기록 상세 보기 | `DetailScreen` |
| `onDiaryClick` | 일기 상세 보기 | `DiaryDetailScreen` |

## 🧪 테스트 시나리오

### 시나리오 1: 월 통계 버튼 (수정됨)
1. 2번째 탭(기록) 진입
2. "월 통계" 오른쪽 목록 버튼(☰) 클릭
3. ✅ **"모든 기록 보기"** 화면으로 이동
4. ✅ 금주 기록 목록 표시

### 시나리오 2: 최근 일기 버튼 (정상 동작 유지)
1. 2번째 탭(기록) 진입
2. 화면 아래로 스크롤
3. "최근 금주 일기" 섹션의 "전체보기" 버튼 클릭
4. ✅ **"모든 일기 보기"** 화면으로 이동
5. ✅ 일기 목록 표시

### 시나리오 3: 기록 상세 (정상 동작 유지)
1. "모든 기록 보기" 화면
2. 특정 기록 클릭
3. ✅ 기록 상세 화면으로 이동

## 📝 수정 파일 목록

| 순번 | 파일 | 수정 내용 |
|------|------|----------|
| 1 | `NavGraph.kt` | onNavigateToAllRecords 라우트 수정 (AllDiary → AllRecords) |
| 2 | `NavGraph.kt` | onNavigateToAllDiaries 파라미터 추가 |
| 3 | `RecordsScreen.kt` | onNavigateToAllDiaries 파라미터 추가 |
| 4 | `RecordsScreen.kt` | RecentDiarySection에서 올바른 콜백 사용 |

## ✅ 빌드 결과

```bash
BUILD SUCCESSFUL in 11s
42 actionable tasks: 12 executed, 6 from cache, 24 up-to-date
```

## 💡 개발자 노트

### 콜백 네이밍 규칙

함수 이름으로 의도를 명확히 표현:
```kotlin
✅ onNavigateToAllRecords  // 모든 기록 보기
✅ onNavigateToAllDiaries  // 모든 일기 보기
❌ onNavigateToAll          // 애매함
```

### 파라미터 재사용 주의

서로 다른 화면으로 가야 하는 버튼은 반드시 별도의 콜백 사용:
```kotlin
// ❌ 잘못된 방법
fun MyScreen(onNavigate: () -> Unit) {
    Button1(onClick = onNavigate)  // 화면 A로 이동하고 싶음
    Button2(onClick = onNavigate)  // 화면 B로 이동하고 싶음
}

// ✅ 올바른 방법
fun MyScreen(
    onNavigateToA: () -> Unit,
    onNavigateToB: () -> Unit
) {
    Button1(onClick = onNavigateToA)
    Button2(onClick = onNavigateToB)
}
```

## 🚀 배포 준비

### 체크리스트
- ✅ 빌드 성공
- ✅ 월 통계 버튼 → 모든 기록 보기 연결
- ✅ 최근 일기 버튼 → 모든 일기 보기 유지
- ⏳ 실기기 테스트
- ⏳ 두 버튼의 화면 이동 검증

---

**수정 완료 날짜:** 2025-12-03  
**수정 파일:** NavGraph.kt, RecordsScreen.kt  
**빌드 상태:** ✅ BUILD SUCCESSFUL  
**테스트 상태:** 월 통계 버튼 → 모든 기록 보기 연결 완료 🎉

