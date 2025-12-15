// [NEW] Tab01 Refactoring: RunScreen moved to tab_01/screens
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.NonCancellable.isCompleted
import kr.sweetapps.alcoholictimer.util.debug.DebugSettings
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DemoData
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState

@Composable
fun RunScreenComposable(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null,
    viewModel: kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity
    )
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


    // [FIX] 뒤로 가기 시 앱 최소화 (타이머 유지)
    // 타이머 앱 특성상 사용자가 뒤로 가기를 누르는 의도는 앱을 끄는 것이 아니라
    // 타이머를 켜둔 채 홈 화면으로 나가려는 것이므로 백그라운드로 이동
    BackHandler(enabled = true) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val isPreview = LocalInspectionMode.current
    val isDemoMode = DebugSettings.isDemoModeEnabled(context)

    // [FIX] Get timer data from ViewModel instead of SharedPreferences
    val startTime by viewModel.startTime.collectAsState()
    val targetDays by viewModel.targetDays.collectAsState()
    val timerCompleted by viewModel.timerCompleted.collectAsState()
    val elapsedMillisFromVM by viewModel.elapsedMillis.collectAsState()

    // SharedPreferences only for indicator state (not timer critical)
    val sp = if (isPreview) null else context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)

    if (!isPreview && !isDemoMode) {
        LaunchedEffect(startTime, timerCompleted) {
            // [FIX] startTime이 0이어도, 만약 '완료된 상태(timerCompleted)'라면
            // MainActivity가 FinishedScreen으로 보낼 것이므로, 여기서 StartScreen으로 쫓아내면 안 됨!
            // 오직 '완료되지 않았는데(timerCompleted == false) startTime이 0'인 경우만 데이터 오류로 판단
            if (startTime == 0L && !timerCompleted) {
                onRequireBackToStart?.invoke()
            }
        }
    }

    // [FIX] dayInMillis는 고정 상수 사용
    val dayInMillis = Constants.DAY_IN_MILLIS

    // [FIX] Use elapsed time from ViewModel (survives tab switches)
    val elapsedMillis = if (isDemoMode) {
        (DemoData.DEMO_ELAPSED_DAYS * dayInMillis).toLong()
    } else if (isPreview) {
        2 * dayInMillis // Preview: 2 days
    } else {
        elapsedMillisFromVM
    }

    // [FIX] displayElapsedMillis는 elapsedMillis와 동일 (이미 가속됨)
    val displayElapsedMillis = elapsedMillis

    // [FIX] 통계 계산용 경과 일수 (가상 시간 기준)
    val elapsedDaysFloat = remember(displayElapsedMillis) {
        displayElapsedMillis / Constants.DAY_IN_MILLIS.toFloat()
    }

    // [FIX] 레벨 계산: 가상 시간 기준, 1일 차부터 시작
    val levelDays = remember(displayElapsedMillis) {
        val days = (displayElapsedMillis / Constants.DAY_IN_MILLIS).toInt()
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

    // [NEW] 실시간 설정 변경 감지 - 탭4에서 설정을 바꾸면 즉시 반영됨
    val userSettings by rememberUserSettingsState(context)
    val costVal = Constants.DrinkingSettings.getCostValue(userSettings.cost)
    val freqVal = Constants.DrinkingSettings.getFrequencyValue(userSettings.frequency)
    val drinkHoursVal = Constants.DrinkingSettings.getDurationValue(userSettings.duration)
    val currencySymbol = userSettings.currencySymbol // 통화 기호도 실시간 반영

    val weeks = elapsedDaysFloat / 7.0
    val savedMoney = remember(weeks, freqVal, costVal) { weeks * freqVal * costVal }
    val savedHours = remember(weeks, freqVal, drinkHoursVal) { weeks * freqVal * drinkHoursVal }
    val lifeGainDays = remember(elapsedDaysFloat) { elapsedDaysFloat / 30.0 }

    // [FIX] 환율 변환 포함 포맷팅 (CurrencyManager 사용)
    val savedMoneyDisplay = remember(savedMoney, userSettings.currencySymbol) {
        if (isPreview) "2,097"
        else CurrencyManager.formatMoneyNoDecimals(savedMoney, context)
    }

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

    val totalTargetMillis = remember(targetDays) { (targetDays * Constants.DAY_IN_MILLIS).toLong() }
    val progress = remember(displayElapsedMillis, totalTargetMillis) {
        if (totalTargetMillis > 0) (displayElapsedMillis.toFloat() / totalTargetMillis).coerceIn(0f, 1f) else 0f
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

    // [REMOVED] 타이머 완료 감지 로직을 UI에서 제거
    // 이제 TimerTimeManager와 Tab01ViewModel에서 자동으로 처리됨
    // 사용자가 어느 화면에 있든 타이머 완료 시 자동으로 DetailScreen으로 이동

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
                        // [STRATEGY 1] 세련된 모노톤 적용 (Slate Blue Gray)
                        // 너무 검정색(Black)은 딱딱해 보이므로, 약간 푸른끼가 도는 짙은 회색을 사용합니다.
                        val commonIconColor = Color(0xFF455A64)

                        // 1. Goal
                        RunStatChip(
                            title = stringResource(id = R.string.stat_goal_days),
                            value = goalDaysText,
                            color = commonIconColor, // ★ 공통 컬러 적용
                            modifier = Modifier.weight(1f),
                            iconRes = kr.sweetapps.alcoholictimer.R.drawable.calendar_blank,
                            contentAlignment = runStatAlignments[0]
                        )

                        // 2. Level
                        RunStatChip(
                            title = stringResource(id = R.string.stat_level),
                            value = levelDisplayText,
                            color = commonIconColor, // ★ 공통 컬러 적용
                            modifier = Modifier.weight(1f),
                            iconRes = kr.sweetapps.alcoholictimer.R.drawable.trophy,
                            contentAlignment = runStatAlignments[1]
                        )

                        // 3. Money Saved
                        RunStatChip(
                            title = stringResource(id = R.string.stat_saved_money_short),
                            value = savedMoneyDisplay,
                            color = commonIconColor, // ★ 공통 컬러 적용
                            modifier = Modifier.weight(1f),
                            iconRes = kr.sweetapps.alcoholictimer.R.drawable.chart_line_up,
                            contentAlignment = runStatAlignments[2]
                        )
                    }

                    // [FIXED_SIZE] 중간 큰 카드 높이를 폰트 스케일 영향 받지 않도록 고정
                    val density = LocalDensity.current
                    val bigCardHeightPx = with(density) { 180.dp.toPx() }
                    val bigCardHeight = with(density) { (bigCardHeightPx / density.density).dp }

                    Card(
                        modifier = Modifier.fillMaxWidth().requiredHeight(bigCardHeight).clickable { toggleIndicator() },
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
                                    2 -> CurrencyManager.formatMoneyNoDecimals(savedMoney, context) // [FIX] 소수점 제거 (환율 변환 포함)
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

                        // [NEW] 공통 컴포넌트 사용 (StartScreen과 동일한 디자인 & 2줄 제한 로직 적용)
                        kr.sweetapps.alcoholictimer.ui.tab_01.components.QuoteDisplay(
                            modifier = Modifier.padding(bottom = 12.dp) // 하단 여백만 살짝 조정, 12.dp
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

@Composable
private fun ModernStopButtonSimple(onStop: () -> Unit, modifier: Modifier = Modifier) {
    // [FAB_UNIFIED] 시스템 폰트 스케일의 영향을 받지 않는 고정 크기 FloatingActionButton
    val density = LocalDensity.current
    val buttonSizePx = with(density) { 77.dp.toPx() }
    val buttonSize = with(density) { (buttonSizePx / density.density).dp }
    val iconSizePx = with(density) { 39.dp.toPx() }
    val iconSize = with(density) { (iconSizePx / density.density).dp }

    FloatingActionButton(
        onClick = onStop,
        modifier = modifier.requiredSize(buttonSize),
        containerColor = colorResource(id = R.color.color_stop_button),
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(id = R.string.cd_stop),
            tint = Color.White,
            modifier = Modifier.requiredSize(iconSize)
        )
    }
}

// RunScreen.kt 맨 아래 함수 교체

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
    val density = LocalDensity.current
    // 카드 높이는 그대로 유지
    val cardHeightPx = with(density) { 110.dp.toPx() }
    val cardHeight = with(density) { (cardHeightPx / density.density).dp }
    val iconSizePx = with(density) { 32.dp.toPx() }
    val iconSize = with(density) { (iconSizePx / density.density).dp }

    Card(
        modifier = modifier.requiredHeight(cardHeight),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            // [FIX] 잘림 방지: 상하 패딩을 10dp -> 8dp로 줄여 수직 공간 확보
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = contentAlignment,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 아이콘 영역
            Box(
                modifier = Modifier.height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.requiredSize(iconSize)
                    )
                } else {
                    iconRes?.let { res ->
                        Image(
                            painter = painterResource(id = res),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(color),
                            modifier = Modifier.requiredSize(iconSize)
                        )
                    }
                }
            }

            // 2. 숫자 영역 (폰트 크기 및 두께 추가 감소)
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val maxPixels = with(density) { maxWidth.toPx() }

                // [FIX] 두께 감소: Bold -> SemiBold
                val baseStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111)
                )

                val calculatedSize = remember(value, maxPixels) {
                    // [FIX] 최대 크기 감소: 18f -> 16f
                    var currentSize = 16f
                    val minSize = 10f

                    while (currentSize > minSize) {
                        val result = textMeasurer.measure(
                            text = AnnotatedString(value),
                            style = baseStyle.copy(fontSize = currentSize.sp)
                        )
                        // 가로폭 85% 제한 유지
                        if (result.size.width <= maxPixels * 0.85f) break
                        currentSize -= 1f
                    }
                    currentSize.coerceAtLeast(minSize).sp
                }

                Text(
                    text = value,
                    style = baseStyle.copy(fontSize = calculatedSize),
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. 라벨 영역
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(id = R.color.color_stat_title_gray),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
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
