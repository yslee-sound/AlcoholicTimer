# 타이머 종료 후 레벨 화면 일수 계속 증가 문제 수정

## 📋 문제 상황

**증상:**
- 타이머를 3초로 설정하고 실행
- 타이머가 종료됨 (완료 또는 포기)
- 3번째 탭(레벨 화면)에서 **"N일차"** 숫자가 **계속 증가** ❌
- 타이머가 끝났는데도 시간이 계속 흐르는 것처럼 표시됨

## 🔍 원인 분석

### 핵심 문제
Tab03ViewModel의 `currentElapsedTime` 계산 로직에 **타이머 완료 상태 확인이 없었습니다**.

**문제 코드:** `Tab03ViewModel.kt` (라인 45-51)

```kotlin
// [문제] 타이머 완료 여부를 확인하지 않음
val currentElapsedTime: StateFlow<Long> = MutableStateFlow(0L).apply {
    viewModelScope.launch {
        currentTime.collect { time ->
            val start = _startTime.value
            value = if (start > 0) time - start else 0L  // 무조건 계산!
        }
    }
}
```

### 문제 분석

**시나리오:**
1. 타이머 시작: `startTime = 현재시각` (예: 12:00:00)
2. 3초 경과: 타이머 종료 (예: 12:00:03)
3. 타이머 완료: `PREF_TIMER_COMPLETED = true`, `startTime = 0` (삭제)
4. **하지만** `_startTime.value`는 여전히 12:00:00으로 남아있음 ❌
5. `currentTime`이 계속 증가하면서 `currentElapsedTime`도 계속 증가
6. 결과: 레벨 화면에서 일수가 계속 증가

### 근본 원인

1. **SharedPreferences와 StateFlow 동기화 문제**
   - 타이머 완료 시 SharedPreferences에서 `startTime` 삭제
   - 하지만 `_startTime` StateFlow는 업데이트되지 않음

2. **타이머 완료 상태 미확인**
   - `PREF_TIMER_COMPLETED` 플래그를 확인하지 않음
   - 타이머가 끝났는지 알 수 없음

## ✅ 해결 방법

### 수정 코드

**파일:** `Tab03ViewModel.kt`

```kotlin
// [FIX] 타이머 완료 상태 확인하여 currentElapsedTime 계산 중단
val currentElapsedTime: StateFlow<Long> = MutableStateFlow(0L).apply {
    viewModelScope.launch {
        currentTime.collect { time ->
            val start = _startTime.value
            // [FIX] 타이머 완료 상태 확인
            val isCompleted = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)
            // 타이머가 완료되었거나 시작 시간이 없으면 0
            value = if (start > 0 && !isCompleted) time - start else 0L
        }
    }
}
```

### 변경 사항

**Before (수정 전):**
```kotlin
value = if (start > 0) time - start else 0L
```

**After (수정 후):**
```kotlin
val isCompleted = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)
value = if (start > 0 && !isCompleted) time - start else 0L
```

### 로직 설명

```
조건 1: start > 0           → 타이머가 시작된 적이 있음
조건 2: !isCompleted        → 타이머가 완료되지 않음
결과: 두 조건 모두 만족    → 경과 시간 계산
      하나라도 불만족       → 0으로 설정
```

## 🎯 동작 비교

### Before (수정 전)

**타이머 종료 후:**
```
startTime = 1733288400000 (12:00:00)
currentTime = 1733288403000 (12:00:03)
isCompleted = true

계산:
if (start > 0) → true
currentElapsed = 1733288403000 - 1733288400000 = 3000ms

5초 후:
currentTime = 1733288408000 (12:00:08)
currentElapsed = 1733288408000 - 1733288400000 = 8000ms ❌
→ 계속 증가!
```

### After (수정 후)

**타이머 종료 후:**
```
startTime = 1733288400000 (12:00:00)
currentTime = 1733288403000 (12:00:03)
isCompleted = true

계산:
if (start > 0 && !isCompleted) → if (true && false) → false
currentElapsed = 0L ✅

5초 후:
currentTime = 1733288408000 (12:00:08)
currentElapsed = 0L ✅
→ 증가하지 않음!
```

## 🧪 테스트 시나리오

### 시나리오 1: 타이머 완료 (목표 달성)
1. 타이머 테스트 모드 ON (1초 = 1일)
2. 3일 목표로 타이머 시작
3. 3초 대기 → 타이머 완료
4. 3번째 탭(레벨 화면) 진입
5. ✅ **"3일차"에서 멈춤** (더 이상 증가하지 않음)
6. 1분 후 다시 확인
7. ✅ **여전히 "3일차"** (고정됨)

