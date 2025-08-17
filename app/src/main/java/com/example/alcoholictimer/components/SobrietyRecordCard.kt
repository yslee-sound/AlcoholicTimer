package com.example.alcoholictimer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SobrietyRecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }

    val startDate = dateFormatter.format(Date(record.startTime))
    val endDate = dateFormatter.format(Date(record.endTime))
    val startTime = timeFormatter.format(Date(record.startTime))
    val endTime = timeFormatter.format(Date(record.endTime))

    val duration = record.endTime - record.startTime
    val durationDays = (duration / (24 * 60 * 60 * 1000)).toInt()
    val durationHours = ((duration % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val durationMinutes = ((duration % (60 * 60 * 1000)) / (60 * 1000)).toInt()

    // 실제 시간 차이를 기반으로 달성률 계산
    val actualDurationDays = (duration / (24 * 60 * 60 * 1000f)).toFloat()
    val progressPercent = if (record.targetDays > 0) {
        ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f).toInt()
    } else {
        // 기존 percentage 값이 있으면 사용, 없으면 0
        record.percentage ?: 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (record.isCompleted) Color(0xFFE8F5E8) else Color(0xFFFFF3CD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단: 상태와 날짜
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 상태 배지와 테스트 표시를 포함하는 Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 상태 배지
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                        Surface(
                            color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = record.status,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 테스트 기록 표시
                    if (record.isTest) {
                        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                            Surface(
                                color = Color(0xFF9C27B0),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "테스트",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (startDate == endDate) startDate else "$startDate ~ $endDate",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 중앙: 주요 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 왼쪽: 달성 일수 (실제 시간 기반)
                Column {
                    Text(
                        text = "${actualDurationDays.toInt()}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "달성",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 중앙: 목표 대비 진행률 (실시간 계산)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${progressPercent}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Text(
                        text = "목표 달성률",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 오른쪽: 목표 일수
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "목표",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 하단: 상세 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "시작: $startTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "종료: $endTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (durationDays > 0 || durationHours > 0 || durationMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "지속 시간: ${durationDays}일 ${durationHours}시간 ${durationMinutes}분",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
