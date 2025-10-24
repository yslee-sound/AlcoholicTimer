# 전면광고(Interstitial) 전용 도입/구현 계획 (AlcoholicTimer)

작성일: 2025-10-24 (전면광고 전용)
담당: Monetization/Android
적용 범위: 전면광고(Interstitial) 1종만 우선 구현 — 배너/리워드/앱 오픈은 본 문서 범위 밖

0) 핵심 요약 및 표시 절차(정책 준수)
- 목표: 초기 릴리즈에서 전면광고만 안전하게 도입해 수익화 기반 마련
- 표시 절차(정책 위반 방지 버전)
  1) 앱 실행
  2) 스플래시 화면: 전면광고 "로딩만" 수행(표시는 하지 않음)
  3) 메인 첫 화면이 표시된 뒤, 자연스러운 전환 시점(예: 첫 사용자 탭 직후, 초기 안내 → 메인 전환 직후 등)에 전면광고 1회 노출
  4) 앱 화면 지속 이용
- 주의: 앱 실행 직후(런치 직후) 즉시 전면광고를 표시하는 행위는 Google 정책 위반 소지가 큼. 런치 직후 노출이 필요하면 Interstitial이 아닌 App Open 포맷을 사용해야 함. 본 문서는 Interstitial의 즉시 노출을 배제하고, 메인 콘텐츠 최소 1회 표시 후 전환 타이밍에서만 노출하도록 정의함.

1) KPI(초기 관찰용)
- 전면광고 노출수/DAU, eCPM, ARPDAU(참고), 리텐션 D1/D7(기존 대비 -1pp 이내)
- 세션당 노출: 0~1회(초기), 일일 캡 2~3회(원격 설정으로 조정)

2) 정책/컴플라이언스 체크리스트(전면광고 한정)
- 금지: 앱 실행 직후/종료 직전 즉시 노출, 사용자 조작/중요 확인 다이얼로그 중간 가림
- 닫기/스킵 버튼 가림 금지, 오탐 클릭 유도 금지
- 연령/민감: 성인 타겟 명시, 최대 광고 콘텐츠 등급(T) 설정 검토
- 개인정보/동의: UMP SDK로 동의 수집 및 RequestConfiguration에 NPA 반영, Privacy Policy/데이터 안전성 반영
- 테스트: 테스트 단위ID/테스트 기기 사용, 릴리즈 직전 실ID 치환

3) 표시 규칙(빈도/쿨다운/예외)
- 콜드 스타트 1회당 최대 1회(메인 표시 후 자연 전환에서만)
- 일일 캡: 기본 2회(원격 설정 0~3)
- 쿨다운: 최근 노출 후 최소 2~3분(원격 설정)
- 신규 유저 그레이스(옵션): 설치 후 24h 내 노출 완화
- 프리미엄/광고제거: 전면광고 완전 비표시
- 로드 실패/불가 조건: 즉시 건너뛰기(대체 광고 없음)

4) 기술 설계(전면광고 전용 최소 구성)
- 의존성: Google Mobile Ads SDK(AdMob), UMP SDK, Firebase Analytics(선택), Remote Config(권장)
- 구성 요소
  - AdsInitializer: MobileAds.initialize, RequestConfiguration(max content rating 등), 테스트 기기 등록
  - ConsentManager: UMP 동의 플로우, 결과 캐시, NPA 설정 반영
  - InterstitialAdManager(싱글톤): preload/load 상태/표시/쿨다운·일일캡 관리
  - AdPolicy: ads_enabled/프리미엄/동의/일일캡/쿨다운/포그라운드·Activity 유효성 체크
- 수명주기/스레드: 로드 타임아웃(8~10s), Activity 유효성/백그라운드 가드, 회전/복귀 시 안전 처리

5) 사용자 플로우(초기 진입)
- 앱 실행 → 스플래시: ConsentManager.update(필요 시 폼 표시) → AdsInitializer.applyRequestConfig → InterstitialAdManager.preload → (로드 여부 무관) 메인으로 전환
- 메인 화면 표시 후: 첫 사용자 제스처 직후 또는 초기 안내 종료 직후의 전환 타이밍에서 AdPolicy.canShow() && isLoaded이면 show(), 아니면 건너뜀
- 표시 후: recordShown(쿨다운/일일캡 갱신), dismiss 이벤트 로깅

