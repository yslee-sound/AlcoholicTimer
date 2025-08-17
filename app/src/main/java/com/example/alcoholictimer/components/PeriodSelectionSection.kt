package com.example.alcoholictimer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PeriodSelectionSection(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    onPeriodClick: (String) -> Unit,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier
) {
    val periods = listOf("주", "월", "년", "전체")

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 기간 선택 탭
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEach { period ->
                    val isSelected = period == selectedPeriod
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable {
                                onPeriodSelected(period)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF74B9FF) else Color.Transparent
                    ) {
                        Text(
                            text = period,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF636E72)
                        )
                    }
                }
            }
        }

        // 세부 기간 표시: 항상 보이도록 조건문 제거
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (selectedPeriod == "전체") Modifier else Modifier.clickable { onPeriodClick(selectedPeriod) }
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = getCurrentPeriodText(selectedPeriod, selectedDetailPeriod),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                if (selectedPeriod != "전체") {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "세부 기간 선택",
                        tint = Color(0xFF74B9FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getCurrentPeriodText(selectedPeriod: String, selectedDetailPeriod: String): String {
    val calendar = Calendar.getInstance()

    return when (selectedPeriod) {
        "주" -> {
            if (selectedDetailPeriod.isNotEmpty()) {
                // 바텀시트에서 선택된 주 정보 파싱
                selectedDetailPeriod
            } else {
                // 기본값: 이번 주
                "이번 주"
            }
        }
        "월" -> {
            if (selectedDetailPeriod.isNotEmpty()) {
                // 바텀시트에서 선택된 월 정보 파싱
                selectedDetailPeriod
            } else {
                // 기본값: 현재 월
                val monthFormat = SimpleDateFormat("yyyy년 M월", Locale.getDefault())
                monthFormat.format(calendar.time)
            }
        }
        "년" -> {
            if (selectedDetailPeriod.isNotEmpty()) {
                // 바텀시트에서 선택된 년 정보 파싱
                selectedDetailPeriod
            } else {
                // 기본값: 현재 년
                val yearFormat = SimpleDateFormat("yyyy년", Locale.getDefault())
                yearFormat.format(calendar.time)
            }
        }
        else -> "전체"
    }
}
