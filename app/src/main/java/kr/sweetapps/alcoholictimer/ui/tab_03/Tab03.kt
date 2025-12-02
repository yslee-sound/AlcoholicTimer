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
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppCard
import kr.sweetapps.alcoholictimer.core.ui.AppColors
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
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
    MainLevelCardFrame(modifier = modifier, backgroundRes = R.drawable.for_you) {
        Column(
            modifier = Modifier
                .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 32.dp)
                .fillMaxWidth()
                .testTag("main_level_card_content"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge
            val badgeColor = currentLevel.color
            val isYellowBadge = badgeColor == Color(0xFFFBC02D)
            val centerBlend = lerp(badgeColor, Color.White, 0.12f)
            val midBlend = lerp(badgeColor, Color.White, 0.05f)
            val badgeSize = 110.dp

            Box(modifier = Modifier.padding(6.dp)) {
                Surface(
                    modifier = Modifier.size(badgeSize).testTag("main_level_badge"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    tonalElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    0.0f to centerBlend,
                                    0.35f to midBlend,
                                    1.0f to badgeColor
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val levelNumber = LevelDefinitions.getLevelNumber(currentDays) + 1
                        val badgeFontSize = if (isYellowBadge) 26.sp else 22.sp
                        Text(
                            text = "LV.$levelNumber",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            fontSize = badgeFontSize
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = context.getString(currentLevel.nameResId),
                style = MaterialTheme.typography.headlineLarge.copy(color = Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("main_level_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("main_level_days_row")
            ) {
                Text(
                    text = "$currentDays",
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.White),
                    modifier = Modifier.testTag("main_level_days_value")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = context.getString(R.string.level_days_label),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                    modifier = Modifier.testTag("main_level_days_label")
                )
            }

            if (nextLevel != null) {
                Spacer(modifier = Modifier.height(24.dp))

                val remainingDaysFloat = (nextLevel.start - elapsedDaysFloat).coerceAtLeast(0f)
                val remainingDaysInt = kotlin.math.floor(remainingDaysFloat.toDouble()).toInt()
                val remainingHoursInt = kotlin.math.floor(((remainingDaysFloat - remainingDaysInt) * 24f).toDouble()).toInt()
                val remainingText = when {
                    remainingDaysInt > 0 && remainingHoursInt > 0 -> "$remainingDaysInt${context.getString(R.string.level_day_unit)} $remainingHoursInt${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_days_remaining)}"
                    remainingDaysInt > 0 -> "$remainingDaysInt${context.getString(R.string.level_day_unit)} ${context.getString(R.string.level_days_remaining)}"
                    remainingHoursInt > 0 -> "$remainingHoursInt${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_hours_remaining)}"
                    else -> context.getString(R.string.level_soon_levelup)
                }

                ProgressToNextLevel(
                    progressFill = progress,
                    displayPercent = (progress * 100.0),
                    remainingDays = (nextLevel.start - currentDays).coerceAtLeast(0),
                    remainingText = remainingText,
                    isSobrietyActive = startTime > 0
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))

                val extraPercent = (elapsedDaysFloat - currentLevel.start).coerceAtLeast(0f)
                val displayPercent = 100.0 + extraPercent.toDouble()
                val rem = displayPercent % 100.0
                val progressFill = if (displayPercent >= 100.0) {
                    if (rem == 0.0) 1.0f else (rem / 100.0).toFloat()
                } else {
                    (displayPercent / 100.0).toFloat()
                }

                ProgressToNextLevel(
                    progressFill = progressFill,
                    displayPercent = displayPercent,
                    remainingDays = 0,
                    remainingText = context.getString(currentLevel.nameResId),
                    isSobrietyActive = startTime > 0
                )
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
                Text(
                    text = "$levelNumber",
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

