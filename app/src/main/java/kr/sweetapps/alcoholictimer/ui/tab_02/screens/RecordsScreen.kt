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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.ui.common.LevelCard
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
import kr.sweetapps.alcoholictimer.util.utils.FormatUtils  // [NEW] 칼로리 축약 포맷터 (2025-12-26)
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState  // [NEW] 실시간 설정 감지
import kr.sweetapps.alcoholictimer.ui.components.AutoResizingText  // [NEW] 자동 크기 조절 텍스트
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import kotlin.math.roundToInt

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
    allDiaries: List<kr.sweetapps.alcoholictimer.data.room.DiaryEntity> = recentDiaries, // [NEW] 전체 일기 (캘린더용) (2025-12-22)
    // [NEW] Phase 2: 레벨 관련 파라미터
    currentLevel: LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {},
    statsData: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData = kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.StatsData(), // [NEW] 실시간 통계 데이터
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onNavigateToAllDiaries: () -> Unit = {},
    onNavigateToDiaryWrite: (Long?) -> Unit = {}, // [FIX] 선택된 날짜 타임스탬프 전달 (2025-12-22)
    onAddRecord: () -> Unit = {},
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {},
    onNavigateToDiaryDetail: (Long) -> Unit = {}, // [NEW] 일기 상세 피드 화면으로 이동 (2025-12-22)
    fontScale: Float = 1.06f
) {
    val context = LocalContext.current

    // 기간 리소스 문자열
    val periodWeek = stringResource(R.string.records_period_week)
    val periodMonth = stringResource(R.string.records_period_month)
    val periodYear = stringResource(R.string.records_period_year)

    // [MOD] UI 전용 상태만 유지 (Bottom Sheet 표시 여부)
    var showBottomSheet by remember { mutableStateOf(false) }

    // [NEW] 캘린더 헤더 클릭 시 고정 기간 모드 활성화 (2025-12-24)
    var isCalendarNavigationMode by remember { mutableStateOf(false) }

    // [NEW] 바텀시트 상태 변경 로깅
    LaunchedEffect(showBottomSheet) {
        Log.d("RecordsScreen", "showBottomSheet 상태 변경: $showBottomSheet, selectedPeriod=$selectedPeriod, isCalendarNavigationMode=$isCalendarNavigationMode")
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

        // [NEW] Scaffold 도입: Tab 3(CommunityScreen)와 동일한 구조 (2025-12-23)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF7F8FA),
            contentWindowInsets = WindowInsets(0.dp) // [핵심] 시스템 인셋 초기화로 TopAppBar 위치 통일
        ) { innerPadding ->

            val layoutDirection = LocalLayoutDirection.current
            // [MODIFIED] 상단 패딩 제거: top = 0.dp (2025-12-23)
            val recordsContentPadding = PaddingValues(
                start = safePadding.calculateLeftPadding(layoutDirection),
                top = 0.dp, // [핵심] 기존 safePadding.calculateTopPadding() 제거
                end = safePadding.calculateRightPadding(layoutDirection),
                bottom = RECORDS_LIST_BOTTOM_PADDING
            )
            Log.d("RecordsScreenDebug", "Records bottom padding replaced with CARD_VERTICAL_SPACING=${UiConstants.CARD_VERTICAL_SPACING}")

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // [NEW] Scaffold의 기본 패딩 적용
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
                            // [MODIFIED] 공통 LevelCard 컴포넌트 사용 (배너 스타일) (2025-12-23)
                            LevelCard(
                                currentLevel = currentLevel,
                                currentDays = currentDays,
                                progress = levelProgress,
                                containerColor = Color(0xFF6366F1), // 단색 보라색
                                cardHeight = 130.dp,
                                showDetailedInfo = false, // 간소화된 프로그레스만 표시
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
                                allRecords = allRecords, // [NEW] 과거 무한 이동 방지용 (2025-12-25)
                                onPreviousMonth = {
                                    // [FIX] Calendar 객체로 정확한 이전 달 계산 (2025-12-25)
                                    val (year, month) = parseYearMonth(selectedDetailPeriod)
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month - 1, 1) // month는 1-based, Calendar는 0-based
                                    cal.add(Calendar.MONTH, -1)
                                    val newYear = cal.get(Calendar.YEAR)
                                    val newMonth = cal.get(Calendar.MONTH) + 1 // 0-based → 1-based
                                    onDetailPeriodSelected(context.getString(R.string.date_format_year_month, newYear, newMonth))
                                },
                                onNextMonth = {
                                    // [FIX] Calendar 객체로 정확한 다음 달 계산 (2025-12-25)
                                    val (year, month) = parseYearMonth(selectedDetailPeriod)
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month - 1, 1) // month는 1-based, Calendar는 0-based
                                    cal.add(Calendar.MONTH, 1)
                                    val newYear = cal.get(Calendar.YEAR)
                                    val newMonth = cal.get(Calendar.MONTH) + 1 // 0-based → 1-based
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
                    Spacer(modifier = Modifier.height(8.dp)) // [FIX] 12dp → 8dp (33% 감소)

                    // 3개의 독립 카드 (칼로리, 절주, 저축)
                    ModernStatisticsGrid(
                        statsData = statsData
                    )

                    Spacer(modifier = Modifier.height(8.dp)) // [FIX] 12dp → 8dp (33% 감소)

                    // 총 금주일 독립 카드 (전체 기록 화면으로 이동)
                    TotalDaysCard(
                        totalDays = statsData.totalDays,
                        onNavigateToAllRecords = onNavigateToAllRecords
                    )
                }

                // ==================== NEW: 네이티브 광고 아이템 (2025-12-22) ====================
                item {
                    // [FIX] 위쪽 여백 축소: 16dp → 8dp (50% 감소) (2025-12-24)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 광고 컨테이너 (좌우 여백 적용)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)
                    ) {
                        NativeAdItem()
                    }

                    // [NEW] 스크롤 유도 힌트 (False Floor 해결) (2025-12-24)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll down for more",
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.scroll_hint_more),
                                fontSize = 11.sp,
                                color = Color.Gray.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // [FIX] 아래쪽 여백 축소: 16dp → 8dp (50% 감소) (2025-12-24)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ==================== Item 4: 최근 금주 일기 섹션 ====================
                item {
                    // [FIX] 섹션 간격 축소: 20dp → 12dp (40% 감소) (2025-12-24)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = RECORDS_SCREEN_HORIZONTAL_PADDING)) {
                        RecentDiarySection(
                            diaries = recentDiaries,
                            allDiaries = allDiaries, // [NEW] 전체 일기 전달 (캘린더용) (2025-12-22)
                            onNavigateToAllDiaries = onNavigateToAllDiaries,
                            onNavigateToDiaryWrite = onNavigateToDiaryWrite, // [NEW] 일기 작성 콜백 전달
                            onDiaryClick = onDiaryClick,
                            onNavigateToDiaryDetail = onNavigateToDiaryDetail, // [NEW] 상세 화면 네비게이션 (2025-12-22)
                            // [MODIFIED] 헤더 클릭 시 고정 기간 모드로 바텀시트 열기 (2025-12-24)
                            onHeaderClick = {
                                android.util.Log.d("RecordsScreen", "캘린더 헤더 클릭 - 네비게이션 모드로 월 선택기 오픈")
                                isCalendarNavigationMode = true // [NEW] 고정 기간 모드 활성화
                                onPeriodSelected(periodMonth) // 필터 상태를 월간으로 설정
                                showBottomSheet = true // 바텀시트 표시
                            }
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
        } // [NEW] Scaffold 닫는 괄호 (2025-12-23)
    }

    // 바텀 시트: 선택된 기간에 따라 각각 다른 피커를 보여줍니다.
    if (showBottomSheet) {
        Log.d("RecordsScreen", "바텀시트 렌더링: selectedPeriod=$selectedPeriod, allRecords.size=${allRecords.size}")
        // [FIX] when (selectedPeriod) 대신 when { } 사용 - contains로 유연한 조건 체크 (2025-12-23)
        when {
            // 1. 주간 (Week) - "주", "Week" 포함 또는 정확히 periodWeek
            selectedPeriod.contains("주") || selectedPeriod.contains("Week", ignoreCase = true) || selectedPeriod == periodWeek -> {
                Log.d("RecordsScreen", "주 선택기 표시")
                WeekPickerBottomSheet(
                    isVisible = true,
                    onDismiss = {
                        Log.d("RecordsScreen", "주 선택기 닫기")
                        showBottomSheet = false
                    },
                    onWeekPicked = { weekStart, weekEnd, displayText ->
                        Log.d("RecordsScreen", "주 선택 완료: $displayText")
                        onDetailPeriodSelected(displayText)
                        onWeekRangeSelected(weekStart to weekEnd)
                        showBottomSheet = false
                    }
                )
            }

            // 2. 월간 (Month) - "월", "Month" 포함 또는 정확히 periodMonth
            selectedPeriod.contains("월") || selectedPeriod.contains("Month", ignoreCase = true) || selectedPeriod == periodMonth -> {
                Log.d("RecordsScreen", "월 선택기 표시 (네비게이션 모드: $isCalendarNavigationMode)")
                MonthPickerBottomSheet(
                    isVisible = true,
                    onDismiss = {
                        Log.d("RecordsScreen", "월 선택기 닫기")
                        showBottomSheet = false
                        isCalendarNavigationMode = false // [NEW] 모드 리셋 (2025-12-24)
                    },
                    onMonthPicked = { year, month ->
                        Log.d("RecordsScreen", "월 선택 완료: $year-$month")
                        onDetailPeriodSelected(context.getString(R.string.date_format_year_month, year, month))
                        showBottomSheet = false
                        isCalendarNavigationMode = false // [NEW] 모드 리셋 (2025-12-24)
                    },
                    records = allRecords,
                    useFixedYearRange = isCalendarNavigationMode // [NEW] 캘린더 네비게이션 모드 전달 (2025-12-24)
                )
            }

            // 3. 연간 (Year) - "년", "Year" 포함 또는 정확히 periodYear
            selectedPeriod.contains("년") || selectedPeriod.contains("Year", ignoreCase = true) || selectedPeriod == periodYear -> {
                Log.d("RecordsScreen", "년 선택기 표시")
                val initialYearForPicker = Regex("(\\d{4})").find(selectedDetailPeriod)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Calendar.getInstance().get(Calendar.YEAR)

                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = {
                        Log.d("RecordsScreen", "년 선택기 닫기")
                        showBottomSheet = false
                    },
                    onYearPicked = { year ->
                        Log.d("RecordsScreen", "년 선택 완료: $year")
                        onDetailPeriodSelected(context.getString(R.string.date_format_year, year))
                        showBottomSheet = false
                    },
                    records = allRecords,
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

    // [UPDATED] value와 unit 완전 분리 (2025-12-26)
    val savedMoneyFormatted = remember(savedMoney) {
        val formatted = CurrencyManager.formatMoneyNoDecimals(savedMoney, context)  // "Rp1,4jt"
        val locale = java.util.Locale.getDefault()
        if (locale.country.equals("ID", ignoreCase = true) || locale.language.equals("in", ignoreCase = true)) {
            "$formatted IDR"  // 인도네시아: "Rp1,4jt IDR"
        } else {
            formatted  // 다른 국가: "₩10,000" 등
        }
    }

    // [CHANGED] Floor(내림) 방식으로 소수점 계산 - 레벨 카드와 동기화 (2025-12-25)
    // 예: 1.96일 -> "1.9" (반올림 2.0 아님), 2.00일 -> "2.0"
    val daysText = remember(totalDays) {
        val displayValue = kotlin.math.floor(totalDays * 10.0) / 10.0
        String.format(java.util.Locale.getDefault(), "%.1f", displayValue)
    }

    // [UPDATED] formatCompactNumber 사용 (2025-12-26)
    val kcalText = remember(totalKcal) {
        FormatUtils.formatCompactNumber(context, totalKcal)
    }

    val bottlesText = remember(totalBottles) {
        String.format(java.util.Locale.getDefault(), "%.1f", totalBottles)
    }

    // [NEW] bottle/bottles 단수/복수 구분 (2025-12-25)
    val bottlesUnit = remember(totalBottles) {
        val count = totalBottles.toInt()
        if (count == 1) "bottle" else "bottles"
    }

    // [UPDATED] formatCompactNumber 사용 (2025-12-26)
    val kcalFormatted = FormatUtils.formatCompactNumber(context, totalKcal)


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
                        .height(140.dp) // [FIX] IntrinsicSize.Max 사용 시 SubcomposeLayout과 충돌하여 런타임 크래시 발생하므로 고정 높이로 대체 (원래 PeriodStatisticsSection에서 사용하던 값)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(RECORDS_CARD_IN_ROW_SPACING)
                ) {
                    val statsScale = 1.3f

                    // [NEW] 좌측: 줄인 칼로리 → 컷 (personsimplerun 아이콘)
                    StatisticItem(
                        title = "CALORIES",
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
                        title = "SOBER",
                        value = "$bottlesText $bottlesUnit", // [CHANGED] 단수/복수 구분 (2025-12-25)
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
                        title = "SAVED",
                        value = savedMoneyFormatted,  // [UPDATED] 인도네시아 축약형 자동 적용 (2025-12-26)
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
            verticalArrangement = Arrangement.Center // [NEW] 내용물을 카드 중앙에 수직 정렬
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

            // [UPDATED] 인도네시아 축약형 지원 - Rp로 시작하는 통화 포함 (2025-12-26)
            // 예: "Rp1,4jt IDR" → 숫자: "Rp1,4jt", 단위: "IDR"
            val regex = Regex("^\\s*([Rp$€¥₩£]*[0-9,]+(?:\\.[0-9]+)?[a-zA-Z]*)\\s*(.*)")
            val m = regex.find(value)

            if (m != null) {
                val num = m.groupValues[1]
                val unit = m.groupValues[2]

                // [FIX] 숫자와 단위를 같은 Column에 배치하여 위아래 간격을 매우 좁게 유지
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val numberBaseStyle = numStyle // 기존 스타일 재사용

                    AutoResizeSingleLineText(
                        text = num,
                        baseStyle = numberBaseStyle,
                        modifier = Modifier
                            .fillMaxWidth(),
                        step = 0.9f,
                        minFontSize = 12f,
                        color = valueColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp)) // 숫자-단위 간격을 아이콘-라벨 간격과 동일하게 2.dp로 설정

                    // 단위 텍스트: offset 제거하고 includeFontPadding을 꺼서 시각적 간격을 더 타이트하게 만듭니다.
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

                // [FIX] 단위는 위에서 Column 내부에 이미 렌더링되므로 중복 렌더링 블록을 제거했습니다.

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
            Spacer(modifier = Modifier.height(8.dp)) // [FIX] 그룹간 간격 증가: 4.dp -> 8.dp

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
 * [NEW] 일기 섹션 - 캘린더 뷰로 변경 (2025-12-22)
 * [MODIFIED] 헤더 클릭 기능 추가 (2025-12-24)
 */
@Composable
private fun RecentDiarySection(
    diaries: List<kr.sweetapps.alcoholictimer.data.room.DiaryEntity>,
    allDiaries: List<kr.sweetapps.alcoholictimer.data.room.DiaryEntity>, // [NEW] 전체 일기 (캘린더용) (2025-12-22)
    onNavigateToAllDiaries: () -> Unit = {},
    onNavigateToDiaryWrite: (Long?) -> Unit = {}, // [FIX] 날짜 파라미터 추가 (2025-12-22)
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {},
    onNavigateToDiaryDetail: (Long) -> Unit = {}, // [NEW] 일기 상세 피드 화면으로 이동 (2025-12-22)
    onHeaderClick: () -> Unit = {} // [NEW] 캘린더 헤더 클릭 이벤트 (2025-12-24)
) {
    val context = LocalContext.current
    val hasAnyDiary = allDiaries.isNotEmpty()
    val latestDiaryId = allDiaries.firstOrNull()?.id

    // [NEW] 오늘 날짜의 일기 존재 여부 확인 (2025-12-24)
    val today = remember { java.util.Calendar.getInstance() }
    val todayDiary = remember(allDiaries) {
        allDiaries.firstOrNull { diary ->
            val diaryCal = java.util.Calendar.getInstance().apply { timeInMillis = diary.timestamp }
            diaryCal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            diaryCal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // [NEW] 헤더: 제목 + 안내 문구 (2025-12-24)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.diary_recent_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    fontSize = 18.sp
                )

                if (hasAnyDiary) {
                    Text(
                        text = stringResource(R.string.records_diary_view_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                latestDiaryId?.let { onNavigateToDiaryDetail(it) }
                            }
                        )
                    )
                }
            }

            // [NEW] 안내 문구 추가 (2025-12-24)
            Text(
                text = stringResource(R.string.diary_section_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // [NEW] 캘린더 위젯 (2025-12-22)
        // [FIX] allDiaries 사용 - 모든 과거 일기 표시 (2025-12-22)
        // [MODIFIED] 헤더 클릭 기능 추가 (2025-12-24)
        kr.sweetapps.alcoholictimer.ui.tab_02.components.CalendarWidget(
            diaries = allDiaries, // [FIX] recentDiaries -> allDiaries (2025-12-22)
            onDateClick = { selectedDate ->
                // 1. [최우선] 해당 날짜에 저장된 일기가 있는지 먼저 검색
                val existingDiary = allDiaries.firstOrNull { // [FIX] allDiaries 사용
                    val diaryCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    diaryCal.get(java.util.Calendar.YEAR) == selectedDate.get(java.util.Calendar.YEAR) &&
                    diaryCal.get(java.util.Calendar.DAY_OF_YEAR) == selectedDate.get(java.util.Calendar.DAY_OF_YEAR)
                }

                // 2. 오늘 날짜 여부 판별
                val today = java.util.Calendar.getInstance()
                val isToday = selectedDate.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                              selectedDate.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)

                // 3. [핵심] 일기 존재 여부를 최우선으로 확인하는 분기 로직 (2025-12-22)
                when {
                    existingDiary != null -> {
                        // [FIX] 이미 일기가 있다면 상세 피드 화면으로 이동 (2025-12-22)
                        onNavigateToDiaryDetail(existingDiary.id)
                        android.util.Log.d("RecordsScreen", "일기 상세 피드 열기: ${existingDiary.id}")
                    }
                    isToday -> {
                        // 일기가 없고 오늘인 경우에만 새 일기 작성
                        onNavigateToDiaryWrite(null)
                        android.util.Log.d("RecordsScreen", "새 일기 작성 (오늘)")
                    }
                    else -> {
                        // 과거 날짜에 일기가 없는 경우는 아무 동작 안 함 (No-op)
                        android.util.Log.d("RecordsScreen", "과거 날짜 클릭 (일기 없음) - No action")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            onHeaderClick = onHeaderClick
        )

        // [NEW] 작성 유도 카드 (CTA Box) (2025-12-24)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (todayDiary == null) {
                        onNavigateToDiaryWrite(null)
                    } else {
                        onNavigateToDiaryDetail(todayDiary.id)
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (todayDiary != null) Color(0xFFF0F9FF) else Color(0xFFFFFBEB)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (todayDiary != null) R.string.diary_cta_completed
                            else R.string.diary_cta_empty
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (todayDiary != null) Color(0xFF1E40AF) else Color(0xFF92400E)
                    )
                    if (todayDiary == null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.diary_cta_empty_action),
                            fontSize = 13.sp,
                            color = Color(0xFFA16207)
                        )
                    }
                }

                Icon(
                    painter = painterResource(
                        id = if (todayDiary != null) R.drawable.notebook
                        else R.drawable.ic_plus
                    ),
                    contentDescription = null,
                    tint = if (todayDiary != null) Color(0xFF3B82F6) else Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
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
        // [FIX] notebook 아이콘 사용
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
 * [MODIFIED] Tab 3와 동일한 TopAppBar로 교체하여 위치 통일 (2025-12-23)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDashboardHeader() {
    TopAppBar(
        title = {
            // [핵심] buildAnnotatedString으로 2가지 색상(검정/파란색) 유지
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF111111))) {
                        append(stringResource(R.string.records_dashboard_title_health))
                    }
                    // [FIX] 명시적 공백 추가 (2025-12-23)
                    append(" ")
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF6366F1))) {
                        append(stringResource(R.string.records_dashboard_title_analysis))
                    }
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp, // [수정] 응원 챌린지와 동일한 크기로 통일
                    fontWeight = FontWeight.Bold
                )
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF111111)
        ),
        modifier = Modifier.height(48.dp), // [핵심] Tab 3와 동일한 높이
        windowInsets = WindowInsets(0, 0, 0, 0) // 시스템 바 패딩 제거
    )
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
        // 좌측: 제목
        Text(
            text = stringResource(R.string.records_statistics_summary),
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
                text = monthlyText,
                isSelected = selectedPeriod.contains("월"),
                onClick = { onPeriodSelected("월") }
            )

            // 전체 버튼
            ToggleButton(
                text = allText,
                isSelected = !selectedPeriod.contains("월"),
                onClick = { onPeriodSelected("전체") }
            )
        }
    }
}

