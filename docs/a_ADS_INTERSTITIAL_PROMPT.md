# [Prompt] Interstitial Ads (전면광고) — 구현/운영 가이드 (Reusable across apps using the same Base)

목표
- 동일 Base(Compose, MainApplication, StandardScreen 등)를 공유하는 앱에서 전면광고를 신속하고 일관되게 도입/운영합니다.
- 개발(디버그) 단계에서는 정책 우회를 통해 광고 표시 지점을 빠르게 검증하고, 릴리즈에서는 정책을 엄격히 준수합니다.

이 프롬프트를 그대로 복사해 AI 코딩 에이전트에게 실행 지시로 전달하세요. 경로와 심볼 이름은 아래 제안과 동일하게 맞춥니다.

---

에이전트에게 줄 프롬프트

1) 의존성과 빌드 설정
- app/build.gradle.kts
  - buildFeatures { buildConfig = true } 유지
  - Google Mobile Ads SDK, UMP SDK 의존성 추가 확인
  - buildTypes 별 BuildConfig 필드 추가/확인
    - debug: ADMOB_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" (Google 테스트 ID)
    - release: ADMOB_INTERSTITIAL_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx" (실 ID) — 미정이면 "REPLACE_WITH_REAL_INTERSTITIAL" 플레이스홀더 가능

2) 초기화
- MainApplication.kt
  - MobileAds.initialize 호출
  - RequestConfiguration: Max ad content rating = T
  - 앱 프로세스 시작마다 InterstitialAdManager.resetColdStartGate() 호출
  - UMP(Consent) 플로우가 앱에서 수행된다면, 동의 완료 이후에만 InterstitialAdManager.preload를 호출하도록 StartActivity에서 게이팅

3) 전면광고 매니저(싱글톤)
- 파일: app/src/main/java/.../core/ads/InterstitialAdManager.kt
- 필요 API
  - fun preload(context: Context)
  - fun isLoaded(): Boolean
  - fun maybeShowIfEligible(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean
  - fun resetColdStartGate()
- 동작 규칙(릴리즈 기준)
  - 콜드 스타트당 최대 1회 게이트
  - 일일 캡 3회, 쿨다운 2분(SharedPreferences에 기록)
  - 표시 실패/닫힘 시 즉시 preload 재개
  - BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID 값이 비었거나 "REPLACE_WITH_REAL_INTERSTITIAL" 포함 시 Google 테스트 ID로 폴백
- 동작 규칙(디버그 기준)
  - BuildConfig.DEBUG가 true이면 정책(콜드스타트/일일캡/쿨다운)을 우회
  - isLoaded일 때 항상 show 시도

4) 표시 지점 연결(자연 전환 트리거)
- StartActivity: "시작" 버튼 클릭 직후
  - isLoaded면 즉시 show
  - 미로드면 최대 1.2초 대기하여 로드되면 show, 실패/타임아웃 시 바로 진행
- RunActivity: 타이머 완료 직전(상세 화면 진입 전에)
  - isLoaded && 정책 통과 시 show, 아니면 패스
- QuitActivity(금주 종료): 종료 버튼 롱프레스 완료 시
  - isLoaded && 정책 통과 시 show, onDismiss에서 안전하게 StartActivity 복귀
  - 디버그에서는 미로드여도 최대 2.5초 대기하여 로드되면 show; 실패/타임아웃 시 바로 복귀

5) 통합 시그니처(예시)
- InterstitialAdManager.preload(applicationContext) — Start/Quit 진입 시점 등 적절한 타이밍에 호출
- InterstitialAdManager.maybeShowIfEligible(activity) { /* onDismiss: 화면 전환 */ }

6) QA 체크리스트
- 디버그: 모든 표시 지점에서 광고가 쉽게 보이는지(정책 우회) 확인, 로깅으로 unitId/blocked 이유 추적
- 릴리즈(내부 테스트 트랙):
  - 런치 직후 즉시 노출 금지(자연 전환 지점에서만 노출)
  - 일일 캡 3회/쿨다운 2분이 집행되는지
  - UMP 동의 전에는 preload/요청 없음
  - 닫힘/실패 시 즉시 preload 재개

7) 로깅 키워드(추천)
- InterstitialAdManager: onAdLoaded, onAdShowedFullScreenContent, onAdDismissedFullScreenContent, onAdFailedToShowFullScreenContent, Blocked: reason(dailycap|cooldown|coldstart|not_loaded)

8) 실패/대응 정책
- isLoaded=false: 자연 전환을 방해하지 않고 즉시 다음 화면으로 진행
- show 실패: onDismiss/onFailed 콜백에서 즉시 다음 화면 진행 + preload 재개

9) 적용 결과(참고 구현 경로)
- MainApplication.kt — MobileAds 초기화 및 cold-start 게이트 reset
- core/ads/InterstitialAdManager.kt — 전면광고 매니저 및 정책/우회
- feature/start/StartActivity.kt — 시작 버튼 진입 지점 연동
- feature/run/RunActivity.kt — 완료 지점 연동
- feature/run/QuitActivity.kt — 롱프레스 완료 지점 연동(디버그 대기 최대 2.5초)

---

부록: 간단 빌드/실행
- 디버그 빌드: gradlew :app:assembleDebug
- 릴리즈 빌드: gradlew :app:assembleRelease (실 유닛ID 교체 필수)

