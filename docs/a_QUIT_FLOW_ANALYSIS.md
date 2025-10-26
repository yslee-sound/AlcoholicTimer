# 금주 종료 플로우 분석

**작성일**: 2025-10-26  
**목적**: QuitActivity → StartActivity 이동 문제 분석

---

## 🔍 현재 구현된 플로우

### 1️⃣ 금주 진행 중 → 중지 버튼 클릭

**RunActivity**
```kotlin
// 하단 중지 버튼 클릭
→ QuitActivity 시작
```

### 2️⃣ QuitActivity 화면

**사용자 선택지**:

#### A) 빨간 버튼 (중지) - 롱프레스 1.5초
```kotlin
progress >= 1f && isPressed
  ↓
1. saveCompletedRecord() // 기록 저장
2. sharedPref.edit {
     remove(PREF_START_TIME)      // start_time 삭제
     putBoolean(PREF_TIMER_COMPLETED, true)
   }
3. navigateToStart() 호출
   ↓
   Intent(StartActivity).apply {
     addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK)
   }
   ↓
4. StartActivity로 이동 ✅
5. finish() - QuitActivity 종료
```

**결과**: StartActivity 표시 ✅

#### B) 초록 버튼 (계속하기) - 일반 클릭
```kotlin
onClick = { 
  (context as? QuitActivity)?.finish()
}
```

**결과**: RunActivity로 복귀 (금주 계속) ✅

#### C) 시스템 뒤로가기
```kotlin
// BackHandler 없음
→ 기본 동작: finish()
```

**결과**: RunActivity로 복귀 ✅

#### D) 빨간 버튼 중간에 손 뗌
```kotlin
progress < 1f
  ↓
waitForUpOrCancellation()
  ↓
isPressed = false
  ↓
progress 서서히 감소
```

**결과**: 아무 일도 안 일어남, QuitActivity에 그대로 머물음 ✅

---

## 3️⃣ StartActivity 조건 체크

**금주 종료 후 StartActivity 실행 시**:

```kotlin
LaunchedEffect(Unit) {
    startTime = sharedPref.getLong("start_time", 0L)       // = 0L (삭제됨)
    timerCompleted = sharedPref.getBoolean("timer_completed", false)  // = true
}

// 조건 체크
if (!gateNavigation && startTime != 0L && !timerCompleted) {
    // RunActivity로 이동
}
```

**조건 분석**:
- `gateNavigation` = false (기본값)
- `startTime` = **0L** (삭제됨)
- `timerCompleted` = **true**

**조건**: `false && false && false` = **FALSE** ✅

**결과**: RunActivity로 이동하지 않음 ✅  
→ StartActivity에 머무름 ✅

---

## 4️⃣ 새로운 금주 시작

**StartActivity에서 시작 버튼 클릭**:

```kotlin
sharedPref.edit {
    putFloat("target_days", formatted)
    putLong("start_time", System.currentTimeMillis())  // 새로운 시간
    putBoolean("timer_completed", false)                 // 초기화!
}
```

**결과**: RunActivity로 이동 ✅

---

## 🎯 정상 작동 확인

### ✅ 시나리오 1: 금주 정상 종료
```
1. RunActivity → 중지 버튼
2. QuitActivity → 빨간 버튼 롱프레스 완료 (1.5초)
3. StartActivity로 이동 ✅
4. 다시 RunActivity로 이동 안 됨 ✅
5. 새 금주 시작 가능 ✅
```

### ✅ 시나리오 2: 금주 취소
```
1. RunActivity → 중지 버튼
2. QuitActivity → 초록 버튼 (계속하기)
3. RunActivity로 복귀 ✅
4. 금주 계속 진행 ✅
```

### ✅ 시나리오 3: 뒤로가기
```
1. RunActivity → 중지 버튼
2. QuitActivity → 시스템 뒤로가기
3. RunActivity로 복귀 ✅
4. 금주 계속 진행 ✅
```

### ✅ 시나리오 4: 롱프레스 중단
```
1. RunActivity → 중지 버튼
2. QuitActivity → 빨간 버튼 누름 (0.5초만)
3. 손 뗌 → 진행바 사라짐
4. QuitActivity에 그대로 머물음 ✅
5. 다시 선택 가능 ✅
```

---

## 🐛 사용자가 경험한 문제 추측

### 가능성 1: 롱프레스 미완료
```
사용자: "종료했다고 생각했는데..."
실제: 1.5초를 채우지 못하고 손을 뗌
결과: 아무 일도 안 일어남
      → 뒤로가기/초록 버튼으로 나감
      → RunActivity로 복귀
```

### 가능성 2: 초록 버튼 클릭
```
사용자: "종료 화면에서 나왔는데..."
실제: 빨간 버튼이 아닌 초록 버튼 클릭
결과: 금주 계속 (의도된 동작)
      → RunActivity로 복귀
```

### 가능성 3: Intent 플래그 이슈 (이전 버전)
```
이전: FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP
문제: 기존 StartActivity 재사용
      → SharedPreferences 업데이트 안 됨
      → 오래된 값으로 RunActivity 이동

현재: FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK ✅
해결: 완전히 새로운 StartActivity 생성
      → 최신 SharedPreferences 로드
      → 정상 작동
```

---

## 💡 개선 제안

### 제안 1: 롱프레스 진행 상태 더 명확히 표시
```kotlin
// 현재: CircularProgressIndicator만
// 개선: 퍼센트 또는 "1.5초 동안 누르세요" 텍스트 추가
```

### 제안 2: 완료 햅틱 피드백
```kotlin
if (progress >= 1f && isPressed) {
    // 진동으로 완료 알림
    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
```

### 제안 3: 디버그 로그 강화
```kotlin
Log.d("QuitActivity", "롱프레스 완료 - StartActivity로 이동")
Log.d("StartActivity", "금주 종료 후 진입 - start_time=$startTime, completed=$timerCompleted")
```

---

## 🎯 결론

**현재 코드는 정상적으로 작동합니다!**

### 확인된 사항
- ✅ 금주 종료 시 StartActivity로 정확히 이동
- ✅ `start_time` 삭제 및 `timer_completed = true` 설정
- ✅ StartActivity에서 RunActivity로 재이동하지 않음
- ✅ 새로운 금주 시작 시 정상 초기화

### 사용자 혼란 가능성
- ⚠️ 롱프레스 1.5초를 완료하지 못함
- ⚠️ 초록 버튼(계속하기)과 빨간 버튼(종료) 혼동
- ⚠️ 뒤로가기로 나가면 RunActivity 복귀 (의도된 동작)

### 권장 사항
1. **사용자 테스트**: 실제 롱프레스 완료 여부 확인
2. **로그 확인**: Logcat에서 "StartActivity로 이동" 로그 확인
3. **UI 개선**: 진행 상태 더 명확히 표시

---

**문서 버전**: 1.0  
**마지막 업데이트**: 2025-10-26

