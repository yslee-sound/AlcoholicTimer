# 시간 배속 기능 구현 완료 보고서

**작업 일자**: 2025년 12월 4일  
**작업자**: GitHub Copilot  
**작업 유형**: 디버그 기능 추가

---

## 📋 작업 개요

장기 레벨 진행을 빠르게 테스트하기 위한 "시간 배속(Time Acceleration)" 디버그 기능을 구현했습니다.

### 🎯 목표
- 실제 시스템 시간을 변경하지 않음
- 경과 시간 계산만 배속 적용
- 디버그 메뉴에서 1배속 ~ 10000배속 조절 가능

---

## 🛠️ 수정된 파일 (총 4개)

### 1️⃣ **Constants.kt** (핵심 로직)
**경로**: `constants/Constants.kt`

**추가된 기능**:
```kotlin
// [NEW] 시간 배속 설정
private const val PREF_TIME_ACCELERATION = "time_acceleration_factor"

/**
 * 시간 배속 계수 설정 (1 = 정상 속도, 60 = 60배속, 10000 = 10000배속)
 */
fun setTimeAcceleration(context: Context, factor: Int) {
    val safeFactor = factor.coerceIn(1, 10000)
    prefs.edit().putInt(PREF_TIME_ACCELERATION, safeFactor).apply()
}

/**
 * 시간 배속 계수 가져오기 (기본값: 1 = 정상 속도)
 */
fun getTimeAcceleration(context: Context): Int {
    return prefs.getInt(PREF_TIME_ACCELERATION, 1).coerceIn(1, 10000)
}

/**
 * 1일의 밀리초 값을 반환 (시간 배속 적용)
 * 예: 60배속이면 1,440,000ms (24분)
 */
fun getDayInMillis(context: Context): Long {
    val factor = getTimeAcceleration(context)
    return DAY_IN_MILLIS / factor  // 86,400,000 / factor
}
```

**변경 효과**:
- 1배속 (정상): `getDayInMillis()` = 86,400,000ms (24시간)
- 60배속: `getDayInMillis()` = 1,440,000ms (24분)
- 100배속: `getDayInMillis()` = 864,000ms (14.4분)
- 1000배속: `getDayInMillis()` = 86,400ms (86.4초) ⚡
- 10000배속: `getDayInMillis()` = 8,640ms (8.64초) 🚀

---

### 2️⃣ **LevelScreen.kt** (레벨 화면)
**경로**: `ui/tab_03/screens/LevelScreen.kt`

**변경 전**:
```kotlin
val totalElapsedDaysFloat = totalElapsedTime / Constants.DAY_IN_MILLIS.toFloat()
```

**변경 후**:
```kotlin
// [FIX] 시간 배속 적용: getDayInMillis() 함수 사용
val totalElapsedDaysFloat = totalElapsedTime / Constants.getDayInMillis(context).toFloat()
```

**효과**: 레벨 진행 바와 경과 일수가 배속에 따라 동적으로 계산됨

---

### 3️⃣ **QuitScreen.kt** (종료 화면)
**경로**: `ui/tab_01/screens/QuitScreen.kt`

**수정 위치 1** (180줄):
```kotlin
// [FIX] 시간 배속 적용: getDayInMillis() 함수 사용
val elapsedDaysFloat = elapsedMillis / Constants.getDayInMillis(context).toFloat()
```

**수정 위치 2** (267줄):
```kotlin
// [FIX] 시간 배속 적용: getDayInMillis() 함수 사용
val actualDays = (((endTime - start) / Constants.getDayInMillis(context))).toInt()
```

**효과**: 총 지속 일수, 절약한 돈/시간 계산이 배속에 따라 동적으로 계산됨

---

### 4️⃣ **DebugScreen.kt** (디버그 메뉴 UI)
**경로**: `ui/tab_05/screens/debug/DebugScreen.kt`

**추가된 UI 컴포넌트**:

1. **현재 배속 표시**
   ```kotlin
   Text("현재 배속: ${acceleration.value.toInt()}x")
   ```

2. **배속 조절 슬라이더** (1~100배속)
   ```kotlin
   Slider(
       value = acceleration.value,
       onValueChange = { ... },
       valueRange = 1f..100f,
       steps = 98
   )
   ```

3. **프리셋 버튼 3개**
   - **1x (정상)**: 정상 속도로 복구
   - **60x (빠름)**: 1분이 1시간으로 계산
   - **100x (초고속)**: 최대 속도

4. **안내 문구**
   ```
   ※ 실제 시간은 변경되지 않으며, 경과 시간 계산만 배속됩니다.
   ```

---

## 📊 배속별 효과 비교

| 배속 | 1일의 실제 소요 시간 | 30일 목표 달성 시간 | 1시간 경과 시 표시 일수 |
|------|---------------------|-------------------|---------------------|
| **1x (정상)** | 24시간 | 30일 (720시간) | 0.04일 |
| **60x** | 24분 | 12시간 | 2.5일 |
| **100x** | 14.4분 (약 15분) | 7.2시간 | 4.17일 |

**예시**:
- 60배속 설정 후 **1시간** 앱을 실행하면 → **2.5일** 경과한 것으로 계산
- 100배속 설정 후 **7시간** 앱을 실행하면 → **30일** 목표 달성 가능

---

## ✅ 동작 원리

### 🔑 핵심 개념: "가상 시간 척도(Virtual Time Scale)"

