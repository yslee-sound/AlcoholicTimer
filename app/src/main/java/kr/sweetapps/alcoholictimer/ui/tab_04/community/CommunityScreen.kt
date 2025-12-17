package kr.sweetapps.alcoholictimer.ui.tab_04.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
                            WritePostTrigger(onClick = { isWritingScreenVisible = true })
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

        // === 2. 글쓰기 전체 화면 (앞에 덮이는 화면) ===
        // Dialog 대신 AnimatedVisibility를 사용하여 부드러운 Slide Up 구현
        AnimatedVisibility(
            visible = isWritingScreenVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight }, // 화면 아래에서 위로
                animationSpec = tween(durationMillis = 300) // 0.3초 동안 부드럽게
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight }, // 다시 아래로
                animationSpec = tween(durationMillis = 300)
            ),
            modifier = Modifier.align(Alignment.BottomCenter) // 아래쪽에 배치
        ) {
            // 여기가 진짜 글쓰기 화면 내용
            WritePostScreenContent(
                onPost = { content ->
                    viewModel.addPost(content)
                    isWritingScreenVisible = false
                },
                onDismiss = { isWritingScreenVisible = false }
            )
        }
    }
}

/**
 * 글쓰기 화면의 내부 콘텐츠 (별도 Composable로 분리하여 깔끔하게 정리)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritePostScreenContent(
    onPost: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }

    // 전체 화면을 흰색으로 덮음
    Scaffold(
        modifier = Modifier.fillMaxSize(), // 전체 화면 꽉 채우기
        containerColor = Color.White,
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                // 키보드가 올라오면 패딩 자동 조절 (Manifest에 windowSoftInputMode="adjustResize" 필요)
                .imePadding()
        ) {
            // 프로필 영역 (페이스북 느낌)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_user_circle),
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }
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

            Spacer(modifier = Modifier.height(16.dp))

            // 텍스트 입력창
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // 남은 공간 모두 차지
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

            // 하단 툴바 (이미지 추가 등)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { /* TODO: 이미지 선택 로직 */ },
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
    }
}

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
