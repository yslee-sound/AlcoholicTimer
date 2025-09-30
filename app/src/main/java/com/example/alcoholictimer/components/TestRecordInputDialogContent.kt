package com.example.alcoholictimer.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var startYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var startMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var startDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var startHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    var endYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var endMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var endDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var endHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    val daysList = (1..365).map { it.toFloat() }.toMutableList().apply {
        addAll(0, (1..9).map { it / 10f }.reversed())
    }

    fun getMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun adjustEndTimeIfNeeded() {
        val startTimeMs = getMillis(startYear, startMonth, startDay, startHour, startMinute)
        val endTimeMs = getMillis(endYear, endMonth, endDay, endHour, endMinute)
        if (endTimeMs < startTimeMs) {
            endYear = startYear
            endMonth = startMonth
            endDay = startDay
            endHour = startHour
            endMinute = startMinute
        }
    }

    LaunchedEffect(startYear, startMonth, startDay, startHour, startMinute) {
        onStartTimeChange(getMillis(startYear, startMonth, startDay, startHour, startMinute))
        adjustEndTimeIfNeeded()
    }

    LaunchedEffect(endYear, endMonth, endDay, endHour, endMinute) {
        adjustEndTimeIfNeeded()
        onEndTimeChange(getMillis(endYear, endMonth, endDay, endHour, endMinute))
    }

    val actualDurationMs = inputEndTime - inputStartTime
    val actualDurationDays = (actualDurationMs / (24 * 60 * 60 * 1000f))
    val achievedPercentage = if (inputTargetDays > 0) {
        com.example.alcoholictimer.utils.PercentUtils.roundPercent(
            ((actualDurationDays / inputTargetDays) * 100).coerceIn(0f, 100f).toDouble()
        )
    } else 0

    fun getValidEndYears(): List<Int> = (startYear..calendar.get(Calendar.YEAR)).toList()
    fun getValidEndMonths(): List<Int> = if (endYear == startYear) (startMonth..12).toList() else (1..12).toList()
    fun getValidEndDays(): List<Int> {
        val cal = Calendar.getInstance()
        cal.set(endYear, endMonth - 1, 1)
        val maxDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (endYear == startYear && endMonth == startMonth) (startDay..maxDayOfMonth).toList() else (1..maxDayOfMonth).toList()
    }
    fun getValidEndHours(): List<Int> = if (endYear == startYear && endMonth == startMonth && endDay == startDay) (startHour..23).toList() else (0..23).toList()
    fun getValidEndMinutes(): List<Int> {
        val validMinutes = (0..59 step 5).toList()
        return if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour) {
            validMinutes.filter { it >= startMinute }
        } else validMinutes
    }

    Column {
        Text("시작일시:")
        Row {
            DropdownSelector(label = "년", options = (2020..calendar.get(Calendar.YEAR)).toList(), selected = startYear, onSelected = { startYear = it })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            DropdownSelector(label = "월", options = (1..12).toList(), selected = startMonth, onSelected = { startMonth = it })
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(label = "일", options = (1..31).toList(), selected = startDay, onSelected = { startDay = it })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            DropdownSelector(label = "시", options = (0..23).toList(), selected = startHour, onSelected = { startHour = it })
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(label = "분", options = (0..59 step 5).toList(), selected = startMinute, onSelected = { startMinute = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("종료일시:")
        Row {
            DropdownSelector(label = "년", options = getValidEndYears(), selected = endYear, onSelected = {
                endYear = it
                if (endYear == startYear && endMonth < startMonth) endMonth = startMonth
                if (endYear == startYear && endMonth == startMonth && endDay < startDay) endDay = startDay
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour < startHour) endHour = startHour
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) endMinute = startMinute
            })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            DropdownSelector(label = "월", options = getValidEndMonths(), selected = endMonth, onSelected = {
                endMonth = it
                val validDays = getValidEndDays()
                if (!validDays.contains(endDay)) endDay = validDays.first()
                if (endYear == startYear && endMonth == startMonth && endDay < startDay) endDay = startDay
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour < startHour) endDay = startDay
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) endMinute = startMinute
            })
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(label = "일", options = getValidEndDays(), selected = endDay, onSelected = {
                endDay = it
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour < startHour) endHour = startHour
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) endMinute = startMinute
            })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            DropdownSelector(label = "시", options = getValidEndHours(), selected = endHour, onSelected = {
                endHour = it
                if (endYear == startYear && endMonth == startMonth && endDay == startDay && endHour == startHour && endMinute < startMinute) endMinute = startMinute
            })
            Spacer(modifier = Modifier.width(8.dp))
            DropdownSelector(label = "분", options = getValidEndMinutes(), selected = endMinute, onSelected = { endMinute = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("목표일수:")
        DropdownSelector(
            label = "일수",
            options = daysList,
            selected = inputTargetDays,
            onSelected = { onTargetDaysChange(it) },
            optionLabel = { String.format(Locale.getDefault(), "%.1f", it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "자동 계산된 정보", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "실제 진행 기간: ${actualDurationDays.toInt()}일 ${((actualDurationMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()}시간", fontSize = 12.sp, color = Color.Gray)
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
        OutlinedButton(onClick = { expanded = true }) { Text("$selected $label") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 200.dp)) {
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
