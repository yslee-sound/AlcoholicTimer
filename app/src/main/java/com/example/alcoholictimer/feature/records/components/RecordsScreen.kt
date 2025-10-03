package com.example.alcoholictimer.feature.records.components

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
import androidx.compose.material.icons.filled.Add
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
import com.example.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import com.example.alcoholictimer.core.util.DateOverlapUtils
import com.example.alcoholictimer.core.data.RecordsDataLoader
import com.example.alcoholictimer.core.model.SobrietyRecord
import com.example.alcoholictimer.core.ui.StandardScreen
import java.util.*
import com.example.alcoholictimer.feature.addrecord.AddRecordActivity
import com.example.alcoholictimer.core.util.PercentUtils

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

    val currentDate = Calendar.getInstance()
    val currentYear = currentDate.get(Calendar.YEAR)
    val currentMonth = currentDate.get(Calendar.MONTH) + 1

    var selectedPeriod by remember { mutableStateOf("월") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDetailPeriod by remember { mutableStateOf("${currentYear}년 ${currentMonth}월") }
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
            "주" -> {
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
            "월" -> {
                val range: Pair<Long, Long> = if (selectedDetailPeriod.isNotEmpty()) {
                    val regex = Regex("(\\d{4})년 (\\d{1,2})월")
                    val match = regex.find(selectedDetailPeriod)
                    if (match != null) {
                        val year = match.groupValues[1].toInt()
                        val month = match.groupValues[2].toInt() - 1
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
            else -> records
        }
    }

    val latestRecords = remember(records) {
        records.sortedByDescending { it.endTime }.take(5)
    }

    LaunchedEffect(externalRefreshTrigger) { loadRecords() }

    val addRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) { loadRecords() }
    }

    StandardScreen {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = LocalDensity.current.fontScale * fontScale)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    PeriodSelectionSection(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { period: String ->
                            selectedPeriod = period
                            selectedDetailPeriod = ""
                        },
                        onPeriodClick = { _ ->
                            showBottomSheet = true
                        },
                        selectedDetailPeriod = selectedDetailPeriod
                    )
                }
                item {
                    if (!isLoading) {
                        PeriodStatisticsSection(
                            records = filteredRecords,
                            selectedPeriod = selectedPeriod,
                            selectedDetailPeriod = selectedDetailPeriod,
                            modifier = Modifier.padding(vertical = 8.dp),
                            weekRange = selectedWeekRange,
                            onAddRecord = {
                                val intent = Intent(context, AddRecordActivity::class.java)
                                addRecordLauncher.launch(intent)
                            }
                        )
                    }
                }
                item {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                    } else if (records.isEmpty()) {
                        EmptyRecordsState()
                    }
                }
                if (!isLoading && latestRecords.isNotEmpty()) {
                    items(latestRecords) { record ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RecordSummaryCard(
                                record = record,
                                compact = false,
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
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showBottomSheet) {
        when (selectedPeriod) {
            "주" -> {
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
            "월" -> {
                MonthPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onMonthPicked = { year, month ->
                        selectedDetailPeriod = "${year}년 ${month}월"
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
                val initialYearForPicker =
                    Regex("(\\d{4})년").find(selectedDetailPeriod)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: Calendar.getInstance().get(Calendar.YEAR)

                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onYearPicked = { year ->
                        selectedDetailPeriod = "${year}년"
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
) { RecordSummaryCard(record = record, onClick = onClick) }

// ↓ 아래는 루트 파일의 하단 절반(PeriodStatisticsSection 등) 그대로 이식
@Suppress("UNUSED_PARAMETER")
@Composable
private fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    weekRange: Pair<Long, Long>? = null,
    onAddRecord: () -> Unit = {}
) {
    val totalRecords = records.size
    val periodRange: Pair<Long, Long>? = remember(selectedPeriod, selectedDetailPeriod, weekRange) {
        when (selectedPeriod) {
            "주" -> {
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

    fun overlappedDays(record: SobrietyRecord): Double {
        return if (periodRange == null) {
            DateOverlapUtils.overlapDays(record.startTime, record.endTime, null, null)
        } else {
            DateOverlapUtils.overlapDays(record.startTime, record.endTime, periodRange.first, periodRange.second)
        }
    }

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
        PercentUtils.roundPercent(totalProgressPercent / totalRecords)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
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

                if (selectedPeriod == "월") {
                    Button(
                        onClick = onAddRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Icon(imageVector = Icons.Filled.Add, contentDescription = "금주 기록 추가", tint = MaterialTheme.colorScheme.onPrimary) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val statsScale = 1.3f
                StatisticItem(
                    title = "성공률\n ",
                    value = "$successRate%",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    titleScale = statsScale,
                    valueScale = statsScale
                )
                StatisticItem(
                    title = "평균\n지속일",
                    value = "${averageDaysDisplay}일",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    titleScale = statsScale,
                    valueScale = statsScale
                )
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
    titleScale: Float = 1.0f,
    valueScale: Float = 1.0f
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp * valueScale),
                contentAlignment = Alignment.Center
            ) {
                val baseTitleMedium = MaterialTheme.typography.titleMedium
                val scaledValueTextStyle = baseTitleMedium
                    .copy(fontWeight = FontWeight.Bold, fontSize = baseTitleMedium.fontSize * valueScale)
                    .merge(androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)))
                Text(text = value, style = scaledValueTextStyle, color = color, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(6.dp))
            run {
                val baseLabel = MaterialTheme.typography.labelMedium
                val scaledLabelFontSize = baseLabel.fontSize * titleScale
                val scaledLabelStyle = baseLabel.copy(fontSize = scaledLabelFontSize, lineHeight = scaledLabelFontSize * 1.3f)
                Text(text = title, style = scaledLabelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Preview(showBackground = true, name = "금주 기록 화면 - 빈 상태")
@Preview(showBackground = true, name = "금주 기록 화면 - 데이터 있음", fontScale = 1.2f)
@Composable
fun PreviewRecordsScreen() {
    AlcoholicTimerTheme {
        RecordsScreen(externalRefreshTrigger = 0, onNavigateToDetail = {})
    }
}

@Preview(showBackground = true, name = "빈 상태")
@Composable
fun PreviewEmptyRecordsState() {
    AlcoholicTimerTheme { EmptyRecordsState() }
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