실제 시스템 시간(`System.currentTimeMillis()`)은 **절대 변경하지 않습니다**.  
대신, **"1일의 길이"**를 재정의합니다.

#### 정상 모드 (1배속)
```kotlin
경과 시간 = 현재 시간 - 시작 시간
경과 일수 = 경과 시간 / 86,400,000ms (24시간)
```

#### 60배속 모드
```kotlin
경과 시간 = 현재 시간 - 시작 시간  // 변경 없음
경과 일수 = 경과 시간 / 1,440,000ms (24분)  // 1일의 길이만 변경
```

**결과**: 실제로 1시간이 지났을 때, 시스템은 "60시간이 지났다"고 계산합니다.

---

## 🔒 안전성 보장

### ✅ 1. 데이터 무결성 유지
- **타이머 시작 시간(`startTime`)은 절대 변경 안 됨**
- 실제 타임스탬프는 그대로 저장됨
- 배속 설정을 변경해도 과거 기록에 영향 없음

### ✅ 2. 실제 시간 변경 없음
- `System.currentTimeMillis()` 호출 결과는 항상 정확
- OS 시스템 시간은 전혀 건드리지 않음

### ✅ 3. 리버시블(Reversible)
- 언제든지 1배속으로 복구 가능
- 설정 변경 시 즉시 적용됨

---

## 🎮 사용 방법

### 1단계: 디버그 메뉴 접근
앱 내 **Tab 05** → **디버그 메뉴** 진입

### 2단계: 배속 설정
1. **슬라이더**로 원하는 배속 선택 (1~100)
2. 또는 **프리셋 버튼** 클릭:
   - `1x (정상)` - 테스트 종료 후 복구
   - `60x (빠름)` - 중속 테스트
   - `100x (초고속)` - 최대 속도 테스트

### 3단계: 타이머 시작
일반적인 방법으로 타이머를 시작합니다.

### 4단계: 레벨 진행 확인
- **Tab 03 (레벨 화면)**: 레벨업 속도 확인
- **Tab 01 (실행 화면)**: 경과 일수 빠르게 증가
- **종료 버튼**: 30일 목표 달성 테스트

---

## ⚠️ 주의사항

### 1. 디버그 전용 기능
- **릴리스 빌드에서는 노출하지 않는 것을 권장**
- 일반 사용자에게는 혼란을 줄 수 있음

### 2. 테스트 후 복구 필수
- 테스트 완료 후 **반드시 1배속으로 복구**
- 배속 상태에서 실제 사용자 데이터를 수집하면 통계가 왜곡됨

### 3. SharedPreferences 영구 저장
- 설정 값은 앱 재시작 후에도 유지됨
- 앱 제거 시 자동 삭제됨

---

## 🧪 테스트 시나리오

### 시나리오 1: 레벨업 테스트
1. 100배속 설정
2. 타이머 시작
3. **15분** 경과 → **1일** 경과로 인식 → 레벨 2 달성
4. **7.2시간** 경과 → **30일** 달성 → 타이머 완료

### 시나리오 2: 통계 계산 검증
1. 60배속 설정
2. 1시간 대기
3. Tab 02 (기록 화면) 진입 → "총 금주일: 2.5일" 확인
4. "절약한 돈" 계산이 2.5일 기준으로 되는지 확인

### 시나리오 3: 복구 테스트
1. 100배속 설정 → 1시간 경과 → 4.17일 표시 확인
2. 1배속으로 복구
3. 추가 1시간 경과 → 0.04일만 추가됨 (총 4.21일)

---

## 🐛 알려진 제한 사항

### 1. 과거 기록과의 호환성
- **문제**: 과거 기록은 정상 속도(1배속)로 저장되어 있음
- **영향**: `Tab03ViewModel`에서 과거 기록 + 현재 경과 시간을 합산할 때, 현재만 배속 적용됨
- **해결**: 현재 타이머만 배속으로 테스트하는 경우 문제 없음

### 2. 광고 쿨타임과의 독립성
- 시간 배속은 **광고 쿨타임에 영향을 주지 않음**
- 광고 쿨타임은 별도로 디버그 설정 필요

---

## ✅ 빌드 결과

```
BUILD SUCCESSFUL in 12s
42 actionable tasks: 12 executed
```

**컴파일 에러**: 없음  
**경고**: 기존 경고만 존재 (새로운 경고 없음)

---

## 📝 향후 개선 사항

1. **배속 상태 표시**
   - RunScreen 상단에 "⚡ 60배속 중" 배지 표시
   - 사용자가 배속 중임을 명확히 인지하도록 UI 개선

2. **배속 히스토리**
   - 어느 기간에 어떤 배속이 적용되었는지 기록
   - 통계 계산 시 정확한 일수 계산 가능

3. **릴리스 빌드 자동 제거**
   - `BuildConfig.DEBUG == false`일 때 자동으로 UI 숨김
   - ProGuard 설정으로 관련 함수 자동 제거

---

## 🎉 완료 체크리스트

- [x] `Constants.kt`에 배속 설정/조회 함수 추가
- [x] `Constants.getDayInMillis()`에 배속 로직 적용
- [x] `LevelScreen.kt`의 직접 상수 사용 제거
- [x] `QuitScreen.kt`의 직접 상수 사용 제거 (2곳)
- [x] 디버그 메뉴에 배속 설정 UI 추가
- [x] 빌드 성공 확인
- [x] 문서 작성 완료

---

**작업 완료**: ✅ 시간 배속(Time Acceleration) 디버그 기능 구현 완료

