# ✅ 릴리즈 APK 재빌드 완료 - 최종 설치 가이드

**날짜**: 2026-01-03  
**버전**: v1.2.0-hotfix (versionCode: 2026010203)  
**APK 위치**: `G:\Workspace\AlcoholicTimer\app\build\outputs\apk\release\app-release.apk`  
**상태**: ✅ 빌드 완료 (UMP 5초 타임아웃 확실히 포함됨)

---

## 🎯 최종 해결 절차

### 1️⃣ 기존 앱 완전 삭제

**PowerShell에서 실행**:

```powershell
adb -s emulator-5554 uninstall kr.sweetapps.alcoholictimer
```

**또는 수동 삭제**:
- 에뮬레이터에서 앱 아이콘 길게 누르기
- "제거" 또는 "삭제" 선택
- 데이터도 함께 삭제

---

### 2️⃣ 새 APK 설치

```powershell
adb -s emulator-5554 install "G:\Workspace\AlcoholicTimer\app\build\outputs\apk\release\app-release.apk"
```

**성공 메시지**:
```
Success
```

---

### 3️⃣ 로그 모니터링 시작

**새 PowerShell 창 열어서**:

```powershell
adb -s emulator-5554 logcat -c

adb -s emulator-5554 logcat -v time | Select-String "UmpConsentManager"
```

---

### 4️⃣ 앱 실행 및 시간 측정

```powershell
# 실행
adb -s emulator-5554 shell am start -n kr.sweetapps.alcoholictimer/.ui.screens.SplashScreen
```

**스톱워치로 측정**:
- 시작: 앱 아이콘 클릭 시점
- 종료: 메인 화면 보이는 시점

---

## 🔍 예상 결과

### ✅ 성공 - 타임아웃 5초

**로그 출력**:
```
01-03 XX:XX:XX.XXX D/UmpConsentManager: gatherConsent() start
01-03 XX:XX:XX.XXX W/UmpConsentManager: ⏱️ TIMEOUT (5s): UMP 서버 응답 없음 - 강제 진행
```

**로딩 시간**: 5초 ✅

---

### ✅ 성공 - 정상 응답 2~3초

**로그 출력**:
```
01-03 XX:XX:XX.XXX D/UmpConsentManager: gatherConsent() start
01-03 XX:XX:XX.XXX D/UmpConsentManager: requestConsentInfoUpdate success
01-03 XX:XX:XX.XXX D/UmpConsentManager: Consent finished: status=NOT_REQUIRED
```

**로딩 시간**: 2~3초 ✅

---

### ❌ 실패 - 여전히 60초

**로그 출력**:
```
01-03 XX:XX:XX.XXX D/UmpConsentManager: gatherConsent() start
... (60초 대기) ...
01-03 XX:XX:XX.XXX E/UmpConsentManager: requestConsentInfoUpdate failed
```

**로딩 시간**: 60초 ❌

**조치**: 아래 "여전히 안 되는 경우" 섹션 참고

---

## 🚨 여전히 안 되는 경우

### 버전 확인

```powershell
adb -s emulator-5554 shell dumpsys package kr.sweetapps.alcoholictimer | Select-String "versionCode|versionName"
```

**확인할 값**:
```
versionCode=2026010203
versionName=1.2.0-hotfix
```

**만약 다른 값이 나온다면**: 이전 버전이 실행 중! 재설치 필요

---

### 앱 데이터 완전 삭제 후 재설치

```powershell
# 1. 완전 삭제 (데이터 포함)
adb -s emulator-5554 shell pm uninstall -k kr.sweetapps.alcoholictimer

# 2. 재설치
adb -s emulator-5554 install "G:\Workspace\AlcoholicTimer\app\build\outputs\apk\release\app-release.apk"

# 3. 실행
adb -s emulator-5554 shell am start -n kr.sweetapps.alcoholictimer/.ui.screens.SplashScreen
```

---

### 실제 기기에서 테스트

**에뮬레이터 대신 실제 Android 기기 사용**:

1. USB 디버깅 활성화
2. 기기 연결 후 확인:
   ```powershell
   adb devices
   ```
3. 기존 앱 삭제:
   ```powershell
   adb uninstall kr.sweetapps.alcoholictimer
   ```
4. 새 APK 설치:
   ```powershell
   adb install "G:\Workspace\AlcoholicTimer\app\build\outputs\apk\release\app-release.apk"
   ```
5. 로그 모니터링:
   ```powershell
   adb logcat -v time | Select-String "UmpConsentManager"
   ```
6. 앱 실행 및 시간 측정

---

## 🎯 변경사항 요약

### 수정된 파일

1. **UmpConsentManager.kt**
   - 5초 타임아웃 안전장치 추가
   - Race Condition 방식 구현
   - `AtomicBoolean`으로 중복 실행 방지

2. **build.gradle.kts**
   - versionCode: 2026010203
   - versionName: 1.2.0-hotfix

### 핵심 로직

```kotlin
// 타임아웃 설정
handler.postDelayed({
    if (isCompleted.compareAndSet(false, true)) {
        Log.w(TAG, "⏱️ TIMEOUT (5s): UMP 서버 응답 없음 - 강제 진행")
        onComplete(false) // 5초 후 강제 진행
    }
}, 5000L)

// UMP 정상 응답 시
if (isCompleted.compareAndSet(false, true)) {
    onComplete(result) // 정상 처리
}
```

---

## 📊 기대 효과

| 케이스 | Before | After | 개선 |
|--------|--------|-------|------|
| 정상 | 2~3초 | 2~3초 | 동일 |
| 지연 | 10~30초 | 5초 | 80% ↓ |
| **타임아웃** | **60초** | **5초** | **92% ↓** |

---

## ✅ 최종 체크리스트

설치 전 확인:

- [x] clean 빌드 완료
- [x] assembleRelease 성공
- [x] versionCode 증가 (2026010203)
- [x] APK 파일 생성 확인
- [ ] 기존 앱 삭제
- [ ] 새 APK 설치
- [ ] Logcat 모니터링
- [ ] 로딩 시간 측정

---

## 🎉 성공 기준

**다음 중 하나라도 만족하면 성공**:

1. ✅ 로딩 시간 5초 이하
2. ✅ 로그에 "TIMEOUT (5s)" 메시지 보임
3. ✅ 60초 대기 현상 사라짐

---

## 📞 문제 지속 시 제공 정보

만약 여전히 60초가 걸린다면 다음 정보를 알려주세요:

1. **설치된 버전**:
   ```powershell
   adb shell dumpsys package kr.sweetapps.alcoholictimer | Select-String "versionCode"
   ```

2. **로그 전체** (처음부터 끝까지):
   ```powershell
   adb logcat -d | Select-String "UmpConsentManager"
   ```

3. **실제 측정 시간**: ? 초

---

**작성**: AI Agent (GitHub Copilot)  
**날짜**: 2026-01-03  
**상태**: ✅ 재빌드 완료 - 설치 대기  
**긴급도**: 🔴 Critical

---

## 💡 TIP

**가장 빠른 확인 방법**:

```powershell
# 한 번에 실행
adb -s emulator-5554 uninstall kr.sweetapps.alcoholictimer; adb -s emulator-5554 install "G:\Workspace\AlcoholicTimer\app\build\outputs\apk\release\app-release.apk"; adb -s emulator-5554 shell am start -n kr.sweetapps.alcoholictimer/.ui.screens.SplashScreen
```

그리고 스톱워치로 시간 측정!

