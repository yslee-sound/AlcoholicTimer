# 🐛 "모든 기록 삭제" 시 진행 중인 타이머까지 종료되는 버그 수정

**작업일**: 2026-01-02  
**문제**: 탭2의 "모든 기록 삭제" 버튼을 누르면 진행 중인 타이머까지 종료됨  
**상태**: ✅ 수정 완료

---

## 🐛 문제 분석

### 재현 시나리오
```
1. 사용자: 타이머 시작 (예: 1일 진행 중)
2. 사용자: 탭2 → 모든 기록 보기 → "모든 기록 삭제" 클릭
3. 결과: ❌ 완료된 과거 기록뿐만 아니라 진행 중인 타이머까지 종료됨!
```

### 원인: RecordsDataLoader.kt의 clearAllRecords()

**문제 코드 (수정 전)**:
```kotlin
fun clearAllRecords(context: Context): Boolean = try {
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    
    sharedPref.edit()
        .putString("sobriety_records", "[]")
        .putLong("start_time", 0L)  // ← 진행 중인 타이머까지 초기화!
        .putBoolean("timer_completed", false)  // ← 타이머 상태까지 초기화!
        .apply()
    
    // ...
}
```

**문제점**:
1. ❌ `start_time`을 0으로 초기화 → 진행 중인 타이머 종료
2. ❌ `timer_completed`를 false로 초기화 → 타이머 상태 리셋
3. ❌ "완료된 기록 삭제"와 "타이머 초기화"가 혼재됨

---

## ✅ 수정 내용

### RecordsDataLoader.kt - clearAllRecords() 수정

**수정 후 코드**:
```kotlin
fun clearAllRecords(context: Context): Boolean = try {
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    // Log before deletion
    val beforeJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
    val beforeStartTime = sharedPref.getLong("start_time", 0L)
    Log.d(TAG, "Records before deletion: $beforeJson")
    Log.d(TAG, "Start time before deletion: $beforeStartTime")

    // [FIX] 완료된 기록만 삭제, 진행 중인 타이머는 유지 (2026-01-02)
    sharedPref.edit()
        .putString("sobriety_records", "[]")
        // ✅ start_time과 timer_completed는 건드리지 않음 (진행 중인 타이머 유지)
        .apply()

    // Verify after deletion
    val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
    val afterStartTime = sharedPref.getLong("start_time", 0L)
    Log.d(TAG, "Records after deletion: $afterJson")
    Log.d(TAG, "Start time after deletion: $afterStartTime (should be preserved)")
    Log.d(TAG, "All completed records deleted successfully (timer preserved)")

    // ...
}
```

**변경 사항**:
- ✅ `.putLong("start_time", 0L)` 삭제
- ✅ `.putBoolean("timer_completed", false)` 삭제
- ✅ 로그 메시지 명확화: "timer preserved"

---

## 📊 수정 전/후 비교

### Before (버그)

```
사용자 상태:
- 진행 중인 타이머: 1일 (start_time = 1735689600000)
- 완료된 기록: 3개

"모든 기록 삭제" 클릭
↓
SharedPreferences:
- sobriety_records: "[]" ✅
- start_time: 0 ❌ (진행 중인 타이머 종료!)
- timer_completed: false ❌

결과:
- 완료된 기록 삭제됨 ✅
- 진행 중인 타이머도 종료됨 ❌
```

### After (수정)

```
사용자 상태:
- 진행 중인 타이머: 1일 (start_time = 1735689600000)
- 완료된 기록: 3개

"모든 기록 삭제" 클릭
↓
SharedPreferences:
- sobriety_records: "[]" ✅
- start_time: 1735689600000 ✅ (유지!)
- timer_completed: (변경 없음) ✅

결과:
- 완료된 기록 삭제됨 ✅
- 진행 중인 타이머 유지됨 ✅
```

---

## 🔍 SharedPreferences 키 역할

