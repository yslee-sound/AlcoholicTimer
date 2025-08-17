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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onMonthPicked: (year: Int, month: Int) -> Unit,
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
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
            MonthPickerContent(
                onMonthPicked = onMonthPicked,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
internal fun MonthPickerContent(
    onMonthPicked: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val monthOptions = remember {
        generateMonthOptions()
    }
    // 기본 선택값: 현재 월이 리스트의 마지막이 아니라 마지막(오름차순) 인덱스가 되도록 계산
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val defaultIndex = monthOptions.indexOfFirst { it.year == currentYear && it.month == currentMonth }.coerceAtLeast(0)
    var selectedMonthIndex by remember { mutableStateOf(defaultIndex) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단: 안내
        Text(
            text = "월 선택",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 가운데: 월 선택 NumberPicker
        NumberPicker(
            value = selectedMonthIndex,
            onValueChange = { selectedMonthIndex = it },
            range = 0 until monthOptions.size,
            displayValues = monthOptions.map { it.displayText },
            modifier = Modifier.width(220.dp)
        )

        // 하단: 선택 버튼
        Button(
            onClick = {
                val selectedMonth = monthOptions[selectedMonthIndex]
                onMonthPicked(selectedMonth.year, selectedMonth.month)
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

data class MonthOption(
    val year: Int,
    val month: Int,
    val displayText: String
)

private fun generateMonthOptions(): List<MonthOption> {
    val calendar = Calendar.getInstance()
    val options = mutableListOf<MonthOption>()

    // 현재 월부터 시작해서 과거로 거슬러 올라가면서 4개월 생성
    for (i in 0 until 4) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작

        val displayText = "${year}년 ${month}월"
        options.add(MonthOption(year, month, displayText))

        // 이전 달로 이동
        calendar.add(Calendar.MONTH, -1)
    }

    // 오름차순(과거→현재)으로 반환
    return options.reversed()
}

@Preview(showBackground = true)
@Composable
fun MonthPickerBottomSheetPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        MonthPickerContent(
            onMonthPicked = { _, _ -> },
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true, name = "MonthPicker - Dark Mode")
@Composable
fun MonthPickerBottomSheetDarkPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        MonthPickerContent(
            onMonthPicked = { _, _ -> },
            onDismiss = { }
        )
    }
}
