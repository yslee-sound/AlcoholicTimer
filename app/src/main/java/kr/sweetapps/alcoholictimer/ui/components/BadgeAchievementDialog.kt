package kr.sweetapps.alcoholictimer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kr.sweetapps.alcoholictimer.R

/**
 * 배지 획득 축하 애니메이션 다이얼로그
 *
 * 알림 클릭으로 진입 시 7일/30일 달성 배지 표시
 *
 * @param days 달성 일수 (7 또는 30)
 * @param onDismiss 다이얼로그 닫기 콜백
 *
 * @since 2025-12-31
 */
@Composable
fun BadgeAchievementDialog(
    days: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    // 애니메이션 시작
    LaunchedEffect(Unit) {
        visible = true
        // 3초 후 자동 닫기
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            BadgeContent(days = days, visible = visible)
        }
    }
}

@Composable
private fun BadgeContent(days: Int, visible: Boolean) {
    // 스케일 애니메이션
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // 회전 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 폭죽 효과 (간단한 원형 애니메이션)
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // 배경 원형 (펄스 효과)
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulse)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 배지 아이콘
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getBadgeEmoji(days),
                    fontSize = 60.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 축하 텍스트
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎉 축하합니다! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = getBadgeTitle(days),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = getBadgeMessage(days),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * 일수별 배지 이모지 반환
 */
private fun getBadgeEmoji(days: Int): String {
    return when (days) {
        3 -> "🌱"
        7 -> "🏆"
        30 -> "👑"
        else -> "⭐"
    }
}

/**
 * 일수별 배지 제목 반환
 */
private fun getBadgeTitle(days: Int): String {
    return when (days) {
        3 -> "3일 달성!"
        7 -> "일주일 달성!"
        30 -> "한 달 달성!"
        else -> "$days 일 달성!"
    }
}

/**
 * 일수별 배지 메시지 반환
 */
private fun getBadgeMessage(days: Int): String {
    return when (days) {
        3 -> "첫 걸음이 가장 어려운 법!\n이미 수면의 질이 개선되고 있어요."
        7 -> "일주일 동안 정말 수고하셨어요!\n간 기능이 개선되기 시작했습니다."
        30 -> "한 달 동안 대단한 노력이었어요!\n새로운 습관이 완전히 자리 잡았습니다."
        else -> "계속해서 멋진 여정을 이어가세요!"
    }
}

