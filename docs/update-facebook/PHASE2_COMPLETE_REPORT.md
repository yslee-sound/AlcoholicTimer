# Phase 2 완료 보고서: 데이터 모델 & Firestore 연동

**작업일**: 2025-12-17  
**작업자**: GitHub Copilot  
**단계**: Phase 2 - 데이터 시뮬레이션 (테스트 기능)

---

## ✅ 작업 완료 내역

### 📦 생성된 파일 (3개)

1. **Post.kt** - Firestore 데이터 모델
2. **CommunityRepository.kt** - Firestore CRUD 로직
3. **CommunityViewModel.kt** - UI 상태 관리

### 🔧 수정된 파일 (3개)

1. **CommunityScreen.kt** - ViewModel 연동 및 실시간 데이터 표시
2. **DebugScreen.kt** - 커뮤니티 테스트 섹션 추가
3. **DebugScreenViewModel.kt** - 테스트 데이터 생성/삭제 메서드

---

## 📊 데이터 모델 (Post)

### Firestore 컬렉션: `posts`

```kotlin
data class Post(
    @DocumentId
    val id: String = "",
    val nickname: String = "",              // "익명의 사자"
    val timerDuration: String = "",         // "72시간"
    val content: String = "",               // 게시글 본문
    val imageUrl: String? = null,           // 이미지 URL (선택)
    val likeCount: Int = 0,                 // 좋아요 수
    val createdAt: Timestamp = now(),       // 생성 시간
    val deleteAt: Timestamp = now()         // 삭제 예정 시간 (24시간 후)
)
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | String | Firestore 문서 ID |
| `nickname` | String | 익명 닉네임 (예: "익명의 사자") |
| `timerDuration` | String | 금주 타이머 (예: "72시간") |
| `content` | String | 게시글 본문 텍스트 |
| `imageUrl` | String? | 이미지 URL (Nullable) |
| `likeCount` | Int | 좋아요(쓰담쓰담) 수 |
| `createdAt` | Timestamp | 게시 시간 |
| `deleteAt` | Timestamp | 24시간 후 자동 삭제 시간 |

---

## 🔥 Firestore 연동

### CommunityRepository

**주요 기능:**

1. **실시간 게시글 구독** (`getPosts()`)
   - `Flow<List<Post>>` 반환
   - `createdAt` 내림차순 정렬 (최신글 위로)
   - Snapshot Listener로 실시간 업데이트

2. **테스트 데이터 생성** (`generateDummyPosts()`)
   - `WriteBatch`로 10개 게시글 한 번에 생성
   - 닉네임, 내용, 타이머 랜덤
   - 3개 중 1개만 이미지 포함
   - `deleteAt` = 현재 + 24시간

3. **모든 게시글 삭제** (`deleteAllPosts()`)
   - 테스트 데이터 초기화용

---

## 🧪 Tab 5 디버그 메뉴

### 새로 추가된 섹션: "커뮤니티 테스트"

```
┌─────────────────────────────────┐
│ 커뮤니티 테스트 (Community Test)│
├─────────────────────────────────┤
│ [📝 테스트 게시글 10개 생성]     │ ← 녹색 버튼
│ [🗑️ 모든 게시글 삭제]           │ ← 빨간 버튼
├─────────────────────────────────┤
│ ※ Tab 4 (커뮤니티)에서 결과 확인│
│ ※ 닉네임: 익명 1, 참는 중인...  │
│ ※ 타이머: 24~240시간 랜덤       │
│ ※ 좋아요: 0~50 랜덤             │
│ ※ 이미지: 3개 중 1개만 포함     │
│ ※ 삭제 예정: 생성 후 24시간     │
└─────────────────────────────────┘
```

### 버튼 동작

**📝 테스트 게시글 10개 생성**
- Firestore `posts` 컬렉션에 10개 문서 생성
- Toast: "✅ 테스트 게시글 10개 생성 완료! Tab 4에서 확인하세요."

**🗑️ 모든 게시글 삭제**
- Firestore `posts` 컬렉션의 모든 문서 삭제
- Toast: "✅ 모든 게시글 삭제 완료!"

---

## 🎨 CommunityScreen 변경사항

### Before (Phase 1)
- 하드코딩된 더미 데이터 (`DummyPost`)
- 정적 리스트 렌더링

### After (Phase 2)
- ViewModel 연동
- Firestore 실시간 구독
- 로딩 상태 표시 (`CircularProgressIndicator`)
- 빈 상태 표시 ("아직 게시글이 없습니다")
- 남은 시간 동적 계산 (`calculateRemainingTime()`)

```kotlin
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ...existing code...

    if (isLoading && posts.isEmpty()) {
        CircularProgressIndicator()
    } else if (posts.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn {
            items(posts, key = { it.id }) { post ->
                PostItem(/*...*/)
            }
        }
    }
}
```

---

## 📱 사용 흐름

### 1단계: 테스트 데이터 생성

1. 앱 실행
2. Tab 5 (더보기) → Debug 메뉴
3. "커뮤니티 테스트" 섹션으로 스크롤
4. **"📝 테스트 게시글 10개 생성"** 버튼 클릭
5. Toast 확인: "✅ 테스트 게시글 10개 생성 완료!"

### 2단계: 피드 확인

1. Tab 4 (커뮤니티) 이동
2. 10개 게시글이 즉시 표시됨
3. 좋아요 버튼 클릭 (Phase 2: 로컬 상태만 변경)
4. 남은 시간 표시 (예: "23h")

### 3단계: 데이터 초기화

1. Tab 5 → Debug 메뉴
2. **"🗑️ 모든 게시글 삭제"** 버튼 클릭
3. Tab 4 돌아가면 빈 상태 표시

---

## 🧩 아키텍처 구조

```
UI Layer (CommunityScreen)
    ↓ collectAsState()
