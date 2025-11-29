@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_IMPORT", "UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.core.data.RecordsDataLoader
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.feature.records.components.MonthPickerBottomSheet
import kr.sweetapps.alcoholictimer.feature.records.components.PeriodSelectionSection
import kr.sweetapps.alcoholictimer.feature.records.components.RecordSummaryCard
import kr.sweetapps.alcoholictimer.feature.records.components.WeekPickerBottomSheet
import kr.sweetapps.alcoholictimer.feature.records.components.YearPickerBottomSheet
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kr.sweetapps.alcoholictimer.core.util.PercentUtils
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale

// Records screen constants (migrated from UiConstants)
val RECORDS_SCREEN_HORIZONTAL_PADDING: Dp = 15.dp // 15
// header specific left/start padding (controls only the start of the "월 통계" title)
// separate header horizontal padding so title start can be adjusted independently
val RECORDS_STATS_INTERNAL_TOP_GAP: Dp = 12.dp // 12
val RECORDS_STATS_ROW_SPACING: Dp = 12.dp // 12, 3칩 하단
val RECORDS_CARD_IN_ROW_SPACING: Dp = 12.dp // 12, 3칩 사이 공간
val RECORDS_SELECTION_ROW_HEIGHT: Dp = 56.dp // 56

// Local small overrides used only inside this file
val RECORDS_HEADER_START_PADDING: Dp = 17.dp // 15 + 2, 월 통계 왼쪽
val RECORDS_TOP_SECTION_EXTERNAL_GAP: Dp = 15.dp // 15, 월 통계 상단 패딩
private val RECORDS_HEADER_TO_CARD_GAP = 0.dp  // removed gap between selection card and header
private val RECORDS_CARD_INTERNAL_TOP_PADDING = 8.dp // 8, 3칩 그룹 내부 상단
// Local elevation for the monthly statistics card (override the value below to adjust only this card).
// Set it to 0.dp / 2.dp / 4.dp according to design tokens (0 = flat, 2 = normal card, 4 = high emphasis).
val RECORDS_STATS_CARD_ELEVATION: Dp = 2.dp // <- change this number in this file to control this card's elevation
// Local bottom padding used for the Records screen list content (controls the space under the last item).
// Change this value here to adjust the visible gap under the "View all records" button.
val RECORDS_LIST_BOTTOM_PADDING: Dp = 15.dp // default: 15.dp (was UiConstants.CARD_VERTICAL_SPACING)
// use RECORDS_TOP_SECTION_EXTERNAL_GAP to control top spacing

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun RecordsScreen(
    externalRefreshTrigger: Int,
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onAddRecord: () -> Unit = {},
    fontScale: Float = 1.06f
) {
    // view_records 이벤트는 하단 네비게이션 버튼 클릭에서 전송하도록 변경됨.

    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentDate = Calendar.getInstance()
    val currentYear = currentDate.get(Calendar.YEAR)
    val currentMonth = currentDate.get(Calendar.MONTH) + 1

    // 기간 리소스 문자열
    val periodWeek = stringResource(R.string.records_period_week)
    val periodMonth = stringResource(R.string.records_period_month)
    val periodYear = stringResource(R.string.records_period_year)

    // 초기 날짜 포맷 문자열
    val initialDateText = stringResource(R.string.date_format_year_month, currentYear, currentMonth)

    var selectedPeriod by remember { mutableStateOf(periodMonth) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDetailPeriod by remember { mutableStateOf(initialDateText) }
    var selectedWeekRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    val loadRecords = {
        isLoading = true
        try {
            val loadedRecords = RecordsDataLoader.loadSobrietyRecords(context)
            records = loadedRecords
            Log.d("RecordsScreen", "기록 로딩 완료: ${loadedRecords.size}개")
        } catch (e: Exception) {
            Log.e("RecordsScreen", "기록 로딩 실패", e)
        } finally {
            isLoading = false
        }
    }

    val filteredRecords = remember(records, selectedPeriod, selectedDetailPeriod, selectedWeekRange) {
        when (selectedPeriod) {
            periodWeek -> {
                val range = selectedWeekRange ?: run {
                    val cal = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.SUNDAY
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    }
                    val weekStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_WEEK, 6)
                    val weekEndInclusive = cal.timeInMillis + (24 * 60 * 60 * 1000L - 1)
                    weekStart to weekEndInclusive
                }
                records.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            periodMonth -> {
                val range: Pair<Long, Long> = if (selectedDetailPeriod.isNotEmpty()) {
                    val numbers = Regex("(\\d+)").findAll(selectedDetailPeriod).map { it.value.toInt() }.toList()
                    if (numbers.size >= 2) {
                        val year = numbers[0]
                        val month = numbers[1] - 1
                        val cal = Calendar.getInstance()
                        cal.set(year, month, 1, 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val monthStart = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        cal.add(Calendar.MILLISECOND, -1)
                        val monthEnd = cal.timeInMillis
                        monthStart to monthEnd
                    } else {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val monthStart = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        cal.add(Calendar.MILLISECOND, -1)
                        val monthEnd = cal.timeInMillis
                        monthStart to monthEnd
                    }
                } else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val monthStart = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val monthEnd = cal.timeInMillis
                    monthStart to monthEnd
                }
                records.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            periodYear -> {
                val range: Pair<Long, Long> = if (selectedDetailPeriod.isNotEmpty()) {
                    val yearMatch = Regex("(\\d{4})").find(selectedDetailPeriod)
                    if (yearMatch != null) {
                        val year = yearMatch.groupValues[1].toInt()
                        val cal = Calendar.getInstance()
                        cal.set(year, 0, 1, 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val yearStart = cal.timeInMillis
                        cal.add(Calendar.YEAR, 1)
                        cal.add(Calendar.MILLISECOND, -1)
                        val yearEnd = cal.timeInMillis
                        yearStart to yearEnd
                    } else {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.MONTH, 0)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val yearStart = cal.timeInMillis
                        cal.add(Calendar.YEAR, 1)
                        cal.add(Calendar.MILLISECOND, -1)
                        val yearEnd = cal.timeInMillis
                        yearStart to yearEnd
                    }
                } else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.MONTH, 0)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val yearStart = cal.timeInMillis
                    cal.add(Calendar.YEAR, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val yearEnd = cal.timeInMillis
                    yearStart to yearEnd
                }
                records.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            else -> records
        }
    }

    LaunchedEffect(externalRefreshTrigger) { loadRecords() }

    CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, fontScale = LocalDensity.current.fontScale * fontScale),
        LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)
    ) {
        val safePadding = LocalSafeContentPadding.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEEEDE9))
        ) {
            // Overlay: match StartScreen / RunScreen subtle top highlight and bottom darkening
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.88f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.12f)
                    )
                )
            )

            val layoutDirection = LocalLayoutDirection.current
            val recordsContentPadding = PaddingValues(
                start = safePadding.calculateLeftPadding(layoutDirection),
                top = safePadding.calculateTopPadding(),
                end = safePadding.calculateRightPadding(layoutDirection),
                bottom = RECORDS_LIST_BOTTOM_PADDING
            )
            Log.d("RecordsScreenDebug", "Records bottom padding replaced with CARD_VERTICAL_SPACING=${UiConstants.CARD_VERTICAL_SPACING}")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = recordsContentPadding,
                verticalArrangement = Arrangement.spacedBy(UiConstants.CARD_VERTICAL_SPACING)
            ) {
                // Combined top section + monthly stats as a single item to avoid extra spacing
                item {
                    Spacer(modifier = Modifier.height(RECORDS_TOP_SECTION_EXTERNAL_GAP))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        PeriodSelectionSection(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { period: String ->
                                selectedPeriod = period
                                selectedDetailPeriod = ""
                                if (period == periodWeek) selectedWeekRange = null
                                // Analytics: 사용자 통계 뷰 변경 이벤트 전송
                                try {
                                    val viewType = when (period) {
                                        periodWeek -> "Week"
                                        periodMonth -> "Month"
                                        periodYear -> "Year"
                                        else -> "All"
                                    }
                                    val currentLevel = records.maxOfOrNull { it.achievedLevel } ?: 0
                                    AnalyticsManager.logChangeRecordView(viewType, currentLevel)
                                } catch (_: Throwable) {}
                            },
                            onPeriodClick = { _ -> showBottomSheet = true },
                            selectedDetailPeriod = selectedDetailPeriod,
                            horizontalPadding = RECORDS_SCREEN_HORIZONTAL_PADDING
                        )
                    }

                    // header: allow different left padding (same visual grouping, no inter-item spacing)
                    Box(modifier = Modifier.fillMaxWidth().padding(start = RECORDS_HEADER_START_PADDING, end = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        PeriodHeaderRow(onAddRecord = onAddRecord)
                    }
                    Spacer(modifier = Modifier.height(RECORDS_HEADER_TO_CARD_GAP))

                    // card: use the standard horizontal padding
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        PeriodStatisticsSection(
                            records = records,
                            selectedPeriod = selectedPeriod,
                            selectedDetailPeriod = selectedDetailPeriod,
                            modifier = Modifier.fillMaxWidth(),
                            weekRange = selectedWeekRange,
                            onAddRecord = { onAddRecord() }
                        )
                    }
                }

                // 기록이 없을 때 메시지 표시
                if (records.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.records_no_records),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 기록 항목: 어떤 기간을 선택하든 전체 기록 목록은 항상 표시합니다.
                items(records) { record ->
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        RecordSummaryCard(
                            record = record,
                            compact = false,
                            headerIconSizeDp = 56.dp,
                            onClick = { onNavigateToDetail(record) }
                        )
                    }
                }

                // '모든 기록 보기' 버튼 (기록이 있을 때만)
                if (records.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                            val viewAllText = stringResource(R.string.records_view_all, records.size)
                            Button(
                                onClick = { onNavigateToAllRecords() },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = viewAllText, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }

                // ... no terminal spacer here; bottom spacing is controlled by recordsContentPadding (RECORDS_LIST_BOTTOM_PADDING)
            }

            // 로딩 중일 때 스켈레톤 표시
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // 바텀 시트: 선택된 기간에 따라 각각 다른 피커를 보여줍니다.
    if (showBottomSheet) {
        when (selectedPeriod) {
            periodWeek -> {
                WeekPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onWeekPicked = { weekStart, weekEnd, displayText ->
                        selectedDetailPeriod = displayText
                        selectedWeekRange = weekStart to weekEnd
                        showBottomSheet = false
                    }
                )
            }
            periodMonth -> {
                MonthPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onMonthPicked = { year, month ->
                        selectedDetailPeriod = context.getString(R.string.date_format_year_month, year, month)
                        showBottomSheet = false
                    },
                    records = records,
                    onYearPicked = { year ->
                        selectedPeriod = periodYear
                        selectedDetailPeriod = context.getString(R.string.date_format_year, year)
                        showBottomSheet = false
                    }
                )
            }
            periodYear -> {
                val initialYearForPicker = Regex("(\\d{4})").find(selectedDetailPeriod)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Calendar.getInstance().get(Calendar.YEAR)

                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onYearPicked = { year ->
                        selectedDetailPeriod = context.getString(R.string.date_format_year, year)
                        showBottomSheet = false
                    },
                    records = records,
                    initialYear = initialYearForPicker
                )
            }
        }
    }
}

