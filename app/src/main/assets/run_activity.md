# RunActivity 업데이트 명세 (Settings 연동 버전)

## 0) 구현 원칙

* **Jetpack Compose** 기반, 미니멀/직관형 UX 유지
* 계산·표시 로직은 **범주형 설정값(코드) → 내부 매핑값**을 통해 파생
* 저장/리셋 이벤트에 **즉시 반영** (Settings → RunActivity)

---

## 1) 변경 요약 (TL;DR)

* **중앙 메인 지표**: 클릭 시 순환 표시는 **금주 일수 → 진행 시간 → 현재 레벨 → 절약한 금액 → 절약한 시간 → 기대 수명** 순서(Loop).&#x20;
* **계산 근거**: 비용·시간·수명은 **설정 화면의 범주 선택값**을 내부 매핑값으로 변환해 산출. 숫자 직접 저장 없음.&#x20;
* **레벨/배경색**: 기존 레벨표 그대로 사용, **연속 금주일수**에 따라 배경색 자동 변경.&#x20;
* **애니메이션**: 중앙 지표 전환은 **애니메이션 없음**으로 통일(설정 스펙 준수).&#x20;

---

## 2) 데이터 연동 규칙

### 2.1 Settings → RunActivity 전달 모델

```kotlin
data class SettingsCategory(
    val cost: CostCategory,          // 저/중/고 (코드 저장)
    val freq: FreqCategory,          // 주 1회 이하 / 주 2~3회 / 주 4회 이상
    val drinkDuration: DrinkDuration,// 2h 이하 / 3~5h / 6h+
    val hangover: HangoverDuration,  // 0h / 4~6h / 7h+
    val lifeRule: LifeRule           // 30→+1 / 30→+2 / 60→+1
)
```

> 저장되는 것은 **범주 코드**이며, RunActivity에서 내부 매핑값으로 변환해 계산합니다. (비개인정보 설계)&#x20;

### 2.2 내부 매핑값 (계산용, 예시)

* 비용(원): 저=10,000 / 중=40,000 / 고=70,000
* 빈도(주당 횟수): 1 / 2.5 / 5
* 음주시간(h): 2 / 4 / 6
* 숙취시간(h): 0 / 5 / 8
* 기대수명 규칙: `30일→+1`, `30일→+2`, `60일→+1` (일수/증가일수)
  위 매핑은 **settings\_activity.md의 표준 매핑**을 그대로 사용합니다.&#x20;

---

## 3) 중앙 메인 지표 사양

### 3.1 표출 순서/전환

1. **금주 일수** (예: `1일 13시간`)
2. **진행 시간** (예: `13:25` - 시:분 형식)
3. **현재 레벨** (예: `작심 7일`)
4. **절약한 금액** (예: `1,250,000원`)
5. **절약한 시간** (예: `150시간`)
6. **기대 수명** (예: `25일`)

* 중앙 영역 **탭/클릭 시 순환(Loop)**, **전환 애니메이션 없음**.&#x20;
* 폰트는 레벨별 배경과 **최대 대비** 색상 자동 적용.

### 3.2 계산식 (RunActivity 측)

```kotlin
val days = abstainDays               // 연속 금주일수(일 단위)
val weeks = days / 7.0

val savedMoney = weeks * freqVal * costVal
val savedHours = weeks * freqVal * (drinkHoursVal + hangoverHoursVal)

val lifeGainDays = when(lifeRule) {
  LifeRule.D_30_ADD_1 -> (days / 30.0) * 1.0
  LifeRule.D_30_ADD_2 -> (days / 30.0) * 2.0
  LifeRule.D_60_ADD_1 -> (days / 60.0) * 1.0
}.roundToInt()
```

> 공식은 **settings\_activity.md의 연동 로직**(금액/시간/수명 계산 방식)과 일치해야 합니다.&#x20;

---

## 4) 상단/진행 상태/배경색

* **상단 좌**: 목표일(예: `목표: 100일`)
* **상단 중**: 현재 레벨명
* **상단 우**: 경과 시간(시:분)
* **진행 바**: `(금주일수 / 목표일수) × 100%` (레벨 계열의 밝은 톤)
* **배경색**: 레벨 테이블에 따라 **연속 금주일수**로 실시간 결정(예: Gray→Yellow→…→Gold). 레벨 정의는 기존 문서(level\_activity.md) 그대로 사용.&#x20;

---

## 5) 상태/아키텍처

```kotlin
@HiltViewModel
class RunViewModel @Inject constructor(
  private val settingsRepo: SettingsRepository,  // 범주 코드 저장소
  private val levelResolver: LevelResolver       // 일수→레벨/색상
) : ViewModel() {

  val uiState: StateFlow<RunUiState> = combine(
      settingsRepo.observeCategories(),          // SettingsCategory(코드)
      abstainTracker.observeDays()               // 연속 금주일수
  ) { cat, days ->
      val map = cat.toMappedValues()             // 내부 매핑값 변환 (Run 쪽)
      val metrics = calcMetrics(days, map)
      RunUiState(
         days = days,
         level = levelResolver.resolve(days),
         bgColor = levelResolver.color(days),
         metrics = metrics
      )
  }.stateIn(...)
}
```

* **Settings 저장/리셋 시**: `observeCategories()`가 갱신 → Run 화면 **즉시 반영**.&#x20;
* **��입 우선순위**: 사용자 선택 > 저장값 > 기본값(중/주2\~3회/보통/보통/30→+1).&#x20;

---

## 6) Compose UI 골격 (요약)

```kotlin
@Composable
fun RunScreen(state: RunUiState, onMainTap: () -> Unit) {
  Box(
    Modifier
      .fillMaxSize()
      .background(state.bgColor)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {

    TopBar(
      goalDays = state.goalDays, 
      levelName = state.level.name, 
      elapsed = state.elapsedTimeText
    )

    Column(
      Modifier.fillMaxSize(), 
      verticalArrangement = Arrangement.Center, 
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      MainMetric(
        metric = state.currentMetric,   // days / money / hours / life
        onClick = onMainTap,            // 클릭 시 순환
        // 애니메이션 없음 (스펙 준수)
      )
      ProgressBar(progress = state.progress) // (days / goalDays)
    }

    BottomControls(
      onPause = { /* 금주 중단 기록 */ },
      onResume = { /* 새 기록 시작 */ }
    )
  }
}
```

---

## 7) 포맷/단위 표기 규칙

* **금액**: 원화 단위, 천단위 구분(예: `1,250,000원`)
* **시간**: 정수 시간 기준 표기(0.5h 등 소수점 발생 시 반올림)
* **기대 수명**: `+X일` 형태, 계산 결과 0 이하면 `+0일`
* **금주 일수**: `N일 HH시간` (예: `1일 13시간`)

---

## 8) QA 체크리스트

* [ ] Settings에서 **범주 코드를 바꿀 때마다** Run 화면 값이 즉시 갱신되는가? 갱신됨
* [ ] 중앙 지표 클릭 순서가 **사양과 동일(일수→금액→시간→수명)** 인가? 그렇다;
* [ ] **애니메이션 없음** 규칙이 지켜지는가? (페이드/스케일 미사용) 그렇다;
* [ ] 레벨/배경색이 **연속 금주일수**에 정확히 연동되는가? 그렇다;
* [ ] 리셋 시 기본 범주가 적용되고, 메인 화면이 즉시 반영되는가? 그렇다;

---
