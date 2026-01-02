package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R

/**
 * Level Summary Banner Component
 * Tab 2 upper banner with current level information
 * Click to navigate to LevelDetail screen
 * Progress bar added at bottom for better visibility
 */
@Composable
fun LevelSummaryBanner(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    progress: Float,
    startTime: Long, // [NEW] 타이머 상태 확인용 파라미터 추가
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = startTime > 0 // [FIX] currentDays -> startTime 기반으로 변경 (2026-01-02)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp), // Height increased (100dp -> 130dp) to prevent text clipping
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6)
                        )
                    )
                )
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
        ) {
            // Top area: Level info + Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Level info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Level mark badge
                    Box(modifier = Modifier.size(48.dp)) {
                        // Glassmorphism Badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // [CHANGED] 0일차도 Lv.1로 표시 (2025-12-25)
                            val levelNumber = LevelDefinitions.getLevelNumber(currentDays) + 1
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

                        // Active status indicator (green badge)
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
                                        .background(Color(0xFF22C55E))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Level info Column
                    // [FIX] Japanese text wrapping and clipping fixed with softWrap + lineHeight (2025-12-24)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 0.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Level title
                        // [UPDATED] 긴 텍스트(인도네시아어 등)를 위해 두 줄까지 허용 (2025-12-26)
                        Text(
                            text = stringResource(id = currentLevel.nameResId),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2, // [CHANGED] 1 -> 2 (두 줄까지 허용)
                            softWrap = true, // [CHANGED] false -> true (자동 줄바꿈 허용)
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp // 줄 간격 유지
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Current days
                        Text(
                            text = "${currentDays}일 달성",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Right: Arrow icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_caret_right),
                    contentDescription = "Level detail",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // Take remaining space to push progress bar to bottom

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1000),
                    label = "progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }
    }
}

