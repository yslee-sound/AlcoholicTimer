# ✅ 알림 권한 요청 로직 이동 완료

**작업일**: 2025-12-31  
**작업**: Pre-Permission 다이얼로그 실행 위치 변경  
**상태**: ✅ 완료

---

## 📊 변경 사항 요약

### Before (이전)
```
AppContentWithStart Composable (앱 시작 후 2초)
  └─> LaunchedEffect(Unit)
      └─> delay(2000)
      └─> Pre-Permission 다이얼로그 표시
```

### After (변경 후)
```
MainActivity.onCreate() (앱 시작 즉시)
  └─> checkAndRequestNotificationPermission()
      └─> Pre-Permission 다이얼로그 즉시 표시
      └─> 확인 클릭 → ActivityResultLauncher 호출
          └─> 시스템 권한 팝업 표시
          └─> Analytics 이벤트 전송 ✅
          └─> RetentionPreferenceManager 저장 ✅
```

---

## 🔄 전체 동작 흐름

```
[앱 시작]
  ↓
MainActivity.onCreate()
  ├─> Splash Screen 설정
  ├─> Firebase Remote Config
  ├─> Analytics 초기화
  │
  ├─> checkAndRequestNotificationPermission() 🆕
  │   ├─> Android 13+ 확인 ✅
  │   ├─> 권한 미허용 확인 ✅
  │   ├─> 다이얼로그 미표시 확인 ✅
  │   │
  │   └─> YES → Pre-Permission 다이얼로그 표시 🔔
  │       │
  │       ├─> "확인" 클릭
  │       │   ├─> requestPermissionLauncher.launch() 🎯
  │       │   │   └─> 시스템 권한 팝업 표시
  │       │   │       ├─> [허용]
  │       │   │       │   ├─> RetentionPreferenceManager.setNotificationPermissionShown(true)
  │       │   │       │   ├─> Analytics: settings_change (denied → granted) 📊
  │       │   │       │   └─> 로그: "✅ Notification permission GRANTED"
  │       │   │       │
  │       │   │       └─> [거부]
  │       │   │           ├─> Analytics: settings_change (→ denied) 📊
  │       │   │           ├─> shouldShowRequestPermissionRationale() 체크
  │       │   │           │   ├─> false: "다시 묻지 않음" 선택 ⚠️
  │       │   │           │   └─> true: 재요청 가능 ℹ️
  │       │   │           └─> 로그: "❌ Notification permission DENIED"
  │       │   │
  │       │   └─> continueAppInitialization() 🚀
  │       │       └─> 정상 앱 UI 표시
  │       │
  │       └─> "나중에" 클릭
  │           └─> continueAppInitialization() 🚀
  │               └─> 정상 앱 UI 표시
  │
  └─> UMP 동의 확인
  └─> 광고 SDK 초기화
  └─> AppOpen 광고 로드
```

---

## 📁 수정된 파일

### MainActivity.kt (3가지 변경)

#### 1️⃣ onCreate()에 권한 체크 호출 추가
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // ...existing code...
    
    // 강제 라이트 모드 설정
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    // [NEW] 알림 권한 체크 및 Pre-Permission 다이얼로그 표시 (2025-12-31)
    checkAndRequestNotificationPermission()

    // [NEW] Session Start 이벤트 전송 (2025-12-31)
    // ...existing code...
}
```

#### 2️⃣ 권한 체크 함수 추가
```kotlin
/**
 * [NEW] 알림 권한 체크 및 Pre-Permission 다이얼로그 표시 (2025-12-31)
 * MainActivity.onCreate()에서 즉시 호출
 */
private fun checkAndRequestNotificationPermission() {
    val permissionManager = NotificationPermissionManager
    val retentionPrefs = RetentionPreferenceManager

    // 권한이 필요하고, 아직 요청하지 않았다면
    if (permissionManager.shouldRequestPermission(this) &&
        !retentionPrefs.isNotificationPermissionShown(this)) {

        android.util.Log.d("MainActivity", "🔔 Notification permission needed - showing Pre-Permission dialog")

        // Compose Dialog를 표시하기 위해 setContent 사용
        setContent {
            NotificationPermissionDialog(
                onConfirm = {
                    android.util.Log.d("MainActivity", "User confirmed - requesting system permission")

                    // 시스템 권한 팝업 요청
                    permissionManager.requestPermission(requestPermissionLauncher)

                    // 다이얼로그를 닫고 정상 앱 플로우로 복귀
                    continueAppInitialization()
                },
                onDismiss = {
                    android.util.Log.d("MainActivity", "User dismissed permission dialog")

                    // 다이얼로그를 닫고 정상 앱 플로우로 복귀
                    continueAppInitialization()
                }
            )
        }
    } else {
        android.util.Log.d("MainActivity", "Notification permission already granted or shown - skipping dialog")
    }
}
```

#### 3️⃣ 앱 초기화 계속 함수 추가
```kotlin
/**
 * [NEW] 권한 다이얼로그 이후 정상 앱 초기화 플로우 계속 진행 (2025-12-31)
 */
