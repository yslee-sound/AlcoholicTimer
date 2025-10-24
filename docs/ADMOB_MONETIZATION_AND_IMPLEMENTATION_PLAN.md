# 애드몹(AdMob) 도입 기획안 및 단계별 구현 계획 (AlcoholicTimer)

작성일: 2025-10-24
담당: Monetization/Android

1) 목표와 KPI
- 비즈니스 목표
  - 광고 기반 수익 창출을 도입하되 핵심 기능(타이머 사용성) 저해 최소화
  - 광고 제거 IAP와의 균형으로 LTV 극대화
- 주요 KPI(릴리즈 후 2~4주 관찰)
  - ARPDAU: 초기 목표 X 원, 12주 내 X% 개선
  - 유저당 일간 광고 노출수(Impressions/DAU): 3~6회(포맷/세그먼트별 상이)
  - Rewarded 완료율: 55%+
  - Interstitial CTR: 2~5% (지표 모니터용, 과도한 클릭 유도 지양)
  - Retention D1/D7: 광고 도입 이전 대비 -1pp 이내 유지(UX 악화 감시 지표)

2) 포맷 선택과 화면 배치 전략
- 앱 특성: 타이머 중심(세션 기반), 집중/완료 시점이 명확, 백그라운드/복귀 발생
- 권장 포맷 구성(초기)
  1) 배너(Adaptive Banner)
     - 위치: 메인(타이머 대기/완료 화면) 하단 고정 컨테이너
     - 조건: 타이머 진행 중엔 숨김(집중 방해 최소화), 일시정지/완료 시 노출
  2) 인터스티셜(Interstitial)
     - 트리거: 타이머 ‘완료’ 후 다음 화면 전환 시
     - 빈도: 1 유저 기준 최대 3회/일, 1회/3세션 수준, 첫 2분 내 재노출 금지
     - 예외: 결제 사용자, 첫 2~3일 신규 유저 온보딩 기간엔 노출 완화
  3) 리워드(Rewarded)
     - 보상안: 
       - 광고 제거 임시 패스(예: 60분 간 배너/인터스티셜 비활성)
       - 테마/사운드 잠금 해제(영구/기간 한정) 중 택일
     - 노출: 유저 의도 기반(버튼 탭)만 허용
  4) 앱 오픈(App Open)
     - 시점: 콜드 스타트(앱 최초 실행) 또는 4시간 이상 백그라운드 복귀
     - 금지: 포그라운드 짧은 전환(30초 내 복귀)에서는 미노출
- 향후 고도화 포맷
  - 네이티브: 통계/히스토리 화면 카드형 광고(초기 릴리즈 후 A/B)

3) 사용자 경험 가이드라인
- 절대 금지
  - 타이머 동작 중 화면 덮는 광고(Interstitial/App Open) 노출
  - UI 조작 중/중요 확인 다이얼로그 위 노출
- 빈도/페이싱(초기값)
  - Interstitial: ≤ 3/일, 세션 간 최소 2분 간격, 실패 시 즉시 재시도 금지(백오프)
  - App Open: ≤ 2/일, 최근 노출 후 4시간 쿨다운
  - Banner: 대기/완료 화면에만; 진행 중에는 GONE 처리
  - Rewarded: 무제한이나 진입 버튼은 2~3곳 이하로 절제
- 프리미엄/광고제거 옵션
  - 일회성 구매 혹은 구독으로 전체 광고 제거 지원
  - Rewarded 임시 패스(60분 무광고)와의 정책 충돌 방지(설명 문구 명확화)

4) 정책/컴플라이언스(주요 체크)
- 개인정보/동의
  - UMP SDK 기반 GDPR/EEA 동의 수집 및 저장, Ad personalization 제어
  - Data Safety/Privacy Policy 반영(이미 `docs/PRIVACY_POLICY.md` 존재 — 광고 관련 내용 업데이트)
- 연령/민감 주제
  - 앱 소재: 주류 관련. Designed for Families 대상 아님. 아동/청소년 대상 금지 세그먼트.
  - 콘텐츠 등급: 18+/성인 타겟 명시(지역별 19/21 고려). 광고 최대 등급(T) 설정 검토.
  - TFUA/child-directed는 사용하지 않음(아동 타겟 아님). 연령 게이트(소프트) 옵션 검토.
