Debug TestActivity 사용 및 테스트 가이드

목적
- 개발(디버그) 환경에서 Firebase(Analytics / Crashlytics / Performance) 연동을 빠르게 검증하기 위한 TestActivity(디버그 전용)를 별도 문서로 정리합니다.

전제 조건
- app/src/debug/google-services.json이 존재하고, 디버그 빌드로 실행해야 합니다.
- Firebase SDK(Analytics, Crashlytics, Performance)가 Gradle 의존성에 포함되어 있어야 합니다. (현재 프로젝트에 포함됨)
- 테스트 기기 또는 에뮬레이터 사용 권장.

TestActivity 개요(버튼 동작)
- 기능 3 (Analytics 이벤트 전송)
  - 동작: Firebase Analytics에 이벤트(debug_test_event)를 전송.
  - 즉시 피드백: 화면 Toast, Logcat(FA 태그), Firebase Console → Analytics → DebugView에서 확인.

- 기능 4 (Crashlytics non-fatal 보고)
  - 동작: FirebaseCrashlytics.getInstance().recordException(Exception("Debug non-fatal")) 호출.
  - 피드백: Toast, Logcat, Firebase Console → Crashlytics의 Non-fatal 섹션에서 확인(지연 있음).

- 기능 5 (Performance trace)
  - 동작: 커스텀 trace("debug_trace") 시작 → 약 1.5초 후 stop.
  - 피드백: Toast, Firebase Console → Performance → Traces(처리 지연 있음).

안전제약
- TestActivity와 Debug 메뉴는 BuildConfig.DEBUG 체크로 디버그에서만 노출 및 동작하도록 구현되어야 합니다.
- 절대 릴리즈에 포함시키지 마세요. (NavGraph 진입 차단 권장)
- 이벤트 파라미터에 PII(이메일, 전화번호, 기기 고유 식별자 등)를 포함하지 마세요.

테스트 절차(단계별)
1) Analytics Debug 활성화
   - 목적 및 설명: adb shell setprop debug.firebase.analytics.app 명령은 단순히 디버그 빌드로 실행하는 것과는 별개로, 해당 앱의 Firebase Analytics SDK를 '디버그 전송' 모드로 강제합니다. 이 모드는 이벤트를 DebugView에 거의 실시간으로 표시하도록 하고, 샘플링을 우회하며 디버그 관련 로그를 활성화해 빠르게 연동 상태를 확인할 수 있게 합니다.

   - 왜 필요한가?
     - BuildConfig.DEBUG는 앱이 디버그 빌드로 컴파일되었는지를 나타내는 플래그입니다(코드 수준의 분기 제어용). 그러나 Analytics의 실시간 DebugView는 기기 쪽에서 디버그 전송 모드를 활성화해야 즉시 이벤트를 보여줍니다.
     - 디버그 빌드로 실행만 하는 경우 서버 처리 지연, 샘플링, 또는 백엔드 집계 때문에 이벤트가 즉시 보이지 않을 수 있습니다. 따라서 DebugView로 빠르게 검증하려면 adb setprop로 Analytics 디버그 모드를 켜야 합니다.

   - 사용법 (예)
     - 활성화:
       adb shell setprop debug.firebase.analytics.app <디버그_패키지명>
       예: adb shell setprop debug.firebase.analytics.app kr.sweetapps.alcoholictimer.debug
       - 적용: 명령 실행 후 앱 프로세스를 재시작해야 SDK가 새로운 설정을 인식합니다(앱 재시작은 적용 목적).
         예:
           adb shell am force-stop <패키지명>
           adb shell monkey -p <패키지명> -c android.intent.category.LAUNCHER 1
         또는 IDE에서 앱을 다시 실행하세요.

  - 재부팅 vs 앱 재시작 (추가 설명)
    - 요지: setprop으로 설정한 값은 '기기(시스템) 속성'을 변경합니다. 이 속성은 앱 프로세스 재시작으로는 제거되지 않으며, 일반적으로 기기 재부팅 시 초기화됩니다. 즉, '앱 재시작'은 변경 적용(apply) 목적이고, '재부팅'은 속성을 초기화(해제)하는 동작입니다.

    - 적용(설정 후 즉시 사용하려면):
      1) 속성 설정: adb shell setprop debug.firebase.analytics.app <패키지명>
      2) 앱 재시작(권장):
         adb shell am force-stop <패키지명>
         adb shell monkey -p <패키지명> -c android.intent.category.LAUNCHER 1
         - 또는 IDE에서 앱을 다시 실행(앱 프로세스가 재시작되어야 SDK가 새로운 속성을 읽음)

    - 완전 해제(속성 제거):
      - 즉시 제거: adb shell setprop debug.firebase.analytics.app .
      - 또는 기기 재부팅: adb reboot  (재부팅하면 대부분의 비영구 시스템 속성이 초기화됨)

    - AVD(에뮬레이터) 특이사항:
      - AVD를 단순히 닫았다가 다시 여는 것만으로는 OS 레벨 속성이 초기화되지 않을 수 있습니다(스냅샷 복원/Cold Boot 설정에 따라 다름).
      - 안전하게 초기화하려면 AVD Manager에서 Cold Boot을 수행하거나 adb reboot을 사용하세요. 필요 시 AVD의 wipe-data로 초기화할 수도 있습니다.

    - 상태 확인명령:
      adb shell getprop debug.firebase.analytics.app

    - 요약 권장 워크플로우:
      - 빠른 확인(로컬): setprop → force-stop → 앱 재시작 → DebugView 확인
      - 점검 종료(권장): setprop . 로 비활성화 또는 adb reboot / AVD Cold Boot
      - 전용 테스트 기기 사용 권장 (공유 기기에서 장기간 켜두지 마세요)

   - 비활성화:
     adb shell setprop debug.firebase.analytics.app .
     - 점검 종료 후 반드시 비활성화(또는 기기 재부팅)하세요.

   - 기대 결과
     - Logcat에서 FA / FA-SVC 관련 로그가 더 자세히 출력됩니다.
     - Firebase Console → Analytics → DebugView에서 이벤트가 거의 실시간으로 표시됩니다.

