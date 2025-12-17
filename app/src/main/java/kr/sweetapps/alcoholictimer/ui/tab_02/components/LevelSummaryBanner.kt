package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * [UPDATE] 레벨 마크와 진행 중 배지 추가
 */
@Composable
fun LevelSummaryBanner(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = currentDays > 0 // 1일 이상이면 활성 상태

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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // [NEW] 레벨 마크 뱃지 (축소 버전)
                Box(
                    modifier = Modifier.size(48.dp)
                ) {
                    // Glassmorphism Badge
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val levelNumber = if (currentDays < 1) {
                            0
                        } else {
                            LevelDefinitions.getLevelNumber(currentDays) + 1
                        }
                        val levelText = if (levelNumber == 11) "L" else "$levelNumber"
                        Text(
                            text = "LV.$levelText",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    // [NEW] 진행 중 상태 표시 (초록색 배지)
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)) // 초록색
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 레벨 정보 Column
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    // 레벨 타이틀
                    Text(
                        text = stringResource(id = currentLevel.nameResId),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 현재 일수
                    Text(
                        text = "${currentDays}일 달성",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // 우측: 화살표 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "레벨 상세 보기",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // [REMOVED] 진행률 바와 텍스트 제거 (축소 버전이므로)
    }
}

