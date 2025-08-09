package com.example.alcoholictimer

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.max

enum class GraphPeriod(val displayName: String) {
    WEEK("주"),
    MONTH("월"),
    YEAR("년"),
    ALL("전체")
}

data class GraphData(
    val label: String,
    val value: Int, // 0 또는 1 (성공 여부)
    val date: Long? = null
)

class GraphActivity : BaseActivity() {

    private val TAG = "GraphActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                GraphScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "금주 통계 그래프"

    @Composable
    private fun GraphScreen() {
        val context = LocalContext.current
        var selectedPeriod by remember { mutableStateOf(GraphPeriod.MONTH) }
        var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
        var graphData by remember { mutableStateOf<List<GraphData>>(emptyList()) }

        // 기록 로드
        LaunchedEffect(Unit) {
            records = loadSobrietyRecords(context)
            Log.d(TAG, "로드된 기록: ${records.size}개")
        }

        // 선택된 기간에 따라 그래프 데이터 생성
        LaunchedEffect(selectedPeriod, records) {
            graphData = generateGraphData(selectedPeriod, records)
            Log.d(TAG, "생성된 그래프 데이터: ${graphData.size}개")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 기간 선택 섹션
            PeriodSelectionCard(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it }
            )

            // 그래프 카드
            GraphCard(
                period = selectedPeriod,
                data = graphData
            )

            // 통계 요약 카드
            StatsSummaryCard(
                period = selectedPeriod,
                data = graphData,
                totalRecords = records.size,
                completedRecords = records.count { it.isCompleted }
            )
        }
    }

    private fun generateGraphData(period: GraphPeriod, records: List<SobrietyRecord>): List<GraphData> {
        val completedRecords = records.filter { it.isCompleted }

        return when (period) {
            GraphPeriod.WEEK -> generateWeeklyData(completedRecords)
            GraphPeriod.MONTH -> generateMonthlyData(completedRecords)
            GraphPeriod.YEAR -> generateYearlyData(completedRecords)
            GraphPeriod.ALL -> generateAllTimeData(completedRecords)
        }
    }

    private fun generateWeeklyData(records: List<SobrietyRecord>): List<GraphData> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")
        val weekStart = calendar.timeInMillis

        return weekDays.mapIndexed { index, dayName ->
            val dayStart = weekStart + (index * 24 * 60 * 60 * 1000L)
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)

            val hasSuccess = records.any { record ->
                record.startTime >= dayStart && record.startTime < dayEnd
            }

            GraphData(dayName, if (hasSuccess) 1 else 0, dayStart)
        }
    }

    private fun generateMonthlyData(records: List<SobrietyRecord>): List<GraphData> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return (1..daysInMonth).map { day ->
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = calendar.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)

            val hasSuccess = records.any { record ->
                record.startTime >= dayStart && record.startTime < dayEnd
            }

            GraphData(day.toString(), if (hasSuccess) 1 else 0, dayStart)
        }
    }

    private fun generateYearlyData(records: List<SobrietyRecord>): List<GraphData> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val months = listOf("1월", "2월", "3월", "4월", "5월", "6월",
                          "7월", "8월", "9월", "10월", "11월", "12월")

        return months.mapIndexed { index, monthName ->
            calendar.set(currentYear, index, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis

            val hasSuccess = records.any { record ->
                record.startTime >= monthStart && record.startTime < monthEnd
            }

            GraphData(monthName, if (hasSuccess) 1 else 0, monthStart)
        }
    }

    private fun generateAllTimeData(records: List<SobrietyRecord>): List<GraphData> {
        if (records.isEmpty()) {
            return listOf(GraphData("2025", 0))
        }

        val years = records.map { record ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = record.startTime
            calendar.get(Calendar.YEAR)
        }.distinct().sorted()

        return years.map { year ->
            val calendar = Calendar.getInstance()
            calendar.set(year, 0, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val yearStart = calendar.timeInMillis

            calendar.add(Calendar.YEAR, 1)
            val yearEnd = calendar.timeInMillis

            val hasSuccess = records.any { record ->
                record.startTime >= yearStart && record.startTime < yearEnd
            }

            GraphData("${year}년", if (hasSuccess) 1 else 0, yearStart)
        }
    }
}

