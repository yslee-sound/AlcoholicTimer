# 타이머 테스트 모드 시 레벨 화면 실시간 업데이트 수정

## 📋 문제 상황

**증상:**
- 디버그 모드에서 타이머 테스트 스위치를 켜면 1초 = 1일로 빠르게 진행됨
- 하지만 레벨 화면(Tab03)에서는 레벨이 변경되지 않음
- 실시간으로 레벨이 올라가야 하는데 고정되어 있음

## 🔍 원인 분석

### 문제의 핵심
Tab03ViewModel에서 **고정된 `Constants.DAY_IN_MILLIS`**를 사용하여 레벨을 계산하고 있었습니다.

```kotlin
// [문제] 고정 값 사용
_totalElapsedDaysFloat.value = total / Constants.DAY_IN_MILLIS.toFloat()
val days = Constants.calculateLevelDays(total)
```

테스트 모드가 켜져도 `DAY_IN_MILLIS = 86400000L`(1일)로 고정되어 있어서, 1초가 지나도 레벨 계산에는 반영되지 않았습니다.

## ✅ 해결 방법

### 1. Tab03ViewModel - 동적 업데이트 주기 변경

**파일:** `Tab03ViewModel.kt`

테스트 모드일 때 **0.1초마다** 시간을 업데이트하도록 변경했습니다.

```kotlin
// [FIX] 타이머 테스트 모드 확인 (1초 = 1일)
val isTestMode = TimerStateRepository.isTimerTestModeEnabled()
val updateInterval = if (isTestMode) 100L else 1000L // 테스트: 0.1초, 정상: 1초

delay(updateInterval)
_currentTime.value = System.currentTimeMillis()
```

**효과:**
- 정상 모드: 1초마다 업데이트 (기존과 동일)
- 테스트 모드: 0.1초마다 업데이트 → 레벨 변화가 부드럽게 보임

### 2. Tab03ViewModel - 동적 DAY_IN_MILLIS 사용

테스트 모드를 인식하는 `Constants.getDayInMillis(context)`를 사용하도록 변경했습니다.

```kotlin
// [FIX] 타이머 테스트 모드를 고려한 동적 DAY_IN_MILLIS
val dayInMillis = Constants.getDayInMillis(getApplication())
_totalElapsedDaysFloat.value = total / dayInMillis.toFloat()

val days = Constants.calculateLevelDays(total, dayInMillis)
```

**효과:**
- 정상 모드: `dayInMillis = 86400000L` (1일 = 24시간)
- 테스트 모드: `dayInMillis = 1000L` (1일 = 1초)

### 3. Constants.kt - 오버로드 함수 추가

동적 `dayInMillis`를 받는 `calculateLevelDays` 오버로드 함수를 추가했습니다.

```kotlin
// [NEW] 레벨 계산: 동적 dayInMillis를 받는 오버로드 (테스트 모드 대응)
fun calculateLevelDays(elapsedTimeMillis: Long, dayInMillis: Long): Int {
    val days = (elapsedTimeMillis / dayInMillis).toInt()
    return if (days == 0) 1 else days + 1
}
```

**장점:**
- 기존 `calculateLevelDays(Long)` 함수는 그대로 유지 (하위 호환성)
- 새로운 오버로드 함수로 테스트 모드 대응

### 4. 디버그 로그 추가

테스트 모드일 때만 상세 로그를 출력하여 디버깅이 쉽도록 했습니다.

```kotlin
// [DEBUG] 테스트 모드 시 로그 출력
if (TimerStateRepository.isTimerTestModeEnabled()) {
    Log.d("Tab03ViewModel", "테스트 모드 레벨 업데이트: elapsed=${total}ms, dayInMillis=${dayInMillis}, days=$days")
}
```

## 🎯 수정된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `Tab03ViewModel.kt` | 테스트 모드 인식 업데이트 주기 변경 (0.1초) |
| `Tab03ViewModel.kt` | 동적 `getDayInMillis()` 사용 |
| `Tab03ViewModel.kt` | TimerStateRepository import 추가 |
| `Tab03ViewModel.kt` | 디버그 로그 추가 |
| `Constants.kt` | `calculateLevelDays` 오버로드 함수 추가 |

