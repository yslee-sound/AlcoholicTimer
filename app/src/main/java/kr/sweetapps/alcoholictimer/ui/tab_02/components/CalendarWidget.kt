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

    // [FIX] diaryMap 생성 - 타임존 문제 해결 (2025-12-22)
    val diaryMap = remember(diaries) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        diaries.associateBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            formatter.format(cal.time)
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

            // [FIX] 날짜 그리드 - diaryMap 사용 (2025-12-22)
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
 * [FIX] diaryMap 사용 및 클릭 로직 개선 (2025-12-22)
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

    // [NEW] 날짜 포맷터 (2025-12-22)
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // 달력 그리드 생성
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
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
                        // [FIX] 시간 정보 초기화 (2025-12-22)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val isCurrentMonth = dayCounter in 1..lastDay
                    val isToday = isCurrentMonth &&
                            date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                    // [FIX] 미래 날짜 체크
                    val isFuture = date.get(Calendar.YEAR) > today.get(Calendar.YEAR) ||
                            (date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                             date.get(Calendar.DAY_OF_YEAR) > today.get(Calendar.DAY_OF_YEAR))

                    // [NEW] 선택 여부 확인
                    val isSelected = isCurrentMonth && selectedDate != null &&
                            date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                            date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)

                    // [FIX] dateKey 생성 및 일기 조회 (2025-12-22)
                    val dateKey = dateFormatter.format(date.time)
                    val diary = diaryMap[dateKey]


                    CalendarDayCell(
                        date = date,
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        isFuture = isFuture,
                        isSelected = isSelected,
                        diary = diary,
                        onClick = {
                            // [FIX] 일기가 있으면 클릭 허용, 없으면 현재 월+미래 아닌 경우만 (2025-12-22)
                            if (diary != null || (isCurrentMonth && !isFuture)) {
                                onDateClick(date)
                            }
                        },
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
 * [MODIFIED] 개별 날짜 셀 - Solid Circle 선택, 간격 개선, 미래 날짜 비활성화 (2025-12-22)
 * - 선택 시: 파란 원형 배경 + 흰색 텍스트
 * - 오늘: 연한 배경 (선택되지 않은 경우)
 * - 미래 날짜: 연한 회색으로 비활성화 표시
 * - 숫자 크기 축소, 숫자와 점 사이 간격 확보
 */
@Composable
private fun CalendarDayCell(
    date: Calendar,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isFuture: Boolean, // [NEW] 미래 날짜 여부 (2025-12-22)
    isSelected: Boolean,
    diary: DiaryEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(0.65f) // [FIX] 세로 공간 확보 (0.75 -> 0.65) - 38dp 원과 10dp 점 모두 수용 (2025-12-22)
            .padding(vertical = 2.dp), // [FIX] 패딩 최소화 (6dp -> 2dp) - 콘텐츠 공간 확보 (2025-12-22)
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // [MODIFIED] 날짜 숫자를 원형 배경으로 감싸기 (2025-12-22)
        Box(
            modifier = Modifier
                .size(
                    // [FIX] 오늘과 선택된 날짜 모두 38dp 통일 (2025-12-22)
                    if (isToday || isSelected) 38.dp else 28.dp
                )
                .clip(CircleShape) // [핵심] 리플 효과를 이 원형 안으로 제한
                .clickable(enabled = isCurrentMonth && !isFuture) { onClick() } // [FIX] clickable을 clip 뒤로 이동 (2025-12-22)
                .then(
                    if (isToday) {
                        // [NEW] 오늘 날짜에만 그림자 효과 (2025-12-22)
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
                        isToday -> Color(0xFF6366F1) // [오늘] 진한 파랑 + 그림자
                        isSelected -> Color(0xFFE5E7EB) // [선택] 연한 회색 (오늘과 구분)
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
                    !isCurrentMonth || isFuture -> Color(0xFFD1D5DB) // 비활성: 연한 회색
                    isToday -> Color.White // 오늘(파란 원): 흰색
                    isSelected -> Color(0xFF111111) // 선택(회색 원): 검정
                    else -> Color(0xFF111827) // 나머지: 검정
                },
                fontWeight = FontWeight.Bold
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

/**
 * [PREVIEW] 캘린더 위젯 프리뷰
 */
@androidx.compose.ui.tooling.preview.Preview(
    name = "캘린더 위젯 - 빈 상태",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun CalendarWidgetPreview_Empty() {
    MaterialTheme {
        CalendarWidget(
            diaries = emptyList(),
            onDateClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "캘린더 위젯 - 일기 데이터 포함",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun CalendarWidgetPreview_WithData() {
    MaterialTheme {
        val sampleDiaries = listOf(
            DiaryEntity(
                id = 1,
                timestamp = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 5)
                }.timeInMillis,
                date = "2025-12-05",
                content = "좋은 하루",
                emoji = "😊",
                cravingLevel = 2
            ),
            DiaryEntity(
                id = 2,
                timestamp = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 10)
                }.timeInMillis,
                date = "2025-12-10",
                content = "조금 힘든 날",
                emoji = "😐",
                cravingLevel = 5
            ),
            DiaryEntity(
                id = 3,
                timestamp = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 15)
                }.timeInMillis,
                date = "2025-12-15",
                content = "매우 힘듦",
                emoji = "😰",
                cravingLevel = 8
            ),
            DiaryEntity(
                id = 4,
                timestamp = Calendar.getInstance().timeInMillis, // 오늘
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                content = "오늘 일기",
                emoji = "🎉",
                cravingLevel = 3
            )
        )

        CalendarWidget(
            diaries = sampleDiaries,
            onDateClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "캘린더 위젯 - 다양한 갈증 수치",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun CalendarWidgetPreview_VariousLevels() {
    MaterialTheme {
        val cal = Calendar.getInstance()
        val sampleDiaries = (1..20).map { day ->
            DiaryEntity(
                id = day.toLong(),
                timestamp = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, day)
                }.timeInMillis,
                date = "2025-12-${String.format("%02d", day)}",
                content = "Day $day",
                emoji = "📝",
                cravingLevel = (day % 10) + 1 // 1~10 순환
            )
        }

        CalendarWidget(
            diaries = sampleDiaries,
            onDateClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
