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
    // 현재와 과거 4개월 목록 생성
    val monthOptions = remember {
        generateMonthOptions()
    }

    // 기본값을 현재 월(첫 번째 인덱스)로 설정
    var selectedMonthIndex by remember { mutableStateOf(0) }

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단: 이전월 표시와 안내
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이전월 표시
                    Text(
                        text = if (monthOptions.size > 1) monthOptions[1].displayText else "",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 안내 텍스트
                    Text(
                        text = "월 선택",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // 가운데: 월 선택 NumberPicker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "월 선택",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    NumberPicker(
                        value = selectedMonthIndex,
                        onValueChange = { selectedMonthIndex = it },
                        range = 0 until monthOptions.size,
                        displayValues = monthOptions.map { it.displayText },
                        modifier = Modifier.width(200.dp)
                    )
                }

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
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "선택",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 하단 여백
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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

    // 현재에서 과거 순으로 반환 (현재 월이 첫 번째)
    return options
}