- 기술 정책
  - 오동작/오탐 클릭 유도 금지, 억지 배치 금지, 닫기 버튼 숨김 금지
  - 테스트 단위ID로 QA, 실ID는 릴리즈 직전 교체

5) 기술 설계 개요
- 의존성
  - Google Mobile Ads SDK(AdMob), UMP SDK
  - Firebase Analytics(이미 사용 시 연계), Remote Config(광고 온/오프, 빈도 조정)
- 구조(제안)
  - AdsInitializer: MobileAds 초기화, RequestConfiguration, test device 등록
  - ConsentManager: UMP 동의 플로우, 결과 캐시, 퍼스널라이즈 여부 노출
  - AdManagers
    - BannerAdHost: View 포함, Activity/Fragment 수명주기 안전 처리
    - InterstitialAdManager: preload + 큐 관리 + 빈도/쿨다운
    - RewardedAdManager: user-initiated 전용 로딩/표시
    - AppOpenAdManager: 프로세스 전역 싱글톤 + ProcessLifecycleOwner로 제어
  - AdPolicy
    - 빈도/쿨다운, 타이머 상태 기반 노출 허용 여부 판단, AdBlock(프리미엄/임시패스)
  - Analytics
    - ad_load, ad_show, ad_fail, rewarded_earned 등 포맷/원인 태깅
- 스레드/수명주기
  - Activity 회전/재생성, 앱 백그라운드 진입 시 광고/로더 정리
  - 표시 시점에 Activity 유효성 체크(weak ref)
- 오류/네트워크
  - 로드 타임아웃(예: 8~10초), 실패 시 지수백오프(최대 3회), 오프라인 graceful degrade

6) 단계별 구현 계획(체크리스트)
- 스텝 0: 콘솔 준비(담당: PM/DevOps)
  - [ ] AdMob 계정/앱 등록, Ad Unit ID 생성(테스트/실제 분리)
  - [ ] Play Console 데이터 안전성, “광고 사용” 항목 업데이트
  - [ ] 개인정보처리방침 문서 업데이트(광고/식별자 목적 포함)

- 스텝 1: SDK/빌드 세팅(담당: Android)
  - [ ] 아키텍처 검토 후 build.gradle.kts 및 `gradle/libs.versions.toml`에 Ads/UMP 추가
  - [ ] AndroidManifest에 APPLICATION_ID 메타데이터, 네트워크 권한 확인
  - [ ] Proguard/R8 설정 검토(기본으로 충분하나 릴리즈 전 리포트 확인)

- 스텝 2: 동의 수집(UMP)(담당: Android)
  - [ ] 첫 실행/주기적(24~48h) 동의 상태 갱신, 폼 필요 시 표시
  - [ ] 동의 결과에 따라 RequestConfiguration 업데이트(퍼스널라이즈 여부)
  - [ ] 거부/철회 UX 경로 제공(설정 화면)

- 스텝 3: 초기화/정책/원격제어(담당: Android)
  - [ ] AdsInitializer 구현: MobileAds.initialize + test device + max content rating
  - [ ] AdPolicy: 빈도/쿨다운, 타이머 상태/백그라운드 차단 규칙
  - [ ] Remote Config 플래그: ads_enabled, interstitial_cooldown_min, app_open_daily_cap 등

- 스텝 4: 배너(Adaptive)(담당: Android/Design)
  - [ ] 메인 레이아웃 하단 컨테이너 추가, insets/IME 대응
  - [ ] 타이머 진행 중 GONE, 대기/완료 시 VISIBLE 로직
  - [ ] 로드 실패 시 자리 접기/플레이스홀더 처리

- 스텝 5: 인터스티셜(완료 시점)(담당: Android)
  - [ ] 프리로드 + 노출 시점 가드(Policy, Activity 유효성)
  - [ ] 빈도/쿨다운/일일캡 구현, 실패 시 fallback 없음(UX 우선)
  - [ ] 전환 트래킹: 완료 화면 진입 시점으로 옮겨 중복 방지

- 스텝 6: 리워드(보상형)(담당: Android/PM)
  - [ ] 진입 버튼 위치 1~2곳(예: ‘광고 보고 60분 무광고’)
  - [ ] Earned 보상 처리/영속 저장, 예외/중복 보상 방지
  - [ ] 사용자 안내 문구/취소 플로우

