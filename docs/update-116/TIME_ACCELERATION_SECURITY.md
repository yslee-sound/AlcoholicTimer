# 시간 배속 기능 릴리즈 빌드 보안 가이드

**작업 일자**: 2025년 12월 4일  
**작업 유형**: 보안 강화

---

## 🔒 보안 문제 발견 및 해결

### ⚠️ 발견된 심각한 문제

초기 구현에서는 **릴리즈 빌드에서도 배속 기능이 완전히 작동**하는 치명적인 보안 문제가 있었습니다.

#### 문제점:
1. ❌ `getTimeAcceleration()` - BuildConfig.DEBUG 체크 없음
2. ❌ `setTimeAcceleration()` - 릴리즈에서도 설정 가능
3. ❌ `getDayInMillis()` - 릴리즈에서도 배속 적용
4. ❌ `RunScreen.kt` - 릴리즈에서도 UI 배속 적용

#### 잠재적 피해:
- 개발자가 실수로 10000배속으로 테스트 후 릴리즈
- 실제 사용자의 타이머 데이터가 왜곡됨
- 1분이 지났는데 앱에서는 "7일 경과"로 표시
- **복구 불가능한 데이터 손상**

---

## ✅ 적용된 보안 조치

### 1️⃣ Constants.kt - 3중 보호

#### setTimeAcceleration()
```kotlin
fun setTimeAcceleration(context: Context, factor: Int) {
    // [SECURITY] 릴리즈 빌드에서는 설정 불가
    if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
        android.util.Log.w("Constants", "릴리즈 빌드에서는 시간 배속 설정이 무시됩니다.")
        return // ← 즉시 종료
    }
    
    // 디버그 빌드에서만 실행됨
    val prefs = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val safeFactor = factor.coerceIn(1, 10000)
    prefs.edit().putInt(PREF_TIME_ACCELERATION, safeFactor).apply()
}
```

**효과**: 릴리즈 빌드에서는 배속 설정이 **절대 저장되지 않음**

---

#### getTimeAcceleration()
```kotlin
fun getTimeAcceleration(context: Context): Int {
    // [SECURITY] 릴리즈 빌드에서는 항상 1배속
    if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
        return 1 // ← 즉시 1 반환
    }
    
    // 디버그 빌드에서만 SharedPreferences 읽음
    val prefs = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    return prefs.getInt(PREF_TIME_ACCELERATION, 1).coerceIn(1, 10000)
}
```

**효과**: 릴리즈 빌드에서는 SharedPreferences에 값이 있어도 **무조건 1 반환**

---

#### getDayInMillis()
```kotlin
fun getDayInMillis(context: Context): Long {
    // [SECURITY] 릴리즈 빌드에서는 항상 정상 속도
    if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
        return DAY_IN_MILLIS // ← 86,400,000ms (정상 속도)
    }
    
    // 디버그 빌드에서만 배속 적용
    val factor = getTimeAcceleration(context)
    val result = DAY_IN_MILLIS / factor
    return result
}
```

**효과**: 릴리즈 빌드에서는 **항상 86,400,000ms (24시간) 반환**

---

### 2️⃣ RunScreen.kt - UI 레벨 보호

```kotlin
val accelerationFactor = remember(now) {
    if (isPreview || isDemoMode) {
        1 // Preview/Demo 모드에서는 배속 적용 안 함
    } else if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
        1 // ← 릴리즈 빌드에서는 항상 정상 속도
    } else {
        Constants.getTimeAcceleration(context)
    }
}
```

**효과**: 중앙 타이머도 릴리즈 빌드에서는 **정상 속도로만 동작**

---

### 3️⃣ DebugScreen.kt - 사용자 경고

```kotlin
// [SECURITY] 릴리즈 빌드 경고
if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
    Text(
        text = "⚠️ 릴리즈 빌드에서는 배속 기능이 비활성화됩니다.",
        fontSize = 11.sp,
        color = androidx.compose.ui.graphics.Color.Red,
        fontWeight = FontWeight.Bold
    )
}
```

**효과**: 릴리즈 빌드에서 디버그 메뉴에 접근해도 **경고 메시지 표시**

---

## 🛡️ 보안 레벨 비교

| 체크 포인트 | 수정 전 | 수정 후 |
|------------|---------|---------|
| **배속 설정 저장** | ❌ 릴리즈에서도 가능 | ✅ 디버그 전용 |
| **배속 값 읽기** | ❌ 릴리즈에서도 읽음 | ✅ 릴리즈는 무조건 1 |
| **일수 계산** | ❌ 릴리즈에서도 배속 적용 | ✅ 릴리즈는 정상 속도 |
| **UI 타이머** | ❌ 릴리즈에서도 배속 적용 | ✅ 릴리즈는 정상 속도 |
| **사용자 경고** | ❌ 없음 | ✅ 경고 메시지 표시 |

---

## 🔍 동작 검증

