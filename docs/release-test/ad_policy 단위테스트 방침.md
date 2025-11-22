# ad_policy 단위테스트 방침

개요
- 목적: 광고 정책(AdPolicy) 관련 핵심 로직의 안정성을 단위테스트로 보장하고, 정책 파싱 및 기본 폴백 동작을 문서화하여 회귀를 방지한다.
- 범위: AdManager, AdController, AdPolicyRepository의 비즈니스 로직(쿨다운, 시간창 카운트, 정책 플래그 적용, 정책 파싱/선택/폴백)과 관련된 JVM 단위테스트

주요 변경 내용(요약)
- P6(Default Fallback Policy) 명세를 코드에 반영:
  - AdPolicy.DEFAULT_FALLBACK 추가 — 핵심 값: isActive=true, ad_app_open_enabled=true, ad_interstitial_enabled=true, ad_banner_enabled=true, ad_interstitial_max_per_day=5, app_open_cooldown_seconds=300, app_open_max_per_hour=1
- AdPolicyRepository 리팩토링:
  - parsePolicyFromJson(jsonText, appId)로 파싱 로직 분리
  - Fetcher 인터페이스(네트워크 응답 주입) 유지
  - 파싱 실패/빈 응답/오류 시 AdPolicy.DEFAULT_FALLBACK 반환
  - android.util.Log 호출을 JVM 테스트에서 예외를 던지지 않도록 안전 래핑
- AdsCombinedTest 보강:
  - P6 관련 테스트를 DEFAULT_FALLBACK 기대값으로 수정/추가
  - P7(app_id 기준 선택) 테스트 유지
- AdManager: policy.is_active 검사 보강 — 전체 비활성화 시 모든 광고 차단

테스트 대상 및 항목
- AdManager
  - 앱오프닝 쿨다운(app_open cooldown)
  - 시간창(hour/day) 초기화 및 카운트 무결성
  - 인터스티셜 카운트 상한 처리
  - 정책 전체 비활성 처리(P1)
- AdController
  - full-screen 플래그 리스너 동작(초기 상태 전달 및 변경 알림)
  - interstitial 표시 플래그 토글
- AdPolicyRepository
  - JSON 파싱 안정성 (빈/잘못된 JSON -> DEFAULT_FALLBACK) (P6)
  - 다중 정책 응답에서 app_id 기반 선택 (P7)
  - Fetcher 주입을 통한 네트워크 시나리오 모킹

중요 테스트 항목 (P1..P7)
- P1: 정책 전체 활성/비활성 인식
  - 목표: AdPolicy의 is_active 플래그가 false일 때 모든 광고 유형(banner, interstitial, app_open)이 즉시 차단되는지 확인
  - 전제조건: 저장된 정책 또는 parse 결과에 is_active=false 설정
  - 테스트 시나리오:
    1) DEFAULT_FALLBACK이 아닌 정책에서 is_active=false로 설정된 정책을 로드
    2) AdManager.canShowAd(ALL_TYPES)을 호출하여 모든 광고 타입에 대해 false 반환 확인
    3) 광고 노출 카운트/쿨다운 로직이 실행되지 않음을 보장
  - 기대 결과: canShowAd는 모든 타입에 대해 false를 반환하고 incrementAdCount 등 카운트 관련 함수는 동작하지 않음
  - 관련 테스트명: policy_isActive_false_disables_all_ad_types

- P2: 개별 광고 타입별 플래그 적용
  - 목표: ad_banner_enabled, ad_interstitial_enabled, ad_app_open_enabled 플래그가 각 광고 타입의 표시 여부에만 영향을 미치는지 확인
  - 전제조건: 정책이 로드되어 있고 각 플래그가 혼합된 상태(예: banner=false, interstitial=true, app_open=true)
  - 테스트 시나리오:
    1) 정책을 주입한 상태에서 각 광고 타입별로 canShowAd(type)을 호출
    2) 플래그가 false인 타입에 대해서는 false, true인 타입에 대해서는 추가 조건(쿨다운/상한)에 따라 결정됨
  - 기대 결과: 타입별 플래그가 우선 적용되며, 플래그 false인 타입은 항상 차단됨
  - 관련 테스트명: policy_type_flag_disables_only_specific_type

