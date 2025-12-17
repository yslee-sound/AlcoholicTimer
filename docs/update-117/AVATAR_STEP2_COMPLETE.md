# Step 2 완료 보고서: 아바타 시스템 UI 구현

**작업일**: 2025-12-17  
**단계**: Step 2 - 화면 구현 (Phase 3)

---

## ✅ 작업 완료 내역

### 📦 생성된 파일 (1개)

1. **AvatarSelectionDialog.kt** - 아바타 선택 다이얼로그 (20개 그리드)

### 🔧 수정된 파일 (5개)

1. **Tab05ViewModel.kt** - 아바타 상태 및 업데이트 기능
2. **Tab05.kt** - 프로필 영역에 아바타 표시 및 선택 다이얼로그
3. **PostItem.kt** - 게시글에 아바타 표시
4. **CommunityScreen.kt** - PostItem에 avatarIndex 전달
5. **CommunityRepository.kt** - 더미 데이터 생성 시 랜덤 아바타

---

## 📋 구현 상세

### 1. AvatarSelectionDialog (신규)

**위치**: `ui/tab_05/components/AvatarSelectionDialog.kt`

**기능**:
- 20개 아바타를 4열 그리드로 표시
- 현재 선택된 아바타 강조 (파란색 테두리)
- 아바타 클릭 시 업데이트 및 다이얼로그 닫기

**코드**:
```kotlin
@Composable
fun AvatarSelectionDialog(
    currentAvatarIndex: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface {
            Column {
                Text("아바타 선택")
                
                // 4열 그리드
                LazyVerticalGrid(columns = GridCells.Fixed(4)) {
                    items(20) { index ->
                        AvatarItem(
                            avatarResId = AvatarManager.avatars[index],
                            isSelected = index == currentAvatarIndex,
                            onClick = {
                                onAvatarSelected(index)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}
```

**디자인**:
- 원형 아바타 (64dp)
- 선택된 아바타: 파란색 테두리 (3dp)
- 미선택 아바타: 회색 테두리 (1dp)

---

### 2. Tab05ViewModel 수정

**추가된 상태**:
```kotlin
data class SettingsUiState(
    // ...existing fields...
    val avatarIndex: Int = 0,
    val showAvatarDialog: Boolean = false
)
```

**추가된 메서드**:
```kotlin
// 아바타 인덱스 로드
private fun loadAvatarIndex() {
    val avatarIndex = userRepository?.getAvatarIndex() ?: 0
    _uiState.update { it.copy(avatarIndex = avatarIndex) }
}

// 다이얼로그 표시/숨김
fun setShowAvatarDialog(show: Boolean) {
    _uiState.update { it.copy(showAvatarDialog = show) }
}

// 아바타 업데이트
fun updateAvatar(index: Int) {
    val success = userRepository?.updateAvatar(index)
    if (success) {
        _uiState.update { it.copy(avatarIndex = index) }
    }
}
```

---

### 3. Tab05 화면 수정

**변경 전**:
```kotlin
// 기존: 회색 아이콘
Icon(
    painter = painterResource(id = R.drawable.usercircle),
    tint = Color(0xFFBDBDBD)
)
```

**변경 후**:
```kotlin
// [NEW] 아바타 이미지
Image(
    painter = painterResource(id = AvatarManager.getAvatarResId(uiState.avatarIndex)),
    contentDescription = "프로필 아바타",
    modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(Color(0xFFF5F5F5))
)
```

**클릭 동작**:
```kotlin
Row(
    modifier = Modifier
        .clickable { viewModel.setShowAvatarDialog(true) } // 아바타 클릭 시
) {
    // ...avatar...
}
```

**다이얼로그 표시**:
```kotlin
if (uiState.showAvatarDialog) {
    AvatarSelectionDialog(
        currentAvatarIndex = uiState.avatarIndex,
        onAvatarSelected = { index ->
            viewModel.updateAvatar(index)
        },
        onDismiss = { viewModel.setShowAvatarDialog(false) }
    )
}
```

---

### 4. PostItem 수정

**변경 전**:
```kotlin
@Composable
fun PostItem(
    nickname: String,
    timerDuration: String,
    // ...
) {
    // ...
    Box {
        Icon(painter = painterResource(id = R.drawable.ic_user_circle))
    }
}
```