2) Logcat 준비
   - 추천 필터 태그: FA, FA-SVC, FirebaseCrashlytics, FirebasePerformance
   - 주의: 개발자 기기가 여러 대 연결된 경우 adb는 대상 장치를 지정해야 합니다. 앞으로 예시는 항상 사용자의 장치 ID인 emulator-5554 를 포함합니다.
   - 예 (Windows, 특정 장치):
     adb -s emulator-5554 logcat -v time | findstr /R "FA FA-SVC FirebaseCrashlytics FirebasePerformance"
   - 예 (macOS/Linux, 특정 장치):
     adb -s emulator-5554 logcat -v time | grep -E "FA|FA-SVC|FirebaseCrashlytics|FirebasePerformance"
   - 장치 목록 확인 및 ID 조회:
     adb devices
     (출력에서 테스트할 장치 ID를 확인하고 -s <DEVICE_ID> 위치에 사용하세요. 예: emulator-5554)

3) TestActivity 접속
   - 앱 About → Debug 메뉴 → Debug Screen 또는 네비게이션으로 Debug 라우트 진입
   - Debug 메뉴가 보이지 않으면 BuildConfig.DEBUG가 false 이거나 AboutScreens에서 Debug 항목이 숨겨진 것임

4) 기능 3 테스트(Analytics)
   - 스위치(기능 3) 켜기
   - 즉시: 화면에 Toast 표시
   - 확인: DebugView에서 debug_test_event 이벤트 수신 확인
   - Logcat: "Logging event" 또는 FA 관련 로그 확인

5) 기능 4 테스트(Crashlytics non-fatal)
   - 스위치(기능 4) 켜기
   - 즉시: Toast 표시
   - Console: Crashlytics → Non-fatal에서 항목 확인(수 분 소요 가능)
   - Logcat: FirebaseCrashlytics 전송 로그

6) 기능 5 테스트(Performance trace)
   - 스위치(기능 5) 켜기
   - 즉시: Toast 표시, 내부적으로 1.5초 trace 실행
   - Console: Performance → Traces에서 반영 확인(처리 지연)

