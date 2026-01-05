// [NEW] 일기 관련 UI 컴포넌트 (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 최근 일기 섹션
 */
@Composable
fun RecentDiarySection(
    diaries: List<DiaryEntity>,
    allDiaries: List<DiaryEntity>,
    onNavigateToAllDiaries: () -> Unit = {},
    onNavigateToDiaryWrite: (Long?) -> Unit = {},
    onDiaryClick: (DiaryEntity) -> Unit = {},
    onNavigateToDiaryDetail: (Long) -> Unit = {},
    onHeaderClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val hasAnyDiary = allDiaries.isNotEmpty()
    val latestDiaryId = allDiaries.firstOrNull()?.id

    val today = remember { Calendar.getInstance() }
    val todayDiary = remember(allDiaries) {
        allDiaries.firstOrNull { diary ->
            val diaryCal = Calendar.getInstance().apply { timeInMillis = diary.timestamp }
            diaryCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            diaryCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 헤더: 제목 + 안내 문구
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.diary_recent_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    fontSize = 18.sp
                )

                if (hasAnyDiary) {
                    Text(
                        text = stringResource(R.string.records_diary_view_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                latestDiaryId?.let { onNavigateToDiaryDetail(it) }
                            }
                        )
                    )
                }
            }

            Text(
                text = stringResource(R.string.diary_section_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 캘린더 위젯
        CalendarWidget(
            diaries = allDiaries,
            onDateClick = { selectedDate ->
                val existingDiary = allDiaries.firstOrNull {
                    val diaryCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    diaryCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    diaryCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                }

                val today = Calendar.getInstance()
                val isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                              selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                when {
                    existingDiary != null -> {
                        onNavigateToDiaryDetail(existingDiary.id)
                        android.util.Log.d("RecordsScreen", "일기 상세 피드 열기: ${existingDiary.id}")
                    }
                    isToday -> {
                        onNavigateToDiaryWrite(null)
                        android.util.Log.d("RecordsScreen", "새 일기 작성 (오늘)")
                    }
                    else -> {
                        android.util.Log.d("RecordsScreen", "과거 날짜 클릭 (일기 없음) - No action")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            onHeaderClick = onHeaderClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 작성 유도 카드 (CTA Box)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (todayDiary == null) {
                        onNavigateToDiaryWrite(null)
                    } else {
                        onNavigateToDiaryDetail(todayDiary.id)
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (todayDiary != null) Color(0xFFF0F9FF) else Color(0xFFFFFBEB)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (todayDiary != null) R.string.diary_cta_completed
                            else R.string.diary_cta_empty
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (todayDiary != null) Color(0xFF1E40AF) else Color(0xFF92400E)
                    )
                    if (todayDiary == null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.diary_cta_empty_action),
                            fontSize = 13.sp,
                            color = Color(0xFFA16207)
                        )
                    }
                }

                Icon(
                    painter = painterResource(
                        id = if (todayDiary != null) R.drawable.notebook
                        else R.drawable.ic_plus
                    ),
                    contentDescription = null,
                    tint = if (todayDiary != null) Color(0xFF3B82F6) else Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 일기 빈 상태 UI
 */
@Composable
fun DiaryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.notebook),
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(R.string.diary_no_entries),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF2D3748),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.diary_write_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * 일기 항목 아이템 (Magazine Style)
 */
@Composable
fun DiaryListItem(
    diary: DiaryEntity,
    onClick: () -> Unit = {}
) {
    val locale = Locale.getDefault()

    val (monthText, dayText, yearText) = remember(diary.timestamp, locale) {
        val cal = Calendar.getInstance().apply { timeInMillis = diary.timestamp }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        when (locale.language) {
            "ko" -> {
                val monthStr = when (month) {
                    1 -> "JAN"; 2 -> "FEB"; 3 -> "MAR"; 4 -> "APR"
                    5 -> "MAY"; 6 -> "JUN"; 7 -> "JUL"; 8 -> "AUG"
                    9 -> "SEP"; 10 -> "OCT"; 11 -> "NOV"; 12 -> "DEC"
                    else -> "DEC"
                }
                Triple(monthStr, day.toString(), "${year}년")
            }
            else -> {
                val monthStr = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(diary.timestamp)).uppercase()
                Triple(monthStr, day.toString(), year.toString())
            }
        }
    }

    val boxColor = remember(diary.id) {
        val colors = listOf(
            Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFF3E0E0),
            Color(0xFFF3E5F5), Color(0xFFFCE4EC)
        )
        colors[kotlin.math.abs(diary.id.hashCode()) % colors.size]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.size(width = 52.dp, height = 52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = boxColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = monthText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    letterSpacing = 0.3.sp,
                    lineHeight = 12.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = dayText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = diary.content,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = yearText,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = diary.emoji,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
    }
}

