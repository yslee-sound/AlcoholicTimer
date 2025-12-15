// [NEW] Tab02 리팩토링: components를 tab_02로 이동
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.theme.AppAlphas
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
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
    headerIconSizeDp: Dp? = null,
    numberFontWeight: FontWeight = FontWeight.SemiBold
) {
    val colorScheme = MaterialTheme.colorScheme

    // [FIX] 첨부 사진 기준으로 색상 변경 - 보라색 제거
    val resolvedContainer = containerColor ?: Color(0xFFF3F4F6)  // [FIX] 연한 회색 배경
    val resolvedNumber = numberColor ?: Color(0xFF1F2937)  // 진한 회색 (숫자)
    val resolvedRateCompleted = rateColorCompleted ?: Color(0xFF6366F1)  // 파란색 (완료)
    val resolvedRateInProgress = rateColorInProgress ?: Color(0xFFEF4444)  // 빨간색 (진행중)
    val resolvedLabel = labelColor ?: Color(0xFF9CA3AF)  // 연한 회색 (레이블)
    val statusIncomplete = Color(0xFFEF4444)  // 빨간색 (미완료)

    val dateFormat = SimpleDateFormat(datePattern, Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val startDate = dateFormat.format(Date(record.startTime))
    val endDate = dateFormat.format(Date(record.endTime))
    val startTime = timeFormat.format(Date(record.startTime))
    val endTime = timeFormat.format(Date(record.endTime))

    val totalDurationMillis = record.endTime - record.startTime
    val totalDays = totalDurationMillis / (24.0 * 60 * 60 * 1000.0)
    val successRate = run {
        val pctFromTarget = if (record.targetDays > 0) {
            ((totalDays / record.targetDays) * 100.0).coerceIn(0.0, 100.0)
        } else null
        val pctFromRecord = record.percentage?.toDouble()
        val pctFallbackDefault = ((totalDays / 30.0) * 100.0).coerceIn(0.0, 100.0)
        (pctFromTarget ?: pctFromRecord ?: pctFallbackDefault).toFloat()
    }

    val cardPadding = if (compact) 14.dp else 20.dp
    val headerDateSize = if (compact) 14.sp else 16.sp
    val baseHeaderIconSize = if (compact) 56.dp else 72.dp
    val headerIconSize = headerIconSizeDp ?: baseHeaderIconSize
    val sectionSpacing = if (compact) 12.dp else 16.dp
    val valueSize = if (compact) 18.sp else 24.sp
    val valueSizePercent = if (compact) 18.sp else 22.sp
    val labelSize = if (compact) 11.sp else 12.sp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = resolvedContainer),  // 흰색 배경
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),  // 엘리베이션 제거
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))  // 연한 회색 테두리
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = (if (record.isCompleted) resolvedRateCompleted else statusIncomplete).copy(alpha = AppAlphas.SurfaceTint),
                    modifier = Modifier.size(headerIconSize)
                ) {
                    Icon(
                        imageVector = if (record.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = stringResource(if (record.isCompleted) R.string.record_completed else R.string.record_incomplete),
                        tint = if (record.isCompleted) resolvedRateCompleted else statusIncomplete,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (compact) 10.dp else 12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (compact) 10.dp else 12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "$startDate ~", fontSize = headerDateSize, color = resolvedNumber)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = endDate, fontSize = headerDateSize, color = resolvedNumber)  // [FIX] 회색에서 검은색으로 변경
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.record_days_format, totalDays),
                        fontSize = valueSize,
                        color = resolvedNumber,
                        fontWeight = numberFontWeight
                    )
                    Text(text = stringResource(R.string.record_actual_days), fontSize = labelSize, color = resolvedLabel)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.record_target_days_format, record.targetDays),
                        fontSize = valueSize,
                        color = resolvedNumber,
                        fontWeight = numberFontWeight
                    )
                    Text(text = stringResource(R.string.record_target_days), fontSize = labelSize, color = resolvedLabel)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.record_rate_format, successRate),
                        fontSize = valueSizePercent,
                        color = if (record.isCompleted) resolvedRateCompleted else resolvedRateInProgress,
                        fontWeight = numberFontWeight
                    )
                    Text(text = stringResource(R.string.record_success_rate), fontSize = labelSize, color = resolvedLabel)
                }
            }

            if (showProgressBar) {
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
                LinearProgressIndicator(
                    progress = { (successRate / 100f).coerceIn(0f, 1f) },
                    color = if (record.isCompleted) resolvedRateCompleted else resolvedRateInProgress,
                    trackColor = resolvedLabel.copy(alpha = AppAlphas.SurfaceTint),
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
                    Text(text = stringResource(R.string.record_start_time) + ": $startTime", fontSize = labelSize, color = timeTextColor)
                    Text(text = stringResource(R.string.record_end_time) + ": $endTime", fontSize = labelSize, color = timeTextColor)
                }
            }
        }
    }
}

// [NEW] 프리뷰: 기록 요약 카드
@androidx.compose.ui.tooling.preview.Preview(
    name = "기록 요약 카드 - 완료",
    showBackground = true
)
@Composable
fun RecordSummaryCardPreview() {
    MaterialTheme {
        val currentTime = System.currentTimeMillis()
        RecordSummaryCard(
            record = SobrietyRecord(
                id = "preview_1",
                startTime = currentTime - (3L * 24 * 60 * 60 * 1000), // 3일 전
                endTime = currentTime,
                targetDays = 7,
                actualDays = 3.0,
                isCompleted = true,
                status = "completed",
                createdAt = currentTime,
                percentage = 42
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "기록 요약 카드 - 진행중",
    showBackground = true
)
@Composable
fun RecordSummaryCardInProgressPreview() {
    MaterialTheme {
        val currentTime = System.currentTimeMillis()
        RecordSummaryCard(
            record = SobrietyRecord(
                id = "preview_2",
                startTime = currentTime - (1L * 24 * 60 * 60 * 1000), // 1일 전
                endTime = currentTime,
                targetDays = 30,
                actualDays = 1.0,
                isCompleted = false,
                status = "active",
                createdAt = currentTime,
                percentage = 3
            )
        )
    }
}

