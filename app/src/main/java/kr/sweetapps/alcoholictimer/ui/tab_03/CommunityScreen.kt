package kr.sweetapps.alcoholictimer.ui.tab_03

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.PostItem
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.CommunityViewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel(),
    onSettingsClick: () -> Unit = {} // [NEW] 설정 화면으로 이동
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState() // [NEW] Pull-to-Refresh 상태 (2025-12-20)
    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState() // [NEW] 현재 사용자 아바타
    val context = LocalContext.current // [NEW] Context 가져오기 (2025-12-19)

    // [UI State] Snackbar를 위한 상태 및 스코프
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 글쓰기 화면 표시 상태
    var isWritingScreenVisible by remember { mutableStateOf(false) }

    // [NEW] Phase 3: 게시글 옵션 바텀 시트
    var selectedPost by remember { mutableStateOf<kr.sweetapps.alcoholictimer.data.model.Post?>(null) }

    // [중요] 글쓰기 화면이 열려있을 때 뒤로가기 버튼 누르면 앱 종료 대신 글쓰기 창 닫기
    BackHandler(enabled = isWritingScreenVisible) {
        isWritingScreenVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // === 1. 메인 리스트 화면 (뒤에 깔리는 화면) ===
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF5F5F5),
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // [FIX] 하단 시스템 바 영역 중복 패딩 제거 (회색 여백 삭제) (2025-12-20)
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
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading && posts.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (posts.isEmpty()) {
                    // [FIX] 게시글이 없을 때도 글쓰기 버튼은 보여야 합니다! (2025-12-19)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. 글쓰기 버튼 (여기 추가됨)
                        WritePostTrigger(
                            onClick = { isWritingScreenVisible = true },
                            currentAvatarIndex = currentUserAvatarIndex
                        )

                        // 2. 나머지 공간에 빈 상태 아이콘 표시 (가운데 정렬)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(onGenerateMock = { viewModel.generateMockData() })
                        }
                    }
                } else {
                    // [NEW] Pull-to-Refresh 적용 (2025-12-20)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshPosts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                                        isLiked = viewModel.isLikedByMe(item),
                                        remainingTime = calculateRemainingTime(item.deleteAt),
                                        currentDays = item.currentDays,
                                        userLevel = item.userLevel,
                                         authorAvatarIndex = item.authorAvatarIndex, // [NEW] 아바타 인덱스 전달
                                         isMine = viewModel.isMyPost(item), // [NEW] Phase 3: 내 글 여부
                                         onLikeClick = { viewModel.toggleLike(item) },
                                         onCommentClick = { },
                                         onMoreClick = { selectedPost = item }, // [NEW] Phase 3: 바텀 시트 열기
                                         onHideClick = {
                                            // 1) 즉시 숨김 처리
                                            viewModel.hidePost(item.id)

                                            // 2) 스낵바로 Undo 제공
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "게시글이 숨겨졌습니다.",
                                                    actionLabel = "되돌리기",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                                )

                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    viewModel.undoHidePost(item.id)
                                                }
                                            }
                                        } // [NEW] Phase 3: 빠른 숨기기 + Undo
                                    )
                                }
                                // [MODIFIED] 디바이더 진하게 (페이스북 스타일) (2025-12-20)
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFBDBDBD))
                            }
                        }
                    }
                }
            }
        }

        // === 2. 글쓰기 전체 화면 (최상위 레이어) ===
        // [MODIFIED] Dialog + 슬라이드 애니메이션 (아래에서 위로) (2025-12-19)
        if (isWritingScreenVisible) {
            Dialog(
                onDismissRequest = { /* 하드웨어 백버튼은 내부 AnimatedVisibility에서 처리 */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false, // 가로 꽉 차게
                    decorFitsSystemWindows = false   // 시스템 바 영역까지 제어 (Edge-to-Edge)
                )
            ) {
                // [NEW] 내부 애니메이션 상태 (2025-12-19)
                var animateVisible by remember { mutableStateOf(false) }

                // [NEW] 다이얼로그가 뜨면 즉시 애니메이션 시작
                LaunchedEffect(Unit) { animateVisible = true }

                // [NEW] 닫기 트리거 함수 (애니메이션 후 종료)
                val triggerClose = {
                    animateVisible = false
                }

                // [NEW] 애니메이션이 끝나면 실제 다이얼로그 닫기
                LaunchedEffect(animateVisible) {
                    if (!animateVisible) {
                        kotlinx.coroutines.delay(300) // 애니메이션 시간 대기
                        isWritingScreenVisible = false // 진짜 종료
                    }
                }

                AnimatedVisibility(
                    visible = animateVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it }, // 화면 아래에서 위로
                        animationSpec = tween(300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it }, // 화면 위에서 아래로
                        animationSpec = tween(300)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    WritePostScreenContent(
                        viewModel = viewModel,
                        onPost = { content ->
                            viewModel.addPost(content, context)
                            triggerClose() // [FIX] 게시 후 애니메이션 종료
                        },
                        onDismiss = { triggerClose() } // [FIX] 뒤로가기 시 애니메이션 종료
                    )
                }
            }
        }

        // === 3. 게시글 옵션 바텀 시트 (Phase 3) ===
        selectedPost?.let { post ->
            ModalBottomSheet(
                onDismissRequest = { selectedPost = null },
                containerColor = Color.White
            ) {
                PostOptionsBottomSheet(
                    post = post,
                    isMyPost = viewModel.isMyPost(post),
                    onDelete = {
                        viewModel.deletePost(post.id)
                        selectedPost = null
                    },
                    onHide = {
                        viewModel.hidePost(post.id)
                        selectedPost = null
                    },
                    onReport = {
                        viewModel.reportPost(post.id, context)
                        selectedPost = null
                    }
                )
            }
        }
    }
}

