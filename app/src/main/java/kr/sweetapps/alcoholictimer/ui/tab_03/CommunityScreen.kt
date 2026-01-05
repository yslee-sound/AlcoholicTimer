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
    onSettingsClick: () -> Unit = {} // ?ㅼ�� ?�硫�?쇰� ?대��
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState() // Pull-to-Refresh ?��� (2025-12-20)
    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState() // ?��� ?ъ��???�諛�?�
    val currentNickname by viewModel.currentNickname.collectAsState() // [NEW] ?��� ?ъ��???���??(2025-12-22)
    val context = LocalContext.current // Context 媛�?몄�ㅺ�?(2025-12-19)

    // [UI State] Snackbar瑜??��� ?��� 諛??ㅼ�??
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 湲�?곌린 ?�硫� ?��� ?���
    var isWritingScreenVisible by remember { mutableStateOf(false) }
    // [NEW] ?���??寃���湲� ?��� (2025-12-22)
    var postToEdit by remember { mutableStateOf<kr.sweetapps.alcoholictimer.data.model.Post?>(null) }
    // ?�泥� ?�硫� ?ъ� ?��� ?��� ?��� (CommunityScreen ?�踰⑤�??���?щ┝)
    var isPhotoSelectionVisible by remember { mutableStateOf(false) }
    var photoIsClosing by remember { mutableStateOf(false) }

    // Phase 3: 寃���湲� ?듭�� 諛��? ?���
    var selectedPost by remember { mutableStateOf<kr.sweetapps.alcoholictimer.data.model.Post?>(null) }
    // [NEW] ??�� ?��� ?ㅼ��?쇰�洹??��� (2025-12-25)
    var showDeleteDialog by remember { mutableStateOf(false) }

    // [以���] 湲�?곌린 ?�硫�???대��?��� ???ㅻ�媛�湲?踰��� ?�瑜대�???醫�猷� ?�??湲�?곌린 李??リ린
    BackHandler(enabled = isWritingScreenVisible) {
        isWritingScreenVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // === 1. 硫��� 由ъ��???�硫� (?ㅼ�� 源�由�???�硫�) ===

        // ?몄�� ?��� 愿�??蹂�???��� (Scaffold 諛��쇰�??대��)
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
            containerColor = Color.White, // [FIX] 諛곌꼍?��� ?곗��?쇰� 蹂�寃?
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
                    // 寃���湲�???��� ?��� ?ㅻ�� + 鍮??��� ?���
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshPosts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // ?ㅻ�� (?�紐� + ??
                            item {
                                Column(modifier = Modifier.background(Color.White)) {
                                    TopAppBar(
                                        title = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.community_title_support),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp, // [FIX] ???� ?���???ш린
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF111111)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.community_title_challenge),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp, // [FIX] ???� ?���???ш린
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF6366F1) // [FIX] ?? '遺���'怨??���???���
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = onSettingsClick) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.gearsix),
                                                    contentDescription = "?ㅼ��",
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

                            // 湲�?곌린 踰���
                            item {
                                WritePostTrigger(
                                    onClick = { isWritingScreenVisible = true },
                                    currentAvatarIndex = currentUserAvatarIndex
                                )
                            }

                            // 鍮??��� ?���
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
                    // NEW Pull-to-Refresh ?��� (2025-12-20)
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshPosts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // === [1] ?ㅻ�� ?��� (?�紐� + ?? - ?ㅽ�щ·�� ===
                            item {
                                Column(modifier = Modifier.background(Color.White)) {
                                    // 1-1. ?�紐⑹�?(TopAppBar)
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
                                                Spacer(modifier = Modifier.width(4.dp)) // [NEW] ??移??��곌�?
                                                Text(
                                                    text = stringResource(R.string.community_title_challenge),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF6366F1) // 蹂대��??(遺���怨??���)
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = onSettingsClick) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.gearsix),
                                                    contentDescription = "?ㅼ��",
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

                                    // 1-2. ??諛?(TabRow)
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

                            // === [2] 湲�?곌린 ?몃━嫄?===
                            item {
                                WritePostTrigger(
                                    onClick = { isWritingScreenVisible = true },
                                    currentAvatarIndex = currentUserAvatarIndex
                                )
                            }

                            // === [3] 愿�怨� 諛?寃���湲� 由ъ��??===

                            // 愿�怨� 諛?寃���湲� 由ъ��??濡�吏� (湲곗〈 ?���)
                            val itemsWithAds = posts.flatMapIndexed { index, post ->
                                if ((index + 1) % 6 == 0 && index > 0) listOf(post, null) else listOf(post)
                            }

                            items(itemsWithAds.size, key = { index ->
                                val item = itemsWithAds[index]
                                item?.id ?: "ad_$index"
                            }) { index ->
                                val item = itemsWithAds[index]
                                if (item == null) {
                                    // [Standard] ?ㅼ��?곕� 愿�怨� ?��� ?щ갚 ?��???(2025-12-23)
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        NativeAdItem(screenKey = "community_screen")
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
                                         remainingTime = calculateRemainingTime(item.deleteAt, context), // [MODIFIED] context ?��� (2025-12-24)
                                         currentDays = item.currentDays,
                                         userLevel = item.userLevel,
                                         authorAvatarIndex = item.authorAvatarIndex, // ?�諛�?� ?몃��???���
                                         thirstLevel = item.thirstLevel,
                                          isMine = viewModel.isMyPost(item), // Phase 3: ??湲� ?щ?
                                          tagType = item.tagType, // [NEW] ?�洹� ?�???��� (2025-12-23)
                                          onLikeClick = { viewModel.toggleLike(item) },
                                          onCommentClick = { },
                                          onMoreClick = { selectedPost = item }, // Phase 3: 諛��? ?��� ?닿린
                                          onHideClick = {
                                             // 1) 利��� ?④? 泥�由�
                                             viewModel.hidePost(item.id)

                                             // 2) ?ㅻ�듬�濡� Undo ?�怨�
                                             coroutineScope.launch {
                                                 val result = snackbarHostState.showSnackbar(
                                                     message = "寃���湲�???④꺼議���?���.",
                                                     actionLabel = "?���由ш린",
                                                     duration = SnackbarDuration.Short
                                                 )

                                                 if (result == SnackbarResult.ActionPerformed) {
                                                     viewModel.undoHidePost(item.id)
                                                 }
                                             }
                                          } // Phase 3: 鍮�瑜� ?④린湲?+ Undo
                                    )
                                }
                                // MODIFIED ?�諛�?대�� 吏���寃?(?���?ㅻ� ?ㅽ??? (2025-12-20)
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFBDBDBD))
                            }
                        }
                    }
                } // else ?リ린
            } // Box ?リ린 (innerPadding)
        } // Scaffold ?リ린

        // === 2. 湲�?곌린 ?�泥� ?�硫� (理���???���?? ===
        // MODIFIED Dialog + ?щ��?대�� ?���硫���??(?���?��� ?�濡�) (2025-12-19)
        if (isWritingScreenVisible) {
            Dialog(
                onDismissRequest = { /* ?���?⑥�� 諛깅�?쇱? ?대? AnimatedVisibility?��� 泥�由� */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false, // 媛�濡?苑?李④�
                    decorFitsSystemWindows = false   // ?���??諛??���源��? ?��� (Edge-to-Edge)
                )
            ) {
                // NEW ?대? ?���硫���???��� (2025-12-19)
                var animateVisible by remember { mutableStateOf(false) }

                // NEW ?ㅼ��?쇰�洹멸? ?⑤㈃ 利��� ?���硫���???���
                LaunchedEffect(Unit) { animateVisible = true }

                // NEW ?リ린 ?몃━嫄??⑥�� (?���硫���????醫�猷�)
                val triggerClose = {
                    animateVisible = false
                }

                // NEW ?���硫���?��� ?���硫??ㅼ�� ?ㅼ��?쇰�洹??リ린
                LaunchedEffect(animateVisible) {
                    if (!animateVisible) {
                        kotlinx.coroutines.delay(300) // ?���硫���???�媛� ?�湲?
                        isWritingScreenVisible = false // 吏�吏� 醫�猷�
                    }
                }

                AnimatedVisibility(
                    visible = animateVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it }, // ?�硫� ?���?��� ?�濡�
                        animationSpec = tween(300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it }, // ?�硫� ?���???���濡?
                        animationSpec = tween(300)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    WritePostScreenContent(
                        viewModel = viewModel,
                        currentNickname = currentNickname, // [NEW] ViewModel?��� 諛��? ?���???��� (2025-12-22)
                        postToEdit = postToEdit, // [NEW] ?���??寃���湲� ?��� (2025-12-22)
                        onPost = {
                            postToEdit = null // [NEW] ?�猷� ??珥�湲�??(2025-12-22)
                            triggerClose()
                        },
                        onDismiss = {
                            postToEdit = null // [NEW] 痍⑥�� ??珥�湲�??(2025-12-22)
                            triggerClose()
                        },
                        onOpenPhoto = {
                            // [NEW] 愿�怨� ?듭�� ?���??- 移대�??媛ㅻ�щ�?蹂듦? ??愿�怨� 李⑤�� (2025-12-22)
                            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isAdSuppressed = true

                            // [?듭��] ?�媛� 湲곕� ?듭�� ?ㅼ�� - ?��� ?�媛�遺�??10珥�媛� 愿�怨� ?몄� 湲��? (2025-12-22)
                            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.lastAdSuppressedTime = System.currentTimeMillis()
                            android.util.Log.d("CommunityScreen", "愿�怨� ?듭�� ?ㅼ��: 10珥�媛� 愿�怨� 李⑤�� ?���")

                            // 湲�?곌린 ?ㅼ��?쇰�洹몃? ?レ? ?�怨�, 洹??��� ?ъ� ?��� Dialog瑜??���?���. (?ㅽ�� 諛⑹��)
                            isPhotoSelectionVisible = true
                        }
                    )
                 }
             }
         }

        // === 3. 寃���湲� ?듭�� 諛��? ?��� (Phase 3) ===
        selectedPost?.let { post ->
             ModalBottomSheet(
                 onDismissRequest = { selectedPost = null },
                 containerColor = Color.White
             ) {
                 PostOptionsBottomSheet(
                     post,
                     isMyPost = viewModel.isMyPost(post),
                     onEdit = { // [NEW] ?��� 踰��� 肄�諛� (2025-12-22)
                         postToEdit = post
                         selectedPost = null
                         isWritingScreenVisible = true
                     },
                     onDelete = {
                         // [NEW] ??�� ?��� ?ㅼ��?쇰�洹??��� (2025-12-25)
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

        // === 4. ??�� ?��� ?ㅼ��?쇰�洹?(2025-12-25) ===
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
                            color = Color(0xFFEF4444) // 鍮④�??媛�議�
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

        // === ?�泥� ?�硫� ?ъ� ?���: Dialog濡?蹂�寃쏀��???��� ?ㅻ�寃���?��� ??��濡???===
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
                // ?대? ?���硫���???���: Dialog媛� 蹂댁�ъ�???��� animateVisible??耳�怨�
                // ?レ�� ?��� animateVisible???�怨� ?���硫���?��� ?��� ??isPhotoSelectionVisible=false濡??ㅼ��
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
 * Phase 3: 寃���湲� ?듭�� 諛��? ?���
 * ??湲�: ??��留?
 * ?⑥�� 湲�: ?④린湲? ?�怨�?�湲�
 */
@Composable
private fun PostOptionsBottomSheet(
    post: kr.sweetapps.alcoholictimer.data.model.Post,
    isMyPost: Boolean,
    onEdit: () -> Unit, // [NEW] ?��� 肄�諛� (2025-12-22)
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // ?�?댄?
        Text(
            text = if (isMyPost) stringResource(R.string.community_post_manage) else stringResource(R.string.community_post_manage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
        )

        if (isMyPost) {
            // [NEW] ?��� 踰��� 異��? (2025-12-22)
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

            // ??湲�: ??�� 硫���
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
            // ?⑥�� 湲�: ?④린湲? ?�怨�?�湲�
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
