package kr.sweetapps.alcoholictimer.ui.tab_03

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.components.AppCard
import kr.sweetapps.alcoholictimer.ui.theme.AppColors
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions
import java.util.Locale

/**
 * [NEW] Tab03 - 레벨 화면 (ViewModel 사용)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: Tab03ViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // [NEW] ViewModel에서 상태 구독
    val currentTime by viewModel.currentTime.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val levelVisits by viewModel.levelVisits.collectAsState()
    val totalElapsedTime by viewModel.totalElapsedTime.collectAsState()
    val totalElapsedDaysFloat by viewModel.totalElapsedDaysFloat.collectAsState()
    val levelDays by viewModel.levelDays.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()

    // 뒤로가기 처리: 광고 정책 확인
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = true) {
        Log.d("LevelBack", "BackHandler triggered: policy check start")
        coroutineScope.launch {
            try {
                val act = activity
                Log.d("LevelBack", "Back pressed, level visits=$levelVisits")

                if (act == null || levelVisits < 3) {
                    try { onNavigateBack() } catch (_: Throwable) {}
                } else {
                    try {
                        Log.d("LevelBack", "AdController.snapshot-before -> ${AdController.debugSnapshot()}")
                        val allowed = AdController.canShowInterstitial(context)
                        Log.d("LevelBack", "AdController.canShowInterstitial returned=$allowed | snapshot-after -> ${AdController.debugSnapshot()}")

                        if (!allowed) {
                            Log.d("LevelBack", "Interstitial suppressed by AdController policy")
                            try { onNavigateBack() } catch (_: Throwable) {}
                        } else {
                            val showed = InterstitialAdManager.maybeShowIfEligible(act) {
                                Log.d("LevelBack", "Interstitial dismissed callback -> ${AdController.debugSnapshot()}")
                                viewModel.resetLevelVisits()
                                try { onNavigateBack() } catch (_: Throwable) {}
                            }
                            Log.d("LevelBack", "maybeShowIfEligible returned: $showed")
                            if (!showed) try { onNavigateBack() } catch (_: Throwable) {}
                        }
                    } catch (t: Throwable) {
                        Log.e("LevelBack", "ad check failed, navigating back", t)
                        try { onNavigateBack() } catch (_: Throwable) {}
                    }
                }
            } catch (t: Throwable) {
                Log.e("LevelBack", "BackHandler coroutine failed", t)
                activity?.finish()
            }
        }
    }

    CompositionLocalProvider(LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            val appliedBottom = 15.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = appliedBottom)
            ) {
                Log.d("LevelScreenDebug", "LEVEL_SCREEN_BOTTOM_PADDING=15.dp, appliedBottom=$appliedBottom")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp, start = 15.dp, end = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    CurrentLevelCard(
                        currentLevel = currentLevel,
                        currentDays = levelDays,
                        elapsedDaysFloat = totalElapsedDaysFloat,
                        startTime = startTime,
                        nextLevel = viewModel.getNextLevel(),
                        progress = viewModel.calculateProgress(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LevelListCard(currentLevel = currentLevel, currentDays = levelDays)
                }
            }
        }
    }
}

@Composable
fun MainLevelCardFrame(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundRes: Int? = null,
    backgroundAlpha: Float = 1.0f,
    backgroundContentScale: ContentScale = ContentScale.Crop,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardContainerColor = if (backgroundRes != null) Color.Transparent else AppColors.SurfaceOverlaySoft

    AppCard(
        modifier = modifier,
        elevation = AppElevation.CARD_HIGH,
        shape = RoundedCornerShape(16.dp),
        containerColor = cardContainerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (backgroundRes != null) {
                Image(
                    painter = painterResource(id = backgroundRes),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(1.4f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = backgroundContentScale,
                    alignment = Alignment.TopCenter,
                    alpha = backgroundAlpha
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.88f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.12f)
                            )
                        )
                )
            }

            Column(content = content)
        }
    }
}

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

    // [REDESIGN] Gradient Card - 이미지 디자인 적용
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
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFC084FC), // 좌측 상단 (보라)
                            Color(0xFFF43F5E)  // 우측 하단 (분홍)
                        )
                    )
                )
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
                        val rem = displayPercent % 100.0
                        val progressFill = if (displayPercent >= 100.0) {
                            if (rem == 0.0) 1.0f else (rem / 100.0).toFloat()
                        } else {
                            (displayPercent / 100.0).toFloat()
                        }

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

@Composable
private fun LevelListCard(currentLevel: LevelDefinitions.LevelInfo, currentDays: Int) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(top = 0.dp)) {
            Text(
                text = context.getString(R.string.level_all_levels),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                ),
                modifier = Modifier.padding(start = 2.dp, bottom = 24.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                LevelDefinitions.levels.forEach { level ->
                    LevelItem(
                        level = level,
                        isCurrent = level == currentLevel,
                        isAchieved = currentDays >= level.start,
                        isNext = level == getNextLevel(currentLevel)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelItem(
    level: LevelDefinitions.LevelInfo,
    isCurrent: Boolean,
    isAchieved: Boolean,
    isNext: Boolean
) {
    val context = LocalContext.current
    val itemElevation = AppElevation.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> level.color.copy(alpha = 0.1f)
                isAchieved -> level.color.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isCurrent -> BorderStroke(1.5.dp, level.color)
            isAchieved -> BorderStroke(1.dp, level.color.copy(alpha = 0.6f))
            else -> BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = itemElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAchieved) level.color else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                val levelNumber = LevelDefinitions.levels.indexOf(level) + 1
                // [FIX] Legend 레벨(11)은 "L"로 표시
                val levelText = if (levelNumber == 11) "L" else "$levelNumber"
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isAchieved) Color.White else Color(0xFF757575)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = context.getString(level.nameResId),
                    style = (if (isCurrent) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium)
                        .copy(color = if (isAchieved) level.color else Color(0xFF757575))
                )

                val dayUnit = context.getString(R.string.level_day_unit)
                val rangeText = if (level.end == Int.MAX_VALUE) "${level.start}$dayUnit+" else "${level.start}~${level.end}$dayUnit"
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF666666))
                )
            }

            when {
                isCurrent -> Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "현재 레벨",
                    tint = level.color,
                    modifier = Modifier.size(20.dp)
                )
                isAchieved -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "달성 완료",
                    tint = level.color,
                    modifier = Modifier.size(20.dp)
                )
                else -> Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "미달성",
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getNextLevel(currentLevel: LevelDefinitions.LevelInfo): LevelDefinitions.LevelInfo? {
    val currentIndex = LevelDefinitions.levels.indexOf(currentLevel)
    return if (currentIndex in 0 until LevelDefinitions.levels.size - 1) {
        LevelDefinitions.levels[currentIndex + 1]
    } else {
        null
    }
}

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

/**
 * [NEW] Full Width 프로그레스 바 - Hybrid Layout용
 */
@Composable
private fun FullWidthProgressBar(
    progressFill: Float,
    displayPercent: Double,
    remainingText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 진행률 & 남은 시간 라벨
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                text = remainingText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 프로그레스 바 (Full Width)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.3f))
                .testTag("fullwidth_progress")
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progressFill,
                animationSpec = tween(durationMillis = 1000),
                label = "fullwidth_progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFBC528).copy(alpha = 0.8f),
                                Color(0xFFFBC528)
                            )
                        )
                    )
                    .testTag("fullwidth_progress_fill")
            )
        }
    }
}

/**
 * [DEPRECATED] 컴팩트 프로그레스 바 - 가로 배치 레이아웃용
 */
@Composable
private fun CompactProgressBar(
    progressFill: Float,
    displayPercent: Double,
    remainingText: String,
    isSobrietyActive: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 프로그레스 바
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.3f))
                .testTag("compact_progress")
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progressFill,
                animationSpec = tween(durationMillis = 1000),
                label = "compact_progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFBC528).copy(alpha = 0.8f),
                                Color(0xFFFBC528)
                            )
                        )
                    )
                    .testTag("compact_progress_fill")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 진행률 & 남은 시간
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.0f%%", displayPercent),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
        }
    }
}


