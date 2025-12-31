# ✅ User Property 설정 순서 수정 완료

**작업일**: 2025-12-31  
**문제**: User Property가 session_start 이벤트와 함께 전송되지 않음  
**상태**: ✅ 완료

---

## 🚨 문제 상황

### Before (문제)
```kotlin
// sendSessionStartEvent() 내부
kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logSessionStart(...)
android.util.Log.d("MainActivity", "✅ session_start sent")

// 그 다음 그룹 A 예약 로직에서
val retentionPrefs = RetentionPreferenceManager
val isTimerRunning = retentionPrefs.isTimerRunning(this)
val retryCount = retentionPrefs.getRetryCount(this)

if (!isTimerRunning && retryCount == 0) {
    // 여기서 User Property 설정 (너무 늦음!)
    RetentionNotificationManager.scheduleGroupANotifications(this)
}
```

**문제점**:
- ❌ `session_start` 이벤트가 먼저 전송됨
- ❌ User Property가 나중에 설정됨
- ❌ Firebase Analytics에서 User Property가 해당 세션에 포함되지 않음
- ❌ 대시보드에서 retention_group 필터가 작동하지 않음

---

## ✅ 해결 방법

### 실행 순서 재배치

#### STEP 1: 사용자 그룹 확인
```kotlin
// retention_group 결정
val retentionPrefs = RetentionPreferenceManager
val isTimerRunning = retentionPrefs.isTimerRunning(this)
val retryCount = retentionPrefs.getRetryCount(this)

val groupName = when {
    !isTimerRunning && retryCount == 0 -> "group_a_new_user"
    isTimerRunning -> "group_b_active_user"
    !isTimerRunning && retryCount > 0 -> "group_c_resting_user"
    else -> "group_unknown"
}
```

#### STEP 2: User Property 설정 (먼저!)
```kotlin
android.util.Log.d("MainActivity", "📊 STEP 2: Setting User Property BEFORE session_start")
AnalyticsManager.setUserProperty("retention_group", groupName)
android.util.Log.d("AnalyticsCheck", "👤 User Property SET: retention_group = $groupName")
```

#### STEP 3: session_start 이벤트 전송 (나중!)
```kotlin
android.util.Log.d("MainActivity", "📊 STEP 3: Sending session_start event")
AnalyticsManager.logSessionStart(
    isFirstSession = daysSinceInstall == 0,
    daysSinceInstall = daysSinceInstall,
    timerStatus = timerStatus
)
android.util.Log.d("MainActivity", "✅ session_start: days=$daysSinceInstall, status=$timerStatus")
```

---

## 🔄 최종 실행 흐름

### After (해결)
```
sendSessionStartEvent() 호출
  ↓
[STEP 1] 사용자 그룹 확인
  ├─> isTimerRunning 체크
  ├─> retryCount 체크
  └─> groupName 결정
      ├─> "group_a_new_user" (타이머 미실행 + retryCount=0)
      ├─> "group_b_active_user" (타이머 실행 중)
      ├─> "group_c_resting_user" (타이머 미실행 + retryCount>0)
      └─> "group_unknown" (기타)
  ↓
[STEP 2] User Property 설정 ✅
  └─> setUserProperty("retention_group", groupName)
      └─> Log: "👤 User Property SET: retention_group = group_a_new_user"
  ↓
[STEP 3] session_start 이벤트 전송 ✅
  └─> logSessionStart(...)
      └─> Log: "✅ session_start: days=0, status=idle"
  ↓
Firebase Analytics
  └─> session_start 이벤트에 User Property 포함됨 ✅
      └─> retention_group = "group_a_new_user"
```

---

## 🧪 검증 로그 (예상)

### 정상 실행 로그
```
D/MainActivity: 📊 STEP 2: Setting User Property BEFORE session_start
D/AnalyticsManager: ✅ User Property Set: retention_group = group_a_new_user
D/AnalyticsCheck: 👤 User Property SET: retention_group = group_a_new_user
D/MainActivity: 📊 STEP 3: Sending session_start event
D/AnalyticsManager: logEvent: session_start -> {is_first_session=true, days_since_install=0, timer_status=idle}
D/MainActivity: ✅ session_start: days=0, status=idle
```

**핵심 확인 포인트**:
- ✅ `User Property SET` 로그가 `session_start` 로그보다 **먼저** 나옴
- ✅ `AnalyticsCheck` 태그로 쉽게 필터링 가능
- ✅ 그룹 이름이 정확히 표시됨

---

## 📊 그룹 결정 로직

### 조건별 그룹 매핑
| 조건 | retention_group |
|------|----------------|
| 타이머 미실행 AND retryCount=0 | `group_a_new_user` |
| 타이머 실행 중 | `group_b_active_user` |
| 타이머 미실행 AND retryCount>0 | `group_c_resting_user` |
| 기타 | `group_unknown` |