/**
 * [NEW] 토글 버튼 (캡슐 모양)
 * [UPDATED] 인도네시아어 등 긴 텍스트 지원 (줄바꿈 방지) (2025-12-24)
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
            .defaultMinSize(minWidth = 70.dp) // [NEW] 최소 너비 확보
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280),
            maxLines = 1, // [NEW] 줄바꿈 방지
            softWrap = false, // [NEW] 강제 1줄 표시
            overflow = TextOverflow.Visible // [NEW] 텍스트가 잘리지 않게
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
            .height(140.dp) // [FIX] IntrinsicSize.Max 대신 고정 높이 사용 - SubcomposeLayout 관련 런타임 예외 방지
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 칼로리 카드
        // [UPDATED] formatCompactNumber 사용하여 큰 숫자 축약 표시 (2025-12-26)
        StatCard(
            icon = R.drawable.personsimplerun,
            iconColor = Color(0xFFFF9F66),
            label = "CALORIES",
            value = FormatUtils.formatCompactNumber(context, statsData.totalKcal),
            unit = "kcal",
            modifier = Modifier.weight(1f).fillMaxHeight() // [NEW] 자식이 부모 높이에 맞게 fillMaxHeight
        )

        // 절주 카드
        StatCard(
            icon = R.drawable.wine,
            iconColor = Color(0xFF6B9DFF),
            label = "SOBER",
            value = statsData.totalBottles.toInt().toString(),
            unit = stringResource(R.string.records_unit_bottles),
            modifier = Modifier.weight(1f).fillMaxHeight() // [NEW]
        )

        // 저축 카드
        // [UPDATED] 모든 통화의 코드를 표시하도록 수정 (2025-12-26)
        val savedMoneyValue = CurrencyManager.formatMoneyNoDecimals(statsData.savedMoney, context)
        val currencyUnit = currency.code  // 항상 통화 코드 표시 (KRW, IDR, USD, JPY 등)

        StatCard(
            icon = R.drawable.piggybank,
            iconColor = Color(0xFF5CD88A),
            label = "SAVED",
            value = savedMoneyValue,  // "Rp1,4jt" (큰 글씨)
            unit = currencyUnit,  // "IDR", "KRW", "USD" 등 (작은 회색 글씨)
            modifier = Modifier.weight(1f).fillMaxHeight()
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
                .fillMaxSize()
                .padding(8.dp), // [UNCHANGED] 내부 패딩
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // [NEW] 내용물을 카드 중앙에 수직 정렬
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
                fontWeight = FontWeight.Medium,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)) // [NEW] 폰트 패딩 제거
            )

            Spacer(modifier = Modifier.height(8.dp)) // [FIX] 그룹간 간격 증가: 2.dp -> 8.dp

            // 값 - [FIX] AutoResizingText로 자동 크기 조절 (깜빡임 없음)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 숫자 (자동 축소)
                val numberStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )

                AutoResizeSingleLineText(
                    text = value,
                    baseStyle = numberStyle,
                    modifier = Modifier.fillMaxWidth(),
                    step = 0.9f,
                    minFontSize = 12f,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp)) // 숫자-단위 간격을 아이콘-라벨 간격과 동일하게 2.dp로 설정

                // 단위 텍스트: offset 제거하고 includeFontPadding을 꺼서 시각적 간격을 더 타이트하게 만듭니다.
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
 * [NEW] 총 금주일 카드 (가로형)
 * 클릭 시 전체 기록 화면으로 이동
 */
