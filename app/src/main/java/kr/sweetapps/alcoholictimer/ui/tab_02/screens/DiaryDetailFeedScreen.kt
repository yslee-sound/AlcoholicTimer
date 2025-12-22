package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.PostItem
import kr.sweetapps.alcoholictimer.data.repository.UserRepository

/**
 * [NEW] 일기 상세 보기 모드 - 커뮤니티 피드 스타일 뷰어 (2025-12-22)
 *
 * 기능:
 * - 선택한 일기를 커뮤니티 피드 스타일로 표시
 * - PostItem을 재사용하여 일관된 UI 제공
 * - 수정/삭제 옵션 제공
 * - 스크롤로 이전 일기 탐색 가능
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailFeedScreen(
    targetDiaryId: Long,
    onBack: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    onDeleteClick: (Long) -> Unit = {},
    diaryViewModel: DiaryViewModel = viewModel()
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }

    // 현재 사용자 닉네임 및 아바타 가져오기
    var myNickname by remember { mutableStateOf("나") }
    var myAvatarIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        myNickname = userRepository.getNickname() ?: "나"
        myAvatarIndex = userRepository.getAvatarIndex()
    }

    // 일기 목록 구독 (최신순)
    val diaries by diaryViewModel.uiState.collectAsState()

    // 선택된 일기를 최상단으로 정렬하고 나머지는 최신순
    val sortedDiaries = remember(diaries, targetDiaryId) {
        val target = diaries.find { it.id == targetDiaryId }
        val others = diaries.filter { it.id != targetDiaryId }.sortedByDescending { it.timestamp }

        if (target != null) {
            listOf(target) + others
        } else {
            others
        }
    }

    // 스크롤 상태 (선택된 일기가 최상단)
    val listState = rememberLazyListState()

    // 선택된 일기 ID 추적 (옵션 바텀시트용)
    var selectedDiaryForOptions by remember { mutableStateOf<DiaryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("일기 보기") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F2F5)) // 커뮤니티 배경색
        ) {
            if (sortedDiaries.isEmpty()) {
                // 빈 상태
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "일기를 찾을 수 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = sortedDiaries,
                        key = { it.id }
                    ) { diary ->
                        // DiaryEntity를 PostItem에 맞게 변환
                        PostItem(
                            nickname = myNickname,
                            timerDuration = formatTimerDuration(diary.timestamp),
                            content = diary.content,
                            imageUrl = diary.imageUrl.takeIf { it.isNotBlank() }, // [NEW] 이미지 URL 전달 (2025-12-22)
                            likeCount = 0, // 일기는 좋아요 없음
                            isLiked = false,
                            remainingTime = "", // 사용 안 함
                            currentDays = calculateDaysSince(diary.timestamp),
                            userLevel = 1, // 사용 안 함
                            authorAvatarIndex = myAvatarIndex,
                            thirstLevel = if (diary.cravingLevel > 0) diary.cravingLevel else null,
                            isMine = true, // 본인 일기
                            onLikeClick = { /* 일기는 좋아요 기능 없음 */ },
                            onCommentClick = { /* 일기는 댓글 기능 없음 */ },
                            onMoreClick = {
                                // 옵션 바텀시트 열기
                                selectedDiaryForOptions = diary
                            },
                            onHideClick = { /* 본인 글이므로 숨기기 없음 */ }
                        )

                        // 구분선
                        HorizontalDivider(
                            thickness = 8.dp,
                            color = Color(0xFFF0F2F5)
                        )
                    }
                }
            }
        }
    }

    // 옵션 바텀시트 (수정/삭제)
    selectedDiaryForOptions?.let { diary ->
        DiaryOptionsBottomSheet(
            diary = diary,
            onDismiss = { selectedDiaryForOptions = null },
            onEdit = {
                selectedDiaryForOptions = null
                onEditClick(diary.id)
            },
            onDelete = {
                selectedDiaryForOptions = null
                onDeleteClick(diary.id)
            }
        )
    }
}

/**
 * [NEW] 일기 옵션 바텀시트 (수정/삭제)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryOptionsBottomSheet(
    diary: DiaryEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // 수정하기
            TextButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "수정하기",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 삭제하기
            TextButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "삭제하기",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * [HELPER] 타임스탬프를 "N일 전" 형식으로 변환
 */
private fun formatTimerDuration(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()

    return when {
        days == 0 -> "오늘"
        days == 1 -> "어제"
        days < 7 -> "${days}일 전"
        days < 30 -> "${days / 7}주 전"
        days < 365 -> "${days / 30}개월 전"
        else -> "${days / 365}년 전"
    }
}

/**
 * [HELPER] 타임스탬프로부터 경과 일수 계산
 */
private fun calculateDaysSince(timestamp: Long): Int {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return (diff / (1000 * 60 * 60 * 24)).toInt() + 1
}

