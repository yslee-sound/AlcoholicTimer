// [NEW] 기록 화면 헤더 컴포넌트 (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R

/**
 * 모던 대시보드 스타일 헤더
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDashboardHeader() {
    TopAppBar(
        title = {
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF111111))) {
                        append(stringResource(R.string.records_dashboard_title_health))
                    }
                    append(" ")
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF6366F1))) {
                        append(stringResource(R.string.records_dashboard_title_analysis))
                    }
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF111111)
        ),
        modifier = Modifier.height(48.dp),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

/**
 * 통계 헤더와 필터 통합 (한 줄)
 */
@Composable
fun StatisticsHeaderWithFilter(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val monthlyText = stringResource(R.string.records_period_filter_monthly)
    val allText = stringResource(R.string.records_period_filter_all)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.records_statistics_summary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            fontSize = 18.sp
        )

        Row(
            modifier = Modifier
                .background(Color(0xFFF1F3F5), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleButton(
                text = monthlyText,
                isSelected = selectedPeriod.contains("월"),
                onClick = { onPeriodSelected("월") }
            )

            ToggleButton(
                text = allText,
                isSelected = !selectedPeriod.contains("월"),
                onClick = { onPeriodSelected("전체") }
            )
        }
    }
}

/**
 * 토글 버튼 (캡슐 모양)
 */
@Composable
fun ToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .defaultMinSize(minWidth = 70.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible
        )
    }
}

