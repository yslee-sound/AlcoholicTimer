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
9. [PocketChord 정책 비교](#pocketchord-정책-비교)

---

## 개요

AlcoholicTimer 앱의 전면 광고는 사용자 경험을 최대한 보호하면서도 적절한 수익화를 목표로 합니다.

### 주요 특징
- ✅ 홈 그룹 진입 기반 트리거: 홈 그룹(START/RUN/STOP)으로 "비홈 → 홈" 전환 시에만 카운트
- ✅ 단순 빈도 제한: 방문 3회 + 시간당/일일 제한 적용
- ✅ Supabase 원격 제어: 실시간 광고 정책 조정 가능
- ✅ 배너/전면 중복 방지: 전면광고 표시 중 배너 자동 숨김 + 컨테이너 공간 예약으로 레이아웃 시프트 방지

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
기본값: 마지막 광고 표시 후 60초
상수: DEFAULT_COOLDOWN_MS = 60 * 1000L
```

#### 표시 후 재쿨다운(정의)
- 전면광고가 한 번 표시된 직후, 최소한의 대기 시간(여기서는 60초) 동안 추가 전면 노출을 막는 시간 게이트입니다.
- 목적: 과도한 연속 노출 방지, 사용자 경험 보호.
- 다른 제한(시간당/일일)과 독립적으로 함께 적용됩니다. 즉, 재쿨다운과 시간당/일일 모두를 통과해야 다음 전면을 띄울 수 있습니다.

#### 콜드 스타트 1회 제한(비활성)
- 변경: 세션당 1회 제한 정책은 비활성화했습니다. 대신 초기 보호 창(60초)을 도입해 콜드 스타트 직후 과도 노출을 방지합니다.
- 기존 정의(참고): 앱 프로세스가 새로 시작된 "콜드 스타트" 세션 동안 전면광고 최대 1회 허용.

#### 초기 보호 창(정의)
- 뜻: 앱을 켠 직후 일정 시간(60초) 동안은 전면광고를 표시하지 않습니다.
- 목적: 초기 로딩/라우팅/동의 등 민감 구간의 실수·중복 트리거 방지와 경험 보호.
- 코드: `InterstitialAdManager.INITIAL_PROTECTION_MS = 60s`, `noteAppStart()`로 시작 시각 기록 후 `passesPolicy()`에서 차단.

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

현재 버전은 다음 간접 보호 요소를 적용합니다:
1. 홈 그룹 진입 3회 조건 충족 필요 (즉시 강제 노출 없음)
2. 인터스티셜 자체 내부 쿨다운(마지막 표시 후 60초)
3. 초기 보호 창: 앱 실행 후 60초간 전면 금지
4. 일/시간 빈도 제한(Supabase 정책)

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
- 중요: LaunchedEffect(Unit) 키로 중복 구독 방지

### 3) AdController.kt (Supabase 정책)
- is_active & ad_interstitial_enabled가 모두 true일 때만 표시 시도
- 시간당/일일 제한을 SharedPreferences의 타임스탬프와 비교

### 4) InterstitialAdManager.kt
- 내부 쿨다운(60초) + 일일 3회 제한(릴리즈)
- Debug 빌드에서는 내부 제한을 우회(개발/테스트 편의)

---

## 배너/전면 중복 방지

전면광고와 배너광고가 동시에 표시되는 것을 방지하고, 동시에 레이아웃 시프트를 최소화합니다.

### 구현 원리

```kotlin
// AdController.kt
private val _isInterstitialShowing = mutableStateOf(false)

@Composable
fun isInterstitialShowingState(): Boolean = _isInterstitialShowing.value

internal fun setInterstitialShowing(showing: Boolean) {
    _isInterstitialShowing.value = showing
}
```

```kotlin
// AdBanner.kt
val isPolicyEnabled = AdController.isBannerEnabledState()
val isInterstitialShowing = AdController.isInterstitialShowingState()
val shouldShowBanner = isPolicyEnabled && !isInterstitialShowing

// shouldShowBanner=false여도 reserveSpaceWhenDisabled=true이면 고정 높이 컨테이너를 렌더링해 공간을 예약합니다.
```

```kotlin
// BaseScaffold.kt - 배너 컨테이너를 항상 렌더링하여 레이아웃 시프트 방지
Column {
    AdmobBanner(
        modifier = Modifier.fillMaxWidth(),
        reserveSpaceWhenDisabled = true
    )
    HorizontalDivider(...)
    Box(Modifier.weight(1f)) { content() }
    HorizontalDivider(...)
    BottomNavBar(...)
}

// 전면광고 표시 중에는 전체 화면을 검은색으로 덮어 앱 UI를 가립니다.
if (isInterstitialShowing) {
    Box(Modifier.fillMaxSize().background(Color.Black))
}
```

### 동작 흐름

1. 전면광고 표시 시작(`onAdShowedFullScreenContent`) → `setInterstitialShowing(true)` → 배너는 숨기되 컨테이너는 유지 → 레이아웃 시프트 없음
2. 전면광고 닫힘(`onAdDismissedFullScreenContent`) → `setInterstitialShowing(false)` → 배너 다시 표시 (컨테이너 재사용)
3. 표시 실패(`onAdFailedToShowFullScreenContent`) → 상태 복구

### 로그 확인
```
AdController: Interstitial showing state: true
AdmobBanner: banner visible=false h=...
AdController: Interstitial showing state: false
AdmobBanner: banner visible=true h=...
```

---

## 테스트 가이드

### 최초 실행 시나리오
1. 앱 최초 실행 → Start 화면 표시 → 카운트 0/3 (초기 이벤트 스킵)
2. Records 탭 → Start 복귀 → 카운트 1/3
3. 다시 다른 탭 → Start 복귀 → 카운트 2/3
4. 다시 비홈 → 홈 복귀 → 카운트 3/3 → 광고 시도 (레이아웃 시프트 없이 전면 표시)

### 빠른 전환 시나리오
- 비홈↔홈 반복 후 3회 도달 시, 로드/정책 허용되는 최초 시점에 표시
- 표시 시에도 상단 배너 영역 높이는 고정 → 콘텐츠가 위로 슬라이드되는 느낌 없음

- 로그 필터: HomeAdTrigger, AdController, InterstitialAdManager, AdmobBanner

예시 로그
```
HomeAdTrigger: Home visit recorded: 3/3 (source=run)
AdController: ✅ Can show interstitial: ...
InterstitialAdManager: onAdShowedFullScreenContent
```

---

## PocketChord 정책 비교

> 중요: 아래 섹션은 제품/정책 리뷰에 필수입니다. 삭제 금지.
>
> <!-- DO NOT DELETE: PocketChord 비교표는 정책 감사 및 회귀 검증에 필요한 고정 섹션입니다. -->

| 구분 | PocketChord | AlcoholicTimer (현재) | 비고 / 설명 |
|---|---|---|---|
| 트리거 기반 | 화면 전환 패턴 3회(홈/상세 반복 등) | 홈 그룹(시작/진행/종료) "비홈 → 홈" 진입 3회 | 그룹 기반 트리거로 단순·예측 가능 |
| 초기 시간 쿨다운 | 명시 없음 | 사용 안 함 (방문 임계치로 간접 보호) | 초기 강제 노출 방지 취지 동일 |
| 표시 후 재쿨다운 | 약 60초 | 60초 | 체감 빈도 억제 |
| 세션 1회 제한 | 미기재 | 비활성(OFF) | 초기 보호 창(60초)로 대체 |
| 시간당 최대 | ≤ 2회 | Supabase 정책 또는 Fallback 2회 | 원격 조정 가능 |
| 일일 최대 | ≤ 15회 | Supabase 정책 또는 Fallback 10회 | 원격 조정 가능 |
| 배너/전면 중복 | 별도 고려 제한 | 전면 중 배너 자동 숨김 + 컨테이너 유지 | 레이아웃 시프트 방지 |

---

## 결론
- 세션 1회 제한 대신 초기 보호 창(60초) + 60초 재쿨다운 + 시간/일일 상한 + 홈 그룹 3회 트리거로 균형을 맞췄습니다.
- 필요 시 초기 보호 시간은 튜닝 가능하며(예: 30~60초), 원격 정책화로 운영 유연성을 확보하는 것을 권장합니다.

---

문서 버전: 1.9  
최종 수정: 2025-11-14 (초기 보호 60초로 통일)  
작성자: AlcoholicTimer Dev Team
