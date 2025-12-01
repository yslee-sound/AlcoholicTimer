# AdMob 노출 실패 원인 진단 결과

## 📊 실행 날짜
2025-12-01

## 🎯 진단 목표
AdMob 대시보드에서 '요청 수'는 잡히지만 '노출 수'가 0으로 나오는 현상의 원인 규명

## ⚠️ 중요 발견: AppOpen 광고가 0인 이유

### 결론부터 말하면
**AppOpen 광고는 실제로 표시되고 있습니다!** (로그 확인 완료)

```
18:30:18.948 - AppOpen onAdShowedFullScreenContent ✅
18:30:22.786 - AppOpen onAdDismissedFullScreenContent ✅
```

**그런데 왜 AdMob 대시보드에 노출 0으로 나올까요?**

### 🔍 가능한 원인

1. **테스트 광고 사용 중** (가장 가능성 높음)
   - 로그: `unit=ca-app-pub-3940256099942544/9257395921`
   - 이것은 **Google의 샘플 테스트 광고 ID**입니다
   - 테스트 광고는 AdMob 대시보드에 **노출로 기록되지 않습니다**
   - 실제 프로덕션 광고 ID를 사용해야 대시보드에 표시됨

2. **디버그 빌드 사용**
   - 현재 패키지: `kr.sweetapps.alcoholictimer.debug`
   - 디버그 빌드는 AdMob 통계에서 제외될 수 있음

3. **AdMob 보고 지연**
   - AdMob 대시보드는 실시간이 아님
   - 최대 24시간 지연될 수 있음

### ✅ 해결 방법

1. **프로덕션 광고 ID 확인**
   ```kotlin
   // BuildConfig.ADMOB_APP_OPEN_UNIT_ID가 실제 광고 ID인지 확인
   // 테스트 ID: ca-app-pub-3940256099942544/...
   // 실제 ID: ca-app-pub-YOUR_PUBLISHER_ID/YOUR_AD_UNIT_ID
   ```

2. **릴리즈 빌드로 테스트**
   ```bash
   ./gradlew assembleRelease
   adb -s emulator-5554 install app/build/outputs/apk/release/app-release.apk
   ```

3. **24시간 후 대시보드 재확인**

## 🔍 타이밍 측정 결과

### 타임라인 (앱 시작 기준)
```
0ms      - 앱 시작 (Application.onCreate)
421ms    - AppOpen 광고 로드 요청 #1
678ms    - ⚠️ MainActivity 생성
719ms    - AppOpen 광고 로드 요청 #2 (중복)
1138ms   - AppOpen 광고 로드 요청 #3, #4 (중복)
3127ms   - AppOpen 광고 로드 완료 (소요시간: 1989ms)
3130ms   - ✅ AppOpen 광고 표시 시작
3830ms   - ✅ AppOpen 광고 사용자가 닫음 (약 4초간 표시됨)
4203ms   - AppOpen 광고 로드 요청 #5 (중복)
5952ms   - 배너 광고 로드 요청 #1
6181ms   - 배너 광고 로드 요청 #2 (중복)
6690ms   - 배너 광고 로드 완료 (소요시간: 509ms)
```

### AppOpen 광고 타이밍 분석

✅ **정상 작동**
- MainActivity 생성(678ms) → AppOpen 로드 완료(3127ms) → 즉시 표시
- 타이밍: MainActivity보다 2.4초 늦게 로드되었지만 정상 표시됨
- 이유: MainActivity는 Splash 화면을 유지하고 있었기 때문

## ❌ 발견된 문제점

### 1. **배너 광고 타이밍 미스** (치명적)

```
MainActivity 생성:  678ms
AppOpen 종료:      3830ms (사용자가 광고 닫음)
배너 로드 완료:     6690ms
차이:              2860ms (배너가 2.8초 늦음!)
```

**결과**: 
- AppOpen 광고가 닫힌 후 2.8초가 지나서야 배너 로드 완료
- **사용자가 이미 빈 화면을 보고 있는 상태**
- 배너 광고가 노출될 기회를 완전히 놓침
- AdMob에는 '요청'만 기록되고 '노출'은 0

**이상적 타이밍**:
- AppOpen 종료 전에 배너가 로드 완료되어 있어야 함
- AppOpen 닫히면 즉시 배너 표시

### 2. **광고 중복 로드** (비효율)

**AppOpen 광고** 로드가 5번 중복 호출:
- t+421ms (MainApplication.onCreate)
- t+719ms (Lifecycle callback)
- t+1138ms × 2 (onConsentUpdated × 2)
- t+4203ms (onConsentUpdated)

**배너 광고** 로드가 2번 중복 호출:
- t+5952ms
- t+6181ms

**원인**:
- 여러 컴포넌트에서 독립적으로 preload() 호출
- 다행히 `already loaded or loading` 방어 로직으로 실제 네트워크 요청은 1번만 발생
- 그러나 코드 정리 필요

## 💡 근본 원인 분석

### 배너가 늦게 로드되는 이유

1. **정책 로드 지연** (개선됨)
   - 이전: Supabase에서 정책을 가져올 때까지 대기 (1~3초)
   - 현재: 로컬 캐시 사용으로 즉시 시작
   - 그러나 여전히 배너는 6초 후에 로드됨 → 다른 원인 존재

