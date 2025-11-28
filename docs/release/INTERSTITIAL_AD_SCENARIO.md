# Interstitial(전면) 광고 시나리오 (현행 구현 기준)

목적
- 현재 코드베이스에 구현된 "홈 그룹(시작/진행/종료) 진입 3회마다 전면광고 트리거" 시나리오를 문서화합니다.
- QA/개발/운영 담당자가 구현 상태를 빠르게 이해하고, 로그로 동작 여부를 검증할 수 있게 합니다.

요약(핵심)
- 트리거: NavGraph에서 "비홈 → 홈" 전환을 감지할 때마다 `HomeAdTrigger.registerHomeVisit(activity, source)`를 호출합니다. 최초 앱 진입(첫 이벤트)은 카운트에서 제외됩니다.
- 카운트: SharedPreferences에 키 `home_visits_count`로 누적. 임계치 `VISIT_THRESHOLD = 3` 도달 시 전면광고 노출 시도.
- 노출 조건: (1) Supabase 기반 정책(`AdController`)에서 전면광고 허용, (2) 빈도/시간 제한 통과, (3) `InterstitialAdManager`에 광고가 로드되어 있을 것, (4) 앱의 초기 보호(초기 60초)와 쿨다운에 걸리지 않을 것, (5) Activity 상태가 유효할 것.
- 동작: 시도 성공 시 방문 카운트 초기화(0). 실패 시 카운트 유지하고 `InterstitialAdManager.preload()`를 호출.

관련 파일(중요)
- `app/src/main/java/kr/sweetapps/alcoholictimer/ads/HomeAdTrigger.kt` — 홈 방문 카운트/트리거 로직
- `app/src/main/java/kr/sweetapps/alcoholictimer/ads/InterstitialAdManager.kt` — 광고 로드/표시/정책 체크(초기 보호, 쿨다운 등)
- `app/src/main/java/kr/sweetapps/alcoholictimer/ads/AdController.kt` — Supabase 정책 로딩과 빈도 제한 체크, 중앙 정책 상태
- `app/src/main/java/kr/sweetapps/alcoholictimer/navigation/NavGraph.kt` — 비홈→홈 전환 감지 후 `HomeAdTrigger` 호출
- `app/src/main/java/kr/sweetapps/alcoholictimer/MainApplication.kt` — 앱 시작 시 정책 리스너 등록 및 interstitial preload 트리거

동작 시퀀스(정상 케이스)
1. 앱 실행 후(초기 보호가 끝난 상태라고 가정) 사용자가 비홈→홈을 3번 반복함.
2. `NavGraph`가 전환을 감지하고, `HomeAdTrigger.registerHomeVisit` 호출.
3. `HomeAdTrigger`는 SharedPreferences의 `home_visits_count`를 ++ 하고 `Home visit recorded: X/3` 로그를 남김.
4. X가 3이 되면 `AdController.canShowInterstitial(activity)` 호출하여 정책/빈도 제한 검사.
5. `InterstitialAdManager.isLoaded()`가 true면 `InterstitialAdManager.maybeShowIfEligible(activity)`를 호출.
6. 내부 정책(초기 보호, cooldown, daily cap 등)을 통과하면 광고를 show하고 성공 콜백으로 `AdController.recordInterstitialShown(activity)`를 호출.
7. 광고 성공 시 `HomeAdTrigger`는 `home_visits_count`를 0으로 리셋.

시퀀스(초기 보호(초기 60초) 내에 3회 방문한 경우)
1. 앱 시작 시 `MainApplication`는 `InterstitialAdManager.noteAppStart()`로 `appStartMs`를 기록.
2. 사용자가 1분(60s) 이내에 홈을 3번 방문하면 `HomeAdTrigger`는 3회 도달 시 노출 시도를 함.
3. `InterstitialAdManager.maybeShowIfEligible`의 `passesPolicy()` 내부에서 `now - appStartMs < INITIAL_PROTECTION_MS` 조건이 참이면 차단(`initial_protection`). 로그에 `Blocked by policy: initial_protection` 출력.
4. `HomeAdTrigger`는 실패 로그(`Interstitial ad not ready or policy restricted. Keeping visit count.`)를 남기고 `InterstitialAdManager.preload()` 호출. 카운트는 그대로 유지.
5. protection이 끝나더라도 현재 구현은 자동 즉시 재시도를 수행하지 않으므로 다음 홈 진입이나 다른 트리거시 재시도됩니다.

시퀀스(광고가 로드되지 않은 경우)
- `InterstitialAdManager.isLoaded()`가 false면 `HomeAdTrigger`는 preload만 시도하고 즉시 노출하지 않습니다. 광고 로드 완료 후 다음 트리거에서 시도됩니다.

