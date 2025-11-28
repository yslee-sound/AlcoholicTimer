# 앱 오프닝(App Open) 광고 정책 및 구현 가이드

목적
- 이 문서는 AlcoholicTimer 앱에 적용된 앱 오프닝(App Open) 광고의 정책, 구현 세부사항, 운영/검증 절차, 장애 대응 및 권장 설정을 릴리스 노트 형식으로 정리합니다.
- 배포(릴리스) 시 QA, 정책 검증, 운영 담당자(Ads/Product)에게 배포하세요.

요약(한눈에 보기)
- 광고 유형: App Open Ad (Google Mobile Ads - AppOpenAd)
- 제어 소스: Supabase 원격 정책(ad_policy) + 앱 내부 Fallback 제한
- 핵심 목표: 스플래시(launch theme)와 광고를 통합해 UX를 유지하되, Google AdMob 정책과 사용자 경험을 모두 보호

정책 핵심 항목
- ad_app_open_enabled (Supabase): 앱 오프닝 광고 전체 ON/OFF
- 쿨다운: 광고 표시 후 재시도 쿨다운 60초
- 초기 보호(임시): Cold start 직후 불필요한 강제 노출 방지(내부 로직으로 제어 가능)
- 안전 타임아웃: 광고가 로드되지 않거나 지연될 때 스플래시를 강제로 해제하는 타임아웃(앱 기본 5000ms — 권장 B)
- 광고 단위 ID: 릴리스 전 운영 단위로 설정 필요(테스트 ID 사용 불가 상태에서 정책 위반 발생 가능)

현재 구현(중요 파일 목록)
- AppOpenAdManager (앱 전역 관리자)
  - 위치: `app/src/main/java/kr/sweetapps/alcoholictimer/ads/AppOpenAdManager.kt`
  - 역할: preload, showIfAvailable, lifecycle 기반 자동 표시(옵션), 로드/표시/종료 콜백 관리
  - 특징: allowAutoShow 플래그로 라이프사이클 자동 노출 제어 가능
  - 콜백: onAdLoadedListener, onAdShownListener, onAdFinishedListener
- MainActivity (런처/Compose 진입점)
  - 위치: `app/src/main/java/kr/sweetapps/alcoholictimer/MainActivity.kt`
  - 역할: 스플래시(테마) 유지와 광고 동기화 담당
  - 동작: `installSplashScreen()` + `splash.setKeepOnScreenCondition{ holdSplashState }`로 시스템 스플래시 유지
  - 광고 흐름 제어: AppOpenAdManager.setAutoShowEnabled(false)로 자동 표시 억제 → onAdLoaded에서 수동 show 시도 → onAdShown에서 스플래시 해제
- AndroidManifest
  - `com.google.android.gms.ads.AdActivity`에 `android:theme="@style/Theme.AlcoholicTimer.Splash"`와 `tools:replace="android:theme"` 적용
  - 이유: 라이브러리 기본 테마(`@android:style/Theme.Translucent`)로 인한 검은 배경 간극을 제거하기 위함
- themes.xml
  - 위치: `app/src/main/res/values/themes.xml`
  - `Theme.AlcoholicTimer.Splash`는 `@drawable/splash_screen`을 `windowBackground`로 사용

상세 동작 시퀀스
1. 앱 런처(Cold start) -> `MainActivity.onCreate` 실행
2. `holdSplashState = true`로 설정하고 AndroidX SplashScreen을 `installSplashScreen()`으로 등록
3. 안전 타임아웃(현재 앱 기본: 5000ms) 등록: 타임아웃 발생 시 `holdSplashState = false`(스플래시 해제)
4. AppOpenAdManager에 대해 auto-show를 비활성화(setAutoShowEnabled(false))하고 리스너를 등록
   - onAdLoadedListener: 광고 로드 시 타임아웃을 취소(removeCallbacks), 스플래시를 유지한 채 수동으로 `showIfAvailable()` 호출
   - onAdShownListener: 광고가 실제로 화면에 보이는 순간에 `holdSplashState = false`로 설정(검은 간극 제거)
   - onAdFinishedListener: 광고가 닫히거나 실패하면 fallback으로 `holdSplashState = false` 호출
5. 광고가 없거나 실패하면 타임아웃(5초) 후 스플래시 해제 및 앱 진입

안전 타임아웃(정의와 구현)
- 정의: 광고의 로드/표시가 지연될 때 사용자가 앱이 멈춘 것으로 인식하지 않도록 강제로 스플래시를 해제하는 타이머.
- 구현 위치: `MainActivity.kt` (timeoutRunnable + `window.decorView.postDelayed(timeoutRunnable, N)`) 
- 업계 표준 요약: 많은 앱이 3–5초(권장: 4초) 범위를 채택하고 있으며, 광고 우선 정책을 택한 앱은 6–8초까지 허용합니다. 실무 권장 방식은 실제 광고 로드 시간의 p90(90백분위) + 300–500ms 보정값을 사용하는 것입니다.
- 기본값(현재 앱 설정): 5000ms (권장 B — UX/수익 균형)
- 동작: 광고 로드 시 `removeCallbacks(timeoutRunnable)`로 타임아웃 취소. 광고가 실제로 보이면 onAdShown에서 스플래시 해제. 광고가 영구히 안뜨면 타임아웃이 스플래시를 해제하고 앱 UI 진입.
- 변경 방법: `MainActivity.kt`에서 `postDelayed(..., <ms>)` 값을 수정

