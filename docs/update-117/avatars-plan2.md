선생님, 파이어스토어 설정과 에셋(그림) 준비까지 완벽합니다! 💯
이제 **"서버에는 번호만 저장하고, 화면에는 그림을 보여주는"** 마법의 코드를 작성할 차례입니다.

개발 효율을 위해 **[1. 로직(뼈대)]**과 **[2. UI(화면)]** 두 단계로 나누어 프롬프트를 준비했습니다. 차례대로 복사해서 AI에게 주시면 됩니다.

---

## 📌 Step 1: 아바타 관리자 & 데이터 모델 (Phase 2)

이 프롬프트는 20개의 그림을 관리하는 **`AvatarManager`**를 만들고, 데이터를 저장할 때 **번호(0~19)**를 사용하도록 로직을 변경합니다.

**[프롬프트 복사]**

***

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 아키텍처 및 파이어베이스 데이터 전문가입니다.

**목표:** 'AlcoholicTimer' 앱에 **20개의 프리셋 아바타 시스템(Vector Assets)**을 적용하려고 합니다.
현재 `res/drawable` 폴더에 `avatar_00.xml` 부터 `avatar_19.xml` 까지 벡터 드로어블 파일이 준비되어 있습니다.

**상세 요구사항:**

1.  **AvatarManager (Singleton Object):**
    * `object AvatarManager`를 생성하세요.
    * `val avatars` 리스트에 `R.drawable.avatar_00` ~ `R.drawable.avatar_19` (총 20개)를 모두 등록하세요.
    * `getAvatarResId(index: Int): Int` 함수를 구현하세요.
    * **안전 장치:** 인덱스가 0~19 범위를 벗어나거나 null일 경우, 무조건 **0번 아바타(`R.drawable.avatar_00`)**를 반환하도록 예외 처리를 해주세요.

2.  **데이터 모델 변경 (Firestore 연동):**
    * **`User` 데이터 클래스:** `avatarIndex: Int = 0` (기본값 0) 필드를 추가하세요. (기존 `profileImageUrl`은 제거).
    * **`Post` 데이터 클래스:** `authorAvatarIndex: Int = 0` (기본값 0) 필드를 추가하세요. (기존 `authorImage`는 제거).
    * *주의:* 기존 Firestore 데이터(`posts`)에는 이 필드가 없습니다. 앱이 죽지 않도록 `@PropertyName` 어노테이션이나 기본값을 사용하여 하위 호환성을 확보하세요.

3.  **Repository 및 ViewModel 로직 업데이트:**
    * **`UserRepository`:**
        * `updateAvatar(index: Int)`: Firestore `users` 컬렉션의 내 문서에서 `avatarIndex` 필드만 업데이트하는 함수를 작성하세요.
    * **`CommunityViewModel` (글쓰기):**
        * `addPost` 함수 실행 시, `UserRepository`나 `UserPreference`에서 **현재 나의 `avatarIndex`**를 가져와서, 게시글 데이터(`Post`)에 포함시켜 저장하는 로직을 작성하세요.

**출력 요청:**
* `AvatarManager` 전체 코드
* 수정된 `User`, `Post` 데이터 클래스
* `UserRepository`의 `updateAvatar` 함수
* `CommunityViewModel`의 수정된 `addPost` 로직

**[프롬프트 끝]**

***

---

## 📌 Step 2: 화면 구현 (Phase 3)

로직이 완성되면, 이제 눈에 보이는 **선택창(Settings)**과 **게시판(Community)**을 수정합니다.

**[Step 1 코드를 적용한 뒤, 아래 프롬프트를 복사해서 주세요]**

***

**[프롬프트 시작]**

**역할:** 당신은 안드로이드 Jetpack Compose UI 전문가입니다.

**목표:** 앞서 만든 `AvatarManager`와 데이터 모델을 사용하여 **아바타 선택 화면**과 **게시글 프로필 표시** 기능을 구현해 주세요.

**상세 요구사항:**

1.  **Tab 5 (SettingsScreen) - 프로필 섹션:**
    * 화면 최상단에 **현재 아바타**를 원형으로 크게(80dp 이상) 표시하세요.
    * 아바타를 클릭하면 `AvatarSelectionDialog`가 뜨도록 만드세요.
    * **`AvatarSelectionDialog`:**
        * `LazyVerticalGrid` (GridCells.Fixed(4))를 사용하여 20개의 아바타를 나열하세요.
        * 각 아이템은 원형으로 클리핑(`CircleShape`)하고, 클릭 시 `viewModel.updateAvatar(index)`를 호출하고 다이얼로그를 닫으세요.
        * 현재 선택된 아바타는 테두리(Border)나 체크 표시로 강조해 주세요.

2.  **Tab 4 (CommunityScreen) - UI 업데이트:**
    * **`PostItem` (게시글 카드):**
        * 기존의 `AsyncImage`(URL 로딩)를 제거하세요.
        * `Image(painter = painterResource(id = AvatarManager.getAvatarResId(post.authorAvatarIndex)))`를 사용하여 **로컬 리소스**를 즉시 로딩하세요.
    * **`WritePostTrigger` (상단 입력바):**
        * "오늘 하루는 어땠나요?" 텍스트 옆의 작은 프로필 아이콘도 **현재 사용자의 아바타(`avatarIndex`)**를 실시간으로 보여주도록 수정하세요.

3.  **테스트 데이터 생성기 (Debug):**
    * Tab 5의 `Generate Dummy Posts` 기능도 수정하여, 더미 글을 생성할 때 `authorAvatarIndex`를 **0~19 사이의 랜덤 값**으로 저장하도록 변경해 주세요.

**출력 요청:**
* `AvatarSelectionDialog` Composable 함수
* 수정된 `SettingsScreen` (프로필 UI 부분)
* 수정된 `PostItem` 코드
* 수정된 `WritePostTrigger` 코드
* 업데이트된 `GenerateDummyPosts` 로직

**[프롬프트 끝]**

***

### 👨‍🏫 전문가의 가이드: 실행 후 확인 포인트

1.  **Tab 5 (설정):**
    * 상단에 0번 아바타(곰?)가 크게 떠 있나요?
    * 눌렀을 때 20개 그림이 쫙~ 뜨나요?
    * 하나를 고르면 상단 아바타가 바뀌나요? (이때 파이어스토어 `users`에 저장이 되는 겁니다.)
2.  **Tab 4 (커뮤니티):**
    * 이제 새 글을 써보세요.
    * 방금 고른 아바타가 글 옆에 예쁘게 나오나요?

이것만 확인되면 성공입니다! 코드가 나오면 바로 적용해 보세요! 🚀