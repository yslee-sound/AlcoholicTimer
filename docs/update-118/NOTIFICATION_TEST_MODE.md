# ✅ 리텐션 알림 테스트 모드 구현 완료

**작업일**: 2025-12-31  
**목적**: 알림 발송 테스트를 위한 시간 단축 (1분 안에 3개 알림 확인)  
**상태**: ✅ 완료

---

## 🎯 구현 내용

### 테스트 모드 플래그 추가

**파일**: `RetentionNotificationManager.kt`

**코드**:
```kotlin
// ============================================================
// [TEST MODE] 알림 발송 테스트용 시간 단축 (2025-12-31)
// ============================================================
private const val TEST_MODE = true  // ⚠️ 배포 시 false로 변경 필수!

// 실제 지연 시간 (시간 단위)
private const val DELAY_24H = 24L   // 1일
private const val DELAY_72H = 72L   // 3일
private const val DELAY_168H = 168L // 7일
private const val DELAY_720H = 720L // 30일

// 테스트 지연 시간 (초 단위)
private const val TEST_DELAY_24H = 10L   // 10초
private const val TEST_DELAY_72H = 20L   // 20초
private const val TEST_DELAY_168H = 30L  // 30초
```

---

## ⏱️ 시간 변경 매핑

### 그룹 A (신규 유저)
| 원래 시간 | 테스트 시간 | 변경 비율 |
|----------|-----------|----------|
| 24시간 (1일) | 10초 | 1:8,640 |
| 72시간 (3일) | 20초 | 1:12,960 |
| 168시간 (7일) | 30초 | 1:20,160 |

**예상 결과**:
```
앱 실행
  ↓
10초 후 → 1차 알림 "🍺 ZERO 앱, 잊으신 건 아니죠?"
  ↓
20초 후 → 2차 알림 "작심삼일도 시작을 해야..."
  ↓
30초 후 → 3차 알림 "벌써 일주일이 지났어요. 술값 아껴서..."
```

### 그룹 C (휴식 유저)
| 원래 시간 | 테스트 시간 | 변경 비율 |
|----------|-----------|----------|
| 24시간 (D+1) | 10초 | 1:8,640 |
| 72시간 (D+3) | 20초 | 1:12,960 |

---

## 🔧 구현된 함수

### scheduleNotificationWithTestMode()

**목적**: TEST_MODE 플래그에 따라 초 단위 또는 시간 단위로 예약

**로직**:
```kotlin
val workRequest = if (TEST_MODE) {
    // 테스트 모드: 초 단위로 예약
    OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInitialDelay(testDelaySeconds, TimeUnit.SECONDS)
        .setInputData(inputData)
        .addTag(tag)
        .build()
} else {
    // 실제 모드: 시간 단위로 예약
    OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInitialDelay(delayHours, TimeUnit.HOURS)
        .setInputData(inputData)
        .addTag(tag)
        .build()
}
```

**로그 출력**:
```kotlin
val delayInfo = if (TEST_MODE) {
    "Delay: ${testDelaySeconds}s (TEST MODE)"
} else {
    "Delay: ${delayHours}h"
}

Log.d("RetentionNotification", "📅 Notification scheduled - Group: $group, $delayInfo, ID: $notificationId")
```

---

## 📊 예상 로그 (TEST_MODE = true)

### 그룹 A 예약 시
```
D/RetentionNotification: 🗑️ Group A notifications cancelled
D/RetentionNotification: 📅 Notification scheduled - Group: group_new_user, Delay: 10s (TEST MODE), ID: 1001
D/RetentionNotification: 📅 Notification scheduled - Group: group_new_user, Delay: 20s (TEST MODE), ID: 1002
D/RetentionNotification: 📅 Notification scheduled - Group: group_new_user, Delay: 30s (TEST MODE), ID: 1003
D/AnalyticsManager: ✅ User Property Set: retention_group = group_a_new_user
D/RetentionNotification: ✅ Group A notifications scheduled - TEST MODE (10s, 20s, 30s)
```

### 알림 발송 시 (10초 후)
```
D/NotificationWorker: 🔔 Worker started - Group: group_new_user, ID: 1001
D/NotificationWorker: ✅ Notification sent - ID: 1001, Title: 🍺 ZERO 앱, 잊으신 건 아니죠?, Target: start
```

### 알림 발송 시 (20초 후)
```
D/NotificationWorker: 🔔 Worker started - Group: group_new_user, ID: 1002
D/NotificationWorker: ✅ Notification sent - ID: 1002, Title: 작심삼일도 시작을..., Target: start
```

### 알림 발송 시 (30초 후)
```
D/NotificationWorker: 🔔 Worker started - Group: group_new_user, ID: 1003
D/NotificationWorker: ✅ Notification sent - ID: 1003, Title: 벌써 일주일이..., Target: start
```

---

## 🧪 테스트 절차

### 1. 앱 재설치 (초기화)
```powershell
# 기존 앱 삭제
adb -s emulator-5554 uninstall kr.sweetapps.alcoholictimer

# 새로 설치
adb -s emulator-5554 install app\build\outputs\apk\debug\app-debug.apk
```

