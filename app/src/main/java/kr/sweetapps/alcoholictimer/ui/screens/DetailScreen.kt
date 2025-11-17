package kr.sweetapps.alcoholictimer.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.theme.AmberSecondaryLight
import kr.sweetapps.alcoholictimer.core.ui.theme.BluePrimaryLight
import kr.sweetapps.alcoholictimer.core.ui.predictAnchoredBannerHeightDp
import kr.sweetapps.alcoholictimer.feature.detail.components.DetailStatCard
import kr.sweetapps.alcoholictimer.core.util.FormatUtils

@Composable
fun DetailScreen(
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int,
    isCompleted: Boolean,
    onBack: () -> Unit,
    onDelete: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val showDeleteDialog = remember { mutableStateOf(false) }
    val accentColor = if (isCompleted) BluePrimaryLight else AmberSecondaryLight

    var shouldHideBanner by remember { mutableStateOf(false) } // DebugAdHelper removed: banner is always shown (supabase remote control handles visibility)

    val dateTimeFormat = remember {
        SimpleDateFormat(
            when (Locale.getDefault().language) {
                "ko" -> "yyyy-MM-dd - a h:mm"
                "ja" -> "yyyy年MM月dd日 - H:mm"
                else -> "yyyy-MM-dd - h:mm a"
            }, Locale.getDefault()
        ).apply { timeZone = TimeZone.getDefault() }
    }
    val displayDateTime = remember(startTime) {
        if (startTime > 0) dateTimeFormat.format(Date(startTime)) else {
            val nowFmt = SimpleDateFormat(
                when (Locale.getDefault().language) {
                    "ko" -> "a h:mm"
                    "ja" -> "H:mm"
                    else -> "h:mm a"
                }, Locale.getDefault()
            ).apply { timeZone = TimeZone.getDefault() }.format(Date())
            context.getString(R.string.detail_today_time, nowFmt)
        }
    }

    val totalDurationMillis = if (startTime > 0) endTime - startTime else actualDays * kr.sweetapps.alcoholictimer.constants.Constants.DAY_IN_MILLIS
    val totalHours = totalDurationMillis / (60 * 60 * 1000.0)
    val totalDays = totalHours / 24.0

    val (selectedCost, selectedFrequency, selectedDuration) = kr.sweetapps.alcoholictimer.constants.Constants.getUserSettings(context)
    val costVal = kr.sweetapps.alcoholictimer.constants.Constants.DrinkingSettings.getCostValue(selectedCost)
    val freqVal = kr.sweetapps.alcoholictimer.constants.Constants.DrinkingSettings.getFrequencyValue(selectedFrequency)
    val drinkHoursVal = kr.sweetapps.alcoholictimer.constants.Constants.DrinkingSettings.getDurationValue(selectedDuration)
    val hangoverHoursVal = kr.sweetapps.alcoholictimer.constants.Constants.DrinkingSettings.HANGOVER_HOURS

    val exactWeeks = totalHours / (24.0 * 7.0)
    val savedMoney = (exactWeeks * freqVal * costVal).roundToInt()
    val savedHoursExact = (exactWeeks * freqVal * (drinkHoursVal + hangoverHoursVal))
    val achievementRate = ((totalDays / targetDays) * 100.0).coerceAtMost(100.0)
    val lifeExpectancyIncrease = totalDays / 30.0

    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = density.fontScale * 0.9f)) {
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val effectiveBottom = maxOf(navBottom, imeBottom)

        Scaffold(
            topBar = { kr.sweetapps.alcoholictimer.core.ui.BackTopBar(title = stringResource(id = R.string.detail_title), onBack = onBack, trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.dialog_delete_title),
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier.size(24.dp).clickable { showDeleteDialog.value = true }
                )
            }) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                // Scrollable content: use a scrollable Column without forcing it to fill remaining
                // space (remove weight). This prevents the content area from stretching and
                // producing a large empty background gap above the bottom banner/navigation.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "${stringResource(id = R.string.detail_start_label)} $displayDateTime",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF718096)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stringResource(id = R.string.detail_end_label)} ${dateTimeFormat.format(Date(endTime))}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF718096)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f", totalDays),
                                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = accentColor
                                    )
                                    Text(
                                        text = stringResource(id = R.string.unit_day),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF718096)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.detail_progress_rate),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF718096)
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f%%", achievementRate),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (achievementRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = accentColor,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = stringResource(id = R.string.detail_progress_current, String.format(Locale.getDefault(), "%.1f", totalDays)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF718096)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.detail_progress_target, targetDays.toInt()),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF718096)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiConstants.STAT_ROW_SPACING)
                    ) {
                        DetailStatCard(
                            value = stringResource(R.string.unit_days_format, totalDays),
                            label = stringResource(id = R.string.stat_total_days),
                            modifier = Modifier.weight(1f),
                            valueColor = colorResource(id = R.color.color_indicator_days)
                        )
                        DetailStatCard(
                            value = FormatUtils.formatMoney(context, savedMoney.toDouble()),
                            label = stringResource(id = R.string.stat_saved_money_short),
                            modifier = Modifier.weight(1f),
                            valueColor = colorResource(id = R.color.color_indicator_money)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiConstants.STAT_ROW_SPACING)
                    ) {
                        DetailStatCard(
                            value = FormatUtils.formatHoursWithUnit(context, savedHoursExact),
                            label = stringResource(id = R.string.stat_saved_hours_short),
                            modifier = Modifier.weight(1f),
                            valueColor = colorResource(id = R.color.color_indicator_hours)
                        )
                        DetailStatCard(
                            value = FormatUtils.daysToDayHourString(context, lifeExpectancyIncrease, 2),
                            label = stringResource(id = R.string.indicator_title_life_gain),
                            modifier = Modifier.weight(1f),
                            valueColor = colorResource(id = R.color.color_indicator_life)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (!shouldHideBanner) {
                    if (UiConstants.BANNER_TOP_GAP > 0.dp) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(UiConstants.BANNER_TOP_GAP).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = effectiveBottom)
                            .height(predictAnchoredBannerHeightDp()),
                        contentAlignment = Alignment.Center
                    ) { /* AdmobBanner centralized - no-op */ }
                }
            }
        }
        if (showDeleteDialog.value) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog.value = false },
                title = {
                    Text(text = stringResource(id = R.string.dialog_delete_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                text = { Text(text = stringResource(id = R.string.dialog_delete_message), style = MaterialTheme.typography.bodyLarge) },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(startTime, endTime)
                        // no need to set showDeleteDialog false before navigating back
                        onBack()
                    }) {
                        Text(text = stringResource(id = R.string.dialog_delete_confirm), color = Color(0xFFE53E3E), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = false }) {
                        Text(text = stringResource(id = R.string.dialog_cancel), color = Color(0xFF718096))
                    }
                }
            )
        }
    }
}
