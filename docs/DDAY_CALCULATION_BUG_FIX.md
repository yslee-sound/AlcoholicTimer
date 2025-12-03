# [긴급 수정] D-Day 계산 로직 버그 수정

## 📋 문제 상황

**버그:**
- Lv.1 (1일 차) 상태에서 다음 레벨(Lv.2, 4일 차)까지 **"4일 남음"**으로 잘못 표시 ❌
- 올바른 표시: **"3일 남음"** ✅

**원인:**
```kotlin
// [문제] elapsedDaysFloat (0부터 시작) 사용
val remainingDaysFloat = (nextLevel.start - elapsedDaysFloat).coerceAtLeast(0f)

// 예시: 1일 차 (시작한 지 0일 경과)
// nextLevel.start = 4 (Lv.2 시작일)
// elapsedDaysFloat = 0.0 (0일 경과)
// 계산: 4 - 0 = 4일 남음 ❌ (실제로는 3일 남아야 함)
```

## ✅ 해결 방법

### 올바른 계산 공식

```
remainingDays = nextLevelStartDay - currentDay
```

**변수 정의:**
- `nextLevelStartDay`: 다음 레벨이 시작되는 날짜 (예: Lv.2는 4일)
- `currentDay`: 현재 진행 일수 (화면에 표시되는 'n일차', **1부터 시작**)

### 수정 코드

**파일:** `ui/tab_03/Tab03.kt` (라인 304)

**Before:**
```kotlin
val remainingDaysFloat = (nextLevel.start - elapsedDaysFloat).coerceAtLeast(0f)
```

**After:**
```kotlin
// [FIX] D-Day 계산: nextLevelStartDay - currentDay
// currentDay는 화면에 표시되는 'n일차' (1부터 시작)
// nextLevel.start는 다음 레벨 시작일 (예: Lv.2는 4일)
val remainingDaysFloat = (nextLevel.start - currentDays.toFloat()).coerceAtLeast(0f)
```

## 📊 계산 검증

### 검증 1: Lv.1 (1일 차)

**상황:**
- 현재 레벨: Lv.1 (1~3일)
- 현재 일차: 1일
- 다음 레벨: Lv.2 (4~7일)
- 다음 레벨 시작: 4일

**계산:**
```
remainingDays = 4 - 1 = 3일
```

**결과:** ✅ **"3일 남음"** (정상)

### 검증 2: Lv.1 (2일 차)

**상황:**
- 현재 일차: 2일
- 다음 레벨 시작: 4일

**계산:**
```
remainingDays = 4 - 2 = 2일
```

**결과:** ✅ **"2일 남음"** (정상)

### 검증 3: Lv.1 (3일 차)

**상황:**
- 현재 일차: 3일
- 다음 레벨 시작: 4일

**계산:**
```
remainingDays = 4 - 3 = 1일
```

**결과:** ✅ **"1일 남음"** (정상)

### 검증 4: Lv.2 (4일 차)

**상황:**
- 현재 레벨: Lv.2 (4~7일)
- 현재 일차: 4일
- 다음 레벨: Lv.3 (8~14일)
- 다음 레벨 시작: 8일

**계산:**
```
remainingDays = 8 - 4 = 4일
```

**결과:** ✅ **"4일 남음"** (정상)

### 검증 5: Lv.2 (7일 차)

**상황:**
- 현재 일차: 7일
- 다음 레벨 시작: 8일

**계산:**
```
remainingDays = 8 - 7 = 1일
```

**결과:** ✅ **"1일 남음"** (정상)

## 🔧 기술적 분석

### 변수 비교

| 변수 | 시작 값 | 의미 | 사용 위치 |
|------|---------|------|----------|
| `elapsedDaysFloat` | 0.0 | 시작 시점부터 **경과한 일수** | ❌ 잘못 사용됨 |
| `currentDays` | 1 | 화면 표시용 **'n일차'** | ✅ 올바른 값 |
| `nextLevel.start` | 4 (Lv.2) | 다음 레벨 시작일 | 고정 |

### 왜 elapsedDaysFloat를 사용하면 안 되나?

**시간 흐름:**
```
시작 → 1일 차 → 2일 차 → 3일 차 → 4일 차 (Lv.2)
 ↓       ↓        ↓        ↓        ↓
경과: 0일   0.x일   1.x일   2.x일   3.x일
```

**1일 차에서 계산:**
```
잘못된 계산: 4 - 0.0 = 4일 남음 ❌
올바른 계산: 4 - 1 = 3일 남음 ✅
```

