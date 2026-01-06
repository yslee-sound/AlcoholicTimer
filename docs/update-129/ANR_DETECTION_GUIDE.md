# ANR(Application Not Responding) 감지 가이드

## 🎯 개요

ANR은 앱이 5초 이상 UI 스레드를 블로킹하여 사용자 입력에 응답하지 못할 때 발생합니다.

---

## 📱 1. 사용자 관점에서 확인

### ANR 발생 시 나타나는 현상
- ✅ "앱이 응답하지 않습니다" 다이얼로그 팝업
- ✅ 화면이 멈추고 터치 입력이 먹히지 않음
- ✅ "대기" 또는 "앱 종료" 버튼이 표시됨

---

## 🔍 2. Logcat으로 확인 (개발자 권장)

### 방법 1: ANR 키워드로 필터링

```powershell
# 실시간 ANR 모니터링 (백그라운드 실행)
adb -s emulator-5554 logcat -v time | findstr /i "ANR"
```

**찾아야 할 로그 패턴:**
```
01-06 15:23:45.123 I/ActivityManager: ANR in kr.sweetapps.alcoholictimer
01-06 15:23:45.456 E/ActivityManager: ANR in kr.sweetapps.alcoholictimer (kr.sweetapps.alcoholictimer/.ui.main.MainActivity)
01-06 15:23:45.789 I/ActivityManager: Reason: Input dispatching timed out
```

### 방법 2: 앱 전체 로그 모니터링

```powershell
# 앱의 모든 로그 실시간 확인
adb -s emulator-5554 logcat -v time | findstr "alcoholictimer"
```

### 방법 3: MessageQueue 블로킹 감지

```powershell
# UI 스레드 블로킹 의심 로그 확인
adb -s emulator-5554 logcat -v time | findstr "nativePollOnce"
```

**의심 패턴:**
```
01-06 15:23:40.000 D/Looper: android.os.MessageQueue.nativePollOnce (blocked for 5000ms)
```

---

## 📂 3. ANR Trace 파일 확인

### ANR 발생 시 자동 생성되는 파일

```powershell
# 1. ANR trace 파일 목록 확인
adb -s emulator-5554 shell ls -lh /data/anr/

# 2. 가장 최근 trace 파일 내용 확인
adb -s emulator-5554 shell cat /data/anr/traces.txt

# 3. trace 파일을 PC로 다운로드
adb -s emulator-5554 pull /data/anr/traces.txt G:\Workspace\AlcoholicTimer\anr_traces.txt
```

**Trace 파일에서 확인할 내용:**
- 어떤 스레드가 블로킹되었는지
- 어떤 함수 호출이 오래 걸렸는지
- Stack trace로 정확한 원인 파악

---

## 🧪 4. 강제 ANR 테스트 (디버깅용)

### 테스트 코드 예시

```kotlin
// ⚠️ 절대 프로덕션에 넣지 말 것!
// 디버깅 목적으로 ANR을 강제 발생시키는 코드

Button(onClick = {
    // UI 스레드를 10초간 블로킹 (ANR 발생)
    Thread.sleep(10_000)
}) {
    Text("ANR 테스트 버튼")
}
```

---

## 🎯 5. 이번 수정 사항 검증 방법

### Before (수정 전)
```
CommunityScreen 진입 시:
└─ LaunchedEffect에서 MobileAds.initialize() 호출
   └─ UI 스레드 블로킹 → ANR 발생 가능
```

### After (수정 후)
```
MainApplication.onCreate():
└─ Dispatchers.IO에서 MobileAds.initialize() 호출
   └─ 백그라운드 스레드 → ANR 없음
```

### 검증 절차 (단계별 실행)

#### ✅ STEP 1: 에뮬레이터 준비

```powershell
# 에뮬레이터 실행 상태 확인
adb devices
```

**기대 출력:**
```
List of devices attached
emulator-5554   device
```

#### ✅ STEP 2: ANR 모니터링 시작 (터미널 1 - 필수!)

```powershell
adb -s emulator-5554 logcat -v time | findstr /i "ANR"
```

