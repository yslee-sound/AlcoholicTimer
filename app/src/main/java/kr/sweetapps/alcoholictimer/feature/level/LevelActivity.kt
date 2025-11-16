package kr.sweetapps.alcoholictimer.feature.level

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.core.data.RecordsDataLoader
import kotlinx.coroutines.delay
import java.util.Locale
import android.util.Log
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import androidx.compose.foundation.BorderStroke
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.components.MainLevelCardFrame
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding

class LevelActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 뒤로가기 버튼: 메인 홈(Start/Run)으로 이동
            BackHandler(enabled = true) {
                navigateToMainHome()
            }

            // AdmobBanner centralized in MainActivity BaseScaffold during Phase-1 migration
            // We manage bottom area (safe insets) inside LevelScreen to avoid duplicated bottom spacer
            BaseScreen(manageBottomAreaExternally = true, content = { LevelScreen() })
        }
    }

    override fun getScreenTitleResId(): Int = R.string.level_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.level_title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen() {
    val context = LocalContext.current

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)

    val currentElapsedTime = if (startTime > 0) currentTime - startTime else 0L

    val pastRecords = RecordsDataLoader.loadSobrietyRecords(context)
    val totalPastDuration = pastRecords.sumOf { record -> (record.endTime - record.startTime) }

    val totalElapsedTime = totalPastDuration + currentElapsedTime
    // 추가: 총 경과 일수(소수점 포함) 계산
    val totalElapsedDaysFloat = totalElapsedTime / Constants.DAY_IN_MILLIS.toFloat()

    val levelDays = Constants.calculateLevelDays(totalElapsedTime)
    val currentLevel = LevelDefinitions.getLevelInfo(levelDays)

    // BaseScreen에서 제공하는 하단 안전 패딩(LocalSafeContentPadding)을 이 화면에서는 무시하고
    // LEVEL_SCREEN_BOTTOM_PADDING 상수로만 하단 여백을 제어하도록 CompositionLocal을 덮어씁니다.
    CompositionLocalProvider(LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)) {
        // We'll rely on navigationBarsPadding() on the scroll content for consistent inset handling.

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Use surface for the screen background so the flat LevelListCard blends to the bottom
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.spacedBy(UiConstants.CARD_VERTICAL_SPACING)
        ) {
            // Scrollable area (does NOT include safeBottom) — explicitly paint it with surface so it blends to bottom
            // Use only the level-screen bottom padding so spacing is not stacked with host reserves.
            // This makes the content extend behind the BottomNavBar; the nav will overlay on top.
            val appliedBottom = UiConstants.LEVEL_SCREEN_BOTTOM_PADDING
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                // Apply adjusted bottom so visual gap above bottom nav equals LEVEL_SCREEN_BOTTOM_PADDING
                .padding(bottom = appliedBottom)
            ) {
                // Debug: log computed appliedBottom for diagnosis
                Log.d("LevelScreenDebug", "LEVEL_SCREEN_BOTTOM_PADDING=${UiConstants.LEVEL_SCREEN_BOTTOM_PADDING}, appliedBottom=$appliedBottom")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 수평 여백을 LEVEL_SCREEN_HORIZONTAL_PADDING으로 통일하여
                        // 상단 메인카드와 하단 리스트카드의 좌우 여백을 한 곳에서 관리합니다.
                        .padding(top = UiConstants.LEVEL_FIRST_CARD_TOP_PADDING, start = UiConstants.LEVEL_SCREEN_HORIZONTAL_PADDING, end = UiConstants.LEVEL_SCREEN_HORIZONTAL_PADDING),
                    verticalArrangement = Arrangement.spacedBy(UiConstants.CARD_VERTICAL_SPACING)
                ) {
                    // 변경: float 경과 일수 전달
                    // Only the top main card should have the horizontal screen padding; LevelListCard should span edge-to-edge.
                    CurrentLevelCard(
                        currentLevel = currentLevel,
                        currentDays = levelDays,
                        elapsedDaysFloat = totalElapsedDaysFloat,
                        startTime = startTime,
                        // 카드 외형 폭은 부모의 LEVEL_SCREEN_HORIZONTAL_PADDING으로만 관리되도록
                        modifier = Modifier.fillMaxWidth()
                    )
                    LevelListCard(currentLevel = currentLevel, currentDays = levelDays)
                }
            }

            // No separate bottom band — nav inset is included as bottom padding inside the scroll area.
        }
    } // end CompositionLocalProvider override
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
        // 카드 자체의 폭은 부모에서 제어되므로, 첫 카드의 시각적 시작 offset은
        // 내부 콘텐츠의 start 패딩으로만 조정합니다. 기존의 32.dp 균일 패딩은 유지하되
        // start는 LEVEL_FIRST_CARD_START_PADDING 만큼 추가합니다.
        Column(
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    top = 32.dp,
                    end = 32.dp,
                    bottom = 32.dp
                )
                .testTag("main_level_card_content"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge: increase font size and add optional visual effect for yellow badge
            val badgeColor = currentLevel.color
            val isYellowBadge = badgeColor == Color(0xFFFBC02D)
            // Radial gradient blended slightly toward white at center (less washed) — remove extra highlight overlay
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

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.height(8.dp))

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

                // 변경: 정수 일수 대신 실수 일수 기반 진행률 계산
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

                ProgressToNextLevel(
                    progress = progress,
                    remainingDays = (nextLevel.start - currentDays).coerceAtLeast(0),
                    remainingText = remainingText,
                    isSobrietyActive = startTime > 0
                )
            }
        }
    }
}