6) 단계별 구현 체크리스트
- 스텝 0: 콘솔/문서
  - [ ] AdMob 전면광고 단위ID(테스트/실제) 생성, 네이밍 규칙 확정
  - [ ] Play Console 데이터 안전성/광고 사용/Privacy Policy 최신화
- 스텝 1: SDK/빌드
  - [ ] Ads/UMP 의존성 추가(BoM), proguard 기본값 확인
  - [ ] AndroidManifest: APPLICATION_ID 메타데이터, INTERNET/ACCESS_NETWORK_STATE 권한
- 스텝 2: UMP
  - [ ] EEA 대상 동의 폼 표시·캐시, NPA 반영
  - [ ] 설정 화면에서 동의 재설정 경로 제공
- 스텝 3: 초기화/정책/원격제어
  - [ ] AdsInitializer: initialize/test device/max content rating(T)
  - [ ] AdPolicy: 프리미엄/쿨다운/일일캡/콜드스타트 1회 제한/전환 시점 가드
  - [ ] Remote Config: ads_enabled, interstitial_daily_cap, interstitial_cooldown_min, first_launch_grace_enabled
- 스텝 4: Interstitial 구현
  - [ ] preload() in Splash after consent, proceed regardless of load result
  - [ ] maybeShowIfEligible() in Main on first natural transition
  - [ ] Activity 유효성/백그라운드 체크, 실패 시 즉시 패스
- 스텝 5: 로깅/분석
  - [ ] ad_load/ad_show/ad_fail/ad_dismiss, ad_blocked(reason) 이벤트 추가
- 스텝 6: QA/정책 점검
  - [ ] 런치 직후 즉시 노출 없음(메인 전 노출 금지) 검증
  - [ ] 테스트 기기·네트워크 상태별 로드·표시, 닫기 버튼 접근성
- 스텝 7: 릴리즈/롤아웃
  - [ ] 실 단위ID 치환, 단계적 롤아웃, Remote Config로 긴급 온·오프/빈도 조정

7) 의사코드(핵심)
- Splash.onCreate
  - ConsentManager.update { result ->
    - AdsInitializer.applyRequestConfig(result)
    - InterstitialAdManager.preload()
    - proceedToMain()
  }
- Main.onFirstUserGestureOrNaturalTransition
  - if (RemoteConfig.ads_enabled && AdPolicy.canShow() && InterstitialAdManager.isLoaded()) {
    - InterstitialAdManager.show(activity) { onDismiss -> AdPolicy.recordShown() }
  } else { continueFlow() }

8) 원격 설정(권장 기본값)
- ads_enabled=true
- interstitial_daily_cap=2
- interstitial_cooldown_min=3
- first_launch_grace_enabled=true

9) QA 시나리오(요약)
- 콜드 스타트: 스플래시에서 로딩만 수행되고, 메인 표시 전 노출이 절대 없는지
- 동의: EEA 동의 후 NPA 반영 상태에서 노출 조건 검증
- 수명주기: 회전/백그라운드 복귀 시 show 가드 정상 동작
- 빈도: 일일캡/쿨다운/그레이스 검증
- 접근성/정책: 닫기 버튼 접근성, 조작 중 방해 없음

부록 A. 의존성/설정(요약)
- Gradle
  - implementation(platform("com.google.android.gms:play-services-ads-bom:<latest>"))
  - implementation("com.google.android.gms:play-services-ads")
  - implementation("com.google.android.ump:user-messaging-platform:<latest>")
- AndroidManifest
  - <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
  - uses-permission INTERNET / ACCESS_NETWORK_STATE
- RequestConfiguration 예
  - Max ad content rating: T
  - child-directed: false, under age of consent: false

부록 B. 단위ID/네이밍(예시)
- ca-app-pub-…/interstitial_launch_flow_transition (첫 진입 전환 전용)

부록 C. 이벤트 스키마(권장)
- ad_load {format: interstitial, status: success|fail, error}
- ad_show {format: interstitial}
- ad_dismiss {format: interstitial, reason: user|auto}
- ad_blocked {reason: premium|cooldown|dailycap|foreground|consent}

