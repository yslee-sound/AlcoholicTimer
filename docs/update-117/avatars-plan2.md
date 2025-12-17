

-----

## 📌 1단계: 아바타 관리자 & 데이터 모델 만들기 (Phase 2)

먼저, 20개의 그림을 관리하는 \*\*'장부(Manager)'\*\*를 만들고, 서버에 사진 대신 \*\*'번호(0\~19)'\*\*를 저장하도록 데이터 구조를 바꿉니다.

**[아래 프롬프트를 복사해서 AI에게 주세요]**

-----

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 아키텍처 및 파이어베이스 전문가입니다.

**목표:** 'AlcoholicTimer' 앱에 **20개의 프리셋 아바타 시스템**을 적용하려고 합니다. 이미지가 `res/drawable` 폴더에 `avatar_00.xml` \~ `avatar_19.xml`로 준비되어 있다고 가정하고 로직을 작성해 주세요.

**상세 요구사항:**

1.  **AvatarManager (Singleton Object):**

      * `object AvatarManager`를 만드세요.
      * `val avatars` 리스트에 `R.drawable.avatar_00`부터 `R.drawable.avatar_19`까지 **총 20개의 리소스 ID**를 등록하세요. (코드가 길어져도 20개를 모두 포함해 주세요).
      * `getAvatarResId(index: Int): Int` 함수를 만드세요. (인덱스가 0\~19 범위를 벗어나면 기본값 0번 아바타를 반환하도록 안전하게 처리하세요).

2.  **데이터 모델 변경 (Firestore):**

      * **`User` 모델:** 기존의 `profileImageUrl: String?`을 제거하고, **`avatarIndex: Int` (기본값 0)** 필드를 추가하세요.
      * **`Post` 모델:** 기존의 `authorImage: String?`을 제거하고, **`authorAvatarIndex: Int` (기본값 0)** 필드를 추가하세요.

3.  **Repository 로직 업데이트:**

      * **프로필 변경:** 사용자가 아바타를 선택하면 Firestore `users` 컬렉션의 `avatarIndex` 필드만 업데이트하는 함수를 작성하세요 (`updateAvatarIndex`).
      * **게시글 작성:** `addPost` 함수에서 글을 저장할 때, 현재 유저의 `avatarIndex`를 가져와서 `Post` 객체의 `authorAvatarIndex`에 저장하도록 수정하세요.

**출력 요청:**

  * `AvatarManager` 코드 (20개 리스트 포함)
  * 수정된 `User`, `Post` 데이터 클래스
  * 수정된 `updateAvatarIndex` 및 `addPost` 함수

**[프롬프트 끝]**

-----

-----

## 📌 2단계: 아바타 선택창 & 보여주기 (Phase 3)

로직이 완성되면, 이제 눈에 보이는 \*\*'선택 화면(그리드)'\*\*과 **'게시판 표시'** 부분을 만듭니다.

**[1단계 코드를 적용한 뒤, 아래 프롬프트를 복사해서 주세요]**

-----

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 Jetpack Compose UI 전문가입니다.

**목표:** Phase 2에서 만든 `AvatarManager`를 사용하여 **Tab 5의 아바타 선택 화면**과 **Tab 4의 게시글 프로필 표시**를 구현해 주세요.

**상세 요구사항:**

1.  **Tab 5 (SettingsScreen) - 프로필 섹션:**

      * 화면 최상단에 현재 나의 아바타(`avatarIndex`)를 원형으로 크게 보여주세요.
      * 아바타를 클릭하면 \*\*`AvatarSelectionDialog`\*\*가 뜨도록 하세요.
      * **다이얼로그 UI:**
          * `LazyVerticalGrid`를 사용하여 20개의 아바타를 \*\*4열(GridCells.Fixed(4))\*\*로 배치하세요.
          * 각 아이템은 원형으로 클리핑하고, 클릭 시 해당 인덱스를 저장(`viewModel.updateAvatarIndex`)하고 다이얼로그를 닫으세요.

2.  **Tab 4 (CommunityScreen) - 게시글 UI 수정:**

      * `PostItem` (게시글 카드)의 프로필 영역을 수정하세요.
      * Coil(`AsyncImage`)을 제거하고, `Image(painter = painterResource(id = AvatarManager.getAvatarResId(post.authorAvatarIndex)))`를 사용하여 **로컬 리소스**를 즉시 로딩하세요. (속도가 매우 빨라집니다).
      * **글쓰기 입력창 상단:** "오늘 하루는 어땠나요?" 문구 옆의 내 프로필 아이콘도 현재 설정된 아바타로 연동하세요.

**출력 요청:**

  * `AvatarSelectionDialog` Composable 함수 (그리드 포함)
  * 수정된 `SettingsScreen`의 프로필 UI 부분
  * 수정된 `PostItem` 및 `WritePostTrigger` UI 부분

**[프롬프트 끝]**

-----

### 👨‍🏫 전문가의 마지막 팁 (개발자가 할 일)

AI가 코드를 짜주면 `AvatarManager` 파일의 리스트 부분을 꼭 확인하세요.

```kotlin
// AI가 이렇게 짜줄 텐데, 파일 이름이 선생님 파일과 맞는지 확인!
val avatars = listOf(
    R.drawable.avatar_00, // 선생님 파일명이 avatar_00.xml 이라면 OK
    R.drawable.avatar_01,
    ...
)
```

만약 선생님 파일명이 `avatar_1` (0이 없음) 이라면, AI가 짜준 코드에서 `00`을 지우고 파일명에 맞게 살짝 고쳐주시기만 하면 됩니다.

이제 이 기능만 붙이면, 선생님 앱은 \*\*"개성 있는 20명의 캐릭터들이 활동하는 활기찬 커뮤니티"\*\*가 됩니다\! 바로 시작해 보세요\! 🚀