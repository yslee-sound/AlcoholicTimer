// [NEW] Tab02 리팩토링: RecordsScreen을 tab_02/screens로 이동
@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_IMPORT", "UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.ui.tab_02.components.MonthPickerBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_02.components.WeekPickerBottomSheet
import kr.sweetapps.alcoholictimer.ui.tab_02.components.YearPickerBottomSheet
import java.util.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager  // [NEW] 동적 통화 표시
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState  // [NEW] 실시간 설정 감지
import kr.sweetapps.alcoholictimer.ui.components.AutoResizingText  // [NEW] 자동 크기 조절 텍스트
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions

val RECORDS_SCREEN_HORIZONTAL_PADDING: Dp = 20.dp // 전체 화면 좌우 여백
val RECORDS_SECTION_SPACING: Dp = 20.dp // [NEW] 섹션 간 통일 간격 (기간 선택 ↔ 월 통계 ↔ 최근 일기)
val RECORDS_STATS_INTERNAL_TOP_GAP: Dp = 10.dp // 12
val RECORDS_STATS_ROW_SPACING: Dp = 10.dp // 12, 3칩 하단
val RECORDS_CARD_IN_ROW_SPACING: Dp = 10.dp // 12, 3칩 사이 공간
val RECORDS_HEADER_START_PADDING: Dp = 20.dp
val RECORDS_TOP_SECTION_EXTERNAL_GAP: Dp = 15.dp // 화면 최상단 패딩
private val RECORDS_HEADER_TO_CARD_GAP = 10.dp  // [UPDATE] 헤더와 카드 사이 간격 조정
private val RECORDS_CARD_INTERNAL_TOP_PADDING = 8.dp // 8, 3칩 그룹 내부 상단
val RECORDS_STATS_CARD_ELEVATION: Dp = 2.dp // <- change this number in this file to control this card's elevation
val RECORDS_LIST_BOTTOM_PADDING: Dp = 100.dp // [UPDATED] Increased from 15.dp to 100.dp for breathing room at bottom

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun RecordsScreen(
    // [MOD] Stateless UI로 변경: 모든 데이터를 파라미터로 받음
    records: List<SobrietyRecord> = emptyList(), // 필터링된 기록
    allRecords: List<SobrietyRecord> = records, // [NEW] 전체 기록 (선택기용)
    isLoading: Boolean = false,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    selectedWeekRange: Pair<Long, Long>?,
    onPeriodSelected: (String) -> Unit = {},
    onDetailPeriodSelected: (String) -> Unit = {},
    onWeekRangeSelected: (Pair<Long, Long>?) -> Unit = {},
    recentDiaries: List<kr.sweetapps.alcoholictimer.data.room.DiaryEntity> = emptyList(),
    // [NEW] Phase 2: 레벨 관련 파라미터
    currentLevel: LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {},
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData = kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData(), // [NEW] 실시간 통계 데이터
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onNavigateToAllDiaries: () -> Unit = {},
    onNavigateToDiaryWrite: () -> Unit = {}, // [NEW] 일기 작성 화면 이동
    onAddRecord: () -> Unit = {},
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {},
    fontScale: Float = 1.06f
) {
    val context = LocalContext.current

    // 기간 리소스 문자열
    val periodWeek = stringResource(R.string.records_period_week)
    val periodMonth = stringResource(R.string.records_period_month)
    val periodYear = stringResource(R.string.records_period_year)

    // [MOD] UI 전용 상태만 유지 (Bottom Sheet 표시 여부)
    var showBottomSheet by remember { mutableStateOf(false) }

    // [NEW] 바텀시트 상태 변경 로깅
    LaunchedEffect(showBottomSheet) {
        Log.d("RecordsScreen", "showBottomSheet 상태 변경: $showBottomSheet, selectedPeriod=$selectedPeriod")
    }

    // [NEW] 월간 탭 선택 시 자동 초기화
    LaunchedEffect(selectedPeriod) {
        if (selectedPeriod.contains("월") && selectedDetailPeriod.isBlank()) {
            // 현재 월로 자동 초기화
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH) + 1
            onDetailPeriodSelected(context.getString(R.string.date_format_year_month, currentYear, currentMonth))
            Log.d("RecordsScreen", "월간 탭 자동 초기화: ${currentYear}년 ${currentMonth}월")
        }
    }

    // [MOD] 필터링 로직 제거 - 이미 필터링된 데이터를 파라미터로 받음

    CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, fontScale = LocalDensity.current.fontScale * fontScale),
        LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)
    ) {
        val safePadding = LocalSafeContentPadding.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
        ) {

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
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ==================== NEW: 상단 헤더 (모던 대시보드 스타일) ====================
                item {
                    // [MODIFIED] 종 버튼 제거로 인해 파라미터 제거 (2025-12-19)
                    ModernDashboardHeader()
                }

                // ==================== Item 0: 레벨 요약 배너 ====================
                if (currentLevel != null) {
                    item {
                        Spacer(modifier = Modifier.height(RECORDS_TOP_SECTION_EXTERNAL_GAP))

                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                            kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelSummaryBanner(
                                currentLevel = currentLevel,
                                currentDays = currentDays,
                                progress = levelProgress,
                                onClick = onNavigateToLevelDetail
                            )
                        }
                    }
                }

                // ==================== Item 1: 통계 헤더 & 필터 통합 ====================
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 통계 요약 제목 + [월간|전체] 토글
                    StatisticsHeaderWithFilter(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { period: String ->
                            Log.d("RecordsScreen", "onPeriodSelected 호출: $period")
                            onPeriodSelected(period)
                            try {
                                val viewType = when (period) {
                                    periodMonth -> "Month"
                                    else -> "All"
                                }
                                val currentLevel = records.maxOfOrNull { it.achievedLevel } ?: 0
                                AnalyticsManager.logChangeRecordView(viewType, currentLevel)
                            } catch (_: Throwable) {}
                        }
                    )
                }

                // ==================== Item 2: 월간 네비게이터 (조건부 표시) ====================
                item {
                    // [NEW] 월간 탭 선택 시에만 표시되는 날짜 컨트롤러
                    AnimatedVisibility(
                        visible = selectedPeriod.contains("월"),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            MonthNavigator(
                                selectedDetailPeriod = selectedDetailPeriod,
                                onPreviousMonth = {
                                    // 이전 달로 이동
                                    val (year, month) = parseYearMonth(selectedDetailPeriod)
                                    val newMonth = if (month == 1) 12 else month - 1
                                    val newYear = if (month == 1) year - 1 else year
                                    onDetailPeriodSelected(context.getString(R.string.date_format_year_month, newYear, newMonth))
                                },
                                onNextMonth = {
                                    // 다음 달로 이동
                                    val (year, month) = parseYearMonth(selectedDetailPeriod)
                                    val newMonth = if (month == 12) 1 else month + 1
                                    val newYear = if (month == 12) year + 1 else year
                                    onDetailPeriodSelected(context.getString(R.string.date_format_year_month, newYear, newMonth))
                                },
                                onDateClick = {
                                    // 날짜 클릭 시 바텀시트 열기
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                }

                // ==================== Item 3: 분리된 통계 카드 그리드 ====================
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3개의 독립 카드 (칼로리, 절주, 저축)
                    ModernStatisticsGrid(
                        statsData = statsData
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 총 금주일 독립 카드 (전체 기록 화면으로 이동)
                    TotalDaysCard(
                        totalDays = statsData.totalDays,
                        onNavigateToAllRecords = onNavigateToAllRecords // [NEW] 전체 기록 화면 이동
                    )
                }

                // ==================== Item 4: 최근 금주 일기 섹션 ====================
                item {
                    // [FIX] 섹션 간격 통일 (20dp)
                    Spacer(modifier = Modifier.height(RECORDS_SECTION_SPACING))

                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        RecentDiarySection(
                            diaries = recentDiaries,
                            onNavigateToAllDiaries = onNavigateToAllDiaries,
                            onNavigateToDiaryWrite = onNavigateToDiaryWrite, // [NEW] 일기 작성 콜백 전달
                            onDiaryClick = onDiaryClick
                        )
                    }
                }
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
        Log.d("RecordsScreen", "바텀시트 렌더링: selectedPeriod=$selectedPeriod, allRecords.size=${allRecords.size}") // [NEW] 로그 추가
        when (selectedPeriod) {
            periodWeek -> {
                Log.d("RecordsScreen", "주 선택기 표시") // [NEW] 로그 추가
                WeekPickerBottomSheet(
                    isVisible = true,
                    onDismiss = {
                        Log.d("RecordsScreen", "주 선택기 닫기") // [NEW] 로그 추가
                        showBottomSheet = false
                    },
                    onWeekPicked = { weekStart, weekEnd, displayText ->
                        Log.d("RecordsScreen", "주 선택 완료: $displayText") // [NEW] 로그 추가
                        onDetailPeriodSelected(displayText)
                        onWeekRangeSelected(weekStart to weekEnd)
                        showBottomSheet = false
                    }
                )
            }
            periodMonth -> {
                Log.d("RecordsScreen", "월 선택기 표시") // [NEW] 로그 추가
                MonthPickerBottomSheet(
                    isVisible = true,
                    onDismiss = {
                        Log.d("RecordsScreen", "월 선택기 닫기") // [NEW] 로그 추가
                        showBottomSheet = false
                    },
                    onMonthPicked = { year, month ->
                        Log.d("RecordsScreen", "월 선택 완료: $year-$month") // [NEW] 로그 추가
                        onDetailPeriodSelected(context.getString(R.string.date_format_year_month, year, month))
                        showBottomSheet = false
                    },
                    records = allRecords // [FIX] 전체 기록 사용
                    // [FIX] onYearPicked 제거: 월 선택기에서 년도 스크롤은 월 필터링용이지 기간 변경용이 아님
                )
            }
            periodYear -> {
                Log.d("RecordsScreen", "년 선택기 표시") // [NEW] 로그 추가
                val initialYearForPicker = Regex("(\\d{4})").find(selectedDetailPeriod)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Calendar.getInstance().get(Calendar.YEAR)

                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = {
                        Log.d("RecordsScreen", "년 선택기 닫기") // [NEW] 로그 추가
                        showBottomSheet = false
                    },
                    onYearPicked = { year ->
                        Log.d("RecordsScreen", "년 선택 완료: $year") // [NEW] 로그 추가
                        onDetailPeriodSelected(context.getString(R.string.date_format_year, year))
                        showBottomSheet = false
                    },
                    records = allRecords, // [FIX] 전체 기록 사용
                    initialYear = initialYearForPicker
                )
            }
        }
    }
}

