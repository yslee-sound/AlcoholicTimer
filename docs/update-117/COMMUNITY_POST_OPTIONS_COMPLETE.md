# ✅ 커뮤니티 게시글 관리 기능 구현 완료 보고서

**작성일**: 2025-12-20  
**상태**: ✅ Phase 1-3 모두 완료 및 빌드 성공

---

## 🎯 구현 목표

게시글의 **더보기(⋮)** 버튼 기능 구현:
- 내 글: 삭제
- 남의 글: 숨기기, 신고하기

---

## 📋 Phase별 구현 내역

### ✅ Phase 1: 데이터 모델 및 소유권 식별

#### 1. Post 데이터 모델 수정
**파일**: `data/model/Post.kt`

```kotlin
data class Post(
    // ...existing fields...
    
    // [NEW] Phase 3: 게시글 관리 (소유권 식별)
    @PropertyName("authorId")
    val authorId: String = "" // 기본값 "" - 하위 호환성 확보
)
```

**특징**:
- ✅ `@PropertyName` 어노테이션으로 Firestore 매핑
- ✅ 기본값 ""으로 하위 호환성 유지
- ✅ 기존 게시글에 필드 없어도 정상 작동

#### 2. UserRepository 확장
**파일**: `data/repository/UserRepository.kt`

```kotlin
/**
 * [NEW] Phase 3: 기기 고유 ID 가져오기
 * 앱 설치 시 한 번만 생성되고 계속 유지됨
 */
fun getInstallationId(): String {
    var installationId = prefs.getString(KEY_INSTALLATION_ID, null)
    if (installationId == null) {
        installationId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, installationId).apply()
    }
    return installationId
}
```

**원리**:
- 첫 실행 시 UUID 생성 → SharedPreferences 저장
- 이후 실행 시 저장된 ID 재사용
- 앱 삭제 전까지 동일한 ID 유지

#### 3. CommunityViewModel 확장
**파일**: `ui/tab_03/viewmodel/CommunityViewModel.kt`

**추가된 기능**:

1. **기기 ID 관리**:
```kotlin
// [NEW] Phase 3: 기기 고유 ID
private val deviceUserId: String by lazy {
    userRepository.getInstallationId()
}
```

2. **소유권 판별**:
```kotlin
/**
 * [NEW] Phase 3: 이 게시글이 내 글인지 확인
 */
fun isMyPost(post: Post): Boolean {
    return post.authorId == deviceUserId
}
```

3. **숨긴 게시글 필터링**:
```kotlin
// [NEW] Phase 3: 숨긴 게시글 ID 목록
private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())

// loadPosts()에서 필터링
_posts.value = postList.filter { post ->
    !_hiddenPostIds.value.contains(post.id)
}
```

4. **게시글 작성 시 authorId 포함**:
```kotlin
val post = Post(
    // ...existing fields...
    authorId = deviceUserId // [NEW] 작성자 기기 ID
)
```

---

### ✅ Phase 2: UI 구현 (BottomSheet)

#### 1. CommunityScreen 상태 추가
```kotlin
// [NEW] Phase 3: 게시글 옵션 바텀 시트
var selectedPost by remember { mutableStateOf<Post?>(null) }
```

#### 2. PostItem 연결
```kotlin
PostItem(
    // ...existing params...
    onMoreClick = { selectedPost = item } // [NEW] 바텀 시트 열기
)
```

#### 3. ModalBottomSheet 구현
```kotlin
selectedPost?.let { post ->
    ModalBottomSheet(
        onDismissRequest = { selectedPost = null },
        containerColor = Color.White
    ) {
        PostOptionsBottomSheet(
            post = post,
            isMyPost = viewModel.isMyPost(post),
            onDelete = { /* ... */ },
            onHide = { /* ... */ },
            onReport = { /* ... */ }
        )
    }
}
```

#### 4. PostOptionsBottomSheet 디자인
**스타일**: 페이스북 스타일 (아이콘 + 텍스트 리스트)

**내 글일 때**:
```
┌─────────────────────────┐
│ 게시글 관리              │
├─────────────────────────┤
│ 🗑️  게시글 삭제          │
└─────────────────────────┘
```

**남의 글일 때**:
```
┌─────────────────────────┐
│ 게시글 옵션              │
├─────────────────────────┤
│ 👁️‍🗨️  이 게시글 숨기기     │
│ 🚨  게시글 신고하기       │
└─────────────────────────┘
```

---

### ✅ Phase 3: 기능 구현 (Action)

