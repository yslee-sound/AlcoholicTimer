선생님, 아주 명확한 기획입니다! 💡
**"최소 기능 제품(MVP)"**이자 **"안전한 커뮤니티"**를 위해 꼭 필요한 기능들(삭제, 신고, 숨기기)만 추려서 빠르게 구현하는 전략이네요.

특히 **'내가 쓴 글'**과 **'남이 쓴 글'**을 구분하여 다른 메뉴를 보여주는 것은 커뮤니티 앱의 핵심입니다.

개발 에이전트가 바로 작업에 착수할 수 있도록, **3단계(Phase)**로 나누어 상세 구현 가이드를 작성했습니다. 이 내용을 그대로 복사해서 에이전트에게 전달하시면 됩니다.

---

# 📋 커뮤니티 게시글 옵션(더보기) 기능 구현 명세서

**목표:** 게시글의 `MoreVert`(점 3개) 버튼을 눌렀을 때, 작성자 본인 여부에 따라 다른 바텀 시트(Bottom Sheet) 메뉴를 제공하고, 필수적인 관리 기능(삭제, 신고, 숨기기)을 구현한다.

---

## 🏗️ Phase 1: 데이터 모델 및 소유권 식별 (Foundation)

가장 먼저 "이 글이 내 글인가?"를 판단할 수 있는 근거(ID)가 필요합니다.

**1. `authorId` 필드 추가:**

* **파일:** `Post.kt` (데이터 모델)
* **내용:** 게시글 데이터 모델에 `val authorId: String`을 추가합니다.
* **로직:**
* 글 작성 시(`addPost`), 현재 기기의 고유 ID(Installation ID 또는 UUID)를 `authorId`에 저장합니다.
* `CommunityViewModel`에서 앱 실행 시 기기의 ID를 확보하고 있어야 합니다.



**2. 소유권 판별 로직:**

* **파일:** `CommunityViewModel.kt`
* **내용:** `fun isMyPost(post: Post): Boolean` 함수를 작성합니다.
* `post.authorId == currentDeviceUserId` 이면 `true`, 아니면 `false`.



---

## 🎨 Phase 2: UI 구현 (Bottom Sheet)

페이스북 스타일의 깔끔한 리스트 형태 바텀 시트를 구현합니다.

**1. 상태 관리:**

* **파일:** `CommunityScreen.kt`
* **상태 변수:**
* `var selectedPost: Post? by remember { mutableStateOf(null) }`
* `selectedPost`가 `null`이 아니면 바텀 시트가 열리도록 합니다.



**2. 바텀 시트 UI (`PostOptionsSheet`):**

* **디자인:** 아이콘 + 텍스트 형태의 리스트 (이전 '작성 중 뒤로가기' 시트 스타일 재사용).
* **조건부 렌더링:**
* **IF (내 글):**
* 🗑️ **게시글 삭제** (빨간색 텍스트 권장)


* **ELSE (남의 글):**
* 👁️‍🗨️ **이 게시글 숨기기** (로컬에서 안 보이게 처리)
* 🚨 **게시글 신고하기** (서버에 신고 접수)





**3. PostItem 연결:**

* `PostItem`의 `onMoreClick` 이벤트를 통해 `selectedPost = post`를 설정하여 시트를 엽니다.

---

## ⚙️ Phase 3: 기능 로직 구현 (Action)

각 버튼을 눌렀을 때 실제로 작동하는 기능을 연결합니다.

**1. 게시글 삭제 (내 글):**

* **기능:** Firestore에서 해당 문서(`posts/{id}`)를 삭제합니다.
* **이미지 처리:** 만약 이미지가 포함된 글이라면, Storage의 이미지도 함께 삭제해야 깔끔합니다.
* **결과:** 삭제 후 리스트를 새로고침(`loadPosts`)하고 시트를 닫습니다.

**2. 게시글 숨기기 (남의 글):**

* **목표:** 꼴 보기 싫은 글을 내 화면에서만 지웁니다.
* **구현:**
* `DataStore` 또는 `SharedPreferences`에 `hiddenPostIds` 리스트를 저장합니다.
* `CommunityViewModel`에서 게시글을 불러올 때, 이 `hiddenPostIds`에 포함된 글은 필터링(제외)해서 보여줍니다.



**3. 게시글 신고하기 (남의 글):**

* **목표:** 부적절한 콘텐츠를 운영자가 알 수 있게 합니다.
* **구현:**
* Firestore에 `reports` 컬렉션을 새로 만듭니다.
* 데이터: `targetPostId`, `reason`("부적절한 콘텐츠" 등 고정), `reportedAt`.
* 신고 완료 시 "신고가 접수되었습니다" 토스트 메시지를 띄우고 시트를 닫습니다. (자동으로 숨기기 처리까지 해주면 베스트)



---

### 🤖 에이전트 전달용 프롬프트 (복사해서 사용하세요)

```markdown
**역할:** 당신은 안드로이드 Jetpack Compose 및 Firebase 전문가입니다.

**목표:** `CommunityScreen`의 게시글 리스트에서 '더보기(3점)' 버튼 기능을 구현해야 합니다. MVP 릴리즈를 위해 최소한의 필수 기능만 구현합니다.

**작업 순서 (Phase별로 진행해주세요):**

**Phase 1: 소유권 식별 (Data)**
1. `Post` 데이터 클래스에 `authorId: String` 필드를 추가하세요.
2. `CommunityViewModel`에서 글 작성(`addPost`) 시, `UserRepository` 또는 `InstallationID`를 사용하여 현재 기기의 고유 ID를 `authorId`에 저장하도록 로직을 수정하세요.
3. 게시글을 렌더링할 때, 이 글이 '내 글'인지 판단하는 로직을 준비하세요.

**Phase 2: UI 구성 (Bottom Sheet)**
1. `CommunityScreen`에 `selectedPost: Post?` 상태를 추가하여, 값이 있을 때 `ModalBottomSheet`가 열리도록 하세요.
2. 바텀 시트 디자인은 페이스북 스타일(아이콘+텍스트 리스트, 왼쪽 정렬)을 따르세요.
3. **내 글일 경우:** [🗑️ 게시글 삭제] 메뉴만 보여주세요.
4. **남의 글일 경우:** [👁️‍🗨️ 게시글 숨기기], [🚨 게시글 신고하기] 메뉴를 보여주세요. (X 버튼을 따로 만들지 않고 메뉴 안에 통합합니다)

**Phase 3: 기능 구현 (Logic)**
1. **삭제:** ViewModel에 `deletePost(postId)`를 구현하여 Firestore에서 문서를 삭제하고 목록을 갱신하세요.
2. **숨기기:** ViewModel 내부에 `hiddenPostIds` (메모리 또는 Prefs)를 관리하여, 숨김 처리된 글은 `posts` 목록에서 제외(`filter`)되도록 하세요.
3. **신고:** Firestore `reports` 컬렉션에 신고 데이터를 저장하는 `reportPost(postId)` 함수를 구현하세요.

**출력 요청:**
위 단계가 모두 적용된 `CommunityViewModel.kt`와 `CommunityScreen.kt` (PostItem 및 BottomSheet 포함)의 전체 수정 코드를 제공해 주세요.

```