@Preview(
    name = "기록 화면 - 데이터 있음",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun RecordsScreenPreview() {
    AlcoholicTimerTheme {
        val currentTime = System.currentTimeMillis()
        val sampleRecords = listOf(
            SobrietyRecord(
                id = "preview_1",
                startTime = currentTime - (7L * 24 * 60 * 60 * 1000),
                endTime = currentTime,
                targetDays = 30,
                actualDays = 7.0,
                isCompleted = false,
                status = "active",
                createdAt = currentTime,
                percentage = 23
            ),
            SobrietyRecord(
                id = "preview_2",
                startTime = currentTime - (15L * 24 * 60 * 60 * 1000),
                endTime = currentTime - (8L * 24 * 60 * 60 * 1000),
                targetDays = 7,
                actualDays = 7.0,
                isCompleted = true,
                status = "completed",
                createdAt = currentTime - (8L * 24 * 60 * 60 * 1000),
                percentage = 100
            )
        )

        RecordsScreen(
            records = sampleRecords,
            allRecords = sampleRecords,
            isLoading = false,
            selectedPeriod = "월",
            selectedDetailPeriod = "2025년 12월",
            selectedWeekRange = null,
            recentDiaries = emptyList(),
            statsData = kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData(
                totalDays = 7.0f,
                savedMoney = 35000.0,
                totalKcal = 1400.0,
                totalBottles = 14.0
            )
        )
    }
}

@Preview(
    name = "기록 화면 - 빈 상태",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun RecordsScreenEmptyPreview() {
    AlcoholicTimerTheme {
        RecordsScreen(
            records = emptyList(),
            allRecords = emptyList(),
            isLoading = false,
            selectedPeriod = "월",
            selectedDetailPeriod = "2025년 12월",
            selectedWeekRange = null,
            recentDiaries = emptyList(),
            statsData = kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData()
        )
    }
}

@Composable
private fun PeriodHeaderRow(
    selectedPeriod: String,
    onNavigateToAllRecords: () -> Unit
) {
    val context = LocalContext.current

    // [FIX] 선택된 기간에 따라 동적 제목 표시
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

        else -> context.getString(R.string.records_all_stats) // 전체 통계
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp), // [UPDATE] Row 높이: 28dp → 32dp (더 여유 있는 간격)
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        // [FIX] IconButton 대신 Icon + clickable 사용 (48dp 터치 영역 제거)
        Icon(
            painter = painterResource(id = R.drawable.ic_caret_right),
            contentDescription = stringResource(R.string.records_view_all_icon_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp),
                    onClick = onNavigateToAllRecords
                )
        )
    }
}

