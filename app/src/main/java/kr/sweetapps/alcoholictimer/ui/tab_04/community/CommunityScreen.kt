package kr.sweetapps.alcoholictimer.ui.tab_04.community

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel(),
    onSettingsClick: () -> Unit = {} // [NEW] 설정 화면으로 이동
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState() // [NEW] 현재 사용자 아바타
    val context = LocalContext.current // [NEW] Context 가져오기 (2025-12-19)

    // 글쓰기 화면 표시 상태
    var isWritingScreenVisible by remember { mutableStateOf(false) }

    // [중요] 글쓰기 화면이 열려있을 때 뒤로가기 버튼 누르면 앱 종료 대신 글쓰기 창 닫기
    BackHandler(enabled = isWritingScreenVisible) {
        isWritingScreenVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // === 1. 메인 리스트 화면 (뒤에 깔리는 화면) ===
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF5F5F5),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.community_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF111111)
                        )
                    },
                    actions = {
                        // 설정 버튼 (우측 상단 톱니바퀴)
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.gearsix),
                                contentDescription = "설정",
                                tint = Color(0xFF111111)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF111111)
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading && posts.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (posts.isEmpty()) {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            WritePostTrigger(
                                onClick = { isWritingScreenVisible = true },
                                currentAvatarIndex = currentUserAvatarIndex // [NEW] 현재 사용자 아바타 전달
                            )
                        }

                        // 광고 및 게시글 리스트 로직 (기존 동일)
                        val itemsWithAds = posts.flatMapIndexed { index, post ->
                            if ((index + 1) % 6 == 0 && index > 0) listOf(post, null) else listOf(post)
                        }

                        items(itemsWithAds.size, key = { index ->
                            val item = itemsWithAds[index]
                            item?.id ?: "ad_$index"
                        }) { index ->
                            val item = itemsWithAds[index]
                            if (item == null) {
                                NativeAdItem()
                            } else {
                                PostItem(
                                    nickname = item.nickname,
                                    timerDuration = item.timerDuration,
                                    content = item.content,
                                    imageUrl = item.imageUrl,
                                    likeCount = item.likeCount,
                                    isLiked = false,
                                    remainingTime = calculateRemainingTime(item.deleteAt),
                                    authorAvatarIndex = item.authorAvatarIndex, // [NEW] 아바타 인덱스 전달
                                    onLikeClick = { viewModel.toggleLike(item.id) },
                                    onCommentClick = { },
                                    onMoreClick = { }
                                )
                            }
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
                        }
                    }
                }
            }
        }

        // === 2. 글쓰기 전체 화면 (최상위 레이어) ===
        // [MODIFIED] Dialog로 변경하여 메인 BottomNavBar를 덮고 키보드와 1:1로 만남 (2025-12-19)
        if (isWritingScreenVisible) {
            Dialog(
                onDismissRequest = { isWritingScreenVisible = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false, // 가로 꽉 차게
                    decorFitsSystemWindows = false   // 시스템 바 영역까지 제어 (Edge-to-Edge)
                )
            ) {
                WritePostScreenContent(
                    viewModel = viewModel,
                    onPost = { content ->
                        viewModel.addPost(content, context) // [MODIFIED] context 전달 (2025-12-19)
                        isWritingScreenVisible = false
                    },
                    onDismiss = { isWritingScreenVisible = false }
                )
            }
        }
    }
}