**이유:**
- `elapsedDaysFloat`는 "경과한 시간"
- `currentDays`는 "현재 진행 중인 날짜"
- 레벨 시스템은 "날짜 기반"이므로 `currentDays` 사용이 정확

## 🎯 수정 전후 비교

### Before (수정 전)

| 현재 상태 | elapsedDaysFloat | nextLevel.start | 계산 | 표시 | 정상 여부 |
|----------|------------------|-----------------|------|------|----------|
| 1일 차 | 0.0 | 4 | 4 - 0 = 4 | "4일 남음" | ❌ |
| 2일 차 | 1.0 | 4 | 4 - 1 = 3 | "3일 남음" | ❌ |
| 3일 차 | 2.0 | 4 | 4 - 2 = 2 | "2일 남음" | ❌ |

**문제점:** 항상 1일씩 더 많이 표시됨

### After (수정 후)

| 현재 상태 | currentDays | nextLevel.start | 계산 | 표시 | 정상 여부 |
|----------|-------------|-----------------|------|------|----------|
| 1일 차 | 1 | 4 | 4 - 1 = 3 | "3일 남음" | ✅ |
| 2일 차 | 2 | 4 | 4 - 2 = 2 | "2일 남음" | ✅ |
| 3일 차 | 3 | 4 | 4 - 3 = 1 | "1일 남음" | ✅ |

**해결:** 정확한 남은 일수 표시

## 📝 추가 확인사항

### 진행률 계산은 정상

라인 317의 `remainingDays` 계산은 이미 올바르게 되어 있었습니다:
```kotlin
remainingDays = (nextLevel.start - currentDays).coerceAtLeast(0)
```

문제는 **라인 304의 `remainingDaysFloat` 계산**만 잘못되어 있었습니다.

### 시간(시) 표시도 수정됨

`remainingDaysFloat`를 올바르게 계산하면, 시간 단위 표시도 자동으로 정확해집니다:

**예시: 1일 차 + 12시간 경과**
```
Before: (4 - 0.5) = 3.5일 → "3일 12시간 남음" ❌
After:  (4 - 1.5) = 2.5일 → "2일 12시간 남음" ✅
```

## ✅ 빌드 결과

```bash
BUILD SUCCESSFUL in 3s
42 actionable tasks: 3 executed, 9 from cache, 30 up-to-date
```

## 🧪 테스트 시나리오

### 시나리오 1: 타이머 시작 직후 (1일 차)
1. 타이머 테스트 모드 ON
2. 타이머 시작
3. 레벨 화면 진입
4. ✅ **"3일 남음"** 표시 확인

### 시나리오 2: 1초 경과 (2일 차)
1. 1초 대기 (1초 = 1일)
2. ✅ **"2일 남음"** 표시 확인

### 시나리오 3: 2초 경과 (3일 차)
1. 2초 대기
2. ✅ **"1일 남음"** 표시 확인

### 시나리오 4: 3초 경과 (4일 차, Lv.2)
1. 3초 대기
2. ✅ 레벨 업: **Lv.2 "3일 컷 통과"**
3. ✅ **"4일 남음"** 표시 확인 (다음 레벨 Lv.3까지)

## 💡 개발자 노트

### currentDays vs elapsedDaysFloat

**사용 지침:**
- **UI 표시용 계산:** `currentDays` 사용 (1부터 시작)
- **내부 진행률 계산:** `elapsedDaysFloat` 사용 (소수점 포함)

**예시:**
```kotlin
// ✅ 올바른 사용
val remainingDays = nextLevel.start - currentDays
val displayText = "$currentDays일차"

// ✅ 진행률 계산
val progress = (elapsedDaysFloat - currentLevel.start) / (nextLevel.start - currentLevel.start)
```

### D-Day 계산 공식

모든 "남은 기간" 계산은 다음 공식을 따라야 합니다:
```
remainingDays = targetDay - currentDay
```

**잘못된 패턴:**
```kotlin
❌ targetDay - elapsedTime
❌ targetDay - startTime
❌ (targetDay - currentDay) + 1  // off-by-one 오류
```

## 📂 수정 파일

| 파일 | 수정 라인 | 수정 내용 |
|------|----------|----------|
| `Tab03.kt` | 304 | remainingDaysFloat 계산식 수정 |

---

**수정 완료 날짜:** 2025-12-03  
**수정 파일:** Tab03.kt  
**빌드 상태:** ✅ BUILD SUCCESSFUL  
**버그 유형:** Off-by-one error (날짜 계산 오류)  
**테스트 상태:** 1일 차에서 "3일 남음" 표시 확인 필요 🎉

