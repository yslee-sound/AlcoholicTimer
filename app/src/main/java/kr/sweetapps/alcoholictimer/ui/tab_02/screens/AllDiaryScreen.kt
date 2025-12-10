// [NEW] AllDiaryScreen: 월별로 그룹화된 모든 일기 보기 화면
package kr.sweetapps.alcoholictimer.ui.tab_02.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllDiaryScreen(
    onNavigateBack: () -> Unit = {},
    onOpenDiaryDetail: (Long) -> Unit = {} // 상세화면으로 이동할 때 사용할 ID
) {
    // [NEW] ViewModel을 통해 Room DB 데이터 가져오기
    val viewModel: DiaryViewModel = viewModel()
    val diaries by viewModel.uiState.collectAsState()

    // [NEW] Group diaries by year/month (preserve order newest -> oldest)
    val grouped = remember(diaries) {
        val map = LinkedHashMap<String, MutableList<DiaryEntity>>()
        diaries.forEach { d ->
            val cal = Calendar.getInstance().apply { timeInMillis = d.timestamp }
            val key = "${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월"
            val list = map.getOrPut(key) { mutableListOf() }
            list.add(d)
        }
        map
    }

    Scaffold(
        topBar = {
            // [NEW] 공통 뒤로가기 제목줄로 통일
            BackTopBar(title = "나의 금주 일지", onBack = onNavigateBack)
        }
    ) { innerPadding ->
        val state = rememberLazyListState()
        if (grouped.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(text = "작성된 일기가 없습니다.", color = Color(0xFF64748B))
            }
            return@Scaffold
        }

        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // [UPDATED] Clean white background
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp) // [NEW] Breathing room at bottom
        ) {
            grouped.forEach { (month, list) ->
                // [UPDATED] Modern & minimal month header
                item(key = "header_$month") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month text (bold, prominent)
                        Text(
                            text = month,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A1A), // Dark grey/black for emphasis
                            modifier = Modifier.weight(1f)
                        )

                        // Subtle divider line on the right (optional, modern touch)
                        HorizontalDivider(
                            modifier = Modifier.weight(1.5f),
                            thickness = 1.dp,
                            color = Color(0xFFE2E8F0) // Light grey divider
                        )
                    }
                }

                list.forEachIndexed { index, diary ->
                    item(key = diary.id) {
                        // [UPDATED] Clean white background list item (matches Tab 1 style)
                        CleanDiaryListItem(diary = diary, onClick = { onOpenDiaryDetail(diary.id) })

                        // [NEW] Add divider between items (not after last item in group)
                        if (index < list.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 1.dp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * [UPDATED] Clean white background list item (matches Tab 1 "최근 금주 일기" style)
 * - No Card, No elevation, No grey background
 * - Simple Row with Date + Emoji + Content
 * - Dividers added between items in parent
 * - [FIX] Date split into 2 lines, Emoji in fixed box, Content uses weight(1f)
 */
@Composable
private fun CleanDiaryListItem(diary: DiaryEntity, onClick: () -> Unit) {
    // Parse date string (e.g., "2025년 12월 7일") into parts
    val dateText = diary.date
    val (yearMonth, day) = remember(dateText) {
        try {
            // Extract year, month, day from "yyyy년 MM월 dd일"
            val parts = dateText.split("년", "월", "일").map { it.trim() }
            if (parts.size >= 3) {
                val year = parts[0].takeLast(2) // Last 2 digits of year (e.g., "25")
                val month = parts[1].padStart(2, '0')
                val day = parts[2].padStart(2, '0')
                "$year.$month" to "${day}일"
            } else {
                dateText to "" // Fallback
            }
        } catch (e: Exception) {
            dateText to ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White) // Clean white background
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Date Column (Fixed Width, 2 lines)
        Column(
            modifier = Modifier.width(50.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Line 1: Year.Month (e.g., "25.12")
            Text(
                text = yearMonth,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8), // Light grey
                lineHeight = 14.sp
            )
            // Line 2: Day (e.g., "07일")
            Text(
                text = day,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748), // Dark grey/black
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Emoji (Fixed Size Box to prevent clipping)
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = diary.emoji,
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 3. Content Preview (Fills remaining space with weight)
        Text(
            text = diary.content.ifEmpty { "내용 없음" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1E293B), // Dark grey/black
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f) // [FIX] Take remaining space
        )
    }
}

/**
 * [DEPRECATED] Old grey card style - kept for reference
 */
@Composable
private fun DiaryCardItem(diary: DiaryEntity, onClick: () -> Unit) {
    val weekday = remember(diary.timestamp) {
        SimpleDateFormat("E", Locale.KOREAN).format(Date(diary.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Left: date and weekday
            Column(modifier = Modifier.width(72.dp)) {
                Text(text = diary.date, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(text = weekday, color = Color(0xFF64748B), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center: status badge + content
            Column(modifier = Modifier.weight(1f)) {
                // Status badge (based on craving or success heuristic)
                val isSuccess = diary.cravingLevel <= 2
                val badgeColor = if (isSuccess) Color(0xFF3B82F6) else Color(0xFFEF4444)
                Box(modifier = Modifier
                    .size(10.dp)
                    .background(badgeColor, shape = MaterialTheme.shapes.small))

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = diary.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "상세 보기",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