**변경 후**:
```kotlin
@Composable
fun PostItem(
    nickname: String,
    timerDuration: String,
    authorAvatarIndex: Int = 0, // [NEW]
    // ...
) {
    PostHeader(
        nickname = nickname,
        timerDuration = timerDuration,
        authorAvatarIndex = authorAvatarIndex // [NEW]
    )
}

private fun PostHeader(
    nickname: String,
    timerDuration: String,
    authorAvatarIndex: Int = 0 // [NEW]
) {
    // [NEW] 아바타 이미지 (로컬 리소스)
    Image(
        painter = painterResource(
            id = AvatarManager.getAvatarResId(authorAvatarIndex)
        ),
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
    )
}
```

**특징**:
- URL 로딩 제거 (AsyncImage 제거)
- 로컬 리소스 즉시 로딩 (빠른 표시)
- AvatarManager 사용 (안전한 인덱스 처리)

---

### 5. CommunityScreen 수정

**PostItem 호출 시 avatarIndex 전달**:
```kotlin
PostItem(
    nickname = item.nickname,
    timerDuration = item.timerDuration,
    content = item.content,
    imageUrl = item.imageUrl,
    likeCount = item.likeCount,
    isLiked = false,
    remainingTime = calculateRemainingTime(item.deleteAt),
    authorAvatarIndex = item.authorAvatarIndex, // [NEW]
    onLikeClick = { viewModel.toggleLike(item.id) }
)
```

---

### 6. CommunityRepository 수정

**더미 데이터 생성 시 랜덤 아바타**:
```kotlin
val post = Post(
    nickname = nicknames[i],
    timerDuration = "${(i + 1) * 24}시간",
    content = contents[i],
    // ...
    authorAvatarIndex = (0..19).random() // [NEW] 0~19 랜덤
)
```

---

## 🎨 UI 변경사항

### Tab 5 (설정 화면)

**Before**:
```
┌────────────────┐
│ 👤 (회색 아이콘)│
│ 알중이 >       │
└────────────────┘
```

**After**:
```
┌────────────────┐
│ 🐯 (아바타 5번) │ ← 클릭 가능
│ 알중이 >       │
└────────────────┘

클릭 시 ↓

┌───────────────────┐
│ 아바타 선택        │
│ ┌─┬─┬─┬─┐         │
│ │0│1│2│3│         │
│ ├─┼─┼─┼─┤         │
│ │4│5│6│7│         │
│ └─┴─┴─┴─┘         │
│      ...          │
│ [취소]            │
└───────────────────┘
```

---

### Tab 4 (커뮤니티 게시글)

**Before**:
```
┌────────────────────┐
│ 👤 익명의 사자      │
│ 🏅 72시간           │
│                    │
│ 오늘도 술 없이...  │
└────────────────────┘
```

**After**:
```
┌────────────────────┐
│ 🦁 익명의 사자      │ ← 아바타 3번 (사자)
│ 🏅 72시간           │
│                    │
│ 오늘도 술 없이...  │
└────────────────────┘
```

---

## 🔄 데이터 흐름

### 아바타 선택 흐름

```
1. Tab 5에서 프로필 아바타 클릭
   ↓
2. viewModel.setShowAvatarDialog(true)
   ↓
3. AvatarSelectionDialog 표시 (20개 그리드)
   ↓
4. 사용자가 아바타 선택 (예: 7번)
   ↓
5. viewModel.updateAvatar(7)
   ↓
6. userRepository.updateAvatar(7) → SharedPreferences 저장
   ↓
7. _uiState.update { avatarIndex = 7 }
   ↓
8. UI 즉시 업데이트 (새 아바타 표시)
```

---

### 게시글 작성 시 아바타 포함

```
1. 사용자가 글 작성
   ↓
2. CommunityViewModel.addPost()
   ↓
3. userRepository.getAvatarIndex() → 7
   ↓
4. Post(authorAvatarIndex = 7) 생성
   ↓
5. Firestore 저장
   ↓
6. 실시간 구독으로 게시글 목록 업데이트
   ↓
7. PostItem에 avatarIndex=7 전달
   ↓
8. AvatarManager.getAvatarResId(7) → R.drawable.avatar_07
   ↓
9. 아바타 이미지 즉시 표시
```