- 스텝 7: 앱 오픈(담당: Android)
  - [ ] ProcessLifecycleOwner 연동, 콜드 스타트/쿨다운 로직
  - [ ] 타이머 즉시 사용 흐름 방해 금지(첫 포그라운드 2~3초 지연 표시 금지)

- 스텝 8: 분석/로그(담당: Android/Data)
  - [ ] 포맷별 load/show/fail, eCPM, revenue(Ads API or mediation) 로깅
  - [ ] ARPDAU 대시보드, 빈도-리텐션 상관 분석 차트

- 스텝 9: QA/정책 점검(담당: QA/Android)
  - [ ] 테스트 단위ID 전면 검증, 실기기 네트워크 상태별 테스트
  - [ ] Ad Inspector/테스트 기기 등록 확인
  - [ ] Google 정책 체크리스트 통과(오동작/오탐 클릭 유도 없음)

- 스텝 10: 릴리즈/모니터링(담당: Android/PM)
  - [ ] 실 ID 치환, Internal testing → Closed → Production 롤아웃(10%→50%→100%)
  - [ ] 크래시/ANR/세션당 광고수 모니터링, Remote Config로 긴급 온·오프 가능

7) QA 테스트 시나리오(요약)
- 네트워크: 오프라인/3G/불안정 상태에서 배너/인터스티셜/리워드/앱 오픈 로드/표시
- 수명주기: 회전, 백그라운드 복귀, 프로세스 킬 후 복귀(App Open 쿨다운 유지)
- 동의: EEA 디바이스에서 동의 폼 노출/변경/철회, 비EEA 디바이스에서 미노출 확인
- 정책: 타이머 동작 중 전면광고 금지 확인, 닫기 버튼 접근성, 배치 간격
- 프리미엄: 광고 제거 구매/복원 시 모든 광고 완전 비표시

8) 배포 체크리스트(요약)
- Play Console
  - Data safety(광고/광고ID/분석), 콘텐츠 등급(성인), 광고 사용 표시
  - 앱 설명/스크린샷에 과도한 광고 유도 문구 금지
- 스토어 정책 문서화
  - Privacy Policy 업데이트(광고/식별자/동의)
  - 인앱 공지: 광고 포함/광고 식별자 사용 고지(설정 화면)

9) 리스크와 완화책
- 리텐션 하락: 빈도/쿨다운 상향, Remote Config 핫픽스, 신규 유저 그레이스 기간
- 수익 저조: Rewarded 보상 가치 상향, 배너 위치/크기 최적화, 네이티브 검토
- 크래시/ANR: 광고 표시 시점 가드, 타임아웃/백오프, 메모리 관리 철저

10) 추후 고도화
- 미디에이션 도입(AdMob → mediation stack): eCPM 최적화, 워터폴/비딩 비교 A/B
- 네이티브 광고 카드 디자인(히스토리/통계)
- A/B: Interstitial 빈도 1/2/3세션, App Open 쿨다운 2/4/8h

부록 A. 의존성/설정 스니펫(참고)
- Gradle(버전은 최신 안정판으로 고정 권장)
  - settings: Google maven 포함
  - module build.gradle.kts
    - implementation(platform("com.google.android.gms:play-services-ads-bom:<latest>"))
    - implementation("com.google.android.gms:play-services-ads")
    - implementation("com.google.android.ump:user-messaging-platform:<latest>")
- AndroidManifest
  - <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
  - INTERNET, ACCESS_NETWORK_STATE 권한
- 초기화 예시(개요)
  - MobileAds.initialize(context)
  - RequestConfiguration.Builder().setMaxAdContentRating("T").setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
- UMP 동의 플로우 개요
  - ConsentInformation.update → 필요 시 ConsentForm.show → 결과 저장

부록 B. 단위ID/환경 분리
- debug: 테스트 단위ID 사용(기기 등록 포함)
- release: 실제 단위ID, Remote Config로 강제 OFF 지원

부록 C. 네이밍 컨벤션(권장)
- ca-app-pub-…/banner_main_bottom
- ca-app-pub-…/interstitial_session_complete
- ca-app-pub-…/rewarded_ad_free_pass_60m
- ca-app-pub-…/app_open_default

실행 우선순위(요약)
1) UMP/초기화/배너 → 2) 인터스티셜(완료 시점) → 3) 리워드 → 4) 앱 오픈 → 5) 분석/원격제어 → 6) QA/릴리즈