- P3: 앱오프닝(app_open) 쿨다운 동작
  - 목표: app_open 노출 후 설정된 app_open_cooldown_seconds 동안 재노출이 차단되는지 확인
  - 전제조건: 정책의 app_open_cooldown_seconds 값이 정의되어 있고 app_open_enabled=true
  - 테스트 시나리오:
    1) 초기 상태에서 app_open 광고 노출 허용 확인
    2) incrementAdCount(APP_OPEN) 호출로 노출 기록
    3) cooldown 미만의 시간 경과를 시뮬레이션하여 canShowAd(APP_OPEN)이 false인지 확인
    4) cooldown 초과 후 canShowAd(APP_OPEN)이 true로 변경되는지 확인
  - 기대 결과: 쿨다운 내 재노출 차단, 쿨다운 경과 후 허용
  - 관련 테스트명: AppOpen_Cooldown_Success, AppOpen_Cooldown_Failure

- P4: 시간창(hour/day) 초기화 및 카운트 무결성
  - 목표: 시간 기반 윈도우(hour/day)가 변경되면 카운트가 초기화되고, 카운트 증가/저장이 정확히 동작하는지 확인
  - 전제조건: 인터스티셜/기타 광고의 시간창 최대값(예: hour/day 기준)이 정의되어 있음
  - 테스트 시나리오:
    1) 특정 시점에서 카운트를 증가시켜 윈도우 내 한도에 근접시키기
    2) 시간창 경계(다른 hour/day)로 시뮬레이션하여 카운트 초기화 확인
    3) 초기화 후 카운트가 0으로 시작하여 정상 증가하는지 확인
  - 기대 결과: 윈도우 경계에서 카운트가 초기화되고 이후 카운트 로직이 정상 작동
  - 관련 테스트명: Count_Integrity_Check, Interstitial_Cooldown_Override

- P5: 전면/풀스크린 플래그와 리스너 동작
  - 목표: AdController의 full-screen 플래그가 광고 표시 상태를 정확히 반영하고 리스너가 즉시 및 변경 시 알림을 받는지 확인
  - 전제조건: AdController 인스턴스와 리스너 등록 메커니즘이 존재
  - 테스트 시나리오:
    1) 리스너 등록 시 현재 full-screen 상태가 즉시 콜백되는지 확인
    2) setFullScreenAdShowing(true) 호출 후 리스너가 변경 알림을 받는지 확인
    3) setFullScreenAdShowing(false) 호출로 원상 복구 및 알림 확인
  - 기대 결과: 리스너는 등록 즉시 현재 상태를 수신하고 상태 변경 시마다 알림을 수신
  - 관련 테스트명: fullScreenListener_receives_initial_and_changes, interstitialFlag_toggles

- P6: 정책 파싱 안전성 (빈/잘못된 응답 -> DEFAULT_FALLBACK)
  - 목표: 서버에서 빈 바디, 잘못된 JSON, 예외 발생 등의 경우 AdPolicyRepository가 안전한 기본 폴백(AdPolicy.DEFAULT_FALLBACK)을 반환하여 앱이 안전하게 동작하는지 확인
  - 전제조건: Fetcher 모킹을 통해 다양한 응답(빈 바디, 무효 JSON, 다중 항목 등)을 주입 가능
  - 테스트 시나리오:
    1) 빈 응답/빈 바디를 반환하는 Fetcher를 주입하여 getPolicy 호출
    2) 잘못된 JSON을 반환하는 Fetcher로 getPolicy 호출
    3) parsePolicyFromJson에 배열 또는 후보가 없을 때의 동작 검증
  - 기대 결과: 모든 실패 케이스에서 AdPolicy.DEFAULT_FALLBACK이 반환되며, 반환된 정책의 핵심 값(쿨다운 300초, interstitial 일일 5건 등)을 테스트로 검증
  - 관련 테스트명: parsePolicy_empty_or_invalid_returns_default_fallback, repository_fetcher_empty_body_returns_default_fallback

- P7: app_id 기반 정책 선택 정확성
  - 목표: 응답에 다수의 정책 항목이 있을 때 app_id(또는 패키지 기반 매칭 규칙)에 따라 올바른 정책을 선택하는지 확인
  - 전제조건: 서버 응답에 후보 정책들이 포함된 JSON(복수 항목)을 주입할 수 있음
  - 테스트 시나리오:
    1) 후보 목록에 pkgBase, simpleName 등 다양한 형태의 app 식별자가 포함된 JSON을 생성
    2) parsePolicyFromJson(json, appId)를 호출하여 선택된 정책 검증
    3) 우선순위 매칭(정확한 appId > pkgBase > simpleName 등)이 올바르게 적용되는지 확인
  - 기대 결과: 지정한 앱 식별자 기준으로 가장 적절한 정책을 선택하고 반환
  - 관련 테스트명: parsePolicy_selects_matching_app_id

