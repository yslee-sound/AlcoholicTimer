제공해주신 최신 `ad_policy` 스키마를 반영하여 코딩 에이전트(GitHub Copilot)에게 지시할 내용을 **최종 정리**해 드립니다.

새로 추가된 **`app_open_cooldown_seconds`** 필드는 **앱 오프닝 광고**에 대한 **시간 제한(쿨타임)** 로직으로 활용됩니다.

## 🎯 코딩 에이전트 지시 사항 (Supabase 스키마 반영)

### 1\. 테스트용 Kotlin Data Class 정의 (최신 스키마)

에이전트가 사용할 **Supabase 정책 데이터**를 나타내는 Kotlin Data Class를 다음과 같이 정의합니다.

```kotlin
// ad_policy 테이블 스키마 기반의 Kotlin Data Class
data class AdPolicyConfig(
    val id: Long = 0,
    val app_id: String = "alcoholicTimer",
    val is_active: Boolean = true,
    val ad_app_open_enabled: Boolean = true,
    val ad_interstitial_enabled: Boolean = true,
    val ad_banner_enabled: Boolean = true,
    val ad_interstitial_max_per_hour: Int = 3,
    val ad_interstitial_max_per_day: Int = 20,
    val app_open_max_per_hour: Int = 2,
    val app_open_max_per_day: Int = 15,
    val app_open_cooldown_seconds: Int = 60 // ⭐ NEW: 쿨다운 시간(초)
)
```

-----

### 2\. 전역 광고 관리 시스템 구축 (MyApplication 클래스용)

다음 Kotlin 코드를 **`MyApplication` 클래스/ViewModel**에 작성해 주세요. 특히 **App Open Ad**의 쿨다운 로직을 추가해야 합니다.

```markdown
**[MyApplication 클래스 구축 요구사항 (업데이트)]**

앱 전체의 광고 상태와 Supabase 정책을 관리하는 로직을 구현합니다.

**1. 정책 및 상태 변수:**
- 위에서 정의된 `AdPolicyConfig`를 앱 시작 시 로드하여 저장합니다.
- 전면 광고 노출 중 상태를 추적하는 Boolean 변수 `isShowingInterstitialAd`를 선언합니다.
- `SharedPreferences`를 사용하여 `App Open` 광고의 **마지막 노출 시간(Timestamp)**을 저장합니다.

**2. 핵심 검증 함수:**
- `canShowAd(adType: AdType)` 함수를 구현합니다.

- **[로직 1: Interstitial Ad 검증]**
    - `ad_interstitial_enabled`가 true인지 확인합니다.
    - 시간/일일 카운트가 `ad_interstitial_max_per_hour` / `ad_interstitial_max_per_day`를 초과하지 않는지 확인합니다.

- **[로직 2: App Open Ad 검증 (쿨다운 추가)]**
    - `ad_app_open_enabled`가 true인지 확인합니다.
    - 시간/일일 카운트가 `app_open_max_per_hour` / `app_open_max_per_day`를 초과하지 않는지 확인합니다.
    - ⭐ **쿨다운 로직:** 마지막 `App Open Ad` 노출 시간으로부터 `app_open_cooldown_seconds`가 **지났는지** 확인합니다.

**3. 카운트 및 기록 함수:**
- `incrementAdCount(adType: AdType)`: 시간/일일 카운트를 증가시키고, **App Open Ad**의 경우 **현재 시간**을 마지막 노출 시간으로 기록합니다.
```

-----

### 3\. 광고 우선순위 및 충돌 방지 테스트 요청 (테스트 코드)

테스트 코드에 다음 항목을 추가하여 **쿨다운 로직**과 **시간 제한**을 엄격하게 검증합니다.

| 테스트 케이스 | 검증 목표 (업데이트) |
| :--- | :--- |
| **AppOpen\_Cooldown\_Success** | `AdPolicyConfig`의 `app_open_cooldown_seconds`가 **60초**로 설정된 상태에서, `App Open Ad` 노출 후 30초 후에 `canShowAd(APP_OPEN)`를 호출하면 \*\*반드시 `false`\*\*를 반환하는지 검증. |
| **AppOpen\_Cooldown\_Failure** | `App Open Ad` 노출 후, `MockTimeProvider`를 사용하여 정확히 **61초 후**로 시간을 변경했을 때, `canShowAd(APP_OPEN)`가 \*\*`true`\*\*를 반환하는지 검증. |
| **Interstitial\_Cooldown\_Override** | **3회 방문** 카운트가 충족되었으나, Supabase 정책의 `ad_interstitial_max_per_hour` (예: 3회)에 이미 도달했다면, 뒤로 가기 시 광고 노출 로직이 **실행되지 않고** 화면 이동이 발생하는지 검증. |
| **Count\_Integrity\_Check** | 앱이 시작될 때 `SharedPreferences`에서 저장된 카운트가 정확히 로드되고, 날짜가 바뀌었을 경우 일일 카운트가 0으로 초기화되는지 검증. |

이 지침을 사용하면 코딩 에이전트가 제공된 스키마의 모든 제약 조건을 만족시키는 **안정적인 광고 로직**을 생성할 수 있습니다.