// [NEW] Tab01 Refactoring: RunScreen moved to tab_01/screens
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.Locale
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.ui.tab_01.components.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions
import kr.sweetapps.alcoholictimer.util.utils.FormatUtils
import kotlinx.coroutines.delay
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import kr.sweetapps.alcoholictimer.util.debug.DebugSettings
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DemoData
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager

@Composable
fun RunScreenComposable(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Local layout constants for RunScreen - keep local to avoid changing global constants
    val RUN_TOP_GROUP_TOP_PADDING = 15.dp            // vertical padding above top stat chips
    val RUN_TOP_GROUP_CHIP_SPACING = 10.dp // 12.dp
    // Unified horizontal padding for the whole Run screen. Use this single constant to keep card widths consistent.
    val RUN_HORIZONTAL_PADDING = 20.dp               // (was RUN_TOP_GROUP_HORIZONTAL_PADDING)
    // Separated local variables: top group and card spacing, card and progress card spacing
    val RUN_CARDS_VERTICAL_SPACING_TOP = 15.dp      // spacing between cards on screen

    // Progress card padding controls
    // Inner padding: Progress card internal margin (set to 0 by default for no inner padding)
    val RUN_CARD_CONTENT_HORIZONTAL_PADDING = 15.dp // Progress bar padding
    // Card internal vertical padding is a separate variable (default 12.dp previously)
    // If inner vertical padding is 0, the colored panel (Progress Surface) will stick to the card edges.
    val RUN_CARD_CONTENT_VERTICAL_PADDING = 10.dp //

    // Per-chip horizontal alignment (left / center / right)
    // All changed to center alignment (client request)
    val runStatAlignments = listOf(Alignment.CenterHorizontally, Alignment.CenterHorizontally, Alignment.CenterHorizontally)


    BackHandler(enabled = true) {
        // Inside NavHost, back action should not trigger background mode
    }

    val isPreview = LocalInspectionMode.current
    val isDemoMode = DebugSettings.isDemoModeEnabled(context)

    // SharedPreferences access is only executed at runtime (not during Preview)
    val sp = if (isPreview) null else context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val startTime = if (isPreview) (System.currentTimeMillis() - (2 * Constants.DAY_IN_MILLIS)) else sp!!.getLong(Constants.PREF_START_TIME, 0L)
    val targetDays = if (isDemoMode) DemoData.DEMO_TARGET_DAYS else if (isPreview) 30f else sp!!.getFloat(Constants.PREF_TARGET_DAYS, 30f)
    val timerCompleted = if (isPreview) false else sp!!.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

    if (!isPreview && !isDemoMode) {
        LaunchedEffect(startTime, timerCompleted) {
            if (timerCompleted || startTime == 0L) {
                onRequireBackToStart?.invoke()
            }
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (!isPreview) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
            }
        }
    }

    // [FIX] 타이머 테스트 모드를 고려한 동적 DAY_IN_MILLIS (now를 의존성에 추가하여 매 초마다 재계산)
    val dayInMillis = remember(now) {
        val value = Constants.getDayInMillis(context)
        val factor = if (isPreview || isDemoMode) 1 else {
            if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) 1
            else Constants.getTimeAcceleration(context)
        }
        android.util.Log.d("RunScreen", "dayInMillis 재계산: $value, 배속: ${factor}x")
        value
    }

    // [FIX] 경과 시간 계산 (배속은 dayInMillis에만 적용, elapsedMillis는 실제 시간)
    val elapsedMillis by remember(now, startTime, isDemoMode) {
        derivedStateOf {
            if (isDemoMode) {
                (DemoData.DEMO_ELAPSED_DAYS * dayInMillis).toLong()
            } else if (startTime > 0) {
                // 실제 경과 시간 (배속 적용 안 함)
                now - startTime
            } else {
                0L
            }
        }
    }

    val elapsedDaysFloat = remember(elapsedMillis, dayInMillis) { elapsedMillis / dayInMillis.toFloat() }

    // [NEW] 중앙 타이머 표시용 경과 시간 (배속 적용)
    val displayElapsedMillis = remember(elapsedMillis, dayInMillis) {
        // elapsedMillis는 실제 시간, dayInMillis는 배속이 적용된 "1일의 길이"
        // 따라서 elapsedDaysFloat에 실제 1일(86,400,000ms)을 곱하면 표시용 시간
        (elapsedDaysFloat * Constants.DAY_IN_MILLIS).toLong()
    }

    // [FIX] 레벨 계산: 1일 차부터 시작 (기존: 0일부터 시작)
    val levelDays = remember(elapsedMillis, dayInMillis) {
        val days = (elapsedMillis / dayInMillis).toInt()
        // 0일이면 1일 차로, 1일이면 2일 차로 변환 (사용자가 시작한 날 = 1일 차)
        if (days == 0) 1 else days + 1
    }
    val levelInfo = remember(levelDays) { LevelDefinitions.getLevelInfo(levelDays) }
    val levelNumber = if (isDemoMode) DemoData.DEMO_LEVEL else remember(levelDays) { LevelDefinitions.getLevelNumber(levelDays) + 1 } // +1 for display (1-indexed)
    // [FIX] Legend 레벨(11)은 "L"로 표시
    val levelDisplayText = if (levelNumber == 11) "Lv.L" else "Lv.$levelNumber"

    // Goal days format based on locale
    val goalDaysText = remember(targetDays) {
        context.getString(R.string.stat_goal_days_format, targetDays.toInt())
    }

    // [FIX] 중앙 타이머 표시: displayElapsedMillis 사용 (배속 반영)
    val elapsedHours = ((displayElapsedMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((displayElapsedMillis % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((displayElapsedMillis % (60 * 1000)) / 1000).toInt()
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)
    val progressTimeTextHM = String.format(Locale.getDefault(), "%02d:%02d", elapsedHours, elapsedMinutes)

    val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)
    val costVal = Constants.DrinkingSettings.getCostValue(selectedCost)
    val freqVal = Constants.DrinkingSettings.getFrequencyValue(selectedFrequency)
    val drinkHoursVal = Constants.DrinkingSettings.getDurationValue(selectedDuration)
    val weeks = elapsedDaysFloat / 7.0
    val savedMoney = remember(weeks, freqVal, costVal) { weeks * freqVal * costVal }
    val savedHours = remember(weeks, freqVal, drinkHoursVal) { weeks * freqVal * (drinkHoursVal + Constants.DrinkingSettings.HANGOVER_HOURS) }
    val lifeGainDays = remember(elapsedDaysFloat) { elapsedDaysFloat / 30.0 }
    // Display saved money as integer without currency symbol, formatted with locale grouping
    // [FIX] Use floor (truncate) instead of round for saved money display
    val savedMoneyRounded = remember(savedMoney) { savedMoney.toLong() }
    val savedMoneyDisplay = remember(savedMoneyRounded) { java.text.NumberFormat.getNumberInstance(Locale.getDefault()).format(savedMoneyRounded) }

    // Debug: compute life gain explicitly (days + hours) with 1 decimal and log values
    val formattedLifeGain = remember(lifeGainDays) {
        val safe = if (lifeGainDays.isNaN() || lifeGainDays.isInfinite()) 0.0 else lifeGainDays.coerceAtLeast(0.0)
        val dayPart = kotlin.math.floor(safe).toInt()
        val frac = safe - dayPart
        val hoursRaw = frac * 24.0
        val hoursRounded = (kotlin.math.round(hoursRaw * 10.0) / 10.0)
        val hourUnit = context.getString(R.string.unit_hour)
        val dayUnit = context.getString(R.string.unit_day)
        val out = if (dayPart == 0) {
            String.format(Locale.getDefault(), "%.1f%s", hoursRounded, hourUnit)
        } else {
            String.format(Locale.getDefault(), "%d%s %.1f%s", dayPart, dayUnit, hoursRounded, hourUnit)
        }
        android.util.Log.d("LifeGainDebug", "elapsedDaysFloat=$elapsedDaysFloat lifeGainDays=$lifeGainDays hoursRaw=$hoursRaw hoursRounded=$hoursRounded formattedLifeGain=$out")
        out
    }

    val totalTargetMillis = remember(targetDays, dayInMillis) { (targetDays * dayInMillis).toLong() }
    val progress = remember(elapsedMillis, totalTargetMillis) {
        if (totalTargetMillis > 0) (elapsedMillis.toFloat() / totalTargetMillis).coerceIn(0f, 1f) else 0f
    }

    val indicatorKey = remember(startTime) { Constants.keyCurrentIndicator(startTime) }
    var currentIndicator by remember { mutableIntStateOf(if (isPreview) 0 else sp!!.getInt(indicatorKey, 0)) }

    fun toggleIndicator() {
        val next = (currentIndicator + 1) % 5
        currentIndicator = next
        if (!isPreview) {
            sp!!.edit().putInt(indicatorKey, next).apply()
        }
    }

    var hasCompleted by remember { mutableStateOf(false) }
    if (!isPreview && !isDemoMode) {
        LaunchedEffect(progress) {
            if (!hasCompleted && progress >= 1f && startTime > 0) {
                try {
                    val endTs = System.currentTimeMillis()
                    val actualDaysInt = (elapsedMillis / dayInMillis).toInt()
                    saveCompletedRecord(
                        context = context,
                        startTime = startTime,
                        endTime = endTs,
                        targetDays = targetDays,
                        actualDays = actualDaysInt
                    )
                    sp!!.edit().remove(Constants.PREF_START_TIME).putBoolean(Constants.PREF_TIMER_COMPLETED, true).apply()

                    // [NEW] Save completed record info to SharedPreferences (used by FinishedScreen)
                    try {
                        sp!!.edit().apply {
                            putLong("completed_start_time", startTime)
                            putLong("completed_end_time", endTs)
                            putFloat("completed_target_days", targetDays)
                            putInt("completed_actual_days", actualDaysInt)
                            apply()
                        }
                        android.util.Log.d("RunScreen", "Saved completed record: startTime=$startTime, endTime=$endTs, targetDays=$targetDays, actualDays=$actualDaysInt")
                    } catch (t: Throwable) {
                        android.util.Log.e("RunScreen", "Failed to save completed record", t)
                    }

                    // [NEW] Save timer expiration state to TimerStateRepository
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(true)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false) // [NEW] Stop timer operation
                        android.util.Log.d("RunScreen", "Timer expiration state saved (active: false)")
                    } catch (t: Throwable) {
                        android.util.Log.e("RunScreen", "Failed to save timer expiration state", t)
                    }

                    hasCompleted = true

                    // Analytics: Log goal achievement event
                    try { AnalyticsManager.logTimerFinish(targetDays.toInt(), actualDaysInt, startTime, endTs) } catch (_: Throwable) {}

                    val goDetail: () -> Unit = {
                        val route = Screen.Detail.createRoute(
                            startTime = startTime,
                            endTime = System.currentTimeMillis(),
                            targetDays = targetDays,
                            actualDays = (elapsedMillis / dayInMillis).toInt(),
                            isCompleted = true
                        )
                        onCompletedNavigateToDetail?.invoke(route)
                    }

                    goDetail()
                    kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.preload(context.applicationContext)
                } catch (_: Exception) { }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StandardScreenWithBottomButton(
            topPadding = 0.dp,
            horizontalPadding = RUN_HORIZONTAL_PADDING,
            forceFillMaxWidth = true,
            // backgroundDecoration: no dark overlay to avoid dimming card contents
            backgroundDecoration = {
                Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(0.0f to Color.Transparent, 1.0f to Color.Transparent)))
            },
            screenBackground = Color(0xFFEEEDE9),
            // Ensure this screen uses the local card spacing
            cardVerticalSpacing = RUN_CARDS_VERTICAL_SPACING_TOP,
            topContent = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RUN_CARDS_VERTICAL_SPACING_TOP)) {
                    // Remove top group card and place 3 chips outside Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = RUN_TOP_GROUP_TOP_PADDING),
                        horizontalArrangement = Arrangement.spacedBy(RUN_TOP_GROUP_CHIP_SPACING)
                    ) {
                        // Left: pastel blue circle, blue icon
                        RunStatChip(
                            title = stringResource(id = R.string.stat_goal_days),
                            value = goalDaysText,
                            color = Color(0xFF2B6CB0),
                            modifier = Modifier.weight(1f),
                            iconRes = kr.sweetapps.alcoholictimer.R.drawable.calendar_blank,
                            iconBg = Color(0xFF0CCEFF), // changed to #0cceff
                            contentAlignment = runStatAlignments[0]
                        )

                        // Center: pastel mint circle, green icon
                        RunStatChip(
                            title = stringResource(id = R.string.stat_level),
                            value = levelDisplayText,
                            color = Color(0xFF06AD6A),
                            modifier = Modifier.weight(1f),
                            iconRes = kr.sweetapps.alcoholictimer.R.drawable.trophy,
                            iconBg = Color(0xFFFFD605), // changed to #ffd605
                            contentAlignment = runStatAlignments[1]
                        )

                        // Right: pastel lilac circle, indigo icon
                        RunStatChip(
                            title = stringResource(id = R.string.stat_saved_money_short),
                            value = savedMoneyDisplay,
                            color = Color(0xFF5873D6),
                            modifier = Modifier.weight(1f),
                            iconRes = kr.sweetapps.alcoholictimer.R.drawable.chart_line_up,
                            iconBg = Color(0xFFFF5C91), // changed to #ff5c91
                            contentAlignment = runStatAlignments[2]
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().height(180.dp).clickable { toggleIndicator() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        // background image fills the card
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.bg9),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Place composable content with the same padding as existing (no padding removal)
                            Box(modifier = Modifier.fillMaxSize().padding(0.dp), contentAlignment = Alignment.Center) {
                                // Reduce center card element spacing: adjust so level/percentage are closer together
                                // Wrap the minimal Column containing level/percentage content and place Box in center
                                // Ensure minimum height to fix label float issue
                                val label: String = when (currentIndicator) {
                                    0 -> stringResource(id = R.string.indicator_title_days)
                                    1 -> stringResource(id = R.string.indicator_title_time)
                                    2 -> stringResource(id = R.string.indicator_title_saved_money)
                                    3 -> stringResource(id = R.string.indicator_title_saved_hours)
                                    else -> stringResource(id = R.string.indicator_title_life_gain)
                                }
                                val valueText: String = when (currentIndicator) {
                                    0 -> String.format(Locale.getDefault(), "%.1f", elapsedDaysFloat)
                                    1 -> progressTimeTextHM
                                    2 -> FormatUtils.formatMoney(context, savedMoney).replace(" ", "")
                                    3 -> FormatUtils.formatHoursValue(savedHours)
                                    else -> formattedLifeGain
                                }

                                // Layout: fill card height and center children so numeric value is visually centered
                                Column(modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = 0.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    // Top label: remove padding, use includeFontPadding=false to tighten and align text center
                                    Box(modifier = Modifier.fillMaxWidth().padding(top = 0.dp), contentAlignment = Alignment.Center) {
                                        val base = MaterialTheme.typography.titleMedium
                                        Text(
                                            text = label,
                                            style = base.copy(
                                                color = Color.White,
                                                lineHeight = base.fontSize * 1.05f,
                                                fontWeight = FontWeight.Bold,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1f), blurRadius = 2f)
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Middle: center main value (no weight so it stays close to label/hint)
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        val baseStyle = MaterialTheme.typography.headlineMedium
                                        // Auto-resize: calculate optimal font size based on available width
                                        val textMeasurer = rememberTextMeasurer()
                                        val density = LocalDensity.current
                                        val baseFontSp = baseStyle.fontSize
                                        val maxMultiplier = 2.5f
                                        val minMultiplier = 0.6f

                                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                            val maxWpx = with(density) { maxWidth.toPx() }
                                            // Initial font size (px)
                                            val initialSizeSp = (baseFontSp.value * maxMultiplier)
                                            // Measurement loop: decrease from initial size to minimum size until it fits available space
                                            val chosenSizeSp = remember(valueText, maxWpx) {
                                                var s = initialSizeSp
                                                val minSize = baseFontSp.value * minMultiplier
                                                while (s >= minSize) {
                                                    val styleTry = baseStyle.copy(fontSize = s.sp, platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                    val result = try { textMeasurer.measure(AnnotatedString(valueText), style = styleTry) } catch (_: Throwable) { null }
                                                    val textW = result?.size?.width ?: 0
                                                    if (textW <= maxWpx * 0.92f) break
                                                    s -= 2f // Decrease step (sp)
                                                }
                                                s.coerceAtLeast(minSize)
                                            }

                                            val bigStyle = baseStyle.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                fontSize = chosenSizeSp.sp,
                                                lineHeight = chosenSizeSp.sp * 1.05f,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                fontFeatureSettings = "tnum",
                                                shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 4f)
                                            )

                                            val unitStyle = baseStyle.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = (baseFontSp.value * 1.25f).sp,
                                                lineHeight = baseFontSp * 1.15f,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 2f)
                                            )

                                            // Render numeric/unit rendering using the same logic as before
                                            val isMoney = currentIndicator == 2
                                            val isLifeGain = currentIndicator == 4
                                            if (isMoney) {
                                                // Previous: code that branched by symbol using regex -> directly process selected currency
                                                val selectedCurrency = CurrencyManager.getSelectedCurrency(context)
                                                val symbol = selectedCurrency.symbol
                                                if (selectedCurrency.code == "KRW") {
                                                    // KRW: show number and append '원' unit
                                                    val numeric = valueText.replace("₩", "").replace("원", "").trim()
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = "원", style = unitStyle)
                                                    }
                                                } else {
                                                    // Other currencies: place currency symbol on left and show only numeric part
                                                    // Remove targets: currency symbol, '원', '円' and other Asian units
                                                    val numeric = valueText.replace(symbol, "")
                                                        .replace("₩", "")
                                                        .replace("원", "")
                                                        .trim()
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                        Text(text = symbol, style = unitStyle)
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                    }
                                                }
                                            } else if (isLifeGain) {
                                                val twoPart = Regex("""(\d+)\s*(?:일|日|day\(s\))\s*([0-9]+(?:\.[0-9]+)?)\s*(?:시간|時間|hr\(s\))""")
                                                val onePart = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:시간|時間|hr\(s\))""")
                                                val m1 = twoPart.find(valueText)
                                                val m2 = if (m1 == null) onePart.find(valueText) else null
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                    if (m1 != null) {
                                                        val dStr = m1.groupValues[1]
                                                        val hStr = m1.groupValues[2]
                                                        Text(text = dStr, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = stringResource(R.string.unit_day), style = unitStyle)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(text = hStr, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = stringResource(R.string.unit_hour), style = unitStyle)
                                                    } else if (m2 != null) {
                                                        val hStr = m2.groupValues[1]
                                                        Text(text = hStr, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = stringResource(R.string.unit_hour), style = unitStyle)
                                                    } else {
                                                        Text(text = valueText, style = bigStyle, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = valueText,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    style = bigStyle,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Clip
                                                )
                                            }
                                         }
                                     }

                                    // Bottom hint (adjust minimal padding to bring closer to number)
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                                        val base = MaterialTheme.typography.labelMedium
                                        Text(
                                            text = stringResource(id = R.string.tap_to_switch_indicator),
                                            style = base.copy(
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 16.sp,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 2f)
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Replace transparent card + surface with a single elevated white Card matching top cards
                    // [FIX] 프로그레스 카드와 응원 문구를 하나의 Column으로 묶어서 spacing 제거
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp) // 간격 완전 제거
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RUN_CARD_CONTENT_HORIZONTAL_PADDING, vertical = RUN_CARD_CONTENT_VERTICAL_PADDING),
                                horizontalAlignment = Alignment.Start
                            ) {
                                // Progress content
                                ModernProgressIndicatorSimple(progress = progress, targetDays = targetDays)
                            }
                        }

                        // [NEW] 응원 문구 (프로그레스 카드 바깥쪽 하단) - 간격 완전 제거
                        val motivationalQuote = rememberSaveable {
                            kr.sweetapps.alcoholictimer.data.model.MotivationalQuotes.getRandomQuote()
                        }

                        Text(
                            text = "\" $motivationalQuote \"",
                            style = kr.sweetapps.alcoholictimer.ui.tab_01.components.QuoteTextStyle.default, // [SHARED] 공통 스타일 사용
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp, bottom = 20.dp)
                                .padding(horizontal = 20.dp),
                            textAlign = TextAlign.Center,
                            minLines = 2
                        )
                    }
                }
            },
            bottomButton = {
                ModernStopButtonSimple(onStop = {
                    onRequestQuit?.invoke()
                })
            },
            // bottomAd = { AdmobBanner() } // moved to MainActivity BaseScaffold during Phase-1
        )
    }
}

@Composable
fun ModernProgressIndicatorSimple(progress: Float, targetDays: Float = 30f) {
    val primary = colorResource(id = R.color.color_progress_primary)
    val track = colorResource(id = R.color.color_progress_track)

    Column(modifier = Modifier.fillMaxWidth()) {
        // top row: icon+title (left), percent (right)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.TrendingUp, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = stringResource(id = R.string.run_progress_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF111111))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(text = "${(progress * 100).toInt().coerceIn(0,100)}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = primary))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // progress track
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(track)) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress)
                .clip(RoundedCornerShape(12.dp))
                .background(primary))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // bottom labels removed per request (start/target texts deleted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernStopButtonSimple(onStop: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onStop,
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_stop_button)),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(id = R.string.cd_stop),
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
@Suppress("UNUSED_VALUE")
private fun AutoResizeSingleLineText(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    minFontSizeSp: Float = 10f,
    step: Float = 0.95f,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    var style by remember(text) { mutableStateOf(baseStyle) }
    var tried by remember(text) { mutableStateOf(0) }
    Text(
        text = text,
        style = style,
        color = color ?: style.color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && tried < 20) {
                val current = style.fontSize.value
                val next = (current * step).coerceAtLeast(minFontSizeSp)
                if (next < current - 0.1f) {
                    style = style.copy(fontSize = next.sp, lineHeight = (next.sp * 1.1f))
                    tried++
                }
            }
        }
    )
}

@Composable
fun RunStatChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    iconBg: Color? = null,
    contentAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    // Card with white background and elevation matching the progress card
    // Increase card height to make top level card more visible
    Card(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = contentAlignment, verticalArrangement = Arrangement.Center) {
            // Top circular icon
            if (icon != null || iconRes != null) {
                // Apply common accent gradient with lighter bottom for chips with iconBg specified
                val applyAccent = iconBg != null
                if (applyAccent) {
                    val topColor = iconBg!!
                    val bottomColor = lerp(topColor, Color.White, 0.18f)
                    Card(
                        modifier = Modifier.size(48.dp), // increased from 44.dp to 48.dp
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(topColor, bottomColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            icon?.let {
                                Icon(imageVector = it, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) // increased from 20.dp to 24.dp
                            } ?: run {
                                iconRes?.let { res ->
                                    Image(
                                        painter = painterResource(id = res),
                                        contentDescription = null,
                                        contentScale = ContentScale.Inside,
                                        colorFilter = ColorFilter.tint(Color.White),
                                        modifier = Modifier.size(24.dp) // increased from 20.dp to 24.dp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier
                        .size(48.dp) // increased from 44.dp to 48.dp
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        if (icon != null) {
                            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) // increased from 20.dp to 24.dp
                        } else {
                            iconRes?.let { res ->
                                Image(
                                    painter = painterResource(id = res),
                                    contentDescription = null,
                                    contentScale = ContentScale.Inside,
                                    modifier = Modifier.size(24.dp) // increased from 20.dp to 24.dp
                                )
                            }
                        }
                    }
                 }
                 Spacer(modifier = Modifier.height(8.dp))
            }

            // Large value
            // Auto-resize value so large numbers fit the chip without unit symbol or decimals
            AutoResizeSingleLineText(
                text = value,
                baseStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111)),
                modifier = Modifier.fillMaxWidth(),
                minFontSizeSp = 12f,
                step = 0.95f,
                color = Color(0xFF111111),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Label
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(color = colorResource(id = R.color.color_stat_title_gray)),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun saveCompletedRecord(context: Context, startTime: Long, endTime: Long, targetDays: Float, actualDays: Int) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val recordId = System.currentTimeMillis().toString()
        val isCompleted = actualDays >= targetDays
        val status = if (isCompleted) "completed" else "in_progress"
        val record = org.json.JSONObject().apply {
            put("id", recordId); put("startTime", startTime); put("endTime", endTime); put("targetDays", targetDays.toInt()); put("actualDays", actualDays); put("isCompleted", isCompleted); put("status", status); put("createdAt", System.currentTimeMillis())
        }
        val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val list = try { org.json.JSONArray(recordsJson) } catch (_: Exception) { org.json.JSONArray() }
        list.put(record)
        sharedPref.edit().putString(Constants.PREF_SOBRIETY_RECORDS, list.toString()).apply()
    } catch (_: Exception) { }
}

// Preview: show the real Run screen composable as-is
@Preview(name = "RunScreen - Live Composable", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun RunScreenLivePreview() {
    // Call the actual RunScreenComposable so preview matches runtime UI
    RunScreenComposable(onRequestQuit = {}, onCompletedNavigateToDetail = {}, onRequireBackToStart = {})
}
