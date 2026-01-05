// [NEW] 기록 화면 통계 섹션 컴포넌트 (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager
import kr.sweetapps.alcoholictimer.util.utils.FormatUtils

// [MOVED] RecordsScreen.kt의 상수들
private val RECORDS_STATS_INTERNAL_TOP_GAP = 10.dp
private val RECORDS_STATS_ROW_SPACING = 10.dp
private val RECORDS_CARD_IN_ROW_SPACING = 10.dp

/**
 * 기간별 통계 섹션 (헤더 + 통계 카드)
 */
@Composable
fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    weekRange: Pair<Long, Long>? = null,
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData,
    onNavigateToAllRecords: () -> Unit = {},
    onAddRecord: () -> Unit = {}
) {
    Column(modifier = modifier) {
        // 헤더
        PeriodHeaderRow(
            selectedPeriod = selectedPeriod,
            onNavigateToAllRecords = onNavigateToAllRecords
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 통계 카드
        PeriodStatisticsCard(
            records = records,
            statsData = statsData
        )
    }
}

/**
 * 통계 섹션 헤더 (제목 + 전체보기 버튼)
 */
@Composable
private fun PeriodHeaderRow(
    selectedPeriod: String,
    onNavigateToAllRecords: () -> Unit
) {
    val context = LocalContext.current

    val title = when {
        selectedPeriod.contains("주", ignoreCase = true) ||
        selectedPeriod.contains("Week", ignoreCase = true) ->
            context.getString(R.string.records_weekly_stats)

        selectedPeriod.contains("월", ignoreCase = true) ||
        selectedPeriod.contains("Month", ignoreCase = true) ->
            context.getString(R.string.records_monthly_stats)

        selectedPeriod.contains("년", ignoreCase = true) ||
        selectedPeriod.contains("Year", ignoreCase = true) ->
            context.getString(R.string.records_yearly_stats)

        else -> context.getString(R.string.records_all_stats)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_caret_right),
            contentDescription = stringResource(R.string.records_view_all_icon_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNavigateToAllRecords
                )
        )
    }
}

/**
 * 통계 카드 (금주 일수, 절약 금액, 칼로리, 술병 수)
 */
@Composable
private fun PeriodStatisticsCard(
    records: List<SobrietyRecord>,
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData
) {
    val context = LocalContext.current
    val userSettings by rememberUserSettingsState(context)

    val totalDays = statsData.totalDays
    val savedMoney = statsData.savedMoney
    val totalKcal = statsData.totalKcal
    val totalBottles = statsData.totalBottles

    val savedMoneyFormatted = remember(savedMoney, userSettings.currencyCode) {
        val formatted = CurrencyManager.formatMoneyNoDecimals(savedMoney, context)
        val currencyCode = CurrencyManager.getSelectedCurrency(context).code
        "$formatted $currencyCode"
    }

    val daysText = remember(totalDays) {
        val displayValue = kotlin.math.floor(totalDays * 10.0) / 10.0
        String.format(java.util.Locale.getDefault(), "%.1f", displayValue)
    }

    val kcalText = remember(totalKcal) {
        FormatUtils.formatCompactNumber(context, totalKcal)
    }

    val bottlesText = remember(totalBottles) {
        String.format(java.util.Locale.getDefault(), "%.1f", totalBottles)
    }

    val bottlesUnit = remember(totalBottles) {
        val count = totalBottles.toInt()
        if (count == 1) "bottle" else "bottles"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(RECORDS_STATS_INTERNAL_TOP_GAP))

            // 3개의 통계 칩 (칼로리, 술병, 저축)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(RECORDS_CARD_IN_ROW_SPACING)
            ) {
                val statsScale = 1.3f

                StatisticItem(
                    title = "CALORIES",
                    value = "$kcalText ${stringResource(R.string.stats_unit_kcal)}",
                    color = MaterialTheme.colorScheme.tertiary,
                    valueColor = Color(0xFF111111),
                    icon = R.drawable.personsimplerun,
                    iconTint = Color(0xFF666666),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    titleScale = statsScale,
                    valueScale = statsScale
                )

                StatisticItem(
                    title = "SOBER",
                    value = "$bottlesText $bottlesUnit",
                    color = MaterialTheme.colorScheme.primary,
                    valueColor = Color(0xFF111111),
                    icon = R.drawable.wine,
                    iconTint = Color(0xFF666666),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    titleScale = statsScale,
                    valueScale = statsScale
                )

                StatisticItem(
                    title = "SAVED",
                    value = savedMoneyFormatted,
                    color = MaterialTheme.colorScheme.error,
                    valueColor = Color(0xFF111111),
                    icon = R.drawable.piggybank,
                    iconTint = Color(0xFF666666),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    titleScale = statsScale,
                    valueScale = statsScale
                )
            }

            Spacer(modifier = Modifier.height(RECORDS_STATS_ROW_SPACING))

            // 총 금주 일수
            val dayUnit = stringResource(R.string.records_day_unit)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_total_days),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF666666)
                )

                Box(
                    modifier = Modifier
                        .background(Color(0xFFF0F0F0), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$daysText$dayUnit",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111)
                        )
                    )
                }
            }
        }
    }
}

/**
 * 개별 통계 아이템 (칼로리, 술병, 저축 각각)
 */
@Composable
private fun StatisticItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    titleScale: Float = 1.0f,
    valueScale: Float = 1.0f,
    valueColor: Color = Color(0xFF111111),
    icon: Any? = null,
    iconTint: Color = Color(0xFF666666)
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color(0xFFF8F8F8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null && icon is Int) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11.sp.value * titleScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF666666),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            AutoResizeSingleLineText(
                text = value,
                baseStyle = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (16.sp.value * valueScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 자동 크기 조절 텍스트 (텍스트가 넘칠 경우 폰트 크기 자동 축소)
 */
@Composable
private fun AutoResizeSingleLineText(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    step: Float = 0.9f,
    minFontSize: Float = 10f,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val textMeasurer = rememberTextMeasurer()

        val optimalFontSize = remember(text, baseStyle, maxWidthPx, step, minFontSize) {
            var currentSize = baseStyle.fontSize.value

            while (currentSize > minFontSize) {
                val testStyle = baseStyle.copy(fontSize = currentSize.sp)
                val measured = textMeasurer.measure(
                    text = text,
                    style = testStyle,
                    maxLines = 1
                )

                if (measured.size.width <= maxWidthPx) {
                    break
                }
                currentSize *= step
            }

            currentSize.coerceAtLeast(minFontSize)
        }

        val finalStyle = baseStyle.copy(
            fontSize = optimalFontSize.sp,
            lineHeight = (optimalFontSize * 1.1f).sp
        )

        Text(
            text = text,
            style = finalStyle,
            color = color ?: finalStyle.color,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