#### 1. 게시글 삭제 (내 글만)
**메서드**: `CommunityViewModel.deletePost()`

```kotlin
fun deletePost(postId: String) {
    viewModelScope.launch {
        try {
            // Firestore에서 삭제
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("posts").document(postId).delete().await()
            
            // 실시간 리스너가 자동으로 목록 업데이트
        } catch (e: Exception) {
            Log.e(TAG, "게시글 삭제 실패", e)
        }
    }
}
```

**특징**:
- ✅ Firestore 문서 영구 삭제
- ✅ 이미지가 있어도 삭제 가능
- ✅ 실시간 리스너 자동 반영

#### 2. 게시글 숨기기 (남의 글)
**메서드**: `CommunityViewModel.hidePost()`

```kotlin
fun hidePost(postId: String) {
    _hiddenPostIds.value = _hiddenPostIds.value + postId
    _posts.value = _posts.value.filter { it.id != postId }
    Log.d(TAG, "게시글 숨김 처리: $postId")
}
```

**특징**:
- ✅ 로컬 메모리에만 저장 (앱 재시작 시 초기화)
- ✅ 즉시 목록에서 제거
- ✅ Firestore는 건드리지 않음 (다른 사용자는 계속 볼 수 있음)

#### 3. 게시글 신고하기 (남의 글)
**메서드**: `CommunityViewModel.reportPost()`

```kotlin
fun reportPost(postId: String) {
    viewModelScope.launch {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val reportData = hashMapOf(
                "targetPostId" to postId,
                "reason" to "부적절한 콘텐츠",
                "reportedAt" to Timestamp.now(),
                "reporterId" to deviceUserId
            )
            
            firestore.collection("reports").add(reportData).await()
            
            // 신고 후 자동으로 숨기기
            hidePost(postId)
        } catch (e: Exception) {
            Log.e(TAG, "신고 실패", e)
        }
    }
}
```

**Firestore 구조**:
```
reports (컬렉션)
├─ {reportId}
    ├─ targetPostId: "post123"
    ├─ reason: "부적절한 콘텐츠"
    ├─ reportedAt: Timestamp
    └─ reporterId: "uuid-xxx"
```

**특징**:
- ✅ Firestore `reports` 컬렉션에 저장
- ✅ 신고자 ID 포함 (악용 방지)
- ✅ 신고 후 자동으로 숨김 처리
- ✅ 토스트 메시지 표시

---

## 🔥 Firestore 데이터 구조

### posts 컬렉션 (기존 + 신규)
```json
{
  "id": "auto_generated_id",
  "nickname": "익명의 사자",
  "timerDuration": "72시간",
  "content": "힘들지만 버텨보자!",
  "imageUrl": "https://...",
  "likeCount": 5,
  "createdAt": Timestamp,
  "deleteAt": Timestamp,
  "authorAvatarIndex": 3,
  "authorId": "uuid-abcd-1234" // ✅ NEW
}
```

### reports 컬렉션 (신규 생성)
```json
{
  "targetPostId": "post_id",
  "reason": "부적절한 콘텐츠",
  "reportedAt": Timestamp,
  "reporterId": "uuid-efgh-5678"
}
```

---

## 📱 사용자 시나리오

### 시나리오 1: 내 글 삭제
1. 사용자가 자신이 작성한 게시글의 `⋮` 버튼 클릭
2. 바텀 시트 열림: "게시글 관리"
3. "🗑️ 게시글 삭제" 메뉴 표시
4. 클릭 시:
   - Firestore에서 문서 삭제
   - 피드에서 즉시 사라짐
   - 바텀 시트 자동으로 닫힘

### 시나리오 2: 남의 글 숨기기
1. 사용자가 다른 사람의 게시글 `⋮` 버튼 클릭
2. 바텀 시트 열림: "게시글 옵션"
3. "👁️‍🗨️ 이 게시글 숨기기" 클릭
4. 결과:
   - 내 화면에서만 즉시 사라짐
   - 다른 사용자는 계속 볼 수 있음
   - 앱 재시작 시 다시 보임 (메모리 저장)

### 시나리오 3: 남의 글 신고하기
1. 사용자가 부적절한 게시글 `⋮` 버튼 클릭
2. "🚨 게시글 신고하기" 클릭
3. 결과:
   - Firestore `reports` 컬렉션에 신고 기록 저장
   - 자동으로 숨김 처리 (내 화면에서 사라짐)
   - "신고가 접수되었습니다" 토스트 표시

---

## 🔒 보안 및 제한 사항

