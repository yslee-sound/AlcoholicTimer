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
                .padding(innerPadding)
        ) {
            grouped.forEach { (month, list) ->
                // Sticky header effect: using item with full width header above group
                item(key = "header_$month") {
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = month, fontWeight = FontWeight.SemiBold)
                    }
                }

                items(items = list, key = { it.id }) { diary ->
                    DiaryCardItem(diary = diary, onClick = { onOpenDiaryDetail(diary.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

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

