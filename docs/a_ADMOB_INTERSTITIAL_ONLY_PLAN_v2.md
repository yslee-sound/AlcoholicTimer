# 전면광고(Interstitial) 전용 도입/구현 계획 v2 (AlcoholicTimer)

> 문서 버전
> - 버전: v2.2.0
> - 최근 업데이트: 2025-10-24
> - 변경 요약: 구현 반영 — UMP 동의 후 preload 게이팅, 일일 캡 3회, 쿨다운 2분, 자연 전환 트리거(시작 버튼/타이머 완료), 첫 클릭 로드 대기(최대 1.2s), 닫힘/실패 시 즉시 프리로드, 스플래시 직후 자동 노출 금지 유지, 빌드타입별 광고 유닛ID 자동 분기(디버그=테스트ID/릴리스=실ID, BuildConfig 기반 폴백 포함)
>
> 변경 이력(Changelog)
> - v2.2.0 (2025-10-24)
>   - 빌드타입별 광고 유닛ID 자동 분기 가이드 추가: BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID 주입, 디버그=Google 테스트 ID, 릴리스=실 ID. 값 미설정/플레이스홀더 시 테스트 ID로 폴백
>   - InterstitialAdManager가 BuildConfig 값을 참조하도록 설계 반영(currentUnitId), BuildConfig 생성 활성화(buildFeatures.buildConfig=true)
> - v2.1.0 (2025-10-24)
>   - UMP 동의 플로우 완료 후에만 Interstitial preload 실행(EEA/UK 등 동의 지역 대비)
>   - 일일 캡 2→3으로 상향, 쿨다운 2분 유지, 콜드 스타트당 1회 게이트 유지
>   - 자연 전환 트리거 2종: 메인(Start) "시작" 버튼 클릭, 타이머 완료→상세(Detail) 진입 직전
>   - 시작 버튼 클릭 시 광고 미로드일 경우 최대 1.2초 로드 대기 후 즉시 표시 시도(타임아웃 시 바로 진행)
>   - 전면광고 닫힘/표시 실패 시 다음 기회 대비 즉시 프리로드 재개
>   - 스플래시에서는 preload만 수행, 스플래시 직후 자동 노출 금지 유지
> - v2.0.0 (2025-10-24)
>   - 초안: 전면광고 전용 도입 계획 v2 정리(배너/리워드/앱 오픈/프리미엄 제외)

담당: Monetization/Android  |  적용 범위: 전면광고(Interstitial) 1종만 — 배너/리워드/앱 오픈 및 프리미엄(광고제거) 기능은 본 문서 범위 밖

---

## 0) 핵심 요약 및 표시 절차(정책 준수)
- 절차: 앱 실행 → 스플래시(UMP 동의 처리, RequestConfiguration 반영, Interstitial preload만) → 메인 표시 → 자연 전환에서 전면광고 1회 노출
- 자연 전환 트리거: (1) 메인(Start) 화면의 "시작" 버튼 클릭 직후, (2) 타이머 완료 후 상세 화면(Detail) 진입 직전
- 주의: 런치 직후 즉시 노출 금지(App Open 포맷 대상) — Interstitial의 즉시 노출 배제 유지
- UMP: 동의 전에는 광고 요청(preload 포함) 금지 → 동의 완료 후에만 preload 시작

## 1) KPI(초기)
- 전면광고 노출수/DAU, eCPM, ARPDAU(참고), 리텐션 D1/D7(기존 대비 -1pp 이내)
- 세션당 노출: 0~1회(콜드 스타트 게이트), 일일 캡 3회(원격 0~3), 쿨다운 2분(원격 2~3분)

