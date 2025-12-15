// [NEW] Tab02 리팩토링: components를 tab_02로 이동
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.AppAlphas
import kr.sweetapps.alcoholictimer.ui.components.NumberPicker
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onMonthPicked: (year: Int, month: Int) -> Unit,
    records: List<SobrietyRecord> = emptyList(),
    onYearPicked: (year: Int) -> Unit = {},
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFF636E72).copy(alpha = AppAlphas.SurfaceTint), RoundedCornerShape(2.dp))
                )
            }
        ) {
            MonthPickerContent(
                onMonthPicked = onMonthPicked,
                onYearPicked = onYearPicked,
                onDismiss = onDismiss,
                records = records,
                initialYear = initialYear,
                initialMonth = initialMonth
            )
        }
    }
}

@Composable
internal fun MonthPickerContent(
    onMonthPicked: (year: Int, month: Int) -> Unit,
    onYearPicked: (year: Int) -> Unit,
    onDismiss: () -> Unit,
    records: List<SobrietyRecord>,
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
) {
    // [NEW] 초기화 로그
    android.util.Log.d("MonthPicker", "초기화: records.size=${records.size}, initialYear=$initialYear, initialMonth=$initialMonth")

    // [FIX] 년도 옵션 및 활성화 상태 계산
    val (yearOptions, yearEnabledStates) = remember(records) {
        val result = generateYearOptionsFromRecordsWithStates(records)
        android.util.Log.d("MonthPicker", "년도 옵션: ${result.first.size}개, 활성화: ${result.second.count { it }}개")
        result
    }

    val defaultYearIndex = remember(yearOptions, initialYear) {
        yearOptions.indexOf(initialYear).let { if (it >= 0) it else (yearOptions.size - 1).coerceAtLeast(0) }
    }

    var selectedYearIndex by remember(yearOptions) { mutableIntStateOf(defaultYearIndex) }

    // [FIX] 선택된 년도의 월 목록 및 활성화 상태 계산
    fun monthsForWithStates(year: Int): Pair<List<Int>, List<Boolean>> {
        val now = Calendar.getInstance()
        val nowYear = now.get(Calendar.YEAR)
        val nowMonth = now.get(Calendar.MONTH) + 1

        // [FIX] 데이터가 없어도 현재 년/월은 표시
        if (records.isEmpty()) {
            return if (year == nowYear) {
                Pair(listOf(nowMonth), listOf(true))
            } else {
                Pair(emptyList(), emptyList())
            }
        }

        val first = records.minByOrNull { it.startTime }!!
        val firstCal = Calendar.getInstance().apply { timeInMillis = first.startTime }
        val firstYear = firstCal.get(Calendar.YEAR)
        val firstMonth = firstCal.get(Calendar.MONTH) + 1

        // [FIX] 시작 월과 종료 월 결정
        val startMonth = if (year == firstYear) firstMonth else 1
        val endMonth = if (year == nowYear) nowMonth else 12

        if (year < firstYear || year > nowYear) return Pair(emptyList(), emptyList())

        val allMonths = (startMonth..endMonth).toList()

        // [NEW] 각 월에 기록이 있는지 확인
        val monthsWithRecords = records.flatMap { record ->
            val recordStartCal = Calendar.getInstance().apply { timeInMillis = record.startTime }
            val recordEndCal = Calendar.getInstance().apply { timeInMillis = record.endTime }

            val recordStartYear = recordStartCal.get(Calendar.YEAR)
            val recordStartMonth = recordStartCal.get(Calendar.MONTH) + 1
            val recordEndYear = recordEndCal.get(Calendar.YEAR)
            val recordEndMonth = recordEndCal.get(Calendar.MONTH) + 1

            // 해당 년도와 겹치는 월 찾기
            buildList {
                if (recordStartYear == year && recordEndYear == year) {
                    // 같은 년도 내
                    addAll(recordStartMonth..recordEndMonth)
                } else if (recordStartYear == year) {
                    // 시작 년도
                    addAll(recordStartMonth..12)
                } else if (recordEndYear == year) {
                    // 종료 년도
                    addAll(1..recordEndMonth)
                } else if (recordStartYear < year && recordEndYear > year) {
                    // 중간 년도 (전체 월)
                    addAll(1..12)
                }
            }
        }.toSet()

        // [FIX] UX 개선: 현재 선택된 월(nowMonth)이 범위 내에 있으면 무조건 활성화
        // 데이터가 하나(이번 달)만 있어도 목록에 표시되도록 수정
        val enabled = allMonths.map { month ->
            month in monthsWithRecords || (year == nowYear && month == nowMonth)
        }

        return Pair(allMonths, enabled)
    }

    val selectedYear = yearOptions.getOrNull(selectedYearIndex) ?: Calendar.getInstance().get(Calendar.YEAR)
    var monthOptionsWithStates by remember { mutableStateOf(monthsForWithStates(selectedYear)) }
    val monthOptions = monthOptionsWithStates.first
    val monthEnabledStates = monthOptionsWithStates.second

    var selectedMonthIndex by remember {
        mutableIntStateOf(
            monthOptions.indexOf(initialMonth).let { if (it >= 0) it else (monthOptions.size - 1).coerceAtLeast(0) }
        )
    }

    LaunchedEffect(yearOptions, selectedYearIndex) {
        val y = yearOptions.getOrNull(selectedYearIndex) ?: return@LaunchedEffect
        val newMonthsWithStates = monthsForWithStates(y)
        monthOptionsWithStates = newMonthsWithStates
        selectedMonthIndex = selectedMonthIndex.coerceIn(0, (newMonthsWithStates.first.size - 1).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.month_picker_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val canSelect = yearOptions.isNotEmpty() && monthOptions.isNotEmpty()

        if (canSelect) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NumberPicker(
                        value = selectedYearIndex,
                        onValueChange = { newIndex ->
                            selectedYearIndex = newIndex
                            yearOptions.getOrNull(newIndex)?.let { y -> onYearPicked(y) }
                        },
                        range = 0 until yearOptions.size,
                        displayValues = yearOptions.map { stringResource(R.string.month_picker_year_format, it) },
                        enabledStates = yearEnabledStates, // [NEW] 년도 활성화 상태 전달
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    NumberPicker(
                        value = selectedMonthIndex,
                        onValueChange = { selectedMonthIndex = it },
                        range = 0 until monthOptions.size,
                        displayValues = monthOptions.map { stringResource(R.string.month_picker_month_format, it) },
                        enabledStates = monthEnabledStates, // [NEW] 월 활성화 상태 전달
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                contentAlignment = Alignment.Center
            ) { Text(text = stringResource(R.string.month_picker_no_data), color = Color(0xFF636E72), fontSize = 14.sp) }
        }

        Button(
            onClick = {
                val year = yearOptions.getOrNull(selectedYearIndex)
                val month = monthOptions.getOrNull(selectedMonthIndex)
                // [NEW] 년도와 월이 모두 활성화되어 있을 때만 처리
                val isYearEnabled = yearEnabledStates.getOrNull(selectedYearIndex) ?: false
                val isMonthEnabled = monthEnabledStates.getOrNull(selectedMonthIndex) ?: false
                if (year != null && month != null && isYearEnabled && isMonthEnabled) {
                    onMonthPicked(year, month)
                    onDismiss()
                }
            },
            enabled = canSelect &&
                      (yearEnabledStates.getOrNull(selectedYearIndex) ?: false) &&
                      (monthEnabledStates.getOrNull(selectedMonthIndex) ?: false), // [NEW] 둘 다 활성화되어야 버튼 활성화
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF74B9FF),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF74B9FF).copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) { Text(text = stringResource(R.string.month_picker_select), fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Suppress("unused")
data class MonthOption(
    val year: Int,
    val month: Int,
    val displayText: String
)

@Suppress("unused")
private fun generateMonthOptionsFromRecords(records: List<SobrietyRecord>): List<MonthOption> {
    val cal = Calendar.getInstance()
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH) + 1

    if (records.isEmpty()) {
        return listOf(MonthOption(nowYear, nowMonth, "${nowYear}년 ${nowMonth}월"))
    }

    val first = records.minByOrNull { it.startTime } ?: return listOf(MonthOption(nowYear, nowMonth, "${nowYear}년 ${nowMonth}월"))
    val firstCal = Calendar.getInstance().apply { timeInMillis = first.startTime }
    var y = firstCal.get(Calendar.YEAR)
    var m = firstCal.get(Calendar.MONTH) + 1

    val result = mutableListOf<MonthOption>()
    while (y < nowYear || (y == nowYear && m <= nowMonth)) {
        result.add(MonthOption(y, m, "${y}년 ${m}월"))
        m += 1
        if (m > 12) { m = 1; y += 1 }
    }

    return result
}

private fun generateYearOptionsFromRecords(records: List<SobrietyRecord>): List<Int> {
    if (records.isEmpty()) return emptyList()
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    val startYear = records.minByOrNull { it.startTime }?.let {
        Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.YEAR)
    } ?: nowYear

    val years = mutableListOf<Int>()
    var y = startYear
    while (y <= nowYear) { years.add(y); y++ }
    return years
}

/**
 * [NEW] 년도 옵션 및 활성화 상태를 함께 반환
 * @return Pair<년도 리스트, 활성화 상태 리스트>
 */
private fun generateYearOptionsFromRecordsWithStates(records: List<SobrietyRecord>): Pair<List<Int>, List<Boolean>> {
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)

    // [FIX] 데이터가 없어도 현재 년도는 표시
    if (records.isEmpty()) {
        return Pair(listOf(nowYear), listOf(true))
    }

    val startYear = records.minByOrNull { it.startTime }?.let {
        Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.YEAR)
    } ?: nowYear

    val years = mutableListOf<Int>()
    var y = startYear
    while (y <= nowYear) { years.add(y); y++ }

    // [NEW] 각 년도에 기록이 있는지 확인
    val yearsWithRecords = records.flatMap { record ->
        val recordStartYear = Calendar.getInstance().apply {
            timeInMillis = record.startTime
        }.get(Calendar.YEAR)
        val recordEndYear = Calendar.getInstance().apply {
            timeInMillis = record.endTime
        }.get(Calendar.YEAR)
        (recordStartYear..recordEndYear).toList()
    }.toSet()

    // [FIX] UX 개선: 현재 년도는 무조건 활성화 (데이터 없어도 표시)
    val enabledStates = years.map { year -> year in yearsWithRecords || year == nowYear }

    return Pair(years, enabledStates)
}

@Preview(showBackground = true)
@Composable
fun MonthPickerBottomSheetPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        MonthPickerContent(onMonthPicked = { _, _ -> }, onYearPicked = {}, onDismiss = { }, records = emptyList())
    }
}

@Preview(showBackground = true, name = "MonthPicker - Dark Mode")
@Composable
fun MonthPickerBottomSheetDarkPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        MonthPickerContent(onMonthPicked = { _, _ -> }, onYearPicked = {}, onDismiss = { }, records = emptyList())
    }
}
