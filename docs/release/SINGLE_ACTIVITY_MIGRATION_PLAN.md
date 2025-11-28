# Single-Activity 전환 및 상단 고정 AdmobBanner 마이그레이션 계획

목적
- 앱을 Single-Activity 구조로 전환하여 최상단에 한 번만 생성되는 `AdmobBanner`를 배치한다. 이를 통해 화면 전환 시 배너가 반복적으로 재로딩되는 문제를 해소한다.

요약
- 현재: 여러 Activity에서 각각 `AdmobBanner()`를 호출해 화면 전환 시마다 AdView가 생성·로드됨
- 목표: `MainActivity`(또는 앱 호스트 Activity) 하나에 `BaseScaffold`/`BaseScreen` 역할의 공통 레이아웃을 두고, 하단(또는 상단)에 `AdmobBanner`를 한 번만 배치한다. 내부 화면 전환은 Compose Navigation(NavHost)으로 처리한다.

체크리스트(우선순위)
1. 화면(기존 Activity) 목록 작성 및 우선순위 지정
2. `BaseScaffold`(공통 레이아웃) 구현 — `AdmobBanner`는 여기 한 번만 호출
3. `MainActivity` 생성 및 임시 NavHost 연결
4. 화면별 Composable 추출 및 Nav Graph 연결(우선 Start, Run 등 주요 화면)
5. 기존 Activity 호출들을 점진적으로 Nav 호출로 대체
6. Manifest에서 LAUNCHER를 `MainActivity`로 전환(검증 후)
7. 정리: 불필요 Activity 제거, 문서화, QA

Phase 0 — 준비 및 조사
- 목표: 전환 범위와 리스크 파악
- 할 일
  - 프로젝트 내 Activity 목록 정리(예: StartActivity, RunActivity, SettingsActivity, RecordsActivity, AboutActivity, QuitActivity 등)
  - 각 Activity에서 `setContent` 내부에 정의된 UI 블록(Composable로 추출해야 할 부분) 목록화
  - 의존성 및 앱스코프로 옮겨야 할 부분(예: DebugAdHelper, UMP 동의 처리, SharedPreferences 초기화 등) 식별
- 산출물: 화면 목록 + 우선순위(권장: Start/Run 우선)
- 검증: 누락 화면 및 외부 인텐트 처리 케이스 확인

Phase 1 — `BaseScaffold` 설계 및 `AdmobBanner` 상단 고정 (권장 첫 작업)
- 목표: 앱 공통 레이아웃 구현 및 배너 한 번만 생성 보장
- 변경 파일(제안)
  - 새 파일: `app/src/main/java/.../core/ui/BaseScaffold.kt`
  - 새 파일: `app/src/main/java/.../core/ui/MainNavGraph.kt` (선택적)
- 구현 포인트
  - `@Composable fun BaseScaffold(content: @Composable (NavController) -> Unit)` 형태로 설계
  - 공통 요소(Theme, Drawer, TopAppBar 등)는 BaseScaffold로 통합
  - `AdmobBanner()`는 BaseScaffold 내에서 단 한 번만 호출(Scaffold의 bottom slot 또는 BottomBar 영역과 별도로 상위에 배치)
  - `NavHost`는 BaseScaffold의 content(중앙 영역)에서 렌더링
  - `DebugAdHelper`나 배너 숨김 로직은 BaseScaffold가 담당
- 검증
  - 임시 `MainActivity`에서 BaseScaffold 호출 후 앱 실행
  - Logcat에서 `AdmobBanner`가 한 번만 초기화/로드되는지 확인
- 리스크/대응
  - 기존 Activity-scoped 설정(WindowCompat 등)은 MainActivity 쪽으로 옮기기

Phase 2 — `MainActivity` 생성 및 NavHost 연결
- 목표: 앱을 호스트할 MainActivity 추가 및 NavHost 기본 구성
- 변경 파일
  - 새 파일: `app/src/main/java/.../MainActivity.kt`
  - (임시) `AndroidManifest.xml` - LAUNCHER 전환은 점진적으로 수행
- 구현 포인트
  - `MainActivity.onCreate`에서 `setContent { BaseScaffold { navController -> NavHost(...) } }`
  - 시스템 Back 처리: `onBackPressedDispatcher` 또는 `NavController`와 연동
  - 외부 인텐트/딥링크는 MainActivity에서 수신 후 Nav로 포워딩
- 검증
  - 앱을 MainActivity로 실행 → 화면 렌더링 확인
  - `AdmobBanner` 로그가 1회인지 확인