@Composable
private fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    weekRange: Pair<Long, Long>? = null,
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData, // [NEW] ViewModel에서 계산된 통계 데이터
    onAddRecord: () -> Unit = {}
) {
    val context = LocalContext.current
    val totalRecords = records.size

    // [NEW] 실시간 설정 변경 감지 - 탭4에서 설정을 바꾸면 즉시 반영됨
    val userSettings by rememberUserSettingsState(context)

    // [FIX] ViewModel에서 이미 계산된 통계 데이터를 사용
    val totalDays = statsData.totalDays
    val savedMoney = statsData.savedMoney
    val totalKcal = statsData.totalKcal
    val totalBottles = statsData.totalBottles

    // [NEW] 저축 금액: 숫자와 통화 기호 분리 (다른 카드들과 동일한 형태)
    val savedMoneyValue = remember(savedMoney, userSettings.currencySymbol) {
        val currency = CurrencyManager.getSelectedCurrency(context)
        val converted = savedMoney / currency.rate
        // 숫자만 포맷팅 (통화 기호 제외)
        String.format(java.util.Locale.getDefault(), "%,.0f", converted)
    }

    val savedMoneyUnit = remember(userSettings.currencySymbol) {
        val currency = CurrencyManager.getSelectedCurrency(context)
        currency.code  // "KRW", "USD", "JPY" 등
    }

    val daysText = remember(totalDays) {
        String.format(java.util.Locale.getDefault(), "%.1f", totalDays)
    }

    val kcalText = remember(totalKcal) {
        String.format(java.util.Locale.getDefault(), "%.0f", totalKcal)
    }

    val bottlesText = remember(totalBottles) {
        String.format(java.util.Locale.getDefault(), "%.1f", totalBottles)
    }

    // [NEW] 천 단위 콤마 포맷터
    val decimalFormat = java.text.DecimalFormat("#,###")
    val kcalFormatted = decimalFormat.format(totalKcal.toLong())


    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // [UPDATE] 16dp 둥근 모서리
        colors = CardDefaults.cardColors(containerColor = Color.White), // [UPDATE] 깔끔한 흰색 배경
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // [UPDATE] 살짝 그림자
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)) // [UPDATE] 연한 테두리
    ) {
        // [REMOVED] Image background 제거 - 깔끔한 흰색 카드로 변경

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
                Spacer(modifier = Modifier.height(RECORDS_STATS_INTERNAL_TOP_GAP))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp), // [FIX] 고정 높이 추가로 3개 카드 높이 통일
                    horizontalArrangement = Arrangement.spacedBy(RECORDS_CARD_IN_ROW_SPACING)
                ) {
                    val statsScale = 1.3f

                    // [NEW] 좌측: 줄인 칼로리 → 컷 (personsimplerun 아이콘)
                    StatisticItem(
                        title = stringResource(R.string.stats_label_calories_short),
                        value = "$kcalFormatted ${stringResource(R.string.stats_unit_kcal)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        valueColor = Color(0xFF111111), // [UPDATE] 진한 검은색
                        icon = R.drawable.personsimplerun,
                        iconTint = Color(0xFF666666), // [UPDATE] 진한 회색
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )

                    // [NEW] 중앙: 참아낸 술 → 절주 (wine 아이콘)
                    StatisticItem(
                        title = stringResource(R.string.stats_label_drinks_short),
                        value = "$bottlesText ${stringResource(R.string.stats_unit_bottles)}",
                        color = MaterialTheme.colorScheme.primary,
                        valueColor = Color(0xFF111111), // [UPDATE] 진한 검은색
                        icon = R.drawable.wine,
                        iconTint = Color(0xFF666666), // [UPDATE] 진한 회색
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        titleScale = statsScale,
                        valueScale = statsScale
                    )

                    // [NEW] 우측: 지켜낸 돈 → 저축 (piggybank 아이콘)
                    StatisticItem(
                        title = stringResource(R.string.stats_label_money_short),
                        value = "$savedMoneyValue $savedMoneyUnit",
                        color = MaterialTheme.colorScheme.error,
                        valueColor = Color(0xFF111111), // [UPDATE] 진한 검은색
                        icon = R.drawable.piggybank,
                        iconTint = Color(0xFF666666), // [UPDATE] 진한 회색
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
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
                        text = stringResource(R.string.stats_total_days),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF666666) // [UPDATE] 진한 회색
                    )

                    // [UPDATE] 연회색 배지로 변경
                    val badgeBg = Color(0xFFF0F0F0) // 연회색
                    val totalTextStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111) // [UPDATE] 진한 검은색
                    )

                    Box(
                        modifier = Modifier
                            .background(badgeBg, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$daysText$dayUnit",
                            style = totalTextStyle // [UPDATE] 색상은 totalTextStyle에 이미 정의됨
                        )
                    }
                }
            }
        }
    }

