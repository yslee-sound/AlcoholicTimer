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
import kr.sweetapps.alcoholictimer.ui.components.ads.NativeAdItem
import kr.sweetapps.alcoholictimer.ui.tab_03.components.WritePostScreenContent
import kr.sweetapps.alcoholictimer.ui.tab_03.components.PostOptionsBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_03.components.WritePostTrigger
import kr.sweetapps.alcoholictimer.ui.tab_03.components.EmptyState
import kr.sweetapps.alcoholictimer.ui.tab_03.components.FullScreenPhotoModal
import kr.sweetapps.alcoholictimer.ui.tab_03.components.calculateRemainingTime
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
    // [NEW] 삭제 확인 다이얼로그 상태 (2025-12-25)
    var showDeleteDialog by remember { mutableStateOf(false) }

    // [중요] 글쓰기 화면이 열려있을 때 뒤로가기 버튼 누르면 앱 종료 대신 글쓰기 창 닫기
    BackHandler(enabled = isWritingScreenVisible) {
        isWritingScreenVisible = false
    }

    // [STATE HOISTING] 네이티브 광고 상태 관리 (2026-01-05)
    var communityScreenAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }

    // [STATE HOISTING] 네이티브 광고 로드 - 화면 진입 시 1회만 실행 (2026-01-05)
    // [FIXED] 이미 로드된 광고가 있으면 재로드하지 않음 (2026-01-05)
    LaunchedEffect(Unit) {
        if (communityScreenAd != null) {
            android.util.Log.d("CommunityScreen", "광고 이미 로드됨, 재로드 스킵")
            return@LaunchedEffect
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context)
            } catch (e: Exception) {
                android.util.Log.w("CommunityScreen", "MobileAds init failed: ${e.message}")
            }
        }

        kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager.getOrLoadAd(
            context = context,
            screenKey = "community_screen",
            onAdReady = { ad ->
                android.util.Log.d("CommunityScreen", "Native ad ready")
                communityScreenAd = ad
            },
            onAdFailed = {
                android.util.Log.w("CommunityScreen", "Native ad failed")
            }
        )
    }

    // [REMOVED] DisposableEffect 제거 (2026-01-05)
    // 탭 전환 시 광고가 파괴되지 않도록 함

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
                                    // [STATE HOISTING] 광고 렌더링 (2026-01-05)
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        NativeAdItem(
                                            nativeAd = communityScreenAd,
                                            viewStyle = kr.sweetapps.alcoholictimer.ui.components.ads.NativeAdViewStyle.FLAT  // [UI 이원화] 피드는 Flat 스타일
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                } else {
                                    PostItem(
                                         nickname = item.nickname,
                                         timerDuration = item.timerDuration,
                                         content = item.content,
                                         imageUrl = item.imageUrl,
                                         likeCount = item.likeCount,
                                         isLiked = viewModel.isLikedByMe(item),
                                         remainingTime = calculateRemainingTime(item.deleteAt, context), // [MODIFIED] context 전달 (2025-12-24)
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
                                             // [DEBUG] 클릭 이벤트 확인
                                             android.util.Log.d("HideClickDebug", "=== X 버튼 클릭됨! ===")
                                             android.util.Log.d("HideClickDebug", "PostID: ${item.id}")
                                             android.util.Log.d("HideClickDebug", "Nickname: ${item.nickname}")

                                             // 1) 利��� ?④? 泥�由�
                                             viewModel.hidePost(item)

                                             // 2) ?ㅻ�듬�濡� Undo ?�怨�
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
                         // [NEW] 삭제 확인 다이얼로그 표시 (2025-12-25)
                         showDeleteDialog = true
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

        // === 4. 삭제 확인 다이얼로그 (2025-12-25) ===
        if (showDeleteDialog && selectedPost != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = {
                    Text(
                        text = stringResource(R.string.community_post_delete_dialog_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF111111)
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.community_post_delete_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedPost?.let { post ->
                                viewModel.deletePost(post.id)
                            }
                            showDeleteDialog = false
                            selectedPost = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.community_post_delete_confirm),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFEF4444) // 빨간색 강조
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(
                            text = stringResource(R.string.community_post_delete_cancel),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            )
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
            text = if (isMyPost) stringResource(R.string.community_post_manage) else stringResource(R.string.community_post_manage),
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
                    text = stringResource(R.string.community_post_edit),
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
                    text = stringResource(R.string.community_post_delete),
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
                    text = stringResource(R.string.community_post_hide),
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
                    text = stringResource(R.string.community_post_report),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}


/**
 * [REFACTORED] 컴포넌트 분리 완료 (2026-01-05)
 *
 * 분리된 파일:
 * 1. ui/tab_03/components/WritePostScreen.kt (~800 라인)
 *    - WritePostScreenContent: 글쓰기 화면 전체 로직
 *
 * 2. ui/tab_03/components/CommunityComponents.kt (~300 라인)
 *    - PostOptionsBottomSheet, WritePostTrigger, EmptyState
 *    - FullScreenPhotoModal, calculateRemainingTime
 *
 * 3. ui/components/ads/NativeAdItem.kt (공통 컴포넌트)
 *
 * Before: 1,800+ 라인
 * After: ~810 라인
 * 감소: 약 1,000 라인 (55% 감소)
 */
