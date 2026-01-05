// [NEW] 글쓰기 화면 컴포넌트 분리 (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_03.components

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.CommunityViewModel

/**
 * 글쓰기 화면의 내부 콘텐츠
 * [REFACTORED] CommunityScreen.kt에서 분리 (2026-01-05)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WritePostScreenContent(
    viewModel: CommunityViewModel,
    currentNickname: String,
    isDiaryMode: Boolean = false,
    postToEdit: Post? = null,
    isTodayDiary: Boolean = true,
    isAlreadyShared: Boolean = false,
    onPost: (String) -> Unit,
    onSaveDiary: (Post, Boolean) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onOpenPhoto: () -> Unit
) {
    val isEditMode = postToEdit != null

    LaunchedEffect(postToEdit, isDiaryMode, isEditMode) {
        android.util.Log.d("WritePostScreen", "수정 모드 확인: isDiaryMode=$isDiaryMode, isEditMode=$isEditMode, postToEdit=${postToEdit?.content?.take(20)}")
    }

    var isShareToCommunity by remember(isAlreadyShared) { mutableStateOf(isAlreadyShared) }
    val originalIsShareToCommunity = remember(isAlreadyShared) { isAlreadyShared }

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
    val focusManager = LocalFocusManager.current
    var showWarningSheet by remember { mutableStateOf(false) }
    var showThirstSlider by remember { mutableStateOf(false) }

    var selectedLevel by remember(postToEdit) {
        mutableStateOf<Int?>(postToEdit?.thirstLevel)
    }

    LaunchedEffect(postToEdit) {
        if (postToEdit == null) {
            viewModel.clearSelectedImage()
        } else {
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

    val currentUserAvatarIndex by viewModel.currentUserAvatarIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms: Map<String, Boolean> ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            try {
                onOpenPhoto()
            } catch (_: SecurityException) {
                Toast.makeText(context, "권한 문제로 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
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

    val requestPermissionsAndOpen: () -> Unit = {
        onOpenPhoto()

        val perms = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        perms.add(android.Manifest.permission.CAMERA)

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
            multiplePermissionLauncher.launch(perms.toTypedArray())
        }
    }

    var selectedTag by remember(postToEdit) {
        mutableStateOf(postToEdit?.tagType?.takeIf { it.isNotBlank() } ?: "diary")
    }

    val placeholderText = when (selectedTag) {
        "diary" -> stringResource(R.string.diary_placeholder_diary)
        "thanks" -> stringResource(R.string.diary_placeholder_thanks)
        "reflect" -> stringResource(R.string.diary_placeholder_reflect)
        else -> stringResource(R.string.diary_placeholder_diary)
    }

    val isModified = remember(textFieldValue, selectedLevel, selectedImageUri, selectedTag, isShareToCommunity, postToEdit) {
        if (postToEdit == null) {
            textFieldValue.text.isNotBlank() || selectedImageUri != null
        } else {
            val contentChanged = textFieldValue.text.trim() != postToEdit.content.trim()
            val levelChanged = selectedLevel != postToEdit.thirstLevel
            val tagChanged = selectedTag != postToEdit.tagType
            val shareChanged = isShareToCommunity != originalIsShareToCommunity

            val currentUriString = selectedImageUri?.toString() ?: ""
            val originalUrlString = postToEdit.imageUrl ?: ""
            val imageChanged = currentUriString != originalUrlString

            contentChanged || levelChanged || tagChanged || imageChanged || shareChanged
        }
    }

    val onBackAction = {
        if (isModified) {
            showWarningSheet = true
        } else {
            onDismiss()
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            showThirstSlider = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isDiaryMode && isEditMode -> stringResource(R.string.diary_edit_title)
                            isDiaryMode -> stringResource(R.string.diary_write_title)
                            isEditMode -> stringResource(R.string.community_edit_title)
                            else -> stringResource(R.string.community_write_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1F2937)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackAction) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if ((textFieldValue.text.isNotBlank() || selectedImageUri != null) && !isLoading) {
                                val payload = textFieldValue.text.trim()
                                try {
                                    if (isDiaryMode) {
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

                                        onSaveDiary(diaryEntry, isShareToCommunity)
                                        onPost(payload)
                                    } else if (isEditMode && postToEdit != null) {
                                        viewModel.updatePost(
                                            postId = postToEdit.id,
                                            newContent = payload,
                                            context = context,
                                            newTagType = selectedTag,
                                            newThirstLevel = selectedLevel,
                                            onSuccess = {
                                                onPost(payload)
                                            }
                                        )
                                    } else {
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
                        enabled = (isLoading || isModified)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    isDiaryMode && isEditMode -> stringResource(R.string.diary_save_complete)
                                    isDiaryMode -> stringResource(R.string.diary_save)
                                    isEditMode -> stringResource(R.string.community_edit_complete)
                                    else -> stringResource(R.string.community_write_publish)
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
                            focusManager.clearFocus()
                            showThirstSlider = !showThirstSlider
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Restaurant, contentDescription = stringResource(R.string.community_thirst_level), tint = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.community_thirst_level), color = Color(0xFF1F2937), style = MaterialTheme.typography.bodyMedium)
                }

                if (showThirstSlider) {
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
                                    .background(if (selected) kr.sweetapps.alcoholictimer.util.ThirstColorUtil.getColor(value) else Color(0xFFF0F0F0))
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
                            focusManager.clearFocus()
                            requestPermissionsAndOpen()
                        } else Modifier)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Image, contentDescription = stringResource(R.string.community_add_photo), tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.community_add_photo), color = Color(0xFF1F2937), style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
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

                Column(modifier = Modifier.weight(1f)) {
                    val displayNickname = if (currentNickname.isNotBlank()) currentNickname else "익명"
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                        if (selectedLevel != null && selectedLevel!! > 0) {
                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111111)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

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

                            Text(
                                text = " ${stringResource(R.string.community_thirst)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111111)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val userStatus by viewModel.userStatus.collectAsState(initial = kr.sweetapps.alcoholictimer.util.manager.UserStatusManager.UserStatus.DEFAULT)
                        val levelInfoText = if (postToEdit != null) {
                            "${stringResource(R.string.level_format, postToEdit.userLevel)} · ${stringResource(R.string.days_format, postToEdit.currentDays)}"
                        } else {
                            "${stringResource(R.string.level_format, userStatus.level)} · ${stringResource(R.string.days_format, userStatus.days)}"
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue.copy(alpha = 0.1f),
                        ) {
                            Text(
                                text = levelInfoText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (isDiaryMode) {
                            Spacer(modifier = Modifier.width(8.dp))

                            val isEnabled = isTodayDiary

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (!isEnabled) {
                                    Color(0xFFE5E7EB)
                                } else if (isShareToCommunity) {
                                    kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
                                } else {
                                    kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue.copy(alpha = 0.1f)
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = isEnabled) {
                                        isShareToCommunity = !isShareToCommunity
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isShareToCommunity) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (!isEnabled) {
                                            Color(0xFF9CA3AF)
                                        } else if (isShareToCommunity) {
                                            Color.White
                                        } else {
                                            kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.community_share_challenge),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (!isEnabled) {
                                                Color(0xFF9CA3AF)
                                            } else if (isShareToCommunity) {
                                                Color.White
                                            } else {
                                                kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data class TagInfo(val key: String, val nameResId: Int, val bgColor: Color, val textColor: Color)
                val tags = listOf(
                    TagInfo("diary", R.string.community_tag_diary, Color(0xFF7C4DFF), Color.White),
                    TagInfo("thanks", R.string.community_tag_thanks, Color(0xFF00BFA5), Color.White),
                    TagInfo("reflect", R.string.community_tag_reflect, Color(0xFFFF6F00), Color.White)
                )

                tags.forEach { tag ->
                    val isSelected = selectedTag == tag.key

                    Surface(
                        modifier = Modifier.clickable { if (!isLoading) selectedTag = tag.key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) tag.bgColor else Color(0xFFF5F5F5),
                        border = null
                    ) {
                        Text(
                            text = stringResource(tag.nameResId),
                            color = if (isSelected) tag.textColor else Color(0xFF9E9E9E),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            val lineHeightDp = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.fontSize.toDp() }
            val totalLines = textFieldValue.text.count { it == '\n' } + 1
            val cursorOffset = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
            val cursorLine = textFieldValue.text.take(cursorOffset).count { it == '\n' } + 1
            val minLines = 4
            val maxLines = maxOf(minLines, totalLines)
            val desiredDistanceLines = 4
            val currentDistanceLines = (maxLines - cursorLine + 1)
            val extraLinesNeeded = maxOf(0, desiredDistanceLines - currentDistanceLines)

            TextField(
                value = textFieldValue,
                onValueChange = { if (!isLoading) textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .onFocusChanged { state ->
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
                isError = false,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    disabledTextColor = Color(0xFF6B7280)
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                enabled = !isLoading
            )

            if (extraLinesNeeded > 0) {
                Spacer(modifier = Modifier.height(lineHeightDp * extraLinesNeeded))
            }

            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "선택된 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )

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

            if (showWarningSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showWarningSheet = false },
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        val titleText = if (isEditMode) {
                            stringResource(R.string.community_cancel_edit_title)
                        } else {
                            stringResource(R.string.community_discard_post_title)
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
                        )

                        val actionText = if (isEditMode) stringResource(R.string.community_discard_changes) else stringResource(R.string.community_post_delete)
                        val actionIcon = if (isEditMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Delete
                        val actionColor = if (isEditMode) Color(0xFF1F2937) else Color(0xFFEF4444)

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

                        val continueText = if (isEditMode) stringResource(R.string.community_continue_editing) else stringResource(R.string.community_continue_writing)

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