/**
 * [NEW] Phase 3: 게시글 옵션 바텀 시트
 * 내 글: 삭제만
 * 남의 글: 숨기기, 신고하기
 */
@Composable
private fun PostOptionsBottomSheet(
    post: kr.sweetapps.alcoholictimer.data.model.Post,
    isMyPost: Boolean,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 타이틀
        Text(
            text = if (isMyPost) "게시글 관리" else "게시글 옵션",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
        )

        if (isMyPost) {
            // 내 글: 삭제 메뉴만
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "게시글 삭제",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
            }
        } else {
            // 남의 글: 숨기기, 신고하기
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHide() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "이 게시글 숨기기",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReport() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "게시글 신고하기",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}

/**
 * 글쓰기 화면의 내부 콘텐츠 (별도 Composable로 분리하여 깔끔하게 정리)
 * [MODIFIED] 사용자 아바타 연동 + bottomBar 구조 + 이미지 업로드 기능 + 터치하여 키보드 닫기 + 스크롤 기능 추가 + 뒤로가기 방지 (2025-12-19)
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
    val focusManager = LocalFocusManager.current // [NEW] FocusManager (2025-12-19)
    val scrollState = rememberScrollState() // [NEW] 스크롤 상태 (2025-12-19)
    var showWarningSheet by remember { mutableStateOf(false) } // [NEW] 경고 바텀 시트 표시 상태 (2025-12-19)
    var showPhotoScreen by remember { mutableStateOf(false) } // [NEW] 사진 추가 화면 표시 상태
    var showThirstSlider by remember { mutableStateOf(false) } // [NEW] 갈증 수치 슬라이더 표시 상태

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

    // [NEW] 4. 수정 상태 감지 (2025-12-19)
    val isModified = content.isNotBlank() || selectedImageUri != null

    // [NEW] 5. 뒤로가기 공통 로직 (2025-12-19)
    val onBackAction = {
        if (isModified) {
            showWarningSheet = true
        } else {
            onDismiss()
        }
    }

    // [NEW] 6. 하드웨어 뒤로가기 제어 (2025-12-19)
    BackHandler(enabled = true, onBack = onBackAction)

    // [NEW] 7. 스크롤 시 키보드 자동 숨김 (2025-12-19)
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // 전체 화면을 흰색으로 덮음
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // [FIX] 키보드가 올라오면 Scaffold 전체 높이를 줄여서 bottomBar가 키보드 위로 올라오도록 함 (2025-12-19)
            .pointerInput(Unit) { // [NEW] 화면 터치 시 키보드 닫기 (2025-12-19)
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
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
                    IconButton(onClick = onBackAction) { // [FIX] 뒤로가기 공통 로직 적용 (2025-12-19)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // [CHANGE] X 버튼 -> 뒤로가기 화살표 (2025-12-19)
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (content.isNotBlank()) onPost(content.trim())
                        },
                        enabled = isModified // [FIX] 내용이 있을 때만 활성화 (2025-12-19)
                    ) {
                        Text(
                            text = "게시하기",
                            color = if (isModified)
                                kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue // 테마 색상 사용 권장
                            else Color(0xFFD1D5DB),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        // [REMOVED] bottomBar을 사용하지 않고 모든 입력 요소를 메인 Column으로 이동했습니다.
    ) { innerPadding ->
             Column(
                 modifier = Modifier
                     .fillMaxSize()
                     .padding(innerPadding) // [FIX] Scaffold가 bottomBar 높이를 자동으로 계산하여 innerPadding에 포함
                     .verticalScroll(scrollState) // [NEW] 스크롤 가능하게 설정 (2025-12-19)
                 ,
                 verticalArrangement = Arrangement.Top // [MODIFIED] 모든 요소를 Top에서부터 쌓도록 변경
             ) {
                // [NEW] 디바이더 + 작성자 정보 (Top bar 바로 아래에 노출되도록 이동)
                // 기존에 bottomBar 근처에 있던 작성자 정보 블록을 여기로 옮겨서
                // '새 게시글 작성' 제목줄 바로 아래에 보이게 합니다.
                var currentNickname by remember { mutableStateOf("익명") }
                LaunchedEffect(currentUserAvatarIndex) {
                    try {
                        val repo = kr.sweetapps.alcoholictimer.data.repository.UserRepository(context)
                        currentNickname = repo.getNickname() ?: "익명"
                    } catch (_: Throwable) {
                        // 실패 시 기본값 유지
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = kr.sweetapps.alcoholictimer.util.AvatarManager.getAvatarResId(currentUserAvatarIndex)),
                        contentDescription = "내 프로필",
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentNickname,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )

                        Text(
                            text = "내 프로필",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

            // 텍스트 입력창
             TextField(
                 value = content,
                 onValueChange = { content = it },
                 modifier = Modifier
                     .fillMaxWidth()
                     .heightIn(min = 200.dp) // [FIX] weight(1f) 제거 -> 최소 높이 설정 (스크롤 가능 Column에서는 weight 사용 불가) (2025-12-19)
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
                             .wrapContentHeight() // [FIX] 이미지 비율에 맞게 높이 조절 - 제한 없이 원본 비율대로 표시 (2025-12-19)
                             .clip(RoundedCornerShape(12.dp)),
                         contentScale = ContentScale.FillWidth // [FIX] 가로를 꽉 채우고 세로는 비율 유지 (잘리지 않음) (2025-12-19)
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

            // === moved from bottomBar: 갈증 수치 & 사진 추가 UI (모든 입력 요소를 Column 안으로 이동) ===
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            // 갈증 수치 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThirstSlider = !showThirstSlider }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = "갈증 수치",
                    tint = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "갈증 수치", color = Color(0xFF1F2937), style = MaterialTheme.typography.bodyMedium)
            }

            AnimatedVisibility(
                visible = showThirstSlider,
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300))
            ) {
                var thirstLevel by remember { mutableStateOf(5) }
                fun thirstColor(level: Int): Color = when (level) {
                    in 1..3 -> Color(0xFF4CAF50)
                    in 4..7 -> Color(0xFFFFA726)
                    else -> Color(0xFFE53935)
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(10) { index ->
                        val value = index + 1
                        val selected = thirstLevel == value
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) thirstColor(value) else Color(0xFFF0F0F0))
                                .clickable { thirstLevel = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) Color.White else Color(0xFF374151))
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            // 사진 추가 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPhotoScreen = true }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Image, contentDescription = "사진", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "사진 추가", color = Color(0xFF1F2937), style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
        }
    }

    // [NEW] 작성 중 뒤로가기 경고 바텀 시트 - 페이스북 스타일 (2025-12-19)
    if (showWarningSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWarningSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                // 타이틀 (왼쪽 정렬, 한 줄 제한)
                Text(
                    text = "작성 중인 글을 삭제하시겠습니까?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
                )

                // 게시글 삭제 메뉴 (리스트 아이템 스타일)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showWarningSheet = false
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "게시글 삭제",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1F2937),
                        maxLines = 1
                    )
                }

                // 수정 계속하기 메뉴 (리스트 아이템 스타일)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWarningSheet = false }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "수정 계속하기",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1F2937),
                        maxLines = 1
                    )
                }
            }
        }
    }

    // [NEW] 사진 추가 화면 표시 상태에 따른 AnimatedVisibility
    AnimatedVisibility(
        visible = showPhotoScreen,
        enter = slideInHorizontally(
            initialOffsetX = { it }, // 오른쪽에서 왼쪽으로
            animationSpec = tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it }, // 왼쪽에서 오른쪽으로
            animationSpec = tween(300)
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        PhotoScreen(onDismiss = { showPhotoScreen = false })
    }
}

/**
 * [NEW] 사진 추가 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoScreen(onDismiss: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "사진 추가",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1F2937)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "사진 추가 기능은 추후 구현 예정입니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
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
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape)
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
                    contentDescription = "이미지",
                    tint = Color(0xFF65676B)
                )
            }
        }

        // 하단 구분선 (페이스북 스타일)
        HorizontalDivider(
            thickness = 8.dp,
            color = Color(0xFFF0F2F5)
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
 * [REAL] 구글 애드몹 네이티브 광고
 * 기존의 노란색 Placeholder를 대체합니다.
 */