Phase 3 — 화면별 Composable 추출 및 Nav 연결 (점진적)
- 목표: 기존 Activity UI를 Composable로 옮기고 NavGraph에 연결
- 우선순위 권장: Start → Run → Settings → Records → About → 기타
- 각 화면 적용 단계
  1. 기존 Activity의 `setContent { BaseScreen { ... } }` 내부 UI를 `@Composable fun XScreen(navController: NavController)`로 추출
  2. 화면 고유의 ViewModel/데이터 의존성은 파라미터 주입 또는 `hiltViewModel()` 사용
  3. `Intent` 인자(예: skip_splash) → Nav arguments로 매핑
  4. 기존 Activity에서 내부 네비게이션을 수행하던 `startActivity(...)` 호출을 우선 주석 처리하고 `navController.navigate(...)`로 대체(점진적 전환)
- 검증
  - 각 스크린을 Nav로 진입했을 때 UI/상태/입력 동작이 기존과 동일한지 테스트
  - `AdmobBanner`가 재로딩되지 않는지 로그 확인

Phase 4 — 앱 내 Activity 호출을 Nav로 전환(대규모)
- 목표: 내부 화면 전환을 Nav로 통일하여 Activity 전환 최소화
- 할 일
  - 전체 코드에서 `startActivity()` 호출 위치 검색
  - 외부 진입(알림, 외부 딥링크 등) 케이스는 MainActivity로 포워딩
  - popUpTo, popUpToInclusive 등 Nav 옵션으로 기존 FLAG_* 동작 맵핑
- 검증
  - Drawer 메뉴, 뒤로가기 스택, 외부 인텐트 시나리오 테스트
  - AdBanner 로드 로그 재확인

Phase 5 — Manifest 정리 및 최종 전환
- 목표: MainActivity를 LAUNCHER로 변경하고 불필요한 Activity 정리
- 할 일
  - `AndroidManifest.xml`에서 LAUNCHER intent-filter를 MainActivity로 이동
  - 기존 Activity는 단계적으로 disabled 또는 제거(호환성 필요 시 주석/비활성화)
  - AdMob/UMP 초기화 지점(현재 Application or Activity)에 대한 검토 및 이동
- 검증
  - 앱 재빌드 및 실행, 모든 네비게이션 시나리오 정상 동작
  - Banner 재로딩 없음

Phase 6 — 정리, QA, 문서화
- 목표: 메모리 누수 검사, 성능/정책 체크, 최종 문서화
- 할 일
  - 불필요한 클래스를 정리
  - LeakCanary로 메모리 누수 확인(AdView 포함)
  - 광고 정책(AdMob) 준수 여부 점검
  - QA 시나리오(네트워크 불안정, 동의 지연, 회전, 백그라운드 복귀) 테스트
- 검증
  - Lint, 빌드 통과
  - 수동 QA 체크리스트 통과

핵심 구현 노하우
- `AdmobBanner`는 BaseScaffold 상위에 한 번만 배치
- 화면 상태는 ViewModel로 관리(Compose 상태는 화면 단에서만 유지)
- 외부 Intent → Nav 매핑 시 startDestination과 route 인자 정의에 신경 쓸 것
- UMP 동의: Application 또는 MainActivity에서 한 번 처리하고, `AdmobBanner`는 `canRequestAds()` 상태에 따라 요청

로그(검증용)
- Log 태그
  - AdBanner: `AdmobBanner` (이미 코드에 존재)
  - BaseActivity: `BaseActivity` (현재 로그 확인용)
- 예시 명령 (개발 환경에서)

```bash
# Android 디바이스 연결 후
adb logcat -s AdmobBanner BaseActivity
```

롤백 및 위험 관리
- 점진적 적용: 각 화면을 옮길 때마다 기능을 확인하므로 대규모 실패 위험이 낮음
- 만약 문제가 발생하면:
  - 즉시 Manifest LAUNCHER 변경을 되돌려 기존 Activity 구조로 복귀
  - 문제 화면만 임시로 기존 Activity 호출을 유지

예상 소요(대략)
- 전체 전환(모든 Activity → Compose Navigation): 3~7일(화면 수와 복잡도에 따라)
- 핵심 화면(Start/Run)만 우선 마이그레이션: 1~2일

다음 단계(권장)
1. 제가 바로 Step 1(또는 Phase 1: `BaseScaffold` + 간단한 `MainActivity` 템플릿) 파일을 생성하고 로컬에서 `AdmobBanner`가 한 번만 로드되는지 로그로 확인해 드리겠습니다. 이 작업은 기존 Activity들을 그대로 두고 병렬로 추가할 수 있습니다.
2. 이후 StartScreen을 Nav로 옮기는 작업을 진행합니다.

원하시면 바로 Phase 1 구현(파일 생성 및 간단한 MainActivity 템플릿 삽입)을 시작하겠습니다. 어떤가요?

---
작성자: 자동 생성 마이그레이션 계획
생성일: 2025-11-11

