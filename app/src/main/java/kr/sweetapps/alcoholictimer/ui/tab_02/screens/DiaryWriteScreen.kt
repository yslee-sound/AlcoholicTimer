// [NEW] 일기 작성 화면
package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import org.json.JSONArray
import org.json.JSONObject
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
    diaryId: String? = null, // [NEW] 일기 ID (null이면 새 작성, 있으면 상세/수정)
    initialMood: String? = null, // [NEW] 초기 기분 이모지
    initialCraving: Int = 0, // [NEW] 초기 갈망 수치
    initialText: String = "", // [NEW] 초기 일기 내용
    initialDate: Long? = null, // [NEW] 초기 날짜 (timestamp)
    onDismiss: () -> Unit = {},
    onSave: (String, String, Int) -> Unit = { _, _, _ -> },
    onUpdate: (String, String, Int) -> Unit = { _, _, _ -> }, // [NEW] 업데이트 콜백
    onDelete: () -> Unit = {} // [NEW] 삭제 콜백
) {
    // [NEW] 모드 관리: 읽기/수정
    var isEditMode by remember { mutableStateOf(diaryId == null) } // 새 작성이면 수정 모드, 기존 일기면 읽기 모드
    val isViewMode = diaryId != null && !isEditMode

    // 상태 관리
    var selectedMood by remember {
        mutableStateOf<MoodType?>(
            initialMood?.let { emoji -> MoodType.entries.find { it.emoji == emoji } }
        )
    }
    var cravingLevel by remember { mutableFloatStateOf(initialCraving.toFloat()) }
    var diaryText by remember { mutableStateOf(initialText) }
    var selectedDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                initialDate?.let { timeInMillis = it }
            }
        )
    }

    // [NEW] 더보기 메뉴 상태
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 날짜 포맷
    val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }
    val timeFormat = remember { SimpleDateFormat("a h:mm", Locale.KOREAN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // [수정] 모드에 따른 TopBar 구성
        BackTopBar(
            title = when {
                isViewMode -> dateFormat.format(selectedDate.time) // 읽기 모드: 날짜 표시
                diaryId != null -> "일기 수정" // 수정 모드 (기존 일기)
                else -> "일기 쓰기" // 새 작성
            },
            onBack = onDismiss,
            trailingContent = {
                when {
                    isViewMode -> {
                        // [NEW] 읽기 모드: 점 3개 메뉴
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "메뉴",
                                    tint = Color(0xFF2D3748)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("수정하기") },
                                    onClick = {
                                        showMenu = false
                                        isEditMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("삭제하기") },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                    isEditMode -> {
                        // [NEW] 수정 모드: 저장 버튼
                        IconButton(
                            onClick = {
                                if (selectedMood != null) {
                                    if (diaryId != null) {
                                        onUpdate(selectedMood!!.emoji, diaryText, cravingLevel.toInt())
                                    } else {
                                        onSave(selectedMood!!.emoji, diaryText, cravingLevel.toInt())
                                    }
                                }
                            },
                            enabled = selectedMood != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "저장",
                                tint = if (selectedMood != null) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        )

        // [NEW] 삭제 확인 다이얼로그
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("일기 삭제") },
                text = { Text("정말 이 일기를 삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        }
                    ) {
                        Text("삭제", color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 1. 날짜/시간 영역
            DateTimeSection(
                date = dateFormat.format(selectedDate.time),
                time = timeFormat.format(selectedDate.time)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 오늘의 기분 선택 (필수)
            MoodSelectionSection(
                selectedMood = selectedMood,
                onMoodSelected = { selectedMood = it },
                enabled = isEditMode // [NEW] 읽기 모드에서는 비활성화
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 음주 욕구 게이지 (선택)
            CravingSliderSection(
                cravingLevel = cravingLevel,
                onCravingChanged = { cravingLevel = it },
                enabled = isEditMode // [NEW] 읽기 모드에서는 비활성화
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 텍스트 입력 영역
            DiaryTextInputSection(
                text = diaryText,
                onTextChanged = { diaryText = it },
                enabled = isEditMode // [NEW] 읽기 모드에서는 비활성화
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 저장 버튼 (수정 모드에서만 표시)
            if (isEditMode) {
                Button(
                    onClick = {
                        if (selectedMood != null) {
                            if (diaryId != null) {
                                onUpdate(selectedMood!!.emoji, diaryText, cravingLevel.toInt())
                            } else {
                                onSave(selectedMood!!.emoji, diaryText, cravingLevel.toInt())
                            }
                        }
                    },
                    enabled = selectedMood != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (diaryId != null) "일기 수정 완료" else "오늘의 기록 저장하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * [NEW] 기분 타입 정의
 */
enum class MoodType(val emoji: String, val label: String, val color: Color) {
    PROUD("😊", "뿌듯", Color(0xFFFCD34D)),
    CALM("😌", "평온", Color(0xFF93C5FD)),
    SAD("😢", "우울", Color(0xFFA78BFA)),
    ANGRY("😡", "화남", Color(0xFFFCA5A5)),
    CRAVING("😰", "갈망", Color(0xFFFB923C))
}

/**
 * [NEW] 날짜/시간 섹션
 */
@Composable
private fun DateTimeSection(date: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
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
 * [NEW] 기분 선택 섹션
 */
@Composable
private fun MoodSelectionSection(
    selectedMood: MoodType?,
    onMoodSelected: (MoodType) -> Unit,
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
            "오늘 하루, 어떠셨나요?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748)
        )

        Text(
            "하나를 선택해주세요 (필수)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MoodType.entries.forEach { mood ->
                MoodItem(
                    mood = mood,
                    isSelected = selectedMood == mood,
                    onClick = { if (enabled) onMoodSelected(mood) }, // [NEW] enabled 체크
                    enabled = enabled // [NEW] enabled 전달
                )
            }
        }
    }
}

/**
 * [NEW] 기분 아이템
 */
@Composable
private fun MoodItem(
    mood: MoodType,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true // [NEW] 읽기 모드 지원
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick) // [NEW] enabled 체크
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isSelected) mood.color.copy(alpha = 0.2f) else Color(0xFFF1F5F9))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) mood.color else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                mood.emoji,
                fontSize = 32.sp,
                color = if (enabled) Color.Unspecified else Color.Gray.copy(alpha = 0.5f) // [NEW] 비활성 상태 표시
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            mood.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color(0xFF2D3748) else Color(0xFF94A3B8),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * [NEW] 음주 욕구 슬라이더 섹션
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
            "술 생각이 나셨나요?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748)
        )

        Text(
            "선택사항",
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
            Text("안남", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            Text("아주 많이", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
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
            "욕구 수치: ${cravingLevel.toInt()}/10",
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
            "오늘의 기록",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748)
        )

        Text(
            "선택사항",
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
                    "오늘 가장 힘들었던 순간이나,\n나에게 해주고 싶은 칭찬을 적어보세요.",
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

