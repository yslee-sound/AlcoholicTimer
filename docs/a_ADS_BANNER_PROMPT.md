# [Prompt] Banner Ads (배너광고) — 구현/운영 가이드 (Reusable across apps using the same Base)

목표
- 동일 Base(Compose, StandardScreenWithBottomButton 등)를 공유하는 앱에서 배너광고를 안정적으로 배치/운영합니다.
- Adaptive Anchored Banner를 사용하여 다양한 화면폭/회전에 대응합니다.

이 프롬프트를 그대로 복사해 AI 코딩 에이전트에게 실행 지시로 전달하세요. 경로와 심볼 이름은 아래 제안과 동일하게 맞춥니다.

---

에이전트에게 줄 프롬프트

1) 의존성과 빌드 설정
- app/build.gradle.kts
  - Google Mobile Ads SDK가 이미 추가되어 있는지 확인 (play-services-ads)
  - buildFeatures { buildConfig = true } 유지
  - buildTypes 별 BuildConfig 필드 추가/확인
    - debug: ADMOB_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" (Google 테스트 Banner)
    - release: ADMOB_BANNER_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx" (실 ID)

2) 배너 컴포저블 준비(재사용)
- 파일 생성(또는 교체): app/src/main/java/.../core/ui/AdBanner.kt
- 요구사항
  - AdmobBanner(modifier: Modifier = Modifier)
  - AndroidView + AdView 사용
  - 단위 ID는 BuildConfig.ADMOB_BANNER_UNIT_ID 사용, 값이 비었거나 플레이스홀더면 테스트 ID로 폴백
  - Anchored Adaptive Banner: 화면폭(dp)로 AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize 계산 후 setAdSize 1회 설정
  - 로그: onAdLoaded / onAdFailedToLoad / onAdImpression 등
- UMP 동의가 필요한 지역에선 동의 전 로드 금지(동의 완료 후 첫 진입에서 loadAd)

3) 배치 슬롯 연결
- 파일: app/src/main/java/.../core/ui/StandardScreen.kt
  - 이미 존재하는 bottomAd 슬롯에 배너를 주입할 수 있어야 함
  - 예시(화면 파일 내):
    ```kotlin
    StandardScreenWithBottomButton(
        topContent = { /* ... */ },
        bottomButton = { /* ... */ },
        bottomAd = { AdmobBanner() }
    )
    ```
  - 민감/집중 상호작용 화면(예: 풀스크린 확인/폼 입력)에서는 bottomAd = null로 비활성화

4) 정책/컴플라이언스
- UMP 동의 전 요청 금지, Privacy Policy/데이터 안전성 반영
- 클릭 유도 배치 금지(버튼과 과도하게 근접 배치 금지). StandardScreenWithBottomButton에서 충분한 간격 확보
- 테스트: 디버그 빌드에서 "Test Ad" 라벨 확인

5) QA 체크리스트
- 회전/다양한 해상도에서 깨짐 없이 배치되는지(Anchored Adaptive)
- 네트워크 불안정 시 onAdFailedToLoad 로그 확인 후 화면 레이아웃 붕괴 없는지
- IME(키보드) 표시 시 버튼/배너 영역 간 충돌 여부 확인

6) 확장 팁(선택)
- 화면 회전 시 사이즈를 재계산하려면 재생성 시점에 setAdSize를 다시 설정하고 loadAd 호출
- 특정 화면에서만 배너를 노출하려면 Route/ScreenKey 별 조건 분기

---

부록: 간단 빌드/실행
- 디버그 빌드: gradlew :app:assembleDebug
- 릴리즈 빌드: gradlew :app:assembleRelease (실 유닛ID 교체 필수)

