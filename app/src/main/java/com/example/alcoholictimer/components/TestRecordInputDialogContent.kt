package com.example.alcoholictimer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun TestRecordInputDialogContent(
    inputStartTime: Long,
    onStartTimeChange: (Long) -> Unit,
    inputEndTime: Long,
    onEndTimeChange: (Long) -> Unit,
    inputTargetDays: Float,
    onTargetDaysChange: (Float) -> Unit,
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

    // 목표일수 드롭다운 (0.1~0.9 + 1~365)
    val daysList = (1..365).map { it.toFloat() }.toMutableList().apply {
        addAll(0, (1..9).map { it / 10f }.reversed())
    }

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
        com.example.alcoholictimer.utils.PercentUtils.roundPercent(
            ((actualDurationDays / inputTargetDays) * 100).coerceIn(0f, 100f).toDouble()
        )
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
            onSelected = { onTargetDaysChange(it) },
            optionLabel = { String.format("%.1f", it) }
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
fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    optionLabel: (T) -> String = { it.toString() }
) {
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
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
