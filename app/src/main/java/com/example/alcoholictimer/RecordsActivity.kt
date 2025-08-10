package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.components.MonthPickerBottomSheet
import com.example.alcoholictimer.components.WeekPickerBottomSheet
import com.example.alcoholictimer.components.YearPickerBottomSheet
import com.example.alcoholictimer.utils.SobrietyRecord
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RecordsActivity : BaseActivity() {

    // 디버깅용 태그
    companion object {
        private const val TAG = "RecordsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RecordsScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 나타날 때마다 데이터를 새로고침
        Log.d(TAG, "onResume: 기록 화면이 다시 나타남 - 데이터 새로고침")
    }

    override fun getScreenTitle(): String = "금주 기록"

    @Composable
    private fun RecordsScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
        var selectedPeriod by remember { mutableStateOf("월") }
        var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
        var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
        var selectedRange by remember { mutableStateOf("${selectedYear}년 ${selectedMonth}월") }
        var showMonthPicker by remember { mutableStateOf(false) }
        var showWeekPicker by remember { mutableStateOf(false) }
        var showYearPicker by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }
        var isRefreshing by remember { mutableStateOf(false) }
        val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

        // 테스트 기록 입력 다이얼로그 상태
        var showTestDialog by remember { mutableStateOf(false) }
        var inputStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var inputEndTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var inputTargetDays by remember { mutableStateOf(30) }
        var inputActualDays by remember { mutableStateOf(30) }
        var inputIsCompleted by remember { mutableStateOf(true) }

        fun addCustomTestRecord() {
            try {
                val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
                val recordsList = try {
                    JSONArray(recordsJson)
                } catch (e: Exception) {
                    JSONArray()
                }
                val testRecord = JSONObject().apply {
                    put("id", "test_${System.currentTimeMillis()}")
                    put("startTime", inputStartTime)
                    put("endTime", inputEndTime)
                    put("targetDays", inputTargetDays)
                    put("actualDays", inputActualDays)
                    put("isCompleted", inputIsCompleted)
                    put("status", if (inputIsCompleted) "완료" else "중지")
                    put("createdAt", System.currentTimeMillis())
                }
                recordsList.put(testRecord)
                sharedPref.edit().apply {
                    putString("sobriety_records", recordsList.toString())
                    apply()
                }
            } catch (_: Exception) {}
        }

        // 데이터 로드 함수
        suspend fun loadRecords() {
            isRefreshing = true
            try {
                // 약간의 지연을 추가하여 새로고침 애니메이션을 보여줌
                delay(500)
                records = loadSobrietyRecords(context)
                Log.d(TAG, "========== 기록 로딩 디버깅 ==========")
                Log.d(TAG, "새로고침: 로드된 기록: ${records.size}개")

                // 디버깅: 각 기록의 상세 정보 출력
                records.forEachIndexed { index, record ->
                    Log.d(TAG, "기록 $index: id=${record.id}")
                    Log.d(TAG, "  목표=${record.targetDays}일, 달성=${record.actualDays}일")
                    Log.d(TAG, "  완료=${record.isCompleted}, 상태=${record.status}")
                    Log.d(TAG, "  시작=${record.startTime}, 종료=${record.endTime}")
                    Log.d(TAG, "  생성=${record.createdAt}")
                }
                Log.d(TAG, "====================================")
            } finally {
                isRefreshing = false
            }
        }

        // 초기 기록 로드 및 화면이 다시 나타날 때마다 새로고침
        LaunchedEffect(refreshTrigger) {
            loadRecords()
        }

        // 화면이 다시 나타날 때 새로고침 (onResume 대신 LaunchedEffect 사용)
        LaunchedEffect(Unit) {
            loadRecords()
        }

        // 선택된 연/월에 따라 데이터 필터링
        val filteredRecords = remember(records, selectedYear, selectedMonth, selectedPeriod) {
            Log.d(TAG, "필터링 적용: 기간=$selectedPeriod, 전체 기록=${records.size}개")
            // 최근 활동(전체)에서는 조건 없이 모든 기록을 보여줌
            records
        }

        // MonthPickerBottomSheet
        MonthPickerBottomSheet(
            isVisible = showMonthPicker,
            onDismiss = { showMonthPicker = false },
            onMonthPicked = { year, month ->
                selectedYear = year
                selectedMonth = month
                selectedRange = "${year}년 ${month}월"
                Log.d(TAG, "선택된 연/월: ${year}년 ${month}월")
            },
            initialYear = selectedYear,
            initialMonth = selectedMonth
        )

        // WeekPickerBottomSheet
        WeekPickerBottomSheet(
            isVisible = showWeekPicker,
            onDismiss = { showWeekPicker = false },
            onWeekPicked = { weekStart, weekEnd, displayText ->
                selectedRange = displayText
                Log.d(TAG, "선택된 주: $displayText")
            }
        )

        // YearPickerBottomSheet
        YearPickerBottomSheet(
            isVisible = showYearPicker,
            onDismiss = { showYearPicker = false },
            onYearPicked = { year ->
                selectedYear = year
                selectedRange = "${year}년"
                Log.d(TAG, "선택된 연: ${year}년")
            },
            initialYear = selectedYear
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    // Pull-to-Refresh 새로고침 실행
                    coroutineScope.launch {
                        loadRecords()
                    }
                }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 상단: 기간 선택 탭 섹션 (새로고침 버튼 제거)
                    item {
                        PeriodSelectionSection(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = {
                                selectedPeriod = it
                                // 기간이 바뀌면 드롭다운 기본값도 변경
                                selectedRange = when (it) {
                                    "주" -> "이번 주"
                                    "월" -> "${selectedYear}년 ${selectedMonth}월"
                                    "년" -> "${selectedYear}년"
                                    "전체" -> "전체"
                                    else -> "전체"
                                }
                            }
                        )
                    }

                    // 통계 카드들을 별도 아이템으로 빼내기
                    item {
                        StatisticsCardsSection(
                            records = filteredRecords,
                            selectedPeriod = selectedPeriod,
                            selectedRange = selectedRange,
                            onRangeSelected = {
                                selectedRange = it
                                // 선택된 기간에 따라 적절한 바텀시트를 표시
                                when (selectedPeriod) {
                                    "월" -> showMonthPicker = true
                                    "주" -> showWeekPicker = true
                                    "년" -> showYearPicker = true
                                }
                            }
                        )
                    }

                    // 그래프 섹션을 별도 아이템으로 분리
                    item {
                        GraphSection(records = filteredRecords, selectedPeriod = selectedPeriod)
                    }

                    // 하단: 최근 활동 섹션 헤더
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "최근 활동",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 디버깅용 테스트 기록 추가 버튼
                            Button(
                                onClick = { showTestDialog = true },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("테스트 기록 추가")
                            }
                        }
                    }

                    if (filteredRecords.isEmpty()) {
                        // 기록이 없을 때
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "아직 금주 기록이 없습니다.\n첫 번째 금주를 시작해보세요!",
                                        fontSize = 16.sp,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // 기록이 있을 때 각 카드를 개별 아이템으로 표시
                        items(filteredRecords) { record ->
                            SobrietyRecordCard(
                                record = record,
                                onClick = { handleCardClick(record) }
                            )
                        }
                    }
                }
            }
        }

        // 테스트 기록 입력 다이얼로그 - LazyColumn 밖으로 이동
        if (showTestDialog) {
            AlertDialog(
                onDismissRequest = { showTestDialog = false },
                confirmButton = {
                    Button(onClick = {
                        addCustomTestRecord()
                        showTestDialog = false
                        refreshTrigger++
                    }) { Text("추가") }
                },
                dismissButton = {
                    Button(onClick = { showTestDialog = false }) { Text("취소") }
                },
                title = { Text("테스트 기록 직접 추가") },
                text = {
                    TestRecordInputDialogContent(
                        inputStartTime = inputStartTime,
                        onStartTimeChange = { v -> v.toLongOrNull()?.let { inputStartTime = it } },
                        inputEndTime = inputEndTime,
                        onEndTimeChange = { v -> v.toLongOrNull()?.let { inputEndTime = it } },
                        inputTargetDays = inputTargetDays,
                        onTargetDaysChange = { v -> v.toIntOrNull()?.let { inputTargetDays = it } },
                        inputActualDays = inputActualDays,
                        onActualDaysChange = { v -> v.toIntOrNull()?.let { inputActualDays = it } },
                        inputIsCompleted = inputIsCompleted,
                        onIsCompletedChange = { inputIsCompleted = it }
                    )
                }
            )
        }
    }

    private fun handleCardClick(record: SobrietyRecord) {
        Log.d(TAG, "===== 카드 클릭 시작 =====")
        Log.d(TAG, "카드 클릭: ${record.id}")
        Log.d(TAG, "actualDays=${record.actualDays}, targetDays=${record.targetDays}")
        Log.d(TAG, "startTime=${record.startTime}, endTime=${record.endTime}")
        Log.d(TAG, "isCompleted=${record.isCompleted}")

        try {
            // 데이터 유효성 검사 (더 관대하게)
            if (record.actualDays < 0) {
                Log.e(TAG, "잘못된 기록 데이터: actualDays=${record.actualDays}")
                return
            }

            Log.d(TAG, "Intent 생성 시작...")

            // targetDays가 0이면 기본값으로 설정
            val safeTargetDays = if (record.targetDays <= 0) 30 else record.targetDays

            // DetailActivity로 이동
            val intent = Intent(this@RecordsActivity, DetailActivity::class.java)
            intent.putExtra("start_time", record.startTime)
            intent.putExtra("end_time", record.endTime)
            intent.putExtra("target_days", safeTargetDays.toFloat())
            intent.putExtra("actual_days", record.actualDays)
            intent.putExtra("is_completed", record.isCompleted)

            Log.d(TAG, "Intent 데이터 전달: targetDays=$safeTargetDays, actualDays=${record.actualDays}")
            Log.d(TAG, "DetailActivity 호출...")
            startActivity(intent)
            Log.d(TAG, "startActivity 호출 완료")
            Log.d(TAG, "===== 카드 클릭 종료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "CardDetail 화면 이동 중 오류", e)
            Log.e(TAG, "오류 스택트레이스: ${e.stackTraceToString()}")
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewRecordsScreen() {
        BaseScreen {
            RecordsScreen()
        }
    }
}

@Composable
fun SobrietyRecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    val startDate = dateFormatter.format(Date(record.startTime))
    val endDate = dateFormatter.format(Date(record.endTime))
    val startTime = timeFormatter.format(Date(record.startTime))
    val endTime = timeFormatter.format(Date(record.endTime))

    val duration = record.endTime - record.startTime
    val durationDays = (duration / (24 * 60 * 60 * 1000)).toInt()
    val durationHours = ((duration % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val durationMinutes = ((duration % (60 * 60 * 1000)) / (60 * 1000)).toInt()

    val progressPercent = if (record.targetDays > 0) {
        ((record.actualDays.toFloat() / record.targetDays) * 100).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (record.isCompleted) Color(0xFFE8F5E8) else Color(0xFFFFF3CD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단: 상태와 날짜
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 상태 배지
                Surface(
                    color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = record.status,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = if (startDate == endDate) startDate else "$startDate ~ $endDate",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 중앙: 주요 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 왼쪽: 달성 일수
                Column {
                    Text(
                        text = "${record.actualDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "달성",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 중앙: 목표 대비 진행률
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${progressPercent}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Text(
                        text = "목표 달성률",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 오른쪽: 목표 일수
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "목표",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 하단: 상세 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "시작: $startTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "종료: $endTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (durationDays > 0 || durationHours > 0 || durationMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "지속 시간: ${durationDays}일 ${durationHours}시간 ${durationMinutes}분",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun loadSobrietyRecords(context: android.content.Context): List<SobrietyRecord> {
    return try {
        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"

        Log.d("RecordsActivity", "========== 디버깅 정보 ==========")
        Log.d("RecordsActivity", "저장된 JSON 문자열: $recordsJson")
        Log.d("RecordsActivity", "JSON 길이: ${recordsJson.length}")

        // SobrietyRecord의 companion object 메서드 사용
        val records = SobrietyRecord.fromJsonArray(recordsJson)

        Log.d("RecordsActivity", "파싱된 기록 개수: ${records.size}")
        records.forEachIndexed { index, record ->
            Log.d("RecordsActivity", "기록 $index:")
            Log.d("RecordsActivity", "  ID: ${record.id}")
            Log.d("RecordsActivity", "  시작시간: ${record.startTime}")
            Log.d("RecordsActivity", "  종료시간: ${record.endTime}")
            Log.d("RecordsActivity", "  목표일수: ${record.targetDays}")
            Log.d("RecordsActivity", "  달성일수: ${record.actualDays}")
            Log.d("RecordsActivity", "  완료여부: ${record.isCompleted}")
            Log.d("RecordsActivity", "  상태: ${record.status}")
            Log.d("RecordsActivity", "  생성시간: ${record.createdAt}")
        }
        Log.d("RecordsActivity", "===============================")

        // 최신 순으로 정렬
        records.sortedByDescending { it.createdAt }
    } catch (e: Exception) {
        Log.e("RecordsActivity", "기록 로딩 중 오류 발생", e)
        Log.e("RecordsActivity", "오류 상세: ${e.message}")
        Log.e("RecordsActivity", "스택 트레이스: ${e.stackTraceToString()}")
        emptyList()
    }
}

// 상단: 기간 선택 탭 섹션
@Composable
fun PeriodSelectionSection(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = listOf("주", "월", "년", "전체")

    // Card 배경 제거하고 바로 Row로 구성
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFE0E0E0),
                RoundedCornerShape(25.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Color.Black else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 중단: 통계 및 그래프 섹션
@Composable
fun StatisticsCardsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    val totalDays = records.sumOf { it.actualDays }
    val completedCount = records.count { it.isCompleted }
    val totalAttempts = records.size
    val successRate = if (totalAttempts > 0) (completedCount * 100) / totalAttempts else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 텍스트 클릭 영역 (왼쪽 정렬) - 개선된 클릭 감지
        Row(
            modifier = Modifier
                .clickable(
                    enabled = selectedPeriod != "전체",
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Material3에서는 기본 ripple을 사용하거나 null로 설정
                ) {
                    if (selectedPeriod != "전체") {
                        onRangeSelected(selectedRange)
                    }
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedRange,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (selectedPeriod != "전체") {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 상단 통계 카드들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "총 금주일",
                value = "${totalDays}일",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "성공률",
                value = "${successRate}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "시도 횟수",
                value = "${totalAttempts}회",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 그래프 섹션을 별도 아이템으로 분���
@Composable
fun GraphSection(records: List<SobrietyRecord>, selectedPeriod: String) {
    // 실제 그래프 표시
    MiniBarChart(
        records = records,
        selectedPeriod = selectedPeriod,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    )
}

// 간단한 미니 막대 그래프 컴포저블 추가
@Composable
fun MiniBarChart(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    modifier: Modifier = Modifier
) {
    // 선택된 기간에 따라 그래프 데이터 생성
    val graphData = when (selectedPeriod) {
        "주" -> generateWeeklyGraphData(records)
        "월" -> generateMonthlyGraphData(records)
        "년" -> generateYearlyGraphData(records)
        "전체" -> generateAllTimeGraphData(records)
        else -> generateWeeklyGraphData(records)
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (graphData.isEmpty()) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height

            // 여백 설정
            val leftMargin = 20.dp.toPx()
            val rightMargin = 20.dp.toPx()
            val topMargin = 20.dp.toPx()
            val bottomMargin = 40.dp.toPx()

            val chartWidth = canvasWidth - leftMargin - rightMargin
            val chartHeight = canvasHeight - topMargin - bottomMargin

            // Y축 가로선 그리기 (값 0, 0.5, 1에 해당하는 위치)
            val yLines = listOf(0f, 0.5f, 1f)
            yLines.forEach { value ->
                val y = topMargin + chartHeight - (value * chartHeight)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(leftMargin, y),
                    end = Offset(leftMargin + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Y축 선
            drawLine(
                color = Color.Gray,
                start = Offset(leftMargin, topMargin),
                end = Offset(leftMargin, topMargin + chartHeight),
                strokeWidth = 2.dp.toPx()
            )

            // X축 선
            drawLine(
                color = Color.Gray,
                start = Offset(leftMargin, topMargin + chartHeight),
                end = Offset(leftMargin + chartWidth, topMargin + chartHeight),
                strokeWidth = 2.dp.toPx()
            )

            // 막대 그래프
            val barWidth = chartWidth / graphData.size * 0.7f
            val barSpacing = chartWidth / graphData.size * 0.3f

            graphData.forEachIndexed { index, item ->
                val barHeight = if (item.value > 0) chartHeight * 0.6f else 0f
                val x = leftMargin + (index * (barWidth + barSpacing)) + barSpacing / 2
                val y = topMargin + chartHeight - barHeight

                // 막대 그리기
                drawRect(
                    color = if (item.value > 0) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }

        // X축 레이블
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            graphData.forEachIndexed { index, item ->
                val shouldShowLabel = when (selectedPeriod) {
                    "주" -> true
                    "월" -> index % 5 == 0 || index == graphData.size - 1
                    "년" -> true
                    "전체" -> true
                    else -> true
                }

                if (shouldShowLabel) {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Y축 레이블
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 5.dp)
                .fillMaxHeight()
        ) {
            // 1 레이블 - 상단 가로선과 정렬
            Text(
                text = "1",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = 15.dp)
            )

            // 0 레이블 - 하단 가로선과 정렬
            Text(
                text = "0",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = (-35).dp)
            )
        }
    }
}

// 최근 7일간의 ���래프 데이터 생성 함수
private fun generateWeeklyGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()
    val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")
    val completedRecords = records.filter { it.isCompleted }

    // 이번 주 월요일부터 시작
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val weekStart = calendar.timeInMillis

    return weekDays.mapIndexed { index, dayName ->
        val dayStart = weekStart + (index * 24 * 60 * 60 * 1000L)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)

        val hasSuccess = completedRecords.any { record ->
            record.startTime >= dayStart && record.startTime < dayEnd
        }

        SimpleGraphData(dayName, if (hasSuccess) 1 else 0)
    }
}

// 최근 30일간의 그���프 데이터 생성 함수
private fun generateMonthlyGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()
    val monthNames = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")
    val completedRecords = records.filter { it.isCompleted }

    // 이�� 달 1일자로 설정
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val monthStart = calendar.timeInMillis

    return monthNames.mapIndexed { index, monthName ->
        val monthStartTime = monthStart + (index * 30 * 24 * 60 * 60 * 1000L)
        val monthEndTime = monthStartTime + (30 * 24 * 60 * 60 * 1000L)

        val hasSuccess = completedRecords.any { record ->
            record.startTime >= monthStartTime && record.startTime < monthEndTime
        }

        SimpleGraphData(monthName, if (hasSuccess) 1 else 0)
    }.take(12) // 최근 12개월만 표시
}

// 최근 1년간의 그래프 데이터 생성 함수
private fun generateYearlyGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()
    val completedRecords = records.filter { it.isCompleted }

    // 올�� 1월 1일자로 설정
    calendar.set(Calendar.MONTH, Calendar.JANUARY)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val yearStart = calendar.timeInMillis

    // 최근 1년간의 월별 데이�� 생성
    return (0 until 12).map { monthOffset ->
        val monthStart = yearStart + (monthOffset * 30 * 24 * 60 * 60 * 1000L)
        val monthEnd = monthStart + (30 * 24 * 60 * 60 * 1000L)

        val hasSuccess = completedRecords.any { record ->
            record.startTime >= monthStart && record.startTime < monthEnd
        }

        SimpleGraphData("${monthOffset + 1}월", if (hasSuccess) 1 else 0)
    }
}

// 전체 기간에 대한 그래프 데이터 생성 함수
private fun generateAllTimeGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    val completedRecords = records.filter { it.isCompleted }

    return listOf(
        SimpleGraphData("전체", if (completedRecords.isNotEmpty()) 1 else 0)
    )
}

// RecordsActivity 전용 간단한 그래프 ���이터 클래스
data class SimpleGraphData(
    val label: String,
    val value: Int // 0 또는 1 (성공 여부)
)

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TestRecordInputDialogContent(
    inputStartTime: Long,
    onStartTimeChange: (String) -> Unit,
    inputEndTime: Long,
    onEndTimeChange: (String) -> Unit,
    inputTargetDays: Int,
    onTargetDaysChange: (String) -> Unit,
    inputActualDays: Int,
    onActualDaysChange: (String) -> Unit,
    inputIsCompleted: Boolean,
    onIsCompletedChange: (Boolean) -> Unit
) {
    Column {
        Text("시작일(Unix ms):")
        OutlinedTextField(
            value = inputStartTime.toString(),
            onValueChange = onStartTimeChange,
            singleLine = true
        )
        Text("종료일(Unix ms):")
        OutlinedTextField(
            value = inputEndTime.toString(),
            onValueChange = onEndTimeChange,
            singleLine = true
        )
        Text("목표 일수:")
        OutlinedTextField(
            value = inputTargetDays.toString(),
            onValueChange = onTargetDaysChange,
            singleLine = true
        )
        Text("실제 일수:")
        OutlinedTextField(
            value = inputActualDays.toString(),
            onValueChange = onActualDaysChange,
            singleLine = true
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = inputIsCompleted,
                onCheckedChange = onIsCompletedChange
            )
            Text("완료 여부")
        }
    }
}
