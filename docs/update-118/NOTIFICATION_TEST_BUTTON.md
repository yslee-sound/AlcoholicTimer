# ✅ 알림 즉시 테스트 버튼 구현 완료

**작업일**: 2025-12-31  
**목적**: 알림 UI 및 채널 설정 검증용 테스트 버튼 추가  
**상태**: ✅ 완료

---

## 🎯 구현 내용

### 1. RetentionNotificationManager에 즉시 알림 함수 추가

**파일**: `RetentionNotificationManager.kt`

**함수**: `showImmediateTestNotification()`

**기능**:
```kotlin
fun showImmediateTestNotification(context: Context, title: String, message: String) {
    // 1. 채널 생성 확인
    NotificationChannelManager.createNotificationChannels(context)
    
    // 2. 알림 표시 (WorkManager 없이 즉시)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID_RETENTION)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
    
    notificationManager.notify(9999, notification)
}
```

**특징**:
- ✅ WorkManager 예약 없이 즉시 실행
- ✅ 채널 생성 보장 (`createNotificationChannels()` 호출)
- ✅ 테스트용 고유 ID (9999) 사용
- ✅ BigTextStyle로 긴 메시지 표시

---

### 2. MainActivity에 테스트 버튼 추가

**위치**: `MainActivityContent` - 화면 하단 중앙

**디자인**:
```
┌─────────────────────────────┐
│                             │
│    AppNavHost (메인 UI)      │
│                             │
│                             │
│                             │
│  ┌───────────────────────┐  │
│  │  🔔 알림 즉시 테스트  │  │ ← 주황색 버튼
│  └───────────────────────┘  │
└─────────────────────────────┘
```

**코드**:
```kotlin
// [NEW] 알림 테스트 버튼 - 초기화 완료 후에만 표시
if (isInitComplete) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = {
                Log.d("MainActivity", "🧪 TEST: Notification test button clicked")
                
                RetentionNotificationManager.showImmediateTestNotification(
                    context = activity,
                    title = "🔔 테스트 알림입니다",
                    message = "아이콘과 배너가 잘 보이나요?"
                )
                
                Log.d("MainActivity", "✅ TEST: Immediate notification triggered")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5722) // 주황색
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text(
                text = "🔔 알림 즉시 테스트",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
```

