// [NEW] Tab02 리팩토링: components를 tab_02로 이동
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import kr.sweetapps.alcoholictimer.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import java.util.*
import androidx.compose.foundation.BorderStroke
import android.os.SystemClock
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation

// Local layout constants for Records screen (screen #2). Adjust here to control spacing
private val RECORDS_SELECTION_ROW_HEIGHT_LOCAL = 48.dp

@Composable
fun PeriodSelectionSection(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    onPeriodClick: (String) -> Unit,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 15.dp // Padding parameter for horizontal spacing
) {
    val periodWeek = stringResource(R.string.records_period_week)
    val periodMonth = stringResource(R.string.records_period_month)
    val periodYear = stringResource(R.string.records_period_year)
    val periodAll = stringResource(R.string.records_period_all)

    val periods = listOf(periodWeek, periodMonth, periodYear, periodAll)

    // 초간단 디바운스: 너무 빠른 연속 탭 무시
    var lastClickAt by remember { mutableStateOf(0L) }
    val debounceMs = 250L

    Column(modifier = modifier.fillMaxWidth()) {
        // 통합된 카드: 상단 기간 버튼 행 + 중앙 디바이더 + 하단 기간 선택 행
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
            border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 상단 버튼 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = RECORDS_SELECTION_ROW_HEIGHT_LOCAL)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    periods.forEach { period ->
                        val isSelected = period == selectedPeriod
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastClickAt >= debounceMs) {
                                        lastClickAt = now
                                        onPeriodSelected(period)
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF74B9FF) else Color.Transparent
                        ) {
                            Text(
                                text = period,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF636E72)
                            )
                        }
                    }
                }

                HorizontalDivider(color = colorResource(id = R.color.color_border_light), thickness = AppBorder.Hairline)

                // 하단 기간 표시/선택 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = RECORDS_SELECTION_ROW_HEIGHT_LOCAL)
                        .then(if (selectedPeriod == periodAll) Modifier else Modifier.clickable {
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastClickAt >= debounceMs) {
                                lastClickAt = now
                                onPeriodClick(selectedPeriod)
                            }
                        })
                        .padding(horizontal = horizontalPadding)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayText = getCurrentPeriodText(
                        selectedPeriod = selectedPeriod,
                        selectedDetailPeriod = selectedDetailPeriod,
                        periodWeek = periodWeek,
                        periodMonth = periodMonth,
                        periodYear = periodYear,
                        periodAll = periodAll
                    )
                    Text(
                        text = displayText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50)
                    )
                    if (selectedPeriod != periodAll) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "세부 기간 선택",
                            tint = Color(0xFF74B9FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getCurrentPeriodText(
    selectedPeriod: String,
    selectedDetailPeriod: String,
    periodWeek: String,
    periodMonth: String,
    periodYear: String,
    periodAll: String
): String {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val thisWeek = stringResource(R.string.records_this_week)

    return when (selectedPeriod) {
        periodWeek -> if (selectedDetailPeriod.isNotEmpty()) selectedDetailPeriod else thisWeek
        periodMonth -> if (selectedDetailPeriod.isNotEmpty()) selectedDetailPeriod else context.getString(
            R.string.date_format_year_month,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
        periodYear -> if (selectedDetailPeriod.isNotEmpty()) selectedDetailPeriod else context.getString(
            R.string.date_format_year,
            calendar.get(Calendar.YEAR)
        )
        else -> periodAll
    }
}
