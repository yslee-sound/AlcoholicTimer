package kr.sweetapps.alcoholictimer.ui.tab_03

import android.app.Activity
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.PostItem
import kr.sweetapps.alcoholictimer.ui.common.CustomGalleryScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.CommunityViewModel
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.Post
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel(),
    onSettingsClick: () -> Unit = {} // 설정 화면으로 이동
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState() // Pull-to-Refresh 상태 (2025-12-20)
    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState() // 현재 사용자 아바타
    val currentNickname by viewModel.currentNickname.collectAsState() // [NEW] 현재 사용자 닉네임 (2025-12-22)
    val context = LocalContext.current // Context 가져오기 (2025-12-19)

    // [UI State] Snackbar를 위한 상태 및 스코프
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 글쓰기 화면 표시 상태
    var isWritingScreenVisible by remember { mutableStateOf(false) }
    // [NEW] 수정할 게시글 상태 (2025-12-22)
    var postToEdit by remember { mutableStateOf<kr.sweetapps.alcoholictimer.data.model.Post?>(null) }
    // 전체 화면 사진 선택 표시 상태 (CommunityScreen 레벨로 끌어올림)
    var isPhotoSelectionVisible by remember { mutableStateOf(false) }
    var photoIsClosing by remember { mutableStateOf(false) }

    // Phase 3: 게시글 옵션 바텀 시트
    var selectedPost by remember { mutableStateOf<kr.sweetapps.alcoholictimer.data.model.Post?>(null) }

    // [중요] 글쓰기 화면이 열려있을 때 뒤로가기 버튼 누르면 앱 종료 대신 글쓰기 창 닫기
    BackHandler(enabled = isWritingScreenVisible) {
        isWritingScreenVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // === 1. 메인 리스트 화면 (뒤에 깔리는 화면) ===

        // 언어 필터 관련 변수 선언 (Scaffold 밖으로 이동)
        val deviceLangRaw = Locale.getDefault().language
        val deviceLang = if (deviceLangRaw.lowercase() == "in") "id" else deviceLangRaw.lowercase()

        val currentFilterState by viewModel.currentLangFilter.collectAsState()

        val myLanguageLabel = remember(deviceLang) {
            when (deviceLang) {
                "ko" -> "한국어"
                "en" -> "English"
                "ja" -> "日本語"
                "id" -> "Bahasa Indo"
                else -> "My Language"
            }
        }

        val selectedTabIndex = if (currentFilterState == null) 1 else 0

        val selectedColor = Color(0xFF000000) // 활성 탭 색상 (검정색)
        val unselectedColor = Color(0xFF9CA3AF) // 비활성 탭 색상
        val dividerColor = Color(0xFFBDBDBD) // 하단 디바이더 색상

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White, // [FIX] 배경색을 흰색으로 변경
            contentWindowInsets = WindowInsets(0.dp),
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
                    // 게시글이 없을 때도 헤더 + 빈 상태 표시
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshPosts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // 헤더 (제목 + 탭)
                            item {
                                Column(modifier = Modifier.background(Color.White)) {
                                    TopAppBar(
                                        title = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.community_title_support),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp, // [FIX] 탭2와 동일한 크기
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF111111)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.community_title_challenge),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp, // [FIX] 탭2와 동일한 크기
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF6366F1) // [FIX] 탭2 '분석'과 동일한 색상
                                                )
                                            }
                                        },
                                        actions = {
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
                                        ),
                                        modifier = Modifier.height(48.dp),
                                        windowInsets = WindowInsets(0, 0, 0, 0)
                                    )

                                    TabRow(
                                        selectedTabIndex = selectedTabIndex,
                                        containerColor = Color.White,
                                        contentColor = selectedColor,
                                        indicator = { tabPositions ->
                                            if (selectedTabIndex < tabPositions.size) {
                                                TabRowDefaults.SecondaryIndicator(
                                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                                    height = 3.dp,
                                                    color = selectedColor
                                                )
                                            }
                                        },
                                        divider = {
                                            HorizontalDivider(thickness = 1.dp, color = dividerColor)
                                        }
                                    ) {
                                        Tab(
                                            selected = selectedTabIndex == 0,
                                            onClick = { viewModel.setLanguageFilter(deviceLang) },
                                            modifier = Modifier.height(40.dp),
                                            text = {
                                                Text(
                                                    text = myLanguageLabel,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 15.sp,
                                                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selectedTabIndex == 0) selectedColor else unselectedColor
                                                )
                                            }
                                        )
                                        Tab(
                                            selected = selectedTabIndex == 1,
                                            onClick = { viewModel.setLanguageFilter(null) },
                                            modifier = Modifier.height(40.dp),
                                            text = {
                                                Text(
                                                    text = "Global",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 15.sp,
                                                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selectedTabIndex == 1) selectedColor else unselectedColor
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // 글쓰기 버튼
                            item {
                                WritePostTrigger(
                                    onClick = { isWritingScreenVisible = true },
                                    currentAvatarIndex = currentUserAvatarIndex
                                )
                            }

                            // 빈 상태 표시
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyState(onWriteClick = { isWritingScreenVisible = true })
                                }
                            }
                        }
                    }
                } else {
                    // NEW Pull-to-Refresh 적용 (2025-12-20)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshPosts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // === [1] 헤더 영역 (제목 + 탭) - 스크롤됨 ===
                            item {
                                Column(modifier = Modifier.background(Color.White)) {
                                    // 1-1. 제목줄 (TopAppBar)
                                    TopAppBar(
                                        title = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.community_title_support),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF111111)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp)) // [NEW] 한 칸 띄우기
                                                Text(
                                                    text = stringResource(R.string.community_title_challenge),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF6366F1) // 보라색 (분석과 동일)
                                                )
                                            }
                                        },
                                        actions = {
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
                                        ),
                                        modifier = Modifier.height(48.dp),
                                        windowInsets = WindowInsets(0, 0, 0, 0)
                                    )

                                    // 1-2. 탭 바 (TabRow)
                                    TabRow(
                                        selectedTabIndex = selectedTabIndex,
                                        containerColor = Color.White,
                                        contentColor = selectedColor,
                                        indicator = { tabPositions ->
                                            if (selectedTabIndex < tabPositions.size) {
                                                TabRowDefaults.SecondaryIndicator(
                                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                                    height = 3.dp,
                                                    color = selectedColor
                                                )
                                            }
                                        },
                                        divider = {
                                            HorizontalDivider(
                                                thickness = 1.dp,
                                                color = dividerColor
                                            )
                                        }
                                    ) {
                                        Tab(
                                            selected = selectedTabIndex == 0,
                                            onClick = { viewModel.setLanguageFilter(deviceLang) },
                                            modifier = Modifier.height(40.dp),
                                            text = {
                                                Text(
                                                    text = myLanguageLabel,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 15.sp,
                                                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selectedTabIndex == 0) selectedColor else unselectedColor
                                                )
                                            }
                                        )
                                        Tab(
                                            selected = selectedTabIndex == 1,
                                            onClick = { viewModel.setLanguageFilter(null) },
                                            modifier = Modifier.height(40.dp),
                                            text = {
                                                Text(
                                                    text = "Global",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 15.sp,
                                                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selectedTabIndex == 1) selectedColor else unselectedColor
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // === [2] 글쓰기 트리거 ===
                            item {
                                WritePostTrigger(
                                    onClick = { isWritingScreenVisible = true },
                                    currentAvatarIndex = currentUserAvatarIndex
                                )
                            }

                            // === [3] 광고 및 게시글 리스트 ===

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
                                         authorAvatarIndex = item.authorAvatarIndex, // 아바타 인덱스 전달
                                         thirstLevel = item.thirstLevel,
                                          isMine = viewModel.isMyPost(item), // Phase 3: 내 글 여부
                                          tagType = item.tagType, // [NEW] 태그 타입 전달 (2025-12-23)
                                          onLikeClick = { viewModel.toggleLike(item) },
                                          onCommentClick = { },
                                          onMoreClick = { selectedPost = item }, // Phase 3: 바텀 시트 열기
                                          onHideClick = {
                                             // 1) 즉시 숨김 처리
                                             viewModel.hidePost(item.id)

                                             // 2) 스낵바로 Undo 제공
                                             coroutineScope.launch {
                                                 val result = snackbarHostState.showSnackbar(
                                                     message = "게시글이 숨겨졌습니다.",
                                                     actionLabel = "되돌리기",
                                                     duration = SnackbarDuration.Short
                                                 )

                                                 if (result == SnackbarResult.ActionPerformed) {
                                                     viewModel.undoHidePost(item.id)
                                                 }
                                             }
                                          } // Phase 3: 빠른 숨기기 + Undo
                                    )
                                }
                                // MODIFIED 디바이더 진하게 (페이스북 스타일) (2025-12-20)
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFBDBDBD))
                            }
                        }
                    }
                } // else 닫기
            } // Box 닫기 (innerPadding)
        } // Scaffold 닫기

        // === 2. 글쓰기 전체 화면 (최상위 레이어) ===
        // MODIFIED Dialog + 슬라이드 애니메이션 (아래에서 위로) (2025-12-19)
        if (isWritingScreenVisible) {
            Dialog(
                onDismissRequest = { /* 하드웨어 백버튼은 내부 AnimatedVisibility에서 처리 */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false, // 가로 꽉 차게
                    decorFitsSystemWindows = false   // 시스템 바 영역까지 제어 (Edge-to-Edge)
                )
            ) {
                // NEW 내부 애니메이션 상태 (2025-12-19)
                var animateVisible by remember { mutableStateOf(false) }

                // NEW 다이얼로그가 뜨면 즉시 애니메이션 시작
                LaunchedEffect(Unit) { animateVisible = true }

                // NEW 닫기 트리거 함수 (애니메이션 후 종료)
                val triggerClose = {
                    animateVisible = false
                }

                // NEW 애니메이션이 끝나면 실제 다이얼로그 닫기
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
                        currentNickname = currentNickname, // [NEW] ViewModel에서 받은 닉네임 전달 (2025-12-22)
                        postToEdit = postToEdit, // [NEW] 수정할 게시글 전달 (2025-12-22)
                        onPost = {
                            postToEdit = null // [NEW] 완료 시 초기화 (2025-12-22)
                            triggerClose()
                        },
                        onDismiss = {
                            postToEdit = null // [NEW] 취소 시 초기화 (2025-12-22)
                            triggerClose()
                        },
                        onOpenPhoto = {
                            // [NEW] 광고 억제 활성화 - 카메라/갤러리 복귀 시 광고 차단 (2025-12-22)
                            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isAdSuppressed = true

                            // [핵심] 시간 기반 억제 설정 - 현재 시간부터 10초간 광고 노출 금지 (2025-12-22)
                            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.lastAdSuppressedTime = System.currentTimeMillis()
                            android.util.Log.d("CommunityScreen", "광고 억제 설정: 10초간 광고 차단 시작")

                            // 글쓰기 다이얼로그를 닫지 않고, 그 위에 사진 선택 Dialog를 띄웁니다. (스택 방식)
                            isPhotoSelectionVisible = true
                        }
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
                     post,
                     isMyPost = viewModel.isMyPost(post),
                     onEdit = { // [NEW] 수정 버튼 콜백 (2025-12-22)
                         postToEdit = post
                         selectedPost = null
                         isWritingScreenVisible = true
                     },
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

        // === 전체 화면 사진 선택: Dialog로 변경하여 하단 네비게이션을 덮도록 함 ===
        if (isPhotoSelectionVisible) {
            Dialog(
                onDismissRequest = {
                    // start exit animation; actual hiding happens after animation completes
                    /* handled inside */
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                // 내부 애니메이션 상태: Dialog가 보여지는 동안 animateVisible을 켜고
                // 닫을 때는 animateVisible을 끄고 애니메이션이 끝난 뒤 isPhotoSelectionVisible=false로 설정
                var animateVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) { animateVisible = true }

                val triggerClosePhoto = {
                    animateVisible = false
                }

                LaunchedEffect(animateVisible) {
                    if (!animateVisible) {
                        // wait for exit animation to finish before removing dialog
                        kotlinx.coroutines.delay(300)
                        isPhotoSelectionVisible = false
                    }
                }

                AnimatedVisibility(
                    visible = animateVisible,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FullScreenPhotoModal(onDismiss = { triggerClosePhoto() }) {
                        CustomGalleryScreen(
                            onImageSelected = { uri ->
                                try {
                                    viewModel.onImageSelected(uri)
                                } catch (e: Exception) {
                                    android.util.Log.e("CommunityScreen", "onImageSelected failed", e)
                                }
                                // close with animation
                                triggerClosePhoto()
                            },
                            onClose = { triggerClosePhoto() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Phase 3: 게시글 옵션 바텀 시트
 * 내 글: 삭제만
 * 남의 글: 숨기기, 신고하기
 */
@Composable
private fun PostOptionsBottomSheet(
    post: kr.sweetapps.alcoholictimer.data.model.Post,
    isMyPost: Boolean,
    onEdit: () -> Unit, // [NEW] 수정 콜백 (2025-12-22)
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
            // [NEW] 수정 버튼 추가 (2025-12-22)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "게시글 수정",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
            }

            // 내 글: 삭제 메뉴
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
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
 * [MODIFIED] 일기 모드 지원 추가 (2025-12-22)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // [NEW] ExperimentalLayoutApi 추가 (isImeVisible 사용)
@Composable
fun WritePostScreenContent( // [MODIFIED] private 제거 -> public (2025-12-22)
    viewModel: CommunityViewModel, // [NEW] ViewModel 주입
    currentNickname: String, // [NEW] ViewModel에서 전달받은 닉네임 (2025-12-22)
    isDiaryMode: Boolean = false, // [NEW] 일기 모드 여부 (2025-12-22)
    postToEdit: Post? = null, // [NEW] 수정할 게시글 (2025-12-22)
    onPost: (String) -> Unit,
    onSaveDiary: (Post) -> Unit = {}, // [NEW] 일기 저장 콜백 (2025-12-22)
    onDismiss: () -> Unit,
    onOpenPhoto: () -> Unit // [NEW] 사진 선택 화면 열기 콜백 (네비게이션 호출)
) {
    // [MODIFIED] 수정 모드인 경우 기존 내용으로 초기화 (2025-12-22)
    val isEditMode = postToEdit != null

    // [DEBUG] 수정 모드 확인 로그 (2025-12-23)
    LaunchedEffect(postToEdit, isDiaryMode, isEditMode) {
        android.util.Log.d("WritePostScreen", "수정 모드 확인: isDiaryMode=$isDiaryMode, isEditMode=$isEditMode, postToEdit=${postToEdit?.content?.take(20)}")
    }

    // [NEW] 일기 모드에서 커뮤니티 공유 여부 (2025-12-22)
    var isShareToCommunity by remember { mutableStateOf(false) }

    // [FIX] postToEdit를 key로 사용하여 수정 모드 진입 시 자동으로 내용 채우기 (2025-12-23)
    var textFieldValue by remember(postToEdit) {
        mutableStateOf(
            if (postToEdit != null) {
                TextFieldValue(
                    text = postToEdit.content,
                    selection = androidx.compose.ui.text.TextRange(postToEdit.content.length)
                )
            } else {
                TextFieldValue("")
            }
        )
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current // [NEW] FocusManager (2025-12-19)
    var showWarningSheet by remember { mutableStateOf(false) } // [NEW] 경고 바텀 시트 표시 상태 (2025-12-19)
    // 하단 패널 상태: 갈증 수치 패널을 토글하기 위한 상태
    var showThirstSlider by remember { mutableStateOf(false) }
    // Note: showPhotoScreen handled via external navigation callback (onOpenPhoto)

    // [MODIFIED] 수정 모드인 경우 기존 값으로 초기화 (2025-12-22)
    var selectedLevel by remember(postToEdit) {
        mutableStateOf<Int?>(postToEdit?.thirstLevel)
    }

    // [FIX] 이미지 복원만 LaunchedEffect에서 처리 (2025-12-23)
    LaunchedEffect(postToEdit) {
        if (postToEdit == null) {
            viewModel.clearSelectedImage()
        } else {
            // 기존 이미지 복원
            if (!postToEdit.imageUrl.isNullOrBlank()) {
                try {
                    viewModel.onImageSelected(android.net.Uri.parse(postToEdit.imageUrl))
                } catch (e: Exception) {
                    android.util.Log.e("WritePostScreen", "이미지 복원 실패: ${postToEdit.imageUrl}", e)
                }
            } else {
                viewModel.clearSelectedImage()
            }
        }
    }

    // [DELETED] thirstColor 함수 제거 - ThirstColorUtil 사용 (2025-12-22)

    // [NEW] 1. 상태 구독 - 현재 사용자의 아바타 인덱스
    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState()

    // [NEW] 로딩 상태 구독 - 업로드 진행 중이면 입력을 잠급니다
    val isLoading by viewModel.isLoading.collectAsState()

    // [NEW] 2. 상태 구독 - 선택된 이미지 URI (2025-12-19)
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    // --- 권한 요청 및 처리 상태 ---
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    // Launcher to request multiple permissions
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms: Map<String, Boolean> ->
        // perms: Map<String, Boolean>
        val allGranted = perms.values.all { it }
        if (allGranted) {
            // 모든 권한 허용일 경우 상위 콜백을 통해 전체 화면 갤러리를 연다
            try {
                onOpenPhoto()
            } catch (_: SecurityException) {
                Toast.makeText(context, "권한 문제로 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
              // done
         } else {
            // Not all granted -> check for permanent denial
            val activity = context as? Activity
            var anyPermanentDenied = false
            perms.forEach { (perm, granted) ->
                if (!granted) {
                    val shouldShow = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, perm) } ?: true
                    if (!shouldShow) anyPermanentDenied = true
                }
            }

            if (anyPermanentDenied) {
                showPermissionSettingsDialog = true
            } else {
                Toast.makeText(context, "사진을 업로드하려면 갤러리 및 카메라 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper to start permission flow when user taps '사진 추가'
    val requestPermissionsAndOpen: () -> Unit = {
        // UX: open gallery UI immediately so user sees feedback; MediaStore will show empty list if no permission
        onOpenPhoto()

         // Build required permissions list per Android version
         val perms = mutableListOf<String>()
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             perms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
         } else {
             perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
         }
         perms.add(android.Manifest.permission.CAMERA)

        // Check currently granted
        val allGranted = perms.all { p ->
            ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            try {
                onOpenPhoto()
            } catch (_: SecurityException) {
                Toast.makeText(context, "권한 문제로 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Launch permission request
            multiplePermissionLauncher.launch(perms.toTypedArray())
        }
     }

    // [MODIFIED] 수정 모드인 경우 기존 태그로 초기화 (2025-12-22)
    var selectedTag by remember(postToEdit) {
        mutableStateOf(postToEdit?.tagType?.takeIf { it.isNotBlank() } ?: "diary")
    }

    val placeholderText = when (selectedTag) {
        "diary" -> "오늘 하루는 어땠나요? 솔직한 이야기를 들려주세요."
        "thanks" -> "오늘 웃게 된 일이나 고마운 순간이 있었나요? 사소한 것도 좋아요. ✨"
        "reflect" -> "아쉬웠던 점이나 내일을 위한 다짐을 적어보세요. 🌙"
        else -> "오늘 하루는 어땠나요? 솔직한 이야기를 들려주세요."
    }

    // [FIX] 변경 사항 감지 로직 (Dirty Check) (2025-12-23)
    val isModified = remember(textFieldValue, selectedLevel, selectedImageUri, selectedTag, postToEdit) {
        if (postToEdit == null) {
            // [신규 작성 모드] 내용이 있거나 사진이 있으면 수정된 것으로 간주
            textFieldValue.text.isNotBlank() || selectedImageUri != null
        } else {
            // [수정 모드] 원본 데이터와 현재 상태를 비교
            val contentChanged = textFieldValue.text.trim() != postToEdit.content.trim()
            val levelChanged = selectedLevel != postToEdit.thirstLevel
            val tagChanged = selectedTag != postToEdit.tagType

            // 이미지 변경 여부 (URL 문자열 비교)
            val currentUriString = selectedImageUri?.toString() ?: ""
            val originalUrlString = postToEdit.imageUrl ?: ""
            val imageChanged = currentUriString != originalUrlString

            contentChanged || levelChanged || tagChanged || imageChanged
        }
    }

    // [NEW] 뒤로가기 공통 로직
    val onBackAction = {
        if (isModified) {
            showWarningSheet = true
        } else {
            onDismiss()
        }
    }

    // NEW IME(키보드) 상태를 구독하여, 키보드가 올라올 때 하단 패널들을 자동으로 닫습니다.
    // WindowInsets.isImeVisible는 @Composable 컨텍스트에서만 안전하게 읽을 수 있으므로
    // 여기서는 컴포저블에서 직접 값을 읽고 LaunchedEffect로 관찰합니다.
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            // 키보드가 올라오면 하단의 패널은 닫음
            showThirstSlider = false
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
                        text = when {
                            isDiaryMode && isEditMode -> "일기 수정" // [FIX] 일기 수정 모드 (2025-12-23)
                            isDiaryMode -> "일기 작성" // [NEW] 일기 작성 모드 (2025-12-22)
                            isEditMode -> "게시글 수정"
                            else -> "새 게시글 작성"
                        },
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
                            // Allow posting when either text exists or an image is selected
                            if ((textFieldValue.text.isNotBlank() || selectedImageUri != null) && !isLoading) {
                                val payload = textFieldValue.text.trim()
                                try {
                                    // [MODIFIED] 일기 모드, 수정 모드, 신규 작성 모드 분기 (2025-12-22)
                                    if (isDiaryMode) {
                                        // 일기 모드: 로컬 저장 + 선택적 커뮤니티 공유
                                        val diaryEntry = Post(
                                            content = payload,
                                            tagType = selectedTag,
                                            thirstLevel = selectedLevel,
                                            imageUrl = selectedImageUri?.toString() ?: "",
                                            nickname = currentNickname,
                                            timerDuration = "",
                                            likeCount = 0,
                                            likedBy = emptyList(),
                                            currentDays = 0,
                                            userLevel = 0,
                                            createdAt = com.google.firebase.Timestamp.now(),
                                            deleteAt = com.google.firebase.Timestamp.now(),
                                            authorAvatarIndex = 0,
                                            authorId = "",
                                            languageCode = ""
                                        )

                                        // 로컬 일기 저장
                                        onSaveDiary(diaryEntry)

                                        // 커뮤니티 공유가 체크되었으면 업로드 수행
                                        if (isShareToCommunity) {
                                            viewModel.addPost(
                                                content = payload,
                                                context = context,
                                                tagType = selectedTag,
                                                thirstLevel = selectedLevel,
                                                onSuccess = { onPost(payload) }
                                            )
                                        } else {
                                            // 공유 안 함 -> 바로 닫기
                                            onPost(payload)
                                        }
                                    } else if (isEditMode && postToEdit != null) {
                                        // 수정 모드: updatePost 호출
                                        viewModel.updatePost(
                                            postId = postToEdit.id,
                                            newContent = payload,
                                            newTagType = selectedTag,
                                            newThirstLevel = selectedLevel,
                                            onSuccess = {
                                                onPost(payload)
                                            }
                                        )
                                    } else {
                                        // 신규 작성 모드: addPost 호출
                                        viewModel.addPost(
                                            content = payload,
                                            context = context,
                                            tagType = selectedTag,
                                            thirstLevel = selectedLevel,
                                            onSuccess = {
                                                onPost(payload)
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CommunityScreen", "Post operation failed", e)
                                }
                            }
                        },
                        // 시각적으로는 활성처럼 보이게 하되 실제 클릭은 onClick에서 막음
                        enabled = (isLoading || isModified)
                    ) {
                        if (isLoading) {
                            // 작은 로딩 인디케이터를 버튼 내부에 표시
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    isDiaryMode && isEditMode -> "수정 완료" // [FIX] 일기 수정 모드 (2025-12-23)
                                    isDiaryMode -> "저장" // [NEW] 일기 작성 모드 (2025-12-22)
                                    isEditMode -> "수정완료"
                                    else -> "게시하기"
                                },
                                color = if (isModified)
                                    kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
                                else Color(0xFFD1D5DB),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        // RESTORE 글쓰기 화면의 하단 바를 원래대로 복원합니다.
        bottomBar = {
            val isImeVisible = WindowInsets.isImeVisible

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(if (isImeVisible) WindowInsets(0) else WindowInsets.navigationBars)
            ) {
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // [FIX] 패널 열기 전에 키보드를 내립니다. (상호 배타적 동작 보장)
                            focusManager.clearFocus()
                            showThirstSlider = !showThirstSlider
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Restaurant, contentDescription = "갈증 수치", tint = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "갈증 수치", color = Color(0xFF1F2937), style = MaterialTheme.typography.bodyMedium)
                }

                if (showThirstSlider) {
                     // selectedLevel이 null이면 모두 비선택 상태(회색) 표시
                     LazyRow(
                          modifier = Modifier.fillMaxWidth(),
                          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                          items(10) { index ->
                              val value = index + 1
                              val selected = selectedLevel == value
                              Box(
                                  modifier = Modifier
                                      .size(35.dp)
                                      .clip(RoundedCornerShape(12.dp))
                                      .background(if (selected) kr.sweetapps.alcoholictimer.util.ThirstColorUtil.getColor(value) else Color(0xFFF0F0F0)) // [MODIFIED] ThirstColorUtil 사용 (2025-12-22)
                                      .then(
                                          if (!isLoading) Modifier.clickable { selectedLevel = value } else Modifier
                                      ),
                                   contentAlignment = Alignment.Center
                               ) {
                                  Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) Color.White else Color(0xFF374151))
                              }
                          }
                      }
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!isLoading) Modifier.clickable {
                            // [NEW] 사진 추가: 키보드 내리고 권한 체크 및 요청 후 풀스크린 갤러리 열기
                            focusManager.clearFocus()
                            requestPermissionsAndOpen()
                        } else Modifier)
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
     ) { innerPadding ->
             // [NEW] 스크롤 상태: 화면 콘텐츠가 길어질 경우 위아래로 스크롤 가능하게 함
             val scrollState = rememberScrollState()

             // [REMOVED] 자동 스크롤 제거 - 사용자가 작성하던 위치 유지 (2025-12-23)
             // LaunchedEffect(selectedImageUri) {
             //     if (selectedImageUri != null) {
             //         localScope.launch { scrollState.animateScrollTo(Int.MAX_VALUE) }
             //     }
             // }

             Column(
                 modifier = Modifier
                     .fillMaxSize()
                     .padding(innerPadding) // Scaffold가 bottomBar 높이를 자동으로 계산하여 innerPadding에 포함
                     .verticalScroll(scrollState), // [NEW] 스크롤 가능하게 변경
                 verticalArrangement = Arrangement.Top // [MODIFIED] 모든 요소를 Top에서부터 쌓도록 변경
             ) {
                // [NEW] 디바이더 + 작성자 정보 (Top bar 바로 아래에 노출되도록 이동)
                // 기존에 bottomBar 근처에 있던 작성자 정보 블록을 여기로 옮겨서
                // '새 게시글 작성' 제목줄 바로 아래에 보이게 합니다.
                // [DELETED] var currentNickname by remember { mutableStateOf("") } - ViewModel에서 관리 (2025-12-22)
                // [DELETED] LaunchedEffect(Unit) { ... } - ViewModel에서 로드 (2025-12-22)

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

                // [MODIFIED] 상단 작성자 정보 영역 Row (2025-12-22)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 1. 아바타 이미지
                    Image(
                        painter = painterResource(id = kr.sweetapps.alcoholictimer.util.AvatarManager.getAvatarResId(currentUserAvatarIndex)),
                        contentDescription = "내 프로필",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            .background(Color(0xFFF5F5F5))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 2. 닉네임 및 알약 2개 영역 Column
                    Column(
                        modifier = Modifier.weight(1f) // 남은 공간 차지
                    ) {
                        // [FIX] 닉네임 + 갈증 수치 표시 Row (2025-12-23)
                        val displayNickname = if (currentNickname.isNotBlank()) currentNickname else "익명"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 1. 닉네임
                            Text(
                                text = displayNickname,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF111827),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            // 2. [복구] 갈증 수치 표시 (PostItem과 동일한 스타일)
                            if (selectedLevel != null && selectedLevel!! > 0) {
                                Spacer(modifier = Modifier.width(4.dp))

                                // 구분자
                                Text(
                                    text = " - ",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF111111)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                // 색상 박스 (숫자)
                                val badgeColor = kr.sweetapps.alcoholictimer.util.ThirstColorUtil.getColor(selectedLevel!!)
                                Box(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .wrapContentWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedLevel.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // "갈증" 텍스트 (검은색)
                                Text(
                                    text = " 갈증",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF111111)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // [수정 2, 3] 알약 2개를 담는 Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // --- 알약 1: 레벨 및 일차 정보 ---
                            // [FIX] 수정 모드일 때는 일기 작성 당시의 레벨/일차 정보 사용 (2025-12-23)
                            val levelInfoText = if (postToEdit != null) {
                                // 수정 모드: 저장된 일기의 레벨/일차 사용
                                "LV.${postToEdit.userLevel} · ${postToEdit.currentDays}일차"
                            } else {
                                // 작성 모드: 현재 타이머 상태 사용
                                val tab03Vm: kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab03ViewModel = viewModel()
                                val levelDays by tab03Vm.levelDays.collectAsState()
                                val levelNumber = if (levelDays == 0) 0 else kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelNumber(levelDays) + 1
                                "LV.$levelNumber · ${levelDays}일차"
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue.copy(alpha = 0.1f), // 연한 하늘색 배경
                            ) {
                                Text(
                                    text = levelInfoText, // [FIX] 계산된 텍스트 사용 (2025-12-23)
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // --- 알약 2: 챌린지 공유 토글 (일기 모드일 때만) ---
                            if (isDiaryMode) {
                                Spacer(modifier = Modifier.width(8.dp)) // 알약 사이 간격

                                // 클릭 가능한 커스텀 칩 (스타일 통일을 위해 Surface 사용)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    // 체크 여부에 따라 배경색 변경 (진한 하늘색 vs 연한 하늘색)
                                    color = if (isShareToCommunity)
                                        kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
                                    else
                                        kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { isShareToCommunity = !isShareToCommunity }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isShareToCommunity) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            // 체크 여부에 따라 아이콘/글자색 변경 (흰색 vs 하늘색)
                                            tint = if (isShareToCommunity)
                                                Color.White
                                            else
                                                kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "챌린지 공유",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (isShareToCommunity)
                                                    Color.White
                                                else
                                                    kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // [MODIFIED] 주제 선택 칩 - 사진 스타일로 변경 (2025-12-23)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()) // 가로 스크롤 허용
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 태그 데이터 정의 (tagKey, tagName, selectedBgColor, selectedTextColor)
                    data class TagInfo(val key: String, val name: String, val bgColor: Color, val textColor: Color)
                    val tags = listOf(
                        TagInfo("diary", "오늘의 일기", Color(0xFF7C4DFF), Color.White), // 밝은 보라 (Material Purple) + 흰색
                        TagInfo("thanks", "오늘 감사할 일", Color(0xFF00BFA5), Color.White), // 민트 그린 (Material Teal) + 흰색
                        TagInfo("reflect", "오늘 반성할 일", Color(0xFFFF6F00), Color.White) // 따뜻한 오렌지 (Material Orange) + 흰색
                    )

                    tags.forEach { tag ->
                        val isSelected = selectedTag == tag.key

                        Surface(
                            modifier = Modifier.clickable { if (!isLoading) selectedTag = tag.key },
                            shape = RoundedCornerShape(20.dp), // 둥근 모서리
                            color = if (isSelected) tag.bgColor else Color(0xFFF5F5F5), // 선택: 해당 색상 배경, 미선택: 연한 회색
                            border = null // 테두리 제거
                        ) {
                            Text(
                                text = tag.name,
                                color = if (isSelected) tag.textColor else Color(0xFF9E9E9E), // 선택: 각 태그별 색상, 미선택: 진한 회색
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp // 텍스트 크기 축소
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

            // 텍스트 입력창
             // Compute cursor/line metrics for spacer calculation
             val lineHeightDp = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.fontSize.toDp() }
             val totalLines = textFieldValue.text.count { it == '\n' } + 1
             val cursorOffset = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
             val cursorLine = textFieldValue.text.take(cursorOffset).count { it == '\n' } + 1
             val minLines = 4
             val maxLines = maxOf(minLines, totalLines)
             val desiredDistanceLines = 4 // 사진은 커서로부터 4줄 아래에 위치
             val currentDistanceLines = (maxLines - cursorLine + 1)
             val extraLinesNeeded = maxOf(0, desiredDistanceLines - currentDistanceLines)

             TextField(
                  value = textFieldValue,
                  onValueChange = { if (!isLoading) textFieldValue = it },
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp) // 좌우 패딩
                      .onFocusChanged { state ->
                          // 입력창에 포커스가 생기면 하단 패널들을 닫아 키보드가 정상 동작하도록 함
                          if (state.isFocused) {
                              showThirstSlider = false
                          }
                      },
                  placeholder = {
                    Text(
                        text = placeholderText,
                        color = Color(0xFF9CA3AF),
                        style = MaterialTheme.typography.bodyLarge
                    )
                 },
                 minLines = minLines,
                 isError = false, // [FIX] 에러 상태 강제 해제 (2025-12-22)
                 colors = TextFieldDefaults.colors(
                     focusedContainerColor = Color.Transparent,
                     unfocusedContainerColor = Color.Transparent,
                     disabledContainerColor = Color.Transparent, // [FIX] 로딩 중에도 투명 배경 유지 (2025-12-22)
                     focusedIndicatorColor = Color.Transparent, // 밑줄 제거
                     unfocusedIndicatorColor = Color.Transparent,
                     disabledIndicatorColor = Color.Transparent, // [FIX] 로딩 중 밑줄도 투명 유지 (2025-12-22)
                     disabledTextColor = Color(0xFF6B7280) // [FIX] 로딩 중 텍스트는 회색으로 (2025-12-22)
                 ),
                 textStyle = MaterialTheme.typography.bodyLarge,
                 enabled = !isLoading // 비활성화 상태 추가
             )

             // Spacer to ensure photo stays desiredDistanceLines below the cursor
             if (extraLinesNeeded > 0) {
                 Spacer(modifier = Modifier.height(lineHeightDp * extraLinesNeeded))
             }

             // NEW 이미지 미리보기 (2025-12-19)
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
                         enabled = !isLoading,
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

            // [NEW] 작성 중 뒤로가기 경고 바텀 시트 - 페이스북 스타일 (2025-12-19)
            if (showWarningSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showWarningSheet = false },
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        // [FIX] 타이틀 문구 분기 처리 (2025-12-23)
                        val titleText = if (isEditMode) "수정을 취소하시겠습니까?" else "작성 중인 글을 삭제하시겠습니까?"

                        // 타이틀 (왼쪽 정렬, 한 줄 제한)
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
                        )

                        // [FIX] 첫 번째 버튼 (취소/삭제) 분기 처리 (2025-12-23)
                        val actionText = if (isEditMode) "변경사항 버리기" else "게시글 삭제"
                        val actionIcon = if (isEditMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Delete
                        val actionColor = if (isEditMode) Color(0xFF1F2937) else Color(0xFFEF4444)

                        // 게시글 삭제/변경사항 버리기 메뉴 (리스트 아이템 스타일)
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
                                imageVector = actionIcon,
                                contentDescription = null,
                                tint = actionColor
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = actionColor,
                                maxLines = 1
                            )
                        }

                        // [FIX] 두 번째 버튼 (계속 작성하기) 문구 분기 (2025-12-23)
                        val continueText = if (isEditMode) "수정 계속하기" else "작성 계속하기"

                        // 수정/작성 계속하기 메뉴 (리스트 아이템 스타일)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWarningSheet = false }
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = Color(0xFF1F2937)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = continueText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF1F2937),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * 페이스북 스타일 상단 작성 트리거
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
        }

        // 하단 구분선 (기본 디바이더 스타일)
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFBDBDBD)
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
 * REAL 구글 애드몹 네이티브 광고
 * 기존의 노란색 Placeholder를 대체합니다.
 */
@Composable
private fun NativeAdItem() {
    val context = LocalContext.current

    val adUnitId = try { BuildConfig.ADMOB_NATIVE_ID } catch (_: Throwable) { "ca-app-pub-3940256099942544/2247696110" }

    var nativeAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }

    // 1. 광고 로드 (최초 1회)
    LaunchedEffect(Unit) {
        try {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context)
            } catch (initEx: Exception) {
                android.util.Log.w("NativeAd", "MobileAds.initialize failed: ${initEx.message}")
            }
            val adLoader = com.google.android.gms.ads.AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad: com.google.android.gms.ads.nativead.NativeAd ->
                    nativeAd = ad
                }
                .withNativeAdOptions(com.google.android.gms.ads.nativead.NativeAdOptions.Builder().build())
                .build()

            try {
                adLoader.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            } catch (se: SecurityException) {
                android.util.Log.w("NativeAd", "Ad load blocked by SecurityException: ${se.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("NativeAd", "Failed setting up ad loader", e)
        }
    }

    // 2. 광고가 로드되었을 때만 표시
    if (nativeAd != null) {
        // [FIX] 카드 스타일 제거 -> 피드형(Flat) 스타일로 변경 (2025-12-22)
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                val adView = com.google.android.gms.ads.nativead.NativeAdView(ctx)

                // 내부 컨테이너: 흰색 배경, 테두리 없음, 평면 디자인
                val container = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    // [중요] 게시글 텍스트 여백과 비슷하게 맞춤 (44px ≈ 16dp)
                    setPadding(44, 32, 44, 32)
                }

                // 1) 상단: 아이콘 + 광고 배지 + 헤드라인
                val headerRow = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val iconView = android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(110, 110)
                }
                headerRow.addView(iconView)

                // [NEW] 텍스트 컨테이너 (배지 + 제목을 세로로 배치) (2025-12-23)
                val textContainer = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = 24 // 아이콘과의 간격
                    }
                }

                // [NEW] ★ 광고 배지 (Ad Badge) 추가 ★ (2025-12-23)
                val badgeView = android.widget.TextView(ctx).apply {
                    text = "광고"
                    textSize = 10f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#FBC02D")) // 노란색 배경
                    setPadding(8, 2, 8, 2)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 4 // 제목과의 간격
                    }
                }
                textContainer.addView(badgeView)

                val headlineView = android.widget.TextView(ctx).apply {
                    textSize = 15f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#111827"))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                textContainer.addView(headlineView)

                headerRow.addView(textContainer)
                container.addView(headerRow)

                // 2) 중간: Body
                val bodyView = android.widget.TextView(ctx).apply {
                    textSize = 13f
                    setPadding(0, 24, 0, 32)
                    setTextColor(android.graphics.Color.parseColor("#6B7280"))
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                container.addView(bodyView)

                // 3) 하단: 버튼
                val callToActionView = android.widget.Button(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"))
                    setTextColor(android.graphics.Color.parseColor("#4B5563"))
                    textSize = 13f
                    stateListAnimator = null
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                container.addView(callToActionView)

                adView.addView(container)

                adView.iconView = iconView
                adView.headlineView = headlineView
                adView.bodyView = bodyView
                adView.callToActionView = callToActionView

                adView
            },
            update = { adView ->
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

                adView.setNativeAd(ad)
            },
            // [중요] Modifier 대폭 수정: 패딩/보더/클립 제거 -> 평면 스타일
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        )
    }
}

/**
 * 빈 상태 표시
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onWriteClick: () -> Unit // [변경] 모의 데이터 대신 글쓰기 클릭 콜백
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 아이콘
        Text(
            text = "📝",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 메인 텍스트
        Text(
            text = stringResource(R.string.community_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF111111)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 서브 텍스트
        Text(
            text = stringResource(R.string.community_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // [핵심] 글쓰기 유도 버튼
        Button(
            onClick = onWriteClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1), // 포인트 컬러 (탭2 분석과 동일)
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(50) // 둥근 버튼
        ) {
            Text(stringResource(R.string.community_empty_button), modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

/**
 * Full-screen modal that allows swipe-down to dismiss with animation.
 * Content should fill available space (e.g., PhotoScreen).
 */
@Composable
private fun FullScreenPhotoModal(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _change, dragAmount ->
                        // Update offset by drag amount (no explicit consumption needed here)
                        scope.launch {
                            val new = offsetY.value + dragAmount
                            offsetY.snapTo(new.coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value > screenHeightPx * 0.25f) {
                                // dismiss
                                offsetY.animateTo(screenHeightPx, tween(200))
                                onDismiss()
                            } else {
                                offsetY.animateTo(0f, spring(stiffness = 800f))
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .fillMaxSize()
                .background(Color.White)
        ) {
            content()
        }
    }
}
