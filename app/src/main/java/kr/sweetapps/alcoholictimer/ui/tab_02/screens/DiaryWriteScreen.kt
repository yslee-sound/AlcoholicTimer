// [NEW] 일기 작성 화면
package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * [NEW] 일기 작성/상세 화면
 * - 새 일기 작성: diaryId = null
 * - 기존 일기 보기/수정: diaryId != null (초기 모드는 읽기 모드)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryWriteScreen(
    diaryId: Long? = null, // [UPDATED] Room DB의 ID (Long 타입)
    onDismiss: () -> Unit = {}
) {
    // [NEW] ViewModel 연결
    val viewModel: DiaryViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // [NEW] 기존 일기 데이터 로드
    var initialMood by remember { mutableStateOf<String?>(null) }
    var initialCraving by remember { mutableIntStateOf(5) } // [FIX] 기본값 5 (중간값, 새 작성 시)
    var initialText by remember { mutableStateOf("") }
    var initialDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            val diary = viewModel.getDiaryById(diaryId)
            if (diary != null) {
                initialMood = diary.emoji
                initialCraving = diary.cravingLevel
                initialText = diary.content
                initialDate = diary.timestamp
            }
        }
    }

    // [NEW] 모드 관리: 읽기/수정
    var isEditMode by remember { mutableStateOf(diaryId == null) } // 새 작성이면 수정 모드, 기존 일기면 읽기 모드
    val isViewMode = diaryId != null && !isEditMode

    // [FIX] 갈망도 슬라이더 값 (필수)
    var cravingLevel by remember { mutableFloatStateOf(initialCraving.toFloat()) }
    var diaryText by remember { mutableStateOf(initialText) }
    var selectedDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                timeInMillis = initialDate
            }
        )
    }

    // [FIX] initial 값들이 변경되면 상태 업데이트
    LaunchedEffect(initialCraving) {
        cravingLevel = initialCraving.toFloat()
    }
    LaunchedEffect(initialText) {
        diaryText = initialText
    }
    LaunchedEffect(initialDate) {
        selectedDate = Calendar.getInstance().apply { timeInMillis = initialDate }
    }

    // [NEW] 갈망도 점수에 따라 이모지 자동 생성
    fun getEmojiByScore(score: Int): String {
        return when (score) {
            in 1..2 -> "🥰" // 아주 좋음 (사랑/행복)
            in 3..4 -> "🙂" // 좋음 (미소)
            in 5..6 -> "😐" // 보통 (무표정)
            in 7..8 -> "😥" // 나쁨/참기 힘듦 (식은땀/걱정)
            in 9..10 -> "😫" // 아주 나쁨/위기 (괴로움/절규)
            else -> "😐" // 기본값
        }
    }

    // [NEW] 더보기 메뉴 상태
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) } // [NEW] 날짜 선택 다이얼로그 상태

    // 날짜 포맷 (시스템 로케일에 따라 자동 선택)
    val currentLocale = Locale.getDefault()
    val dateFormat = remember(currentLocale) {
        when (currentLocale.language) {
            "ko" -> SimpleDateFormat("yyyy년 M월 d일 (E)", currentLocale)
            else -> SimpleDateFormat("MMM d, yyyy (E)", currentLocale)
        }
    }
    val timeFormat = remember(currentLocale) {
        SimpleDateFormat("a h:mm", currentLocale)
    }

    // [FIX] Scaffold 패턴 적용: TopBar 고정, Content 스크롤 분리
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // [MOVED] BackTopBar를 Scaffold의 topBar 슬롯으로 이동 → 화면 상단에 고정
            BackTopBar(
                title = when {
                    isViewMode -> "" // [FIX] 상세보기 모드에서는 중앙 타이틀 제거
                    diaryId != null -> stringResource(R.string.diary_edit_title)
                    else -> stringResource(R.string.diary_write_title)
                },
                onBack = onDismiss,
                trailingContent = {
                    when {
                        isViewMode -> {
                            // 읽기 모드: 점 3개 메뉴
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.cd_menu),
                                        tint = Color(0xFF2D3748)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.diary_menu_edit)) },
                                        onClick = {
                                            showMenu = false
                                            isEditMode = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.diary_menu_delete)) },
                                        onClick = {
                                            showMenu = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                        isEditMode -> {
                            // [UPDATED] 수정/작성 모드: "저장" 텍스트 버튼
                            TextButton(
                                onClick = {
                                    // [FIX] 갈망도 점수에 따라 이모지 자동 생성
                                    val autoEmoji = getEmojiByScore(cravingLevel.toInt())

                                    scope.launch {
                                        if (diaryId != null) {
                                            viewModel.updateDiary(
                                                id = diaryId,
                                                emoji = autoEmoji,
                                                content = diaryText,
                                                cravingLevel = cravingLevel.toInt(),
                                                timestamp = selectedDate.timeInMillis // [NEW] 선택된 날짜 사용
                                            )
                                        } else {
                                            viewModel.saveDiary(
                                                emoji = autoEmoji,
                                                content = diaryText,
                                                cravingLevel = cravingLevel.toInt(),
                                                timestamp = selectedDate.timeInMillis, // [NEW] 선택된 날짜 사용
                                            )
                                        }
                                        onDismiss()
                                    }
                                },
                                enabled = true // [FIX] 갈망도는 기본값이 있으므로 항상 활성화
                            ) {
                                Text(
                                    text = stringResource(R.string.diary_save_button),
                                    color = kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue, // [FIX] 표준 색상 적용
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium // [FIX] 표준 타이포그래피 적용
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // [FIX] Content 영역: innerPadding 적용하여 TopBar와 겹치지 않도록
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding) // [NEW] Scaffold의 innerPadding 적용
                .imePadding() // [FIX] 키보드 높이만큼 패딩을 주어 가려짐 방지
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 1. 날짜/시간 영역
            DateTimeSection(
                date = dateFormat.format(selectedDate.time),
                time = timeFormat.format(selectedDate.time),
                onClick = { if (isEditMode) showDatePicker = true } // [NEW] 수정 모드에서만 날짜 변경 가능
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. [FIX] 음주 욕구 게이지 (필수) - 첫 번째 섹션으로 이동
            CravingSliderSection(
                cravingLevel = cravingLevel,
                onCravingChanged = { cravingLevel = it },
                enabled = isEditMode // [NEW] 읽기 모드에서는 비활성화
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 텍스트 입력 영역
            DiaryTextInputSection(
                text = diaryText,
                onTextChanged = { diaryText = it },
                enabled = isEditMode // [NEW] 읽기 모드에서는 비활성화
            )

            Spacer(modifier = Modifier.height(32.dp)) // [UPDATED] 저장 버튼 제거 (TopBar로 이동)
        }
    }

    // [NEW] 삭제 확인 다이얼로그 (Scaffold 외부에 배치)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.diary_delete_confirm_title)) },
            text = { Text(stringResource(R.string.diary_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            if (diaryId != null) {
                                viewModel.deleteDiary(diaryId)
                            }
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.diary_delete_confirm_button), color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // [NEW] 날짜 선택 다이얼로그
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.timeInMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // 미래 날짜 선택 불가
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Calendar.getInstance().apply {
                                timeInMillis = millis
                                // 시간은 현재 시간으로 유지
                                set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, Calendar.getInstance().get(Calendar.MINUTE))
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


/**
 * [NEW] 날짜/시간 섹션
 */
@Composable
private fun DateTimeSection(
    date: String,
    time: String,
    onClick: () -> Unit = {} // [NEW] 클릭 이벤트
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick) // [NEW] 클릭 가능
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📅", fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                date,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2D3748)
            )
            Text(
                time,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}

/**
 * [FIX] 음주 욕구 슬라이더 섹션 (필수)
 */
@Composable
private fun CravingSliderSection(
    cravingLevel: Float,
    onCravingChanged: (Float) -> Unit,
    enabled: Boolean = true // [NEW] 읽기 모드 지원
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Text(
            stringResource(R.string.diary_question_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748)
        )

        Text(
            stringResource(R.string.diary_question_required),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.diary_craving_weak), style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            Text(stringResource(R.string.diary_craving_strong), style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }

        Slider(
            value = cravingLevel,
            onValueChange = onCravingChanged,
            valueRange = 0f..10f,
            steps = 9,
            enabled = enabled, // [NEW] 읽기 모드에서는 비활성화
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0xFFE2E8F0),
                disabledThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), // [NEW] 비활성 색상
                disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // [NEW] 비활성 색상
            )
        )

        Text(
            stringResource(R.string.diary_craving_label, cravingLevel.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * [NEW] 텍스트 입력 섹션
 */
@Composable
private fun DiaryTextInputSection(
    text: String,
    onTextChanged: (String) -> Unit,
    enabled: Boolean = true // [NEW] 읽기 모드 지원
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Text(
            stringResource(R.string.diary_mood_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748)
        )

        Text(
            stringResource(R.string.diary_mood_optional),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            placeholder = {
                Text(
                    stringResource(R.string.diary_content_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )
            },
            enabled = enabled, // [NEW] 읽기 모드에서는 비활성화
            readOnly = !enabled, // [NEW] 읽기 전용
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                disabledBorderColor = Color(0xFFE2E8F0), // [NEW] 비활성 테두리
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF8F9FA), // [NEW] 비활성 배경
                disabledTextColor = Color(0xFF2D3748) // [NEW] 비활성 텍스트 색상 (읽기 가능하게)
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
        )
    }
}