5-1) Crashlytics(비치명) 전송 확인 및 Logcat 해석 (중요)
- 핵심 요약
  - Crashlytics의 non-fatal(비치명) 리포트는 Analytics DebugView에 표시되지 않습니다. Crashlytics 전용 콘솔에서 확인하세요.
  - 앱에서 recordException()을 호출하면 앱 로그에는 기록이 남고, SDK가 업로드를 시도합니다. 콘솔 반영은 지연될 수 있습니다.

- 빠른 Logcat 검사(항상 사용자 장치 emulator-5554 사용 예시)
  - ViewModel 호출 로그 확인:
    adb -s emulator-5554 logcat -v time | findstr /R "DebugScreenVM"
  - Crashlytics SDK 업로드/에러 로그 확인:
    adb -s emulator-5554 logcat -v time | findstr /R "CrashlyticsCore|FirebaseCrashlytics|Uploading|Crashlytics"
  - 전체 로그 파일로 저장(전달/분석용):
    adb -s emulator-5554 logcat -v time > logcat_emulator-5554.txt
    (캡처 후 Ctrl+C로 중지, 관련 라인 복사해서 공유)

- 로그 예시 및 해석
  - 예: 
    11-29 03:07:48.742 D/DebugScreenVM(16431): Recorded non-fatal exception to Crashlytics
    11-29 03:07:48.743 D/DebugScreenVM(16431): Requested sendUnsentReports()
  - 의미: ViewModel에서 recordException 호출이 정상적으로 실행되었고, sendUnsentReports()로 즉시 전송을 시도했음을 뜻합니다.
  - 다음으로 확인할 항목:
    1) Crashlytics SDK 로그(Uploading, CrashlyticsCore 등)가 이어지는지 확인 — 업로드 성공/실패 로그가 출력되어야 함.
    2) 네트워크/권한 오류(HTTP 4xx/5xx 등)가 있는지 확인.
    3) 콘솔에 반영되지 않았더라도 위 로그가 있다면 서버로 전송 시도는 이뤄진 상태이므로 몇 분 기다려 보세요.

- Firebase 콘솔에서 확인 위치
  - Firebase Console → Crashlytics → Issues → Non‑fatal
  - 반영 지연: 보통 수 분(5~15분) 소요. 콘솔에서 검색/필터를 사용해 앱 인스턴스 또는 메시지 텍스트로 찾으세요.

- 추가 점검 포인트(문제가 계속되면)
  - debug google-services.json이 올바른 프로젝트(sweetapps-dev-a3a03)를 가리키는지 재확인
  - AndroidManifest나 코드에서 Crashlytics 수집이 비활성화되지 않았는지 확인(android:name="firebase_crashlytics_collection_enabled")
  - 기기(AVD)가 인터넷에 연결되어 있는지 확인
  - 로그에 Upload 실패나 인증 오류가 있으면 그 메시지를 복사해 공유해 주세요

