package com.sweetapps.alcoholictimer.feature.records.components

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
import com.sweetapps.alcoholictimer.R
import com.sweetapps.alcoholictimer.core.model.SobrietyRecord
import com.sweetapps.alcoholictimer.core.ui.AppAlphas
import com.sweetapps.alcoholictimer.core.ui.AppElevation
import com.sweetapps.alcoholictimer.core.ui.AppBorder
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
        colors = CardDefaults.cardColors(containerColor = resolvedContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
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
                    Text(text = endDate, fontSize = headerDateSize, color = resolvedLabel)
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
