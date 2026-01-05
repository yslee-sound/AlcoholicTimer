// [NEW] 기록 화면 통계 카드 컴포넌트 (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.AutoResizingText
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager
import kr.sweetapps.alcoholictimer.util.utils.FormatUtils

/**
 * 모던 통계 그리드 (3개 독립 카드)
 */
@Composable
fun ModernStatisticsGrid(
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData
) {
    val context = LocalContext.current
    val currency = CurrencyManager.getSelectedCurrency(context)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 칼로리 카드
        StatCard(
            icon = R.drawable.personsimplerun,
            iconColor = Color(0xFFFF9F66),
            label = "CALORIES",
            value = FormatUtils.formatCompactNumber(context, statsData.totalKcal),
            unit = "kcal",
            modifier = Modifier.weight(1f).fillMaxHeight()
        )

        // 절주 카드
        StatCard(
            icon = R.drawable.wine,
            iconColor = Color(0xFF6B9DFF),
            label = "SOBER",
            value = statsData.totalBottles.toInt().toString(),
            unit = stringResource(R.string.records_unit_bottles),
            modifier = Modifier.weight(1f).fillMaxHeight()
        )

        // 저축 카드
        val savedMoneyValue = CurrencyManager.formatMoneyNoDecimals(statsData.savedMoney, context)
        val currencyUnit = currency.code

        StatCard(
            icon = R.drawable.piggybank,
            iconColor = Color(0xFF5CD88A),
            label = "SAVED",
            value = savedMoneyValue,
            unit = currencyUnit,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

/**
 * 개별 통계 카드 (세로형)
 */
@Composable
private fun StatCard(
    icon: Int,
    iconColor: Color,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 아이콘
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )

            // 라벨
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 값
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 숫자 (자동 축소)
                AutoResizingText(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center,
                    minFontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 단위 텍스트
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }
    }
}

/**
 * 총 금주일 카드 (가로형)
 */
@Composable
fun TotalDaysCard(
    totalDays: Float,
    onNavigateToAllRecords: () -> Unit = {}
) {
    val displayValue = kotlin.math.floor(totalDays * 10.0) / 10.0

    val decimalFormat = remember {
        java.text.DecimalFormat("#,###.#", java.text.DecimalFormatSymbols(java.util.Locale.US))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onNavigateToAllRecords() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 아이콘 + 제목
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.calendarblank_regular),
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = stringResource(R.string.records_total_sober_days),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 우측: 값 + 화살표
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoResizingText(
                    text = decimalFormat.format(displayValue),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.End,
                    minFontSize = 16.sp
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = "전체 기록 보기",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