## 2) 정책/컴플라이언스
- 앱 실행 직후/종료 직전 즉시 노출 금지, 사용자 조작/중요 확인 가림 금지, 닫기 버튼 가림/오탐 클릭 유도 금지
- 성인 타깃 명시, 최대 광고 콘텐츠 등급(T) 적용(RequestConfiguration)
- UMP 동의 수집 및 NPA 반영(동의 전 광고 요청 금지), Privacy Policy/데이터 안전성 업데이트
- 테스트 단위ID/테스트 기기 사용 → 릴리즈 직전 실ID 치환

## 3) 표시 규칙(프리미엄 제외)
- 콜드 스타트당 최대 1회(메인 표시 후 자연 전환에서만)
- 일일 캡: 3회, 쿨다운: 2분(둘 다 원격으로 0~3회, 2~3분 등 조정 가능 — 선택)
- 자연 전환 트리거 2종: "시작" 버튼 클릭 직후, 타이머 완료→상세 진입 직전
- 로드 실패/불가/정책 차단: 즉시 건너뛰기(대체 광고 없음, 다음 화면으로 곧바로 진행)

## 4) 기술 설계(최소 구성)
- 의존성: Google Mobile Ads SDK(AdMob), UMP SDK, Remote Config(선택)
- 구성 요소(구현 기준)
  - AdsInitializer(MainApplication): MobileAds.initialize, RequestConfiguration(MAX_AD_CONTENT_RATING_T 등) 적용
  - ConsentManager(UmpConsentManager): UMP 동의 플로우 실행, canRequestAds 판정(동의 전 로드 금지)
  - InterstitialAdManager(싱글톤): preload/load/표시/상태 관리, 정책(콜드 스타트 1회/일일 캡/쿨다운) 집행, 닫힘/실패 시 즉시 프리로드 재개
  - UnitIdProvider(BuildConfig 기반): BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID를 사용, 비정상/플레이스홀더 값이면 Google 테스트 ID로 폴백
  - AdPolicy(내장): daily_cap=3, cooldown_ms=120000(2분), 콜드 스타트 게이트(프로세스당 1회)
- 수명주기/스레드 가드
  - 로드 타임아웃(권장 8~10s) 고려, Activity 유효성/포그라운드 가드, 회전/복귀 안전 처리
  - 첫 사용자 제스처(시작 버튼) 시 미로드면 최대 1.2초 로드 대기 후 즉시 표시 시도(UX 지연 최소화)

## 5) 사용자 플로우(초기 진입)
- 앱 실행 → 스플래시: UMP 동의 처리 → RequestConfiguration 반영 → Interstitial preload(동의 완료 후) → (로드 여부 무관) 메인으로 전환
- 메인 표시 후(자연 전환):
  - 시작 버튼 클릭: canShow && isLoaded이면 show(), 미로드면 최대 1.2초 대기 후 성공 시 show(), 아니면 패스
  - 타이머 완료→상세 진입 직전: canShow && isLoaded이면 show(), 아니면 패스

## 6) 체크리스트
- 콘솔/문서: 전면광고 단위ID(테스트/실제), Data safety/Privacy Policy 업데이트
- SDK/빌드: Ads/UMP 추가, Manifest APPLICATION_ID/권한
- UMP: 동의 폼, NPA 반영, 재설정 경로(Privacy options, 추후)
- 초기화/정책/원격: ads_enabled(선택), interstitial_daily_cap(기본 3), interstitial_cooldown_min(기본 2분)
- 구현: preload(스플래시), maybeShow(메인 자연 전환 2점: 시작/완료), Activity 유효성/포그라운드 가드, 닫힘 후 즉시 preload 재개
- 유닛ID 관리: debug=테스트ID, release=실ID(치환 필요), BuildConfig 기반 자동 분기, 값 미설정/플레이스홀더 시 테스트ID 폴백
- 로깅: ad_load/ad_show/ad_fail/ad_dismiss/ad_blocked(reason)
- QA: 런치 직후 노출 금지, 네트워크/동의/수명주기/빈도/접근성, 시작/완료 트리거에서 중복 노출 없는지(콜드 스타트 1회)
- 릴리즈: 실 단위ID 치환, 점진 롤아웃, Remote 긴급 온·오프(선택)

