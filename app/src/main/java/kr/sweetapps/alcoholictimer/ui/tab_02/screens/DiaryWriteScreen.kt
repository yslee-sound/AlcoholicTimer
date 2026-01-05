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
import kotlinx.coroutines.tasks.await
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import kr.sweetapps.alcoholictimer.ui.common.CustomGalleryScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.components.WritePostScreenContent
import kr.sweetapps.alcoholictimer.util.manager.UserStatusManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * [MODIFIED] 일기 작성/수정 화면
 * - WritePostScreenContent를 재사용하여 커뮤니티 글쓰기와 동일한 UI 제공
 * - isDiaryMode = true로 설정하여 "챌린지 공유" 기능 활성화
 * - 새 일기 작성: diaryId = null, selectedDate = 선택된 날짜 타임스탬프
 * - 기존 일기 수정: diaryId != null (날짜 유지)
 * [FIX] onDismiss(취소)와 onSaved(저장 완료) 콜백 분리 (2026-01-03)
 */
@Composable
fun DiaryWriteScreen(
    diaryId: Long? = null,
    selectedDate: Long? = null, // [NEW] 선택된 날짜 받기 (2025-12-22)
    onDismiss: () -> Unit = {},
    onSaved: () -> Unit = {} // [FIX] 저장 완료 전용 콜백 추가 (2026-01-03)
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
    // [NEW] 이미 공유된 일기인지 여부 (2025-12-25)
    var isAlreadyShared by remember(diaryId) { mutableStateOf(false) }

    // [NEW] 데이터 로딩 상태 추가 (2025-12-23)
    var isDataLoaded by remember(diaryId) { mutableStateOf(diaryId == null) } // ID가 없으면 즉시 표시

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            scope.launch {
                // Room DB에서 기존 일기 불러오기
                val diary = diaryViewModel.getDiaryById(diaryId)
                existingDiary = diary

                // [NEW] 공유 상태 확인 (2025-12-25)
                isAlreadyShared = !diary?.sharedPostId.isNullOrBlank()

                // Post 객체로 변환하여 WritePostScreenContent에 전달
                if (diary != null) {
                    // [CRITICAL] 수정 모드: DB에 저장된 레벨/일수 우선 사용 (2025-12-27)
                    // 타이머 시작 시간 가져오기 (fallback용)
                    val prefs = context.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE)
                    val startTime = prefs.getLong("start_time", 0L)

                    // [CHANGED] DB에 저장된 userLevel/currentDays가 있으면 우선 사용, 없으면 계산 (2025-12-27)
                    val savedLevel = diary.userLevel
                    val savedDays = diary.currentDays

                    val displayLevel: Int
                    val displayDays: Int

                    if (savedLevel > 0 && savedDays > 0) {
                        // DB에 저장된 값이 유효하면 그대로 사용
                        displayLevel = savedLevel
                        displayDays = savedDays
                        android.util.Log.d("DiaryWriteScreen", "[수정 모드] DB 저장값 사용: Lv.$displayLevel, Day $displayDays")
                    } else {
                        // 저장값이 없으면 타임스탬프 기준으로 계산 (레거시 일기 호환)
                        val elapsedDays = if (startTime > 0) {
                            kotlin.math.max(1, ((diary.timestamp - startTime) / (1000 * 60 * 60 * 24)).toInt() + 1)
                        } else {
                            1
                        }
                        val levelNumber = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelNumber(elapsedDays)
                        displayLevel = levelNumber + 1
                        displayDays = elapsedDays
                        android.util.Log.d("DiaryWriteScreen", "[수정 모드] 계산값 사용: Lv.$displayLevel, Day $displayDays")
                    }

                    postToEdit = Post(
                        id = diary.id.toString(),
                        content = diary.content,
                        tagType = diary.tagType,
                        thirstLevel = diary.cravingLevel,
                        imageUrl = diary.imageUrl,
                        nickname = "",
                        timerDuration = "",
                        likeCount = 0,
                        likedBy = emptyList(),
                        currentDays = displayDays, // [CHANGED] DB 저장값 또는 계산값
                        userLevel = displayLevel,   // [CHANGED] DB 저장값 또는 계산값
                        createdAt = com.google.firebase.Timestamp(diary.timestamp / 1000, 0),
                        deleteAt = com.google.firebase.Timestamp.now(),
                        authorAvatarIndex = 0,
                        authorId = "",
                        languageCode = ""
                    )

                    android.util.Log.d("DiaryWriteScreen", "postToEdit 설정 완료: id=${diary.id}, Lv.$displayLevel, Day $displayDays")
                }

                // [NEW] 데이터 로드 완료 (2025-12-23)
                isDataLoaded = true
            }
        }
    }

    // 4. [FIX] 데이터가 로드된 후에만 화면을 그림 (2025-12-23)
    if (isDataLoaded) {
        // [NEW] 일기가 오늘 날짜인지 확인 (API 21 호환) (2025-12-24)
        val isTodayDiary = remember(selectedDate, existingDiary) {
            val targetTimestamp = if (diaryId != null) {
                existingDiary?.timestamp ?: System.currentTimeMillis()
            } else {
                selectedDate ?: System.currentTimeMillis()
            }

            // 오늘 날짜와 비교
            val today = Calendar.getInstance()
            val diaryDate = Calendar.getInstance().apply { timeInMillis = targetTimestamp }

            today.get(Calendar.YEAR) == diaryDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == diaryDate.get(Calendar.DAY_OF_YEAR)
        }

        WritePostScreenContent(
            viewModel = communityViewModel,
            currentNickname = currentNickname,
            isDiaryMode = true, // [중요] 일기 모드 활성화
            postToEdit = postToEdit, // 수정 모드일 경우 기존 데이터 전달
            isTodayDiary = isTodayDiary, // [NEW] 오늘 일기 여부 전달 (2025-12-24)
            isAlreadyShared = isAlreadyShared, // [NEW] 이미 공유된 일기인지 전달 (2025-12-25)
            onPost = {
                // [FIX] 저장/게시 완료 후 onSaved 콜백 호출 (2026-01-03)
                onSaved()
            },
        onSaveDiary = { postData, isSharing ->
            // [핵심] 로컬 일기장(Room DB) 저장 로직 + Firestore 연동 (2025-12-25)
            scope.launch {
                try {
                    // [CRITICAL] 상태값 우선 참조: onSaveDiary 시작 시 현재 레벨/일수 확정 (2025-12-26)
                    val currentStatus = UserStatusManager.userStatus.value
                    val currentDays = currentStatus.totalDaysPrecise.toInt()
                    val currentLevel = currentStatus.level
                    android.util.Log.d("DiaryWriteScreen", "[UserStatus] 현재 상태: Lv.$currentLevel, Day $currentDays (타임머신 보정 포함)")

                    val currentSharedPostId = existingDiary?.sharedPostId
                    var newSharedPostId: String? = currentSharedPostId

                    // === Firestore 처리 (3가지 케이스) ===
                    when {
                        // Case 1: 체크박스 ON & sharedPostId == null (새로 공유)
                        isSharing && currentSharedPostId.isNullOrBlank() -> {
                            android.util.Log.d("DiaryWriteScreen", "[Case 1] 새로 공유 - Firestore에 게시글 생성")

                            // Firestore에 새 글 생성 및 Document ID 받기
                            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val deviceLang = java.util.Locale.getDefault().language.let {
                                if (it.lowercase() == "in") "id" else it.lowercase()
                            }

                            // 닉네임 및 아바타 가져오기
                            val userRepo = kr.sweetapps.alcoholictimer.data.repository.UserRepository(context)
                            val nickname = userRepo.getNickname() ?: "익명"
                            val avatarIndex = try { userRepo.getAvatarIndex() } catch (_: Exception) { 0 }
                            val deviceUserId = android.provider.Settings.Secure.getString(
                                context.contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID
                            )

                            val now = System.currentTimeMillis()
                            val post = hashMapOf(
                                "nickname" to nickname,
                                "content" to postData.content,
                                "tagType" to postData.tagType,
                                "thirstLevel" to (postData.thirstLevel ?: 0),
                                "imageUrl" to (postData.imageUrl ?: ""),
                                "likeCount" to 0,
                                "likedBy" to emptyList<String>(),
                                "currentDays" to currentDays, // [FIXED] 상단에서 가져온 검증된 값 사용 (2025-12-26)
                                "userLevel" to currentLevel, // [FIXED] 상단에서 가져온 검증된 값 사용 (2025-12-26)
                                "createdAt" to com.google.firebase.Timestamp.now(),
                                "deleteAt" to com.google.firebase.Timestamp((now / 1000) + 86400, 0), // 24시간 후
                                "authorAvatarIndex" to avatarIndex,
                                "authorId" to deviceUserId,
                                "languageCode" to deviceLang,
                                "timerDuration" to "0"
                            )

                            val docRef = firestore.collection("posts").add(post).await()
                            newSharedPostId = docRef.id
                            android.util.Log.d("DiaryWriteScreen", "Firestore 게시글 생성 완료: $newSharedPostId (Lv.$currentLevel, Day $currentDays)")
                        }

                        // Case 2: 체크박스 ON & sharedPostId != null (기존 글 수정)
                        isSharing && !currentSharedPostId.isNullOrBlank() -> {
                            android.util.Log.d("DiaryWriteScreen", "[Case 2] 기존 공유 글 수정 - Firestore 업데이트: $currentSharedPostId")

                            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            firestore.collection("posts").document(currentSharedPostId)
                                .update(
                                    mapOf(
                                        "content" to postData.content,
                                        "tagType" to postData.tagType,
                                        "thirstLevel" to (postData.thirstLevel ?: 0),
                                        "imageUrl" to (postData.imageUrl ?: "")
                                    )
                                ).await()
                            android.util.Log.d("DiaryWriteScreen", "Firestore 게시글 수정 완료")
                        }

                        // Case 3: 체크박스 OFF & sharedPostId != null (공유 취소 - 삭제)
                        !isSharing && !currentSharedPostId.isNullOrBlank() -> {
                            android.util.Log.d("DiaryWriteScreen", "[Case 3] 공유 취소 - Firestore에서 삭제: $currentSharedPostId")

                            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            firestore.collection("posts").document(currentSharedPostId).delete().await()
                            newSharedPostId = null
                            android.util.Log.d("DiaryWriteScreen", "Firestore 게시글 삭제 완료")
                        }

                        // Case 4: 체크박스 OFF & sharedPostId == null (아무 작업 없음)
                        else -> {
                            android.util.Log.d("DiaryWriteScreen", "[Case 4] 공유하지 않음 - Firestore 작업 없음")
                        }
                    }

                    // === Room DB 저장 ===
                    if (diaryId != null) {
                        // [수정 모드] 날짜 변경 금지 (기존 타임스탬프 유지)
                        val originalTimestamp = existingDiary?.timestamp ?: System.currentTimeMillis()

                        // [CRITICAL] 수정 모드: 작성 당시의 레벨/일수 유지 (2025-12-27)
                        val originalLevel = existingDiary?.userLevel ?: currentLevel
                        val originalDays = existingDiary?.currentDays ?: currentDays

                        val updatedDiary = existingDiary?.copy(
                            content = postData.content,
                            cravingLevel = postData.thirstLevel ?: 0,
                            imageUrl = postData.imageUrl ?: "",
                            tagType = postData.tagType,
                            timestamp = originalTimestamp,
                            date = formatDate(originalTimestamp),
                            sharedPostId = newSharedPostId, // [NEW] Firestore ID 저장 (2025-12-25)
                            userLevel = originalLevel,   // [CHANGED] 원본 레벨 유지 (현재 레벨로 덮어쓰지 않음)
                            currentDays = originalDays   // [CHANGED] 원본 일수 유지 (현재 일수로 덮어쓰지 않음)
                        )
                        if (updatedDiary != null) {
                            diaryViewModel.updateDiary(updatedDiary)
                            android.util.Log.d("DiaryWriteScreen", "일기 수정 성공: 태그=${postData.tagType}, 원본 Lv.$originalLevel 유지, Day $originalDays 유지, sharedPostId=$newSharedPostId")
                        }
                    } else {
                        // [신규 모드] 선택된 날짜 사용
                        val targetTimestamp = selectedDate ?: System.currentTimeMillis()
                        val newDiary = DiaryEntity(
                            emoji = "📝",
                            content = postData.content,
                            cravingLevel = postData.thirstLevel ?: 0,
                            timestamp = targetTimestamp,
                            date = formatDate(targetTimestamp),
                            imageUrl = postData.imageUrl ?: "",
                            tagType = postData.tagType,
                            sharedPostId = newSharedPostId, // [NEW] Firestore ID 저장 (2025-12-25)
                            userLevel = currentLevel, // [NEW] 현재 레벨 저장 (2025-12-26)
                            currentDays = currentDays // [NEW] 현재 일수 저장 (2025-12-26)
                        )
                        diaryViewModel.insertDiary(newDiary)
                        android.util.Log.d("DiaryWriteScreen", "일기 생성 성공: 태그=${postData.tagType}, 날짜=${formatDate(targetTimestamp)}, Lv.$currentLevel, Day $currentDays, sharedPostId=$newSharedPostId")

                        // [NEW] Firebase Analytics: 일기 저장 이벤트 전송 (2026-01-02)
                        try {
                            kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logDiarySave(
                                mood = postData.tagType ?: "none",
                                contentLength = postData.content.length,
                                hasImage = postData.imageUrl?.isNotEmpty() == true,
                                dayCount = currentDays
                            )
                            android.util.Log.d("DiaryWriteScreen", "✅ diary_save 이벤트 전송 완료")
                        } catch (e: Exception) {
                            android.util.Log.e("DiaryWriteScreen", "❌ diary_save 이벤트 전송 실패", e)
                        }
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
                                // [핵심 추가] 선택한 사진에 대한 읽기 권한을 '영구적으로' 확보합니다.
                                // 이 코드가 있어야 앱을 껐다 켜도 사진이 사라지지 않습니다.
                                val contentResolver = context.contentResolver
                                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                contentResolver.takePersistableUriPermission(uri, takeFlags)

                                communityViewModel.onImageSelected(uri)
                            } catch (e: Exception) {
                                android.util.Log.e("DiaryWriteScreen", "Photo select failed", e)
                                communityViewModel.onImageSelected(uri)
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
