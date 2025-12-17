# DetailScreen 기록 삭제 시 타이머 상태 초기화 수정

**작업 일자**: 2025년 12월 4일  
**작업 유형**: 버그 수정  
**심각도**: 🔴 Critical  
**상태**: ✅ 수정 완료

---

## 🐛 문제 상황

### 재현 시나리오
```
1. 타이머 완료 → '목표 달성 완료' 화면 진입
2. '결과 확인' 버튼 클릭 → '기록 상세(DetailScreen)' 진입
3. 기록 삭제 실행
4. ❌ 뒤로가기 후 다시 '목표 달성 완료' 화면이 뜸 (오류!)
5. ❌ '결과 확인' 재클릭 시 삭제된 데이터가 보이거나 오류 발생
```

### 근본 원인

**문제점**:
- 기록(sobriety_records)만 삭제하고 타이머 상태는 그대로 유지됨
- `PREF_TIMER_COMPLETED = true` 상태가 남아있음
- 앱이 여전히 "타이머가 완료된 상태"로 인식
- 백스택에서 GoalAchievedScreen으로 돌아가면 삭제된 데이터를 참조하려고 시도

---

## ✅ 해결 방법

### 수정된 로직

**DetailScreen.kt - deleteImpl() 함수**

```kotlin
if (removed > 0) {
    val committed = sharedPref.edit()
        .putString("sobriety_records", newArray.toString())
        .commit()
    
    if (committed) {
        // [FIX] 타이머 상태 초기화 (기록 삭제 시 타이머 완료 상태도 리셋)
        sharedPref.edit().apply {
            putBoolean(Constants.PREF_TIMER_COMPLETED, false) // ← 추가
            putLong(Constants.PREF_START_TIME, 0L)            // ← 추가
            commit()
        }
        Log.d("DetailScreen", "타이머 상태 초기화 완료")
        
        try { onDeleted?.invoke() } catch (_: Exception) {}
        Toast.makeText(context, "기록이 삭제되었습니다", Toast.LENGTH_SHORT).show()
    }
}
```

### 핵심 변경 사항

#### 1️⃣ **타이머 완료 상태 초기화**
```kotlin
putBoolean(Constants.PREF_TIMER_COMPLETED, false)
```
- 기존: `true` (타이머 완료 상태 유지) ❌
- 수정: `false` (타이머 초기 상태로 리셋) ✅

#### 2️⃣ **타이머 시작 시간 초기화**
```kotlin
putLong(Constants.PREF_START_TIME, 0L)
```
- 기존: 이전 타이머의 시작 시간 유지 ❌
- 수정: `0L` (시작 시간 리셋) ✅

---

## 🔄 수정 후 동작 흐름

```
1. 타이머 완료 → '목표 달성 완료' 화면 진입
   ↓
2. '결과 확인' 클릭 → '기록 상세' 진입
   ↓
3. 기록 삭제 실행
   ↓
   [삭제 로직]
   - sobriety_records에서 기록 제거 ✅
   - PREF_TIMER_COMPLETED = false ✅
   - PREF_START_TIME = 0L ✅
   - 타이머 상태 초기화 완료
   ↓
4. 뒤로가기 (onBack())
   ↓
5. ✅ GoalAchievedScreen이 타이머 완료 상태가 아님을 감지
   ↓
6. ✅ 자동으로 메인 타이머 화면(Tab 01)으로 이동
   ↓
7. ✅ 새로운 타이머 시작 가능 상태
```

---

## 📊 상태 변화 비교

### 수정 전 (버그)

| 단계 | PREF_TIMER_COMPLETED | PREF_START_TIME | sobriety_records |
|------|---------------------|-----------------|------------------|
| 타이머 완료 | `true` | `123456789` | `[{record1}]` |
| 기록 삭제 | `true` ❌ | `123456789` ❌ | `[]` |
| 결과 | 타이머 완료 상태 유지 | 삭제된 데이터 참조 시도 → 오류 |

### 수정 후 (정상)

| 단계 | PREF_TIMER_COMPLETED | PREF_START_TIME | sobriety_records |
|------|---------------------|-----------------|------------------|
| 타이머 완료 | `true` | `123456789` | `[{record1}]` |
| 기록 삭제 | `false` ✅ | `0L` ✅ | `[]` |
| 결과 | 초기 상태 복구 | 새 타이머 시작 가능 |

---

## 🎯 타이머 상태 초기화 로직 통일

이번 수정으로 다음 시나리오들이 동일한 초기화 로직을 사용하게 됨:

### 1️⃣ **타이머 포기 (QuitScreen)**
```kotlin
sharedPref.edit().apply {
    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
    putLong(Constants.PREF_START_TIME, 0L)
    commit()
}
```

### 2️⃣ **새 타이머 시작 (StartScreen)**
```kotlin
sharedPref.edit().apply {
    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
    putLong(Constants.PREF_START_TIME, System.currentTimeMillis())
    commit()
}
```

### 3️⃣ **기록 삭제 (DetailScreen)** 🆕
```kotlin
sharedPref.edit().apply {
    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
    putLong(Constants.PREF_START_TIME, 0L)
    commit()
}
```

---

## 🧪 테스트 시나리오

### 시나리오 1: 정상 삭제 흐름
```
✅ 타이머 완료 → 기록 상세 → 삭제 → 뒤로가기 → 메인 화면
```

### 시나리오 2: 삭제 후 새 타이머 시작
```
✅ 타이머 완료 → 기록 상세 → 삭제 → 뒤로가기 → 새 타이머 시작 → 정상 작동
```

### 시나리오 3: 삭제 취소
```
✅ 타이머 완료 → 기록 상세 → 삭제 다이얼로그 → 취소 → 기록 유지
```

---

## 📝 추가 개선 사항 (선택)

### 현재 구현
- 기록 삭제 → `onBack()` 호출 → 이전 화면으로 복귀
- GoalAchievedScreen에서 타이머 상태 확인 후 자동 이동

### 향후 개선 가능
```kotlin
// DetailScreen에 onNavigateToHome 콜백 추가
fun DetailScreen(
    // ...existing parameters...
    onNavigateToHome: (() -> Unit)? = null, // 추가
    // ...
) {
    // 삭제 확인 버튼
    confirmButton = {
        TextButton(onClick = {
            val action: (Long, Long) -> Unit = onDelete ?: { a, b -> deleteImpl(a, b) }
            action(startTime, endTime)
            
            // [OPTION] 직접 홈으로 이동
            onNavigateToHome?.invoke() ?: onBack()
        }) {
            Text("삭제")
        }
    }
}
```

**장점**: 백스택을 거치지 않고 직접 메인 화면으로 이동  
**현재**: 기존 네비게이션 구조를 유지하면서 자동 처리

---

## ✅ 빌드 결과

```
BUILD SUCCESSFUL in 2s
```

---

## 🎉 최종 결과

**기록 삭제 시 타이머 상태가 정상적으로 초기화됩니다!**
- ✅ `PREF_TIMER_COMPLETED = false` 설정
- ✅ `PREF_START_TIME = 0L` 설정
- ✅ 타이머 초기 상태로 복구
- ✅ 삭제된 데이터 참조 오류 방지
- ✅ 새 타이머 시작 가능 🚀

---

**수정된 파일**:
- `ui/screens/DetailScreen.kt`

**수정된 함수**:
- `deleteImpl(s: Long, e: Long)`

**추가된 로직**:
- 타이머 완료 상태 초기화
- 타이머 시작 시간 초기화

