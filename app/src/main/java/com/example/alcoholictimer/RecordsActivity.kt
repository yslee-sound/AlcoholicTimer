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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.forEachIndexed

class RecordsActivity : BaseActivity() {

    // 디버깅용 태그
    companion object {
        private const val TAG = "RecordsActivity"
    }

    // refreshTrigger를 Activity 레벨로 이동
    private var refreshTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RecordsScreen(refreshTrigger)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 나타날 때마다 데이터를 새로고침
        Log.d(TAG, "onResume: 기록 화면이 다시 나타남 - 데이터 새로고침")
        // refreshTrigger 증가시켜 강제 새로고침
        refreshTrigger++
    }

    override fun getScreenTitle(): String = "금주 기록"

    @Composable
    private fun RecordsScreen(externalRefreshTrigger: Int) {
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
        var isRefreshing by remember { mutableStateOf(false) }
        val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

        // 테스트 기록 입력 다이얼로그 상태
        var showTestDialog by remember { mutableStateOf(false) }
        var inputStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var inputEndTime by remember { mutableStateOf(System.currentTimeMillis()) }
        var inputTargetDays by remember { mutableStateOf(1) }
        var inputActualDays by remember { mutableStateOf(1) }

        // 선택된 주 시작일 변수 추가
        var selectedWeekStart by remember { mutableStateOf<Long?>(null) }

        // 데이터 로드 함수
        suspend fun loadRecords() {
            isRefreshing = true
            try {
                // 약간의 지연을 추가하여 새로고침 애니메이션을 보여줌
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

        fun addCustomTestRecord() {
            try {
                val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
                val recordsList = try {
                    JSONArray(recordsJson)
                } catch (e: Exception) {
                    JSONArray()
                }

                // 중복 시간대 검사
                for (i in 0 until recordsList.length()) {
                    val existingRecord = recordsList.getJSONObject(i)
                    val existingStartTime = existingRecord.getLong("startTime")
                    val existingEndTime = existingRecord.getLong("endTime")

                    // 새로운 기록의 시간대가 기존 기록과 겹치는지 검사
                    if ((inputStartTime >= existingStartTime && inputStartTime <= existingEndTime) ||
                        (inputEndTime >= existingStartTime && inputEndTime <= existingEndTime) ||
                        (inputStartTime <= existingStartTime && inputEndTime >= existingEndTime)) {

                        // 중복된 시간대가 있으면 토스트 메시지 표시하고 함수 종료
                        android.widget.Toast.makeText(context, "시간대가 중복입니다", android.widget.Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                // 실제 시간 차이를 기반으로 달성률 계산
                val actualDurationMs = inputEndTime - inputStartTime
                val actualDurationDays = (actualDurationMs / (24 * 60 * 60 * 1000f))
                val achievedPercentage = ((actualDurationDays / inputTargetDays) * 100).coerceAtMost(100f).toInt()

                // 목표 달성 여부 판단 (100% 이상이면 완료, 미만이면 중지)
                val isCompleted = achievedPercentage >= 100
                val status = if (isCompleted) "완료" else "중지"

                val testRecord = JSONObject().apply {
                    put("id", "test_${System.currentTimeMillis()}")
                    put("startTime", inputStartTime)
                    put("endTime", inputEndTime)
                    put("targetDays", inputTargetDays)
                    put("actualDays", actualDurationDays.toInt())
                    put("isCompleted", isCompleted)
                    put("status", status)
                    put("createdAt", System.currentTimeMillis())
                    put("percentage", achievedPercentage) // 실제 시간 차이 기반 달성률
                }
                recordsList.put(testRecord)
                sharedPref.edit().apply {
                    putString("sobriety_records", recordsList.toString())
                    apply()
                }

                // 데이터 즉시 새로고침
                coroutineScope.launch {
                    loadRecords()
                }

                // 성공적으로 추가되었음을 알리는 토스트 메시지
                android.widget.Toast.makeText(context, "테스트 기록이 추가되었습니다", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }


        // 외부 refreshTrigger가 변경될 때마다 새로고침
        LaunchedEffect(externalRefreshTrigger) {
            loadRecords()
        }

        // 초기 로드
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
                selectedWeekStart = weekStart
                Log.d(TAG, "선택된 주: $displayText, 시작: $weekStart")
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
                        GraphSection(
                            records = filteredRecords,
                            selectedPeriod = selectedPeriod,
                            selectedYear = selectedYear,
                            selectedMonth = selectedMonth,
                            selectedWeekStart = selectedWeekStart
                        )
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
                        onStartTimeChange = { inputStartTime = it },
                        inputEndTime = inputEndTime,
                        onEndTimeChange = { inputEndTime = it },
                        inputTargetDays = inputTargetDays,
                        onTargetDaysChange = { inputTargetDays = it },
                        inputActualDays = inputActualDays,
                        onActualDaysChange = { inputActualDays = it }
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
            RecordsScreen(0)
        }
    }
}

@Composable
fun SobrietyRecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }

    val startDate = dateFormatter.format(Date(record.startTime))
    val endDate = dateFormatter.format(Date(record.endTime))
    val startTime = timeFormatter.format(Date(record.startTime))
    val endTime = timeFormatter.format(Date(record.endTime))

    val duration = record.endTime - record.startTime
    val durationDays = (duration / (24 * 60 * 60 * 1000)).toInt()
    val durationHours = ((duration % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val durationMinutes = ((duration % (60 * 60 * 1000)) / (60 * 1000)).toInt()

    // 실제 시간 차이를 기반으로 달성률 계산
    val actualDurationDays = (duration / (24 * 60 * 60 * 1000f)).toFloat()
    val progressPercent = if (record.targetDays > 0) {
        ((actualDurationDays / record.targetDays) * 100).coerceIn(0f, 100f).toInt()
    } else {
        // 기존 percentage 값이 있으면 사용, 없으면 0
        record.percentage ?: 0
    }

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
                // 상태 배지와 테스트 표시를 포함하는 Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                    // 테스트 기록 표시
                    if (record.isTest) {
                        Surface(
                            color = Color(0xFF9C27B0),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "테스트",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
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
                // 왼쪽: 달성 일수 (실제 시간 기반)
                Column {
                    Text(
                        text = "${actualDurationDays.toInt()}일",
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

                // 중앙: 목표 대비 진행률 (실시간 계산)
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

        // 종료일 기준으로 최신 순 정렬 (가장 최근 종료된 기록이 맨 위)
        records.sortedByDescending { it.endTime }
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
fun GraphSection(records: List<SobrietyRecord>, selectedPeriod: String, selectedYear: Int, selectedMonth: Int, selectedWeekStart: Long?) {
    // 실제 그래프 표시
    MiniBarChart(
        records = records,
        selectedPeriod = selectedPeriod,
        selectedYear = selectedYear,
        selectedMonth = selectedMonth,
        selectedWeekStart = selectedWeekStart,
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
    selectedYear: Int,
    selectedMonth: Int,
    selectedWeekStart: Long?,
    modifier: Modifier = Modifier
) {
    // 선택된 기간에 따라 그래프 데이터 생성
    val graphData = when (selectedPeriod) {
        "주" -> generateWeeklyGraphData(records, selectedWeekStart)
        "월" -> generateMonthlyGraphData(records, selectedYear, selectedMonth)
        "년" -> generateYearlyGraphData(records)
        "전체" -> generateAllTimeGraphData(records)
        else -> generateWeeklyGraphData(records, selectedWeekStart)
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
                // 실제 비율에 따라 막대 높이 계산 (0.0~1.0 비율을 차트 높이에 맞게 변환)
                val barHeight = chartHeight * item.value
                val x = leftMargin + (index * (barWidth + barSpacing)) + barSpacing / 2
                val y = topMargin + chartHeight - barHeight

                // 막대 그리기 - 진행 시간이 있을 때만 표시
                if (item.value > 0) {
                    drawRect(
                        color = when {
                            item.value >= 1.0f -> Color(0xFF4CAF50) // 완전 금주 (녹색)
                            item.value >= 0.5f -> Color(0xFFFF9800) // 절반 이상 (주황색)
                            else -> Color(0xFFFFEB3B) // 절반 미만 (노란색)
                        },
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }
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
                val day = index + 1 // 1-based 날짜
                val shouldShowLabel = when (selectedPeriod) {
                    "주" -> true
                    "월" -> {
                        // 매월의 시작일(1일), 매월의 월요일, 매월의 마지막일에만 표시
                        if (day == 1 || day == graphData.size) {
                            true // 1일과 마지막일은 항상 표시
                        } else {
                            // 해당 날짜가 월요일인지 확인
                            val calendar = Calendar.getInstance()
                            calendar.set(selectedYear, selectedMonth - 1, day)
                            calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                        }
                    }
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
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
private fun generateWeeklyGraphData(records: List<SobrietyRecord>, weekStart: Long?): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()
    val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")

    // 선택된 주의 시작이 있으면 그 값을 사용, 없으면 이번 주 월요일
    val actualWeekStart = weekStart ?: run {
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

    return weekDays.mapIndexed { index, dayName ->
        val dayStart = actualWeekStart + (index * 24 * 60 * 60 * 1000L)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L) - 1

        // 해당 날짜가 기록 기간에 포함되는 기록들 찾기
        val dayRecords = records.filter { record ->
            dayStart <= record.endTime && dayEnd >= record.startTime
        }

        if (dayRecords.isEmpty()) {
            SimpleGraphData(dayName, 0f)
        } else {
            // 해당 날짜에 실제로 달성한 시간만 반영 (월 그래프와 동일)
            val dayTotalMs = 24 * 60 * 60 * 1000f
            val achievedRatio = dayRecords.map { record ->
                val recordStart = record.startTime
                val recordEnd = record.endTime
                val overlapStart = maxOf(recordStart, dayStart)
                val overlapEnd = minOf(recordEnd, dayEnd)
                val overlapMs = (overlapEnd - overlapStart).coerceAtLeast(0)
                overlapMs / dayTotalMs
            }.sum().coerceAtMost(1.0f)

            // 최소 5% 표시
            val ratio = if (achievedRatio == 0f && dayRecords.isNotEmpty()) {
                0.05f
            } else {
                achievedRatio
            }

            Log.d("GraphDebug", "주간 ${dayName} 실제 달성 비율: $achievedRatio, 표시 비율: $ratio")

            SimpleGraphData(dayName, ratio)
        }
    }
}

// 최근 30일간의 그래프 데이터 생성 함수
private fun generateMonthlyGraphData(records: List<SobrietyRecord>, year: Int, month: Int): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()

    // 선택된 연/월의 1일부터 마지막 날까지 생성
    calendar.set(year, month - 1, 1, 0, 0, 0) // month - 1 because Calendar.MONTH is 0-based
    calendar.set(Calendar.MILLISECOND, 0)

    // 선택된 월의 마지막 날 계산
    val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Log.d("GraphDebug", "=== 월별 그래프 생성 시작 ===")
    Log.d("GraphDebug", "선택된 기간: ${year}년 ${month}월")
    Log.d("GraphDebug", "전체 기록 수: ${records.size}")

    records.forEachIndexed { index, record ->
        val recordCal = Calendar.getInstance().apply { timeInMillis = record.startTime }
        val actualDurationMs = record.endTime - record.startTime
        val actualDurationDays = (actualDurationMs / (24 * 60 * 60 * 1000f))
        val realPercentage = if (record.targetDays > 0) {
            ((actualDurationDays / record.targetDays) * 100).coerceAtMost(100f).toInt()
        } else {
            0
        }
        Log.d("GraphDebug", "기록 $index: ${recordCal.get(Calendar.YEAR)}년 ${recordCal.get(Calendar.MONTH)+1}월 ${recordCal.get(Calendar.DAY_OF_MONTH)}일, ID=${record.id}, 실제달성률=${realPercentage}%")
    }

    return (1..lastDayOfMonth).map { day ->
        // 그래프의 해당 날짜 시작과 끝
        calendar.set(year, month - 1, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis

        calendar.set(year, month - 1, day, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val dayEnd = calendar.timeInMillis

        // 해당 날짜가 기록 기간에 포함되는 기록들 찾기
        val dayRecords = records.filter { record ->
            dayStart <= record.endTime && dayEnd >= record.startTime
        }

        if (day == 15 && month == 6) { // 6월 15일에 대해서만 로그 출력
            Log.d("GraphDebug", "6월 15일 매칭된 기록 수: ${dayRecords.size}")
            dayRecords.forEach { record ->
                val startCal = Calendar.getInstance().apply { timeInMillis = record.startTime }
                val endCal = Calendar.getInstance().apply { timeInMillis = record.endTime }
                val actualDurationMs = record.endTime - record.startTime
                val actualDurationDays = (actualDurationMs / (24 * 60 * 60 * 1000f))
                val realPercentage = if (record.targetDays > 0) {
                    ((actualDurationDays / record.targetDays) * 100).coerceAtMost(100f).toInt()
                } else {
                    0
                }
                Log.d("GraphDebug", "6월 15일 기록: ${startCal.get(Calendar.MONTH)+1}/${startCal.get(Calendar.DAY_OF_MONTH)} ~ ${endCal.get(Calendar.MONTH)+1}/${endCal.get(Calendar.DAY_OF_MONTH)}, 실제달성률=${realPercentage}%")
            }
        }

        if (dayRecords.isEmpty()) {
            SimpleGraphData("${day}", 0.0f)
        } else {
            // 해당 날짜에 실제로 달성한 시간만 반영
            val dayTotalMs = 24 * 60 * 60 * 1000f
            val achievedRatio = dayRecords.map { record ->
                val recordStart = record.startTime
                val recordEnd = record.endTime
                val overlapStart = maxOf(recordStart, dayStart)
                val overlapEnd = minOf(recordEnd, dayEnd)
                val overlapMs = (overlapEnd - overlapStart).coerceAtLeast(0)
                overlapMs / dayTotalMs
            }.sum().coerceAtMost(1.0f)

            // 최소 5% 표시
            val ratio = if (achievedRatio == 0f && dayRecords.isNotEmpty()) {
                0.05f
            } else {
                achievedRatio
            }

            Log.d("GraphDebug", "${month}월 ${day}일 실제 달성 비율: $achievedRatio, 표시 비율: $ratio")

            SimpleGraphData("${day}", ratio)
        }
    }.also {
        Log.d("GraphDebug", "=== 월별 그래프 생성 완료 ===")
    }
}

// 최근 1년간의 그래프 데이터 생성 함수
private fun generateYearlyGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)

    // 올해 1월~12월 반복
    return (0 until 12).map { monthOffset ->
        // 월 시작/끝 계산
        calendar.set(currentYear, monthOffset, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(currentYear, monthOffset, lastDayOfMonth, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val monthEnd = calendar.timeInMillis

        // 해당 월에 걸친 기록들 찾기
        val monthRecords = records.filter { record ->
            record.endTime >= monthStart && record.startTime <= monthEnd
        }

        // 실제 금주 일수 합산
        val monthTotalDays = lastDayOfMonth
        val achievedDays = monthRecords.sumOf { record ->
            val overlapStart = maxOf(record.startTime, monthStart)
            val overlapEnd = minOf(record.endTime, monthEnd)
            val overlapMs = (overlapEnd - overlapStart).coerceAtLeast(0)
            (overlapMs / (24 * 60 * 60 * 1000L)).toInt()
        }
        val ratio = if (monthTotalDays > 0) achievedDays.toFloat() / monthTotalDays else 0f

        SimpleGraphData("${monthOffset + 1}월", ratio.coerceIn(0f, 1f))
    }
}

// 전체 기간에 대한 그래프 데이터 생성 함수
private fun generateAllTimeGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    // 12년치 그래프 (현재 기준 2025년만 데이터 있음)
    val startYear = 2025
    val maxYears = 12
    val years = (startYear until startYear + maxYears)

    return years.map { year ->
        // 해당 년도에 걸친 기록들 찾기
        val yearStartCal = Calendar.getInstance().apply { set(year, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        val yearStart = yearStartCal.timeInMillis
        val yearEndCal = Calendar.getInstance().apply { set(year, Calendar.DECEMBER, 31, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
        val yearEnd = yearEndCal.timeInMillis
        val yearRecords = records.filter { it.endTime >= yearStart && it.startTime <= yearEnd }

        // 실제 금주 일수 합산
        val totalDays = if (yearRecords.isNotEmpty()) {
            val daysInYear = yearEndCal.getActualMaximum(Calendar.DAY_OF_YEAR)
            val achievedDays = yearRecords.sumOf { record ->
                val overlapStart = maxOf(record.startTime, yearStart)
                val overlapEnd = minOf(record.endTime, yearEnd)
                val overlapMs = (overlapEnd - overlapStart).coerceAtLeast(0)
                (overlapMs / (24 * 60 * 60 * 1000L)).toInt()
            }
            achievedDays.toFloat() / daysInYear
        } else {
            0f
        }
        // x축 레이블: 2025년만 표시, 나머지는 빈 문자열
        val label = if (year == startYear) "2025년" else ""
        SimpleGraphData(label, totalDays.coerceIn(0f, 1f))
    }
}

// RecordsActivity 전용 간단한 그래프 데이터 클래스
data class SimpleGraphData(
    val label: String,
    val value: Float // 0.0~1.0 (금주 진행 비율)
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
    onStartTimeChange: (Long) -> Unit,
    inputEndTime: Long,
    onEndTimeChange: (Long) -> Unit,
    inputTargetDays: Int,
    onTargetDaysChange: (Int) -> Unit,
    inputActualDays: Int,
    onActualDaysChange: (Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    // 시작일 드롭다운 상태
    var startYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var startMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var startDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var startHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    // 종료일 드롭다운 상태
    var endYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var endMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var endDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var endHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    // 목표일수 드롭다운
    val daysList = (1..365).toList()

    // 날짜 선택 시 Unix ms로 변환
    fun getMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // 종료일시를 시작일시 이후로 조정하는 함수
    fun adjustEndTimeIfNeeded() {
        val startTimeMs = getMillis(startYear, startMonth, startDay, startHour, startMinute)
        val endTimeMs = getMillis(endYear, endMonth, endDay, endHour, endMinute)

        if (endTimeMs < startTimeMs) {
            // 종료일시가 시작일시보다 이전이면 시작일시와 동일하게 설정
            endYear = startYear
            endMonth = startMonth
            endDay = startDay
            endHour = startHour
            endMinute = startMinute
        }
    }

    // 시작 시간이 변경될 때마다 업데이트
    LaunchedEffect(startYear, startMonth, startDay, startHour, startMinute) {
        onStartTimeChange(getMillis(startYear, startMonth, startDay, startHour, startMinute))
        adjustEndTimeIfNeeded() // 시작일시 변경 시 종료일시 검증
    }

    // 종료 시간이 변경될 때마다 업데이트
    LaunchedEffect(endYear, endMonth, endDay, endHour, endMinute) {
        adjustEndTimeIfNeeded() // 종료일시 변경 시 검증
        onEndTimeChange(getMillis(endYear, endMonth, endDay, endHour, endMinute))
    }

    // 달성률 계산 (실시간)
    val actualDurationMs = inputEndTime - inputStartTime
    val actualDurationDays = (actualDurationMs / (24 * 60 * 60 * 1000f)).toFloat()
    val achievedPercentage = if (inputTargetDays > 0) {
        ((actualDurationDays / inputTargetDays) * 100).coerceIn(0f, 100f).toInt()
    } else 0

    // 종료일시 선택 옵션을 시작일시 이후로 제한하는 함수들
    fun getValidEndYears(): List<Int> {
        return (startYear..calendar.get(Calendar.YEAR)).toList()
    }

    fun getValidEndMonths(): List<Int> {
        return if (endYear == startYear) {
            (startMonth..12).toList()
        } else {
            (1..12).toList()
        }
    }

    fun getValidEndDays(): List<Int> {
        val cal = Calendar.getInstance()
        cal.set(endYear, endMonth - 1, 1)
        val maxDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (endYear == startYear && endMonth == startMonth) {
            (startDay..maxDayOfMonth).toList()
        } else {
            (1..maxDayOfMonth).toList()
        }
    }

    fun getValidEndHours(): List<Int> {
        return if (endYear == startYear && endMonth == startMonth && endDay == startDay) {
            (startHour..23).toList()
        } else {
            (0..23).toList()
        }
    }

    fun getValidEndMinutes(): List<Int> {
        val validMinutes = (0..59 step 5).toList()
        return if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour) {
            validMinutes.filter { it >= startMinute }
        } else {
            validMinutes
        }
    }

    Column {
        Text("시작일시:")
        // 시작일 - 년도
        Row {
            DropdownSelector(
                label = "년",
                options = (2020..calendar.get(Calendar.YEAR)).toList(),
                selected = startYear,
                onSelected = { startYear = it }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 시작일 - 월과 일
        Row {
            DropdownSelector(
                label = "월",
                options = (1..12).toList(),
                selected = startMonth,
                onSelected = { startMonth = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(
                label = "일",
                options = (1..31).toList(),
                selected = startDay,
                onSelected = { startDay = it }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 시작일 - 시간
        Row {
            DropdownSelector(
                label = "시",
                options = (0..23).toList(),
                selected = startHour,
                onSelected = { startHour = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(
                label = "분",
                options = (0..59 step 5).toList(),
                selected = startMinute,
                onSelected = { startMinute = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("종료일시:")
        // 종료일 - 년도 (시작년도 이후만 선택 가능)
        Row {
            DropdownSelector(
                label = "년",
                options = getValidEndYears(),
                selected = endYear,
                onSelected = {
                    endYear = it
                    // 년도 변경 시 월/일/시/분 재조정
                    if (endYear == startYear && endMonth < startMonth) {
                        endMonth = startMonth
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay < startDay) {
                        endDay = startDay
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour < startHour) {
                        endHour = startHour
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) {
                        endMinute = startMinute
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 종료일 - 월과 일
        Row {
            DropdownSelector(
                label = "월",
                options = getValidEndMonths(),
                selected = endMonth,
                onSelected = {
                    endMonth = it
                    // 월 변경 시 일/시/분 재조정
                    val validDays = getValidEndDays()
                    if (!validDays.contains(endDay)) {
                        endDay = validDays.first()
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay < startDay) {
                        endDay = startDay
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour < startHour) {
                        endHour = startHour
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) {
                        endMinute = startMinute
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(
                label = "일",
                options = getValidEndDays(),
                selected = endDay,
                onSelected = {
                    endDay = it
                    // 일 변경 시 시/분 재조정
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour < startHour) {
                        endHour = startHour
                    }
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) {
                        endMinute = startMinute
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 종료일 - 시간
        Row {
            DropdownSelector(
                label = "시",
                options = getValidEndHours(),
                selected = endHour,
                onSelected = {
                    endHour = it
                    // 시간 변경 시 분 재조정
                    if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) {
                        endMinute = startMinute
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(
                label = "분",
                options = getValidEndMinutes(),
                selected = endMinute,
                onSelected = { endMinute = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("목표일수:")
        DropdownSelector(
            label = "일수",
            options = daysList,
            selected = inputTargetDays,
            onSelected = { onTargetDaysChange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 실시간 계산된 정보 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "자동 계산된 정보",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "실제 진행 기간: ${actualDurationDays.toInt()}일 ${((actualDurationMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()}시간",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "목표 달성률: ${achievedPercentage}%",
                    fontSize = 12.sp,
                    color = when {
                        achievedPercentage >= 100 -> Color(0xFF4CAF50)
                        achievedPercentage >= 50 -> Color(0xFFFF9800)
                        else -> Color(0xFFFF5722)
                    }
                )
            }
        }
    }
}

@Composable
fun <T> DropdownSelector(label: String, options: List<T>, selected: T, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$selected $label")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp) // 최대 높이 제한으로 스크롤 가능하게 함
        ) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.toString()) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}
