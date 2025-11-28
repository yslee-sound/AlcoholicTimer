package kr.sweetapps.alcoholictimer.feature.run

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AccessTime
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
import java.util.Locale
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.feature.level.LevelDefinitions
import kr.sweetapps.alcoholictimer.core.util.FormatUtils
import kotlinx.coroutines.delay
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.navigation.Screen
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import kr.sweetapps.alcoholictimer.core.util.DebugSettings
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DemoData
import kr.sweetapps.alcoholictimer.analytics.AnalyticsLogger

@Composable
fun RunScreenComposable(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Local layout constants for RunScreen — keep local to avoid changing global constants
    val RUN_TOP_GROUP_TOP_PADDING = 15.dp            // vertical padding above top stat chips
    // Unified horizontal padding for the whole Run screen. Use this single constant to keep card widths consistent.
    val RUN_HORIZONTAL_PADDING = 15.dp               // (was RUN_TOP_GROUP_HORIZONTAL_PADDING)
    // 분리된 로컬 상수: 상단 그룹과 첫 카드 사이, 카드와 프로그레스 카드 사이
    val RUN_CARDS_VERTICAL_SPACING_TOP = 15.dp      // 화면 내 카드들 사이 간격

    // Progress card padding controls
    // 외부 여백: 프로그레스 카드 주변의 외부 마진(기본 0으로 설정하여 외부 여백 없음)
    val RUN_CARD_CONTENT_HORIZONTAL_PADDING = 15.dp // 프로그레스 바의 패딩
    // 카드 내부의 수직 패딩은 별도 상수(기존 12.dp 유지)
    // 내부 수직 패딩을 0으로 하면 흰색 패널(Progress Surface) 상단과 위 카드가 더 붙습니다.
    val RUN_CARD_CONTENT_VERTICAL_PADDING = 12.dp // 프로그레스 내부 패딩 (기본 0) 12

    // Per-chip horizontal alignment (left / center / right)
    // 모두 중앙 정렬로 변경 (요청사항)
    val runStatAlignments = listOf(Alignment.CenterHorizontally, Alignment.CenterHorizontally, Alignment.CenterHorizontally)
    val RUN_TOP_GROUP_CHIP_SPACING = 12.dp

    BackHandler(enabled = true) {
        // NavHost 내에서는 뒤로가기를 소비해 백그라운드 이동 대신 유지
    }

    val isPreview = LocalInspectionMode.current
    val isDemoMode = DebugSettings.isDemoModeEnabled(context)

    // SharedPreferences 접근은 런타임에서만 수행 (Preview 안전성)
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

    val elapsedMillis by remember(now, startTime, isDemoMode) {
        derivedStateOf {
            if (isDemoMode) {
                (DemoData.DEMO_ELAPSED_DAYS * Constants.DAY_IN_MILLIS).toLong()
            } else if (startTime > 0) {
                now - startTime
            } else {
                0L
            }
        }
    }

    val elapsedDaysFloat = remember(elapsedMillis) { elapsedMillis / Constants.DAY_IN_MILLIS.toFloat() }

    val levelDays = remember(elapsedMillis) { Constants.calculateLevelDays(elapsedMillis) }
    val levelInfo = remember(levelDays) { LevelDefinitions.getLevelInfo(levelDays) }
    val levelNumber = if (isDemoMode) DemoData.DEMO_LEVEL else remember(levelDays) { LevelDefinitions.getLevelNumber(levelDays) + 1 } // +1 for display (1-indexed)
    val levelDisplayText = "Lv.$levelNumber"

    // Goal days format based on locale
    val goalDaysText = remember(targetDays) {
        context.getString(R.string.stat_goal_days_format, targetDays.toInt())
    }

    val elapsedHours = ((elapsedMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedMillis % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedMillis % (60 * 1000)) / 1000).toInt()
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
    val savedMoneyRounded = remember(savedMoney) { kotlin.math.round(savedMoney).toLong() }
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

    val totalTargetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
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
                    val actualDaysInt = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt()
                    saveCompletedRecord(
                        context = context,
                        startTime = startTime,
                        endTime = endTs,
                        targetDays = targetDays,
                        actualDays = actualDaysInt
                    )
                    sp!!.edit().remove(Constants.PREF_START_TIME).putBoolean(Constants.PREF_TIMER_COMPLETED, true).apply()
                    hasCompleted = true

                    // Analytics: 목표 달성 이벤트 기록
                    try { AnalyticsLogger.timerComplete(targetDays.toInt(), actualDaysInt, startTime, endTs) } catch (_: Throwable) {}

                    val goDetail: () -> Unit = {
                        val route = Screen.Detail.createRoute(
                            startTime = startTime,
                            endTime = System.currentTimeMillis(),
                            targetDays = targetDays,
                            actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt(),
                            isCompleted = true
                        )
                        onCompletedNavigateToDetail?.invoke(route)
                    }

                    goDetail()
                    kr.sweetapps.alcoholictimer.ads.InterstitialAdManager.preload(context.applicationContext)
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
                    // 상단 그룹 카드 제거 — 3개 칩을 Card 밖으로 배치
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
                            // 기존 내용은 이미지 위에 동일한 패딩으로 배치 (내부 패딩 제거)
                            Box(modifier = Modifier.fillMaxSize().padding(0.dp), contentAlignment = Alignment.Center) {
                                // 중앙 카드 요소 간격 축소: 라벨/값/힌트가 더 붙도록 조정
                                // 라벨/힌트 최소화 — Column은 컨텐츠 높이로 래핑하여 박스 중앙에 배치
                                // label이 잘리는 문제 해결을 위해 최소 높이를 확보
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
                                    // Top label: padding 제거, includeFontPadding=false로 줄여 숫자 중심 정렬 보정
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
                                        // 자동 축소: 가용 너비에 맞춰 큰 숫자 폰트 크기 계산
                                        val textMeasurer = rememberTextMeasurer()
                                        val density = LocalDensity.current
                                        val baseFontSp = baseStyle.fontSize
                                        val maxMultiplier = 2.5f
                                        val minMultiplier = 0.6f

                                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                            val maxWpx = with(density) { maxWidth.toPx() }
                                            // 초기 폰트 크기(px)
                                            val initialSizeSp = (baseFontSp.value * maxMultiplier)
                                            // 측정 루프: 초기 크기에서 최소 크기까지 감소시키며 가로 공간에 맞추기
                                            val chosenSizeSp = remember(valueText, maxWpx) {
                                                var s = initialSizeSp
                                                val minSize = baseFontSp.value * minMultiplier
                                                while (s >= minSize) {
                                                    val styleTry = baseStyle.copy(fontSize = s.sp, platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                    val result = try { textMeasurer.measure(AnnotatedString(valueText), style = styleTry) } catch (_: Throwable) { null }
                                                    val textW = result?.size?.width ?: 0
                                                    if (textW <= maxWpx * 0.92f) break
                                                    s -= 2f // 감소 스텝 (sp)
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

                                            // 숫자/단위 렌더링은 기존 로직과 동일하게 사용
                                            val isMoney = currentIndicator == 2
                                            val isLifeGain = currentIndicator == 4
                                            if (isMoney) {
                                                // 기존: 심볼별 정규식으로 분기하던 코드 -> 선택된 통화를 직접 가져와 처리
                                                val selectedCurrency = kr.sweetapps.alcoholictimer.core.util.CurrencyManager.getSelectedCurrency(context)
                                                val symbol = selectedCurrency.symbol
                                                if (selectedCurrency.code == "KRW") {
                                                    // KRW: 숫자만 보여주고 '원' 단위를 붙임
                                                    val numeric = valueText.replace("원", "").replace("₩", "").trim()
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = "원", style = unitStyle)
                                                    }
                                                } else {
                                                    // 기타 통화: 통화 심볼을 좌측에 두고 숫자 부분만 표시
                                                    // 제거 대상: 통화 심볼, '원', '₩' 등 남아 있을 수 있는 단위들을 모두 제거
                                                    val numeric = valueText.replace(symbol, "")
                                                        .replace("원", "")
                                                        .replace("₩", "")
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

                                    // Bottom hint (작은 패딩으로 숫자와 가깝게 조정)
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
    // 카드 높이를 증가시켜 하단 라벨이 잘 보이도록 함
    Card(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = contentAlignment, verticalArrangement = Arrangement.Center) {
            // Top circular icon
            if (icon != null || iconRes != null) {
                // iconBg가 지정된 칩에는 공통적으로 아래쪽이 더 밝은 수직 그라데이션과 약한 엘리베이션을 적용
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
        val status = if (isCompleted) "완료" else "중지"
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
