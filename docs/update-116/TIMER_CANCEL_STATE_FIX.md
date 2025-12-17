# 타이머 취소 후 탭 이동 시 잘못된 화면 표시 문제 해결

## 📋 문제 상황

**증상:**
1. 타이머 탭에서 목표를 설정하고 타이머 시작
2. [포기] 버튼을 눌러 타이머 중지
3. 다른 탭(예: 통계 탭)으로 이동
4. 다시 타이머 탭으로 돌아옴
5. **[오류]** 초기 목표 설정 화면이 아닌, 엉뚱하게 **목표 달성 성공 화면**이 표시됨

## 🔍 원인 분석

### 문제의 핵심
- **'취소(User Canceled)'와 '성공(Completed)'을 구분하는 명확한 상태 플래그가 없었음**
- `PREF_TIMER_COMPLETED` 플래그가 포기와 성공 모두에서 `true`로 설정되어 혼란 발생

### 기존 로직의 문제점

**QuitScreen.kt (기존 코드):**
```kotlin
// 포기 버튼을 누르면
sharedPref.edit {
    putBoolean(Constants.PREF_TIMER_COMPLETED, true)  // ❌ 취소인데 완료로 표시
    remove(Constants.PREF_START_TIME)
}
```

**RunScreen.kt:**
```kotlin
LaunchedEffect(startTime, timerCompleted) {
    if (timerCompleted || startTime == 0L) {
        onRequireBackToStart?.invoke()  // Start 화면으로 이동
    }
}
```

**문제 발생 시나리오:**
1. 사용자가 포기 → `timerCompleted = true`, `startTime = 0L`
2. 다른 탭으로 이동
3. 타이머 탭으로 복귀 시, RunScreen이 로드되며 상태 확인
4. `timerCompleted = true`를 보고 "완료됐구나!"라고 착각
5. NavGraph가 Finished 화면으로 이동시킴 ❌

## ✅ 해결 방법

### 수정 사항 1: QuitScreen.kt

**포기 시 상태를 명확히 'false'로 설정**

```kotlin
// [FIX] 포기 시 완료 상태를 false로 설정 (취소는 완료가 아님)
sharedPref.edit {
    putBoolean(Constants.PREF_TIMER_COMPLETED, false)  // ✅ 취소는 완료가 아님
    remove(Constants.PREF_START_TIME)
}

// [FIX] TimerStateRepository에도 명확히 취소 상태 저장
try {
    kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
    kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
    android.util.Log.d("QuitScreen", "타이머 취소: 완료 상태 false로 설정")
} catch (t: Throwable) {
    android.util.Log.e("QuitScreen", "타이머 상태 저장 실패", t)
}
```

### 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     타이머 상태 관리                          │
└─────────────────────────────────────────────────────────────┘

[초기 상태]
  PREF_START_TIME = 0
  PREF_TIMER_COMPLETED = false
  TimerStateRepository.isTimerActive() = false
        │
        │ [시작 버튼 클릭]
        ↓
[타이머 작동 중]
  PREF_START_TIME = 현재시각
  PREF_TIMER_COMPLETED = false
  TimerStateRepository.isTimerActive() = true
        │
        ├─── [포기 버튼 클릭] ─────→ [취소 상태] ✅ 수정됨
        │                           PREF_START_TIME = 0
        │                           PREF_TIMER_COMPLETED = false ← 변경!
        │                           TimerStateRepository.isTimerActive() = false
        │
        └─── [목표 시간 도달] ─────→ [완료 상태]
                                     PREF_START_TIME = 0
                                     PREF_TIMER_COMPLETED = true
                                     TimerStateRepository.isTimerFinished() = true
```

## 🧪 검증 시나리오

### 시나리오 1: 포기 후 탭 이동 (수정 전 오류 케이스)
1. ✅ 타이머 시작
2. ✅ [포기] 버튼 클릭 → Start 화면으로 이동
3. ✅ 통계 탭으로 이동
4. ✅ 타이머 탭으로 복귀 → **Start 화면 유지** (성공 화면 X)

### 시나리오 2: 정상 완료
1. ✅ 타이머 시작
2. ✅ 목표 시간 도달 → Finished 화면으로 이동
3. ✅ 통계 탭으로 이동
4. ✅ 타이머 탭으로 복귀 → **Finished 화면 유지**

### 시나리오 3: 앱 재시작 후 상태 복원
1. ✅ 타이머 실행 중 앱 종료
2. ✅ 앱 재시작 → Run 화면 복원
3. ✅ 포기 버튼 클릭
4. ✅ 앱 재시작 → Start 화면 표시 (Finished X)

## 📝 수정 파일

| 파일 | 수정 내용 |
|------|----------|
| `QuitScreen.kt` | 포기 시 `PREF_TIMER_COMPLETED = false`로 설정 |
| `QuitScreen.kt` | `TimerStateRepository.setTimerFinished(false)` 호출 추가 |

## 🔑 핵심 원칙

> **"타이머가 꺼져있다고 해서 다 성공한 게 아니다."**
> 
> **'포기'해서 꺼진 건지, '다 채워서' 꺼진 건지를 명확히 구분해야 한다.**

## ✅ 빌드 결과

```bash
BUILD SUCCESSFUL in 7s
42 actionable tasks: 12 executed, 6 from cache, 24 up-to-date
```

**수정 완료 날짜:** 2025-12-03

