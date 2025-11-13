# 전면 광고 (Interstitial Ad) 시나리오

## 📋 목차
1. [개요](#개요)
2. [트리거 조건](#트리거-조건)
3. [빈도 제한 정책](#빈도-제한-정책)
4. [초기 경험 보호](#초기-경험-보호)
5. [광고 노출 흐름도](#광고-노출-흐름도)
6. [구현 세부사항](#구현-세부사항)
7. [테스트 가이드](#테스트-가이드)

---

## 개요

AlcoholicTimer 앱의 전면 광고는 사용자 경험을 최대한 보호하면서도 적절한 수익화를 목표로 합니다.

### 주요 특징
- ✅ **홈 화면 방문 기반 트리거**: 시작 버튼 클릭이 아닌 홈 화면 재방문으로 변경
- ✅ **단순 빈도 제한**: 방문 3회 + 시간당/일일 제한 적용
- ✅ **Supabase 원격 제어**: 실시간 광고 정책 조정 가능

---

## 트리거 조건

### 1. 홈 화면 방문 카운트 방식 (현재 적용 중)

```
홈(시작) 화면 진입 → 카운트 증가 → 3회 누적 시 광고 시도
```

#### 카운트 증가 조건
- ✅ 사용자가 홈(시작) 화면을 실제로 본 경우
- ✅ 진행 중 세션으로 자동 리다이렉트되지 않은 경우
- ❌ 광고 정책이 비활성화된 경우
- ❌ (이전) 앱 시작 후 5분 쿨다운: 제거됨

#### 카운트 리셋
- 📅 **일일 리셋**: 자정(00:00)이 지나면 카운트 0으로 초기화
- 🎯 **광고 표시 후**: 임계치 도달 시 광고 시도 후 카운트 0으로 초기화

### 2. 구현 위치

| 위치 | 트리거 시점 | 구현 |
|------|-------------|------|
| `StartActivity.onResume()` | 홈 화면 실제 표시 시 | `HomeAdTrigger.registerHomeVisit(this)` |
| `MainActivity` (Start route) | NavHost에서 Start로 이동 시 | (향후 추가 가능) |

---

## 빈도 제한 정책

### 1. 시간당 제한 (AdController)
```
제어: Supabase 원격 제어 (ad_interstitial_max_per_hour)
Fallback 기본값: 2회 (Supabase 값이 없을 경우)
```
**중요**: Supabase에서 값을 설정하면 그 값이 우선 적용됩니다.

### 2. 일일 제한 (AdController)
```
제어: Supabase 원격 제어 (ad_interstitial_max_per_day)
Fallback 기본값: 10회 (Supabase 값이 없을 경우)
```
**중요**: Supabase에서 값을 설정하면 그 값이 우선 적용됩니다.

### 3. 쿨다운 (InterstitialAdManager)
```
기본값: 마지막 광고 표시 후 2분
상수: DEFAULT_COOLDOWN_MS = 2 * 60 * 1000L
```

### 4. 방문 임계치 (HomeAdTrigger)
```
기본값: 홈 화면 3회 방문 시
상수: VISIT_THRESHOLD = 3
```

### 💡 Fallback(대체값)이란?

**Fallback**은 주 데이터 소스를 사용할 수 없을 때 대신 사용하는 **예비 기본값**입니다.

#### 동작 원리
```kotlin
val maxPerHour = policy?.adInterstitialMaxPerHour ?: 2 // Supabase 값(우선), 없으면 Fallback 2
```

#### 실제 시나리오

| 상황 | Supabase 상태 | 앱이 사용하는 값 | 설명 |
|------|---------------|-----------------|------|
| 정상 | `5` | **5** | ✅ Supabase 값 우선 사용 |
| 데이터 없음 | `null` | **2** (Fallback) | 🔄 대체값으로 동작 |
| 네트워크 오류 | 로드 실패 | **2** (Fallback) | 🔄 대체값으로 동작 |
| 첫 실행 | 아직 로드 안 됨 | **2** (Fallback) | 🔄 대체값으로 동작 |
| 서버 장애 | 접속 불가 | **2** (Fallback) | 🔄 대체값으로 동작 |

#### 왜 Fallback이 필요한가요?

1. **안정성**: 서버 문제가 있어도 앱이 정상 동작
2. **오프라인 대응**: 인터넷이 없어도 기본 기능 유지
3. **초기 실행**: 데이터를 아직 못 받았을 때도 동작
4. **사용자 경험**: 로딩 실패로 앱이 멈추지 않음

#### 한국어로 표현하면
- Fallback = **대체값** / **예비값** / **백업 기본값**
- "Supabase 값이 없으면 대체값 2회 사용"

---

## 초기 경험 보호

현재 버전은 “시간 기반 초기 쿨다운”을 사용하지 않습니다. 대신 다음 간접 보호 요소만 유지됩니다:
1. 첫 세션 즉시 강제 노출 없음 (3회 방문 조건 충족 필요)
2. 인터스티셜 자체 내부 쿨다운(마지막 표시 후 2분, InterstitialAdManager)
3. 일/시간 빈도 제한(Supabase 정책)

이전 버전(5분 보호)은 사용자 초기 탐색 시간이 충분한 앱(교육/리텐션 민감형)에 적합하지만 본 앱에서는 PocketChord와 유사한 빠른 노출 전략을 채택했습니다.

---

## 광고 노출 흐름도

### 전체 플로우

```mermaid
graph TD
    A[사용자가 홈 화면 진입] --> B{AdController 활성?}
    B -->|No| H[프리로드 유지]
    B -->|Yes| C[카운트 +1]
    C --> D{카운트 >= 3?}
    D -->|No| H[프리로드 유지]
    D -->|Yes| E{시간당/일일 제한 OK?}
    E -->|No| H
    E -->|Yes| F{쿨다운(2분) 경과?}
    F -->|No| H
    F -->|Yes| G{광고 로드됨?}
    G -->|No| I[프리로드 시도]
    G -->|Yes| J[전면 광고 표시→카운트 0]
    J --> H
```

### 상태별 동작

| 상태 | 카운트 증가 | 광고 시도 | 프리로드 |
|------|-------------|-----------|----------|
| 초기 쿨다운 중 (0~5분) | ❌ | ❌ | ✅ |
| 일반 방문 (카운트 < 3) | ✅ | ❌ | ✅ |
| 임계치 도달 (카운트 = 3) | ✅ → 0 | ✅ | ✅ |
| 빈도 제한 초과 | ❌ | ❌ | ✅ |

---

## 구현 세부사항

### 핵심 컴포넌트

#### 1. HomeAdTrigger.kt
**역할**: 홈 화면 방문 추적 및 광고 트리거

```kotlin
object HomeAdTrigger {
    // 상수
    private const val VISIT_THRESHOLD = 3
    private const val INITIAL_COOLDOWN_MS = 5 * 60 * 1000L
    
    // 주요 함수
    fun recordAppStart(context: Context)
    fun registerHomeVisit(activity: Activity)
    private fun isInInitialCooldown(context: Context): Boolean
    private fun resetIfDayChanged(context: Context)
}
```

**저장 데이터** (SharedPreferences: `home_ad_trigger_prefs`)
- `home_visits_count`: 현재 방문 카운트 (Int)
- `home_visits_day`: 마지막 리셋 날짜 (String, yyyyMMdd)
- `app_start_time_ms`: 앱 시작 시각 (Long, 밀리초)

#### 2. AdController.kt
**역할**: Supabase 기반 광고 정책 관리

```kotlin
object AdController {
    fun canShowInterstitial(context: Context): Boolean
    fun recordInterstitialShown(context: Context)
    fun isInterstitialEnabled(): Boolean
}
```

**Supabase 연동 필드** (`ad_policy` 테이블)
- `is_active`: 광고 시스템 활성화 여부 (Boolean, Fallback: false)
- `ad_interstitial_enabled`: 전면 광고 활성화 (Boolean, Fallback: false)
- `ad_interstitial_max_per_hour`: 시간당 제한 (Int, **Supabase 우선**, Fallback: 2)
- `ad_interstitial_max_per_day`: 일일 제한 (Int, **Supabase 우선**, Fallback: 10)

**중요**: Supabase에 값이 있으면 그 값을 사용하고, 없거나 로드 실패 시에만 앱의 Fallback 기본값을 사용합니다.

#### 3. InterstitialAdManager.kt
**역할**: 광고 로드 및 표시 관리

```kotlin
object InterstitialAdManager {
    fun preload(context: Context)
    fun isLoaded(): Boolean
    fun maybeShowIfEligible(activity: Activity, onDismiss: () -> Unit): Boolean
    fun resetColdStartGate()
}
```

**정책**
- 콜드 스타트당 1회 제한 (`hasShownThisColdStart`)
- 일일 3회 제한 (`DEFAULT_DAILY_CAP`)
- 2분 쿨다운 (`DEFAULT_COOLDOWN_MS`)

#### 4. AdHelpers.kt
**헬퍼 함수**

```kotlin
// 광고 시도 또는 fallback 실행
fun showOr(activity: Activity, fallback: () -> Unit)

// 프리로드 후 광고 시도
fun preloadThenShowOr(activity: Activity, timeoutMs: Long, fallback: () -> Unit)
```

---

## 설정 및 튜닝

### 1. 방문 임계치 변경

```kotlin
// HomeAdTrigger.kt
private const val VISIT_THRESHOLD = 3  // 기본값: 3회
```

**권장 범위**: 2~5회
- 2회: 더 공격적인 수익화
- 5회: 더 관대한 사용자 경험

### 2. 초기 쿨다운 시간 변경

```kotlin
// HomeAdTrigger.kt
private const val INITIAL_COOLDOWN_MS = 5 * 60 * 1000L  // 기본값: 5분
```

**권장 범위**: 3~10분
- 3분: 빠른 수익화 시작
- 10분: 긴 초기 탐색 시간 제공

### 3. Supabase 원격 제어

**정책 우선순위**: Supabase 값 > 앱 내장 Fallback 값

| 필드 | 용도 | Fallback 기본값 | 권장 범위 | 제어 방식 |
|------|------|--------|-----------|-----------|
| `is_active` | 광고 시스템 전체 ON/OFF | `false` | - | Supabase 우선 |
| `ad_interstitial_enabled` | 전면 광고 활성화 | `false` | - | Supabase 우선 |
| `ad_interstitial_max_per_hour` | 시간당 제한 | `false` | - | Supabase 우선 |
| `ad_interstitial_max_per_day` | 일일 제한 | `10` | - | Supabase 우선 |

**동작 방식**:
```kotlin
// AdController.kt
val maxPerHour = policy?.adInterstitialMaxPerHour ?: 2  // Supabase 값 없으면 2
val maxPerDay = policy?.adInterstitialMaxPerDay ?: 10   // Supabase 값 없으면 10
```

**예시**: Supabase에서 `ad_interstitial_max_per_hour = 5`로 설정하면 앱은 즉시 시간당 5회로 동작합니다.

---

## 테스트 가이드

### 1. 개발 환경 설정

```kotlin
// BuildConfig.DEBUG = true 시
// - 정책 우회 모드 (InterstitialAdManager.isPolicyBypassed())
// - 테스트 광고 단위 ID 자동 사용
```

### 2. 테스트 시나리오

#### A. 초기 쿨다운 테스트
1. 앱 완전 종료
2. 앱 재시작
3. 홈 화면 진입 → 카운트 증가 안 함 확인
4. 5분 대기
5. 홈 화면 재진입 → 카운트 증가 확인

#### B. 방문 카운트 테스트
1. 앱 시작 후 5분 대기
2. 홈 화면 진입 (1회)
3. Run 화면으로 이동 후 백버튼으로 돌아오기
4. 홈 화면 진입 (2회)
5. 다시 반복
6. 홈 화면 진입 (3회) → 광고 시도 확인

#### C. 빈도 제한 테스트
1. Supabase에서 `ad_interstitial_max_per_hour = 1` 설정
2. 광고 1회 표시 후
3. 바로 3회 방문 → 광고 표시 안 됨 확인
4. 1시간 대기
5. 3회 방문 → 광고 표시 확인

#### D. Fallback(대체값) 동작 테스트
1. **Supabase 정상 작동 확인**
   - Supabase에서 `ad_interstitial_max_per_hour = 5` 설정
   - 앱 재시작
   - 로그 확인: "Can show interstitial: X/10 (day), X/5 (hour)" ← 5가 적용됨

2. **Fallback 동작 확인**
   - 네트워크 끄기 (비행기 모드)
   - 앱 완전 종료 후 재시작
   - 로그 확인: "Can show interstitial: X/10 (day), X/2 (hour)" ← Fallback 2가 적용됨
   - 광고는 정상 작동 (Fallback 값으로)

3. **Supabase 복구 확인**
   - 네트워크 다시 켜기
   - 앱 재시작 (또는 AdController.refreshPolicy() 호출)
   - 로그 확인: 다시 Supabase 값(5)으로 동작

### 3. 로그 모니터링

```bash
# Android Studio Logcat 필터
HomeAdTrigger  # 방문 카운트 및 쿨다운
AdController   # 빈도 제한 체크
InterstitialAdManager  # 광고 로드/표시
```

**주요 로그 메시지**
```
HomeAdTrigger: App start time recorded
HomeAdTrigger: Initial cooldown active: 287s remaining
HomeAdTrigger: Home visit recorded: 1/3
HomeAdTrigger: Home visit recorded: 3/3
AdController: ✅ Can show interstitial: 5/10 (day), 1/2 (hour)
InterstitialAdManager: onAdShowedFullScreenContent
```

### 4. 강제 테스트 모드

**빠른 테스트를 위한 임시 값 변경**
```kotlin
// HomeAdTrigger.kt (테스트 후 원복 필수!)
private const val VISIT_THRESHOLD = 1  // 1회만에 광고
private const val INITIAL_COOLDOWN_MS = 10 * 1000L  // 10초 쿨다운
```

---

## 사용자 경험 최적화 전략

### 현재 구현의 장점

✅ **비침습적**: 시작 버튼이 아닌 화면 이동으로 트리거  
✅ **예측 가능**: 3회마다 규칙적으로 표시  
✅ **자연스러운 흐름**: 화면 전환 시점에 표시  

### 개선 여지

🔄 **A/B 테스트 포인트**
- 방문 임계치: 2회 vs 3회 vs 5회
- 초기 쿨다운: 3분 vs 5분 vs 7분
- 시간당 제한: 1회 vs 2회 vs 3회

📊 **수집 권장 지표**
- 광고 노출 빈도 (사용자당 평균)
- 광고 표시 성공률
- 사용자 이탈률 (광고 표시 직후)
- 세션당 방문 횟수

---

## 버전 히스토리

| 버전 | 날짜 | 변경 사항 |
|------|------|-----------|
| 1.0 | 2025-01 | 시작 버튼 클릭 시 광고 표시 (구 버전) |
| 2.0 | 2025-11 | 홈 화면 3회 방문 방식으로 변경 |
| 2.1 | 2025-11 | (이전) 초기 5분 쿨다운 실험 도입 |
| 2.2 | 2025-11 | 초기 5분 시간 기반 쿨다운 제거, 방문 임계치만 유지 |

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `ads/HomeAdTrigger.kt` | 홈 화면 방문 추적 |
| `ads/AdController.kt` | Supabase 정책 관리 |
| `ads/InterstitialAdManager.kt` | 광고 로드/표시 |
| `ads/AdHelpers.kt` | 헬퍼 함수 |
| `MainApplication.kt` | 앱 시작 시각 기록 |
| `feature/start/StartActivity.kt` | 홈 화면 방문 등록 |

---

## FAQ

### Q: 왜 시작 버튼이 아닌 홈 화면 방문 방식으로 변경했나요?
**A**: 시작 버튼은 사용자가 금주를 시작하려는 중요한 순간입니다. 이 시점에 광고를 표시하면 전환율이 떨어지고 사용자 경험이 크게 저하됩니다. 홈 화면 재방문은 상대적으로 덜 중요한 시점이므로 광고 표시에 적합합니다.

### Q: 3회 방문이 적절한가요?
**A**: Pocket Chord 앱의 성공 사례를 참고했습니다. 너무 자주(1~2회)면 짜증나고, 너무 드물게(5회 이상)면 수익이 낮습니다. 3회는 균형점입니다.

### Q: 5분 쿨다운이 충분한가요?
**A**: 평균 사용자가 앱의 주요 기능을 둘러보는 데 3~5분이 소요됩니다. 5분이면 기본 탐색을 마치고 앱 가치를 이해한 후 광고를 보게 됩니다.

### Q: 광고가 너무 많이 표시되는 것 같아요.
**A**: Supabase에서 실시간으로 조정 가능합니다:
```sql
UPDATE ad_policy 
SET ad_interstitial_max_per_hour = 1,
    ad_interstitial_max_per_day = 5
WHERE app_id = 'your_app_id'
```

### Q: 특정 사용자에게만 광고를 끌 수 있나요?
**A**: 현재는 전역 정책만 지원합니다. 향후 프리미엄 구독 기능 추가 시 사용자별 제어가 가능합니다.

### Q: Fallback 값은 어떻게 변경하나요?
**A**: Fallback 값은 앱 코드에 하드코딩되어 있어 앱 업데이트 없이는 변경할 수 없습니다. 하지만 **Supabase에서 값을 설정하면 Fallback 값은 무시되고 Supabase 값이 사용**되므로, 실제 운영에서는 Supabase 제어만으로 충분합니다.

```kotlin
// 코드 수정 시 (앱 업데이트 필요)
val maxPerHour = policy?.adInterstitialMaxPerHour ?: 2  // 2를 변경

// Supabase 제어 (즉시 반영, 권장)
UPDATE ad_policy SET ad_interstitial_max_per_hour = 5
```

### Q: Supabase 연결이 끊기면 어떻게 되나요?
**A**: Supabase 로드 실패 시 자동으로 Fallback 값(시간당 2회, 일일 10회)으로 동작합니다. 사용자는 차이를 느끼지 못하며 앱은 정상 작동합니다.

---

## PocketChord 정책 비교

아래는 PocketChord 앱에서 사용 중인 전면 광고 정책(스크린샷 기준: "로드 완료 · 이전 표시 후 60초 · 화면 전환 3회 · 시간당≤2 · 일일≤15 · 정책 ON")과 현재 AlcoholicTimer 앱 정책의 차이점 비교입니다.

| 구분 | PocketChord | AlcoholicTimer (현재) | 비고 / 설명 |
|---|---|---|---|
| 트리거 기반 | 화면 전환 패턴 3회 (상세↔홈 반복) | 홈(시작) 화면 실제 표시 3회 | PocketChord는 특정 전환 패턴 요구, AlcoholicTimer는 단순 홈 방문 카운트 |
| 초기 시간 쿨다운 | 문서상 명시 없음 | 사용 안 함 (방문 임계치만 적용) | 두 앱 모두 시간 게이트 없음 |
| 표시 후 재쿨다운 | 약 60초 | 120초 (DEFAULT_COOLDOWN_MS) + 콜드 스타트 1회 제한 | AlcoholicTimer가 더 길어 빈도 억제 |
| 시간당 최대 | ≤ 2회 | Supabase 값 또는 Fallback 2회 | 기본 동등, 원격 조정 가능 |
| 일일 최대 | ≤ 15회 | Supabase 값 또는 Fallback 10회 | PocketChord 더 공격적 |
| 정책 플래그 | ad_interstitial_enabled | is_active + ad_interstitial_enabled | AlcoholicTimer는 이중 게이트 |
| 원격 제어 저장소 | Supabase ad_policy | Supabase ad_policy | 동일 |
| Fallback 메커니즘 | 문서 언급 없음 | 시간당2 / 일일10 Fallback 명시 | 네트워크 실패 대비 명확 |
| 방문 카운트 리셋 | 명시 없음 | 자정 기준 자동 리셋 | AlcoholicTimer 유지 정책 |
| 로깅 태그 | tag:AdPolicyRepo, tag:InterstitialAdManager | HomeAdTrigger, AdController, InterstitialAdManager | 추가 태그로 디버깅 가시성↑ |
| 프리로드 타이밍 | 로드 완료 후 조건 충족 시 사용 | 앱 시작(UmpConsent 후) + 표시 직후 재프리로드 | 재프리로드 사이클 문서화 |
| 콜드 스타트 제한 | 문서 비포함 | 첫 세션 1회 제한 | 첫 인상 보호 |
| UX 전략 | 빠른 초기 노출 | 방문 3회 후 노출 | AlcoholicTimer 초기 광폭 억제 |

### 핵심 차이 요약
1. AlcoholicTimer는 시간 기반 초기 쿨다운 없이 방문 기반(3회)만 사용.
2. PocketChord는 일일 한도가 더 높고 재쿨다운이 짧아 수익 극대화 지향.
3. AlcoholicTimer는 Fallback/자정 리셋/콜드 스타트 제한을 명확히 구현.

---

## 결론

현재 구현된 전면 광고 시스템은 다음을 보장합니다:

✅ **사용자 경험 최우선**: 초기 경험 보호 + 비침습적 트리거  
✅ **유연한 제어**: Supabase 원격 조정 가능  
✅ **예측 가능한 빈도**: 3회 방문마다 1회  
✅ **다층 보호**: 시간당/일일/쿨다운 제한  

이 문서는 전면 광고 시스템을 이해하고 관리하기 위한 완전한 가이드입니다.

---

**문서 버전**: 1.0  
**최종 수정**: 2025-11-14 (v2.2, 시간 기반 쿨다운 제거 반영)  
**작성자**: AlcoholicTimer Dev Team
