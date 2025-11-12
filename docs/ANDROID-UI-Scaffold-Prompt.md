# Android UI 스캐폴드 프롬프트 (Compose + Navigation + Hilt)

수정일: 2025-11-12  
목적: 최상단 배너광고, 중앙 컨텐츠, 하단 Bottom Navigation, 내부 스크린/상태관리까지 일관된 구조를 다른 앱에 신속히 재사용하기 위한 LLM 프롬프트 템플릿 제공.

---

## LLM에 붙여넣을 프롬프트 본문

아래 요구사항을 만족하는 Android(App) UI/아키텍처 코드를 Jetpack Compose + Navigation + ViewModel(Hilt)로 작성해 주세요.

- 레이아웃 구조: 최상단 고정 배너광고 영역(고정 높이), 중앙 가변 컨텐츠(Box), 하단 고정 Bottom Navigation Bar
- 배너광고: AdMob 예시(테스트용), 재구성 최소화(key 기반 remember), 회전/다크모드 대응, 로딩 스켈레톤, 실패 시 placeholder
- Bottom Navigation: 3~5개 탭(Home, Feed, Profile 등), 아이콘+라벨, 선택 탭 강조(색/opacity), 백스택 상태 복원
- 네비게이션: Single Activity, rememberNavController + NavHost, 각 화면은 composable, 딥링크 가능 구조
- Destination 정의: sealed class Screen(route:String, icon, label). routes 상수화, 새 탭 추가 확장 용이
- 상태관리: 각 화면 ViewModel. UI State(immutable data class) + Intent(사용자 액션) + Reducer(또는 단순 함수) 패턴
- 의존성 주입: Hilt. Repository 인터페이스/구현 분리. 재구성 시 ViewModel state 보존(멱등)
- 테마/접근성: Material3 다크/라이트, System bars 색상 동기화, contentDescription, 최소 터치 영역 48dp
- 광고 성능: key/remember로 재구성 최소화. 실패 시 placeholder(Text/Box) 표시
- 테스트: ViewModel 단위 테스트(state 변화 시나리오), Navigation route 파싱 테스트
- 코드 구조: ui/components, ui/navigation, ui/screens, domain, data 패키지로 분리
- 빌드/의존성: Compose BOM, Navigation, Hilt 등 필요 종속성 명시

제공할 항목(파일 단위 예시 코드 포함):
1) sealed class `Screen`
2) `BottomNavBar` 컴포저블
3) `BannerAd` 컴포저블(AdMob 테스트 ID 사용)
4) `AppScaffold` 루트(Top/Bot 바 포함)
5) `NavGraph` 설정 함수
6) 샘플 화면 2~3개(`HomeScreen`, `FeedScreen`, `ProfileScreen`)
7) Sample `ViewModel` + `UIState` + `Intent` 예시
8) `Repository` 인터페이스 + Fake 구현
9) 간단한 unit test 예시(JUnit + Turbine/Coroutine)

세부 구현 지침:
- Navigation: `launchSingleTop = true`, `restoreState = true`, `popUpTo(startDestination) { saveState = true }`
- ViewModel: `StateFlow` 기반. 로딩/성공/에러 3상태를 `runCatching` 또는 Result로 처리. 진입점은 `dispatch(Intent)`
- 테스트: 성공/에러/재시도 시나리오. Turbine으로 `StateFlow` 검증
- 확장성: 탭 추가 시 `Screen.bottomItems`만 수정해 네비/바 반영
- 접근성: 아이콘 `contentDescription`, 포커스/대비 고려

최종 출력 형식:
- 간단 설명 → 전체 예시 코드(필요 파일) 순
- 주석은 간결하게, 불필요한 장황함 제거
- 패키지 구조(경로)는 주석으로 병기

---

## 복사용 프롬프트 (그대로 붙여넣기)

아래 요구사항을 만족하는 Android(App) UI/아키텍처 코드를 Jetpack Compose + Navigation + ViewModel(Hilt)로 작성해 주세요.