@Composable
private fun TotalDaysCard(
    totalDays: Float,
    onNavigateToAllRecords: () -> Unit = {}
) {
    // [CHANGED] Floor(내림) 방식으로 소수점 계산 - 레벨 카드와 동기화 (2025-12-25)
    // 예: 1.96일 -> "1.9" (반올림 2.0 아님), 2.00일 -> "2.0"
    val displayValue = kotlin.math.floor(totalDays * 10.0) / 10.0

    // [FIX] 영어 로케일을 사용하여 소수점을 점(.)으로 고정 (인도네시아 쉼표 문제 해결)
    val decimalFormat = remember {
        java.text.DecimalFormat("#,###.#", java.text.DecimalFormatSymbols(java.util.Locale.US))
    }

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
                .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 12.dp), // [CHANGED] 우측 패딩만 12dp로 조정 (2025-12-26)
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 아이콘 + 제목 (긴 텍스트 대응 - 인도네시아어 등)
            Row(
                modifier = Modifier.weight(1f), // [FIX] 남은 공간만 사용 (2025-12-24)
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
                    maxLines = 1, // [FIX] 1줄 제한 (2025-12-24)
                    overflow = TextOverflow.Ellipsis // [FIX] 넘치면 ... 처리 (2025-12-24)
                )
            }

            // 중간 여백 (타이틀과 값 사이)
            Spacer(modifier = Modifier.width(16.dp))

            // 우측: 값 + 화살표 (절대 잘리지 않게)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 날짜 값 (단위 제거, 2025-12-25)
                // [CHANGED] displayValue(내림 처리된 값) 사용 (2025-12-25)
                // [FIX] AutoResizingText로 자동 크기 조절 (깜빡임 없음)
                AutoResizingText(
                    text = decimalFormat.format(displayValue),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.End,
                    minFontSize = 16.sp
                )


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
 * [UPDATED] 과거 무한 이동 방지 - 가장 오래된 기록이 있는 달까지만 이동 가능 (2025-12-25)
 */