@Composable
fun PeriodSelectionCard(
    selectedPeriod: GraphPeriod,
    onPeriodSelected: (GraphPeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "기간별 보기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                GraphPeriod.values().forEach { period ->
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
                            text = period.displayName,
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GraphCard(
    period: GraphPeriod,
    data: List<GraphData>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${period.displayName}별 금주 성공 현황",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "표시할 데이터가 없습니다",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                BarChart(
                    data = data,
                    period = period,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<GraphData>,
    period: GraphPeriod,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (data.isEmpty()) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height

            // 여백 설정
            val leftMargin = 40.dp.toPx()
            val rightMargin = 20.dp.toPx()
            val topMargin = 20.dp.toPx()
            val bottomMargin = 60.dp.toPx()

            val chartWidth = canvasWidth - leftMargin - rightMargin
            val chartHeight = canvasHeight - topMargin - bottomMargin

            // Y축 (0, 1만 표시)
            drawYAxis(leftMargin, topMargin, chartHeight)

            // X축
            drawXAxis(leftMargin, topMargin + chartHeight, chartWidth)

            // 막대 그래프
            val barWidth = chartWidth / data.size * 0.7f
            val barSpacing = chartWidth / data.size * 0.3f

            data.forEachIndexed { index, item ->
                val barHeight = if (item.value > 0) chartHeight * 0.8f else 0f
                val x = leftMargin + (index * (barWidth + barSpacing)) + barSpacing / 2
                val y = topMargin + chartHeight - barHeight

                // 막대 그리기
                drawRect(
                    color = if (item.value > 0) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }

        // X축 레이블을 별도로 오버레이
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, item ->
                val shouldShowLabel = when (period) {
                    GraphPeriod.WEEK -> true
                    GraphPeriod.MONTH -> index % 5 == 0 || index == data.size - 1
                    GraphPeriod.YEAR -> true
                    GraphPeriod.ALL -> true
                }

                if (shouldShowLabel) {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Y축 레이블을 별도로 오버레이
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "0",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

fun DrawScope.drawYAxis(x: Float, y: Float, height: Float) {
    // Y축 선
    drawLine(
        color = Color.Gray,
        start = Offset(x, y),
        end = Offset(x, y + height),
        strokeWidth = 2.dp.toPx()
    )
}

fun DrawScope.drawXAxis(x: Float, y: Float, width: Float) {
    // X축 선
    drawLine(
        color = Color.Gray,
        start = Offset(x, y),
        end = Offset(x + width, y),
        strokeWidth = 2.dp.toPx()
    )
}

@Composable
fun StatsSummaryCard(
    period: GraphPeriod,
    data: List<GraphData>,
    totalRecords: Int,
    completedRecords: Int
) {
    val successCount = data.count { it.value > 0 }
    val totalPeriods = data.size
    val successRate = if (totalPeriods > 0) (successCount * 100) / totalPeriods else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${period.displayName}별 통계 요약",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "성공 ${period.displayName}",
                    value = "${successCount}${if (period == GraphPeriod.WEEK) "일" else when(period) {
                        GraphPeriod.MONTH -> "일"
                        GraphPeriod.YEAR -> "월"
                        GraphPeriod.ALL -> "년"
                        else -> ""
                    }}",
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    title = "성공률",
                    value = "${successRate}%",
                    modifier = Modifier.weight(1f)
                )

                StatItem(
                    title = "전체 기록",
                    value = "${completedRecords}/${totalRecords}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatItem(
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

// RecordsActivity에서 사용하는 loadSobrietyRecords 함수 재사용
private fun loadSobrietyRecords(context: android.content.Context): List<SobrietyRecord> {
    return try {
        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val jsonArray = org.json.JSONArray(recordsJson)

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

        records.sortedByDescending { it.createdAt }
    } catch (e: Exception) {
        emptyList()
    }
}