- Crashlytics 콘솔에 'Issues / Non‑fatal' 탭이 보이지 않을 때 (문제 해결)

  1) Firebase 콘솔에서 프로젝트를 올바르게 선택했는지 확인
     - google-services.json의 project_id 값을 확인하세요. 예시: "sweetapps-dev-a3a03".
     - 콘솔에서 상단 프로젝트 드롭다운이 동일한 프로젝트인지 반드시 확인합니다.
     - 빠른 이동 URL 예시: https://console.firebase.google.com/project/sweetapps-dev-a3a03/crashlytics
       (프로젝트 ID를 위 URL의 project/ 뒤에 넣어 직접 접근해 보세요.)

  2) Crashlytics 제품이 해당 Firebase 프로젝트에서 활성화되어 있는지 확인
     - Firebase 콘솔 왼쪽 메뉴에서 "Build" → "Crashlytics" 항목이 있어야 합니다.
     - 처음 설정되지 않았다면 콘솔에 "Get started" 또는 "Setup Crashlytics" 안내가 보입니다. 화면 지시에 따라 SDK/플러그인 적용 후 앱을 실행하여 등록하세요.
     - 이미 SDK를 추가했어도 콘솔에서 설정 완료 버튼을 누르지 않으면 대시보드가 생성되지 않을 수 있습니다.

  3) SDK 업로드 로그 확인 (디바이스 지정: emulator-5554)
     - ViewModel에서 전송 로그는 확인하셨습니다. 다음으로 Crashlytics SDK가 실제로 업로드를 시도했는지 로그에서 찾아보세요:
       adb -s emulator-5554 logcat -v time | findstr /R "CrashlyticsCore|FirebaseCrashlytics|Uploading|Crashlytics|upload"
     - 업로드 성공 로그(또는 HTTP 오류 메시지)를 확인하면 원인 진단에 도움이 됩니다.

  4) 콘솔 반영 지연 및 검색 팁
     - non‑fatal 이슈는 콘솔에 반영되기까지 수 분(5~15분) 이상 걸릴 수 있습니다. 반영 직후 바로 보이지 않더라도 10~15분 후 재확인하세요.
     - 콘솔에서 검색 시 메시지 텍스트(예: "Debug non-fatal")나 앱 버전 필터를 사용해 보세요.

  5) 앱/프로젝트 설정 점검
     - debug google-services.json이 올바른 프로젝트를 가리키는지 확인(파일의 project_info.project_id).
     - AndroidManifest 또는 코드에 firebase_crashlytics_collection_enabled=false 같은 항목이 없는지 확인(있다면 true 또는 제거).
     - FirebaseApp.initializeApp(this) 가 앱 시작 시 호출되는지(이미 MainApplication에서 호출되는지 확인됨).

  6) 권한/네트워크 문제
     - 테스트 기기가 인터넷에 연결되어 있는지 확인하세요(에뮬레이터의 네트워크 연결 유무).
     - 사내 프록시나 방화벽이 업로드를 차단하지 않는지 점검(HTTP 403/401/5xx 에러 확인).

  7) 추가 조치(문제가 계속될 때)
     - 로그 전체를 파일로 저장해서 공유하면 분석에 도움이 됩니다:
       adb -s emulator-5554 logcat -v time > logcat_emulator-5554.txt
       (Ctrl+C로 중단 후 관련 Crashlytics 라인만 발췌해 전달)
     - 콘솔에 아무 UI가 보이지 않는다면 다른 브라우저나 시크릿 창에서 다시 로그인해 보세요(권한/캐시 이슈 가능).
     - 필요 시 치명적(crash) 테스트로 동작을 확인할 수 있으나, 데이터 손실 우려가 있으므로 주의해서 전용 테스트 기기에서만 수행하세요.

추가 확인 포인트
- google-services.json 내 client.android_client_info.package_name이 BuildConfig.APPLICATION_ID(디버그)와 일치하는지 확인
- 디버그가 아닌 경우(릴리즈) TestActivity가 보이지 않거나 performAction이 호출되더라도 내부에서 무시되도록 BuildConfig.DEBUG로 보호되어 있어야 함
- NavGraph에 Debug 라우트가 등록되어 있으면 추가로 진입 체크(BuildConfig.DEBUG)로 라우트 접근을 차단하면 안전함

문제 해결 팁
- 이벤트가 DebugView에 안 뜰 때
  1. adb로 debug.analytics.app 설정 확인(또는 기본값으로 리셋)
  2. Logcat에서 FA 태그 로그가 나오는지 확인
  3. google-services.json의 gmp_app_id와 package_name 확인

- Crashlytics 리포트가 안 올라올 때
  1. 네트워크 연결 확인
  2. Logcat에서 FirebaseCrashlytics 관련 에러 확인
  3. 콘솔 반영 지연 가능성(몇 분에서 10분 이상)

문서 저장 위치
- 이 파일: docs/reference/DebugTestActivity_사용_및_테스트_가이드.md

원하시면 이 문서를 기반으로 TestActivity를 별도 Activity로 생성(코드/레이아웃)하거나 NavGraph 진입 차단 코드를 추가해 드립니다. 원하는 작업을 한 줄로 알려 주세요: "Activity 생성", "NavGraph 차단 추가", 또는 "문서만".
