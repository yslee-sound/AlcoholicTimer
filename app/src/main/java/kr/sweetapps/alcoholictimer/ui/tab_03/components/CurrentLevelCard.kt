package kr.sweetapps.alcoholictimer.ui.tab_03.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kr.sweetapps.alcoholictimer.R
import java.util.Locale

/**
 * 현재 레벨 카드 컴포넌트
 * 사용자의 현재 레벨, 일수, 진행률을 표시하는 메인 카드
 */
@Composable
fun CurrentLevelCard(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    elapsedDaysFloat: Float,
    startTime: Long,
    nextLevel: LevelDefinitions.LevelInfo?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActive = startTime > 0

    // 단색 배경 카드 - 파란색
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E40AF))
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
                            val levelNumber = LevelDefinitions.getLevelNumber(currentDays) + 1
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
                                        if (isActive) Color(0xFF22C55E) else Color(0xFF9CA3AF)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 텍스트 정보
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Title
                        Text(
                            text = context.getString(currentLevel.nameResId),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle (숫자 크게, 단위 작게)
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$currentDays",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
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
                }

                // [하단] 프로그레스 영역
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
                            targetValue = progress,
                            animationSpec = tween(durationMillis = 1000),
                            label = "gradient_progress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.9f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Footer Info
                    if (nextLevel != null) {
                        val remainingDaysFloat = (nextLevel.start - currentDays.toFloat()).coerceAtLeast(0f)
                        val remainingDaysInt = kotlin.math.floor(remainingDaysFloat.toDouble()).toInt()
                        val remainingHoursInt = kotlin.math.floor(((remainingDaysFloat - remainingDaysInt) * 24f).toDouble()).toInt()
                        val remainingText = when {
                            remainingDaysInt > 0 && remainingHoursInt > 0 -> "$remainingDaysInt${context.getString(R.string.level_day_unit)} $remainingHoursInt${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_days_remaining)}"
                            remainingDaysInt > 0 -> "$remainingDaysInt${context.getString(R.string.level_day_unit)} ${context.getString(R.string.level_days_remaining)}"
                            remainingHoursInt > 0 -> "$remainingHoursInt${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_hours_remaining)}"
                            else -> context.getString(R.string.level_soon_levelup)
                        }

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
                            Text(
                                text = remainingText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    } else {
                        val extraPercent = (elapsedDaysFloat - currentLevel.start).coerceAtLeast(0f)
                        val displayPercent = 100.0 + extraPercent.toDouble()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.0f%%", displayPercent),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = context.getString(currentLevel.nameResId),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 프로그레스 바 (사용하지 않음 - 향후 확장용)
 */
@Composable
private fun ProgressToNextLevel(
    progressFill: Float,
    displayPercent: Double,
    remainingDays: Int,
    remainingText: String,
    isSobrietyActive: Boolean
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(remainingDays, isSobrietyActive) {
        if (remainingDays > 0 && isSobrietyActive) {
            while (true) {
                delay(1000)
                isVisible = !isVisible
            }
        } else {
            isVisible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "indicator_blink"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = context.getString(R.string.level_until_next),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (remainingDays > 0 && isSobrietyActive) {
                            Color(0xFFFBC528).copy(alpha = alpha)
                        } else {
                            Color(0xFF999999)
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
                .testTag("main_level_progress")
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progressFill,
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
                    .testTag("main_level_progress_fill")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.0f%%", displayPercent),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontSize = 18.sp
                )
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontSize = 18.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) { }
    }
}