## 7) 의사코드(핵심)
- Splash.onCreate
  - Consent.update → AdsInitializer.applyRequestConfig → if (consent.canRequestAds) Interstitial.preload → proceedToMain()
- Main.onFirstUserGestureOrNaturalTransition
  - if (Policy.canShow() && Interstitial.isLoaded()) show(); else if (firstGesture && !isLoaded) waitUpTo(1.2s){retryOnceOrPass}; Policy.recordShownOnShow()
- Timer.onCompletedBeforeNavigate
  - if (Policy.canShow() && Interstitial.isLoaded()) show(); else pass

## 8) 빌드타입별 광고 유닛ID 관리(공통 가이드)
- 목적: 개발/QA에선 테스트 광고 고정, 릴리스에선 실 광고 사용. 실수로 실 광고가 노출되거나 테스트 광고가 릴리스에 노출되는 것을 예방
- Gradle 설정(앱 모듈)
  - buildFeatures.buildConfig = true 활성화
  - buildTypes 별 BuildConfig 필드 주입
    - debug: `ADMOB_INTERSTITIAL_UNIT_ID = ca-app-pub-3940256099942544/1033173712` (Google 테스트 Interstitial)
    - release: `ADMOB_INTERSTITIAL_UNIT_ID = <실 유닛ID>` — 초기엔 `REPLACE_WITH_REAL_INTERSTITIAL` 플레이스홀더 유지 가능
- 코드(광고 매니저)
  - `BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID`를 읽어 사용
  - 값이 비었거나 `REPLACE_WITH_REAL_INTERSTITIAL` 문구를 포함하면 Google 테스트 ID로 폴백
  - 로깅에 현재 unitId 출력(개발 추적용)
- QA 체크
  - 디버그 빌드: 광고 상단에 “Test Ad” 라벨, Logcat에 테스트 unitId 출력
  - 릴리스 후보: 실 유닛ID로 교체 후 내부테스트/클로즈드 트랙에서 동작 확인
- 보안/운영
  - 유닛ID는 비밀정보는 아니지만, 필요 시 환경변수/local.properties에서 값을 읽어 BuildConfig에 주입하는 방식으로 코드 내 노출 최소화 가능

부록 A. 의존성/설정(요약)
- Gradle
  - implementation(platform("com.google.android.gms:play-services-ads-bom:<latest>"))
  - implementation("com.google.android.gms:play-services-ads")
  - implementation("com.google.android.ump:user-messaging-platform:<latest>")
- AndroidManifest(예시)
  ```xml
  <manifest>
      <uses-permission android:name="android.permission.INTERNET" />
      <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
      <application>
          <meta-data
              android:name="com.google.android.gms.ads.APPLICATION_ID"
              android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
      </application>
  </manifest>
  ```
- RequestConfiguration 예: Max ad content rating=T, child-directed=false, underAgeOfConsent=false

부록 B. 이벤트 스키마
- ad_load {format: interstitial, status: success|fail, error}
- ad_show {format: interstitial}
- ad_dismiss {format: interstitial, reason: user|auto}
- ad_blocked {reason: cooldown|dailycap|foreground|consent|not_loaded|coldstart}

부록 C. 적용 포인트(프로젝트 기준 예)
- 초기화: `MainApplication` — RequestConfiguration, MobileAds.initialize, 콜드 스타트 게이트 reset
- 스플래시/메인: `StartActivity` — UMP 동의 플로우, 동의 후 preload, 시작 버튼 트리거 로드 대기
- 런(타이머): `RunActivity` — 타이머 완료 트리거(상세 진입 직전 maybeShow)
- 매니저: `core/ads/InterstitialAdManager`, `core/ads/UmpConsentManager` — 정책 집행, 로드/표시/리스너, 재프리로드
