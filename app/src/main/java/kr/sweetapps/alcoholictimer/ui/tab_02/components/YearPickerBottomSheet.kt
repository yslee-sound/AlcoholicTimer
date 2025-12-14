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
import androidx.compose.ui.platform.LocalContext
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
fun YearPickerBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onYearPicked: (year: Int) -> Unit,
    records: List<SobrietyRecord> = emptyList(),
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
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
            YearPickerContent(
                onYearPicked = onYearPicked,
                onDismiss = onDismiss,
                records = records,
                initialYear = initialYear
            )
        }
    }
}

@Composable
internal fun YearPickerContent(
    onYearPicked: (year: Int) -> Unit,
    onDismiss: () -> Unit,
    records: List<SobrietyRecord>,
    initialYear: Int
) {
    val context = LocalContext.current

    // [NEW] 초기화 로그
    android.util.Log.d("YearPicker", "초기화: records.size=${records.size}, initialYear=$initialYear")

    // [FIX] 모든 년도를 표시하되, 기록이 있는 년도와 없는 년도를 구분
    val (yearOptions, enabledStates) = remember(records) {
        if (records.isEmpty()) {
            android.util.Log.d("YearPicker", "기록 없음")
            Pair(emptyList(), emptyList())
        } else {
            val nowYear = Calendar.getInstance().get(Calendar.YEAR)
            val startYear = records.minByOrNull { it.startTime }?.let {
                Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.YEAR)
            } ?: nowYear

            // 시작 년도부터 현재 년도까지 모든 년도 생성
            val allYears = (startYear..nowYear).toList()

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

            // [NEW] 활성화 상태 리스트 생성 (기록이 있는 년도만 true)
            val enabled = allYears.map { year -> year in yearsWithRecords }

            android.util.Log.d("YearPicker", "년도 범위: $startYear~$nowYear, 활성화: ${enabled.count { it }}개/${enabled.size}개")

            Pair(allYears, enabled)
        }
    }

    val defaultIndex = remember(yearOptions, initialYear) {
        yearOptions.indexOf(initialYear).let { if (it >= 0) it else (yearOptions.size - 1).coerceAtLeast(0) }
    }
    var selectedIndex by remember(yearOptions) { mutableIntStateOf(defaultIndex) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.year_picker_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val canSelect = yearOptions.isNotEmpty()

        if (canSelect) {
            NumberPicker(
                value = selectedIndex,
                onValueChange = { selectedIndex = it },
                range = 0 until yearOptions.size,
                displayValues = yearOptions.map { context.getString(R.string.year_picker_year_format, it) },
                enabledStates = enabledStates, // [NEW] 비활성화 상태 전달
                modifier = Modifier.width(160.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                contentAlignment = Alignment.Center
            ) { Text(text = stringResource(R.string.year_picker_no_data), color = Color(0xFF636E72), fontSize = 14.sp) }
        }

        Button(
            onClick = {
                val year = yearOptions.getOrNull(selectedIndex)
                // [NEW] 선택된 년도가 활성화되어 있을 때만 처리
                val isEnabled = enabledStates.getOrNull(selectedIndex) ?: false
                if (year != null && isEnabled) {
                    onYearPicked(year)
                    onDismiss()
                }
            },
            enabled = canSelect && (enabledStates.getOrNull(selectedIndex) ?: false), // [NEW] 비활성화된 년도는 버튼도 비활성화
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF74B9FF), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) { Text(text = stringResource(R.string.year_picker_select), fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun YearPickerBottomSheetPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        YearPickerContent(onYearPicked = { }, onDismiss = { }, records = emptyList(), initialYear = 2025)
    }
}

@Preview(showBackground = true, name = "YearPicker - Dark Mode")
@Composable
fun YearPickerBottomSheetDarkPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        YearPickerContent(onYearPicked = { }, onDismiss = { }, records = emptyList(), initialYear = 2025)
    }
}
