package kr.sweetapps.alcoholictimer.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import java.util.Locale

/**
 * 공통 레벨 카드 컴포넌트
 * 홈 화면과 레벨 상세 화면에서 재사용
 *
 * @param currentLevel 현재 레벨 정보
 * @param currentDays 현재 금주 일수
 * @param progress 다음 레벨까지의 진행률 (0.0 ~ 1.0)
 * @param containerColor 카드 배경색 (기본: Deep Blue)
 * @param cardHeight 카드 높이
 * @param showDetailedInfo 상세 정보 표시 여부 (남은 시간 등)
 * @param onClick 클릭 이벤트 (null이면 클릭 불가)
 * @param modifier Modifier
 */
@Composable
fun LevelCard(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    progress: Float,
    containerColor: Color = Color(0xFF1E40AF), // Deep Blue
    cardHeight: Dp = 200.dp,
    showDetailedInfo: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActive = currentDays > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // [상단] 뱃지 + 텍스트
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    // Level Badge + Status Indicator
                    Box(
                        modifier = Modifier.size(72.dp)
                    ) {
                        // Glassmorphism Badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
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
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }

                        // Status Indicator (우측 상단)
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) Color(0xFF10B981) // Green
                                        else Color(0xFF6B7280) // Gray
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 레벨 이름 및 일수
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = context.getString(currentLevel.nameResId),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currentDays",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = context.getString(R.string.level_days_label),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // [NEW] 우측 화살표 아이콘 (클릭 가능할 때만 표시)
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "자세히 보기",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }

                // [하단] 프로그레스 영역
                if (showDetailedInfo) {
                    LevelProgressSection(
                        progress = progress,
                        currentLevel = currentLevel
                    )
                } else {
                    // 간소화된 프로그레스 바만 표시
                    SimpleLevelProgress(progress = progress)
                }
            }
        }
    }
}

/**
 * 상세 프로그레스 섹션 (남은 시간 정보 포함)
 */
@Composable
private fun LevelProgressSection(
    progress: Float,
    currentLevel: LevelDefinitions.LevelInfo
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Label
        Text(
            text = context.getString(R.string.level_until_next),
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Bar
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
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFBC528).copy(alpha = 0.7f),
                                Color(0xFFFBC528)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.0f%%", progress * 100.0),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * 간소화된 프로그레스 바 (배너용)
 */
@Composable
private fun SimpleLevelProgress(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.3f))
    ) {
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800),
            label = "simple_progress"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(Color(0xFFFBC528))
        )
    }
}

