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
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }

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
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, -1)
                    val prevMonth = calendar.get(Calendar.MONTH) + 1

                    Text(
                        text = "${prevMonth}월",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 안내 텍스트
                    Text(
                        text = "년/월 선택",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // 가운데: NumberPicker 2개 (연도, 월)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 연도 선택
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "연도",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        NumberPicker(
                            value = selectedYear,
                            onValueChange = { selectedYear = it },
                            range = 2000..2030,
                            displayValues = (2000..2030).map { "${it}년" },
                            modifier = Modifier.width(120.dp)
                        )
                    }

                    // 구분선
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(120.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )

                    // 월 선택
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "월",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        NumberPicker(
                            value = selectedMonth,
                            onValueChange = { selectedMonth = it },
                            range = 1..12,
                            displayValues = (1..12).map { "${it}월" },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                // 하단: 선택 버튼
                Button(
                    onClick = {
                        onMonthPicked(selectedYear, selectedMonth)
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
