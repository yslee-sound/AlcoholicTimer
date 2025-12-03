# Legend 레벨(11) 표시 변경: "11" → "L"

## 📋 수정 내용

**변경 사항:**
- 11번째 레벨 "전설의 레전드"의 레벨 번호 표시를 **"11"**에서 **"L"**(Legend)로 변경

**적용 범위:**
1. ✅ 레벨 화면 메인 카드 (상단 큰 뱃지)
2. ✅ 레벨 화면 리스트 (11개 레벨 목록)
3. ✅ 타이머 진행 화면 (RunScreen)

## 🎯 수정 위치

### 1. Tab03.kt - 메인 레벨 카드 뱃지

**파일:** `ui/tab_03/Tab03.kt` (라인 254-257)

**Before:**
```kotlin
val levelNumber = LevelDefinitions.getLevelNumber(currentDays) + 1
Text(
    text = "LV.$levelNumber",
    ...
)
```

**After:**
```kotlin
val levelNumber = LevelDefinitions.getLevelNumber(currentDays) + 1
// [FIX] Legend 레벨(11)은 "L"로 표시
val levelText = if (levelNumber == 11) "L" else "$levelNumber"
Text(
    text = "LV.$levelText",
    ...
)
```

### 2. Tab03.kt - 레벨 리스트 아이템

**파일:** `ui/tab_03/Tab03.kt` (라인 416-419)

**Before:**
```kotlin
val levelNumber = LevelDefinitions.levels.indexOf(level) + 1
Text(
    text = "$levelNumber",
    ...
)
```

**After:**
```kotlin
val levelNumber = LevelDefinitions.levels.indexOf(level) + 1
// [FIX] Legend 레벨(11)은 "L"로 표시
val levelText = if (levelNumber == 11) "L" else "$levelNumber"
Text(
    text = levelText,
    ...
)
```

### 3. RunScreen.kt - 타이머 진행 화면

**파일:** `ui/tab_01/screens/RunScreen.kt` (라인 151-153)

**Before:**
```kotlin
val levelNumber = ... { LevelDefinitions.getLevelNumber(levelDays) + 1 }
val levelDisplayText = "Lv.$levelNumber"
```

**After:**
```kotlin
val levelNumber = ... { LevelDefinitions.getLevelNumber(levelDays) + 1 }
// [FIX] Legend 레벨(11)은 "L"로 표시
val levelDisplayText = if (levelNumber == 11) "Lv.L" else "Lv.$levelNumber"
```

## 📊 표시 비교

### Before (수정 전)

| 레벨 | 일수 | 표시 |
|------|------|------|
| Lv.9 | 180~299일 | "LV.9" / "9" |
| Lv.10 | 300~364일 | "LV.10" / "10" |
| **Lv.11** | **365일+** | **"LV.11" / "11"** ❌ |

### After (수정 후)

| 레벨 | 일수 | 표시 |
|------|------|------|
| Lv.9 | 180~299일 | "LV.9" / "9" |
| Lv.10 | 300~364일 | "LV.10" / "10" |
| **Legend** | **365일+** | **"LV.L" / "L"** ✅ |

## 🎨 UI 변경 미리보기

### 레벨 화면 메인 카드
```
┌─────────────────────────┐
│                         │
│     ╭───────╮           │
│     │       │           │
│     │ LV.L  │  ← 금색 뱃지
│     │       │           │
│     ╰───────╯           │
│                         │
│   전설의 레전드          │
│      365일+              │
│                         │
└─────────────────────────┘
```

### 레벨 리스트
```
┌─────────────────────────────────────┐
│ 10  금주 마스터         300~364일  ✓│
├─────────────────────────────────────┤
│  L  전설의 레전드        365일+    ★│  ← "11" 대신 "L"
└─────────────────────────────────────┘
```

### 타이머 진행 화면
```
┌─────────────────────┐
│   Lv.L              │  ← "Lv.11" 대신 "Lv.L"
│   전설의 레전드      │
│   400일차            │
└─────────────────────┘
```