@Composable
private fun ProgressToNextLevel(
    progress: Float,
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
                    // 인디케이터 크기를 6.dp에서 10.dp로 증가시켜 가시성 향상
                    .size(10.dp) // 인디케이터 점
                    .clip(CircleShape)
                    // 변경: 요청대로 인디케이터 색상을 #FBC528로 적용
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
            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 1000), label = "progress")
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    // 변경: 진행 바 색상도 요청한 #FBC528 색상 계열로 고정
                    .background(Brush.horizontalGradient(colors = listOf(Color(0xFFFBC528).copy(alpha = 0.7f), Color(0xFFFBC528))))
                    .testTag("main_level_progress_fill")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", progress * 100),
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

@Composable
private fun LevelListCard(currentLevel: LevelDefinitions.LevelInfo, currentDays: Int) {
    val context = LocalContext.current
    // Flat container: no rounded card, no elevation, no border.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // keep the visual grouping by using the surface color (flat, no corners)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = context.getString(R.string.level_all_levels),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333)),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(UiConstants.CARD_VERTICAL_SPACING)) {
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
@Suppress("UNUSED_PARAMETER")
private fun LevelItem(
    level: LevelDefinitions.LevelInfo,
    isCurrent: Boolean,
    isAchieved: Boolean,
    isNext: Boolean
) {
    val context = LocalContext.current
    // 내부 리스트 아이템에서는 그림자를 사용하지 않는다(중첩 음영에 의한 두꺼운 회색띠 방지)
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

            if (isCurrent) {
                Icon(imageVector = Icons.Filled.Star, contentDescription = "현재 레벨", tint = level.color, modifier = Modifier.size(20.dp))
            } else if (isAchieved) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "달성 완료", tint = level.color, modifier = Modifier.size(20.dp))
            } else {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = "미달성", tint = Color(0xFFBDBDBD), modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getNextLevel(currentLevel: LevelDefinitions.LevelInfo): LevelDefinitions.LevelInfo? {
    val currentIndex = LevelDefinitions.levels.indexOf(currentLevel)
    return if (currentIndex < LevelDefinitions.levels.size - 1) LevelDefinitions.levels[currentIndex + 1] else null
}

@Preview(showBackground = true, name = "LevelScreen - 기본", widthDp = 360, heightDp = 800)
@Composable
fun LevelScreenPreview() {
    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) { LevelScreen() }
    }
}
