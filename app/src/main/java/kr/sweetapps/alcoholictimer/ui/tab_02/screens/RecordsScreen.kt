// [NEW] Tab02 리팩토링: RecordsScreen을 tab_02/screens로 이동
@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_IMPORT", "UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.repository.RecordsDataLoader
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.ui.tab_02.components.MonthPickerBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_02.components.PeriodSelectionSection
import kr.sweetapps.alcoholictimer.ui.tab_02.components.WeekPickerBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_02.components.YearPickerBottomSheet
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.util.utils.DateOverlapUtils

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
    onNavigateToAllRecords: () -> Unit = {}, // [FIX] 모든 금주 기록 보기
    onNavigateToAllDiaries: () -> Unit = {}, // [NEW] 모든 일기 보기
    onAddRecord: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {}, // [NEW] 일기 클릭 콜백
    fontScale: Float = 1.06f
) {
    // view_records 이벤트는 하단 네비게이션 버튼 클릭에서 전송하도록 변경됨.

    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // [NEW] 최근 일기 상태: SharedPreferences의 'diaries'를 반영합니다.
    var diaries by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }

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
            // [NEW] 최근 일기 로드 (최신순 최대 3개)
            try {
                val sharedPref = context.getSharedPreferences("diary_data", android.content.Context.MODE_PRIVATE)
                val diariesJson = sharedPref.getString("diaries", "[]") ?: "[]"
                val diariesArray = org.json.JSONArray(diariesJson)
                val loadedDiaries = (0 until minOf(3, diariesArray.length())).map { i ->
                    val item = diariesArray.getJSONObject(i)
                    DiaryEntry(
                        id = item.optLong("timestamp", 0L).toString(), // [NEW] timestamp를 ID로 사용
                        timestamp = item.optLong("timestamp", 0L), // [NEW] timestamp 필드
                        date = item.optString("date", ""),
                        emoji = item.optString("emoji", ""),
                        content = item.optString("content", ""),
                        cravingLevel = item.optInt("cravingLevel", 0) // [NEW] cravingLevel 필드
                    )
                }
                diaries = loadedDiaries
            } catch (e: Exception) {
                Log.e("RecordsScreen", "일기 로드 실패", e)
                diaries = emptyList()
            }
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
                        PeriodHeaderRow(onNavigateToAllRecords = onNavigateToAllRecords)
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

                // [NEW] 최근 금주 일기 섹션
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        RecentDiarySection(
                            diaries = diaries,
                            onNavigateToAllDiaries = onNavigateToAllDiaries, // [FIX] 모든 일기 보기 콜백 사용
                            onDiaryClick = onDiaryClick // [NEW] 일기 클릭 콜백 전달
                        )
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

            // [NEW] 글쓰기 FAB (우측 하단)
            FloatingActionButton(
                onClick = { onAddRecord() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = safePadding.calculateBottomPadding() + 16.dp
                    ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "일기 작성",
                    modifier = Modifier.size(24.dp)
                )
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
private fun PeriodHeaderRow(onNavigateToAllRecords: () -> Unit) {
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
        // [수정] + 버튼 -> 전체 기록 보기 아이콘으로 변경
        IconButton(onClick = onNavigateToAllRecords) {
            Icon(
                painter = painterResource(id = R.drawable.ic_list),
                contentDescription = stringResource(R.string.records_view_all_icon_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

    // [NEW] 실시간 업데이트를 위한 현재 시간 상태
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // 1초마다 업데이트
            now = System.currentTimeMillis()
        }
    }

    // [NEW] Context는 여기서 미리 가져오기
    val context = LocalContext.current

    // [NEW] 배속된 하루 길이 가져오기 (실시간 업데이트)
    val dayInMillis = remember(now) {
        Constants.getDayInMillis(context)
    }

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
            DateOverlapUtils.overlapDays(record.startTime, record.endTime, null, null)
        } else {
            DateOverlapUtils.overlapDays(record.startTime, record.endTime, periodRange.first, periodRange.second)
        }
    }

    // [FIX] 현재 진행 중인 타이머의 경과 일수 계산 (배속 반영)
    val currentTimerDays = remember(now, periodRange, dayInMillis) {
        val sharedPref = context.getSharedPreferences(
            Constants.USER_SETTINGS_PREFS,
            android.content.Context.MODE_PRIVATE
        )
        val startTime = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
        val timerCompleted = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

        if (startTime > 0 && !timerCompleted) {
            // 현재 진행 중인 타이머가 있음
            val currentEndTime = now // 실시간 종료 시간

            // [핵심] 배속된 dayInMillis를 사용하여 경과 일수 계산
            val elapsedMillis = if (periodRange == null) {
                // 기간 필터 없음: 전체 경과 시간
                currentEndTime - startTime
            } else {
                // 기간 필터 있음: 범위 내에서 겹치는 시간만 계산
                val start = maxOf(startTime, periodRange.first)
                val end = minOf(currentEndTime, periodRange.second)
                if (end > start) end - start else 0L
            }

            // 배속이 적용된 일수로 변환
            elapsedMillis.toDouble() / dayInMillis.toDouble()
        } else {
            0.0 // 진행 중인 타이머 없음
        }
    }

    // [FIX] 기간별 금주 일수 계산 (완료된 기록 + 현재 진행 중인 타이머)
    val totalDaysDouble = remember(records, periodRange, currentTimerDays) {
        records.sumOf { record -> overlappedDays(record) } + currentTimerDays
    }
    val totalDaysDisplay = String.format(Locale.getDefault(), "%.1f", totalDaysDouble)

    // [NEW] 사용자 설정값 가져오기
    val (userCost, userFreq, _) = remember { Constants.getUserSettings(context) }

    // [NEW] 일일 음주 확률 계산
    val dailyFactor = Constants.DrinkingSettings.getFrequencyValue(userFreq) / 7.0

    // [FIX] 1. 피한 칼로리 계산 (좌측) - 실시간 업데이트
    val kcalPerSession = when (userCost) {
        Constants.KEY_COST_LOW -> 500
        Constants.KEY_COST_MEDIUM -> 1500
        Constants.KEY_COST_HIGH -> 2800
        else -> 1500
    }
    val totalKcal = remember(totalDaysDouble, dailyFactor, kcalPerSession) {
        (totalDaysDouble * dailyFactor * kcalPerSession).toInt()
    }

    // [FIX] 2. 안 마신 술 계산 (중앙) - 실시간 업데이트
    val bottlesPerSession = when (userCost) {
        Constants.KEY_COST_LOW -> 1.0
        Constants.KEY_COST_MEDIUM -> 2.5
        Constants.KEY_COST_HIGH -> 4.0
        else -> 2.5
    }
    val totalBottles = remember(totalDaysDouble, dailyFactor, bottlesPerSession) {
        (totalDaysDouble * dailyFactor * bottlesPerSession)
    }

    // [FIX] 3. 절약한 돈 계산 (우측) - 실시간 업데이트
    val costPerSession = Constants.DrinkingSettings.getCostValue(userCost)
    val totalMoney = remember(totalDaysDouble, dailyFactor, costPerSession) {
        (totalDaysDouble * dailyFactor * costPerSession).toLong()
    }

    // [NEW] 천 단위 콤마 포맷터
    val decimalFormat = java.text.DecimalFormat("#,###")

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RECORDS_CARD_IN_ROW_SPACING)
                ) {
                    val statsScale = 1.3f

                    // [NEW] 좌측: 줄인 칼로리
                    StatisticItem(
                        title = "줄인\n칼로리",
                        value = "+${decimalFormat.format(totalKcal)} kcal",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )

                    // [NEW] 중앙: 참아낸 술
                    StatisticItem(
                        title = "참아낸\n술",
                        value = "+${String.format(Locale.getDefault(), "%.1f", totalBottles)} 병",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )

                    // [NEW] 우측: 지켜낸 돈
                    StatisticItem(
                        title = "지켜낸\n돈",
                        value = "+${decimalFormat.format(totalMoney)} 원",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )
                }

                Spacer(modifier = Modifier.height(RECORDS_STATS_ROW_SPACING))

                val dayUnit = stringResource(R.string.records_day_unit)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "총 금주일",
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
    // [FIX] BoxWithConstraints로 크기를 측정하여 넘치지 않도록 자동 조절
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val textMeasurer = rememberTextMeasurer()

        // 텍스트가 넘치지 않는 최적 크기 계산
        val optimalFontSize = remember(text, baseStyle, maxWidthPx) {
            var currentSize = baseStyle.fontSize.value
            val minSize = 10f

            while (currentSize > minSize) {
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

            currentSize.coerceAtLeast(minSize)
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

// [NEW] 최근 금주 일기 데이터 모델
data class DiaryEntry(
    val id: String, // [NEW] 일기 고유 ID (timestamp 활용)
    val timestamp: Long, // [NEW] 작성 시간 (상세보기에 필요)
    val date: String,
    val emoji: String,
    val content: String,
    val cravingLevel: Int = 0 // [NEW] 음주 욕구 수치
)

/**
 * [NEW] 최근 금주 일기 섹션
 */
@Composable
private fun RecentDiarySection(
    diaries: List<DiaryEntry>,
    onNavigateToAllDiaries: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {} // [NEW] 일기 클릭 콜백
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        // [NEW] 헤더: 제목 + 전체 보기 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "최근 금주 일기",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // [NEW] 전체 보기 버튼 (일기가 있을 때만 표시)
            if (diaries.isNotEmpty()) {
                IconButton(onClick = onNavigateToAllDiaries) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = "전체 일기 보기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // [수정] 일기 항목 카드 or 빈 상태 UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            if (diaries.isEmpty()) {
                // [NEW] 빈 상태 UI
                DiaryEmptyState()
            } else {
                // 일기 항목들
                diaries.forEachIndexed { index, diary ->
                    DiaryListItem(
                        diary = diary,
                        onClick = { onDiaryClick(diary) } // [NEW] 클릭 이벤트 전달
                    )

                    if (index < diaries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * [NEW] 일기 빈 상태 UI
 */
@Composable
private fun DiaryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 이모지 아이콘
        Text(
            text = "📝",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 제목
        Text(
            text = "아직 작성된 일기가 없어요",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 설명
        Text(
            text = "우측 하단 + 버튼을 눌러\n첫 일기를 작성해보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * [NEW] 일기 항목 아이템
 */
@Composable
private fun DiaryListItem(diary: DiaryEntry, onClick: () -> Unit = {}) { // [NEW] onClick 파라미터 추가
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // [NEW] 클릭 이벤트 연결
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 날짜
            Text(
                text = diary.date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                modifier = Modifier.width(90.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 이모지
            Text(
                text = diary.emoji,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 내용 미리보기
            Text(
                text = diary.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // 화살표 아이콘
        Icon(
            painter = painterResource(id = R.drawable.ic_caret_right),
            contentDescription = "상세 보기",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}