/**
 * 글쓰기 화면의 내부 콘텐츠 (별도 Composable로 분리하여 깔끔하게 정리)
 * [MODIFIED] 사용자 아바타 연동 + bottomBar 구조 + 이미지 업로드 기능 (2025-12-19)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // [NEW] ExperimentalLayoutApi 추가 (isImeVisible 사용)
@Composable
private fun WritePostScreenContent(
    viewModel: CommunityViewModel, // [NEW] ViewModel 주입
    onPost: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current

    // [NEW] 1. 상태 구독 - 현재 사용자의 아바타 인덱스
    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState()

    // [NEW] 2. 상태 구독 - 선택된 이미지 URI (2025-12-19)
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    // [NEW] 3. Photo Picker 설정 (2025-12-19)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    // 전체 화면을 흰색으로 덮음
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // [FIX] 키보드가 올라오면 Scaffold 전체 높이를 줄여서 bottomBar가 키보드 위로 올라오도록 함 (2025-12-19)
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars, // [FIX] 기본값 사용 - 시스템 바만 계산 (2025-12-19)
        topBar = {
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
                            if (content.isNotBlank()) onPost(content.trim())
                        },
                        enabled = content.isNotBlank()
                    ) {
                        Text(
                            text = "게시하기",
                            color = if (content.isNotBlank())
                                kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue // 테마 색상 사용 권장
                            else Color(0xFFD1D5DB),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        // [NEW] bottomBar로 사진 추가 버튼 이동 (2025-12-19)
        bottomBar = {
            // [FIX] 키보드 가시성에 따라 조건부 패딩 적용
            val isImeVisible = WindowInsets.isImeVisible

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // 배경 흰색으로 고정
                    .windowInsetsPadding(
                        if (isImeVisible) WindowInsets(0) else WindowInsets.navigationBars
                    ) // 키보드 보이면 패딩 없음, 아니면 네비게이션바 높이만큼
            ) {
                // 상단 구분선
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0)
                )

                // 사진 추가 버튼 (목록형)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // [NEW] Photo Picker 실행 (2025-12-19)
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "사진",
                        tint = Color(0xFF4CAF50) // 초록색 아이콘
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "사진 추가",
                        color = Color(0xFF1F2937),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // [FIX] Scaffold가 bottomBar 높이를 자동으로 계산하여 innerPadding에 포함
        ) {
            // [MODIFIED] 프로필 영역 - 실제 사용자 아바타 표시 (2025-12-19)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(all = 16.dp) // [NEW] 개별 패딩 적용
            ) {
                // [NEW] 2 & 3. 실제 아바타 데이터 바인딩
                Image(
                    painter = painterResource(
                        id = kr.sweetapps.alcoholictimer.util.AvatarManager.getAvatarResId(currentUserAvatarIndex)
                    ),
                    contentDescription = "내 아바타",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "익명",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black
                    )
                    Text(
                        text = "모두에게 공개",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // 텍스트 입력창
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 남은 공간 모두 차지
                    .padding(horizontal = 16.dp), // [NEW] 좌우 패딩만 적용
                placeholder = {
                    Text(
                        text = "오늘 하루는 어땠나요? 솔직한 이야기를 들려주세요.",
                        color = Color(0xFF9CA3AF),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, // 밑줄 제거
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            // [NEW] 이미지 미리보기 (2025-12-19)
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 이미지 표시
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "선택된 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // 우측 상단 X 버튼
                    IconButton(
                        onClick = { viewModel.onImageSelected(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "이미지 제거",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * [NEW] 페이스북 스타일 상단 작성 트리거
 * (v2.1) 현재 사용자의 아바타 실시간 표시
 */
@Composable
private fun WritePostTrigger(
    onClick: () -> Unit,
    currentAvatarIndex: Int = 0 // [NEW] 현재 사용자 아바타 인덱스
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
            // [NEW] 좌측: 현재 사용자의 아바타 이미지
            Image(
                painter = painterResource(id = kr.sweetapps.alcoholictimer.util.AvatarManager.getAvatarResId(currentAvatarIndex)),
                contentDescription = "내 프로필",
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape) // 회색 테두리
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
            )

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

/**
 * 빈 상태 표시
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📝",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "아직 게시글이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF666666)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tab 5 디버그 메뉴에서\n테스트 게시글을 생성해 보세요!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF999999),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * 남은 시간 계산 (deleteAt - now)
 */
private fun calculateRemainingTime(deleteAt: com.google.firebase.Timestamp): String {
    val now = System.currentTimeMillis()
    val deleteAtMillis = deleteAt.seconds * 1000
    val diffMillis = deleteAtMillis - now

    if (diffMillis <= 0) return "만료됨"

    val hours = (diffMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((diffMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()

    return when {
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "곧 만료"
    }
}

/**
 * [NEW Phase 3] 네이티브 광고 아이템
 */
@Composable
private fun NativeAdItem() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFBF0))
            .padding(16.dp)
    ) {
        Text(
            text = "Sponsored",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF999999),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📢 Native Ad Placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}
