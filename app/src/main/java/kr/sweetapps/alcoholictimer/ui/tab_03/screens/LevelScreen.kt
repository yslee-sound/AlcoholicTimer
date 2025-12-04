// [NEW] Tab03 리팩토링: LevelScreen을 tab_03/screens로 이동
package kr.sweetapps.alcoholictimer.ui.tab_03.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.data.repository.RecordsDataLoader
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import androidx.compose.foundation.BorderStroke
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import kr.sweetapps.alcoholictimer.ui.theme.AppColors
import kr.sweetapps.alcoholictimer.ui.components.AppCard
import androidx.compose.ui.draw.scale

// LevelActivity removed. LevelScreen is now hosted by Compose NavHost (Screen.Level route).
// If you need a legacy activity for other entry points, create a thin wrapper that calls LevelScreen via setContent.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val LEVEL_VISITS_KEY = "level_visits"

    // 화면 진입 카운터: LevelScreen이 NavHost로 네비게이션될 때마다 1증가
    LaunchedEffect(Unit) {
        try {
            val prev = sharedPref.getInt(LEVEL_VISITS_KEY, 0)
            val next = prev + 1
            sharedPref.edit().putInt(LEVEL_VISITS_KEY, next).apply()
            Log.d("LevelBack", "LevelScreen visited; count=$next")
        } catch (_: Throwable) {}
    }

    // 뒤로가기 처리: 더 이상 레벨 화면에서 자동 전면광고를 트리거하지 않음
    // [NEW] 방문 카운터는 뒤로가기 시 초기화하고 즉시 네비게이션을 수행합니다.
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = true) {
        Log.d("LevelBack", "BackHandler triggered: navigating back without interstitial")
        coroutineScope.launch {
            try {
                try { sharedPref.edit().putInt(LEVEL_VISITS_KEY, 0).apply() } catch (_: Throwable) {}
                try { onNavigateBack() } catch (_: Throwable) {}
            } catch (t: Throwable) {
                Log.e("LevelBack", "BackHandler coroutine failed", t)
                activity?.finish()
            }
        }
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val startTime = sharedPref.getLong("start_time", 0L)

    val currentElapsedTime = if (startTime > 0) currentTime - startTime else 0L

    val pastRecords = RecordsDataLoader.loadSobrietyRecords(context)
    val totalPastDuration = pastRecords.sumOf { record -> (record.endTime - record.startTime) }

    val totalElapsedTime = totalPastDuration + currentElapsedTime
    // [FIX] 시간 배속 적용: getDayInMillis() 함수 사용
    val totalElapsedDaysFloat = totalElapsedTime / Constants.getDayInMillis(context).toFloat()

    val levelDays = Constants.calculateLevelDays(totalElapsedTime)
    val currentLevel = LevelDefinitions.getLevelInfo(levelDays)

    // BaseScreen?�서 ?�공?�는 ?�단 ?�전 ?�딩(LocalSafeContentPadding)?????�면?�서??무시?�고
    // LEVEL_SCREEN_BOTTOM_PADDING ?�수로만 ?�단 ?�백???�어?�도�?CompositionLocal????��?�니??
    CompositionLocalProvider(LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)) {
        // We'll rely on navigationBarsPadding() on the scroll content for consistent inset handling.

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Use surface for the screen background so the flat LevelListCard blends to the bottom
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Scrollable area (does NOT include safeBottom) ??explicitly paint it with surface so it blends to bottom
            // Use only the level-screen bottom padding so spacing is not stacked with host reserves.
            // This makes the content extend behind the BottomNavBar; the nav will overlay on top.
            val appliedBottom = 15.dp
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                // Apply adjusted bottom so visual gap above bottom nav equals LEVEL_SCREEN_BOTTOM_PADDING
                .padding(bottom = appliedBottom)
            ) {
                // Debug: log computed appliedBottom for diagnosis
                Log.d("LevelScreenDebug", "LEVEL_SCREEN_BOTTOM_PADDING=15.dp, appliedBottom=$appliedBottom")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // ?�평 ?�백??LEVEL_SCREEN_HORIZONTAL_PADDING?�로 ?�일?�여
                        // ?�단 메인카드?� ?�단 리스?�카?�의 좌우 ?�백????곳에??관리합?�다.
                        .padding(top = 15.dp, start = 15.dp, end = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 변경: float 경과 일수 전달
                    // Only the top main card should have the horizontal screen padding; LevelListCard should span edge-to-edge.
                    CurrentLevelCard(
                        currentLevel = currentLevel,
                        currentDays = levelDays,
                        elapsedDaysFloat = totalElapsedDaysFloat,
                        startTime = startTime,
                        // 카드 ?�형 ??? 부모의 LEVEL_SCREEN_HORIZONTAL_PADDING?�로�?관리되?�록
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Explicit external bottom gap for the main card
                    Spacer(modifier = Modifier.height(24.dp))

                    LevelListCard(currentLevel = currentLevel, currentDays = levelDays)
                }
            }

            // No separate bottom band ??nav inset is included as bottom padding inside the scroll area.
        }
    } // end CompositionLocalProvider override
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
            // 배경 이미지는 카드 전체를 채우도록 (그리고 표시)
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
                // Overlay: apply same subtle vertical overlay used on StartScreen/Run screens
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    MainLevelCardFrame(modifier = modifier, backgroundRes = R.drawable.for_you) {
        // 카드 ?�체????? 부모에???�어?��?�? �?카드???�각???�작 offset?�
        // ?��? 콘텐츠의 start ?�딩?�로�?조정?�니?? 기존??32.dp 균일 ?�딩?� ?��??�되
        // start??LEVEL_FIRST_CARD_START_PADDING 만큼 추�??�니??
        Column(
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    top = 32.dp,
                    end = 32.dp,
                    bottom = 32.dp
                )
                .fillMaxWidth()
                .testTag("main_level_card_content"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge: increase font size and add optional visual effect for yellow badge
            val badgeColor = currentLevel.color
            val isYellowBadge = badgeColor == Color(0xFFFBC02D)
            // Radial gradient blended slightly toward white at center (less washed) ??remove extra highlight overlay
            // Reduced lerp values and tightened mid stop to make the badge gradient more subtle
            // (was 0.20f / 0.08f -> then 0.35f / 0.18f; now reduced for subtler effect)
            val centerBlend = lerp(badgeColor, Color.White, 0.12f)
            val midBlend = lerp(badgeColor, Color.White, 0.05f)
            // Use a Surface to ensure shadow (elevation) is rendered for the badge itself.
            // shadowElevation is applied to the Surface's outline, so the inner radial gradient can remain
            // and the elevation will be visible regardless of badge color. Apply to all badges.
            val badgeSize = 110.dp
            // Add a small outer padding so the badge's shadow can render without being visually clipped
            Box(modifier = Modifier.padding(6.dp)) {
                Surface(
                    modifier = Modifier.size(badgeSize).testTag("main_level_badge"),
                    shape = CircleShape,
                    // Use surface color (opaque) so elevation shadow draws consistently across devices
                    color = MaterialTheme.colorScheme.surface,
                    // Visible elevation; increase if you want a stronger cast
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
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold),
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = Color.White),
                    modifier = Modifier.testTag("main_level_days_label")
                )
            }

            val nextLevel = getNextLevel(currentLevel)
            if (nextLevel != null) {
                Spacer(modifier = Modifier.height(24.dp))

                // 변경: 소수 일수 기반 진행률 계산
                val progress = if (nextLevel.start > currentLevel.start) {
                    val progressInLevel = elapsedDaysFloat - currentLevel.start
                    val totalNeeded = (nextLevel.start - currentLevel.start).toFloat()
                    if (totalNeeded > 0f) (progressInLevel / totalNeeded).coerceIn(0f, 1f) else 0f
                } else 0f

                // 추가: 남은 시간(일+시간) 문자열 생성
                val remainingDaysFloat = (nextLevel.start - elapsedDaysFloat).coerceAtLeast(0f)
                val remainingDaysInt = kotlin.math.floor(remainingDaysFloat.toDouble()).toInt()
                val remainingHoursInt = kotlin.math.floor(((remainingDaysFloat - remainingDaysInt) * 24f).toDouble()).toInt()
                val remainingText = when {
                    remainingDaysInt > 0 && remainingHoursInt > 0 -> "$remainingDaysInt${context.getString(R.string.level_day_unit)} $remainingHoursInt${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_days_remaining)}"
                    remainingDaysInt > 0 -> "$remainingDaysInt${context.getString(R.string.level_day_unit)} ${context.getString(R.string.level_days_remaining)}"
                    remainingHoursInt > 0 -> "$remainingHoursInt${context.getString(R.string.level_hour_unit)} ${context.getString(R.string.level_hours_remaining)}"
                    else -> context.getString(R.string.level_soon_levelup)
                }

                // Pass both the visual fill (0..1) and the numeric percent for display
                ProgressToNextLevel(
                    progressFill = progress,
                    displayPercent = (progress * 100.0),
                    remainingDays = (nextLevel.start - currentDays).coerceAtLeast(0),
                    remainingText = remainingText,
                    isSobrietyActive = startTime > 0
                )
            } else {
                // 마지막(최고) 레벨인 경우에도 동일한 공간을 유보하여 레이아웃 붕괴를 방지
                Spacer(modifier = Modifier.height(24.dp))

                // For the topmost level (start = 365), allow percent to grow beyond 100.
                // displayPercent = 100 + elapsedDaysFloat - currentLevel.start (so 366d -> 101)
                val extraPercent = (elapsedDaysFloat - currentLevel.start).coerceAtLeast(0f)
                val displayPercent = 100.0 + extraPercent.toDouble()
                // progressFill wraps so that 101% -> 1% fill, 200% -> full, etc. Special-case exact multiples of 100 to show full bar.
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
                    // 한국어 지원: 현재 레벨의 리소스 ID로부터 문자열을 가져옵니다.
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333)),
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
                Text(text = rangeText, style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF666666)))
            }

            when {
                isCurrent -> Icon(imageVector = Icons.Filled.Star, contentDescription = "현재 레벨", tint = level.color, modifier = Modifier.size(20.dp))
                isAchieved -> Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "달성 완료", tint = level.color, modifier = Modifier.size(20.dp))
                else -> Icon(imageVector = Icons.Filled.Lock, contentDescription = "미달성", tint = Color(0xFFBDBDBD), modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getNextLevel(currentLevel: LevelDefinitions.LevelInfo): LevelDefinitions.LevelInfo? {
    val currentIndex = LevelDefinitions.levels.indexOf(currentLevel)
    return if (currentIndex in 0 until LevelDefinitions.levels.size - 1) LevelDefinitions.levels[currentIndex + 1] else null
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
        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "indicator_blink"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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
                    .background(if (remainingDays > 0 && isSobrietyActive) Color(0xFFFBC528).copy(alpha = alpha) else Color(0xFF999999))
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
            val animatedProgress by animateFloatAsState(targetValue = progressFill, animationSpec = tween(durationMillis = 1000), label = "progress")
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(Brush.horizontalGradient(colors = listOf(Color(0xFFFBC528).copy(alpha = 0.7f), Color(0xFFFBC528))))
                    .testTag("main_level_progress_fill")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