/**
 * [NEW] 자동 크기 조절 텍스트 컴포넌트
 * 텍스트가 가로 공간을 넘을 경우 폰트 크기를 자동으로 축소하여 전체 텍스트가 보이도록 함
 *
 * @param text 표시할 텍스트
 * @param baseStyle 기본 텍스트 스타일 (목표 폰트 크기 포함)
 * @param modifier Modifier
 * @param step 폰트 크기 축소 비율 (기본: 0.9 = 10%씩 축소)
 * @param minFontSize 최소 폰트 크기 (기본: 10.sp)
 * @param color 텍스트 색상
 * @param textAlign 텍스트 정렬
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
    // [FIX] BoxWithConstraints로 크기를 측정하여 넘치지 않도록 자동 조절
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val textMeasurer = rememberTextMeasurer()

        // 텍스트가 넘치지 않는 최적 크기 계산
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

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    titleScale: Float = 1.0f,
    valueScale: Float = 1.0f,
    valueColor: Color = Color(0xFF111111), // [UPDATE] 진한 검은색 (흰 배경용)
    icon: Any? = null,
    iconTint: Color = Color(0xFF666666)  // [UPDATE] 진한 회색 (흰 배경용)
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color(0xFFF8F8F8) // [UPDATE] 연한 회색 배경 (흰 카드 안에서 구분)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // [개선] 숫자와 단위를 분리하여 표시
            val base = MaterialTheme.typography.titleMedium
            val numSize = (base.fontSize * valueScale * 0.75f)
            val numStyle = base.copy(
                fontWeight = FontWeight.Bold,
                fontSize = numSize,
                lineHeight = numSize * 1.1f,
                letterSpacing = (-0.5).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )

            val regex = Regex("^\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*(.*)")
            val m = regex.find(value)

            if (m != null) {
                val num = m.groupValues[1]
                val unit = m.groupValues[2]

                // [FIX] 1단계: 숫자 영역 - AutoResizing (유연한 크기)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp), // 고정 높이
                    contentAlignment = Alignment.BottomCenter // 바닥 앵커
                ) {
                    AutoResizeSingleLineText(
                        text = num,
                        baseStyle = numStyle,
                        color = valueColor,
                        textAlign = TextAlign.Center,
                        step = 0.9f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // [FIX] 2단계: 단위 영역 - 고정 11sp (제목과 통일)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (unit.isNotBlank()) {
                        Text(
                            text = unit,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF888888), // [UPDATE] 중간 톤 회색
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            } else {
                // 파싱 실패 시 전체 문자열 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AutoResizeSingleLineText(
                        text = value,
                        baseStyle = numStyle,
                        color = valueColor,
                        textAlign = TextAlign.Center,
                        step = 0.9f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(28.dp)) // [FIX] 24dp → 28dp
            }

            // [FIX] 고정 간격
            Spacer(modifier = Modifier.height(4.dp))

            // [FIX] 3단계: [아이콘 + 제목] 영역 - 중앙 정렬로 변경
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center  // [FIX] 중앙 정렬
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,  // [FIX] 중앙 정렬
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [NEW] 아이콘이 있으면 표시
                    if (icon != null) {
                        when (icon) {
                            is androidx.compose.ui.graphics.vector.ImageVector -> {
                                // Material Icons
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            is Int -> {
                                // Drawable Resource
                                Icon(
                                    painter = painterResource(id = icon),
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    // 제목 텍스트
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666), // [UPDATE] 진한 회색
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        letterSpacing = (-0.5).sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}


/**
 * [NEW] 최근 금주 일기 섹션 (Room DB 기반)
 */
