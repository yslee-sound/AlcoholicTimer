# 페이스북 스타일 UI 개편 가이드

**목표**: CommunityScreen을 FAB + BottomSheet에서 → 상단 입력바 + 전체화면 다이얼로그로 전환

---

## 📋 작업 요약

### 현재 상태 (프롬프트 1 완료)
- ✅ FAB (+) 버튼: 우측 하단
- ✅ ModalBottomSheet: 하단에서 올라오는 글쓰기 창
- ✅ Firestore 저장 기능

### 목표 상태 (페이스북 스타일)
- 🎯 상단 작성 트리거: LazyColumn 첫 번째 아이템
- 🎯 전체화면 다이얼로그: Dialog with usePlatformDefaultWidth = false
- 🎯 상단바: [X 취소] - [제목] - [게시하기]

---

## 🔧 단계별 수정 가이드

### 1단계: Import 추가

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
```

### 2단계: Scaffold 수정 (FAB 제거)

**변경 전:**
```kotlin
Scaffold(
    // ...
    floatingActionButton = {
        FloatingActionButton(
            onClick = { showWriteSheet = true },
            // ...
        )
    }
)
```

**변경 후:**
```kotlin
Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color(0xFFF5F5F5),
    topBar = {
        TopAppBar(/* ...existing... */)
    }
    // floatingActionButton 제거
)
```

### 3단계: 상태 변수 변경

**변경 전:**
```kotlin
var showWriteSheet by remember { mutableStateOf(false) }
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
```

**변경 후:**
```kotlin
var isWritingScreenVisible by remember { mutableStateOf(false) }
// sheetState 제거
```

### 4단계: LazyColumn에 상단 작성 트리거 추가

**변경 전:**
```kotlin
LazyColumn(/* ... */) {
    // [NEW Phase 3] 6번째 아이템마다 광고 삽입
    val itemsWithAds = posts.flatMapIndexed { /* ... */ }
    
    items(itemsWithAds.size, /* ... */) {
        // 게시글 아이템
    }
}
```

**변경 후:**
```kotlin
LazyColumn(/* ... */) {
    // [NEW] 페이스북 스타일 상단 작성 트리거
    item {
        WritePostTrigger(
            onClick = { isWritingScreenVisible = true }
        )
    }
    
    // [NEW Phase 3] 6번째 아이템마다 광고 삽입
    val itemsWithAds = posts.flatMapIndexed { /* ... */ }
    
    items(itemsWithAds.size, /* ... */) {
        // 게시글 아이템
    }
}
```

### 5단계: WritePostTrigger 컴포넌트 추가

```kotlin
/**
 * [NEW] 페이스북 스타일 상단 작성 트리거
 */
@Composable
private fun WritePostTrigger(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 익명 프로필 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_user_circle),
                    contentDescription = "프로필",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 중앙: 작성 트리거 박스
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFFF0F2F5)
            ) {
                Text(
                    text = "오늘 하루는 어땠나요? (익명)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF65676B),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 우측: 이미지 아이콘
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "이미지 추가",
                    tint = Color(0xFF65676B)
                )
            }
        }
        
        // 구분선
        HorizontalDivider(
            thickness = 8.dp,
            color = Color(0xFFF0F2F5)
        )
    }
}
```

### 6단계: ModalBottomSheet를 전체화면 Dialog로 교체

**변경 전:**
```kotlin
if (showWriteSheet) {
    ModalBottomSheet(
        onDismissRequest = { showWriteSheet = false },
        sheetState = sheetState
    ) {
        WritePostSheet(/* ... */)
    }
}
```

**변경 후:**
```kotlin
if (isWritingScreenVisible) {
    FullScreenWriteDialog(
        onPost = { content ->
            viewModel.addPost(content)
            isWritingScreenVisible = false
        },
        onDismiss = { isWritingScreenVisible = false }
    )
}
```

### 7단계: FullScreenWriteDialog 컴포넌트 추가

```kotlin
/**
 * [NEW] 전체화면 글쓰기 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenWriteDialog(
    onPost: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // 전체 화면
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
            topBar = {
                // 상단바: 취소 - 제목 - 게시하기
                TopAppBar(
                    title = {
                        Text(
                            text = "새 게시글 작성",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1F2937)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "취소",
                                tint = Color(0xFF6B7280)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (content.isNotBlank()) {
                                    onPost(content.trim())
                                }
                            },
                            enabled = content.isNotBlank()
                        ) {
                            Text(
                                text = "게시하기",
                                color = if (content.isNotBlank()) 
                                    kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue 
                                else 
                                    Color(0xFFD1D5DB)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .imePadding()
            ) {
                // 텍스트 입력창
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = {
                        Text(
                            text = "오늘 하루는 어땠나요? (익명)",
                            color = Color(0xFF9CA3AF)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 이미지 선택 버튼 (향후 기능)
                OutlinedButton(
                    onClick = { /* TODO: 이미지 선택 */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "이미지 추가",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("이미지 추가 (준비 중)")
                }
            }
        }
    }
}
```

### 8단계: 기존 WritePostSheet 컴포넌트 제거

- `WritePostSheet` 함수 전체 삭제

---

## 📊 변경 사항 요약

| 항목 | Before | After |
|------|--------|-------|
| 진입점 | FAB (우측 하단) | 상단 입력바 (LazyColumn 첫 아이템) |
| 글쓰기 UI | ModalBottomSheet | Dialog (전체화면) |
| 상단바 | 없음 | [X] - [제목] - [게시하기] |
| 디자인 | Material 3 기본 | 페이스북 스타일 |

---

## 🎨 디자인 스펙

### 상단 작성 트리거
- **배경**: 흰색 (#FFFFFF)
- **프로필 아이콘**: 40dp, 회색 배경 (#E0E0E0)
- **입력 박스**: RoundedCornerShape(50.dp), 회색 배경 (#F0F2F5)
- **텍스트**: #65676B
- **구분선**: 8dp 두께, #F0F2F5

### 전체화면 다이얼로그
- **배경**: 흰색 (#FFFFFF)
- **취소 아이콘**: #6B7280
- **게시하기 버튼**: MainPrimaryBlue (활성) / #D1D5DB (비활성)
- **텍스트 입력창**: 테두리 없음, 투명 배경

---

## ⚠️ 주의사항

1. **Import 필수**: Dialog, DialogProperties, CircleShape, RoundedCornerShape 등 추가 import 필요
2. **함수 순서**: WritePostTrigger와 FullScreenWriteDialog는 CommunityScreen 다음에 배치
3. **Preview 유지**: 기존 CommunityScreenWithDummyData는 그대로 유지
4. **빌드 확인**: 각 단계마다 빌드 테스트 권장

---

**작성일**: 2025-12-17  
**작성자**: GitHub Copilot  
**문서 목적**: 페이스북 스타일 UI 개편을 위한 단계별 가이드