### 구현된 보안 기능
✅ authorId 기반 소유권 검증 (클라이언트 측)  
✅ 신고자 ID 기록 (악용 추적 가능)  
✅ 삭제는 본인 글만 가능 (UI 레벨 제한)

### 추후 개선 필요사항 (Optional)
⚠️ **Firestore Security Rules** (서버 측 검증):
```javascript
// 본인 글만 삭제 가능하도록 강제
match /posts/{postId} {
  allow delete: if request.auth != null && 
                   resource.data.authorId == request.auth.uid;
}
```

⚠️ **신고 누적 시 자동 숨김** (Cloud Functions):
```javascript
// 신고 3회 이상 시 자동 비활성화
exports.checkReportCount = functions.firestore
  .document('reports/{reportId}')
  .onCreate(async (snap, context) => {
    const targetPostId = snap.data().targetPostId;
    const count = await getReportCount(targetPostId);
    if (count >= 3) {
      await hidePost(targetPostId);
    }
  });
```

---

## 🧪 테스트 체크리스트

### 기본 기능 테스트
- [x] 내 글에서 `⋮` 클릭 시 "게시글 삭제" 메뉴만 표시
- [x] 남의 글에서 `⋮` 클릭 시 "숨기기", "신고하기" 메뉴 표시
- [x] 내 글 삭제 시 Firestore에서 삭제되고 피드에서 사라짐
- [x] 남의 글 숨기기 시 내 화면에서만 사라짐
- [x] 남의 글 신고 시 자동 숨김 + 토스트 표시

### 예외 상황 테스트
- [ ] 네트워크 끊김 상태에서 삭제 시도 → 에러 핸들링
- [ ] 동일한 게시글 여러 번 신고 → 중복 체크 (현재 미구현)
- [ ] 숨긴 게시글 30개 이상 → 메모리 관리 (현재 제한 없음)

---

## 📂 수정된 파일 목록

### 1. 데이터 모델
- ✅ `data/model/Post.kt` - `authorId` 필드 추가

### 2. Repository
- ✅ `data/repository/UserRepository.kt` - `getInstallationId()` 메서드 추가

### 3. ViewModel
- ✅ `ui/tab_03/viewmodel/CommunityViewModel.kt`
  - deviceUserId 관리
  - isMyPost() 함수
  - deletePost() 함수
  - hidePost() 함수
  - reportPost() 함수
  - 숨긴 게시글 필터링 로직

### 4. UI
- ✅ `ui/tab_03/CommunityScreen.kt`
  - selectedPost 상태 추가
  - onMoreClick 연결
  - ModalBottomSheet 구현
  - PostOptionsBottomSheet 컴포넌트 추가
  - Material Icons import 추가

---

## 🚀 빌드 결과

```
BUILD SUCCESSFUL in 10s
44 actionable tasks: 8 executed, 36 up-to-date
Installed on 2 devices.
```

✅ **컴파일 에러**: 0개  
✅ **런타임 테스트**: 정상 작동  
✅ **하위 호환성**: 유지 (기존 게시글도 정상 표시)

---

## 📖 사용자 가이드

### 내 게시글 삭제하기
1. 내가 작성한 글의 우측 상단 `⋮` 버튼 클릭
2. "게시글 삭제" 선택
3. 게시글이 영구 삭제되고 모든 사용자의 화면에서 사라집니다

### 부적절한 게시글 숨기기
1. 보기 싫은 글의 `⋮` 버튼 클릭
2. "이 게시글 숨기기" 선택
3. 내 화면에서만 사라집니다 (다른 사람은 계속 볼 수 있음)

### 부적절한 게시글 신고하기
1. 문제가 있는 글의 `⋮` 버튼 클릭
2. "게시글 신고하기" 선택
3. 운영자가 확인할 수 있도록 신고가 접수됩니다
4. 신고한 글은 자동으로 숨김 처리됩니다

---

## 🎯 다음 단계 제안 (Optional)

### Phase 4: 서버 측 검증 (Priority: High)
- Firestore Security Rules 적용
- 타인의 게시글 삭제 차단

### Phase 5: 신고 관리 시스템 (Priority: Medium)
- 신고 누적 시 자동 비활성화
- 관리자 대시보드 (신고 목록 확인)

### Phase 6: 숨기기 개선 (Priority: Low)
- SharedPreferences에 영구 저장
- 숨긴 게시글 목록 관리 UI

---

**구현 완료일**: 2025-12-20  
**담당**: GitHub Copilot  
**문서 버전**: 1.0

