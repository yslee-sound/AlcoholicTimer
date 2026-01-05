package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.PostItem
import kr.sweetapps.alcoholictimer.ui.components.ads.NativeAdItem
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

    // [FIX] 전체 일기를 최신순으로만 정렬 (선택 일기 강제 상단 이동 제거) (2025-12-22)
    val allDiaries = remember(diaries) {
        diaries.sortedByDescending { it.timestamp }
    }

    // 스크롤 상태
    val listState = rememberLazyListState()

    // [STATE HOISTING] 네이티브 광고 상태 관리 (2026-01-05)
    var diaryDetailAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }

    // [NEW] 선택된 일기 위치로 초기 스크롤 이동 (2025-12-22)
    LaunchedEffect(targetDiaryId, allDiaries) {
        val index = allDiaries.indexOfFirst { it.id == targetDiaryId }
        if (index != -1) {
            // 즉시 이동하여 해당 일기부터 보이도록 함
            listState.scrollToItem(index)
        }
    }

    // [STATE HOISTING] 네이티브 광고 로드 (2026-01-05)
    // [FIXED] 이미 로드된 광고가 있으면 재로드하지 않음 (2026-01-05)
    LaunchedEffect(Unit) {
        if (diaryDetailAd != null) {
            android.util.Log.d("DiaryDetailFeed", "광고 이미 로드됨, 재로드 스킵")
            return@LaunchedEffect
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context)
            } catch (e: Exception) {
                android.util.Log.w("DiaryDetailFeed", "MobileAds init failed: ${e.message}")
            }
        }

        kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager.getOrLoadAd(
            context = context,
            screenKey = "diary_detail_feed",
            onAdReady = { ad ->
                android.util.Log.d("DiaryDetailFeed", "Native ad ready")
                diaryDetailAd = ad
            },
            onAdFailed = {
                android.util.Log.w("DiaryDetailFeed", "Native ad failed")
            }
        )
    }

    // [REMOVED] DisposableEffect 제거 (2026-01-05)
    // 화면 전환 시 광고가 파괴되지 않도록 함

    // 선택된 일기 ID 추적 (옵션 바텀시트용)
    var selectedDiaryForOptions by remember { mutableStateOf<DiaryEntity?>(null) }

    // [NEW] 삭제 확인 다이얼로그 상태 (2025-12-25)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var diaryToDelete by remember { mutableStateOf<DiaryEntity?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [FIX] 커뮤니티 화면과 동일 - 하단 여백 제거
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diary_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.finished_back)
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
                .background(Color.White) // [FIX] 커뮤니티 피드와 동일하게 흰색 배경 (2025-12-22)
        ) {
            if (allDiaries.isEmpty()) {
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
                // [NEW] 광고가 포함된 리스트 생성 (4개 간격) (2025-12-23)
                val itemsWithAds = remember(allDiaries) {
                    allDiaries.flatMapIndexed { index, diary ->
                        // 4번째 아이템 뒤에 광고 삽입 (마지막 아이템 뒤에는 제외)
                        if ((index + 1) % 4 == 0 && index < allDiaries.lastIndex) {
                            listOf(diary, null) // null은 광고를 의미
                        } else {
                            listOf(diary)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        count = itemsWithAds.size,
                        // [중요] 키 관리: 일기는 ID, 광고는 인덱스 기반 고유 키 사용
                        key = { index ->
                            val item = itemsWithAds[index]
                            item?.id ?: "ad_$index"
                        }
                    ) { index ->
                        val item = itemsWithAds[index]

                        if (item != null) {
                            // === [A] 일기 아이템 렌더링 ===
                            // DiaryEntity를 PostItem에 맞게 변환
                            PostItem(
                                nickname = myNickname,
                                timerDuration = formatTimerDuration(item.timestamp),
                                content = item.content,
                                imageUrl = item.imageUrl.takeIf { it.isNotBlank() },
                                likeCount = 0,
                                isLiked = false,
                                remainingTime = "",
                                currentDays = item.currentDays, // [FIXED] DB에 저장된 값 사용 (2025-12-26)
                                userLevel = item.userLevel, // [FIXED] DB에 저장된 값 사용 (2025-12-26)
                                authorAvatarIndex = myAvatarIndex,
                                thirstLevel = if (item.cravingLevel > 0) item.cravingLevel else null,
                                isMine = true,
                                createdDate = remember(item.timestamp) {
                                    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(item.timestamp))
                                },
                                tagType = item.tagType, // [FIX] DB에 저장된 tagType 사용 (2025-12-23)
                                showTimer = false, // [NEW] 내 일기 화면에서는 타이머 숨김 (2025-12-24)
                                isDiaryMode = true, // [NEW] 일기 모드 활성화 - 하단 좋아요 영역 숨김 (2025-12-26)
                                onLikeClick = { },
                                onCommentClick = { },
                                onMoreClick = {
                                    selectedDiaryForOptions = item
                                },
                                onHideClick = { }
                            )
                        } else {
                            // === [B] 광고 아이템 렌더링 (STATE HOISTING) (2026-01-05) ===
                            NativeAdItem(nativeAd = diaryDetailAd)
                        }

                        // [FIX] 구분선 (일기, 광고 모두 하단에 표시) (2025-12-23)
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color(0xFFBDBDBD)
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
                // [CHANGED] 바로 삭제하지 않고 다이얼로그 표시 (2025-12-25)
                diaryToDelete = diary
                showDeleteDialog = true
                selectedDiaryForOptions = null
            }
        )
    }

    // [NEW] 삭제 확인 다이얼로그 (2025-12-25)
    if (showDeleteDialog && diaryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                diaryToDelete = null
            },
            title = {
                Text(
                    text = stringResource(R.string.diary_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.diary_delete_dialog_message),
                    color = Color(0xFF4A5568)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        diaryToDelete?.let { diary ->
                            onDeleteClick(diary.id)
                        }
                        showDeleteDialog = false
                        diaryToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.diary_delete_confirm),
                        color = Color(0xFFEF4444), // 빨간색으로 강조
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        diaryToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.diary_delete_cancel),
                        color = Color(0xFF6B7280)
                    )
                }
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
                    text = stringResource(R.string.diary_action_edit),
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
                    text = stringResource(R.string.diary_action_delete),
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

/**
 * [REFACTORED] NativeAdItem은 공통 컴포넌트로 분리됨 (2026-01-05)
 * - 위치: ui/components/ads/NativeAdItem.kt
 * - 약 170 라인 감소
 */