| 키 | 역할 | "모든 기록 삭제" 시 동작 |
|----|------|----------------------|
| `sobriety_records` | 완료된 과거 기록 JSON 배열 | ✅ 삭제 (빈 배열로 변경) |
| `start_time` | 진행 중인 타이머 시작 시간 | ✅ **유지** (건드리지 않음) |
| `timer_completed` | 타이머 완료 여부 | ✅ **유지** (건드리지 않음) |

**핵심**: "모든 기록 삭제"는 **완료된 기록(`sobriety_records`)만 삭제**해야 하며, **진행 중인 타이머(`start_time`)는 건드리면 안 됨**!

---

## 🧪 테스트 시나리오

### 시나리오 1: 타이머 진행 중 + 완료된 기록 있음

```
Before:
- 진행 중: 2일 째 타이머 (start_time = 1735689600000)
- 완료된 기록: 5개

Action: "모든 기록 삭제" 클릭

After:
- 진행 중: 2일 째 타이머 ✅ (유지됨!)
- 완료된 기록: 0개 ✅ (삭제됨)
```

### 시나리오 2: 타이머 없음 + 완료된 기록만 있음

```
Before:
- 진행 중: 없음 (start_time = 0)
- 완료된 기록: 3개

Action: "모든 기록 삭제" 클릭

After:
- 진행 중: 없음 ✅ (변경 없음)
- 완료된 기록: 0개 ✅ (삭제됨)
```

### 시나리오 3: 타이머 진행 중 + 완료된 기록 없음

```
Before:
- 진행 중: 1일 째 타이머 (start_time = 1735689600000)
- 완료된 기록: 0개

Action: "모든 기록 삭제" 클릭

After:
- 진행 중: 1일 째 타이머 ✅ (유지됨!)
- 완료된 기록: 0개 ✅ (이미 없음)
```

---

## 🎯 다른 화면과의 차이

### "모든 기록 삭제" vs "앱 데이터 초기화"

| 기능 | 위치 | 완료된 기록 | 진행 중 타이머 | 설정 |
|-----|------|----------|--------------|-----|
| **모든 기록 삭제** | 탭2 → 모든 기록 보기 | ✅ 삭제 | ✅ 유지 | ✅ 유지 |
| **앱 데이터 초기화** | 탭4 → 설정 | ✅ 삭제 | ✅ 삭제 | ✅ 삭제 |

**중요**: "모든 기록 삭제"는 **완료된 기록만 삭제**하고, "앱 데이터 초기화"는 **모든 것을 삭제**함!

---

## 📝 수정된 파일

| 파일 | 변경 내용 |
|-----|----------|
| `RecordsDataLoader.kt` | `clearAllRecords()`에서 `start_time`과 `timer_completed` 초기화 제거 |

**수정 줄 수**:
- 삭제: 2줄 (`.putLong("start_time", 0L)`, `.putBoolean("timer_completed", false)`)
- 추가: 1줄 (주석 설명)
- 수정: 2줄 (로그 메시지)

---

## ✅ 결과

### Before (버그)
```
❌ "모든 기록 삭제" → 진행 중인 타이머까지 종료됨
```

### After (수정)
```
✅ "모든 기록 삭제" → 완료된 기록만 삭제, 타이머는 유지됨
```

**개선 효과**:
- ✅ 사용자가 의도한 대로 동작 (완료된 기록만 삭제)
- ✅ 진행 중인 타이머 보호
- ✅ 예상치 못한 타이머 종료 방지

---

## 🔧 추가 개선 권장 사항

### 1. UI에 경고 문구 추가 (선택사항)

**현재**:
```
"모든 기록을 삭제하시겠습니까?"
```

**권장**:
```
"완료된 모든 기록을 삭제하시겠습니까?
(진행 중인 타이머는 유지됩니다)"
```

### 2. 버튼 텍스트 명확화 (선택사항)

**현재**:
```
"모든 기록 삭제"
```

**권장**:
```
"완료된 기록 전체 삭제"
또는
"과거 기록 전체 삭제"
```

---

**작성일**: 2026-01-02  
**상태**: ✅ 수정 완료  
**테스트**: 빌드 진행 중

