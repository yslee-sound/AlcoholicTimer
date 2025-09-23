package com.example.alcoholictimer.components

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import com.example.alcoholictimer.ui.StandardScreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    externalRefreshTrigger: Int,
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {}
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
    val filteredRecords = remember(records, selectedPeriod, selectedDetailPeriod) {
        when (selectedPeriod) {
            "주" -> {
                if (selectedDetailPeriod.isNotEmpty()) {
                    // 특정 주 필터링 로직 (예: "2025-08-10 ~ 2025-08-16")
                    val parts = selectedDetailPeriod.split("~").map { it.trim() }
                    if (parts.size == 2) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val weekStart = try { sdf.parse(parts[0])?.time ?: 0L } catch (_: Exception) { 0L }
                        val weekEnd = try { sdf.parse(parts[1])?.time?.plus(24*60*60*1000L-1) ?: Long.MAX_VALUE } catch (_: Exception) { Long.MAX_VALUE }
                        records.filter { it.startTime in weekStart..weekEnd }
                    } else records
                } else {
                    // 이번 주(일요일~토요일) 시작~끝
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    val weekStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_WEEK, 6)
                    val weekEnd = cal.timeInMillis + (24*60*60*1000L-1)
                    records.filter { it.startTime in weekStart..weekEnd }
                }
            }
            "월" -> {
                if (selectedDetailPeriod.isNotEmpty()) {
                    // 특정 월 필터링 로직 (예: "2025년 8월")
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
                        records.filter { it.startTime in monthStart..monthEnd }
                    } else records
                } else {
                    // 이번 달 1일~말일
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
                    records.filter { it.startTime in monthStart..monthEnd }
                }
            }
            "년" -> {
                if (selectedDetailPeriod.isNotEmpty()) {
                    // 특정 년 필터링 로직 (예: "2025년")
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
                        records.filter { it.startTime in yearStart..yearEnd }
                    } else records
                } else {
                    // 올해 1월 1일~12월 31일
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
                    records.filter { it.startTime in yearStart..yearEnd }
                }
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
                        }
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
                            color = Color(0xFF74B9FF)
                        )
                    }
                } else if (records.isEmpty()) { // filteredRecords 대신 전체 records로 변경
                    EmptyRecordsState(selectedPeriod, selectedDetailPeriod)
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
                            onClick = { onNavigateToDetail(record) },
                            containerColor = Color.White,
                            showTimeRow = false,
                            datePattern = "yyyy.MM.dd",
                            numberColor = Color(0xFF2C3E50),
                            rateColorCompleted = Color(0xFF00B894),
                            rateColorInProgress = Color(0xFF74B9FF)
                        )
                    }
                }
                if (records.size > 5) {
                    item {
                        Button(
                            onClick = onNavigateToAllRecords,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF74B9FF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "모든 기록 보기 (${records.size}개)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
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

    // 바텀시트 표시
    if (showBottomSheet) {
        when (selectedPeriod) {
            "주" -> {
                WeekPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onWeekPicked = { weekStart, weekEnd, displayText ->
                        selectedDetailPeriod = displayText // 바텀시트에서 제공하는 displayText 사용
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
                    }
                )
            }
            "년" -> {
                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onYearPicked = { year ->
                        selectedDetailPeriod = "${year}년" // 읽기 좋은 형태로 저장
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordsState(selectedPeriod: String, @Suppress("UNUSED_PARAMETER") selectedSubPeriod: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF74B9FF).copy(alpha = 0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "빈 상태",
                    tint = Color(0xFF74B9FF),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "아직 금주 기록이 없습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "금주를 시작하고 완료하면\n기록이 여기에 표시됩니다",
            fontSize = 14.sp,
            color = Color(0xFF636E72),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 선택된 기간에 대한 안내 문구
        if (selectedPeriod != "전체") {
            Text(
                text = "선택한 기간: $selectedPeriod",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit
) {
    RecordSummaryCard(
        record = record,
        onClick = onClick,
        containerColor = Color.White,
        showTimeRow = false,
        datePattern = "yyyy.MM.dd",
        numberColor = Color(0xFF2C3E50),
        rateColorCompleted = Color(0xFF00B894),
        rateColorInProgress = Color(0xFF74B9FF)
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier,
    onAddTestRecord: () -> Unit = {}
) {
    // 통계 계산 - 수정된 로직
    val totalRecords = records.size

    // 각 기록의 실제 달성률을 평균으로 계산
    val successRate = if (totalRecords > 0) {
        val totalProgressPercent = records.sumOf { record ->
            val actualDurationDays = (record.endTime - record.startTime) / (24 * 60 * 60 * 1000f)
            val progressPercent = if (record.targetDays > 0) {
                ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f)
            } else {
                record.percentage?.toFloat() ?: 0f
            }
            progressPercent.toDouble()
        }
        (totalProgressPercent / totalRecords).toInt()
    } else 0

    // 실제 시간 기반으로 총 일수 계산
    val totalDays = records.sumOf { record ->
        val duration = record.endTime - record.startTime
        (duration / (24 * 60 * 60 * 1000.0)).toInt()
    }

    // 실제 시간 기반으로 평균 지속일 계산
    val averageDays = if (totalRecords > 0) {
        val avgDuration = records.map { record ->
            (record.endTime - record.startTime) / (24 * 60 * 60 * 1000.0)
        }.average()
        avgDuration.toInt()
    } else 0

    // 실제 시간 기반으로 최대 지속일 계산
    val maxDays = if (records.isNotEmpty()) {
        records.maxOf { record ->
            val duration = record.endTime - record.startTime
            (duration / (24 * 60 * 60 * 1000.0)).toInt()
        }
    } else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )

                Button(
                    onClick = onAddTestRecord,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF74B9FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "테스트 기록 추가",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 통계 그리드
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 성공률
                StatisticItem(
                    title = "성공률",
                    value = "$successRate%",
                    color = Color(0xFF00B894),
                    modifier = Modifier.weight(1f)
                )

                // 평균 지속일
                StatisticItem(
                    title = "평균 지속일",
                    value = "${averageDays}일",
                    color = Color(0xFF74B9FF),
                    modifier = Modifier.weight(1f)
                )

                // 최대 지속일
                StatisticItem(
                    title = "최대 지속일",
                    value = "${maxDays}일",
                    color = Color(0xFFE17055),
                    modifier = Modifier.weight(1f)
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
                    fontSize = 14.sp,
                    color = Color(0xFF636E72)
                )
                Text(
                    text = "${totalDays}일",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF636E72),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, name = "금주 기록 화면 - 빈 상태")
@Preview(showBackground = true, name = "금주 기록 화면 - 데이터 있음", fontScale = 1.2f)
@Composable
fun PreviewRecordsScreen() {
    MaterialTheme {
        // 샘플 데이터로 프리뷰
        RecordsScreen(
            externalRefreshTrigger = 0,
            onNavigateToDetail = {}
        )
    }
}

@Preview(showBackground = true, name = "빈 상태")
@Composable
fun PreviewEmptyRecordsState() {
    MaterialTheme {
        EmptyRecordsState("전체", "")
    }
}

@Preview(showBackground = true, name = "기록 카드")
@Composable
fun PreviewRecordCard() {
    MaterialTheme {
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
    MaterialTheme {
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