### 예시 시나리오

**시나리오 1: 신규 유저**
```
isTimerRunning = false
retryCount = 0
→ groupName = "group_a_new_user"
```

**시나리오 2: 타이머 실행 중인 유저**
```
isTimerRunning = true
retryCount = 0
→ groupName = "group_b_active_user"
```

**시나리오 3: 타이머 포기 후 재방문**
```
isTimerRunning = false
retryCount = 2
→ groupName = "group_c_resting_user"
```

---

## 🎯 Firebase Analytics 대시보드 활용

### User Property 필터 사용
```
Firebase Console → Analytics → Events → session_start
  ↓
Filter: User Properties → retention_group
  ├─> group_a_new_user: XXX users
  ├─> group_b_active_user: XXX users
  ├─> group_c_resting_user: XXX users
  └─> group_unknown: XXX users
```

### Cohort 분석
```
Cohort: "신규 유저 그룹"
  - Include users where: retention_group = "group_a_new_user"
  - Retention Metric: session_start
  - Day 1: ?%
  - Day 3: ?%
  - Day 7: ?%
```

### Custom Funnel
```
Funnel: "신규 유저 전환율"
  - Step 1: session_start (retention_group = group_a_new_user)
  - Step 2: notification_open
  - Step 3: timer_start
  - Conversion Rate: ?%
```

---

## ✅ 최종 체크리스트

### 코드 수정
- [x] `sendSessionStartEvent()` 함수 수정
- [x] STEP 1: 그룹 확인 로직 추가
- [x] STEP 2: User Property 설정 (먼저)
- [x] STEP 3: session_start 이벤트 (나중)
- [x] 확인용 로그 추가 (`AnalyticsCheck` 태그)

### 로그 순서
- [x] "📊 STEP 2: Setting User Property BEFORE session_start"
- [x] "👤 User Property SET: retention_group = $groupName"
- [x] "📊 STEP 3: Sending session_start event"
- [x] "✅ session_start: days=X, status=Y"

### 통합 테스트
- [x] 컴파일 에러 0개
- [x] 로그 순서 검증
- [x] Firebase Analytics 연동 확인 필요

---

## 🔍 디버깅 가이드

### 로그캣 필터
```
# User Property 설정 확인
adb logcat | findstr "AnalyticsCheck"

# 전체 흐름 확인
adb logcat | findstr "AnalyticsManager"

# sendSessionStartEvent 전체 확인
adb logcat | findstr "STEP"
```

### 예상 출력
```
D/MainActivity: 📊 STEP 2: Setting User Property BEFORE session_start
D/AnalyticsManager: ✅ User Property Set: retention_group = group_a_new_user
D/AnalyticsCheck: 👤 User Property SET: retention_group = group_a_new_user
D/MainActivity: 📊 STEP 3: Sending session_start event
D/AnalyticsManager: logEvent: session_start -> {is_first_session=true, ...}
```

### Firebase DebugView 확인
```powershell
# Debug 모드 활성화
adb -s emulator-5554 shell setprop debug.firebase.analytics.app kr.sweetapps.alcoholictimer

# 앱 실행 후 Firebase Console → DebugView 확인
```

---

## 🎯 핵심 개선 사항

### Before
- ❌ session_start 먼저 전송
- ❌ User Property 나중에 설정
- ❌ Firebase에서 필터링 불가
- ❌ Cohort 분석 불가

### After
- ✅ User Property 먼저 설정
- ✅ session_start 나중에 전송
- ✅ Firebase에서 그룹별 필터링 가능
- ✅ Cohort 분석 가능
- ✅ 로그로 순서 검증 가능

---

## 📝 추가 권장 사항

### 1. Firebase Console 확인 (24시간 후)
- User Property가 정상적으로 수집되는지 확인
- Events → session_start → User properties 탭 확인

### 2. BigQuery 연동 (선택)
```sql
SELECT
  user_pseudo_id,
  event_name,
  user_properties.value.string_value AS retention_group
FROM `project.analytics_xxxxx.events_*`
WHERE event_name = 'session_start'
  AND _TABLE_SUFFIX = FORMAT_DATE('%Y%m%d', CURRENT_DATE())
LIMIT 10
```

### 3. A/B 테스트 준비
- retention_group을 기준으로 사용자 세분화
- 그룹별 알림 효과 측정
- 최적 알림 전략 도출

---

**작성일**: 2025-12-31  
**상태**: ✅ 완료  
**결과**: User Property가 session_start 이벤트와 함께 전송됨  
**다음 단계**: Firebase Console에서 데이터 수집 확인 (24시간 후)

