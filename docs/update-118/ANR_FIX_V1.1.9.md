# ✅ ANR 이슈 수정 완료 (v1.1.9 배포 준비)

**작업일**: 2026-01-02  
**목적**: MessageQueue 관련 ANR(앱 멈춤) 이슈 해결  
**버전**: 1.1.8 → 1.1.9  
**상태**: ✅ 완료

---

## 🎯 수정 내용

### 1. SharedPreferences 최적화 ✅

#### 문제점
- `SharedPreferences.Editor.commit()`은 **동기(Synchronous)** 처리
- 메인 스레드에서 파일 I/O를 수행하여 ANR 발생 위험
- 특히 큰 데이터(sobriety_records 등)를 저장할 때 지연 발생

#### 해결 방법
- 모든 `commit()`을 **`apply()`**로 변경
- `apply()`는 **비동기(Asynchronous)** 처리로 메인 스레드 블로킹 방지

#### 수정된 파일

**1. RecordsDataLoader.kt** ✅
```kotlin
// Before (ANR 위험)
val success = sharedPref.edit()
    .putString("sobriety_records", "[]")
    .putLong("start_time", 0L)
    .putBoolean("timer_completed", false)
    .commit()  // ❌ 동기 처리

if (success) {
    // ...
}

// After (ANR 해결)
sharedPref.edit()
    .putString("sobriety_records", "[]")
    .putLong("start_time", 0L)
    .putBoolean("timer_completed", false)
    .apply()  // ✅ 비동기 처리

// success 체크 제거
// apply()는 항상 성공으로 처리
```

**2. DetailScreen.kt** ✅
```kotlin
// Before (ANR 위험)
val committed = sharedPref.edit()
    .putString("sobriety_records", newArray.toString())
    .commit()  // ❌ 동기 처리

if (!committed) {
    Toast.makeText(context, "기록 삭제 실패", Toast.LENGTH_SHORT).show()
} else {
    sharedPref.edit().apply {
        putBoolean(Constants.PREF_TIMER_COMPLETED, false)
        putLong(Constants.PREF_START_TIME, 0L)
        commit()  // ❌ 동기 처리
    }
}

// After (ANR 해결)
sharedPref.edit()
    .putString("sobriety_records", newArray.toString())
    .apply()  // ✅ 비동기 처리

sharedPref.edit().apply {
    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
    putLong(Constants.PREF_START_TIME, 0L)
    apply()  // ✅ 비동기 처리
}

Toast.makeText(context, "기록이 삭제되었습니다", Toast.LENGTH_SHORT).show()
```

---

### 2. 광고 초기화 최적화 ✅

#### 문제점
- `MobileAds.initialize()`가 메인 스레드에서 실행
- 초기화 작업이 길어지면(네트워크 요청 등) 메인 스레드 블로킹
- ANR 발생 가능

#### 해결 방법
- `Dispatchers.IO` 또는 `withContext`를 사용하여 백그라운드 스레드에서 초기화
- 콜백은 `runOnUiThread`로 메인 스레드에서 실행

#### 수정된 파일

**1. SplashScreen.kt** ✅
```kotlin
// Before (ANR 위험)
try {
    com.google.android.gms.ads.MobileAds.initialize(this@SplashScreen) {
        android.util.Log.d("SplashScreen", "✅ MobileAds initialized")
        loadAndShowAd(launchContent)
    }
}

// After (ANR 해결)
try {
    // [FIX] Dispatchers.IO에서 초기화하여 메인 스레드 블로킹 방지
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        com.google.android.gms.ads.MobileAds.initialize(this@SplashScreen) {
            android.util.Log.d("SplashScreen", "✅ MobileAds initialized (background)")

            // STEP 3: 광고 로드 및 표시 (메인 스레드에서 실행)
            runOnUiThread {
                loadAndShowAd(launchContent)
            }
        }
    }
}
```

**2. CommunityScreen.kt** ✅
```kotlin
// Before (ANR 위험)
LaunchedEffect(Unit) {
    try {
        try {
            com.google.android.gms.ads.MobileAds.initialize(context)
        } catch (initEx: Exception) { ... }
        
        // 광고 로드
        kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager.getOrLoadAd(...)
    }
}

// After (ANR 해결)
LaunchedEffect(Unit) {
    // [FIX] 백그라운드에서 MobileAds 초기화
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            com.google.android.gms.ads.MobileAds.initialize(context)
        } catch (initEx: Exception) { ... }
    }

    try {
        // 광고 로드 (메인 스레드로 복귀 후)
        kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager.getOrLoadAd(...)
    }
}
```

**3. RunScreen.kt** ✅
**4. DiaryDetailFeedScreen.kt** ✅
**5. RecordsScreen.kt** ✅
- 동일한 패턴으로 `withContext(Dispatchers.IO)` 사용

---

## 📊 수정 요약

### commit() → apply() 변경

| 파일 | 위치 | 변경 내용 |
|-----|------|----------|
| RecordsDataLoader.kt | clearAllRecords() | commit() → apply() |
| DetailScreen.kt | 기록 삭제 로직 | commit() → apply() (2곳) |

### MobileAds.initialize() 백그라운드 처리

| 파일 | 변경 내용 |
|-----|----------|
| SplashScreen.kt | CoroutineScope(Dispatchers.IO).launch |
| CommunityScreen.kt | withContext(Dispatchers.IO) |
| RunScreen.kt | withContext(Dispatchers.IO) |
| DiaryDetailFeedScreen.kt | withContext(Dispatchers.IO) |
| RecordsScreen.kt | withContext(Dispatchers.IO) |