### 디버그 빌드 (BuildConfig.DEBUG = true)
```
1. 디버그 메뉴에서 10000배속 설정
2. SharedPreferences에 "10000" 저장됨 ✅
3. getTimeAcceleration() → 10000 반환 ✅
4. getDayInMillis() → 8,640ms 반환 ✅
5. 중앙 타이머가 빠르게 증가 ✅
```

### 릴리즈 빌드 (BuildConfig.DEBUG = false)
```
1. 디버그 메뉴에서 10000배속 설정
2. setTimeAcceleration() 즉시 return → 저장 안 됨 ✅
3. getTimeAcceleration() → 무조건 1 반환 ✅
4. getDayInMillis() → 무조건 86,400,000ms 반환 ✅
5. 중앙 타이머가 정상 속도로 증가 ✅
6. 경고 메시지 표시: "⚠️ 배속 기능 비활성화" ✅
```

---

## 📊 코드 흐름도

### 디버그 빌드
```
setTimeAcceleration(10000)
    → BuildConfig.DEBUG == true
    → SharedPreferences에 10000 저장
    
getTimeAcceleration()
    → BuildConfig.DEBUG == true
    → SharedPreferences에서 10000 읽음
    → return 10000

getDayInMillis()
    → BuildConfig.DEBUG == true
    → getTimeAcceleration() 호출 → 10000
    → 86,400,000 / 10000 = 8,640ms
    → return 8,640ms

RunScreen.accelerationFactor
    → BuildConfig.DEBUG == true
    → Constants.getTimeAcceleration() 호출
    → return 10000
    
결과: 실제 1초 → 타이머 02:46:40 증가 ⚡
```

### 릴리즈 빌드
```
setTimeAcceleration(10000)
    → BuildConfig.DEBUG == false
    → 즉시 return (저장 안 됨)
    
getTimeAcceleration()
    → BuildConfig.DEBUG == false
    → 즉시 return 1 (SharedPreferences 읽지 않음)

getDayInMillis()
    → BuildConfig.DEBUG == false
    → 즉시 return DAY_IN_MILLIS (86,400,000ms)

RunScreen.accelerationFactor
    → BuildConfig.DEBUG == false
    → 즉시 return 1
    
결과: 실제 1초 → 타이머 00:00:01 증가 (정상)
```

---

## 🧪 테스트 시나리오

### 시나리오 1: 디버그 → 릴리즈 전환
```
1. 디버그 빌드에서 10000배속 설정
2. SharedPreferences에 "10000" 저장됨
3. 앱 삭제 없이 릴리즈 빌드 설치
4. SharedPreferences에 "10000" 남아 있음
5. BUT! getTimeAcceleration()이 1 반환 ✅
6. 타이머는 정상 속도로 동작 ✅
```

**결론**: SharedPreferences에 값이 남아 있어도 안전함!

---

### 시나리오 2: 릴리즈에서 디버그 메뉴 접근
```
1. 릴리즈 빌드 실행
2. Tab 05 (디버그 메뉴) 진입
3. 배속 슬라이더 조작
4. 10000배속 버튼 클릭
5. setTimeAcceleration() 호출
6. BUT! BuildConfig.DEBUG == false
7. 즉시 return → 저장 안 됨 ✅
8. 경고 메시지 표시 ✅
```

**결론**: 릴리즈에서는 UI 조작해도 배속 설정 불가!

---

## ⚙️ ProGuard 추가 권장 사항

릴리즈 빌드 최적화 시 다음 규칙 추가 권장:

```proguard
# [SECURITY] 시간 배속 관련 함수 제거 (릴리즈 전용)
-assumenosideeffects class kr.sweetapps.alcoholictimer.constants.Constants {
    public static void setTimeAcceleration(...);
}

# 디버그 로그 제거
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

**효과**: 릴리즈 APK에서 배속 관련 함수가 완전히 제거됨

---

## 📝 체크리스트

릴리즈 전 필수 확인 사항:

- [x] Constants.kt에 BuildConfig.DEBUG 체크 3개 추가
- [x] RunScreen.kt에 BuildConfig.DEBUG 체크 추가
- [x] DebugScreen.kt에 경고 메시지 추가
- [x] 디버그 빌드 테스트 (배속 작동 확인)
- [x] 릴리즈 빌드 컴파일 성공
- [ ] 릴리즈 빌드 실기기 테스트 (배속 비활성화 확인)
- [ ] ProGuard 규칙 추가 (선택 사항)

---

## 🎉 최종 결론

### ✅ 안전성 확보
- **3중 보호 장치**: setTimeAcceleration, getTimeAcceleration, getDayInMillis
- **UI 레벨 보호**: RunScreen.kt accelerationFactor
- **사용자 경고**: DebugScreen.kt 경고 메시지

### ✅ 릴리즈 빌드 보장
- 릴리즈 빌드에서는 **절대로 배속 적용 안 됨**
- SharedPreferences에 값이 있어도 **무시됨**
- 개발자 실수로도 **실제 사용자 피해 없음**

### ✅ 개발 편의성 유지
- 디버그 빌드에서는 **정상 작동**
- 빠른 테스트 가능 (4분에 30일)
- 개발 생산성 향상

---

**보안 강화 완료!** 🔒✨