@Composable
private fun RecentDiarySection(
    diaries: List<kr.sweetapps.alcoholictimer.data.room.DiaryEntity>,
    onNavigateToAllDiaries: () -> Unit = {},
    onNavigateToDiaryWrite: () -> Unit = {}, // [NEW] 일기 작성 콜백
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {}
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        // [UPDATE] 헤더: 제목 + 작성 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.diary_recent_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // [UPDATE] "작성" 버튼 (기존 더보기와 동일한 스타일)
            Text(
                text = "작성",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6366F1),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp),
                    onClick = onNavigateToDiaryWrite
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // [수정] 일기 항목 카드 or 빈 상태 UI
        if (diaries.isEmpty()) {
            DiaryEmptyState()
        } else {
            // [UPDATE] Card로 감싸서 elevation 적용
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 일기 항목들
                    diaries.forEachIndexed { index, diary ->
                        DiaryListItem(
                            diary = diary,
                            onClick = { onDiaryClick(diary) }
                        )

                        if (index < diaries.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                thickness = 1.dp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // [NEW] 푸터: 전체 내역 보기
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE2E8F0)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = onNavigateToAllDiaries
                                ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "전체 내역 보기",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                painter = painterResource(id = R.drawable.ic_caret_right),
                                contentDescription = "전체 내역 보기",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // [NEW] 하단 여백 추가 (카드 바닥과의 breathing room)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * [REDESIGN] 일기 빈 상태 UI - 심리스형
 * - 배경색 제거, 크림색 배경에 자연스럽게 녹아듦
 * - 여백 증가로 시각적 안정감 확보
 */
@Composable
private fun DiaryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // [FIX] notebook 아이콘으로 변경 (AllDiaryScreen과 동일)
        Icon(
            painter = painterResource(id = R.drawable.notebook),
            contentDescription = null,
            tint = Color(0xFFCBD5E1), // 연한 회색
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp)
        )

        // 제목
        Text(
            text = stringResource(R.string.diary_no_entries),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 설명
        Text(
            text = stringResource(R.string.diary_write_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * [NEW] 일기 항목 아이템 (Magazine Style)
 * 날짜 박스 + 제목 + 이모지
 */
@Composable
private fun DiaryListItem(
    diary: kr.sweetapps.alcoholictimer.data.room.DiaryEntity,
    onClick: () -> Unit = {}
) {
    val locale = Locale.getDefault()

    // 날짜 파싱
    val (monthText, dayText, yearText) = remember(diary.timestamp, locale) {
        val cal = Calendar.getInstance().apply { timeInMillis = diary.timestamp }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        when (locale.language) {
            "ko" -> {
                val monthStr = when (month) {
                    1 -> "JAN"
                    2 -> "FEB"
                    3 -> "MAR"
                    4 -> "APR"
                    5 -> "MAY"
                    6 -> "JUN"
                    7 -> "JUL"
                    8 -> "AUG"
                    9 -> "SEP"
                    10 -> "OCT"
                    11 -> "NOV"
                    12 -> "DEC"
                    else -> "DEC"
                }
                Triple(monthStr, day.toString(), "${year}년")
            }
            else -> {
                val monthStr = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(diary.timestamp)).uppercase()
                Triple(monthStr, day.toString(), year.toString())
            }
        }
    }

    // 날짜 박스 색상 (랜덤 파스텔 컬러)
    val boxColor = remember(diary.id) {
        val colors = listOf(
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFFE8F5E9), // Light Green
            Color(0xFFF3E0E0), // Light Orange
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFFFCE4EC)  // Light Pink
        )
        colors[kotlin.math.abs(diary.id.hashCode()) % colors.size]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth() // [순서 1] 영역 확보
            .clickable { onClick() } // [순서 2] 클릭 리스너 (패딩보다 먼저!)
            .padding(horizontal = 20.dp, vertical = 16.dp), // [순서 3] 내부 여백 (클릭 영역 안쪽으로 밀어넣기)
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // 좌측: 날짜 박스 (2번째 사진 스타일: 더 작고 컴팩트)
        Card(
            modifier = Modifier.size(width = 52.dp, height = 52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = boxColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 월 (위)
                Text(
                    text = monthText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    letterSpacing = 0.3.sp,
                    lineHeight = 12.sp
                )

                Spacer(modifier = Modifier.height(1.dp))

                // 일 (아래)
                Text(
                    text = dayText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 중앙: 제목 + 연도
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = diary.content,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = yearText,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 우측: 이모지
        Text(
            text = diary.emoji,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * [NEW] 모던 대시보드 스타일 헤더
 * "나의 건강 분석" 제목 + 인사말 + 알림 벨
 */
@Composable
// [MODIFIED] 종 버튼 제거 (2025-12-19)
private fun ModernDashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측: 제목 + 인사말
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "나의 건강 ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    fontSize = 24.sp
                )
                Text(
                    text = "분석",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1), // 보라색 강조
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "오늘도 건강한 하루 되세요!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * [NEW] 통계 헤더와 필터 통합 (한 줄)
 * "통계 요약" (좌측) + [월간|전체] 토글 (우측)
 */
@Composable
private fun StatisticsHeaderWithFilter(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측: 제목
        Text(
            text = "통계 요약",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            fontSize = 18.sp
        )

        // 우측: [월간|전체] 토글
        Row(
            modifier = Modifier
                .background(Color(0xFFF1F3F5), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 월간 버튼
            ToggleButton(
                text = "월간",
                isSelected = selectedPeriod.contains("월"),
                onClick = { onPeriodSelected("월") }
            )

            // 전체 버튼
            ToggleButton(
                text = "전체",
                isSelected = !selectedPeriod.contains("월"),
                onClick = { onPeriodSelected("전체") }
            )
        }
    }
}

/**
 * [NEW] 토글 버튼 (캡슐 모양)
 */
@Composable
private fun ToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280)
        )
    }
}

/**
 * [NEW] 모던 통계 그리드 (3개 독립 카드)
 */
@Composable
private fun ModernStatisticsGrid(
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData
) {
    val decimalFormat = remember { java.text.DecimalFormat("#,###") }
    val context = LocalContext.current
    val currency = CurrencyManager.getSelectedCurrency(context)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 칼로리 카드
        StatCard(
            icon = R.drawable.personsimplerun,
            iconColor = Color(0xFFFF9F66),
            label = "CALORIES",
            value = decimalFormat.format(statsData.totalKcal.toLong()),
            unit = "kcal",
            modifier = Modifier.weight(1f)
        )

        // 절주 카드
        StatCard(
            icon = R.drawable.wine,
            iconColor = Color(0xFF6B9DFF),
            label = "SOBER",
            value = String.format("%.1f", statsData.totalBottles),
            unit = "병",
            modifier = Modifier.weight(1f)
        )

        // 저축 카드
        StatCard(
            icon = R.drawable.piggybank,
            iconColor = Color(0xFF5CD88A),
            label = "SAVED",
            value = decimalFormat.format((statsData.savedMoney / currency.rate).toLong()),
            unit = currency.code,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * [NEW] 개별 통계 카드 (세로형)
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
                .fillMaxWidth()
                .padding(8.dp), // [UPDATED] 내부 패딩을 더 컴팩트하게 축소 (기존 12.dp -> 8.dp)
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이콘
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp) // [UPDATED] 아이콘 크기 축소 (32.dp -> 28.dp)로 시각적 간격 감소
            )

            // 라벨
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp)) // [UPDATED] 아이콘-통합텍스트 간격을 4.dp -> 2.dp로 감소

            // 값 - [FIX] AutoResizingText로 자동 크기 조절 (깜빡임 없음)
            AutoResizingText(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center,
                minFontSize = 12.sp
            )

            // 단위
            Text(
                text = unit,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

/**
 * [NEW] 총 금주일 카드 (가로형)
 * 클릭 시 전체 기록 화면으로 이동
 */
@Composable
private fun TotalDaysCard(
    totalDays: Float,
    onNavigateToAllRecords: () -> Unit = {} // [NEW] 전체 기록 화면 이동 콜백
) {
    val decimalFormat = remember { java.text.DecimalFormat("#,###.#") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onNavigateToAllRecords() }, // [NEW] 카드 전체 클릭 가능
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 아이콘 + 제목
            Row(
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
                    text = "총 금주일",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827)
                )
            }

            // 우측: 값 + 화살표
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 날짜 값
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // [FIX] AutoResizingText로 자동 크기 조절 (깜빡임 없음)
                    AutoResizingText(
                        text = decimalFormat.format(totalDays),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.End,
                        minFontSize = 16.sp
                    )

                    Text(
                        text = "일",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // [NEW] 화살표 아이콘
                Icon(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = "전체 기록 보기",
                    tint = Color(0xFF9CA3AF), // 연한 회색
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * [NEW] 월간 네비게이터
 * 이전/다음 달 이동 버튼과 현재 월 표시
 */
@Composable
private fun MonthNavigator(
    selectedDetailPeriod: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: () -> Unit
) {
    // 현재 선택된 월과 오늘의 월 비교
    val (year, month) = parseYearMonth(selectedDetailPeriod)
    val currentYearMonth = Calendar.getInstance().let {
        it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) + 1
    }
    val selectedYearMonth = year * 12 + month

    // 미래 날짜 선택 방지
    val isFutureMonth = selectedYearMonth >= currentYearMonth

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, // [FIX] SpaceBetween → Center
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 이전 달 버튼
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_left),
                contentDescription = "이전 달",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp)) // [NEW] 화살표-날짜 간격

        // 중앙: 현재 월 (클릭 가능)
        Text(
            text = selectedDetailPeriod,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp),
                    onClick = onDateClick
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.width(24.dp)) // [NEW] 날짜-화살표 간격

        // 다음 달 버튼 (미래 방지)
        IconButton(
            onClick = onNextMonth,
            enabled = !isFutureMonth, // [NEW] 미래 월이면 비활성화
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "다음 달",
                tint = if (isFutureMonth) Color(0xFFD1D5DB) else Color(0xFF6B7280), // [NEW] 비활성화 시 연한 회색
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * [NEW] 날짜 문자열 파싱 헬퍼 함수
 * "2025년 12월" → Pair(2025, 12)
 */
private fun parseYearMonth(dateString: String): Pair<Int, Int> {
    return try {
        // "2025년 12월" 형식 파싱
        val yearRegex = Regex("(\\d{4})년")
        val monthRegex = Regex("(\\d{1,2})월")

        val year = yearRegex.find(dateString)?.groupValues?.getOrNull(1)?.toInt() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = monthRegex.find(dateString)?.groupValues?.getOrNull(1)?.toInt() ?: Calendar.getInstance().get(Calendar.MONTH) + 1

        Pair(year, month)
    } catch (e: Exception) {
        // 파싱 실패 시 현재 날짜 반환
        val now = Calendar.getInstance()
        Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }
}
