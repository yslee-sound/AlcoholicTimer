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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun StatisticsCardsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    // 주간 범위 파싱 함수
    fun parseWeekRange(range: String): Pair<Long, Long>? {
        // 예: "7-28 ~ 08-03"
        val regex = Regex("(\\d{1,2})-(\\d{1,2}) ~ (\\d{1,2})-(\\d{1,2})")
        val match = regex.find(range)
        if (match != null) {
            val (startMonth, startDay, endMonth, endDay) = match.destructured
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, startMonth.toInt() - 1)
                set(Calendar.DAY_OF_MONTH, startDay.toInt())
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, endMonth.toInt() - 1)
                set(Calendar.DAY_OF_MONTH, endDay.toInt())
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return startCal.timeInMillis to endCal.timeInMillis
        }
        return null
    }

    val weekRange = parseWeekRange(selectedRange)
    val (weekStart, weekEnd) = weekRange ?: (null to null)

    // 실제 시간 기반으로 정확한 통계 계산 (주간 범위에 맞게)
    val filteredRecords = if (weekStart != null && weekEnd != null) {
        records.filter { it.endTime >= weekStart && it.startTime <= weekEnd }
    } else {
        records
    }

    val totalDays = filteredRecords.sumOf { record ->
        if (weekStart != null && weekEnd != null) {
            val overlapStart = max(record.startTime, weekStart)
            val overlapEnd = min(record.endTime, weekEnd)
            if (overlapStart < overlapEnd) {
                ((overlapEnd - overlapStart) / (24 * 60 * 60 * 1000f)).roundToInt()
            } else 0
        } else {
            val duration = record.endTime - record.startTime
            (duration / (24 * 60 * 60 * 1000f)).roundToInt()
        }
    }

    val totalAttempts = filteredRecords.count { record ->
        if (weekStart != null && weekEnd != null) {
            val overlapStart = max(record.startTime, weekStart)
            val overlapEnd = min(record.endTime, weekEnd)
            overlapStart < overlapEnd
        } else true
    }

    // 각 기록의 실제 달성률을 평균으로 계산 (주간 범위에 맞게)
    val successRate = if (totalAttempts > 0) {
        val totalProgressPercent = filteredRecords.sumOf { record ->
            val overlapStart = if (weekStart != null) max(record.startTime, weekStart) else record.startTime
            val overlapEnd = if (weekEnd != null) min(record.endTime, weekEnd) else record.endTime
            val actualDurationDays = if (overlapStart < overlapEnd) (overlapEnd - overlapStart) / (24 * 60 * 60 * 1000f) else 0f
            val progressPercent = if (record.targetDays > 0) {
                ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f)
            } else {
                record.percentage?.toFloat() ?: 0f
            }
            progressPercent.toDouble()
        }
        (totalProgressPercent / totalAttempts).toInt()
    } else 0

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