ViewModel Layer (CommunityViewModel)
    ↓ Flow
Repository Layer (CommunityRepository)
    ↓ SnapshotListener
Firestore (posts 컬렉션)
```

### 데이터 흐름

```
Firestore posts 컬렉션 변화
    ↓ (SnapshotListener)
CommunityRepository.getPosts() Flow
    ↓ (viewModelScope.launch)
CommunityViewModel._posts StateFlow
    ↓ (collectAsState())
CommunityScreen UI 자동 업데이트
```

---

## 🎯 생성되는 더미 데이터 예시

```json
{
  "id": "auto-generated-id",
  "nickname": "익명의 사자",
  "timerDuration": "72시간",
  "content": "오늘도 술 없이 하루를 보냈습니다. 처음엔 힘들었지만 점점 익숙해지고 있어요. 여러분도 할 수 있습니다!",
  "imageUrl": "https://picsum.photos/seed/1/400/300",
  "likeCount": 24,
  "createdAt": Timestamp(2025, 12, 17, 10, 30),
  "deleteAt": Timestamp(2025, 12, 18, 10, 30)
}
```

### 10개 게시글 패턴

- **닉네임**: 익명 1, 참는 중인 사자, 새벽의 독수리... (10개 고정)
- **타이머**: 24h, 48h, 72h... 240h (24시간씩 증가)
- **좋아요**: 0~50 랜덤
- **이미지**: 0, 3, 6, 9번째만 포함 (30%)
- **시간 간격**: 1시간씩 과거로 배치

---

## 🧪 테스트 시나리오

### ✅ 성공 케이스

1. **생성 버튼 클릭 → Toast 표시 → Tab 4에서 10개 확인**
2. **실시간 업데이트**: Firestore 콘솔에서 수동 추가 → 즉시 피드에 반영
3. **삭제 버튼 → 피드가 빈 상태로 전환**
4. **로딩 상태**: 데이터 없을 때 CircularProgressIndicator 표시

### ⚠️ 예외 케이스

- Firestore 연결 실패 → Toast: "❌ 게시글 생성 실패"
- 권한 없음 → Logcat 에러 확인

---

## 🔍 Phase 2 vs Phase 1 비교

| 항목 | Phase 1 (UI만) | Phase 2 (데이터 연동) |
|------|----------------|----------------------|
| 데이터 소스 | 하드코딩 | Firestore |
| 실시간 업데이트 | ❌ | ✅ |
| 좋아요 기능 | 더미 | 로컬 상태 변경 |
| 테스트 도구 | 없음 | Tab 5 디버그 메뉴 |
| 이미지 | 플레이스홀더 | 플레이스홀더 (Phase 3에서 실제 로드) |

---

## 📝 다음 단계 (Phase 3)

### 준비 완료
- ✅ UI 완성
- ✅ Firestore 연동
- ✅ 테스트 도구 구축
- ✅ 실시간 데이터 흐름

### Phase 3 작업 예정
1. **Tab 2 연동**: 일기 작성 시 "24시간 챌린지에 익명 공유" 체크박스
2. **이미지 최적화**: 1024px 리사이징 + JPEG 80% 압축
3. **AdMob 광고**: 매 6번째 아이템에 네이티브 광고 삽입
4. **좋아요 기능**: Firestore 업데이트 (현재는 로컬만)
5. **24시간 자동 삭제**: Cloud Functions 또는 클라이언트 필터링

---

## 🎉 Phase 2 완료!

Firestore와의 실시간 연동이 완벽하게 구현되었습니다. Tab 5 디버그 메뉴를 통해 언제든지 테스트 데이터를 생성하고, Tab 4에서 즉시 확인할 수 있습니다.

**다음 단계**: Phase 3 프롬프트를 AI에게 제공하여 실전 기능을 완성하세요! 🚀