@Composable
private fun MonthNavigator(
    selectedDetailPeriod: String,
    allRecords: List<SobrietyRecord>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: () -> Unit
) {
    // 현재 선택된 월 파싱
    val (year, month) = parseYearMonth(selectedDetailPeriod)
    val selectedYearMonth = year * 12 + month

    // 현재 날짜 (미래 방지용)
    val currentYearMonth = Calendar.getInstance().let {
        it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) + 1
    }

    // [NEW] 가장 오래된 기록의 날짜 (과거 방지용) (2025-12-25)
    val oldestTimestamp = allRecords.minOfOrNull { it.startTime } ?: System.currentTimeMillis()
    val minYearMonth = Calendar.getInstance().apply {
        timeInMillis = oldestTimestamp
    }.let {
        it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) + 1
    }

    // [FIX] 미래 날짜 선택 방지 - 선택된 달이 현재 달 이상이면 비활성화 (2025-12-25)
    val isFutureMonth = selectedYearMonth >= currentYearMonth

    // [NEW] 과거 날짜 선택 방지 - 선택된 달이 최소 달 이하이면 비활성화 (2025-12-25)
    val isPastMonth = selectedYearMonth <= minYearMonth

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 이전 달 버튼 (과거 방지)
        IconButton(
            onClick = onPreviousMonth,
            enabled = !isPastMonth, // [NEW] 최소 달이면 비활성화
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_left),
                contentDescription = "이전 달",
                tint = if (isPastMonth) Color(0xFFD1D5DB) else Color(0xFF6B7280), // [NEW] 비활성화 시 연한 회색
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // 중앙: 현재 월 (클릭 가능)
        Text(
            text = selectedDetailPeriod,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDateClick
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.width(24.dp))

        // 다음 달 버튼 (미래 방지)
        IconButton(
            onClick = onNextMonth,
            enabled = !isFutureMonth,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "다음 달",
                tint = if (isFutureMonth) Color(0xFFD1D5DB) else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * [FIXED] 날짜 문자열 파싱 헬퍼 함수 (2025-12-25)
 * - "2025년 12월", "12/2025", "2025.12" 등 다양한 형식 지원
 * - 큰 숫자(100 이상)를 연도로, 작은 숫자를 월로 자동 판별
 */
private fun parseYearMonth(dateString: String): Pair<Int, Int> {
    return try {
        // 모든 숫자 추출
        val numbers = Regex("(\\d+)").findAll(dateString).map { it.value.toInt() }.toList()

        if (numbers.size >= 2) {
            // [FIX] 큰 숫자를 연도로, 작은 숫자를 월로 자동 판별 (ViewModel과 동일)
            val num1 = numbers[0]
            val num2 = numbers[1]
            val year = if (num1 > 100) num1 else num2
            val month = if (num1 > 100) num2 else num1

            Pair(year, month)
        } else {
            // 파싱 실패 시 현재 날짜 반환
            val now = Calendar.getInstance()
            Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        }
    } catch (e: Exception) {
        // 예외 발생 시 현재 날짜 반환
        val now = Calendar.getInstance()
        Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }
}

/**
 * [NEW] 네이티브 광고 아이템 (2025-12-22)
 * - RecordsScreen 중간에 삽입하여 섹션 분리 역할
 * - 다른 통계 카드들과 동일한 스타일 (Shadow, Radius 16dp)
 * - [FIX] 고정 높이로 UI 흔들림 방지 (2025-12-22)
 */
@Composable
private fun NativeAdItem() {
    val context = LocalContext.current

    val adUnitId = try { kr.sweetapps.alcoholictimer.BuildConfig.ADMOB_NATIVE_ID } catch (_: Throwable) { "ca-app-pub-3940256099942544/2247696110" }

    var nativeAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
    // [NEW] 광고 로드 실패 플래그 (No Fill 대응, 2025-12-24)
    var adLoadFailed by remember { mutableStateOf(false) }

    // 1. 광고 로드 로직
    LaunchedEffect(Unit) {
        try {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context)
            } catch (initEx: Exception) {
                android.util.Log.w("NativeAd", "MobileAds.initialize failed: ${initEx.message}")
            }
            val adLoader = com.google.android.gms.ads.AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad: com.google.android.gms.ads.nativead.NativeAd ->
                    nativeAd = ad
                }
                // [NEW] 광고 로드 실패 리스너 추가 (No Fill 대응, 2025-12-24)
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        android.util.Log.w("NativeAd", "Ad load failed (No Fill): ${error.message}")
                        adLoadFailed = true // [핵심] UI 숨김 플래그
                    }
                })
                .withNativeAdOptions(com.google.android.gms.ads.nativead.NativeAdOptions.Builder().build())
                .build()

            try {
                adLoader.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            } catch (se: SecurityException) {
                android.util.Log.w("NativeAd", "Ad load blocked by SecurityException: ${se.message}")
                adLoadFailed = true // [추가] SecurityException도 실패로 처리
            }
        } catch (e: Exception) {
            android.util.Log.e("NativeAd", "Failed setting up ad loader", e)
            adLoadFailed = true // [추가] 예외 발생 시 실패로 처리
        }
    }

    // [NEW] 광고 로드 실패 시 UI 아예 숨김 (Graceful Degradation, 2025-12-24)
    if (adLoadFailed) {
        return // 광고 영역 렌더링하지 않음
    }

    // 2. [FIX] 광고 로딩 상태에 따른 높이 조절로 레이아웃 시프트 방지 (2025-12-23)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // [핵심] 로딩 중(null)이면 고정 높이, 로딩 완료 후 wrapContentHeight
            .then(
                if (nativeAd == null) Modifier.height(250.dp)
                else Modifier.wrapContentHeight()
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (nativeAd != null) {
            // 광고 로드 완료 시
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    val adView = com.google.android.gms.ads.nativead.NativeAdView(ctx)

                    // 내부 레이아웃 - 내용물에 맞게 높이 조절
                    val container = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setBackgroundColor(android.graphics.Color.WHITE)
                        setPadding(40, 40, 40, 40)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT // [FIX] 내용물 크기에 맞춤
                        )
                    }

                    // 1) 상단: 아이콘 + 광고 배지 + 헤드라인
                    val headerRow = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    val iconView = android.widget.ImageView(ctx).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(110, 110)
                    }
                    headerRow.addView(iconView)

                    // [NEW] 텍스트 컨테이너 (배지 + 제목을 세로로 배치) (2025-12-23)
                    val textContainer = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = 24
                        }
                    }

                    // [NEW] ★ 광고 배지 (Ad Badge) 추가 ★ (2025-12-23)
                    val badgeView = android.widget.TextView(ctx).apply {
                        text = "광고"
                        textSize = 10f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#FBC02D"))
                        setPadding(8, 2, 8, 2)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 4
                        }
                    }
                    textContainer.addView(badgeView)

                    val headlineView = android.widget.TextView(ctx).apply {
                        textSize = 15f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#111827"))
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    textContainer.addView(headlineView)

                    headerRow.addView(textContainer)
                    container.addView(headerRow)

                    // 2) 중간: Body
                    val bodyView = android.widget.TextView(ctx).apply {
                        textSize = 13f
                        setPadding(0, 24, 0, 32)
                        setTextColor(android.graphics.Color.parseColor("#6B7280"))
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    container.addView(bodyView)

                    // 3) 하단: 버튼
                    val callToActionView = android.widget.Button(ctx).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"))
                        setTextColor(android.graphics.Color.parseColor("#4B5563"))
                        textSize = 13f
                        stateListAnimator = null
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    container.addView(callToActionView)

                    adView.addView(container)
                    adView.iconView = iconView
                    adView.headlineView = headlineView
                    adView.bodyView = bodyView
                    adView.callToActionView = callToActionView
                    adView
                },
                update = { adView ->
                    val ad = nativeAd!!
                    (adView.headlineView as android.widget.TextView).text = ad.headline
                    (adView.bodyView as android.widget.TextView).text = ad.body
                    (adView.callToActionView as android.widget.Button).text = ad.callToAction ?: "자세히 보기"
                    if (ad.icon != null) {
                        (adView.iconView as android.widget.ImageView).setImageDrawable(ad.icon?.drawable)
                        adView.iconView?.visibility = android.view.View.VISIBLE
                    } else {
                        adView.iconView?.visibility = android.view.View.GONE
                    }
                    adView.setNativeAd(ad)
                },
                modifier = Modifier.fillMaxSize() // [FIX] 카드 전체 영역 사용
            )
        } else {
            // [NEW] 광고 로딩 중 표시될 Placeholder (스켈레톤) (2025-12-22)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FAFB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ad",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
    }
}
