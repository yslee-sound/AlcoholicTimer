// [NEW] Tab02 리팩토링: RecordsScreen을 tab_02/screens로 이동
@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_IMPORT", "UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ripple.rememberRipple
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue  // [NEW] 메인 UI 색상
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager  // [NEW] 동적 통화 표시
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState  // [NEW] 실시간 설정 감지

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
    currentLevel: kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {},
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData = kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData(), // [NEW] 실시간 통계 데이터
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onNavigateToAllDiaries: () -> Unit = {},
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

    // [MOD] 필터링 로직 제거 - 이미 필터링된 데이터를 파라미터로 받음

    CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, fontScale = LocalDensity.current.fontScale * fontScale),
        LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)
    ) {
        val safePadding = LocalSafeContentPadding.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)) // [UPDATE] 연한 회색 배경 (모던 대시보드 스타일)
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
                verticalArrangement = Arrangement.spacedBy(0.dp) // [FIX] 명시적 Spacer로 제어
            ) {
                // ==================== Item 0: 레벨 요약 배너 (Phase 2) ====================
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

                // ==================== Item 1: 통계 제목줄 ====================
                item {
                    Spacer(modifier = Modifier.height(RECORDS_TOP_SECTION_EXTERNAL_GAP))

                    // 헤더
                    Box(modifier = Modifier.fillMaxWidth().padding(start = RECORDS_HEADER_START_PADDING, end = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        PeriodHeaderRow(
                            selectedPeriod = selectedPeriod,
                            onNavigateToAllRecords = onNavigateToAllRecords
                        )
                    }

                    // 헤더와 필터 사이 간격
                    Spacer(modifier = Modifier.height(RECORDS_HEADER_TO_CARD_GAP))
                }

                // ==================== Item 2: 기간 선택 섹션 ====================
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        PeriodSelectionSection(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { period: String ->
                                Log.d("RecordsScreen", "onPeriodSelected 호출: $period")
                                onPeriodSelected(period)
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
                            onPeriodClick = { clickedPeriod ->
                                Log.d("RecordsScreen", "onPeriodClick 호출: $clickedPeriod, 바텀시트 열기")
                                showBottomSheet = true
                            },
                            selectedDetailPeriod = selectedDetailPeriod,
                            horizontalPadding = RECORDS_SCREEN_HORIZONTAL_PADDING
                        )
                    }
                }

                // ==================== Item 3: 통계 카드 ====================
                item {
                    // 필터와 카드 사이 간격
                    Spacer(modifier = Modifier.height(RECORDS_HEADER_TO_CARD_GAP))

                    // 통계 카드
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        PeriodStatisticsSection(
                            records = records,
                            selectedPeriod = selectedPeriod,
                            selectedDetailPeriod = selectedDetailPeriod,
                            modifier = Modifier.fillMaxWidth(),
                            weekRange = selectedWeekRange,
                            statsData = statsData, // [NEW] 실시간 통계 데이터 전달
                            onAddRecord = { onAddRecord() }
                        )
                    }
                }

                // ==================== Item 3: 최근 금주 일기 섹션 ====================
                item {
                    // [FIX] 섹션 간격 통일 (20dp)
                    Spacer(modifier = Modifier.height(RECORDS_SECTION_SPACING))

                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        RecentDiarySection(
                            diaries = recentDiaries, // [UPDATED] 파라미터로 전달받은 Room DB 데이터 사용
                            onNavigateToAllDiaries = onNavigateToAllDiaries, // [FIX] 모든 일기 보기 콜백 사용
                            onDiaryClick = onDiaryClick // [NEW] 일기 클릭 콜백 전달
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

            // [NEW] 글쓰기 FAB (우측 하단)
            FloatingActionButton(
                onClick = { onAddRecord() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = safePadding.calculateBottomPadding() + 16.dp
                    ),
                containerColor = MainPrimaryBlue,  // [FIX] 메인 UI 색상 적용
                contentColor = Color.White
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.diary_write_button),
                    modifier = Modifier.size(24.dp)
                )
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
    diaries: List<kr.sweetapps.alcoholictimer.data.room.DiaryEntity>, // [UPDATED] DiaryEntity 사용
    onNavigateToAllDiaries: () -> Unit = {},
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {} // [UPDATED] DiaryEntity 사용
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        // [NEW] 헤더: 제목 + 전체 보기 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp), // [UPDATE] Row 높이: 28dp → 32dp (더 여유 있는 간격)
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.diary_recent_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // [FIX] IconButton 대신 Icon + clickable 사용 (48dp 터치 영역 제거)
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = stringResource(R.string.diary_view_all),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        onClick = onNavigateToAllDiaries
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp)) // [UPDATE] 제목-콘텐츠 간격 조정

        // [수정] 일기 항목 카드 or 빈 상태 UI
        if (diaries.isEmpty()) {
            // [REDESIGN] 빈 상태 UI - 심리스형 (배경 투명)
            DiaryEmptyState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
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
 * [NEW] 일기 항목 아이템 (Room DB 기반)
 * 첨부 사진과 같은 디자인: 캘린더 아이콘 + 날짜 + 이모지 + 내용
 */
@Composable
private fun DiaryListItem(
    diary: kr.sweetapps.alcoholictimer.data.room.DiaryEntity,
    onClick: () -> Unit = {}
) {
    val locale = Locale.getDefault()
    val (yearText, dateText) = remember(diary.timestamp, locale) {
        val cal = Calendar.getInstance().apply { timeInMillis = diary.timestamp }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        when (locale.language) {
            "ko" -> Pair("${year}년", "${month}월 ${day}일")
            "ja" -> Pair("${year}年", "${month}月${day}日")
            "zh" -> Pair("${year}年", "${month}月${day}日")
            else -> {
                val monthName = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(diary.timestamp))
                Pair("$year", "$monthName $day")
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // 왼쪽: 캘린더 아이콘
        Icon(
            painter = painterResource(id = R.drawable.ic_nav_calendardots),
            contentDescription = null,
            tint = Color(0xFF8B7BE8), // 보라색
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 중앙: 날짜 정보
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = yearText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 오른쪽 상단: 이모지
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // 이모지
            Text(
                text = diary.emoji,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 내용 미리보기
            Text(
                text = diary.content,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                color = Color(0xFF374151),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}