> ⚠️ **중요:** 이 명령어는 계속 실행 상태로 두세요! (Ctrl+C로 종료 전까지 실시간 모니터링)

#### ✅ STEP 3: 앱 로그 모니터링 (터미널 2 - 선택)

**새 터미널 창을 열고 실행:**

```powershell
adb -s emulator-5554 logcat -v time | findstr "MainApplication"
```

**기대 로그:**
```
01-06 15:23:45.123 D/MainApplication: ✅ MobileAds initialized (background)
01-06 15:23:45.456 D/MainApplication:    Adapter status: {...}
```

#### ✅ STEP 4: 앱 실행 및 테스트

1. **에뮬레이터에서 앱 실행**
2. **커뮤니티 탭으로 이동** (이전에 ANR 발생 지점)
3. **여러 탭을 전환하며 5분간 테스트**

#### ✅ STEP 5: 결과 확인

**성공 시나리오:**
- ✅ 터미널 1에 아무 로그도 안 뜸 (ANR 없음)
- ✅ 터미널 2에 "MobileAds initialized (background)" 로그 출력
- ✅ 앱이 부드럽게 작동

**실패 시나리오:**
- ❌ 터미널 1에 "ANR in kr.sweetapps.alcoholictimer" 출력
- ❌ 화면에 "앱이 응답하지 않습니다" 팝업
- ❌ 화면이 5초 이상 멈춤

---

## 📊 6. Android Studio에서 확인

### Logcat 필터 설정

1. **Android Studio** → **Logcat** 탭 열기
2. 필터 생성:
   - **Package**: `kr.sweetapps.alcoholictimer`
   - **Regex**: `ANR|nativePollOnce|MobileAds`
3. 앱 실행 후 로그 관찰

### Profiler로 UI 스레드 블로킹 확인

1. **View** → **Tool Windows** → **Profiler**
2. **CPU** 프로파일러 시작
3. 커뮤니티 탭 진입
4. Main Thread 타임라인에서 5초 이상 블로킹 구간 확인

---

## 🚨 7. ANR 발생 시 대응 방법

### 즉시 확인 사항

```powershell
# 1. 최근 ANR trace 확인
adb -s emulator-5554 pull /data/anr/traces.txt ./anr_latest.txt

# 2. 앱 로그 전체 덤프
adb -s emulator-5554 logcat -d > logcat_dump.txt

# 3. 시스템 로그 확인
adb -s emulator-5554 logcat -b system -d > system_log.txt
```

### Stack Trace 분석 팁

ANR trace 파일에서 찾아야 할 패턴:
```
"main" prio=5 tid=1 Sleeping
  | group="main" ...
  | sysTid=12345 nice=0 ...
  at java.lang.Thread.sleep(Native Method)
  at kr.sweetapps.alcoholictimer.ui.tab_03.CommunityScreen$NativeAdItem$2$1.invokeSuspend(CommunityScreen.kt:1715)
  at com.google.android.gms.ads.MobileAds.initialize(MobileAds.java:123)
```

---

## ✅ 8. 이번 수정으로 해결된 문제

### 원인
```kotlin
// ❌ BAD: UI 스레드에서 동기 초기화
LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
        MobileAds.initialize(context) // 여전히 메인 스레드 블로킹 가능
    }
}
```

### 해결책
```kotlin
// ✅ GOOD: 앱 시작 시점에 백그라운드에서 초기화
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainApplication) { }
        }
    }
}
```

---

## 📝 9. 체크리스트

- [ ] Logcat에서 ANR 키워드 모니터링 중
- [ ] 앱 시작 시 "MobileAds initialized (background)" 로그 확인
- [ ] 커뮤니티 탭 진입 시 UI 버벅임 없음
- [ ] 5분간 여러 탭을 전환해도 ANR 미발생
- [ ] ANR trace 파일 없음 확인

---

## 🎓 참고 자료

- [Android Developers - ANR 디버깅](https://developer.android.com/topic/performance/vitals/anr)
- [AdMob Best Practices - 초기화 타이밍](https://developers.google.com/admob/android/app-open-ads)