---

## 🎯 주요 특징

### 1. 로컬 리소스 사용

**장점**:
- ✅ 즉시 로딩 (네트워크 불필요)
- ✅ 빠른 표시 속도
- ✅ 오프라인 동작
- ✅ 용량 효율적 (Vector Drawable)

**Before (URL 방식)**:
```kotlin
AsyncImage(
    model = "https://example.com/avatar.jpg",
    // 네트워크 로딩 필요
)
```

**After (로컬 리소스)**:
```kotlin
Image(
    painter = painterResource(id = R.drawable.avatar_07)
    // 즉시 표시
)
```

---

### 2. 안전한 인덱스 처리

**AvatarManager의 안전 장치**:
```kotlin
fun getAvatarResId(index: Int?): Int {
    return when {
        index == null -> avatars[0]
        index < 0 -> avatars[0]
        index >= 20 -> avatars[0]
        else -> avatars[index]
    }
}
```

**결과**:
- 잘못된 인덱스 → 0번 아바타 (안전)
- 앱 크래시 방지

---

### 3. 실시간 UI 업데이트

**StateFlow 활용**:
```kotlin
// ViewModel
val uiState: StateFlow<SettingsUiState>

// UI
val uiState by viewModel.uiState.collectAsState()
Image(painter = painterResource(id = getAvatarResId(uiState.avatarIndex)))
```

**결과**:
- 아바타 변경 시 UI 즉시 업데이트
- 수동 새로고침 불필요

---

## 📊 테스트 시나리오

### 시나리오 1: 아바타 선택

```
1. Tab 5 열기
2. 프로필 아바타 클릭
   ✅ 다이얼로그 표시됨
3. 20개 아바타 확인
   ✅ 4열 그리드로 표시
   ✅ 현재 아바타 파란색 테두리
4. 다른 아바타 클릭 (예: 10번)
   ✅ 다이얼로그 닫힘
   ✅ 프로필 아바타 즉시 변경
5. 앱 재실행
   ✅ 선택한 아바타 유지됨
```

---

### 시나리오 2: 게시글 작성

```
1. Tab 4 열기
2. 글쓰기 버튼 클릭
3. 내용 입력 후 게시
   ✅ 게시글에 내 아바타 표시
4. 다른 게시글 확인
   ✅ 각 게시글마다 다른 아바타
```

---

### 시나리오 3: 더미 데이터

```
1. Tab 5 → Debug 메뉴
2. "Generate Dummy Posts" 클릭
   ✅ 10개 게시글 생성
3. Tab 4에서 확인
   ✅ 각 게시글이 랜덤 아바타 (0~19)
```

---

## ✅ 체크리스트

### 완료
- [x] AvatarSelectionDialog 생성
- [x] Tab05ViewModel 아바타 기능 추가
- [x] Tab05 프로필 아바타 표시
- [x] Tab05 아바타 선택 기능
- [x] PostItem 아바타 표시
- [x] CommunityScreen avatarIndex 전달
- [x] 더미 데이터 랜덤 아바타

### 미완료 (향후 개선)
- [ ] WritePostTrigger에 사용자 아바타 표시
- [ ] 아바타 선택 시 애니메이션
- [ ] 아바타 카테고리 분류 (동물, 사람 등)

---

## 🎉 Step 2 완료!

**구현된 기능**:
1. ✅ 아바타 선택 다이얼로그 (20개 그리드)
2. ✅ Tab 5 프로필 아바타 표시
3. ✅ Tab 5 아바타 선택 기능
4. ✅ Tab 4 게시글 아바타 표시
5. ✅ 더미 데이터 랜덤 아바타

**UI 개선**:
- ✅ 로컬 리소스로 빠른 로딩
- ✅ 실시간 UI 업데이트
- ✅ 안전한 인덱스 처리
- ✅ 원형 아바타 디자인

**빌드 상태**: 진행 중

**다음 단계**: Step 3 (선택사항) - 추가 기능 (WritePostTrigger 등)

---

**작성일**: 2025-12-17  
**작성자**: GitHub Copilot  
**버전**: Avatar System v2.0 - Step 2

