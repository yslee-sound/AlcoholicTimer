# [Prompt] Native Ad on Back Press (뒤로가기 네이티브 팝업) — 구현/운영 가이드 (Reusable across apps using the same Base)

목표
- 동일 Base(Compose, BaseActivity, StandardScreen 등)를 공유하는 앱에서 “뒤로가기 시 표시되는 네이티브 팝업” 패턴을 안전하게 구현합니다.
- 정책 준수: 사용자의 앱 종료/이탈을 부당하게 방해하지 않으며, 닫기/종료 경로를 명확히 제공합니다.

이 프롬프트를 그대로 복사해 AI 코딩 에이전트에게 실행 지시로 전달하세요. 경로와 심볼 이름은 아래 제안과 동일하게 맞춥니다.

경고(중요)
- Google Ads 정책상 사용자의 중요한 결정을 방해하는 광고, 앱 종료를 가로막는 광고는 금지됩니다.
- 본 가이드는 “종료 확인” UI 안에 네이티브 Ad를 보조 요소로 함께 배치하는 패턴입니다. 닫기/종료 버튼은 항상 뚜렷해야 하며, 광고가 있어도 즉시 종료할 수 있어야 합니다.
- UMP 동의 전에는 광고 로드를 금지합니다.

---

에이전트에게 줄 프롬프트

1) 의존성과 빌드 설정
- app/build.gradle.kts
  - Google Mobile Ads SDK가 이미 추가되어 있는지 확인 (play-services-ads)
  - buildFeatures { buildConfig = true } 유지
  - buildTypes 별 BuildConfig 필드 추가/확인
    - debug: ADMOB_NATIVE_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" (Google 테스트 Native Advanced)
    - release: ADMOB_NATIVE_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx" (실 ID)

2) 네이티브 광고 로더(싱글톤)
- 파일: app/src/main/java/.../core/ads/NativeAdManager.kt
- 요구 API
  - fun preload(context: Context)
  - fun acquire(): NativeAd?  // 호출 측에서 소유권 획득(표시 후 반드시 destroy())
  - fun isLoaded(): Boolean
  - 정책(릴리즈 기준): 일일 캡(권장 1~2회), 쿨다운 2분, UMP 동의 게이팅
  - 디버그: BuildConfig.DEBUG이면 정책 우회(항상 allow), 단 테스트 유닛ID로만 로드
- 구현 요점
  - BuildConfig.ADMOB_NATIVE_UNIT_ID 사용, 값이 비었거나 플레이스홀더면 Google 테스트 ID로 폴백
  - load 성공 시 캐시 보관, acquire() 시점에 반환하고 내부 캐시는 null로 초기화 → 즉시 재-preload
  - Activity/Context 누수 방지: NativeAd는 표시 종료 시점에 반드시 nativeAd.destroy() 호출

3) 레이아웃 리소스(템플릿)
- 파일: app/src/main/res/layout/include_native_exit_ad.xml
- 구성: <com.google.android.gms.ads.nativead.NativeAdView>
  - MediaView(@+id/ad_media)
  - ImageView(@+id/ad_icon)
  - TextView(@+id/ad_headline)
  - TextView(@+id/ad_body)
  - Button(@+id/ad_call_to_action)
  - TextView(@+id/ad_advertiser) (선택)
- 뷰 바인딩 유틸: NativeViewBinder.bind(nativeAdView, nativeAd) — 필드 존재 여부 체크 후 안전하게 set

4) Compose 팝업 UI
- 파일: app/src/main/java/.../core/ui/NativeExitPopup.kt
- API
  - @Composable fun NativeExitPopup(visible: Boolean, onConfirmExit: () -> Unit, onDismiss: () -> Unit)
- 동작
  - visible=true면 Dialog(또는 ModalBottomSheet)로 표시
  - 상단에 “앱을 종료하시겠어요?” 같은 명확한 문구 + 종료/취소 버튼
  - 본문 영역에 네이티브 광고(있으면 표시, 없으면 플레이스홀더 카드)
  - 광고 영역: AndroidView(factory=...)로 include_native_exit_ad.xml inflate 후, acquire()로 받은 NativeAd를 bind
  - onDismiss/confirm 시 NativeAd.destroy() 호출 보장, 그리고 NativeAdManager.preload() 재개

5) 뒤로가기 연동
- 화면(예: StartActivity, RunActivity 등 Compose 스크린)
  - var showExit by remember { mutableStateOf(false) }
  - BackHandler(enabled = true) { showExit = true }
  - NativeExitPopup(
      visible = showExit,
      onConfirmExit = { activity.finish() },
      onDismiss = { showExit = false }
    )
  - 정책(릴리즈):
    - 이미 이번 세션에 1회 노출했다면 바로 종료로 진행(중복 노출 방지)
    - cooldown/daily cap 위반이면 showExit=false로 두고 즉시 종료 진행
  - 디버그: 정책 우회(항상 팝업 표시), 단 광고 미로드면 광고 영역은 플레이스홀더로 대체

6) UMP 동의 게이팅
- UMP 동의 완료 후에만 NativeAdManager.preload 수행
- 동의 전엔 팝업은 표시되되 광고 영역은 플레이스홀더(빈 카드)로 처리(종료/취소는 정상 동작)

7) QA 체크리스트
- 종료/취소 버튼이 광고보다 명확하고 접근 가능(터치 타겟 충분)한지
- 백버튼을 연타해도 앱 종료를 과도하게 지연시키지 않는지(최대 1회 노출/세션)
- 네트워크/광고 실패 시에도 팝업 레이아웃이 안정적으로 보이는지
- UMP 동의 전에는 광고가 로드되지 않는지
- 디버그 빌드에서 테스트 유닛ID로 로드되는지("Test Ad" 라벨 확인)

8) 정책 유의사항
- 팝업에서 광고를 누르도록 유도하는 텍스트/그래픽 금지
- 광고와 ‘종료’ 버튼을 혼동 가능하게 배치 금지
- 팝업을 닫더라도 앱 종료가 즉시 가능해야 함(추가 단계 강요 금지)

9) 적용 예(의사코드)
```kotlin
// BackHandler in Compose screen
var showExit by remember { mutableStateOf(false) }
BackHandler(true) { showExit = true }
NativeExitPopup(
    visible = showExit,
    onConfirmExit = { activity.finish() },
    onDismiss = { showExit = false }
)
```

부록: 간단 빌드/실행
- 디버그 빌드: gradlew :app:assembleDebug
- 릴리즈 빌드: gradlew :app:assembleRelease (실 유닛ID 교체 필수)

