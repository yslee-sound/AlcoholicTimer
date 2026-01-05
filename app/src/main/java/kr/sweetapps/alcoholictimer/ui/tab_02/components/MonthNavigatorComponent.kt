// [NEW] 월간 네비게이터 컴포넌트 (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import java.util.Calendar

/**
 * 월간 네비게이터
 * 이전/다음 달 이동 버튼과 현재 월 표시
 */
@Composable
fun MonthNavigator(
    selectedDetailPeriod: String,
    allRecords: List<SobrietyRecord>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: () -> Unit
) {
    // 현재 선택된 월 파싱
    val (year, month) = parseYearMonth(selectedDetailPeriod)
    val selectedYearMonth = year * 12 + month

    // 현재 날짜 (미래 방지용)
    val currentYearMonth = Calendar.getInstance().let {
        it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) + 1
    }

    // 가장 오래된 기록의 날짜 (과거 방지용)
    val oldestTimestamp = allRecords.minOfOrNull { it.startTime } ?: System.currentTimeMillis()
    val minYearMonth = Calendar.getInstance().apply {
        timeInMillis = oldestTimestamp
    }.let {
        it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) + 1
    }

    val isFutureMonth = selectedYearMonth >= currentYearMonth
    val isPastMonth = selectedYearMonth <= minYearMonth

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 이전 달 버튼 (과거 방지)
        IconButton(
            onClick = onPreviousMonth,
            enabled = !isPastMonth,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_left),
                contentDescription = "이전 달",
                tint = if (isPastMonth) Color(0xFFD1D5DB) else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // 중앙: 현재 월 (클릭 가능)
        Text(
            text = selectedDetailPeriod,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDateClick
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.width(24.dp))

        // 다음 달 버튼 (미래 방지)
        IconButton(
            onClick = onNextMonth,
            enabled = !isFutureMonth,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "다음 달",
                tint = if (isFutureMonth) Color(0xFFD1D5DB) else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 날짜 문자열 파싱 헬퍼 함수
 * "2025년 12월", "12/2025", "2025.12" 등 다양한 형식 지원
 */
fun parseYearMonth(dateString: String): Pair<Int, Int> {
    return try {
        val numbers = Regex("(\\d+)").findAll(dateString).map { it.value.toInt() }.toList()

        if (numbers.size >= 2) {
            val num1 = numbers[0]
            val num2 = numbers[1]
            val year = if (num1 > 100) num1 else num2
            val month = if (num1 > 100) num2 else num1

            Pair(year, month)
        } else {
            val now = Calendar.getInstance()
            Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        }
    } catch (e: Exception) {
        val now = Calendar.getInstance()
        Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }
}

