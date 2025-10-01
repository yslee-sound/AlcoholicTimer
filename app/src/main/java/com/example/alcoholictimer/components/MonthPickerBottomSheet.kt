package com.example.alcoholictimer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord
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
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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
                        .background(
                            Color(0xFF636E72).copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
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
    // 연도 목록: 첫 기록의 연도부터 현재 연도까지
    val yearOptions = remember(records) { generateYearOptionsFromRecords(records) }

    // 기본 선택 연도 인덱스
    val defaultYearIndex = remember(yearOptions, initialYear) {
        yearOptions.indexOf(initialYear).let { if (it >= 0) it else (yearOptions.size - 1).coerceAtLeast(0) }
    }

    // yearOptions가 변경되면 선택 인덱스를 기본값으로 재초기화 (primitive state 사용)
    var selectedYearIndex by remember(yearOptions) { mutableIntStateOf(defaultYearIndex) }

    // 선택된 연도에 맞는 월 목록 계산
    fun monthsFor(year: Int): List<Int> {
        // 기록이 전혀 없으면 월 리스트도 비움
        if (records.isEmpty()) return emptyList()
        val now = Calendar.getInstance()
        val nowYear = now.get(Calendar.YEAR)
        val nowMonth = now.get(Calendar.MONTH) + 1

        val first = records.minByOrNull { it.startTime }!!
        val firstCal = Calendar.getInstance().apply { timeInMillis = first.startTime }
        val firstYear = firstCal.get(Calendar.YEAR)
        val firstMonth = firstCal.get(Calendar.MONTH) + 1

        val startMonth = when (year) {
            firstYear -> firstMonth
            else -> 1
        }
        val endMonth = when (year) {
            nowYear -> nowMonth
            else -> 12
        }
        return if (year < firstYear || year > nowYear) emptyList() else (startMonth..endMonth).toList()
    }

    val selectedYear = yearOptions.getOrNull(selectedYearIndex) ?: Calendar.getInstance().get(Calendar.YEAR)
    var monthOptions by remember { mutableStateOf(monthsFor(selectedYear)) }

    // 기본 선택 월 인덱스: initialMonth가 월 목록에 없으면 마지막 인덱스 (primitive state 사용)
    var selectedMonthIndex by remember {
        mutableIntStateOf(
            monthOptions.indexOf(initialMonth).let { if (it >= 0) it else (monthOptions.size - 1).coerceAtLeast(0) }
        )
    }

    // 연도/옵션 변경 시 월 목록/선택 인덱스 갱신
    LaunchedEffect(yearOptions, selectedYearIndex) {
        val y = yearOptions.getOrNull(selectedYearIndex) ?: return@LaunchedEffect
        val newMonths = monthsFor(y)
        monthOptions = newMonths
        selectedMonthIndex = selectedMonthIndex.coerceIn(0, (newMonths.size - 1).coerceAtLeast(0))
        // 사용자 명시 동작이 아닌 초기/스크롤 변화 시 상위 콜백을 호출하지 않습니다.
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "월 선택",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 빈 데이터일 때는 Picker를 렌더링하지 않음
        val canSelect = yearOptions.isNotEmpty() && monthOptions.isNotEmpty()

        if (canSelect) {
            // 2열 선택 (왼쪽: 연, 오른쪽: 월)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 연 선택
                Box(modifier = Modifier.weight(1f)) {
                    NumberPicker(
                        value = selectedYearIndex,
                        onValueChange = { newIndex ->
                            selectedYearIndex = newIndex
                            yearOptions.getOrNull(newIndex)?.let { y -> onYearPicked(y) }
                        },
                        range = 0 until yearOptions.size,
                        displayValues = yearOptions.map { "${it}년" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 월 선택 (선택된 연도에 따라 동적)
                Box(modifier = Modifier.weight(1f)) {
                    NumberPicker(
                        value = selectedMonthIndex,
                        onValueChange = { selectedMonthIndex = it },
                        range = 0 until monthOptions.size,
                        displayValues = monthOptions.map { "${it}월" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // 비어있는 상태 안내
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "표시할 항목이 없습니다",
                    color = Color(0xFF636E72),
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = {
                val year = yearOptions.getOrNull(selectedYearIndex)
                val month = monthOptions.getOrNull(selectedMonthIndex)
                if (year != null && month != null) {
                    onMonthPicked(year, month)
                    onDismiss()
                }
            },
            enabled = canSelect,
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
        ) {
            Text(
                text = "선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 사용 가능하지만 현재 파일 내에서는 참조되지 않음
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
        // 기록이 전혀 없는 경우: 현재 월만 노출
        return listOf(MonthOption(nowYear, nowMonth, "${nowYear}년 ${nowMonth}월"))
    }

    val first = records.minByOrNull { it.startTime } ?: return listOf(MonthOption(nowYear, nowMonth, "${nowYear}년 ${nowMonth}월"))
    val firstCal = Calendar.getInstance().apply { timeInMillis = first.startTime }
    var y = firstCal.get(Calendar.YEAR)
    var m = firstCal.get(Calendar.MONTH) + 1

    // firstMonth부터 현재 월까지 모두 추가
    val result = mutableListOf<MonthOption>()
    while (y < nowYear || (y == nowYear && m <= nowMonth)) {
        result.add(MonthOption(y, m, "${y}년 ${m}월"))
        // 다음 달로 이동
        m += 1
        if (m > 12) { m = 1; y += 1 }
    }

    return result
}

private fun generateYearOptionsFromRecords(records: List<SobrietyRecord>): List<Int> {
    // 기록이 전혀 없으면 연 리스트도 비움(요구사항: 기록이 있는 경우만 리스트 표시)
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

@Preview(showBackground = true)
@Composable
fun MonthPickerBottomSheetPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        MonthPickerContent(
            onMonthPicked = { _, _ -> },
            onYearPicked = {},
            onDismiss = { },
            records = emptyList()
        )
    }
}

@Preview(showBackground = true, name = "MonthPicker - Dark Mode")
@Composable
fun MonthPickerBottomSheetDarkPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        MonthPickerContent(
            onMonthPicked = { _, _ -> },
            onYearPicked = {},
            onDismiss = { },
            records = emptyList()
        )
    }
}
