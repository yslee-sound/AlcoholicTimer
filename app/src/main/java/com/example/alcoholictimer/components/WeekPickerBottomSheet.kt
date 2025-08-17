package com.example.alcoholictimer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekPickerBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onWeekPicked: (weekStart: Long, weekEnd: Long, displayText: String) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            Color(0xFF636E72).copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            WeekPickerContent(
                onWeekPicked = onWeekPicked,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
internal fun WeekPickerContent(
    onWeekPicked: (weekStart: Long, weekEnd: Long, displayText: String) -> Unit,
    onDismiss: () -> Unit
) {
    // 최근 8주간의 주 목록 생성
    val weekOptions = remember {
        generateWeekOptions()
    }

    // 기본값을 "이번 주"(마지막 인덱스)로 설정
    var selectedWeekIndex by remember { mutableStateOf(weekOptions.size - 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단: 안내
        Text(
            text = "주 선택",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "최근 4주 중에서 선택하세요",
            fontSize = 14.sp,
            color = Color(0xFF636E72),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 가운데: 주 선택 NumberPicker
        NumberPicker(
            value = selectedWeekIndex,
            onValueChange = { selectedWeekIndex = it },
            range = 0 until weekOptions.size,
            displayValues = weekOptions.map { it.displayText },
            modifier = Modifier.width(220.dp)
        )

        // 하단: 선택 버튼
        Button(
            onClick = {
                val selectedWeek = weekOptions[selectedWeekIndex]
                onWeekPicked(selectedWeek.startTime, selectedWeek.endTime, selectedWeek.displayText)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF74B9FF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

data class WeekOption(
    val startTime: Long,
    val endTime: Long,
    val displayText: String
)

private fun generateWeekOptions(): List<WeekOption> {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    val options = mutableListOf<WeekOption>()

    // 이번 주부터 시작해서 과거로 거슬러 올라가면서 데이터 생성
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    for (i in 0 until 4) { // 최근 4주로 제한
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_WEEK, 6) // 일요일로 이동
        val weekEnd = calendar.timeInMillis

        val startDate = dateFormat.format(Date(weekStart))
        val endDate = dateFormat.format(Date(weekEnd))

        val displayText = when (i) {
            0 -> "이번 주"
            1 -> "지난 주"
            else -> "$startDate ~ $endDate"
        }

        options.add(WeekOption(weekStart, weekEnd, displayText))

        // 다음 주로 이동 (실제로는 이전 주)
        calendar.add(Calendar.DAY_OF_WEEK, -13) // 일요일에서 이전 주 월요일로
    }

    // 과거에서 현재 순으로 정렬 (reverse)
    return options.reversed()
}

@Preview(showBackground = true)
@Composable
fun WeekPickerBottomSheetPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        WeekPickerContent(
            onWeekPicked = { _, _, _ -> },
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true, name = "WeekPicker - Dark Mode")
@Composable
fun WeekPickerBottomSheetDarkPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        WeekPickerContent(
            onWeekPicked = { _, _, _ -> },
            onDismiss = { }
        )
    }
}
