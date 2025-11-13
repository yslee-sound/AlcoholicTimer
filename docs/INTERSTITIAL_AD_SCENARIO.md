# 전면 광고 (Interstitial Ad) 시나리오

## 📋 목차
1. [개요](#개요)
2. [트리거 조건](#트리거-조건)
3. [빈도 제한 정책](#빈도-제한-정책)
4. [초기 경험 보호](#초기-경험-보호)
5. [광고 노출 흐름도](#광고-노출-흐름도)
6. [구현 세부사항](#구현-세부사항)
7. [배너/전면 중복 방지](#배너전면-중복-방지)
8. [테스트 가이드](#테스트-가이드)

---

## 개요

AlcoholicTimer 앱의 전면 광고는 사용자 경험을 최대한 보호하면서도 적절한 수익화를 목표로 합니다.

### 주요 특징
- ✅ 홈 그룹 진입 기반 트리거: 홈 그룹(START/RUN/STOP)으로 "비홈 → 홈" 전환 시에만 카운트
- ✅ 단순 빈도 제한: 방문 3회 + 시간당/일일 제한 적용
- ✅ Supabase 원격 제어: 실시간 광고 정책 조정 가능
- ✅ 배너/전면 중복 방지: 전면광고 표시 중 배너 자동 숨김

---

## 트리거 조건

### 1. 홈 그룹 진입 카운트 방식 (현재 적용 중)

```
비홈 화면 → 홈 그룹(START/RUN/STOP) 진입 → 카운트 +1 → 3회 누적 시 광고 시도
```

#### 카운트 증가 조건
- ✅ 현재 라우트가 홈 그룹이고, 직전 라우트가 홈 그룹이 아닐 때 (비홈 → 홈 전환)
- ✅ 진행 중 세션으로 자동 리다이렉트된 "홈 그룹 내부 전환"은 카운트하지 않음
- ❌ 광고 정책이 비활성화된 경우

#### 카운트 리셋
- 📅 일일 리셋: 자정(00:00) 경과 시 0으로 초기화
- 🎯 광고 표시 성공 시: 실제로 전면광고가 표시된 경우에만 카운트 0으로 리셋
- ⚠️ 광고 미표시 시: 로드 안 됨/정책 제한으로 표시 못 하면 카운트 유지 → 다음 기회에 재시도

### 2. 구현 위치

| 위치 | 트리거 시점 | 구현 |
|------|-------------|------|
| NavGraph | NavController의 backStack 변화 구독 | 비홈→홈 그룹 전환 시 `HomeAdTrigger.registerHomeVisit(activity, sourceRoute)` 호출 (중앙 일괄 처리) |

---

## 빈도 제한 정책

### 1. 시간당 제한 (AdController)
```
제어: Supabase 원격 제어 (ad_interstitial_max_per_hour)
Fallback 기본값: 2회 (Supabase 값이 없을 경우)
```
Supabase에 값이 있으면 우선 적용됩니다.

### 2. 일일 제한 (AdController)
```
제어: Supabase 원격 제어 (ad_interstitial_max_per_day)
Fallback 기본값: 10회 (Supabase 값이 없을 경우)
```
Supabase에 값이 있으면 우선 적용됩니다.

### 3. 쿨다운 (InterstitialAdManager)
```
기본값: 마지막 광고 표시 후 2분
상수: DEFAULT_COOLDOWN_MS = 2 * 60 * 1000L
```

### 4. 방문 임계치 (HomeAdTrigger)
```
기본값: 홈 그룹 진입 3회
상수: VISIT_THRESHOLD = 3
```

### 💡 Fallback(대체값)이란?

Fallback은 주 데이터 소스를 사용할 수 없을 때 대신 사용하는 예비 기본값입니다.

```kotlin
val maxPerHour = policy?.adInterstitialMaxPerHour ?: 2 // Supabase 값(우선), 없으면 Fallback 2
```

---

## 초기 경험 보호

현재 버전은 “시간 기반 초기 쿨다운”을 사용하지 않습니다. 대신 다음 간접 보호 요소만 유지됩니다:
1. 홈 그룹 진입 3회 조건 충족 필요 (즉시 강제 노출 없음)
2. 인터스티셜 자체 내부 쿨다운(마지막 표시 후 2분)
3. 일/시간 빈도 제한(Supabase 정책)

---

## 광고 노출 흐름도

```mermaid
graph TD
    A[비홈 화면] --> B[홈 그룹(START/RUN/STOP) 진입]
    B --> C[카운트 +1]
    C --> D{카운트 >= 3?}
    D -->|No| E[프리로드 유지]
    D -->|Yes| F{canShow & isLoaded?}
    F -->|No| G[카운트 유지, 프리로드]
    F -->|Yes| H[전면 광고 표시 시도]
    H --> I{showed=true?}
    I -->|Yes| J[카운트 0 리셋, 기록]
    I -->|No| G
    J --> E
    G --> E
```

---

## 구현 세부사항

### 1) HomeAdTrigger.kt (홈 그룹 카운트)

```kotlin
object HomeAdTrigger {
    private const val VISIT_THRESHOLD = 3
    // prefs keys: home_visits_count, home_visits_day

    fun registerHomeVisit(activity: Activity, source: String)
    private fun resetIfDayChanged(context: Context)
}
```

- 표시 성공(showed=true)일 때만 카운트 0으로 리셋
- 표시 실패/제한 시 카운트 유지하여 다음 기회 보장

### 2) NavGraph.kt (중앙 관찰자)

```kotlin
LaunchedEffect(Unit) {  // Unit 키로 단 한 번만 시작
    var wasHome = false
    navController.currentBackStackEntryFlow.collect { entry ->
        val route = entry.destination.route
        val isHome = route in setOf(Screen.Start.route, Screen.Run.route, Screen.Quit.route)
        if (activity != null && isHome && !wasHome) {
            HomeAdTrigger.registerHomeVisit(activity, route ?: "home")
        }
        wasHome = isHome
    }
}
```

- 홈 그룹: Start, Run, Quit(Stop)
- 그룹 내부 전환(Start↔Run↔Quit)은 카운트 없음
- **중요**: LaunchedEffect(Unit) 키로 중복 구독 방지 (navController를 키로 쓰면 리컴포지션 시 재시작됨)
- **중요**: StartActivity 등 레거시 진입점의 중복 호출은 모두 제거됨

### 3) AdController.kt (Supabase 정책)
- is_active & ad_interstitial_enabled가 모두 true일 때만 표시 시도
- 시간당/일일 제한을 SharedPreferences의 타임스탬프와 비교

### 4) InterstitialAdManager.kt
- 내부 쿨다운(2분) + 일일 3회 제한(릴리즈)
- Debug 빌드에서는 내부 제한을 우회(개발/테스트 편의)

---

## 배너/전면 중복 방지

전면광고와 배너광고가 동시에 표시되는 것을 방지하기 위한 메커니즘입니다.

### 구현 원리

```kotlin
// AdController.kt
private val _isInterstitialShowing = mutableStateOf(false)

val isInterstitialShowing: State<Boolean>
    get() = _isInterstitialShowing  // State로 노출하여 Compose 구독 가능

internal fun setInterstitialShowing(showing: Boolean) {
    _isInterstitialShowing.value = showing
}
```

```kotlin
// AdBanner.kt - State 직접 구독
// 정책은 초기에 한 번만 체크
val isPolicyEnabled = remember { 
    AdController.isBannerEnabled() 
}

// 전면광고 상태는 실시간 구독
val isInterstitialShowing by AdController.isInterstitialShowing

// 배너 표시 여부 계산
val shouldShowBanner = isPolicyEnabled && !isInterstitialShowing

if (!shouldShowBanner) {
    return  // 아예 렌더링하지 않음
}
```

```kotlin
// BaseScaffold.kt - 배너 영역 전체 조건부 렌더링
val isInterstitialShowing by AdController.isInterstitialShowing

Column {
    if (!isInterstitialShowing) {
        Surface {  // 배너 컨테이너
            AdmobBanner()
        }
        HorizontalDivider()  // 구분선
    }
    // 콘텐츠...
}
```

### 동작 흐름

1. **전면광고 표시 시작** (`onAdShowedFullScreenContent`)
   - `AdController.setInterstitialShowing(true)` 호출
   - 배너의 `isBannerEnabled()`가 false 반환
   - 배너 컴포저블이 자동으로 숨김 처리

2. **전면광고 닫힘** (`onAdDismissedFullScreenContent`)
   - `AdController.setInterstitialShowing(false)` 호출
   - 배너의 `isBannerEnabled()`가 다시 정책 기반 판단
   - 배너 컴포저블이 자동으로 다시 표시

3. **전면광고 표시 실패** (`onAdFailedToShowFullScreenContent`)
   - `AdController.setInterstitialShowing(false)` 호출
   - 실패해도 배너는 정상 복구

### 주요 파일

| 파일 | 역할 |
|------|------|
| `AdController.kt` | 전면광고 표시 상태 관리 (`_isInterstitialShowing`) |
| `InterstitialAdManager.kt` | 전면광고 콜백에서 상태 업데이트 |
| `AdBanner.kt` | `isInterstitialShowing` 구독, false일 때 early return |
| `BaseScaffold.kt` | `isInterstitialShowing` 구독, true일 때 배너 영역 전체 제거 |

### 로그 확인

```
AdController: Interstitial showing state: true
AdController: Banner disabled: interstitial is showing
... (전면광고 표시)
AdController: Interstitial showing state: false
AdController: Banner enabled (정책에 따라)
```

---

## 테스트 가이드

### 최초 실행 시나리오
1. 앱 최초 실행 → Start 화면 표시 → 카운트 1/3 (source=start)
2. Records 탭 → Start 복귀 → 카운트 2/3
3. 다시 다른 탭 → Start 복귀 → 카운트 3/3 → 광고 시도

### 빠른 전환 시나리오
- 비홈↔홈을 빠르게 반복해도 3회 도달 후 "표시 가능한 최초 순간"에 광고 표시
- 로드 안 됐거나 정책 제한이면 카운트 유지, 다음 진입 시 재시도

### 배너/전면 중복 방지 테스트
1. 홈 화면에서 배너 광고가 표시되는지 확인
2. 홈 그룹 3회 진입하여 전면광고 트리거
3. 전면광고 표시 중 배너가 사라지는지 확인
4. 전면광고 닫으면 배너가 다시 나타나는지 확인

- 홈 그룹 진입 테스트: 비홈 → 홈(START/RUN/STOP)으로 들어올 때만 카운트 증가하는지 확인
- 빈도 제한 테스트: 시간당/일일 한도를 낮춰 차단 로그 확인
- 로그 필터: HomeAdTrigger, AdController, InterstitialAdManager

예시 로그
```
HomeAdTrigger: Home visit recorded: 3/3 (source=run)
AdController: ✅ Can show interstitial: 1/10 (day), 0/2 (hour)
InterstitialAdManager: onAdShowedFullScreenContent
```

---

## PocketChord 정책 비교 (요약)

| 구분 | PocketChord | AlcoholicTimer (현재) | 비고 / 설명 |
|---|---|---|---|
| 트리거 기반 | 화면 전환 패턴 3회 (상세↔홈 반복) | 홈 그룹(START/RUN/STOP) 진입 3회 | PocketChord는 특정 전환 패턴, AlcoholicTimer는 그룹 진입 이벤트 |
| 초기 시간 쿨다운 | 문서상 명시 없음 | 사용 안 함 (그룹 임계치만 적용) | 시간 게이트 미사용 |
| 표시 후 재쿨다운 | ~60초 | 120초 + 콜드 스타트 1회 제한 | AlcoholicTimer가 더 길어 빈도 억제 |
| 시간당/일일 최대 | ≤2 / ≤15 | Supabase 값 또는 Fallback 2/10 | 원격 제어 가능 |

---

## 결론
- 홈 그룹 진입 기반 트리거로 “실제 홈 복귀”를 정확히 카운트합니다.
- Supabase 정책과 내부 쿨다운으로 과도한 노출을 방지합니다.
- 운영은 Supabase에서 정책을 안전하게 조정하고, 앱은 Fallback으로 안정성을 확보합니다.

---

문서 버전: 1.3  
최종 수정: 2025-11-14 (배너/전면 중복 방지 로직 추가)  
작성자: AlcoholicTimer Dev Team
