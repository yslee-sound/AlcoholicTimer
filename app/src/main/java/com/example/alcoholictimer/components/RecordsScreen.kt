package com.example.alcoholictimer.components

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.alcoholictimer.ui.theme.AlcoholicTimerTheme
import com.example.alcoholictimer.utils.DateOverlapUtils
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import com.example.alcoholictimer.ui.StandardScreen
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    externalRefreshTrigger: Int,
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    fontScale: Float = 1.06f
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 현재 월을 기본값으로 설정
    val currentDate = Calendar.getInstance()
    val currentYear = currentDate.get(Calendar.YEAR)
    val currentMonth = currentDate.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작하므로 +1

    var selectedPeriod by remember { mutableStateOf("월") } // "전체"에서 "월"로 변경
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDetailPeriod by remember { mutableStateOf("${currentYear}년 ${currentMonth}월") } // 현재 월로 기본값 설정
    // 주 선택 시 실제 범위를 저장 (일요일 00:00 ~ 토요일 23:59:59.999)
    var selectedWeekRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    // 데이터 로딩 함수
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

    // Activity Result Launcher for AddTestRecordActivity
    val addTestRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 새 기록이 추가되었으므로 목록을 새로고침
            loadRecords()
        }
    }

    // 기간에 따른 기록 필터링 (통계용)
    val filteredRecords = remember(records, selectedPeriod, selectedDetailPeriod, selectedWeekRange) {
        when (selectedPeriod) {
            "주" -> {
                // 선택된 주 범위가 있으면 그대로 사용
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
                // 주간 겹침 판정으로 필터링
                records.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            "월" -> {
                // 월 범위 계산 후, 해당 월과 겹치는 모든 기록 포함
                val range: Pair<Long, Long> = if (selectedDetailPeriod.isNotEmpty()) {
                    val regex = Regex("(\\d{4})년 (\\d{1,2})월")
                    val match = regex.find(selectedDetailPeriod)
                    if (match != null) {
                        val year = match.groupValues[1].toInt()
                        val month = match.groupValues[2].toInt() - 1 // Calendar.MONTH는 0부터 시작
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
            "년" -> {
                // 년 범위 계산 후, 해당 년과 겹치는 모든 기록 포함
                val range: Pair<Long, Long> = if (selectedDetailPeriod.isNotEmpty()) {
                    val regex = Regex("(\\d{4})년")
                    val match = regex.find(selectedDetailPeriod)
                    if (match != null) {
                        val year = match.groupValues[1].toInt()
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
            else -> records // "전체"
        }
    }

    // 카드 영역: 최신 5개만 별도로 추출
    val latestRecords = remember(records) {
        records.sortedByDescending { it.endTime }.take(5)
    }

    // 초기 로딩 및 외부 트리거에 따른 새로고침
    LaunchedEffect(externalRefreshTrigger) {
        loadRecords()
    }

    StandardScreen {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = LocalDensity.current.fontScale * fontScale)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    // 기간 선택 탭 섹션
                    PeriodSelectionSection(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { period: String ->
                            selectedPeriod = period
                            // 기간이 변경되면 세부 기간만 초기화 (기본값으로 리셋하지 않음)
                            selectedDetailPeriod = ""
                        },
                        onPeriodClick = { period: String ->
                            // 세부 기간 텍스트 클릭 시 바텀시트 표시
                            showBottomSheet = true
                        },
                        selectedDetailPeriod = selectedDetailPeriod
                    )
                }
                item {
                    // 해당 기간에 대한 정보를 보여주는 섹션
                    if (!isLoading) {
                        PeriodStatisticsSection(
                            records = filteredRecords,
                            selectedPeriod = selectedPeriod,
                            selectedDetailPeriod = selectedDetailPeriod,
                            modifier = Modifier.padding(vertical = 8.dp),
                            onAddTestRecord = {
                                // AddTestRecordActivity로 이동
                                val intent = Intent(context, AddTestRecordActivity::class.java)
                                addTestRecordLauncher.launch(intent)
                            },
                            weekRange = selectedWeekRange // 주간 범위 전달
                        )
                    }
                }
                item {
                    // 로딩/빈 상태/기록 목록
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (records.isEmpty()) {
                        EmptyRecordsState()
                    }
                }
                if (!isLoading && latestRecords.isNotEmpty()) {
                    items(latestRecords) { record ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp) // 카드 상하 여백만 유지
                        ) {
                            RecordSummaryCard(
                                record = record,
                                compact = false, // 더 큰 타입오 사용
                                headerIconSizeDp = 56.dp,
                                onClick = { onNavigateToDetail(record) }
                            )
                        }
                    }
                    if (records.isNotEmpty()) {
                        item {
                            Button(
                                onClick = onNavigateToAllRecords,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "모든 기록 보기 (${records.size}개)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // 바텀시트 표시
    if (showBottomSheet) {
        when (selectedPeriod) {
            "주" -> {
                WeekPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onWeekPicked = { weekStart, weekEnd, displayText ->
                        selectedDetailPeriod = displayText // UX용 표시 텍스트
                        selectedWeekRange = weekStart to weekEnd // 실제 필터 범위
                        showBottomSheet = false
                    }
                )
            }
            "월" -> {
                MonthPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onMonthPicked = { year, month ->
                        selectedDetailPeriod = "${year}년 ${month}월" // 읽기 좋은 형태로 저장
                        showBottomSheet = false
                    },
                    records = records,
                    onYearPicked = { year ->
                        selectedPeriod = "년"
                        selectedDetailPeriod = "${year}년"
                        showBottomSheet = false
                    }
                )
            }
            "년" -> {
                // 선택된 상세 기간에서 연도 추출(있으면), 없으면 현재 연도
                val initialYearForPicker =
                    Regex("(\\d{4})년").find(selectedDetailPeriod)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: Calendar.getInstance().get(Calendar.YEAR)

                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onYearPicked = { year ->
                        selectedDetailPeriod = "${year}년" // 읽기 좋은 형태로 저장
                        showBottomSheet = false
                    },
                    records = records,
                    initialYear = initialYearForPicker
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "빈 상태",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "아직 금주 기록이 없습니다",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit
) {
    RecordSummaryCard(
        record = record,
        onClick = onClick
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    onAddTestRecord: () -> Unit = {},
    weekRange: Pair<Long, Long>? = null
) {
    // 통계 계산 - 수정된 로직 (모든 기간에서 겹치는 구간만 합산)
    val totalRecords = records.size

    // 현재 선택된 기간의 시간 범위 계산 (전체는 null)
    val periodRange: Pair<Long, Long>? = remember(selectedPeriod, selectedDetailPeriod, weekRange) {
        when (selectedPeriod) {
            "주" -> {
                // 주 범위가 전달되었으면 사용, 없으면 이번 주로 계산
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
            "월" -> {
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
            "년" -> {
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

    // 지정된 기간과 겹치는 일수(일 단위)를 계산하는 함수
    fun overlappedDays(record: SobrietyRecord): Double {
        return if (periodRange == null) {
            DateOverlapUtils.overlapDays(record.startTime, record.endTime, null, null)
        } else {
            DateOverlapUtils.overlapDays(record.startTime, record.endTime, periodRange.first, periodRange.second)
        }
    }

    // 각 기록의 실제 달성률을 평균으로 계산 (기간 겹침 반영)
    val successRate = if (totalRecords > 0) {
        val totalProgressPercent = records.sumOf { record ->
            val actualDurationDays = overlappedDays(record).toFloat()
            val progressPercent = if (record.targetDays > 0) {
                ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f)
            } else {
                record.percentage?.toFloat() ?: ((actualDurationDays / 30f) * 100f).coerceIn(0f, 100f)
            }
            progressPercent.toDouble()
        }
        com.example.alcoholictimer.utils.PercentUtils.roundPercent(totalProgressPercent / totalRecords)
    } else 0

    // 총 누적 일수: 소수 1자리까지 표시 (겹치는 기간만 합산)
    val totalDaysDouble = records.sumOf { record -> overlappedDays(record) }
    val totalDaysDisplay = String.format(Locale.getDefault(), "%.1f", totalDaysDouble)

    // 평균/최대 지속일 (UI는 정수 표기 유지, 겹치는 기간 기준)
    val averageDaysDisplay = if (totalRecords > 0) {
        String.format(Locale.getDefault(), "%.1f", records.map { record -> overlappedDays(record) }.average())
    } else "0.0"

    val maxDaysDisplay = if (records.isNotEmpty()) {
        String.format(Locale.getDefault(), "%.1f", records.maxOf { record -> overlappedDays(record) })
    } else "0.0"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedPeriod) {
                        "주" -> "주 통계"
                        "월" -> "월 통계"
                        "년" -> "년 통계"
                        else -> "전체 통계"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onAddTestRecord,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "테스트 기록 추가",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 통계 그리드
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 모든 기간에서 동일하게 1.3배 확대 적용
                val statsScale = 1.3f

                // 성공률
                StatisticItem(
                    title = "성공률\n ",
                    value = "$successRate%",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    titleScale = statsScale,
                    valueScale = statsScale
                )

                // 평균 지속일
                StatisticItem(
                    title = "평균\n지속일",
                    value = "${averageDaysDisplay}일",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    titleScale = statsScale,
                    valueScale = statsScale
                )

                // 최대 지속일
                StatisticItem(
                    title = "최대\n지속일",
                    value = "${maxDaysDisplay}일",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    titleScale = statsScale,
                    valueScale = statsScale
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 총 누적 일수
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "총 누적 금주일",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${totalDaysDisplay}일",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    // 글꼴 확장 비율 (기본 1.0f). 월 통계에서만 1.3f로 전달
    titleScale: Float = 1.0f,
    valueScale: Float = 1.0f
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Row 높이에 의존하지 않고, 각 카드의 최소 높이를 고정하여 균일하게 맞춤
            .defaultMinSize(minHeight = 96.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 숫자 영역: 고정 높이 박스를 스케일에 맞춰 증가
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp * valueScale),
                contentAlignment = Alignment.Center
            ) {
                val baseTitleMedium = MaterialTheme.typography.titleMedium
                val scaledValueTextStyle = baseTitleMedium
                    .copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = baseTitleMedium.fontSize * valueScale
                    )
                    .merge(
                        androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )

                Text(
                    text = value,
                    style = scaledValueTextStyle,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 제목: 2줄 표시, 줄간격을 더 띄우기 위해 lineHeight를 폰트 크기의 1.3배로 설정
            run {
                val baseLabel = MaterialTheme.typography.labelMedium
                val scaledLabelFontSize = baseLabel.fontSize * titleScale
                val scaledLabelStyle = baseLabel.copy(
                    fontSize = scaledLabelFontSize,
                    lineHeight = scaledLabelFontSize * 1.3f
                )
                Text(
                    text = title,
                    style = scaledLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "금주 기록 화면 - 빈 상태")
@Preview(showBackground = true, name = "금주 기록 화면 - 데이터 있음", fontScale = 1.2f)
@Composable
fun PreviewRecordsScreen() {
    AlcoholicTimerTheme {
        RecordsScreen(
            externalRefreshTrigger = 0,
            onNavigateToDetail = {}
        )
    }
}

@Preview(showBackground = true, name = "빈 상태")
@Composable
fun PreviewEmptyRecordsState() {
    AlcoholicTimerTheme {
        EmptyRecordsState()
    }
}

@Preview(showBackground = true, name = "기록 카드")
@Composable
fun PreviewRecordCard() {
    AlcoholicTimerTheme {
        RecordCard(
            record = SobrietyRecord(
                id = "sample",
                startTime = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L),
                endTime = System.currentTimeMillis(),
                targetDays = 30,
                actualDays = 10,
                isCompleted = false,
                status = "진행 중",
                createdAt = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "기간 통계 섹션")
@Composable
fun PreviewPeriodStatisticsSection() {
    AlcoholicTimerTheme {
        PeriodStatisticsSection(
            records = listOf(
                SobrietyRecord(
                    id = "1",
                    startTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L),
                    endTime = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L),
                    targetDays = 30,
                    actualDays = 10,
                    isCompleted = false,
                    status = "실패",
                    createdAt = System.currentTimeMillis()
                ),
                SobrietyRecord(
                    id = "2",
                    startTime = System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000L),
                    endTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L),
                    targetDays = 30,
                    actualDays = 30,
                    isCompleted = true,
                    status = "성공",
                    createdAt = System.currentTimeMillis()
                )
            ),
            selectedPeriod = "월",
            selectedDetailPeriod = "2024년 8월",
            modifier = Modifier.padding(16.dp)
        )
    }
}