## 🧪 테스트 시나리오

### 시나리오 1: 정상 모드 (기존 동작 유지)
1. 타이머 시작
2. 레벨 화면 진입
3. **1초마다 화면 업데이트** (변화 없음)

### 시나리오 2: 테스트 모드 (신규 기능)
1. 디버그 모드에서 타이머 테스트 스위치 ON
2. 타이머 시작 (1초 = 1일)
3. 레벨 화면 진입
4. **0.1초마다 화면 업데이트**
5. ✅ **1초마다 레벨 1씩 증가** (1일 차 → 2일 차 → 3일 차...)
6. ✅ **레벨 전환 실시간 확인 가능**
   - 3일 차 → 4일 차: Lv.1 → Lv.2 전환
   - 7일 차 → 8일 차: Lv.2 → Lv.3 전환
   - ...

## 📊 동작 비교

### Before (수정 전)
```
정상 모드: 1초마다 업데이트 ✅
테스트 모드: 1초마다 업데이트 ❌ (레벨 변경 안 됨)
```

### After (수정 후)
```
정상 모드: 1초마다 업데이트 ✅
테스트 모드: 0.1초마다 업데이트 ✅ (레벨 실시간 변경)
```

## 🔧 기술적 세부사항

### 시간 계산 로직

**테스트 모드:**
```
1초 경과 → elapsedMillis = 1000L
dayInMillis = 1000L (테스트 모드)
levelDays = 1000 / 1000 + 1 = 2일 차 → Lv.1
```

**정상 모드:**
```
1일 경과 → elapsedMillis = 86400000L
dayInMillis = 86400000L (정상 모드)
levelDays = 86400000 / 86400000 + 1 = 2일 차 → Lv.1
```

### 업데이트 주기

| 모드 | 업데이트 주기 | 이유 |
|------|-------------|------|
| 정상 | 1초 (1000ms) | 배터리 절약, 충분한 반응성 |
| 테스트 | 0.1초 (100ms) | 빠른 레벨 변화를 부드럽게 표시 |

## ✅ 빌드 결과

```bash
BUILD SUCCESSFUL in 10s
42 actionable tasks: 12 executed, 6 from cache, 24 up-to-date
```

## 💡 사용법

### 테스트 모드 활성화 방법
1. 디버그 빌드 설치
2. 설정 화면 → 디버그 메뉴
3. "타이머 테스트 모드" 스위치 ON
4. 타이머 시작
5. 레벨 화면 이동
6. **1초마다 레벨이 올라가는 것을 확인**

### 로그 확인
```bash
adb -s emulator-5554 logcat -v time | findstr "Tab03ViewModel"
```

**출력 예시:**
```
12-03 14:23:45.123 D Tab03ViewModel: 테스트 모드 레벨 업데이트: elapsed=1000ms, dayInMillis=1000, days=2
12-03 14:23:46.234 D Tab03ViewModel: 테스트 모드 레벨 업데이트: elapsed=2000ms, dayInMillis=1000, days=3
12-03 14:23:47.345 D Tab03ViewModel: 테스트 모드 레벨 업데이트: elapsed=3000ms, dayInMillis=1000, days=4
```

## 🎉 개선 효과

1. **개발 생산성 향상**
   - 레벨 시스템 테스트 시간 단축 (365일 → 365초)
   - 레벨 전환 로직 실시간 검증 가능

2. **버그 발견 용이**
   - 레벨 경계값(3일, 7일, 100일 등) 빠른 테스트
   - 레벨 색상, 타이틀 변경 즉시 확인

3. **안전한 구현**
   - 기존 정상 모드 동작 유지 (하위 호환성 ✅)
   - 테스트 모드만 별도 처리

## 🔒 주의사항

- **릴리즈 빌드에서는 테스트 모드 비활성화 필수**
- 로그 출력은 테스트 모드일 때만 활성화됨
- 업데이트 주기 변경은 배터리 소모와 무관 (디버그 전용)

---

**수정 완료 날짜:** 2025-12-03  
**빌드 상태:** ✅ BUILD SUCCESSFUL  
**테스트 상태:** 레벨 실시간 변경 확인 필요

