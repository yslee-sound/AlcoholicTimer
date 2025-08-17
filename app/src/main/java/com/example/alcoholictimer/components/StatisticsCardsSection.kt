package com.example.alcoholictimer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord

@Composable
fun StatisticsCardsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    val totalDays = records.sumOf { it.actualDays }
    val completedCount = records.count { it.isCompleted }
    val totalAttempts = records.size
    val successRate = if (totalAttempts > 0) (completedCount * 100) / totalAttempts else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 텍스트 클릭 영역 (왼쪽 정렬) - 개선된 클릭 감지
        Row(
            modifier = Modifier
                .clickable(
                    enabled = selectedPeriod != "전체",
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Material3에서는 기본 ripple을 사용하거나 null로 설정
                ) {
                    if (selectedPeriod != "전체") {
                        onRangeSelected(selectedRange)
                    }
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedRange,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (selectedPeriod != "전체") {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 상단 통계 카드들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "총 금주일",
                value = "${totalDays}일",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "성공률",
                value = "${successRate}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "시도 횟수",
                value = "${totalAttempts}회",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
