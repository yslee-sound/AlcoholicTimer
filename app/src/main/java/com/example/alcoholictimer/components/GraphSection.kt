package com.example.alcoholictimer.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord
import java.util.*

@Composable
fun GraphSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedYear: Int,
    selectedMonth: Int,
    selectedWeekStart: Long?
) {
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
                        textAlign = TextAlign.Center,
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

// 최근 7일간의 그래프 데이터 생성 함수
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
