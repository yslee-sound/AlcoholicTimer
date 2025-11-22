# ad_policy 단위테스트 방침

목적
- 앱 광고 관련 핵심 로직(서버 정책 반영, 앱오프닝 쿨다운, 카운트/윈도우 관리, 전면/풀스크린 플래그)이 자동화된 단위테스트로 검증되도록 정책을 정의한다.

테스트 위치(현재 구현)
- 통합 단위테스트 파일: app/src/test/java/kr/sweetapps/alcoholictimer/ads/AdsCombinedTest.kt
  - 이전의 개별 테스트들을 병합하여 광고 영역 관련 로직을 한곳에서 관리함.

테스트 대상 요약
- AdManager(패키지: com.alcoholictimer.ad)
  - AppOpen 쿨다운: incrementAdCount 후 cooldown 기간 내(canShowAd) 동작 검증
  - 시간창(hour/day) 초기화: 이전 날짜 데이터가 있으면 새로운 인스턴스에서 카운트가 초기화되는지 검증
  - 인터스티셜 카운트 상한 처리: 시간당 최대치 도달 시 canShowAd가 false를 반환하는지 검증

- AdController(패키지: kr.sweetapps.alcoholictimer.ads)
  - full-screen 플래그 리스너: addFullScreenShowListener 등록 시 현재 상태를 즉시 전달하고, setFullScreenAdShowing 변경이 리스너로 전달되는지 검증
  - interstitial 표시 플래그 토글: setInterstitialShowing / isInterstitialShowingNow 동작 검증

- Release/빌드 설정 관련 검사
  - BuildConfig.DEBUG, ADMOB_BANNER_UNIT_ID, ADMOB_INTERSTITIAL_UNIT_ID 기본 체크(테스트 환경에 따라 일부 검사 스킵)

실행 방법
- 로컬 실행(단일 모듈, JVM 단위테스트):
  - Windows PowerShell / CMD에서 프로젝트 루트에서 실행:
    - ./gradlew :app:testDebugUnitTest
- 테스트 리포트 위치: app/build/reports/tests/testDebugUnitTest/index.html
  - 통과 기준: 해당 리포트에서 실패(failures) 0

유의 사항 및 유지보수
- 이 문서에 명시된 단위테스트는 논리 로직(비즈니스 규칙)에 초점을 둠. UI/Android 프레임워크 종속 동작(Compose/AndroidView, Ad SDK 동작)은 Robolectric 또는 instrumentation 테스트로 보완할 것.
- 광고/정책 관련 로직 변경 시(AdController, AdManager, AdPolicyRepository 등) 반드시 AdsCombinedTest를 갱신하여 회귀를 방지할 것.
- CI 통합 권장: PR마다 :app:testDebugUnitTest가 실행되도록 워크플로에 추가.
- 테스트를 세분화하려면 통합 파일을 분리하되 이름/패키지 네이밍 규칙을 유지하여 관리하기 쉽게 유지할 것.

변경 이력
- 통합 적용일: 현재 작업 시점(통합된 테스트 파일 생성 및 원본 테스트 삭제)

간단한 디버깅 팁
- 테스트 실패 시 gradle 콘솔 출력과 app/build/reports/tests/testDebugUnitTest 폴더의 상세 리포트를 확인.
- BuildConfig 값은 빌드타입(debug/release)에 따라 달라지므로 로컬에서는 DEBUG 빌드에서 일부 검증을 스킵하도록 테스트에 구현되어 있음.

단위 테스트 통과의 의미
- 검증된 항목
  - AdManager: 앱오프닝 쿨다운, 시간창(hour/day) 초기화, 카운트 증가/제한 로직이 의도대로 동작함을 확인함.
  - AdController: full‑screen 플래그(등록 시 즉시 콜백, 변경시 알림)와 interstitial 플래그 토글 동작이 정상임을 확인함.
  - 빌드 설정: BuildConfig 관련 기본 검사가 현재 빌드 환경 기준으로 통과함.

- 어떤 보증을 제공하는가
  - 문서에 명시된 특정 함수와 시나리오(테스트 코드에 포함된 입력/상태)에 대해 반복 가능하게 동일한 결과를 반환함을 보장함.
  - 리팩토링이나 로직 변경 시 회귀를 빠르게 발견할 수 있는 안전망 역할을 함.

- 한계(테스트로 보장되지 않는 것)
  - UI 레이아웃, Compose/AndroidView 상호작용, 실제 AdMob SDK 실행, 네트워크 예외, 기기별 타이밍 레이스 등은 단위테스트 범위를 벗어남.
  - 멀티스레드 경쟁 조건, 시스템 상태(메모리/리소스), OS 버전별 동작은 Robolectric 또는 instrumentation(기기) 테스트가 필요함.

- 권장 후속 조치
  - UI/SDK 의존 동작은 Robolectric 또는 instrumentation 테스트로 보완할 것.
  - CI에 단위테스트를 등록해 PR마다 자동 실행되도록 설정할 것.
  - 테스트가 실패했을 때의 디버깅 가이드를 팀 위키에 추가할 것.

끝.
