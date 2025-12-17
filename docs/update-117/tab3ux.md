# 🚀 [수정된 로드맵] 하단 탭 3개로 축소 및 대시보드화

## 📌 Phase 1: 네비게이션 구조 변경 (탭 삭제 & 상세 페이지화)

현재 4개인 탭에서 **Tab 3(Level)를 제거**하여 3개로 만들고, 기존 레벨 화면을 **'슬라이드 상세 페이지'**로 전환합니다.

**[프롬프트 복사]**

***

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 Jetpack Compose 네비게이션 아키텍처 전문가입니다.

**목표:** 현재 4개(Tab 1, 2, 3, 4)로 구성된 하단 네비게이션 바에서 **Tab 3 (Level) 항목을 제거**하고, 최종적으로 **3개의 탭 (Tab 1: Timer, Tab 2: Record, Tab 4: Community)**만 남기려고 합니다. 기존 Tab 3 화면은 **'레벨 상세 페이지'**로 변환하여 보존합니다.

**상세 요구사항:**

1.  **Bottom Navigation 수정:**
    * `Screen` Sealed Class 또는 네비게이션 아이템 리스트에서 **Tab 3 (Level) 항목을 제거**하세요.
    * 하단 바(`NavigationBar`)에는 이제 [Tab 1, Tab 2, Tab 4] 총 **3개의 아이템**만 표시되어야 합니다.

2.  **Navigation Graph 수정 (`AppNavHost` - Root Level):**
    * 기존의 `addTab03Graph` 호출을 제거하거나 주석 처리하세요.
    * 대신, **`Screen.LevelDetail`**이라는 새로운 라우트를 `AppNavHost`(최상위)에 정의하세요.
    * **애니메이션:** 이 화면은 Tab 2에서 진입하므로, **오른쪽에서 왼쪽으로 슬라이드(`slideInHorizontally`)** 되며 전체 화면을 덮도록 설정하세요. (이전에 구현한 `Settings` 화면과 동일한 방식).
    * **UI 구성:** 기존 `LevelScreen` 내용을 그대로 사용하되, **뒤로 가기 버튼이 있는 `Scaffold` (TopAppBar 포함)**로 감싸야 합니다. (Title: "나의 레벨").

**출력 요청:**
* 3개로 줄어든 `BottomNavigationBar` 아이템 설정 코드
* `AppNavHost`에 추가된 `LevelDetail` 라우트 코드
* `LevelDetailScreen` (Wrapper) 코드

**[프롬프트 끝]**

***

---

## 📌 Phase 2: '레벨 요약 배너' 컴포넌트 제작 (UI)

탭 2 상단에 붙일 **요약 카드**를 만듭니다.

**[프롬프트 복사]**

***

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 Jetpack Compose UI 디자이너입니다.

**목표:** Tab 2 (Record) 화면 최상단에 배치할 **`LevelSummaryBanner`** 컴포넌트를 구현해 주세요. 사라진 Tab 3의 핵심 정보를 요약해서 보여주는 배너입니다.

**디자인 요구사항:**

1.  **레이아웃:**
    * 높이: 약 `100.dp` ~ `120.dp` (컴팩트하게).
    * 배경: `Brush.horizontalGradient`를 사용하여 **파란색 계열(MainPrimaryBlue 등)**의 그라데이션을 적용하세요.
    * 형태: 모서리가 둥근 카드 (`RoundedCornerShape(16.dp)`).
    * `clickable` 속성을 주어 전체 클릭이 가능해야 합니다.

2.  **내부 구성:**
    * **좌측 상단:** 레벨 아이콘(작게) + 텍스트 "Lv.1 알코올 스톱" (흰색, Bold).
    * **우측 상단:** "전체 보기 >" 텍스트 (흰색, 작게).
    * **중앙 하단:**
        * 가로로 긴 `LinearProgressIndicator` (흰색 트랙, 노란색 진행 바).
        * 진행 바 아래에 "다음 레벨까지 3일 남음" 같은 상태 텍스트.

**출력 요청:**
* `LevelSummaryBanner` Composable 함수 코드
* Preview 코드

**[프롬프트 끝]**

***

---

## 📌 Phase 3: 탭 2 (Record) 화면 통합 (Integration)

만들어진 배너를 **탭 2의 최상단**에 배치하고 연결합니다.

**[프롬프트 복사]**

***

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 Jetpack Compose UI 전문가입니다.

**목표:** Phase 2에서 만든 `LevelSummaryBanner`를 **Tab 2 (RecordScreen)**의 최상단에 배치하여 대시보드 형태로 개편해 주세요.

**상세 요구사항:**

1.  **화면 구조 변경 (RecordScreen):**
    * `RecordScreen`의 메인 `Column` (스크롤 영역)의 **가장 첫 번째 요소**로 `LevelSummaryBanner`를 추가하세요.
    * **순서:** `[LevelSummaryBanner]` -> `[DateFilter]` -> `[StatisticsCards]` -> `[DiaryList]`

2.  **데이터 및 네비게이션 연결:**
    * 배너 클릭 시(`onClick`), Phase 1에서 만든 **`Screen.LevelDetail` 라우트로 이동**하도록 `navController.navigate()`를 연결하세요.
    * ViewModel을 통해 현재 사용자의 레벨 정보를 배너에 전달하세요.

**출력 요청:**
* 수정된 `RecordScreen` 전체 구조 코드

**[프롬프트 끝]**

***

### 👨‍🏫 전문가의 조언

이 작업이 끝나면 선생님의 앱은 **[홈 - 기록 - 커뮤니티]** 딱 3개의 탭만 남게 됩니다.
사용자는 **"앱이 정말 심플하고 쓰기 편해졌다"**고 느낄 것이며, 기록 탭에 들어갈 때마다 내 레벨을 보게 되어 **동기부여** 효과도 훨씬 커질 것입니다.

바로 Phase 1부터 적용해 보시죠! 🚀