package com.example.alcoholictimer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordSummaryCard(
    record: SobrietyRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    containerColor: Color = Color.White,
    showTimeRow: Boolean = false,
    datePattern: String = "yyyy.MM.dd",
    numberColor: Color = Color(0xFF2C3E50),
    rateColorCompleted: Color = Color(0xFF00B894),
    rateColorInProgress: Color = Color(0xFF74B9FF)
) {
    val dateFormat = SimpleDateFormat(datePattern, Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val startDate = dateFormat.format(Date(record.startTime))
    val endDate = dateFormat.format(Date(record.endTime))
    val startTime = timeFormat.format(Date(record.startTime))
    val endTime = timeFormat.format(Date(record.endTime))

    // 실제 시간 차이를 기반으로 값 계산
    val totalDurationMillis = record.endTime - record.startTime
    val totalDays = totalDurationMillis / (24.0 * 60 * 60 * 1000.0)
    val successRate = if (record.targetDays > 0) {
        ((totalDays / record.targetDays) * 100.0).let { rate ->
            if (rate > 100) 100.0 else rate
        }.toFloat()
    } else {
        record.percentage?.toFloat() ?: 0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 헤더: 날짜/상태
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$startDate ~ $endDate",
                        fontSize = 16.sp,
                        color = numberColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.status,
                        fontSize = 12.sp,
                        color = if (record.isCompleted) rateColorCompleted else Color(0xFFE17055)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (record.isCompleted)
                        rateColorCompleted.copy(alpha = 0.1f)
                    else
                        Color(0xFFE17055).copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (record.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = if (record.isCompleted) "완료" else "미완료",
                        tint = if (record.isCompleted) rateColorCompleted else Color(0xFFE17055),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 메인 정보 - 숫자 3열
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 달성 일수
                Column {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f일", totalDays),
                        fontSize = 28.sp,
                        color = numberColor
                    )
                    Text(text = "달성 일수", fontSize = 12.sp, color = Color(0xFF636E72))
                }

                // 목표 일수
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = 28.sp,
                        color = numberColor
                    )
                    Text(text = "목표 일수", fontSize = 12.sp, color = Color(0xFF636E72))
                }

                // 달성률
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", successRate),
                        fontSize = 26.sp,
                        color = if (record.isCompleted) rateColorCompleted else rateColorInProgress
                    )
                    Text(text = "달성률", fontSize = 12.sp, color = Color(0xFF636E72))
                }
            }

            if (showTimeRow) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "시작: $startTime", fontSize = 12.sp, color = Color(0xFF999999))
                    Text(text = "종료: $endTime", fontSize = 12.sp, color = Color(0xFF999999))
                }
            }
        }
    }
}