@Composable
private fun NativeAdItem() {
    val context = LocalContext.current // [NEW] Context 사용

    // 테스트용 광고 ID (배포 시 실제 ID로 교체 필수!)
    // 네이티브 고급 광고 테스트 ID: ca-app-pub-3940256099942544/2247696110
    // [TODO] 배포 전 반드시 애드몹 콘솔에서 발급받은 네이티브 광고 단위 ID로 교체하세요!
    // 현재는 플레이스홀더가 사용됩니다. (테스트용 ID 백업: "ca-app-pub-3940256099942544/2247696110")
    // [FIX] BuildConfig에서 빌드타입(Debug/Release)에 따라 자동으로 주입됩니다.
    val adUnitId = try { BuildConfig.ADMOB_NATIVE_ID } catch (_: Throwable) { "" }

    // 광고가 로드되면 UI를 갱신하기 위한 State
    var nativeAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }

    // 1. 광고 로드 (최초 1회)
    LaunchedEffect(Unit) {
        val adLoader = com.google.android.gms.ads.AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: com.google.android.gms.ads.nativead.NativeAd ->
                nativeAd = ad
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    android.util.Log.e("NativeAd", "광고 로드 실패: ${'$'}{error.message}")
                }
            })
            .withNativeAdOptions(com.google.android.gms.ads.nativead.NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
    }

    // 2. 광고가 로드되었을 때만 표시
    if (nativeAd != null) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                // XML 레이아웃 없이 코드로 뷰 생성 (Compose 호환성 위해)
                val adView = com.google.android.gms.ads.nativead.NativeAdView(ctx)

                // --- 뷰 계층 구조 생성 (카드 형태) ---
                val container = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(32, 32, 32, 32)
                }

                // 1) 상단: 아이콘 + 헤드라인
                val headerRow = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                }

                val iconView = android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(120, 120) // 약 40dp
                }

                val headlineView = android.widget.TextView(ctx).apply {
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(16, 0, 0, 0)
                    setTextColor(android.graphics.Color.BLACK)
                }

                headerRow.addView(iconView)
                headerRow.addView(headlineView)
                container.addView(headerRow)

                // 2) 중간: 광고 문구 (Body)
                val bodyView = android.widget.TextView(ctx).apply {
                    textSize = 14f
                    setPadding(0, 16, 0, 16)
                    setTextColor(android.graphics.Color.DKGRAY)
                    maxLines = 2
                }
                container.addView(bodyView)

                // 3) 하단: 액션 버튼 (설치/자세히보기)
                val callToActionView = android.widget.Button(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0")) // 연회색
                    setTextColor(android.graphics.Color.BLACK)
                }
                container.addView(callToActionView)

                // --- AdView에 뷰 등록 ---
                adView.addView(container)

                adView.iconView = iconView
                adView.headlineView = headlineView
                adView.bodyView = bodyView
                adView.callToActionView = callToActionView

                adView
            },
            update = { adView ->
                // 데이터 바인딩
                val ad = nativeAd!!

                (adView.headlineView as android.widget.TextView).text = ad.headline
                (adView.bodyView as android.widget.TextView).text = ad.body
                (adView.callToActionView as android.widget.Button).text = ad.callToAction ?: "자세히 보기"

                if (ad.icon != null) {
                    (adView.iconView as android.widget.ImageView).setImageDrawable(ad.icon?.drawable)
                    adView.iconView?.visibility = android.view.View.VISIBLE
                } else {
                    adView.iconView?.visibility = android.view.View.GONE
                }

                // [중요] 광고 객체 등록 (클릭 이벤트 처리됨)
                adView.setNativeAd(ad)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

/**
 * 빈 상태 표시
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier, onGenerateMock: () -> Unit = {}) {
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
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGenerateMock) {
            Text("모의 데이터 생성")
        }
    }
}
