# 변경 이력 (Banner Ads)
- 2025-10-24 v1.1.3
  - 배너 상단 기본 간격 `BANNER_TOP_GAP`을 12dp → 16dp로 상향(전역 기본). 화면별로 필요 시 `bannerTopGap` 파라미터로 재정의 가능.
- 2025-10-24 v1.1.2
  - BaseScreen에 `bannerTopGap` 파라미터 추가: 화면별로 배너 위 간격을 조절 가능. 기본값은 `LayoutConstants.BANNER_TOP_GAP`.
  - 설정 화면은 `bannerTopGap = 0.dp`로 적용하여 배너 위 회색 영역 제거.
  - BaseScreen: 배너가 있을 때 콘텐츠 쪽 `bottomExtra`와 내비 인셋을 제거, IME 표시시에만 IME 높이 반영.
  - 설정 화면: 콘텐츠가 화면을 다 채우지 못할 때 마지막에 남는 높이만큼 Spacer를 삽입해 배너 직전까지 채우도록 보정.
- 2025-10-24 v1.1.1
  - 설정 화면(SettingsActivity) 하단 여백 과다 이슈 수정: BaseScreen이 제공하는 LocalSafeContentPadding과 별도 수동 bottom 패딩(8dp)이 중복되어 배너 상단에 불필요한 회색 공간이 생긴 문제 해결. 수동 bottom 패딩 제거.
  - 가이드 보강: BaseScreen + bottomAd 조합에서는 콘텐츠 영역에 별도의 하단 패딩을 추가하지 말 것(Do/Don't 섹션 추가).
- 2025-10-24 v1.1.0
  - StandardScreenWithBottomButton 개선: 배너를 화면 최하단(시스템바/IME 위)으로 이동 배치, 버튼은 그 위로 고정 배치. 버튼 위치 일관성을 위해 `reserveSpaceForBottomAd` 옵션 추가.
  - LayoutConstants에 `BANNER_MIN_HEIGHT(50dp)`, `BANNER_TOP_GAP(12dp)` 추가.
  - `AdBanner.kt` 구현: `AdmobBanner` 컴포저블 도입. BuildConfig.ADMOB_BANNER_UNIT_ID 사용, 플레이스홀더면 테스트 ID 폴백. Anchored Adaptive 적용. UMP 동의 전 로드 금지.
  - Gradle: `ADMOB_BANNER_UNIT_ID` BuildConfig 필드(디버그=테스트 ID, 릴리즈=실 ID 자리표시자) 추가.
  - 화면 적용: 금주 진행/종료 화면은 `bottomAd = { AdmobBanner() }`로 노출. 금주 설정 화면은 광고 미노출 대신 `reserveSpaceForBottomAd = true`로 동일 버튼 높이 유지.
  - 컴플라이언스: 버튼과 배너 간 시각적 간격 확보, UMP 동의 전 요청 금지 문서 반영.
- 2025-10-10 v1.0.0
  - 초안 작성: 배너 의존성, 기본 배치 가이드, QA 체크리스트.

---

# [Prompt] Banner Ads (배너광고) — 구현/운영 가이드 (Reusable across apps using the same Base)

목표
- 동일 Base(Compose, StandardScreenWithBottomButton 등)를 공유하는 앱에서 배너광고를 안정적으로 배치/운영합니다.
- Adaptive Anchored Banner를 사용하여 다양한 화면폭/회전에 대응합니다.

이 프롬프트를 그대로 복사해 AI 코딩 에이전트에게 실행 지시로 전달하세요. 경로와 심볼 이름은 아래 제안과 동일하게 맞춥니다.

---

에이전트에게 줄 프롬프트

1) 의존성과 빌드 설정
- `app/build.gradle.kts`
  - Google Mobile Ads SDK가 이미 추가되어 있는지 확인 (`play-services-ads`)
  - `buildFeatures { buildConfig = true }` 유지
  - buildTypes 별 BuildConfig 필드 추가/확인
    - debug: `ADMOB_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"` (Google 테스트 Banner)
    - release: `ADMOB_BANNER_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx"` (실 ID)
  - 참고: 본 저장소에는 이미 다음 설정이 반영됨
    - debug: `ADMOB_BANNER_UNIT_ID` 테스트 ID, `ADMOB_INTERSTITIAL_UNIT_ID` 테스트 ID
    - release: `ADMOB_BANNER_UNIT_ID` 자리표시자(실 ID로 교체 필요), `ADMOB_INTERSTITIAL_UNIT_ID` 자리표시자