**특징**:
- ✅ 초기화 완료 후에만 표시 (`isInitComplete` 체크)
- ✅ 화면 하단 중앙 배치
- ✅ 주황색(#FF5722) 배경으로 눈에 띄게
- ✅ 화면 너비의 80% 크기
- ✅ 높이 56dp (표준 버튼 크기)

---

## 🧪 테스트 절차

### 1. 앱 실행
```
1. 앱 실행
2. UMP Consent 처리
3. Pre-Permission 다이얼로그 "확인" 클릭
4. 시스템 권한 팝업 "허용" 클릭
5. 초기화 완료 대기
```

### 2. 테스트 버튼 확인
```
화면 하단에 주황색 버튼 "🔔 알림 즉시 테스트" 확인
```

### 3. 버튼 클릭
```
1. 버튼 클릭
2. 즉시 알림 표시 확인
3. 알림 배너 내용 확인:
   - 제목: "🔔 테스트 알림입니다"
   - 메시지: "아이콘과 배너가 잘 보이나요?"
```

### 4. 로그 확인
```powershell
# 버튼 클릭 로그
adb -s emulator-5554 logcat | findstr "TEST: Notification test button clicked"

# 알림 표시 로그
adb -s emulator-5554 logcat | findstr "TEST: Notification displayed"

# 전체 흐름 확인
adb -s emulator-5554 logcat | findstr "RetentionNotification"
```

---

## 📊 예상 로그

### 버튼 클릭 시
```
D/MainActivity: 🧪 TEST: Notification test button clicked
D/RetentionNotification: 🧪 TEST: Showing immediate notification
D/RetentionNotification: ✅ Notification channel verified
D/RetentionNotification: ✅ TEST: Notification displayed - Title: 🔔 테스트 알림입니다
D/MainActivity: ✅ TEST: Immediate notification triggered
```

---

## 🔍 문제 진단 가이드

### Case 1: 버튼이 안 보이는 경우

**원인**: 초기화 미완료
```
isInitComplete = false 상태
```

**해결**:
```
1. UMP Consent 완료 확인
2. Pre-Permission 다이얼로그 "확인" 클릭 확인
3. 로그 확인:
   D/MainActivity: 🚨 DEBUG: isInitializationComplete = TRUE
```

---

### Case 2: 버튼 클릭해도 알림이 안 뜨는 경우

**체크 1: 알림 권한 확인**
```powershell
# 앱 알림 권한 상태 확인
adb -s emulator-5554 shell dumpsys notification | findstr "alcoholictimer"
```

**체크 2: 알림 채널 확인**
```
설정 → 앱 → ZERO → 알림
  └─> "리텐션 알림" 채널 활성화 확인
```

**체크 3: 로그 확인**
```powershell
# 채널 생성 로그
adb -s emulator-5554 logcat | findstr "Notification channel"

# 알림 표시 로그
adb -s emulator-5554 logcat | findstr "TEST: Notification displayed"
```

---

### Case 3: 알림은 뜨는데 아이콘이 안 보이는 경우

**원인**: 아이콘 리소스 문제
```kotlin
.setSmallIcon(R.drawable.ic_launcher_foreground)
```

**해결**:
```
1. res/drawable/ic_launcher_foreground.xml 존재 확인
2. 없다면 다른 아이콘으로 변경:
   .setSmallIcon(R.mipmap.ic_launcher)
```

---

### Case 4: 알림 클릭해도 앱이 안 열리는 경우

**원인**: PendingIntent 문제

**체크**:
```kotlin
val pendingIntent = PendingIntent.getActivity(
    context,
    9999,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

**로그 확인**:
```powershell
adb -s emulator-5554 logcat | findstr "PendingIntent"
```

---

## 🎯 검증 포인트

### UI 검증
- [ ] 버튼이 화면 하단 중앙에 표시됨
- [ ] 버튼 색상이 주황색(#FF5722)
- [ ] 버튼 텍스트 "🔔 알림 즉시 테스트"
- [ ] 버튼 크기가 적절함 (화면 너비 80%)

### 알림 검증
- [ ] 버튼 클릭 즉시 알림 표시
- [ ] 알림 제목: "🔔 테스트 알림입니다"
- [ ] 알림 메시지: "아이콘과 배너가 잘 보이나요?"
- [ ] 알림 아이콘 표시됨
- [ ] 알림 클릭 시 앱 열림

### 로그 검증
- [ ] "TEST: Notification test button clicked"
- [ ] "Notification channel verified"
- [ ] "TEST: Notification displayed"
- [ ] "TEST: Immediate notification triggered"

---

## 🔧 추가 테스트 시나리오

### 시나리오 1: 연속 클릭 테스트
```
1. 버튼을 3번 연속 클릭
2. 알림이 3번 모두 표시되는지 확인
3. 알림 ID가 동일하므로 최신 알림만 보임 (정상)
```

### 시나리오 2: 알림 클릭 → 앱 진입
```
1. 버튼 클릭 → 알림 표시
2. Home 버튼으로 백그라운드 전환
3. 알림 클릭
4. 앱이 포그라운드로 복귀하는지 확인
```

### 시나리오 3: 알림 삭제
```
1. 버튼 클릭 → 알림 표시
2. 알림을 스와이프하여 삭제
3. 알림이 사라지는지 확인 (autoCancel = true)
```

---

## 📝 코드 위치 요약

### 수정된 파일

**1. RetentionNotificationManager.kt**
- 함수 추가: `showImmediateTestNotification()`
- 위치: 라인 227~280 (cancelAllNotifications 다음)

**2. MainActivity.kt**
- 버튼 추가: `MainActivityContent` 내부
- 위치: 라인 973~1012 (다이얼로그 다음)
- Import 추가: `padding`, `height`, `fillMaxWidth`, `sp`, `Button`, `ButtonDefaults`, `Text`

---

## ⚠️ 배포 전 주의사항

### 테스트 버튼 제거 필수!

**이유**:
- 실제 사용자에게 테스트 버튼이 보이면 혼란 발생
- UI가 지저분해 보임
- 의도하지 않은 알림 발송 가능

**제거 방법**:
```kotlin
// 배포 전 이 블록 전체 주석 처리 또는 삭제
/*
if (isInitComplete) {
    Box(...) {
        Button(...) {
            // ...
        }
    }
}
*/
```

**또는 BuildConfig 사용**:
```kotlin
// DEBUG 빌드에서만 표시
if (isInitComplete && BuildConfig.DEBUG) {
    Box(...) {
        // ...
    }
}
```

---

## ✅ 최종 체크리스트

### 구현 완료
- [x] `showImmediateTestNotification()` 함수 구현
- [x] 채널 생성 보장 로직 추가
- [x] MainActivity에 테스트 버튼 추가
- [x] 버튼 디자인 (주황색, 하단 중앙)
- [x] 로그 추가 (버튼 클릭, 알림 표시)
- [x] 필요한 import 추가

### 테스트 준비
- [x] 컴파일 에러 0개
- [x] 테스트 절차 문서화
- [x] 문제 진단 가이드 작성
- [x] 예상 로그 정리

### 배포 전 확인 (필수!)
- [ ] 테스트 버튼 제거 또는 주석 처리
- [ ] BuildConfig.DEBUG 조건 추가 (권장)
- [ ] UI 최종 확인

---

## 🎯 다음 단계

### 알림이 정상 표시되는 경우
```
✅ UI 및 채널 설정 정상
✅ WorkManager 예약 로직 검증으로 이동
   → TEST_MODE = true로 10초, 20초, 30초 테스트
```

### 알림이 표시되지 않는 경우
```
❌ 문제 진단 필요
1. 로그 확인 (채널 생성 여부)
2. 권한 확인 (알림 허용 여부)
3. 아이콘 리소스 확인
```

---

**작성일**: 2025-12-31  
**상태**: ✅ 완료  
**다음 단계**: 실제 기기에서 버튼 클릭 테스트  
**배포 전**: ⚠️ 테스트 버튼 제거 필수!

