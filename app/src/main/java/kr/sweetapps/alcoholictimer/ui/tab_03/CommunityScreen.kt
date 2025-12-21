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
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current // Context 가져오기 (2025-12-19)

    // [UI State] Snackbar를 위한 상태 및 스코프
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 글쓰기 화면 표시 상태
    var isWritingScreenVisible by remember { mutableStateOf(false) }
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
            // LANGUAGE FILTER UI: TopBar 바로 아래에 배치됩니다.
            val deviceLangRaw = Locale.getDefault().language
            val deviceLang = if (deviceLangRaw.lowercase() == "in") "id" else deviceLangRaw.lowercase()
            var showAllLanguages by remember { mutableStateOf(false) }

            // Apply initial filter (ensure ViewModel matches UI) - sync when Composable first runs
            LaunchedEffect(Unit) {
                viewModel.setLanguageFilter(if (showAllLanguages) null else deviceLang)
            }

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
                            item {
                                WritePostTrigger(
                                    onClick = { isWritingScreenVisible = true },
                                    currentAvatarIndex = currentUserAvatarIndex // 현재 사용자 아바타 전달
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
                                         authorAvatarIndex = item.authorAvatarIndex, // 아바타 인덱스 전달
                                         thirstLevel = item.thirstLevel,
                                          isMine = viewModel.isMyPost(item), // Phase 3: 내 글 여부
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
                }
            }
        }

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
                        onPost = { triggerClose() }, // [MODIFIED] 실제 게시처리는 내부에서 실행, 부모에는 닫기만 위임
                        onDismiss = { triggerClose() }, // [FIX] 뒤로가기 시 애니메이션 종료
                        onOpenPhoto = {
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
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // [NEW] ExperimentalLayoutApi 추가 (isImeVisible 사용)
@Composable
private fun WritePostScreenContent(
    viewModel: CommunityViewModel, // [NEW] ViewModel 주입
    onPost: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenPhoto: () -> Unit // [NEW] 사진 선택 화면 열기 콜백 (네비게이션 호출)
) {
    // Use TextFieldValue to track cursor position and selection
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    // initialize empty on entry
    LaunchedEffect(Unit) { textFieldValue = TextFieldValue("") }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current // [NEW] FocusManager (2025-12-19)
    var showWarningSheet by remember { mutableStateOf(false) } // [NEW] 경고 바텀 시트 표시 상태 (2025-12-19)
    // 하단 패널 상태: 갈증 수치 패널을 토글하기 위한 상태
    var showThirstSlider by remember { mutableStateOf(false) }
    // Note: showPhotoScreen handled via external navigation callback (onOpenPhoto)

    // [FIX] 갈증 수치 상태를 nullable로 변경하여 초기에는 선택이 없음
    // 초기값: null (아무 숫자도 선택되지 않은 상태)
    var selectedLevel by remember { mutableStateOf<Int?>(null) }

    // [NEW] 갈증 레벨에 따른 색상 계산 함수(Reused by top badge and bottom selector)
    fun thirstColor(level: Int): Color = when (level) {
        in 1..3 -> Color(0xFF4CAF50)
        in 4..7 -> Color(0xFFFFA726)
        else -> Color(0xFFE53935)
    }

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

    // [FIX] 갈증 수치 상태를 nullable로 변경하여 초기에는 선택이 없음
    // 초기값: null (아무 숫자도 선택되지 않은 상태)
    var selectedTag by remember { mutableStateOf("diary") } // diary, thanks, reflect

    val placeholderText = when (selectedTag) {
        "diary" -> "오늘 하루는 어땠나요? 솔직한 이야기를 들려주세요."
        "thanks" -> "오늘 웃게 된 일이나 고마운 순간이 있었나요? 사소한 것도 좋아요. ✨"
        "reflect" -> "아쉬웠던 점이나 내일을 위한 다짐을 적어보세요. 🌙"
        else -> "오늘 하루는 어땠나요? 솔직한 이야기를 들려주세요."
    }

    // [NEW] 수정 상태 감지
    val isModified = textFieldValue.text.isNotBlank() || selectedImageUri != null

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
                            // Allow posting when either text exists or an image is selected
                            if ((textFieldValue.text.isNotBlank() || selectedImageUri != null) && !isLoading) {
                                val payload = textFieldValue.text.trim()
                                try {
                                    // Do NOT clear local UI state here. ViewModel starts loading immediately and
                                    // will call onSuccess when upload completes. Then we close the dialog.
                                    viewModel.addPost(
                                        content = payload,
                                        context = context,
                                        tagType = selectedTag,
                                        thirstLevel = selectedLevel,
                                        onSuccess = {
                                            // Called from ViewModel after upload & DB save succeed
                                            onPost(payload)
                                        }
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("CommunityScreen", "addPost call failed", e)
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
                                text = "게시하기",
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
                                      .background(if (selected) thirstColor(value) else Color(0xFFF0F0F0))
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
                            Toast.makeText(context, "사진 추가 버튼 눌림", Toast.LENGTH_SHORT).show()
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
             val localScope = rememberCoroutineScope()

             // 자동 스크롤: 이미지가 추가되면 맨 아래로 스크롤하여 사용자가 바로 이미지를 보도록 함
             LaunchedEffect(selectedImageUri) {
                 if (selectedImageUri != null) {
                     // animateScrollTo에 큰 값을 줘도 안전: ScrollState는 콘텐츠 크기에 맞게 clamp됨
                     localScope.launch { scrollState.animateScrollTo(Int.MAX_VALUE) }
                 }
             }

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
                var currentNickname by remember { mutableStateOf("") } // [NEW]

                // 화면이 생성될 때(진입 시) 무조건 최신 닉네임을 불러옵니다. (하드코딩 금지)
                LaunchedEffect(Unit) {
                     try {
                         val repo = kr.sweetapps.alcoholictimer.data.repository.UserRepository(context)
                         // 존재하면 값 사용, 없으면 빈 문자열 유지(화면에 아무것도 표시하지 않음)
                         currentNickname = repo.getNickname() ?: ""
                     } catch (_: Throwable) {
                         // 실패 시 빈 문자열 유지(섣불리 '익명' 등 하드코딩 금지)
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
                        // 닉네임과 뱃지의 배치를 Row로 변경: 닉네임, 구분자(" - "), 숫자 뱃지, 후행 텍스트(" 갈증") 순
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 1) 닉네임: 로드되기 전까지는 비워두어 깜빡임을 방지합니다.
                            // [NEW] 작성자 닉네임 표시 보장: 닉네임이 비어있으면 '익명'으로 대체하여 항상 텍스트가 노출되게 함
                            val displayNickname = if (currentNickname.isNotBlank()) currentNickname else "익명"
                            Text(
                                // [NEW] 상단에 항상 내 별명이 보이도록 기본값 처리
                                text = displayNickname,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111827) // 색상 변경 금지
                            )

                        // 2~4) selectedLevel이 있을 때만 구분자, 뱃지, 후행 텍스트 노출
                        if (selectedLevel != null) {
                            // 요소 A: 구분자
                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111827)
                            )

                            // 요소 B: 숫자 뱃지 (숫자만, 배경색은 thirstColor 사용)
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .wrapContentWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(thirstColor(selectedLevel!!))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedLevel.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // 요소 C: 후행 텍스트
                            Text(
                                text = " 갈증",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111827)
                            )
                        }
                        }

                        // [FIX] 하드코딩된 "내 프로필" 대신, 게시글 리스트에서 사용하는 포맷과 동일하게
                        // LV.{레벨} · {일수}일차 를 표시합니다. SharedPreferences의 timer_prefs에서 시작시간을 읽어 계산합니다.
                        val tab03Vm: kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab03ViewModel = viewModel()
                        val levelDays by tab03Vm.levelDays.collectAsState()
                        // 요구사항: 만약 levelDays == 0 이면 LV.0 으로 그대로 표시해야 함
                        val levelNumber = if (levelDays == 0) 0 else kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelNumber(levelDays) + 1

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LV.$levelNumber",
                                style = MaterialTheme.typography.labelSmall,
                                color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.alignByBaseline() // [FIX] PostItem과 동일한 baseline 정렬 사용
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.alignByBaseline() // [FIX] baseline 정렬
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "${levelDays}일차",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.alignByBaseline() // [FIX] baseline 정렬
                            )
                        }
                    }
                }

                // [NEW] 주제 선택 칩 (작성자 정보 바로 아래, 입력창 위)
                // 선택된 태그에 따라 placeholder가 바뀝니다.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()) // [NEW] 가로 스크롤 허용
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTag == "diary",
                        onClick = { if (!isLoading) selectedTag = "diary" },
                        label = { Text("오늘의 일기") },
                        colors = FilterChipDefaults.filterChipColors(
                            // 비선택(기본) 상태 색상
                            containerColor = Color(0xFFF0F0F0),
                            labelColor = Color(0xFF374151),
                            // 선택 상태 색상
                            selectedContainerColor = Color(0xFF7C3AED), // 보라
                            selectedLabelColor = Color.White
                         ),
                         modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                     )

                    FilterChip(
                        selected = selectedTag == "thanks",
                        onClick = { if (!isLoading) selectedTag = "thanks" },
                        label = { Text("오늘 감사할 일") },
                        colors = FilterChipDefaults.filterChipColors(
                            // 비선택(기본) 상태 색상
                            containerColor = Color(0xFFF0F0F0),
                            labelColor = Color(0xFF374151),
                            // 선택 상태 색상
                            selectedContainerColor = Color(0xFFFFD54F), // 노랑
                            selectedLabelColor = Color.Black
                         ),
                         modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                     )

                    FilterChip(
                        selected = selectedTag == "reflect",
                        onClick = { if (!isLoading) selectedTag = "reflect" },
                        label = { Text("오늘 반성할 일") },
                        colors = FilterChipDefaults.filterChipColors(
                            // 비선택(기본) 상태 색상
                            containerColor = Color(0xFFF0F0F0),
                            labelColor = Color(0xFF374151),
                            // 선택 상태 색상
                            selectedContainerColor = Color(0xFF6B7280), // 회색
                            selectedLabelColor = Color.White
                         ),
                         modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                     )
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
                 colors = TextFieldDefaults.colors(
                     focusedContainerColor = Color.Transparent,
                     unfocusedContainerColor = Color.Transparent,
                     focusedIndicatorColor = Color.Transparent, // 밑줄 제거
                     unfocusedIndicatorColor = Color.Transparent
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
                        // 타이틀 (왼쪽 정렬, 한 줄 제한)
                        Text(
                            text = "작성 중인 글을 삭제하시겠습니까?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
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
                                imageVector = Icons.Filled.Delete,
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
                                imageVector = Icons.Filled.Edit,
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
 * REAL 구글 애드몹 네이티브 광고
 * 기존의 노란색 Placeholder를 대체합니다.
 */
@Composable
private fun NativeAdItem() {
    val context = LocalContext.current // NEW Context 사용

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
        try {
            // Ensure Mobile Ads SDK initialized; guard against exceptions on some devices / setups
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

            // Guard against SecurityException coming from Play Services broker
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
