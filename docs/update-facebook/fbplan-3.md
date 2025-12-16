요청하신 대로 **"FAB(플로팅 버튼) 삭제"**하고, **"상단 입력바(Trigger)"**와 **"전체 화면 슬라이드 업(Full Screen Dialog)"** 형태로 변경하는 프롬프트를 작성했습니다.

---

### 🏗️ 변경될 구조 미리보기

1.  **진입점 (Entry Point):**
    * 기존 (+) 버튼 삭제.
    * 피드 맨 위에 **[프로필 사진] + [오늘 하루는..?(회색 박스)]** 형태의 Row 추가.
2.  **작성 화면 (Write Screen):**
    * 박스를 누르면 바텀 시트(반쪽)가 아니라, **새로운 페이지가 아래에서 위로 꽉 차게(Full Screen)** 올라옴.
    * 내용은 방금 만드신 것(텍스트 입력, 버튼 등)을 그대로 유지.

---

# 🤖 [UI 리팩토링 프롬프트: 페이스북 스타일 글쓰기]

이 내용을 AI에게 그대로 복사해서 전달하세요.

***

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 Jetpack Compose UI 전문가입니다.

**목표:** 현재 `CommunityScreen`의 게시글 작성 UX를 **'Floating Action Button(FAB) + ModalBottomSheet'** 방식에서 **'페이스북 스타일 상단 입력바 + 풀스크린 다이얼로그'** 방식으로 전면 수정해 주세요.

**상세 요구사항:**

1.  **기존 UI 제거:**
    * 화면 우측 하단의 `FloatingActionButton`을 제거하세요.
    * 기존 `ModalBottomSheet` 코드를 제거하거나 비활성화하세요.

2.  **신규 UI 1: 상단 작성 트리거 (Feed Header):**
    * `LazyColumn`의 **가장 첫 번째 아이템(`item`)**으로 '글쓰기 진입바'를 추가하세요.
    * **디자인 (Facebook Style):**
        * `Row` 레이아웃을 사용하고 배경은 흰색, `padding`을 주세요.
        * **좌측:** 익명 프로필 아이콘 (`CircleShape`, 회색 배경 + 사람 아이콘).
        * **중앙:** 둥근 모서리(`RoundedCornerShape(50.dp)`)의 회색 배경(`Color(0xFFF0F2F5)`)을 가진 `Box` 또는 `Surface`.
        * **텍스트:** 중앙 박스 안에 "오늘 하루는 어땠나요? (익명)"이라는 힌트 텍스트를 넣으세요.
        * **우측:** '이미지/갤러리' 아이콘 (`IconButton`).
    * **동작:** 중앙 박스나 우측 아이콘을 클릭하면 `isWritingScreenVisible = true`로 상태를 변경하여 작성 화면을 엽니다.

3.  **신규 UI 2: 전체 화면 작성 페이지 (Full Screen Slide-up):**
    * 클릭 시 열리는 화면은 `Dialog`를 사용하되, `properties = DialogProperties(usePlatformDefaultWidth = false)`를 설정하여 **전체 화면(Full Screen)**을 꽉 채우게 만드세요.
    * **애니메이션:** 가능하다면 `AnimatedVisibility`의 `slideInVertically` 효과를 주어 아래에서 위로 올라오는 느낌을 주세요. (또는 단순히 전체 화면을 덮는 Composable로 구현).
    * **내용 (Content):**
        * **상단바 (TopBar):** [취소(X) 버튼] - [새 게시글 작성(타이틀)] - [게시하기(Button)] 형태로 구성하세요.
        * **본문:** 기존에 구현했던 텍스트 입력창(`TextField`)과 이미지 미리보기 기능을 이 화면 중앙에 배치하세요.

**출력 요청:**
* 수정된 `CommunityScreen` 전체 코드 (LazyColumn 구조 포함)
* 새로 만든 `PostInputHeader` (상단 진입바) Composable 함수
* 새로 만든 `WritePostFullScreen` (전체 화면 작성창) Composable 함수

**[프롬프트 끝]**

***

### 👨‍🏫 전문가의 조언

이 프롬프트를 실행하면 디자인이 확 바뀔 겁니다.

* **팁:** AI가 코드를 짜주면, 앱을 실행해서 **상단 회색 박스**를 눌러보세요. 화면이 꽉 차게 위로 슥~ 올라오면 성공입니다!
* 그 상태에서 방금 만드셨던 기능(글쓰기, 이미지 선택 등)이 잘 작동하는지 확인하시면 됩니다.

성공적으로 변경되면 알려주세요! 다음 단계(세부 디자인 다듬기)로 넘어가겠습니다. 🚀
