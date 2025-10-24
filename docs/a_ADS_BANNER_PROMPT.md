# 변경 이력 (Banner Ads)
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
- 파일: `app/src/main/java/.../core/ui/StandardScreen.kt`
  - `StandardScreenWithBottomButton`의 `bottomAd` 슬롯에 배너를 주입하여 “버튼 아래, 화면 최하단”에 표시
  - 버튼은 항상 동일 위치로 올려 배치되며, 배너 유무와 무관하게 일관성 유지 가능
  - 버튼과 배너 사이에 내부 상수(`BANNER_TOP_GAP`)로 간격을 둬 클릭 유도 배치 방지
  - 추가 옵션: `reserveSpaceForBottomAd`
    - 광고를 노출하지 않아도 배너 영역의 공간만 예약해 버튼 높이를 동일하게 유지
    - 민감/집중 상호작용 화면에서 UX를 훼손하지 않으면서 레이아웃 일관성을 유지할 때 사용
- 예시(일반 화면: 배너 노출)
  ```kotlin
  StandardScreenWithBottomButton(
      topContent = { /* ... */ },
      bottomButton = { /* ... */ },
      bottomAd = { AdmobBanner() }
  )
  ```
- 예시(민감 화면: 배너 미노출, 공간만 예약 — 금주 설정 화면 반영됨)
  ```kotlin
  StandardScreenWithBottomButton(
      topContent = { /* ... */ },
      bottomButton = { /* ... */ },
      reserveSpaceForBottomAd = true
  )
  ```

4) 정책/컴플라이언스
- UMP 동의 전 광고 요청 금지, Privacy Policy/데이터 안전성 반영
- 클릭 유도 배치 금지: 버튼과 배너 간 충분한 상/하 간격 유지(`BANNER_TOP_GAP` 등으로 확보)
- 테스트: 디버그 빌드에서 “Test Ad” 라벨 확인

5) QA 체크리스트
- 회전/다양한 해상도에서 깨짐 없이 배치되는지(Anchored Adaptive)
- 네트워크 불안정 시 `onAdFailedToLoad` 로그 확인 후 화면 레이아웃 붕괴 없는지(배너 영역 최소 높이 유지)
- IME(키보드) 표시 시 버튼/배너 영역과 충돌 없는지(시스템 바/IME 인셋을 고려해 하단 패딩 계산)

6) 확장 팁(선택)
- 화면 회전 시 사이즈 재계산/재로딩을 원하면 재생성 타이밍에 `setAdSize` 재설정 후 `loadAd` 호출
- 특정 화면에서만 배너를 노출하려면 Route/ScreenKey 별 조건 분기

---

부록: 간단 빌드/실행
- 디버그 빌드: `gradlew :app:assembleDebug`
- 릴리즈 빌드: `gradlew :app:assembleRelease` (실 유닛ID 교체 필수)
