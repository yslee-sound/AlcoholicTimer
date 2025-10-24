# 전면광고(Interstitial) 전용 도입/구현 계획 v2 (AlcoholicTimer)

작성일: 2025-10-24 (전면광고 전용, 프리미엄 제외)
담당: Monetization/Android
적용 범위: 전면광고(Interstitial) 1종만 — 배너/리워드/앱 오픈 및 프리미엄(광고제거) 기능은 본 문서 범위 밖

0) 핵심 요약 및 표시 절차(정책 준수)
- 표시 절차: 앱 실행 → 스플래시(로딩만) → 메인 표시 → 자연 전환에서 전면광고 1회 노출
- 주의: 런치 직후 즉시 노출 금지(App Open 포맷 대상). 본 문서는 Interstitial의 즉시 노출을 배제합니다.

1) KPI(초기)
- 전면광고 노출수/DAU, eCPM, ARPDAU(참고), 리텐션 D1/D7(기존 대비 -1pp 이내)
- 세션당 노출: 0~1회, 일일 캡 2회(원격 0~3), 쿨다운 2~3분(원격)

2) 정책/컴플라이언스
- 앱 실행 직후/종료 직전 즉시 노출 금지, 사용자 조작/중요 확인 가림 금지
- 닫기 버튼 가림/오탐 클릭 유도 금지
- 성인 타깃 명시, 최대 광고 콘텐츠 등급(T) 고려
- UMP 동의 수집 및 NPA 반영, Privacy Policy/데이터 안전성 업데이트
- 테스트 단위ID/테스트 기기 사용 → 릴리즈 직전 실ID 치환

3) 표시 규칙(프리미엄 제외)
- 콜드 스타트당 최대 1회(메인 표시 후 자연 전환에서만)
- 일일 캡/쿨다운만 적용(프리미엄/광고제거 예외 없음)
- 로드 실패/불가 조건: 즉시 건너뛰기(대체 광고 없음)

4) 기술 설계(최소 구성)
- 의존성: Google Mobile Ads SDK(AdMob), UMP SDK, Remote Config(권장)
- 구성 요소
  - AdsInitializer: MobileAds.initialize, RequestConfiguration(max content rating 등)
  - ConsentManager: UMP 동의 플로우, 결과 캐시, NPA 반영
  - InterstitialAdManager(싱글톤): preload/load/표시/상태 관리
  - AdPolicy: ads_enabled/일일캡/쿨다운/포그라운드·Activity 유효성 체크(프리미엄 제외)
- 수명주기/스레드: 로드 타임아웃(8~10s), Activity 유효성/백그라운드 가드, 회전/복귀 안전 처리

5) 사용자 플로우(초기 진입)
- 앱 실행 → 스플래시: UMP 동의 처리 → RequestConfiguration 반영 → Interstitial preload → (로드 여부 무관) 메인으로 전환
- 메인 표시 후: 첫 사용자 제스처(자연 전환)에서 canShow && isLoaded이면 show(), 아니면 패스

6) 체크리스트
- 콘솔/문서: 전면광고 단위ID(테스트/실제), Data safety/Privacy Policy 업데이트
- SDK/빌드: Ads/UMP 추가, Manifest APPLICATION_ID/권한
- UMP: 동의 폼, NPA 반영, 재설정 경로
- 초기화/정책/원격: ads_enabled, interstitial_daily_cap, interstitial_cooldown_min
- 구현: preload(스플래시), maybeShow(메인 자연 전환), Activity 유효성 가드
- 로깅: ad_load/ad_show/ad_fail/ad_dismiss/ad_blocked(reason)
- QA: 런치 직후 노출 금지, 네트워크/동의/수명주기/빈도/접근성
- 릴리즈: 실 단위ID 치환, 점진 롤아웃, Remote 긴급 온·오프

7) 의사코드(핵심)
- Splash.onCreate
  - Consent.update → AdsInitializer.applyRequestConfig → Interstitial.preload → proceedToMain()
- Main.onFirstUserGestureOrNaturalTransition
  - if (Remote.ads_enabled && Policy.canShow() && Interstitial.isLoaded()) show(); Policy.recordShown()

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
- ad_blocked {reason: cooldown|dailycap|foreground|consent}

