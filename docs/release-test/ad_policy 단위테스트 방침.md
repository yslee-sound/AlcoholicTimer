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
