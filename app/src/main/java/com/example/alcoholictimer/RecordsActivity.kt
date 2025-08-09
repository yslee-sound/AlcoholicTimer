package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

data class SobrietyRecord(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val targetDays: Int,
    val actualDays: Int,
    val isCompleted: Boolean,
    val status: String,
    val createdAt: Long
)

class RecordsActivity : BaseActivity() {

    // 디버깅용 태그
    private val TAG = "RecordsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RecordsScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "금주 기록"

    @Composable
    private fun RecordsScreen() {
        val context = LocalContext.current
        var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
        var selectedPeriod by remember { mutableStateOf("월") } // 선택된 기간 상태 추가
        var selectedRange by remember { mutableStateOf("전체") } // 드롭다운 선택 상태 추가

        // 기록 로드
        LaunchedEffect(Unit) {
            records = loadSobrietyRecords(context)
            Log.d(TAG, "로드된 기록: ${records.size}개")

            // 더미 데이터 자동 추가 로직 제거 (초기화 테스트를 위해)
            // 필요시 수동으로 테스트 데이터를 추가할 수 있도록 별도 함수로 분리
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 상단: 기간 선택 탭 섹션
            item {
                PeriodSelectionSection(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = {
                        selectedPeriod = it
                        // 기간이 바뀌면 드롭다운 기본값도 변경
                        selectedRange = when (it) {
                            "주" -> "이번 주"
                            "월" -> "2025년"
                            "년" -> "2025년"
                            "전체" -> "2025년"
                            else -> "전체"
                        }
                    }
                )
            }

            // 통계 카드들을 별도 아이템으로 빼내기
            item {
                StatisticsCardsSection(
                    records = records,
                    selectedPeriod = selectedPeriod,
                    selectedRange = selectedRange,
                    onRangeSelected = { selectedRange = it }
                )
            }

            // 그래프 섹션을 별도 아이템으로 분리
            item {
                GraphSection(records = records, selectedPeriod = selectedPeriod)
            }

            // 하단: 최근 활동 섹션 헤더
            item {
                Text(
                    text = "최근 활동",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (records.isEmpty()) {
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
                items(records) { record ->
                    SobrietyRecordCard(
                        record = record,
                        onClick = { handleCardClick(record) }
                    )
                }
            }
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
            .clickable { onClick() }, // 클릭 이벤트 추가
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
        val jsonArray = JSONArray(recordsJson)

        val records = mutableListOf<SobrietyRecord>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            records.add(
                SobrietyRecord(
                    id = jsonObject.getString("id"),
                    startTime = jsonObject.getLong("startTime"),
                    endTime = jsonObject.getLong("endTime"),
                    targetDays = jsonObject.getInt("targetDays"),
                    actualDays = jsonObject.getInt("actualDays"),
                    isCompleted = jsonObject.getBoolean("isCompleted"),
                    status = jsonObject.getString("status"),
                    createdAt = jsonObject.getLong("createdAt")
                )
            )
        }

        // 최신 순으로 정렬
        records.sortedByDescending { it.createdAt }
    } catch (_: Exception) {
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

    // 드롭다운 메뉴 항목 생성 함수
    fun getDropdownItems(period: String): List<String> {
        val now = Calendar.getInstance()
        return when (period) {
            "주" -> listOf(
                "이번 주",
                "지난 주",
                "07-20 ~ 07-26",
                "07-13 ~ 07-19"
            )
            "월" -> listOf(
                "2025년", "2024년", "8월", "7월"
            )
            "년" -> listOf(
                "2025년", "2024년"
            )
            "전체" -> listOf(
                "2025년", "2024년 - 2025년"
            )
            else -> listOf("전체")
        }
    }

    var expanded by remember { mutableStateOf(false) }
    val dropdownItems = getDropdownItems(selectedPeriod)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 텍스트 드롭다운 (왼쪽 정렬)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            Text(
                text = selectedRange,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = selectedPeriod != "전체") { if (selectedPeriod != "전체") expanded = true }
                    .padding(vertical = 8.dp)
            )
            if (selectedPeriod == "월") {
                // 월일 때 2컬럼 드롭다운
                val years = listOf("2025년", "2024년")
                val months = listOf("8월", "7월", "6월", "5월", "4월", "3월", "2월", "1월")
                var selectedYear by remember { mutableStateOf(years.first()) }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(260.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        // 년도 선택
                        Column(modifier = Modifier.weight(1f)) {
                            years.forEach { year ->
                                Text(
                                    text = year,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedYear = year
                                        }
                                        .padding(8.dp),
                                    fontWeight = if (selectedYear == year) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        // 월 선택
                        Column(modifier = Modifier.weight(1f)) {
                            months.forEach { month ->
                                Text(
                                    text = month,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onRangeSelected("$selectedYear $month")
                                            expanded = false
                                        }
                                        .padding(8.dp),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else if (selectedPeriod != "전체") {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(180.dp)
                ) {
                    dropdownItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onRangeSelected(item)
                                expanded = false
                            }
                        )
                    }
                }
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

// 그래프 섹션을 별도 아이템으로 분리
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

// 최근 7일간의 그래프 데이터 생성 함수
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

// 최근 30일간의 그래프 데이터 생성 함수
private fun generateMonthlyGraphData(records: List<SobrietyRecord>): List<SimpleGraphData> {
    val calendar = Calendar.getInstance()
    val monthNames = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")
    val completedRecords = records.filter { it.isCompleted }

    // 이번 달 1일자로 설정
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

    // 올해 1월 1일자로 설정
    calendar.set(Calendar.MONTH, Calendar.JANUARY)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val yearStart = calendar.timeInMillis

    // 최근 1년간의 월별 데이터 생성
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

// RecordsActivity 전용 간단한 그래프 데이터 클래스
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
