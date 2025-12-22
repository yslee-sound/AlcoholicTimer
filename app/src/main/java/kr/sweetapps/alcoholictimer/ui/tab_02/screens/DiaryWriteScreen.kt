// [MODIFIED] 일기 작성 화면 - WritePostScreenContent 재사용 (2025-12-22)
package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import kr.sweetapps.alcoholictimer.ui.common.CustomGalleryScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.WritePostScreenContent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * [MODIFIED] 일기 작성/수정 화면
 * - WritePostScreenContent를 재사용하여 커뮤니티 글쓰기와 동일한 UI 제공
 * - isDiaryMode = true로 설정하여 "챌린지 공유" 기능 활성화
 * - 새 일기 작성: diaryId = null, selectedDate = 선택된 날짜 타임스탬프
 * - 기존 일기 수정: diaryId != null (날짜 유지)
 */
@Composable
fun DiaryWriteScreen(
    diaryId: Long? = null,
    selectedDate: Long? = null, // [NEW] 선택된 날짜 받기 (2025-12-22)
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. CommunityViewModel 인스턴스 획득 (이미지, 갈증 수치 등 로직 재사용)
    val communityViewModel: kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.CommunityViewModel = viewModel()
    val currentNickname by communityViewModel.currentNickname.collectAsState()

    // 2. DiaryViewModel 인스턴스 획득 (로컬 DB 저장용)
    val diaryViewModel: DiaryViewModel = viewModel()

    // [NEW] 사진 선택 화면 표시 상태 (2025-12-22)
    var isPhotoSelectionVisible by remember { mutableStateOf(false) }

    // 3. 기존 일기 데이터 로드 (수정 모드)
    var existingDiary by remember { mutableStateOf<DiaryEntity?>(null) }
    var postToEdit by remember(diaryId) { mutableStateOf<Post?>(null) } // [FIX] diaryId를 key로 추가 (2025-12-23)

    // [NEW] 데이터 로딩 상태 추가 (2025-12-23)
    var isDataLoaded by remember(diaryId) { mutableStateOf(diaryId == null) } // ID가 없으면 즉시 표시

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            scope.launch {
                // Room DB에서 기존 일기 불러오기
                val diary = diaryViewModel.getDiaryById(diaryId)
                existingDiary = diary

                // Post 객체로 변환하여 WritePostScreenContent에 전달
                if (diary != null) {
                    // [FIX] 타이머 시작 시간 가져오기 (2025-12-23)
                    val prefs = context.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE)
                    val startTime = prefs.getLong("start_time", 0L)

                    // [FIX] 일기 작성 당시의 경과 일수 및 레벨 계산 (2025-12-23)
                    val elapsedDays = if (startTime > 0) {
                        kotlin.math.max(1, ((diary.timestamp - startTime) / (1000 * 60 * 60 * 24)).toInt() + 1)
                    } else {
                        1 // 타이머 없으면 기본값
                    }
                    val levelNumber = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelNumber(elapsedDays)

                    postToEdit = Post(
                        id = diary.id.toString(),
                        content = diary.content,
                        tagType = diary.tagType, // [FIX] DB에 저장된 실제 태그 값 사용 (2025-12-23)
                        thirstLevel = diary.cravingLevel, // [FIX] cravingLevel -> thirstLevel 매핑
                        imageUrl = diary.imageUrl, // [FIX] 기존 이미지 URL 매핑 (2025-12-23)
                        nickname = "",
                        timerDuration = "", // 사용하지 않음
                        likeCount = 0,
                        likedBy = emptyList(),
                        currentDays = elapsedDays, // [FIX] 일기 작성 당시 경과 일수 (2025-12-23)
                        userLevel = levelNumber + 1, // [FIX] 레벨 번호 (1-indexed) (2025-12-23)
                        createdAt = com.google.firebase.Timestamp(diary.timestamp / 1000, 0), // [FIX] 일기 작성 시간 (2025-12-23)
                        deleteAt = com.google.firebase.Timestamp.now(),
                        authorAvatarIndex = 0,
                        authorId = "",
                        languageCode = ""
                    )

                    // [DEBUG] postToEdit 설정 확인 (2025-12-23)
                    android.util.Log.d("DiaryWriteScreen", "postToEdit 설정됨: id=${diary.id}, content=${diary.content.take(20)}, days=$elapsedDays, level=${levelNumber + 1}")
                }

                // [NEW] 데이터 로드 완료 (2025-12-23)
                isDataLoaded = true
            }
        }
    }

    // 4. [FIX] 데이터가 로드된 후에만 화면을 그림 (2025-12-23)
    if (isDataLoaded) {
        WritePostScreenContent(
            viewModel = communityViewModel,
            currentNickname = currentNickname,
            isDiaryMode = true, // [중요] 일기 모드 활성화
            postToEdit = postToEdit, // 수정 모드일 경우 기존 데이터 전달
            onPost = {
                // 저장/게시 완료 후 화면 닫기
                onDismiss()
            },
        onSaveDiary = { postData ->
            // [핵심] 로컬 일기장(Room DB) 저장 로직
            scope.launch {
                try {
                    if (diaryId != null) {
                        // [수정 모드] 날짜 변경 금지 (기존 타임스탬프 유지)
                        val originalTimestamp = existingDiary?.timestamp ?: System.currentTimeMillis()
                        val updatedDiary = existingDiary?.copy(
                            content = postData.content,
                            cravingLevel = postData.thirstLevel ?: 0,
                            imageUrl = postData.imageUrl ?: "", // [FIX] 이미지 URL 업데이트 (2025-12-23)
                            tagType = postData.tagType, // [NEW] 태그 타입 저장 (2025-12-23)
                            timestamp = originalTimestamp, // [FIX] 기존 시간 유지 (2025-12-22)
                            date = formatDate(originalTimestamp)
                        )
                        if (updatedDiary != null) {
                            diaryViewModel.updateDiary(updatedDiary)
                            android.util.Log.d("DiaryWriteScreen", "일기 수정 성공: 태그=${postData.tagType}")
                        }
                    } else {
                        // [신규 모드] 선택된 날짜 사용
                        val targetTimestamp = selectedDate ?: System.currentTimeMillis() // [FIX] 선택된 날짜 우선 사용 (2025-12-22)
                        val newDiary = DiaryEntity(
                            emoji = "📝", // 기본 이모지 (추후 선택 기능 추가 가능)
                            content = postData.content,
                            cravingLevel = postData.thirstLevel ?: 0,
                            timestamp = targetTimestamp, // [FIX] 선택된 날짜로 저장 (2025-12-22)
                            date = formatDate(targetTimestamp),
                            imageUrl = postData.imageUrl ?: "", // [NEW] 이미지 URL 저장 (2025-12-23)
                            tagType = postData.tagType // [NEW] 태그 타입 저장 (2025-12-23)
                        )
                        diaryViewModel.insertDiary(newDiary)
                        android.util.Log.d("DiaryWriteScreen", "일기 생성 성공: 태그=${postData.tagType}, 날짜=${formatDate(targetTimestamp)}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DiaryWriteScreen", "일기 저장 실패", e)
                }
            }
        },
        onDismiss = {
            // 취소 시 화면 닫기
            onDismiss()
        },
        onOpenPhoto = {
            // [NEW] 광고 억제 활성화 - 카메라/갤러리 복귀 시 광고 차단 (2025-12-22)
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isAdSuppressed = true

            // [핵심] 시간 기반 억제 설정 - 현재 시간부터 10초간 광고 노출 금지 (2025-12-22)
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.lastAdSuppressedTime = System.currentTimeMillis()
            android.util.Log.d("DiaryWriteScreen", "광고 억제 설정: 10초간 광고 차단 시작")

            // 사진 선택 화면 열기
            isPhotoSelectionVisible = true
        }
    )
    } else {
        // [NEW] 로딩 중일 때 표시할 화면 (2025-12-23)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
            )
        }
    }

    // [NEW] 5. 전체 화면 사진 선택 Dialog (CommunityScreen과 동일 로직) (2025-12-22)
    if (isPhotoSelectionVisible) {
        Dialog(
            onDismissRequest = { /* 내부 애니메이션으로 처리 */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            var animateVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) { animateVisible = true }

            val triggerClosePhoto = { animateVisible = false }

            LaunchedEffect(animateVisible) {
                if (!animateVisible) {
                    kotlinx.coroutines.delay(300)
                    isPhotoSelectionVisible = false
                }
            }

            AnimatedVisibility(
                visible = animateVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                FullScreenPhotoModal(onDismiss = { triggerClosePhoto() }) {
                    CustomGalleryScreen(
                        onImageSelected = { uri ->
                            try {
                                communityViewModel.onImageSelected(uri)
                            } catch (e: Exception) {
                                android.util.Log.e("DiaryWriteScreen", "Photo select failed", e)
                            }
                            triggerClosePhoto()
                        },
                        onClose = { triggerClosePhoto() }
                    )
                }
            }
        }
    }
}

/**
 * [HELPER] timestamp를 날짜 문자열로 변환
 */
private fun formatDate(timestamp: Long): String {
    val locale = Locale.getDefault()
    val sdf = when (locale.language) {
        "ko" -> SimpleDateFormat("yyyy년 M월 d일", locale)
        "ja" -> SimpleDateFormat("yyyy年M月d日", locale)
        "zh" -> SimpleDateFormat("yyyy年M月d日", locale)
        "es" -> SimpleDateFormat("d 'de' MMMM 'de' yyyy", locale)
        else -> SimpleDateFormat("MMM d, yyyy", locale)
    }
    return sdf.format(Date(timestamp))
}

/**
 * [NEW] Full-screen photo modal with swipe-down to dismiss animation (2025-12-22)
 * CommunityScreen과 동일한 로직
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
                    onVerticalDrag = { _, dragAmount ->
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