## 🔧 구현 로직

### 조건 분기
```kotlin
val levelNumber = LevelDefinitions.getLevelNumber(currentDays) + 1

val levelText = if (levelNumber == 11) "L" else "$levelNumber"
```

**로직 설명:**
- `getLevelNumber()`는 0-indexed 반환 (0~10)
- `+ 1`로 1-indexed 변환 (1~11)
- 11이면 "L", 아니면 숫자 그대로

### 레벨 번호 매핑

| getLevelNumber() | +1 후 | 표시 |
|------------------|-------|------|
| 0 | 1 | "1" |
| 1 | 2 | "2" |
| ... | ... | ... |
| 9 | 10 | "10" |
| **10** | **11** | **"L"** ✅ |

## 🧪 테스트 시나리오

### 시나리오 1: Legend 레벨 달성 (테스트 모드)
1. 타이머 테스트 모드 ON (1초 = 1일)
2. 365일 목표로 타이머 시작
3. 365초 대기
4. ✅ 타이머 화면에 **"Lv.L"** 표시
5. 3번째 탭(레벨 화면) 진입
6. ✅ 메인 카드 뱃지: **"LV.L"** (금색)
7. ✅ 레벨 리스트: **"L" 전설의 레전드** (활성화)

### 시나리오 2: 10레벨에서 11레벨로 전환
1. 타이머 테스트 모드 ON
2. 364일 차 → **"Lv.10"** 표시
3. 1초 경과 (365일 차)
4. ✅ **"Lv.L"**로 변경 확인

### 시나리오 3: 레벨 리스트 확인
1. 레벨 화면 진입
2. 아래로 스크롤
3. ✅ 10번: "10 금주 마스터"
4. ✅ 11번: **"L 전설의 레전드"** (숫자가 아닌 L)

## 💡 디자인 의도

### 왜 "L"인가?

1. **특별함 강조**
   - 마지막 최고 레벨이라는 특별한 의미
   - 숫자가 아닌 문자로 차별화

2. **Legend 직관 표현**
   - L = Legend의 이니셜
   - 국제적으로 통용되는 표현

3. **UI 간결성**
   - "11"보다 "L" 한 글자가 더 깔끔
   - 금색 뱃지와 조화

4. **게임적 요소**
   - RPG, 모바일 게임에서 최고 등급은 숫자가 아닌 S, SS, L 등 특수 표기 사용
   - 사용자에게 친숙한 패턴

## 📝 수정 파일 목록

| 파일 | 수정 내용 |
|------|----------|
| `Tab03.kt` | 메인 카드 뱃지 "L" 표시 |
| `Tab03.kt` | 레벨 리스트 아이템 "L" 표시 |
| `RunScreen.kt` | 타이머 화면 "Lv.L" 표시 |

## ✅ 빌드 결과

```bash
BUILD SUCCESSFUL in 9s
42 actionable tasks: 12 executed, 6 from cache, 24 up-to-date
```

## 🚀 배포 준비

### 체크리스트
- ✅ 빌드 성공
- ✅ 3개 화면 모두 수정 완료
- ⏳ 실기기 테스트 (365일 달성 시 "L" 표시 확인)
- ⏳ 테스트 모드에서 레벨 전환 확인

### 다국어 지원
현재 "L"은 하드코딩되어 있으므로 모든 언어에서 동일하게 표시됩니다.
- ✅ 한국어: "LV.L" / "L"
- ✅ 영어: "LV.L" / "L"
- ✅ 일본어: "LV.L" / "L"
- ✅ 중국어: "LV.L" / "L"

**이유:** "Legend"의 L은 국제적으로 통용되는 표현

---

**수정 완료 날짜:** 2025-12-03  
**수정 파일:** Tab03.kt, RunScreen.kt  
**빌드 상태:** ✅ BUILD SUCCESSFUL  
**테스트 상태:** 365일 달성 시 "L" 표시 확인 필요 🎉