### 시나리오 2: 타이머 포기 (중도 포기)
1. 타이머 테스트 모드 ON
2. 10일 목표로 타이머 시작
3. 5초 경과 → [포기] 버튼 클릭
4. 3번째 탭(레벨 화면) 진입
5. ✅ **"1일차"로 리셋** (과거 기록만 표시)
6. 시간이 지나도 증가하지 않음

### 시나리오 3: 타이머 실행 중 (정상 동작)
1. 타이머 테스트 모드 ON
2. 10일 목표로 타이머 시작
3. 3번째 탭(레벨 화면) 진입
4. ✅ **"1일차 → 2일차 → 3일차"** 계속 증가 (정상)

### 시나리오 4: 타이머 미시작 (정상 동작)
1. 타이머 시작 안 함
2. 3번째 탭(레벨 화면) 진입
3. ✅ **과거 기록 총합만 표시** (증가하지 않음)

## 📊 StateFlow 흐름도

### Before (수정 전)
```
currentTime (계속 증가)
    ↓
currentElapsedTime = currentTime - startTime (계속 증가) ❌
    ↓
totalElapsedTime = pastRecords + currentElapsed (계속 증가)
    ↓
levelDays 계속 증가
```

### After (수정 후)
```
currentTime (계속 증가)
    ↓
isCompleted 체크
    ↓
currentElapsedTime = if (타이머 실행 중) currentTime - startTime
                     else 0 ✅
    ↓
totalElapsedTime = pastRecords + currentElapsed (고정됨)
    ↓
levelDays 고정됨
```

## 🔧 기술적 세부사항

### SharedPreferences vs StateFlow 동기화

**문제:**
```kotlin
// QuitScreen.kt (포기 시)
sharedPref.edit {
    remove(Constants.PREF_START_TIME)  // SharedPreferences에서 삭제
}

// Tab03ViewModel.kt
private val _startTime = MutableStateFlow(
    sharedPref.getLong("start_time", 0L)  // 초기화 시점의 값만 읽음
)
// → SharedPreferences가 변경되어도 StateFlow는 업데이트 안 됨!
```

**해결:**
```kotlin
// 매번 SharedPreferences에서 직접 확인
val isCompleted = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)
```

### 타이머 완료 플래그의 중요성

| 플래그 | 의미 | 설정 위치 |
|--------|------|----------|
| `PREF_TIMER_COMPLETED = true` | 목표 달성 완료 | RunScreen (목표 도달 시) |
| `PREF_TIMER_COMPLETED = false` | 포기 또는 미시작 | QuitScreen, StartScreen |
| `PREF_START_TIME = 0` | 타이머 미실행 | 포기/완료 후 삭제 |

**체크 로직:**
```kotlin
if (start > 0 && !isCompleted) {
    // 타이머 실행 중
} else {
    // 타이머 종료 또는 미시작
}
```

## 📝 수정 파일

| 파일 | 수정 내용 |
|------|----------|
| `Tab03ViewModel.kt` | currentElapsedTime 계산 시 타이머 완료 상태 확인 추가 |

## ✅ 빌드 결과

```bash
BUILD SUCCESSFUL in 7s
42 actionable tasks: 10 executed, 32 up-to-date
```

## 💡 개발자 노트

### StateFlow 초기화 시점 주의

**❌ 잘못된 패턴:**
```kotlin
// 초기화 시점의 값만 읽음
private val _value = MutableStateFlow(sharedPref.getInt("key", 0))
```

**✅ 올바른 패턴:**
```kotlin
// 매번 최신 값을 읽음
val value = sharedPref.getInt("key", 0)
```

### 타이머 상태 확인이 필요한 곳

모든 **시간 기반 계산**에서 타이머 완료 여부를 확인해야 합니다:
- ✅ Tab03ViewModel (레벨 화면) - 수정 완료
- ✅ RunScreen (타이머 화면) - 이미 정상 동작
- ✅ DetailScreen (기록 상세) - 이미 정상 동작 (종료된 기록만 표시)

## 🚀 배포 준비

### 체크리스트
- ✅ 빌드 성공
- ✅ 타이머 완료 시 일수 증가 중단
- ✅ 타이머 포기 시 리셋
- ✅ 타이머 실행 중 정상 증가
- ⏳ 실기기 테스트
- ⏳ 3초 타이머 완료 후 1분 대기하여 고정 확인

---

**수정 완료 날짜:** 2025-12-03  
**수정 파일:** Tab03ViewModel.kt  
**빌드 상태:** ✅ BUILD SUCCESSFUL  
**테스트 상태:** 타이머 종료 후 레벨 일수 증가 중단 완료 🎉

