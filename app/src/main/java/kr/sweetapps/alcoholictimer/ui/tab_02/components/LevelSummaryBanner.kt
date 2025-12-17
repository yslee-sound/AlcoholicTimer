package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions

/**
 * 레벨 요약 배너
 * Tab 2의 상단에 표시되는 현재 레벨 정보
 * 클릭 시 LevelDetail 화면으로 이동
 */
@Composable
fun LevelSummaryBanner(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6366F1), // Indigo
                        Color(0xFF8B5CF6)  // Purple
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 레벨 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 레벨 타이틀
                Text(
                    text = stringResource(id = currentLevel.nameResId),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 현재 일수
                Text(
                    text = "${currentDays}일 달성",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 진행률 바
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 진행률 텍스트
                Text(
                    text = "다음 레벨까지 ${((1f - progress) * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // 우측: 화살표 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "레벨 상세 보기",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

