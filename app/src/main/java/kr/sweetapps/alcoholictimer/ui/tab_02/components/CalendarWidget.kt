package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * [NEW] 월간 캘린더 위젯 (2025-12-22)
 * - 일기 데이터를 달력 형태로 표시
 * - 각 날짜 아래에 갈증 수치를 나타내는 색상 점(Dot) 표시
 * - Calendar 기반 (API 21+ 호환)
 */
@Composable
fun CalendarWidget(
    diaries: List<DiaryEntity>,
    onDateClick: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    // 현재 표시 중인 년월 (사용자가 이동 가능)
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // 다이어리 데이터를 날짜별로 매핑 (yyyy-MM-dd -> DiaryEntity)
    val diaryMap = remember(diaries) {
        diaries.associateBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 헤더: 년월 표시 + 이전/다음 버튼
            CalendarHeader(
                calendar = currentCalendar,
                onPreviousMonth = {
                    currentCalendar = Calendar.getInstance().apply {
                        timeInMillis = currentCalendar.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                },
                onNextMonth = {
                    currentCalendar = Calendar.getInstance().apply {
                        timeInMillis = currentCalendar.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 요일 행
            WeekdayRow()

            Spacer(modifier = Modifier.height(8.dp))

            // 날짜 그리드
            CalendarGrid(
                calendar = currentCalendar,
                diaryMap = diaryMap,
                onDateClick = onDateClick
            )
        }
    }
}

/**
 * 캘린더 헤더 (년월 표시 + 이동 버튼)
 */
@Composable
private fun CalendarHeader(
    calendar: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val locale = Locale.getDefault()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1

    val yearMonthText = when (locale.language) {
        "ko" -> "${year}년 ${month}월"
        "ja" -> "${year}年${month}月"
        "zh" -> "${year}年${month}月"
        else -> SimpleDateFormat("MMMM yyyy", locale).format(calendar.time)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "이전 달",
                tint = Color(0xFF6B7280)
            )
        }

        Text(
            text = yearMonthText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = Color(0xFF111827)
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "다음 달",
                tint = Color(0xFF6B7280)
            )
        }
    }
}

/**
 * 요일 행 (일, 월, 화, 수, 목, 금, 토)
 */
@Composable
private fun WeekdayRow() {
    val locale = Locale.getDefault()
    val weekdays = when (locale.language) {
        "ko" -> listOf("일", "월", "화", "수", "목", "금", "토")
        "ja" -> listOf("日", "月", "火", "水", "木", "金", "土")
        "zh" -> listOf("日", "一", "二", "三", "四", "五", "六")
        else -> listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 날짜 그리드 (달력 본체)
 */
@Composable
private fun CalendarGrid(
    calendar: Calendar,
    diaryMap: Map<String, DiaryEntity>,
    onDateClick: (Calendar) -> Unit
) {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    // 이번 달 첫날
    val firstDayOfMonth = Calendar.getInstance().apply {
        set(year, month, 1)
    }

    // 첫날의 요일 (1=일요일)
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

    // 이번 달 마지막 날짜
    val lastDay = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    // 오늘 날짜
    val today = Calendar.getInstance()

    // 달력 그리드 생성
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var dayCounter = 1 - firstDayOfWeek

        repeat(6) { weekIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) {
                    val date = Calendar.getInstance().apply {
                        set(year, month, dayCounter)
                    }

                    val isCurrentMonth = dayCounter in 1..lastDay
                    val isToday = isCurrentMonth &&
                            date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                    val dateKey = if (isCurrentMonth) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.time)
                    } else {
                        ""
                    }
                    val diary = diaryMap[dateKey]

                    CalendarDayCell(
                        date = date,
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        diary = diary,
                        onClick = { if (isCurrentMonth) onDateClick(date) },
                        modifier = Modifier.weight(1f)
                    )

                    dayCounter++
                }
            }

            // 다음 달로 넘어가면 중단
            if (dayCounter > lastDay && weekIndex >= 4) {
                return@Column
            }
        }
    }
}

/**
 * 개별 날짜 셀
 */
@Composable
private fun CalendarDayCell(
    date: Calendar,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    diary: DiaryEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isToday) {
                    Modifier.border(2.dp, kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = isCurrentMonth) { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 날짜 숫자
            Text(
                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !isCurrentMonth -> Color(0xFFD1D5DB) // 다른 달: 연한 회색
                    isToday -> kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue // 오늘: 파란색
                    else -> Color(0xFF111827) // 이번 달: 검정
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 갈증 수치 점 (Dot)
            if (diary != null && isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(getCravingColor(diary.cravingLevel))
                )
            } else {
                // 빈 공간 유지 (레이아웃 일관성)
                Spacer(modifier = Modifier.size(6.dp))
            }
        }
    }
}

/**
 * 갈증 수치에 따른 색상 반환
 */
private fun getCravingColor(level: Int): Color {
    return when (level) {
        in 1..3 -> Color(0xFF4CAF50) // 초록색 (안정)
        in 4..7 -> Color(0xFFFFA726) // 주황색 (주의)
        in 8..10 -> Color(0xFFE53935) // 빨간색 (위험)
        else -> Color(0xFF9CA3AF) // 회색 (기본)
    }
}