실행 방법
- 로컬 JVM 단위테스트 실행:
  - 프로젝트 루트에서: ./gradlew :app:testDebugUnitTest
- 리포트: app/build/reports/tests/testDebugUnitTest/index.html
- 통과 기준: 실패 0

단위테스트 결과
- 현재 실행 결과: 13 tests, 실패 0 — 모두 통과
- 관련 리포트 경로: app/build/reports/tests/testDebugUnitTest/index.html

해결된 초기 문제 요약
- parse 함수가 null을 반환하던 케이스를 기본 폴백 반환으로 수정
- JVM 단위테스트 환경에서 android.util.Log 호출로 인한 예외를 안전하게 처리

보장되는 동작 (P6 관련)
- 서버 응답이 빈 바디이거나 파싱 오류 등으로 정책을 읽지 못할 때 AdPolicyRepository는 안전한 DEFAULT_FALLBACK을 반환하여 광고 관련 로직이 예측 가능하게 동작함을 보장함.
- 단위테스트는 DEFAULT_FALLBACK의 핵심 필드(쿨다운 300초, interstitial 일일 5건, 시간당 app_open 1건 등)를 검증함.

단위 테스트 통과의 의미 — 상세 설명
- 검증 범위와 신뢰도
  - 보장되는 것:
    - 문서에 명시된 단위 로직(AdManager의 카운트/윈도우/쿨다운, AdController의 플래그/리스너 동작, AdPolicyRepository의 파싱/선택/폴백)은 변경 후에도 정의된 동작을 유지함을 반복적으로 검증 가능.
    - 파싱 실패/빈 응답과 같은 서버 사이드 비정상 케이스에서 앱이 안전한 기본 정책(DEFAULT_FALLBACK)을 사용하여 과도한 광고 노출 또는 크래시를 방지함.
    - 테스트에 포함된 특정 수치(예: app_open 쿨다운 300초, interstitial 일일 상한 5건)는 코드에서 의도한 대로 적용됨을 확인함.
  - 보장하지 않는 것(한계):
    - 통합·런타임 문제: AdMob SDK 동작, 네이티브 SDK 버전 이슈, Android 플랫폼/기기별 런타임 이슈는 단위테스트로 보장되지 않음.
    - 멀티스레드·동시성: 실제 앱의 동시성 경로에서 발생할 수 있는 경쟁 조건은 단위테스트로 모두 포착되지 않을 수 있음.
    - 네트워크 환경 특이점: 대규모 페이로드, 네트워크 지연/타임아웃, HTTP 5xx 정책에 따른 서버 정책 변화는 추가 테스트(통합/계측) 필요.

- 위험 감지 및 회귀 방지
  - 통과는 로직 레벨의 회귀 가능성을 크게 줄여 줌. 이후 코드 변경 시 AdsCombinedTest와 관련 단위테스트가 실패하면 즉시 회귀로 판단 가능.
  - CI에 단위테스트를 배치하면 PR 단계에서 자동으로 회귀를 차단할 수 있어 안정성 확보에 효과적임.

- 운영(릴리즈) 관점에서의 의미
  - 통과는 '기능적 안전성(functional safety)'을 어느 수준 보장하지만, 실제 릴리즈 전에는 다음을 권장:
    1) CI에서 모든 단위테스트 자동 실행 및 실패 시 배포 차단 설정
    2) Robolectric 또는 instrumentation 테스트로 UI/SDK 통합 검증 수행
    3) 스모크 테스트(릴리스 후보 빌드)로 실제 디바이스에서 핵심 시나리오 검증
    4) 변경 로그에 DEFAULT_FALLBACK 관련 수정 사항 명시

- 문제가 발생했을 때의 대응 지침
  - 단위테스트 실패: 변경된 코드와 테스트를 우선 비교하여 의도된 변경인지 확인. 의도되지 않은 회귀라면 변경을 롤백하거나 테스트를 수정할 수 없음(테스트가 올바르다면 코드 수정 필요).
  - CI 통과 후 런타임 이슈 발생: 로그/리포트(예: Firebase Crashlytics)와 단위테스트 입력 케이스를 대조하여 재현 테스트 케이스 추가 후 수정.

- 권장 프로세스 요약
  - PR마다 :app:testDebugUnitTest 실행(필수)
  - 주요 정책/쿨다운/상한 값을 변경할 때 관련 단위테스트도 함께 갱신
  - 통합/계측 테스트를 별도로 운영하여 런타임·UI·네트워크 이슈를 보완

문의/지시
- CI 워크플로 추가 또는 추가 엣지케이스 테스트 작성 중 원하시는 작업을 선택해 주세요.
