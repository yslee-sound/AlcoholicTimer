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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    containerColor: Color? = null,
    showTimeRow: Boolean = false,
    datePattern: String = "yyyy.MM.dd",
    numberColor: Color? = null,
    rateColorCompleted: Color? = null,
    rateColorInProgress: Color? = null,
    labelColor: Color? = null,
    compact: Boolean = true,
    showProgressBar: Boolean = true,
    headerIconSizeDp: Dp? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    val resolvedContainer = containerColor ?: colorScheme.surface
    val resolvedNumber = numberColor ?: colorScheme.onSurface
    val resolvedRateCompleted = rateColorCompleted ?: colorScheme.primary
    val resolvedRateInProgress = rateColorInProgress ?: colorScheme.secondary
    val resolvedLabel = labelColor ?: colorScheme.onSurfaceVariant
    val statusIncomplete = colorScheme.error

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

    // 컴팩트/일반 모드별 타이포/여백 프리셋
    val cardPadding = if (compact) 14.dp else 20.dp
    val headerDateSize = if (compact) 14.sp else 16.sp
    val baseHeaderIconSize = if (compact) 56.dp else 72.dp
    val headerIconSize = headerIconSizeDp ?: baseHeaderIconSize
    val sectionSpacing = if (compact) 12.dp else 16.dp
    val valueSize = if (compact) 18.sp else 28.sp
    val valueSizePercent = if (compact) 18.sp else 26.sp
    val labelSize = if (compact) 11.sp else 12.sp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = resolvedContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            // 헤더: 아이콘(왼쪽) + 날짜(오른쪽)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (record.isCompleted) resolvedRateCompleted else statusIncomplete).copy(alpha = 0.12f),
                    modifier = Modifier.size(headerIconSize)
                ) {
                    Icon(
                        imageVector = if (record.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = if (record.isCompleted) "완료" else "미완료",
                        tint = if (record.isCompleted) resolvedRateCompleted else statusIncomplete,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (compact) 10.dp else 12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$startDate ~",
                        fontSize = headerDateSize,
                        color = resolvedNumber
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = endDate,
                        fontSize = headerDateSize,
                        color = resolvedLabel
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            // 메인 정보 - 숫자 3열 (컴팩트 타이포)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 달성 일수
                Column {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f일", totalDays),
                        fontSize = valueSize,
                        color = resolvedNumber
                    )
                    Text(text = "달성 일수", fontSize = labelSize, color = resolvedLabel)
                }

                // 목표 일수
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = valueSize,
                        color = resolvedNumber
                    )
                    Text(text = "목표 일수", fontSize = labelSize, color = resolvedLabel)
                }

                // 달성률
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", successRate),
                        fontSize = valueSizePercent,
                        color = if (record.isCompleted) resolvedRateCompleted else resolvedRateInProgress
                    )
                    Text(text = "달성률", fontSize = labelSize, color = resolvedLabel)
                }
            }

            if (showProgressBar) {
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
                LinearProgressIndicator(
                    progress = { (successRate / 100f).coerceIn(0f, 1f) },
                    color = if (record.isCompleted) resolvedRateCompleted else resolvedRateInProgress,
                    trackColor = resolvedLabel.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            if (showTimeRow) {
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val timeTextColor = resolvedLabel
                    Text(text = "시작: $startTime", fontSize = labelSize, color = timeTextColor)
                    Text(text = "종료: $endTime", fontSize = labelSize, color = timeTextColor)
                }
            }
        }
    }
}