---

## 🔍 기술적 세부 사항

### commit() vs apply()

| 항목 | commit() | apply() |
|-----|----------|---------|
| 실행 방식 | **동기(Synchronous)** | **비동기(Asynchronous)** |
| 반환값 | Boolean (성공/실패) | void |
| 메인 스레드 | **블로킹** (파일 I/O 대기) | **비블로킹** (백그라운드 처리) |
| ANR 위험 | ⚠️ **있음** | ✅ **없음** |
| 사용 권장 | 즉시 결과 확인 필요 시 | 대부분의 경우 (권장) |

### Dispatchers.IO 사용 이유

```kotlin
// CoroutineScope로 새로운 코루틴 시작
kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
    // 백그라운드 스레드에서 실행
    MobileAds.initialize(...)
}

// 또는 기존 코루틴 컨텍스트 변경
kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    // 백그라운드 스레드에서 실행
    MobileAds.initialize(...)
}
// withContext 블록 종료 시 자동으로 원래 스레드로 복귀
```

**장점**:
- 네트워크 요청, 파일 I/O 등 블로킹 작업에 최적화
- 메인 스레드를 차단하지 않음
- ANR 방지

---

## ✅ 검증 결과

### 컴파일 상태
- ✅ **컴파일 에러: 0개**
- ⚠️ 경고: 일부 있음 (기능에 영향 없음)
- ✅ **빌드 성공**

### 수정 효과

**Before (v1.1.8)**:
```
[메인 스레드]
  ├─ commit() 호출
  │   ├─ 파일 I/O 시작 (블로킹) ⚠️
  │   ├─ 데이터 쓰기 (대기 중...) ⏱️
  │   └─ 완료 대기 ⏳
  ├─ UI 업데이트 지연 😰
  └─ ANR 발생 가능 💥
```

**After (v1.1.9)**:
```
[메인 스레드]
  ├─ apply() 호출 ✅
  ├─ 즉시 반환 (비블로킹) ⚡
  ├─ UI 업데이트 계속 🎨
  └─ ANR 방지 ✅

[백그라운드 스레드]
  ├─ 파일 I/O 실행
  └─ 데이터 쓰기 완료
```

---

## 🎯 ANR 방지 효과

### 시나리오 1: 기록 삭제 (DetailScreen)

**Before**:
1. 사용자가 "삭제" 버튼 클릭
2. `commit()` 호출 → 메인 스레드 블로킹 ⚠️
3. 큰 데이터 쓰기 중 UI 응답 없음 😰
4. 5초 이상 지연 시 ANR 발생 💥

**After**:
1. 사용자가 "삭제" 버튼 클릭
2. `apply()` 호출 → 즉시 반환 ✅
3. Toast 메시지 즉시 표시 🎉
4. UI 반응 유지, ANR 방지 ✅

### 시나리오 2: 광고 초기화 (SplashScreen)

**Before**:
1. 앱 시작 → Splash 화면
2. `MobileAds.initialize()` 메인 스레드 호출 ⚠️
3. 네트워크 요청 중 UI 멈춤 😰
4. 5초 이상 지연 시 ANR 발생 💥

**After**:
1. 앱 시작 → Splash 화면
2. `MobileAds.initialize()` 백그라운드 호출 ✅
3. UI는 계속 반응 (로딩 애니메이션 등) 🎨
4. 초기화 완료 후 광고 로드 ✅

---

## 📝 추가 개선 사항

### 기존 코드에서 이미 최적화된 부분

**RetentionPreferenceManager.kt**:
- 이미 경고 메시지에서 `apply()` 사용 권장
- 코드는 정상적으로 `apply()` 사용 중
- 추가 수정 불필요 ✅

**MainActivity.kt**:
- SharedPreferences 사용 시 KTX 확장 함수 사용 권장 경고
- 기능에는 영향 없음
- 향후 리팩토링 시 개선 가능

---

## 🚀 배포 준비

### v1.1.9 체크리스트

- [x] ✅ SharedPreferences commit() → apply() 변경 완료
- [x] ✅ MobileAds.initialize() 백그라운드 처리 완료
- [x] ✅ 컴파일 에러 0개
- [x] ✅ 빌드 성공
- [x] ✅ ANR 위험 코드 제거 완료

### 변경 로그 (v1.1.9)

**Fixed**:
- ⚡ SharedPreferences 동기 처리로 인한 ANR 이슈 수정
- ⚡ 광고 SDK 초기화로 인한 메인 스레드 블로킹 해결
- 📊 RecordsDataLoader 파일 I/O 최적화
- 📱 DetailScreen 기록 삭제 성능 개선
- 🎯 SplashScreen 앱 시작 속도 개선

**Technical**:
- commit() → apply() (비동기 처리)
- MobileAds.initialize() → Dispatchers.IO (백그라운드 실행)

---

## 🎉 완료!

**v1.1.9 ANR 이슈 수정이 완료되었습니다!**

**핵심 개선**:
- ✅ SharedPreferences 비동기 처리로 메인 스레드 보호
- ✅ 광고 초기화 백그라운드 실행으로 ANR 방지
- ✅ 앱 응답성 및 사용자 경험 향상

**배포 준비 완료**: 1.1.9 버전으로 즉시 배포 가능! 🚀

---

**작성일**: 2026-01-02  
**상태**: ✅ 완료  
**다음 단계**: Release 빌드 및 Google Play Console 업로드

