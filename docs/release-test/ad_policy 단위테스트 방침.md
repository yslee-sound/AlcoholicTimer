# ad_policy 단위테스트 방침

개요
- 목적: 광고 정책(AdPolicy) 관련 핵심 로직의 안정성을 단위테스트로 보장하고, 정책 파싱 및 기본 폴백 동작을 문서화하여 회귀를 방지한다.
- 범위: AdManager, AdController, AdPolicyRepository의 비즈니스 로직(쿨다운, 시간창 카운트, 정책 플래그 적용, 정책 파싱/선택/폴백)과 관련된 JVM 단위테스트

핵심 변경 및 현재 상태(요약)
- AdPolicy.DEFAULT_FALLBACK 추가 및 명세 반영
  - 기본 폴백은 앱이 안전하게 동작하도록 보장하는 정책으로 정의됨(예: isActive=true, ad_app_open_enabled=true, ad_interstitial_enabled=true, ad_banner_enabled=true, ad_interstitial_max_per_day=5, app_open_cooldown_seconds=300, app_open_max_per_hour=1 등).
- AdPolicyRepository
  - parsePolicyFromJson(jsonText, appId)로 파싱 로직 분리 및 실패 시 DEFAULT_FALLBACK 반환하도록 보장.
  - 네트워크 Fetcher 주입으로 다양한 응답 시나리오를 테스트 가능하게 구성.
  - android.util.Log 호출은 JVM 단위테스트 환경에서 예외를 발생시키지 않도록 안전하게 래핑함.
- AdsCombinedTest 업데이트
  - P6(기본 폴백) 관련 테스트를 포함하여 파싱 실패/빈 응답 시 DEFAULT_FALLBACK 반환을 검증.

중요 테스트 항목 요약 (P1..P7)
- P1: 정책 전체 활성/비활성 인식 — is_active=false 시 모든 광고 차단 확인
- P2: 광고 타입별 플래그(ad_banner_enabled, ad_interstitial_enabled, ad_app_open_enabled) 동작 확인
- P3: app_open 쿨다운 동작 검증
- P4: 시간창(hour/day) 초기화 및 카운트 무결성 검증
- P5: full-screen 플래그 및 리스너 동작 검증
- P6: 정책 파싱 안전성(빈/잘못된 응답 -> DEFAULT_FALLBACK 반환)
- P7: app_id 기반 정책 선택 정확성

실행 방법
- 로컬 JVM 단위테스트 실행:
  - Windows: .\\gradlew.bat :app:testDebugUnitTest
  - 특정 테스트만 실행: .\\gradlew.bat :app:test --tests "kr.sweetapps.alcoholictimer.ads.AdsCombinedTest.*"
- 리포트 위치: app/build/reports/tests/testDebugUnitTest/index.html

현재 로컬 결과
- AdsCombinedTest 및 관련 단위테스트가 로컬에서 실행되어 모두 통과하였음(모든 실패 0).
- DEFAULT_FALLBACK 동작과 주요 카운트/쿨다운 값들이 테스트에서 검증됨.

권장 프로세스
1) PR마다 :app:testDebugUnitTest 자동 실행을 요구하여 정책 로직 회귀를 방지한다.
2) 통합 테스트(예: MockWebServer)를 통해 서버 응답 파싱과 app_id 매칭 로직을 검증한다.
3) Instrumentation/통합(디바이스) 테스트로 광고 SDK·UI 통합을 확인한다.
4) 릴리즈 전 verifyReleaseAdConfig 또는 assembleRelease로 릴리즈 검증을 수행한다.

주의 사항 및 한계
- 단위테스트는 로직 레벨의 안정성을 보장하지만, 실제 AdMob SDK 동작·네트워크 이슈·기기별 런타임 버그는 별도 통합/계측 테스트로 검증해야 한다.

문의/추가 작업
- CI에 단위테스트를 배치(예: GitHub Actions)하거나, 추가 통합 케이스(페이로드, 다중 정책 응답 등)를 추가하려면 지시해 주세요.

## 서버 제어 항목(=Supabase) 로컬 단위테스트 가이드

많은 광고 정책 항목이 Supabase에서 내려오는 값으로 동작을 제어하므로, 모든 조합을 서버에서 직접 실험하기 어려워 로컬 단위테스트/통합 테스트에서 "가짜 데이터(모킹)"를 사용하여 검증해야 합니다. 아래는 권장 방법 요약입니다.

- 핵심 원칙
  - 서버 응답을 흉내내는 Fake/Mock 객체를 사용해 정책 값을 테스트에 주입한다.
  - 서버가 제어하는 수치(쿨다운, 시간창 제한, 활성화/비활성화 플래그 등)는 테스트 전용 정책으로 고정해 검증한다.
  - 실제 네트워크를 사용하지 않는 JVM 단위테스트는 빠르고 안정적이며 CI에 적합하다.

- 사용 가능한 방법들 (우선순위 권장)
  1. AdController.setPolicyForTest(policy)
     - 가장 간단하고 빠른 방법입니다. 테스트에서 원하는 AdPolicy 객체를 만들어 setPolicyForTest로 주입하면 canShowInterstitialNow(), canShowAppOpen() 등 컨텍스트-프리 API로 바로 검증할 수 있습니다.
     - 예: 테스트용 permissive/strict 정책을 만들어 경계 케이스와 쿨다운 동작을 확인합니다.

  2. MockPolicyRepository / MockSharedPreferences
     - 기존 Popup 테스트에서 사용중인 MockPolicyRepository 패턴을 재사용합니다(코드베이스에 MockPolicyRepository 구현이 존재합니다).
     - initialize(context) 흐름을 검증하거나 AdPolicyRepository 파싱 로직을 직접 테스트할 때 유용합니다.

  3. MockWebServer (통합 수준)
     - 정책 JSON 페이로드를 그대로 돌려주는 테스트 서버를 띄워 AdPolicyRepository의 네트워크 흐름과 parsePolicyFromJson 로직을 통합 검증합니다.
     - 네트워크 예외/잘못된 페이로드 시 DEFAULT_FALLBACK 동작을 검증할 때 권장합니다.

- 테스트 작성 팁
  - 시간/윈도우 의존성은 MockTimeProvider 또는 카운터와 SharedPreferences를 초기화해서 통제하세요.
  - reserveInterstitialSlot / unreserveInterstitialSlot 같은 상태 변화는 단위테스트에서 직접 호출해 상태 일관성을 검증합니다.
  - 통합 시나리오(I1: 팝업 활성 중 광고 노출 거부)는 PopupManager의 모킹된 동작과 AdController.setFullScreenAdShowing(true) 호출을 조합하여 테스트합니다.

- 실행 예시
  - 전체 단위테스트: .\\gradlew.bat :app:testDebugUnitTest
  - 특정 테스트: .\\gradlew.bat :app:test --tests "kr.sweetapps.alcoholictimer.ads.SomeAdPolicyTest.*"

- 권장 디렉터리 및 파일 예시
  - app/src/test/java/... 에 MockPolicyRepository.kt, MockSharedPreferences.kt, MockTimeProvider.kt를 배치해 재사용합니다.
  - AdPolicyRepository의 parsePolicyFromJson(json, appId) 함수를 직접 단위테스트로 검증합니다.
