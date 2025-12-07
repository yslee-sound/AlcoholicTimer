package kr.sweetapps.alcoholictimer.ui.tab_05.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.NotificationItem
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 알림 목록 화면
 */
@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF7)) // BackgroundCream
    ) {
        BackTopBar(title = "알림 센터", onBack = onBack)

        when (val state = uiState) {
            is NotificationViewModel.UiState.Loading -> {
                LoadingView()
            }
            is NotificationViewModel.UiState.Success -> {
                NotificationList(notifications = state.notifications)
            }
            is NotificationViewModel.UiState.Empty -> {
                EmptyView()
            }
            is NotificationViewModel.UiState.Error -> {
                ErrorView(message = state.message)
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF8A6CFF)
        )
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bell),
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "새로운 알림이 없습니다.",
                fontSize = 16.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bell),
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "오류가 발생했습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun NotificationList(notifications: List<NotificationItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { notification ->
            NotificationCard(notification = notification)
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 아이콘 (타입별 분기)
            Icon(
                painter = painterResource(
                    id = when (notification.type) {
                        "EVENT" -> R.drawable.star
                        "UPDATE" -> R.drawable.bell
                        else -> R.drawable.bell // NOTICE
                    }
                ),
                contentDescription = null,
                tint = when (notification.type) {
                    "EVENT" -> Color(0xFFFBC02D) // 노란색
                    "UPDATE" -> Color(0xFF8A6CFF) // 보라색
                    else -> Color(0xFF666666) // 회색
                },
                modifier = Modifier.size(24.dp)
            )

            // 콘텐츠
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Text(
                    text = notification.content,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(notification.timestamp), // [NEW] Date? 타입 처리
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

// [NEW] Date?을 받아서 안전하게 포맷. null이면 오늘 날짜로 포맷하여 표시(빈 공간 방지)
private fun formatTimestamp(timestamp: Date?): String {
    val dateToFormat = timestamp ?: Date() // null이면 현재 날짜 사용
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return dateFormat.format(dateToFormat)
}

// ==================== Preview ====================

@Preview(showBackground = true)
@Composable
fun NotificationListScreenPreview() {
    // 더미 데이터로 프리뷰
    val dummyNotifications = listOf(
        NotificationItem(
            id = "1",
            title = "앱 업데이트 안내",
            content = "새로운 기능이 추가되었습니다. 지금 업데이트하세요!",
            timestamp = Date(), // [NEW] Date 객체 사용
            type = "UPDATE"
        ),
        NotificationItem(
            id = "2",
            title = "이벤트 공지",
            content = "7일 챌린지 완료 시 특별 보상을 드립니다.",
            timestamp = Date(System.currentTimeMillis() - 86400000L), // 1일 전
            type = "EVENT"
        ),
        NotificationItem(
            id = "3",
            title = "서비스 점검 안내",
            content = "서버 점검이 진행됩니다.\n시간: 2025.12.10 02:00~04:00",
            timestamp = Date(System.currentTimeMillis() - 172800000L), // 2일 전
            type = "NOTICE"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF7))
    ) {
        BackTopBar(title = "알림 센터", onBack = {})
        NotificationList(notifications = dummyNotifications)
    }
}
