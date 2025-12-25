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
import androidx.compose.ui.text.style.TextOverflow
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
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1), // 밝은 보라색
                            Color(0xFF8B5CF6)  // 진한 보라색
                        )
                    )
                )
                // [CHANGED] padding을 Box에서 제거하고 내부 Column으로 이동 (2025-12-26)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp), // [CHANGED] Column에 padding 적용 (2025-12-26)
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
                            // [CHANGED] 0일차도 Lv.1로 표시 (2025-12-25)
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

                        // [REMOVED] Status Indicator 제거 (2025-12-26)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Level name and days
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 24.dp) // [NEW] 화살표 공간 확보 (2025-12-26)
                    ) {
                        Text(
                            text = context.getString(currentLevel.nameResId),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp, // Reduced from 20sp to fit Japanese text
                                lineHeight = 20.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible // Show all text even if it overflows slightly
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
                                // [CHANGED] plurals 사용하여 단수/복수 구분 (2025-12-25)
                                text = context.resources.getQuantityString(R.plurals.days_count, currentDays, currentDays).substringAfter(" "),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // [REMOVED] 화살표 아이콘을 Row에서 제거 - 아래 Box의 직계 자식으로 이동 (2025-12-26)
                }

                // [하단] 프로그레스 영역
                if (showDetailedInfo) {
                    LevelProgressSection(
                        progress = progress,
                        currentLevel = currentLevel,
                        currentDays = currentDays // [FIX] 현재 일수 전달
                    )
                } else {
                    // 간소화된 프로그레스 바만 표시
                    SimpleLevelProgress(progress = progress)
                }
            }

            // [NEW] 화살표 아이콘을 Box의 직계 자식으로 추가 - 우측 끝에 고정 (2025-12-26)
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "자세히 보기",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 12.dp) // 뱃지 높이의 중앙에 정렬
                        .size(32.dp)
                )
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
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int // [FIX] 현재 일수 추가
) {
    val context = LocalContext.current

    // [NEW] 다음 레벨 정보 가져오기
    val currentLevelIndex = LevelDefinitions.levels.indexOf(currentLevel)
    val nextLevel = if (currentLevelIndex in 0 until LevelDefinitions.levels.size - 1) {
        LevelDefinitions.levels[currentLevelIndex + 1]
    } else {
        null
    }

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

        // [NEW] animatedProgress를 Box 밖으로 이동 (2025-12-25)
        // [FIX] 애니메이션 시간을 50ms로 단축하여 부드럽게 실시간 반영 (2025-12-25)
        val animatedProgress = animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 50),
            label = "progress"
        ).value

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF34D399), // 밝은 민트 그린
                                Color(0xFF10B981)  // 진한 에메랄드 그린
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // [FIX] Percentage + 남은 일수 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // [FIX] 수직 중앙 정렬
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", animatedProgress * 100.0),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            // [NEW] 남은 일수 표시
            if (nextLevel != null) {
                val remainingDaysFloat = (nextLevel.start - currentDays.toFloat()).coerceAtLeast(0f)
                val remainingDaysInt = kotlin.math.floor(remainingDaysFloat.toDouble()).toInt()
                val remainingHoursInt = kotlin.math.floor(((remainingDaysFloat - remainingDaysInt) * 24f).toDouble()).toInt()

                val remainingText = when {
                    remainingDaysInt > 0 && remainingHoursInt > 0 -> "$remainingDaysInt ${context.getString(R.string.level_day_unit)} $remainingHoursInt ${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_days_remaining)}"
                    remainingDaysInt > 0 -> "$remainingDaysInt ${context.getString(R.string.level_day_unit)} ${context.getString(R.string.level_days_remaining)}"
                    remainingHoursInt > 0 -> "$remainingHoursInt ${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_hours_remaining)}"
                    else -> context.getString(R.string.level_soon_levelup)
                }

                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
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
            animationSpec = tween(durationMillis = 50),
            label = "simple_progress"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF34D399), // 밝은 민트 그린
                            Color(0xFF10B981)  // 진한 에메랄드 그린
                        )
                    )
                )
        )
    }
}