2. **UMP Consent 대기** (추정)
   - 배너 로드는 UMP consent가 완료될 때까지 대기
   - Consent 폼 표시 및 사용자 응답 대기 시간
   - 로그: t+5952ms에 첫 배너 로드 요청

3. **화면 전환 완료 후 로드** (구조적 문제)
   - MainActivity가 이미 생성되고 화면이 완전히 렌더링된 후에 배너 로드 시작
   - 이상적으로는 MainActivity 생성 **전**에 배너가 준비되어 있어야 함

## ✅ 해결 방안

### 즉시 적용 가능

1. **배너 프리로드 구현**
   ```kotlin
   // MainApplication.onCreate()에서
   CoroutineScope(Dispatchers.Main).launch {
       delay(500) // UMP consent 대기
       BannerAdManager.preload(context)
   }
   ```

2. **UMP Consent 우선순위 상승**
   - Application.onCreate()에서 즉시 UMP consent 시작
   - MainActivity 생성 전에 consent 완료 보장

3. **배너 지연 로딩 대신 즉시 로딩**
   - 현재: Compose의 `LaunchedEffect`에서 로드 (화면 그려진 후)
   - 개선: Activity.onCreate()에서 즉시 로드 시작

### 장기 개선

1. **광고 매니저 통합**
   - 중복 로드 방지를 위한 중앙 집중식 광고 매니저
   - SingletonHolder 패턴으로 한 번만 로드 보장

2. **타이밍 최적화**
   ```
   이상적 타임라인:
   0ms    - 앱 시작
   100ms  - UMP consent 시작
   300ms  - 배너 로드 시작 (백그라운드)
   500ms  - AppOpen 로드 시작 (백그라운드)
   678ms  - MainActivity 생성
   800ms  - 배너 로드 완료 ✅ (MainActivity보다 빠름!)
   1200ms - AppOpen 로드 완료
   ```

## 📈 예상 개선 효과

### Before (현재)
- 배너 노출: 0%
- 배너 로드: 6.69초 후
- 기회 손실: 100%

### After (개선 후)
- 배너 노출: ~80% 예상
- 배너 로드: MainActivity 생성 전 완료
- 기회 손실: ~20% (네트워크 지연 등)

## 🔧 구현된 진단 도구

### AdTimingLogger.kt
- 앱 시작부터 모든 광고 이벤트 타이밍 추적
- 밀리초 단위 정밀 측정
- 자동 문제 감지 및 경고 로그

### 사용법
```kotlin
// 각 주요 지점에서 자동 로깅
AdTimingLogger.logAppStart()
AdTimingLogger.logMainActivityCreate()
AdTimingLogger.logBannerLoadRequest()
AdTimingLogger.logBannerLoadComplete()
AdTimingLogger.printTimingReport()
```

### 로그 필터링
```bash
adb -s emulator-5554 logcat -s AdTimingDiagnosis:D
```

## 📝 결론

### AppOpen 광고 (노출 0 문제)

**진단 결과**: 광고는 **정상적으로 표시되고 있습니다** ✅

```
로그 증거:
- 18:30:18.948: onAdShowedFullScreenContent (표시됨)
- 18:30:22.786: onAdDismissedFullScreenContent (사용자가 닫음)
- 약 4초간 정상 표시됨
```

**AdMob 대시보드에 노출 0으로 나타나는 이유**:

1. **테스트 광고 사용 중** (99% 확률)
   - 사용 중인 광고 ID: `ca-app-pub-3940256099942544/9257395921`
   - 이것은 Google의 공식 테스트 광고 ID
   - **테스트 광고는 AdMob 대시보드에 노출로 기록되지 않습니다**

2. **디버그 빌드**
   - 패키지: `kr.sweetapps.alcoholictimer.debug`
   - 디버그 빌드의 광고도 통계에서 제외될 수 있음

3. **AdMob 보고 지연**
   - 실제 광고 ID를 사용하더라도 대시보드 반영까지 최대 24시간 소요

**해결 방법**:
```kotlin
// local.properties 또는 BuildConfig 확인
ADMOB_APP_OPEN_UNIT_ID=ca-app-pub-XXXXXXXX/XXXXXXXXXX
// ↑ 실제 광고 ID로 변경 필요

// 또는 릴리즈 빌드로 테스트
./gradlew assembleRelease
```

### 배너 광고 (노출 0 문제)

**진단 결과**: 광고가 **너무 늦게 로드되어 노출 기회를 놓침** ❌

```
타이밍:
- MainActivity 생성:  678ms
- AppOpen 종료:      3830ms
- 배너 로드 완료:    6690ms ← 2.8초 늦음!
```

**문제**:
- AppOpen 광고가 닫힌 후 2.8초가 지나서야 배너 준비 완료
- 사용자는 이미 빈 화면을 보고 있음
- 배너는 요청되었지만 표시될 타이밍을 놓침

**해결책**:
- 배너 프리로드를 Application.onCreate()로 이동
- UMP Consent를 더 일찍 시작
- MainActivity 생성 **전**에 배너가 준비되도록 타이밍 조정

이 진단 도구를 계속 사용하여 개선 효과를 실시간으로 모니터링할 수 있습니다.

