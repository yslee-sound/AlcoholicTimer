# Tab02 실시간 동기화 구현 완료

## 📋 문제점
사용자가 `QuitScreen`에서 타이머를 중단하고 기록을 저장한 후, **Tab 2(통계 화면)로 이동하면 방금 저장한 기록이 통계에 반영되지 않는 문제**가 있었습니다.

## 🔧 원인 분석
`Tab02ViewModel`이 앱 실행 시점에만 기록을 로드하고, 이후 SharedPreferences의 데이터 변경 사항을 감지하지 못했습니다.

## ✅ 해결 방법

### 1. SharedPreferences 변경 감지 리스너 추가

```kotlin
// [NEW] SharedPreferences 변경 감지 리스너
private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    when (key) {
        // [FIX] 기록 추가/삭제, 타이머 시작/완료 시 즉시 반영
        Constants.PREF_SOBRIETY_RECORDS,
        Constants.PREF_TIMER_COMPLETED,
        Constants.PREF_START_TIME -> {
            Log.d("Tab02ViewModel", "Data changed ($key), reloading records...")
            // 기록 목록 즉시 갱신 (QuitScreen에서 저장한 기록 반영)
            loadRecords()
        }
    }
}
```

### 2. 리스너 등록 (init 블록)

```kotlin
init {
    // [FIX] SharedPreferences 변경 감지 시작
    sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    Log.d("Tab02ViewModel", "Preference change listener registered")

    // [REFACTORED] TimerTimeManager의 elapsedMillis를 구독하여 통계 갱신
    viewModelScope.launch {
        TimerTimeManager.elapsedMillis.collect { elapsedMillis ->
            calculateStatsFromElapsed(elapsedMillis)
        }
    }
}
```

### 3. 메모리 누수 방지 (onCleared)

```kotlin
override fun onCleared() {
    super.onCleared()
    sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    Log.d("Tab02ViewModel", "Preference change listener unregistered")
}
```

## 📊 동작 흐름

### Before (수정 전)
1. 사용자가 Tab 1에서 7일 차 진행 중
2. 포기 버튼 롱프레스 → `QuitScreen`에서 기록 저장
3. Tab 2로 이동 → **통계에 0일로 표시** (기록 미반영)
4. 앱 재시작 후에야 기록이 보임

### After (수정 후)
1. 사용자가 Tab 1에서 7일 차 진행 중
2. 포기 버튼 롱프레스 → `QuitScreen`에서 기록 저장
   - SharedPreferences에 `PREF_SOBRIETY_RECORDS` 업데이트
3. **리스너가 즉시 감지** → `loadRecords()` 자동 호출
4. Tab 2로 이동 → **7일 치 통계가 즉시 반영** ✅

## 🎯 테스트 시나리오

### 시나리오 1: 타이머 포기 후 Tab 2 확인
1. Tab 1에서 타이머 시작 (1440배속 설정 시 빠르게 진행)
2. 7일 차쯤 포기 버튼 롱프레스
3. Tab 2로 이동
4. **예상 결과:** "총 금주일 7.0일", "절약한 돈" 등이 즉시 표시됨

### 시나리오 2: 타이머 완료 후 Tab 2 확인
1. Tab 1에서 타이머 시작 (목표: 1일)
2. 1일 달성 → 자동 완료
3. Tab 2로 이동
4. **예상 결과:** 완료된 1일 치 기록이 통계에 반영됨

### 시나리오 3: 다른 탭 이동 중 타이머 완료
1. Tab 1에서 타이머 시작 (목표: 1일, 1440배속)
2. 즉시 Tab 2로 이동
3. Tab 2에서 대기 (1분 후 타이머 완료)
4. **예상 결과:** 완료 순간 통계가 자동으로 업데이트됨

## 📝 구현 참고

이 구현은 `Tab03ViewModel`의 SharedPreferences 리스너 패턴을 참고했습니다.

**Tab03ViewModel 참고 코드:**
```kotlin
private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    when (key) {
        Constants.PREF_SOBRIETY_RECORDS,
        Constants.PREF_START_TIME,
        Constants.PREF_TIMER_COMPLETED -> {
            Log.d("Tab03ViewModel", "Data changed ($key), reloading...")
            _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
            loadRecordsAndCalculateTotalTime()
        }
    }
}
```

## ⚠️ 주의사항

1. **리스너 등록 순서:** `init` 블록에서 반드시 리스너를 먼저 등록한 후 데이터 로딩을 시작해야 합니다.
2. **메모리 누수:** `onCleared()`에서 반드시 `unregisterOnSharedPreferenceChangeListener`를 호출해야 합니다.
3. **Thread Safety:** `loadRecords()`는 `viewModelScope.launch` 내부에서 실행되므로 UI 스레드 블로킹 걱정이 없습니다.

## 🔍 디버깅 로그

수정 후 다음 로그들이 출력되어야 합니다:

```
D/Tab02ViewModel: Preference change listener registered
D/Tab02ViewModel: 기록 로딩 완료: 0개
D/Tab02ViewModel: Data changed (sobriety_records), reloading records...
D/Tab02ViewModel: 기록 로딩 완료: 1개
```

## 📅 작업 완료 일시
- **날짜:** 2025년 12월 11일
- **수정 파일:** `Tab02ViewModel.kt`
- **빌드 상태:** 성공 (BUILD SUCCESSFUL in 2s)
- **커밋 메시지 (권장):** `feat(tab02): Add SharedPreferences listener for real-time record sync`

---

**작성자:** GitHub Copilot  
**문서 타입:** 기능 구현 완료 보고서