정책/빈도 제한 영향
- Supabase에서 `adInterstitialMaxPerHour`와 `adInterstitialMaxPerDay`를 충분히 크게 설정하면 빈도 제한에 의해 차단될 가능성은 낮습니다.
- 단, `InterstitialAdManager` 자체적으로 `DEFAULT_DAILY_CAP`와 `DEFAULT_COOLDOWN_MS` 같은 fallback 정책을 갖고 있으므로 운영 설정과 코드의 디폴트 값을 검토해야 합니다.

핵심 로그 키워드(검증용)
- HomeAdTrigger: `registerHomeVisit called with source=`, `Home visit recorded: X/3`, `VISIT_THRESHOLD reached. Attempting to show interstitial ad.`, `Interstitial ad shown successfully. Resetting visit count.`, `Interstitial ad not ready or policy restricted. Keeping visit count.`
- InterstitialAdManager: `Loading interstitial with unitId=`, `onAdLoaded`, `onAdFailedToLoad`, `Blocked by policy: initial_protection`, `Blocked: ad not loaded`, `onAdShowedFullScreenContent`, `onAdDismissedFullScreenContent`
- AdController: `AdPolicy loaded:`, `✅ Can show interstitial:`, `❌ Interstitial limit reached:` , `📝 Interstitial shown recorded`

Logcat 필터(예)
```
adb logcat -s HomeAdTrigger InterstitialAdManager AdController MainApplication
```
또는 Android Studio Logcat에서 위 태그로 필터링.

검증 시나리오(권장)
1. 정책(AdPolicy) 활성화 확인: Supabase에서 `adInterstitialEnabled=true`, `adInterstitialMaxPerHour=9999`, `adInterstitialMaxPerDay=9999`.
2. 앱 재시작(앱 완전 종료 → 재실행) — `InterstitialAdManager.noteAppStart()`가 실행되어 초기 보호 기준 시점이 갱신됩니다.
3. (초기 보호가 지나간 뒤) 비홈→홈 전환을 3번 수행. Logcat에서 위 로그 흐름을 확인.
4. 초기 보호 내에서 테스트하려면 앱 시작 직후(1분 내)에 3회 홈 전환 후 `Blocked by policy: initial_protection` 로그를 확인.
5. 광고가 로드되지 않는 경우 `Preload` 후 `onAdLoaded` 로그가 나오는지 확인. 로드된 다음 방문에서 노출되는지 확인.

운영/테스트 팁
- 디버그 모드에서 초기 보호 우회를 원하면 `InterstitialAdManager`의 `INITIAL_PROTECTION_MS`를 임시로 0으로 설정(테스트 전용)하되 운영 빌드에는 반영하지 마세요.
- `MainApplication`에서 `AdController.addPolicyFetchListener`로 정책 로드 시 `InterstitialAdManager.preload()`를 호출하도록 했습니다(정책이 늦게 도착해 preload 못 하는 상황 대비).
- 자동 재시도 기능이 필요하면 `HomeAdTrigger`에서 `initial_protection` 차단 이유로 실패했을 때 `remainingMs` (초기 보호가 끝날 때까지의 시간)를 계산하여 `Handler.postDelayed`로 자동 재시도를 예약할 수 있습니다. (권장: 제품팀 합의 필요)

향후 개선 제안
- 자동 재시도(예약): 초기 보호로 차단되었을 때 보호 해제 시 자동으로 `maybeShowIfEligible`를 호출하도록 하여 사용자의 추가 조작 없이 광고 노출을 보장.
- preload 보강: 정책 도착 시점에 확실히 preload 되도록 백업 로직(여러 번 시도 또는 exponential backoff)을 추가.
- Telemetry: 광고 로드/표시 실패 사유(LoadAdError code), protection 차단 이벤트, 방문 카운트 도달 이벤트를 Analytics로 수집.

마무리
- 현재 구현은 "홈 방문 3회마다 광고 시도"라는 요구를 충족하도록 설계되어 있으나, 광고 로드 상태·초기 보호·활성 정책·액티비티 상태 등의 외부 조건에 따라 실제 즉시 노출 여부가 달라집니다.
- Supabase에서 시간당/일일 제한을 아주 크게 열어두었다면, 정상 조건(초기 보호 종료 및 광고 로드 완료)에서는 홈 방문 3회 시 전면광고가 표시되어야 합니다.

---
Generated from code inspection on the current repository (files referenced in this doc). If you want, I can:
- (A) add an "automatic retry" implementation to `HomeAdTrigger` and run a debug build and quick simulation;
- (B) add a short checklist for QA engineers to follow during staging tests (I already included verification steps above but can expand into a runnable test script);
- (C) update this repository's release docs to cross-reference `APP_OPEN_AD_POLICY.md` for app-open vs interstitial differences.

Which follow-up action do you want me to take? (A/B/C or none)