### 2. 앱 실행 및 권한 허용
```
1. 앱 실행
2. UMP Consent 처리
3. Pre-Permission 다이얼로그 "확인" 클릭
4. 시스템 권한 팝업 "허용" 클릭
```

### 3. 로그캣 모니터링
```powershell
# 알림 예약 확인
adb -s emulator-5554 logcat | findstr "RetentionNotification"

# 알림 발송 확인
adb -s emulator-5554 logcat | findstr "NotificationWorker"

# 전체 흐름 확인
adb -s emulator-5554 logcat | findstr "TEST MODE"
```

### 4. 타이머 확인 (선택)
```
# 예약된 WorkRequest 확인
adb -s emulator-5554 shell dumpsys jobscheduler | findstr "notification"
```

---

## ⏰ 타임라인 (테스트 모드)

```
00:00 - 앱 실행 및 초기화 완료
  └─> 그룹 A 알림 예약 (10s, 20s, 30s)
  
00:10 - 1차 알림 도착 🔔
  └─> "🍺 ZERO 앱, 잊으신 건 아니죠?"
  
00:20 - 2차 알림 도착 🔔
  └─> "작심삼일도 시작을 해야..."
  
00:30 - 3차 알림 도착 🔔
  └─> "벌써 일주일이 지났어요. 술값 아껴서..."
  
00:40 - 테스트 완료 ✅
```

**총 소요 시간**: 약 40초 (여유 포함 1분)

---

## 🚨 배포 시 주의사항

### ⚠️ 필수: TEST_MODE 비활성화

**배포 전 체크리스트**:
```kotlin
// ❌ 테스트 모드 (절대 배포 금지!)
private const val TEST_MODE = true

// ✅ 실제 모드 (배포 시 필수)
private const val TEST_MODE = false
```

**이유**:
- 실제 사용자에게 10초, 20초, 30초 후 알림이 발송되면 스팸으로 인식됨
- 앱 삭제율 증가 위험
- AdMob 정책 위반 가능성

### 배포 전 검증 스크립트 (제안)
```powershell
# TEST_MODE 체크
findstr "TEST_MODE = true" RetentionNotificationManager.kt

# 결과가 나오면 배포 금지!
# 결과 없음: 안전 ✅
```

---

## 📝 테스트 시나리오

### 시나리오 1: 신규 유저 (그룹 A)
```
1. 앱 재설치
2. 권한 허용 완료
3. 앱 종료하지 말고 대기
4. 10초 후 알림 확인
5. 20초 후 알림 확인
6. 30초 후 알림 확인
```

### 시나리오 2: 타이머 포기 유저 (그룹 C)
```
1. 앱 실행 → 타이머 시작
2. 즉시 타이머 포기
3. 10초 후 알림 확인 (D+1)
4. 20초 후 알림 확인 (D+3)
```

### 시나리오 3: 알림 클릭 → 딥링크
```
1. 알림 도착 대기
2. 알림 클릭
3. START 화면으로 이동 확인
4. notification_open 이벤트 전송 확인
```

---

## 🔍 문제 해결

### 알림이 오지 않는 경우

**체크 1: 알림 권한 확인**
```powershell
adb -s emulator-5554 shell dumpsys notification
```

**체크 2: WorkManager 상태 확인**
```powershell
adb -s emulator-5554 shell dumpsys jobscheduler
```

**체크 3: 알림 채널 확인**
```
설정 → 앱 → ZERO → 알림 → "리텐션 알림" 활성화
```

### 로그가 안 나오는 경우

**로그캣 초기화**:
```powershell
adb -s emulator-5554 logcat -c
adb -s emulator-5554 logcat | findstr "Retention"
```

---

## ✅ 최종 체크리스트

### 구현 완료
- [x] TEST_MODE 플래그 추가
- [x] 테스트 지연 시간 상수 정의 (10s, 20s, 30s)
- [x] scheduleNotificationWithTestMode() 함수 구현
- [x] 그룹 A 함수에 적용
- [x] 그룹 C 함수에 적용
- [x] 로그 메시지 "TEST MODE" 표시

### 테스트 준비
- [x] 컴파일 에러 0개
- [x] 테스트 절차 문서화
- [x] 예상 로그 정리
- [x] 배포 전 주의사항 작성

### 배포 전 확인 (필수!)
- [ ] TEST_MODE = false 설정
- [ ] 빌드 후 재확인
- [ ] 실제 알림 시간 테스트 (24시간 후)

---

## 🎯 예상 효과

### 개발 효율
- **Before**: 24시간 대기 → 3일 대기 → 7일 대기 (총 7일 필요)
- **After**: 10초 → 20초 → 30초 (총 1분 안에 완료) ✅
- **개선율**: 약 10,080배 빠름

### 테스트 커버리지
- ✅ 알림 예약 로직 검증
- ✅ 알림 발송 로직 검증
- ✅ 알림 메시지 내용 검증
- ✅ 딥링크 네비게이션 검증
- ✅ Analytics 이벤트 검증

---

**작성일**: 2025-12-31  
**상태**: ✅ 완료  
**다음 단계**: 실제 기기에서 테스트 실행  
**배포 전**: ⚠️ TEST_MODE = false 필수 확인!