2) 배너 컴포저블 준비(재사용)
- 파일: `app/src/main/java/.../core/ui/AdBanner.kt`
- 요구사항
  - `AdmobBanner(modifier: Modifier = Modifier)`
  - AndroidView + AdView 사용
  - 단위 ID는 `BuildConfig.ADMOB_BANNER_UNIT_ID` 사용, 값이 비었거나 플레이스홀더면 테스트 ID로 폴백
  - Anchored Adaptive Banner: 화면폭(dp)로 `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize` 계산 후 `setAdSize` 1회 설정
  - 로그: `onAdLoaded` / `onAdFailedToLoad` / `onAdImpression` 등
  - UMP 동의가 필요한 지역에선 동의 전 로드 금지(동의 완료 후 첫 진입에서 `loadAd`)
- 참고: 본 저장소에는 위 요구사항을 충족하는 `AdmobBanner`가 구현되어 있음

3) 배치 슬롯 연결
- 파일: `app/src/main/java/.../core/ui/StandardScreen.kt` 또는 `BaseActivity.kt`의 `BaseScreen`
  - `bottomAd` 슬롯에 배너를 주입하여 “콘텐츠 아래, 화면 최하단”에 표시
  - 배너 위 간격은 `bannerTopGap`으로 화면별 조절 가능 (전역 기본은 `LayoutConstants.BANNER_TOP_GAP`=16dp)
  - 버튼/기타 고정 UI가 있는 경우 `reserveSpaceForBottomAd`로 공간만 예약 가능
- 예시(일반 화면: 기본 간격 유지)
  ```kotlin
  BaseScreen(bottomAd = { AdmobBanner() }) { /* content */ }
  ```
- 예시(설정 화면: 간격 제거)
  ```kotlin
  BaseScreen(bottomAd = { AdmobBanner() }, bannerTopGap = 0.dp) { /* content */ }
  ```

4) 정책/컴플라이언스
- UMP 동의 전 광고 요청 금지, Privacy Policy/데이터 안전성 반영
- 클릭 유도 배치 금지: 버튼과 배너 간 충분한 상/하 간격 유지(필요 시 `bannerTopGap`으로 조절)
- 테스트: 디버그 빌드에서 “Test Ad” 라벨 확인

5) QA 체크리스트
- 회전/다양한 해상도에서 깨짐 없이 배치되는지(Anchored Adaptive)
- 네트워크 불안정 시 `onAdFailedToLoad` 로그 확인 후 화면 레이아웃 붕괴 없는지(배너 영역 최소 높이 유지)
- IME(키보드) 표시 시 버튼/배너 영역과 충돌 없는지(시스템 바/IME 인셋을 고려해 하단 패딩 계산)

6) 확장 팁(선택)
- 화면 회전 시 사이즈 재계산/재로딩을 원하면 재생성 타이밍에 `setAdSize` 재설정 후 `loadAd` 호출
- 특정 화면에서만 배너를 노출하려면 Route/ScreenKey 별 조건 분기

---

부록 A: BaseScreen 사용 시 하단 패딩 가이드 (Do/Don't)

- Do
  - 콘텐츠 루트에 `LocalSafeContentPadding`만 적용하세요. 예) `Column(...).padding(LocalSafeContentPadding.current)`
  - 배너는 `BaseScreen(bottomAd = { AdmobBanner() })`에만 전달하세요. 배너 영역과 콘텐츠 영역을 분리해 일관성을 유지합니다.
  - 배너 미노출 화면에서 버튼 높이를 맞추려면 `reserveSpaceForBottomAd = true`를 사용하세요.
  - 화면 특성에 따라 배너 위 간격이 필요 없으면 `bannerTopGap = 0.dp`를 사용하세요.

- Don't
  - BaseScreen + `bottomAd`를 사용할 때 콘텐츠에 별도 `.padding(bottom = xx.dp)`를 더하지 마세요. 시스템 인셋/추가여백은 Base가 이미 적용합니다. 중복 적용 시 배너 상단에 불필요한 빈 공간(회색 영역)이 생깁니다.
  - 배너와 콘텐츠 사이에 임의의 Spacer를 직접 넣지 마세요. 간격은 `bannerTopGap` 또는 `BANNER_TOP_GAP`으로 일괄 관리합니다.

부록: 간단 빌드/실행
- 디버그 빌드: `gradlew :app:assembleDebug`
- 릴리즈 빌드: `gradlew :app:assembleRelease` (실 유닛ID 교체 필수)
