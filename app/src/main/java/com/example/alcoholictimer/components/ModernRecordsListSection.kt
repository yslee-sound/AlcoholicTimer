package com.example.alcoholictimer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernRecordsListSection(
    records: List<SobrietyRecord>,
    onRecordClick: (SobrietyRecord) -> Unit,
    onAddTestRecord: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "최근 활동",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                // 테스트 기록 추가 버튼
                Card(
                    onClick = onAddTestRecord,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1976D2)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "추가",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "테스트 기록",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 기록 리스트
            if (records.isEmpty()) {
                // 빈 상태
                ModernEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp), // 고정 높이로 스크롤 가능
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records) { record ->
                        ModernSobrietyRecordCard(
                            record = record,
                            onClick = { onRecordClick(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📝",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "아직 금주 기록이 없습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "첫 번째 금주를 시작해보세요!",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSobrietyRecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("MM.dd", Locale.getDefault()).apply {
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
    val actualDurationDays = (duration / (24 * 60 * 60 * 1000f)).toFloat()
    val progressPercent = if (record.targetDays > 0) {
        ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f).toInt()
    } else {
        record.percentage ?: 0
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isCompleted) {
                Color(0xFFF1F8E9)
            } else {
                Color(0xFFFFF8E1)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 상태 배지
                    Surface(
                        color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = record.status,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 테스트 기록 표시
                    if (record.isTest) {
                        Surface(
                            color = Color(0xFF9C27B0),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "테스트",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (startDate == endDate) startDate else "$startDate~$endDate",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 메인 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 달성 일수
                Column {
                    Text(
                        text = "${actualDurationDays.toInt()}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = "달성",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }

                // 진행률 표시
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 원형 진행률
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(60.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            strokeWidth = 4.dp,
                            trackColor = Color(0xFFE0E0E0),
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "${progressPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                }

                // 목표 일수
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "목표",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 하단: 시간 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "시작 $startTime",
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
                Text(
                    text = "종료 $endTime",
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}