왜 안전 타임아웃이 필요한가
- 광고 SDK가 느리거나 네트워크가 불안정할 때 스플래시가 무한 유지되어 앱이 멈춘 것처럼 보일 수 있음
- 정책/광고 로딩 실패(예: AdUnit mismatch)로 ad가 아예 뜨지 않을 때 자동 복구(앱 진입)를 위해 필요

로그 및 디버깅 ��크리스트 (검증용)
- MainActivity 로그(예시 태그/메시지)
  - "disabling auto-show on AppOpenAdManager"
  - "Ad loaded -> manual show requested (cancelling timeout)"
  - "attempting to show ad via AppOpenAdManager.showIfAvailable"
  - "onAdShownListener invoked: ad is visible; releasing holdSplashState"
  - "splash timeout fired -> releasing holdSplashState"
- AppOpenAdManager 로그(예시 태그)
  - onAdLoaded: "onAdLoaded app-open @..."
  - onAdShowedFullScreenContent: "onAdShowedFullScreenContent @..."
  - onAdDismissedFullScreenContent: "onAdDismissedFullScreenContent @..."
  - onAdFailedToLoad: "onAdFailedToLoad app-open: ..."

운영(릴리스)/검증 절차
1. 릴리스 전(스테이징)
   - 테스트 광고 단위로 동작 확인 (테스트 ID 또는 테스트 디바이스 설정)
   - Cold start 시 스플래시 → 광고 → 광고 닫힘 → 앱 진입 시퀀스 정상 확인
   - 광고 실패 케이스에서 7초 타임아웃 작동 확인
2. 릴리스(프로덕션)
   - Supabase의 `ad_app_open_enabled` 값을 검증(ON/OFF) — 배포 전 정책 문서화
   - 배포 후 24~48시간 로그 모니터링(광고 로드 실패율, 스플래시 timeout 이벤트)
3. 롤백/긴급 OFF
   - Supabase `ad_app_open_enabled=false` 적용 → 앱이 다음 정책 pull 시 앱오프닝 광고 시도 중지

테스트 케이스(최소)
- 기본 흐름: Cold start에서 스플래시 위에 광고가 즉시 또는 곧바로 표시되고 광고 보이면 스플래시가 해제되는지
- 실패 흐름: 광고 로드 실패 시 `onAdFailedToLoad` 로그 + 7초 후 스플래시 해제
- 느린 네트워크: 광고 로드가 3~6초 딜레이될 때에도 광고가 보이면 스플래시가 유지되고 광고가 보일 때 해제되는지
- 자동노출 억제: `AppOpenAdManager.setAutoShowEnabled(false)`가 정상 적용되어 MainActivity가 수동으로 광고 표시를 담당하는지

구성 옵션(권장)
- ad_app_open_enabled: Supabase 원격 제어 (기본: true)
- safe_splash_timeout_ms: 기본 5000 (권장 5000~7000)
- allow_auto_show: default false (앱에서 명시적 수동 제어 권장)

추가 권장 개선사항
- 타임아웃/로딩 통계를 Analytics에 수집 (ad_load_time, ad_show_time, splash_timeout_event)
- Supabase 필드로 타임아웃/쿨다운 값을 원격화: `ad_app_open_cooldown_sec`, `ad_open_safe_timeout_ms`
- AppOpen 로드/표시 실패 원인(LoadError code) 원격 집계 후 정책 또는 단위 교체 자동화

릴리스 노트(요약)
- `MainActivity`에서 AndroidX SplashScreen과 광고 동기화를 적용했습니다.
- `AppOpenAdManager`의 자동노출을 억제하고 MainActivity에서 수동 표시하도록 변경했습니다.
- `AdActivity`에 splash 테마를 적용해 검은 간극을 제거했습니다.
- 안전 타임아웃을 앱은 권장 B(5초)로 설정되어 있습니다 (코드 변경 위치: `MainActivity.kt`).
 
업데이트 요약 (이번 릴리스)
- 업계 표준 분석을 문서에 반영했습니다(권장 범위: 3–5s, 광고 우선: 6–8s, p90 기반 규칙 권장).
- 우리 앱은 권장 B로 판단하여 안전 타임아웃을 5초(5000ms)로 설정했습니다.
- 문서와 코드가 일치하도록 `MainActivity.kt`의 타임아웃 값을 5000ms로 변경했습니다.

문의/담당자
- 개발: kr.sweetapps.alcoholictimer 개발팀
- 광고/운영: Ads 담당자 (Supabase 정책 변경 권한 보유)

부록: 빠른 변경 예시
- 안전 타임아웃 변경(예: 5초로):
```kotlin
// MainActivity.kt
window.decorView.postDelayed(timeoutRunnable, 5000)
```

- Supabase에서 앱 오프닝 광고 비활성화(예):
```sql
UPDATE ad_policy SET ad_app_open_enabled = false WHERE app_id = 'kr.sweetapps.alcoholictimer'
```

---
문서 끝.
