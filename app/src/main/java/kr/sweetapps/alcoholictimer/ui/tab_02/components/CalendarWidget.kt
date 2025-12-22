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
import androidx.compose.ui.draw.shadow
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
 * [MODIFIED] UI 디자인 고도화 (2025-12-22)
 * - 전체 너비 확장, 요일 색상 구분, 선택 시 solid circle 배경
 */
@Composable
fun CalendarWidget(
    diaries: List<DiaryEntity>,
    onDateClick: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    // 현재 표시 중인 년월 (사용자가 이동 가능)
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // [NEW] 선택된 날짜 상태 (2025-12-22)
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }

    // 다이어리 데이터를 날짜별로 매핑 (yyyy-MM-dd -> DiaryEntity)
    val diaryMap = remember(diaries) {
        diaries.associateBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(), // [FIX] 내부 패딩 제거 - 상위 레이아웃 패딩과 중복 방지 (2025-12-22)
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

            // [MODIFIED] 요일 행 - 색상 구분 (2025-12-22)
            WeekdayRow()

            Spacer(modifier = Modifier.height(12.dp)) // [MODIFIED] 간격 조정 (2025-12-22)

            // 날짜 그리드
            CalendarGrid(
                calendar = currentCalendar,
                diaryMap = diaryMap,
                selectedDate = selectedDate, // [NEW] 선택된 날짜 전달 (2025-12-22)
                onDateClick = { date ->
                    selectedDate = date // [NEW] 선택 상태 업데이트 (2025-12-22)
                    onDateClick(date)
                }
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
 * [MODIFIED] 요일 행 - 색상 구분 (2025-12-22)
 * - 일요일: 빨강, 토요일: 파랑, 평일: 검정
 * - 모두 Bold 처리
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
        weekdays.forEachIndexed { index, day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                // [MODIFIED] 요일별 색상 구분 (2025-12-22)
                color = when (index) {
                    0 -> Color(0xFFE53935) // 일요일: 빨강
                    6 -> Color(0xFF1E88E5) // 토요일: 파랑
                    else -> Color(0xFF111111) // 평일: 진한 검정
                },
                fontWeight = FontWeight.Bold, // [MODIFIED] 모두 굵게 (2025-12-22)
                fontSize = 13.sp, // [MODIFIED] 크기 조정 (2025-12-22)
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * [MODIFIED] 날짜 그리드 - 선택 상태 지원 (2025-12-22)
 */
@Composable
private fun CalendarGrid(
    calendar: Calendar,
    diaryMap: Map<String, DiaryEntity>,
    selectedDate: Calendar?, // [NEW] 선택된 날짜 (2025-12-22)
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
        verticalArrangement = Arrangement.spacedBy(0.dp) // [FIX] Row 사이 간격 축소 (8dp -> 4dp) - 날짜 셀 간격 좁힘 (2025-12-22)
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

                    // [NEW] 선택 여부 확인 (2025-12-22)
                    val isSelected = isCurrentMonth && selectedDate != null &&
                            date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                            date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)

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
                        isSelected = isSelected, // [NEW] 선택 상태 전달 (2025-12-22)
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
 * [MODIFIED] 개별 날짜 셀 - Solid Circle 선택, 간격 개선 (2025-12-22)
 * - 선택 시: 파란 원형 배경 + 흰색 텍스트
 * - 오늘: 연한 배경 (선택되지 않은 경우)
 * - 숫자 크기 축소, 숫자와 점 사이 간격 확보
 */
@Composable
private fun CalendarDayCell(
    date: Calendar,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean, // [NEW] 선택 상태 (2025-12-22)
    diary: DiaryEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(0.65f) // [FIX] 세로 공간 확보 (0.75 -> 0.65) - 38dp 원과 10dp 점 모두 수용 (2025-12-22)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isCurrentMonth) { onClick() }
            .padding(vertical = 2.dp), // [FIX] 패딩 최소화 (6dp -> 2dp) - 콘텐츠 공간 확보 (2025-12-22)
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // [MODIFIED] 날짜 숫자를 원형 배경으로 감싸기 (2025-12-22)
        Box(
            modifier = Modifier
                .size(if (isToday) 38.dp else 28.dp) // [중요] 오늘 날짜 38dp 크기 유지 (수정 금지)
                .then(
                    if (isToday) {
                        // [NEW] 오늘 날짜에 그림자 효과 추가 (2025-12-22)
                        Modifier.shadow(
                            elevation = 4.dp,
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    color = when {
                        isToday -> Color(0xFF6366F1) // [MODIFIED] 오늘: #6366F1 파란 원 + 그림자
                        isSelected -> kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue // 선택: 파란 원
                        else -> Color.Transparent // 그 외: 투명
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !isCurrentMonth -> Color(0xFFD1D5DB) // 다른 달: 연한 회색
                    isToday -> Color.White // 오늘: 흰색
                    isSelected -> Color.White // 선택: 흰색
                    else -> Color(0xFF111827) // 이번 달: 검정
                },
                fontWeight = FontWeight.Bold // [MODIFIED] 모든 날짜 Bold 처리
            )
        }

        Spacer(modifier = Modifier.height(6.dp)) // 숫자와 점 사이 간격

        // 갈증 수치 점 (Dot)
        if (diary != null && isCurrentMonth) {
            Box(
                modifier = Modifier
                    .size(10.dp) // [유지] 10dp 점 크기 유지 - 명확한 가시성
                    .clip(CircleShape)
                    .background(kr.sweetapps.alcoholictimer.util.ThirstColorUtil.getColor(diary.cravingLevel))
            )
        } else {
            // 빈 공간 유지 (레이아웃 흔들림 방지)
            Spacer(modifier = Modifier.size(10.dp))
        }
    }
}