private fun continueAppInitialization() {
    android.util.Log.d("MainActivity", "Continuing app initialization after permission dialog")

    // 타이머 상태에 따른 초기 라우트 결정
    val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)
    val startDestination = when {
        timerCompleted -> Screen.Success.route
        startTime > 0L -> Screen.Run.route
        else -> Screen.Start.route
    }

    // 정상 앱 UI 표시
    setTheme(R.style.Theme_AlcoholicTimer)
    setContent {
        val holdSplashState = remember { mutableStateOf(false) }
        AppContentWithStart(startDestination, holdSplashState)
    }
}
```

#### 4️⃣ requestPermissionLauncher에 Analytics 추가
```kotlin
internal val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted: Boolean ->
    if (isGranted) {
        // 권한 허용됨
        android.util.Log.d("MainActivity", "✅ Notification permission GRANTED")
        RetentionPreferenceManager.setNotificationPermissionShown(this, true)

        // [NEW] Firebase Analytics 이벤트 전송 (2025-12-31)
        try {
            AnalyticsManager.logSettingsChange(
                settingType = "notification_permission",
                oldValue = "denied",
                newValue = "granted"
            )
            android.util.Log.d("MainActivity", "Analytics: settings_change sent (notification_permission: denied → granted)")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to log settings_change", e)
        }
    } else {
        // 권한 거부됨
        android.util.Log.d("MainActivity", "❌ Notification permission DENIED")

        // [NEW] Firebase Analytics 이벤트 전송 (2025-12-31)
        try {
            AnalyticsManager.logSettingsChange(
                settingType = "notification_permission",
                oldValue = null,
                newValue = "denied"
            )
            android.util.Log.d("MainActivity", "Analytics: settings_change sent (notification_permission: → denied)")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to log settings_change", e)
        }

        // shouldShowRequestPermissionRationale 체크
        // ...existing code...
    }
}
```

#### 5️⃣ AppContentWithStart의 중복 로직 제거
```kotlin
@Composable
private fun AppContentWithStart(
    startDestination: String,
    holdSplashState: MutableState<Boolean> = mutableStateOf(false)
) {
    // ...existing code...

    // [REMOVED] 알림 권한 요청 로직을 MainActivity.onCreate()로 이동 (2025-12-31)
    // 이유: 앱 시작 시 즉시 권한을 확인하고 다이얼로그를 표시하기 위함

    // ...existing code...
}
```

---

## ✅ 구현 완료 체크리스트

- [x] 권한 요청 트리거를 `timer_start`가 아닌 `MainActivity.onCreate()`로 이동
- [x] 앱 실행 시 POST_NOTIFICATIONS 권한 즉시 확인
- [x] 권한 없으면 Pre-Permission 다이얼로그 즉시 표시
- [x] "확인" 버튼 클릭 시 ActivityResultLauncher로 시스템 팝업 호출
- [x] Firebase Analytics `settings_change` 이벤트 전송
- [x] RetentionPreferenceManager에 권한 상태 저장
- [x] shouldShowRequestPermissionRationale 체크
- [x] 빌드 성공 확인

---

## 🧪 테스트 가이드

### 1. 정상 케이스 (첫 실행 → 허용)

```powershell
# Logcat 모니터링
adb -s emulator-5554 logcat -s MainActivity NotificationPermission
```

**예상 동작**:
1. 앱 시작
2. 즉시 Pre-Permission 다이얼로그 표시
3. "확인" 클릭
4. 시스템 권한 팝업 표시
5. "허용" 클릭
6. 정상 앱 UI 표시

**예상 로그**:
```
D/MainActivity: 🔔 Notification permission needed - showing Pre-Permission dialog
D/MainActivity: User confirmed - requesting system permission
D/NotificationPermission: 🔔 시스템 권한 팝업 요청 (ActivityResultLauncher)
D/MainActivity: ✅ Notification permission GRANTED
D/MainActivity: Analytics: settings_change sent (notification_permission: denied → granted)
D/MainActivity: Continuing app initialization after permission dialog
```

---

### 2. 나중에 클릭 케이스

**예상 동작**:
1. 앱 시작
2. Pre-Permission 다이얼로그 표시
3. "나중에" 클릭
4. 정상 앱 UI 표시 (권한 플래그 저장 안 됨)
5. 앱 재시작 시 다이얼로그 다시 표시

**예상 로그**:
```
D/MainActivity: 🔔 Notification permission needed - showing Pre-Permission dialog
D/MainActivity: User dismissed permission dialog
D/MainActivity: Continuing app initialization after permission dialog
```

---

### 3. 거부 케이스

**예상 동작**:
1. Pre-Permission 다이얼로그 → "확인"
2. 시스템 팝업 → "거부"
3. Analytics 이벤트 전송
4. 정상 앱 UI 표시

**예상 로그**:
```
D/MainActivity: User confirmed - requesting system permission
D/NotificationPermission: 🔔 시스템 권한 팝업 요청 (ActivityResultLauncher)
D/MainActivity: ❌ Notification permission DENIED
D/MainActivity: Analytics: settings_change sent (notification_permission: → denied)
D/MainActivity: ℹ️ User can be asked again later
```

---

### 4. "다시 묻지 않음" 케이스

**예상 로그**:
```
D/MainActivity: ❌ Notification permission DENIED
D/MainActivity: Analytics: settings_change sent (notification_permission: → denied)
D/MainActivity: ⚠️ User selected 'Don't ask again' - permission permanently denied
```

---

## 📊 Firebase Analytics 데이터

### settings_change 이벤트

**허용 시**:
```json
{
  "event": "settings_change",
  "params": {
    "setting_type": "notification_permission",
    "old_value": "denied",
    "new_value": "granted"
  }
}
```

**거부 시**:
```json
{
  "event": "settings_change",
  "params": {
    "setting_type": "notification_permission",
    "old_value": null,
    "new_value": "denied"
  }
}
```

### Firebase Console 확인 방법

1. Firebase Console 접속
2. Analytics → Events
3. `settings_change` 이벤트 선택
4. Parameter: `setting_type = notification_permission` 필터
5. `new_value`별 분포 확인:
   - `granted`: 허용 비율
   - `denied`: 거부 비율

---

## 💡 왜 이 작업이 중요한가?

### Before (이전 문제점)
- ❌ 앱 시작 후 2초 대기 → 권한 요청
- ❌ 사용자가 앱을 사용하다가 갑자기 다이얼로그 표시
- ❌ 타이밍이 부적절하여 거부율 높음

### After (개선된 점)
- ✅ 앱 시작 즉시 권한 요청
- ✅ 자연스러운 타이밍 (앱 첫 화면 전)
- ✅ Pre-Permission 다이얼로그로 가치 설명
- ✅ 권한 허용률 향상 예상

### 리텐션 시스템과의 연계
```
권한 허용 (granted)
  ↓
RetentionPreferenceManager 플래그 저장
  ↓
Phase 2: 푸시 알림 발송 가능
  ├─> 타이머 리마인더
  ├─> 목표 달성 축하
  └─> 재방문 유도

권한 거부 (denied)
  ↓
Analytics 데이터 수집
  ↓
거부율 분석
  ├─> 다이얼로그 문구 개선
  └─> 요청 타이밍 최적화
```

---

## 🎯 결과

### 구현 완료
✅ Pre-Permission 다이얼로그 실행 위치: **timer_start → MainActivity.onCreate()**  
✅ 시스템 권한 팝업: ActivityResultLauncher 정상 연결  
✅ Firebase Analytics: settings_change 이벤트 전송  
✅ RetentionPreferenceManager: 권한 상태 저장  
✅ 빌드: 성공 (경고만 있음)

### 예상 효과
- 📈 권한 허용률 향상 (자연스러운 타이밍)
- 📊 정확한 권한 추적 (Analytics)
- 🔔 Phase 2 푸시 알림 준비 완료

---

**작성일**: 2025-12-31  
**상태**: ✅ 완료  
**빌드**: ✅ 성공