- 레이아웃 구조: 최상단 고정 배너광고 영역(고정 높이), 중앙 가변 컨텐츠 영역(Box), 하단 고정 Bottom Navigation Bar  
- 배너광고: AdMob 예시(테스트용), 재구성 최소화(key 기반 remember), 화면 회전/다크모드 대응, 로딩 스켈레톤, 실패 시 placeholder  
- Bottom Navigation: 3~5개 탭 (Home, Feed, Profile 등). 아이콘 + 라벨. 선택 탭 강조(색상/opacity). 백스택 상태 복원  
- 네비게이션: Single Activity, rememberNavController + NavHost. 각 화면은 composable. 딥링크 가능 구조  
- Destination 정의: sealed class Screen(route: String, icon, label). routes 상수화  
- 상태관리: 각 Screen 별 ViewModel. UI State(immutable data class) + Intent(사용자 액션) + Reducer(또는 단순 함수) 패턴  
- 의존성 주입: Hilt. Repository 인터페이스/구현 분리. 재구성 시 ViewModel state 유지(멱등)  
- 광고 View는 재구성 최소화: key 기반 remember. 실패 시 placeholder 표시  
- 다크/라이트 테마 지원(Material3). System bars 색상 동기화.  
- 접근성: contentDescription, 최소 터치 영역(48dp)  
- 테스트: 각 ViewModel에 단위 테스트 (state 변경 시나리오). Navigation route 파싱 테스트.  
- 코드 분리: ui/components, ui/navigation, ui/screens, domain, data 패키지.  
- 제공할 항목:  
  1. sealed class Screen  
  2. BottomNavBar 컴포저블  
  3. BannerAd 컴포저블(AdMob placeholder)  
  4. AppScaffold 루트  
  5. NavGraph 설정 함수  
  6. 샘플 2~3개 화면(HomeScreen, FeedScreen, ProfileScreen)  
  7. Sample ViewModel + UIState + Intent 예시  
  8. Repository 인터페이스 + Fake 구현  
  9. 간단한 unit test 예시(JUnit + Turbine/Coroutine)  

세부 구현 지침:  
- Navigation: `launchSingleTop = true`, `restoreState = true`, `popUpTo(startDestination) { saveState = true }`  
- ViewModel: `StateFlow` 기반, `runCatching`로 로딩/성공/에러 3상태 처리, `dispatch(Intent)` 진입점  
- 테스트: 성공/에러/재시도 시나리오, Turbine으로 `StateFlow` 검증  
- 접근성: 아이콘 `contentDescription`, 최소 터치 영역 보장  
- 확장성: 탭 추가 시 `Screen.bottomItems`만 수정해 네비게이션/BottomBar 반영  

최종 출력: 위 구조의 전체 예시 코드(필요 파일), 간단 설명 먼저 후 코드. 주석 간결. 불필요한 장황한 문장 제거.

---

## 체크리스트

- [ ] `Screen` sealed class와 `bottomItems` 목록 정의
- [ ] `BannerAd`(AdMob 테스트 ID) & 실패 placeholder
- [ ] `BottomNavBar`(상태 복원, 선택 강조)
- [ ] `AppScaffold`(topBar/bottomBar/컨텐츠 padding)
- [ ] `NavHost` startDestination & 각 화면 route
- [ ] 각 화면 ViewModel(로딩/성공/에러) & Fake Repository
- [ ] JUnit + Turbine 테스트 샘플
- [ ] Material3 테마 & System bars 동기화
- [ ] 접근성 속성 적용

---

## 사용 방법

1) 본 문서의 “복사용 프롬프트” 전체를 복사  
2) 새로운 앱/모듈에서 LLM에 붙여넣고 실행  
3) 생성된 코드의 패키지명/리소스/의존성만 프로젝트에 맞게 조정

Tip: 광고 유닛은 운영 시 실제 ID로 교체하고, 테스트에선 Google 테스트 ID 사용.

---

## 참고 메모 (선택)

- 의존성 템플릿: Compose BOM, navigation-compose, hilt-android, hilt-navigation-compose, lifecycle-runtime-ktx, activity-compose 등.
- 네비게이션 상태 복원: 동일 탭 재탭 시 `launchSingleTop`/`restoreState`/`popUpTo` 조합 확인.
- 배너 광고: AndroidView로 감싸고 `AdView`/`AdRequest` 사용. 테스트 ID: `ca-app-pub-3940256099942544/6300978111`.