@Preview
@Composable
fun RecordsScreenPreview() {
    AlcoholicTimerTheme {
        Surface {
            RecordsScreen(
                externalRefreshTrigger = 0
            )
        }
    }
}

@Composable
private fun PeriodHeaderRow(onAddRecord: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.records_monthly_stats),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onAddRecord) {
            Image(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = stringResource(R.string.records_add),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    weekRange: Pair<Long, Long>? = null,
    onAddRecord: () -> Unit = {}
) {
    val periodWeek = stringResource(R.string.records_period_week)
    val periodMonth = stringResource(R.string.records_period_month)
    val periodYear = stringResource(R.string.records_period_year)

    val totalRecords = records.size
    val periodRange: Pair<Long, Long>? = remember(selectedPeriod, selectedDetailPeriod, weekRange) {
        when (selectedPeriod) {
            periodWeek -> {
                weekRange ?: run {
                    val cal = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.SUNDAY
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    }
                    val weekStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_WEEK, 6)
                    val weekEndInclusive = cal.timeInMillis + (24 * 60 * 60 * 1000L - 1)
                    weekStart to weekEndInclusive
                }
            }
            periodMonth -> {
                val regex = Regex("(\\d{4})년 (\\d{1,2})월")
                val match = regex.find(selectedDetailPeriod)
                if (match != null) {
                    val year = match.groupValues[1].toInt()
                    val month = match.groupValues[2].toInt() - 1
                    val cal = Calendar.getInstance()
                    cal.set(year, month, 1, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val end = cal.timeInMillis
                    start to end
                } else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val end = cal.timeInMillis
                    start to end
                }
            }
            periodYear -> {
                val regex = Regex("(\\d{4})년")
                val match = regex.find(selectedDetailPeriod)
                if (match != null) {
                    val year = match.groupValues[1].toInt()
                    val cal = Calendar.getInstance()
                    cal.set(year, 0, 1, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.add(Calendar.YEAR, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val end = cal.timeInMillis
                    start to end
                } else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.MONTH, 0)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.add(Calendar.YEAR, 1)
                    cal.add(Calendar.MILLISECOND, -1)
                    val end = cal.timeInMillis
                    start to end
                }
            }
            else -> null
        }
    }

    fun overlappedDays(record: SobrietyRecord): Double {
        return if (periodRange == null) {
            kr.sweetapps.alcoholictimer.core.util.DateOverlapUtils.overlapDays(record.startTime, record.endTime, null, null)
        } else {
            kr.sweetapps.alcoholictimer.core.util.DateOverlapUtils.overlapDays(record.startTime, record.endTime, periodRange.first, periodRange.second)
        }
    }

    val successRate = if (totalRecords > 0) {
        if (selectedPeriod == periodWeek && periodRange != null) {
            val (periodStart, periodEnd) = periodRange
            val dayMillis = 24 * 60 * 60 * 1000.0
            val intervals = records.mapNotNull { record ->
                val s = max(record.startTime.toDouble(), periodStart.toDouble())
                val e = min(record.endTime.toDouble(), periodEnd.toDouble())
                if (s < e) s to e else null
            }.sortedBy { it.first }
            var mergedMs = 0.0
            var curStart = Double.NaN
            var curEnd = Double.NaN
            for ((s, e) in intervals) {
                if (curStart.isNaN()) {
                    curStart = s; curEnd = e
                } else if (s <= curEnd) {
                    if (e > curEnd) curEnd = e
                } else {
                    mergedMs += (curEnd - curStart)
                    curStart = s; curEnd = e
                }
            }
            if (!curStart.isNaN()) {
                mergedMs += (curEnd - curStart)
            }
            val periodDays = ((periodEnd - periodStart + 1) / dayMillis)
            val unionDays = (mergedMs / dayMillis).coerceAtMost(periodDays)
            val ratio = if (periodDays > 0) (unionDays / periodDays).coerceIn(0.0, 1.0) else 0.0
            PercentUtils.roundPercent(ratio * 100.0)
        } else {
            val totalProgressPercent = records.sumOf { record ->
                val actualDurationDays = overlappedDays(record).toFloat()
                val progressPercent = if (record.targetDays > 0) {
                    ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f)
                } else {
                    record.percentage?.toFloat() ?: ((actualDurationDays / 30f) * 100).coerceIn(0f, 100f)
                }
                progressPercent.toDouble()
            }
            PercentUtils.roundPercent(totalProgressPercent / totalRecords)
        }
    } else 0

    val totalDaysDouble = records.sumOf { record -> overlappedDays(record) }
    val totalDaysDisplay = String.format(Locale.getDefault(), "%.1f", totalDaysDouble)

    val averageDaysDisplay = if (totalRecords > 0) {
        String.format(Locale.getDefault(), "%.1f", records.map { record -> overlappedDays(record) }.average())
    } else "0.0"

    val maxDaysDisplay = if (records.isNotEmpty()) {
        String.format(Locale.getDefault(), "%.1f", records.maxOf { record -> overlappedDays(record) })
    } else "0.0"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        // Card container left transparent so image inside is visible (we still use elevation/border)
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = RECORDS_STATS_CARD_ELEVATION),
        border = androidx.compose.foundation.BorderStroke(AppBorder.Hairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        // Image background (bg7) clipped to card shape, with subtle overlay to keep text legible
        Box(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)) {
            Image(
                painter = painterResource(id = R.drawable.bg11),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .scale(2.3f, 2.3f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopStart
            )

            // subtle overlay (transparent -> white(0.8)) to ensure text contrast
            val gradientEndY = with(LocalDensity.current) { 280.dp.toPx() } // 원하는 그라디언트 끝 위치 지정
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f)),
                            startY = 0.0f,
                            endY = gradientEndY
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RECORDS_CARD_INTERNAL_TOP_PADDING, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(RECORDS_STATS_INTERNAL_TOP_GAP))

                val dayUnit = stringResource(R.string.records_day_unit)
                val percentUnit = stringResource(R.string.records_percent_unit)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RECORDS_CARD_IN_ROW_SPACING)
                ) {
                    val statsScale = 1.3f

                    StatisticItem(
                        title = stringResource(R.string.records_success_rate) + "\n ",
                        value = "$successRate$percentUnit",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )
                    StatisticItem(
                        // Show as two lines: "평균\n지속일"
                        title = stringResource(R.string.records_avg_duration).replace(" ", "\n"),
                        value = "$averageDaysDisplay$dayUnit",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )
                    StatisticItem(
                        // Show as two lines: "최대\n지속일"
                        title = stringResource(R.string.records_max_duration).replace(" ", "\n"),
                        value = "$maxDaysDisplay$dayUnit",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )
                }

                Spacer(modifier = Modifier.height(RECORDS_STATS_ROW_SPACING))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.records_total_days),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 변경: 숫자 오른쪽에 어두운 '펀치아웃' 박스 추가하여 가독성 향상
                    val badgeYellow = Color(0xFFFBC528)
                    // 상단 3개 카드의 마스크와 동일한 투명도 사용
                    val badgeBg = Color.Black.copy(alpha = 0.3f)
                     val totalTextStyle = MaterialTheme.typography.titleMedium.copy(
                         fontWeight = FontWeight.Bold,
                         shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 2f), blurRadius = 4f)
                     )

                    Box(
                        modifier = Modifier
                            .background(badgeBg, shape = MaterialTheme.shapes.small)
                             .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$totalDaysDisplay$dayUnit",
                            style = totalTextStyle,
                            color = badgeYellow
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoResizeSingleLineText(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    step: Float = 0.95f,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    val minSpLocal = 10f
    var style by remember(text) { mutableStateOf(baseStyle) }
    var tried by remember(text) { mutableStateOf(0) }
    Text(
        text = text,
        style = style,
        color = color ?: style.color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && tried < 20) {
                val current = style.fontSize.value
                val next = (current * step).coerceAtLeast(minSpLocal)
                if (next < current - 0.1f) {
                    style = style.copy(fontSize = next.sp, lineHeight = (next.sp * 1.1f))
                    tried++
                }
            }
        }
    )
}

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    titleScale: Float = 1.0f,
    valueScale: Float = 1.0f
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        shape = MaterialTheme.shapes.small,
        // Make the translucent black mask stronger for better contrast over the background
        color = Color.Black.copy(alpha = 0.3f) // 0.22f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val valueBoxH = 40.dp
            val minTitleHeight = 48.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(valueBoxH),
                contentAlignment = Alignment.Center
            ) {
                val base = MaterialTheme.typography.titleMedium
                val numSize = (base.fontSize * valueScale)
                val numStyle = base.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = numSize,
                    lineHeight = numSize * 1.1f,
                    platformStyle = PlatformTextStyle(includeFontPadding = true),
                    fontFeatureSettings = "tnum"
                )
                val unitStyle = base.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = base.fontSize * 0.9f,
                    lineHeight = base.fontSize * 1.1f,
                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                )
                val regex = Regex("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(.*)")
                val m = regex.find(value)
                if (m != null) {
                    val num = m.groupValues[1]
                    val unit = m.groupValues[2]
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        // Numeric value and unit should be white for contrast over the background image/overlay
                        AutoResizeSingleLineText(
                            text = num,
                            baseStyle = numStyle,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alignByBaseline().wrapContentWidth()
                        )
                        if (unit.isNotBlank()) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = unit,
                                style = unitStyle,
                                color = Color.White,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                } else {
                    AutoResizeSingleLineText(
                        text = value,
                        baseStyle = numStyle,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minTitleHeight),
                contentAlignment = Alignment.Center
            ) {
                val baseLabel = MaterialTheme.typography.labelMedium
                val scaledLabelFontSize = baseLabel.fontSize * titleScale
                val scaledLabelStyle = baseLabel.copy(
                    fontSize = scaledLabelFontSize,
                    lineHeight = scaledLabelFontSize * 1.28f,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = scaledLabelStyle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
