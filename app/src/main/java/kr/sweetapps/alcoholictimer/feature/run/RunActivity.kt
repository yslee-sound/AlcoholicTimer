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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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
    val RUN_CARD_CONTENT_HORIZONTAL_PADDING = 12.dp
    // 카드 내부의 수직 패딩은 별도 상수(기존 12.dp 유지)
    // 내부 수직 패딩을 0으로 하면 흰색 패널(Progress Surface) 상단과 위 카드가 더 붙습니다.
    val RUN_CARD_CONTENT_VERTICAL_PADDING = 12.dp // 프로그레스 내부 패딩 (기본 0) 12

    // Per-chip horizontal alignment (left / center / right)
    val runStatAlignments = listOf(Alignment.Start, Alignment.CenterHorizontally, Alignment.End)
    // Local spacing for the top stat chips so the spacing can be tuned per-screen
    // Detached from UiConstants: kept local so this file controls spacing independently
    val RUN_STAT_CHIP_SPACING = 0.dp
    // New: spacing specifically for the top stat chip group (horizontal gap between the 3 chips)
    val RUN_TOP_GROUP_CHIP_SPACING = 15.dp

    BackHandler(enabled = true) {
        // NavHost 내에서는 뒤로가기를 소비해 백그라운드 이동 대신 유지
    }

    val sp = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val startTime = sp.getLong(Constants.PREF_START_TIME, 0L)
    val targetDays = sp.getFloat(Constants.PREF_TARGET_DAYS, 30f)
    val timerCompleted = sp.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

    LaunchedEffect(startTime, timerCompleted) {
        if (timerCompleted || startTime == 0L) {
            onRequireBackToStart?.invoke()
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val elapsedMillis by remember(now, startTime) {
        derivedStateOf { if (startTime > 0) now - startTime else 0L }
    }
    val elapsedDaysFloat = remember(elapsedMillis) { elapsedMillis / Constants.DAY_IN_MILLIS.toFloat() }

    val levelDays = remember(elapsedMillis) { Constants.calculateLevelDays(elapsedMillis) }
    val levelInfo = remember(levelDays) { LevelDefinitions.getLevelInfo(levelDays) }
    val levelNumber = remember(levelDays) { LevelDefinitions.getLevelNumber(levelDays) + 1 } // +1 for display (1-indexed)
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
    var currentIndicator by remember { mutableIntStateOf(sp.getInt(indicatorKey, 0)) }

    fun toggleIndicator() { val next = (currentIndicator + 1) % 5; currentIndicator = next; sp.edit().putInt(indicatorKey, next).apply() }

    var hasCompleted by remember { mutableStateOf(false) }
    LaunchedEffect(progress) {
        if (!hasCompleted && progress >= 1f && startTime > 0) {
            try {
                saveCompletedRecord(
                    context = context,
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    targetDays = targetDays,
                    actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt()
                )
                sp.edit().remove(Constants.PREF_START_TIME).putBoolean(Constants.PREF_TIMER_COMPLETED, true).apply()
                hasCompleted = true

                // toast suppressed per request
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

                // 직접 전면광고 호출 제거: 홈 전환 3회 규칙 준수
                goDetail()
                // 다음 기회 대비 프리로드만 유지
                kr.sweetapps.alcoholictimer.ads.InterstitialAdManager.preload(context.applicationContext)
            } catch (_: Exception) { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StandardScreenWithBottomButton(
            topPadding = 0.dp,
            horizontalPadding = RUN_HORIZONTAL_PADDING,
            forceFillMaxWidth = true,
            backgroundDecoration = {
                Box(modifier = Modifier.matchParentSize().background(Color(0xFFEEEDE9)))
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
                        RunStatChip(title = stringResource(id = R.string.stat_goal_days), value = goalDaysText, color = colorResource(id = R.color.color_stat_goal), modifier = Modifier.weight(1f), darkBackground = true, contentAlignment = runStatAlignments[0])
                        RunStatChip(title = stringResource(id = R.string.stat_level), value = levelDisplayText, color = levelInfo.color, modifier = Modifier.weight(1f), darkBackground = true, contentAlignment = runStatAlignments[1])
                        RunStatChip(title = stringResource(id = R.string.stat_time), value = progressTimeTextHM, color = colorResource(id = R.color.color_stat_time), modifier = Modifier.weight(1f), darkBackground = true, contentAlignment = runStatAlignments[2])
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().height(168.dp).clickable { toggleIndicator() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH),
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
                                val labelBoxH = 36.dp; val valueBoxH = 66.dp; val hintBoxH = 20.dp; val gapSmall = 6.dp; val gapMedium = 8.dp
                                val (label, valueText, _) = when (currentIndicator) {
                                    0 -> Triple(stringResource(id = R.string.indicator_title_days), String.format(Locale.getDefault(), "%.1f", elapsedDaysFloat), colorResource(id = R.color.color_indicator_days))
                                    1 -> Triple(stringResource(id = R.string.indicator_title_time), progressTimeText, colorResource(id = R.color.color_indicator_time))
                                    2 -> Triple(stringResource(id = R.string.indicator_title_saved_money), FormatUtils.formatMoney(context, savedMoney).replace(" ", ""), colorResource(id = R.color.color_indicator_money))
                                    3 -> Triple(stringResource(id = R.string.indicator_title_saved_hours), FormatUtils.formatHoursValue(savedHours), colorResource(id = R.color.color_indicator_hours))
                                    else -> Triple(stringResource(id = R.string.indicator_title_life_gain), formattedLifeGain, colorResource(id = R.color.color_indicator_life))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.fillMaxWidth().height(labelBoxH), contentAlignment = Alignment.Center) {
                                        val base = MaterialTheme.typography.titleMedium
                                        // label text in white for readability over background image
                                        Text(
                                            text = label,
                                            style = base.copy(
                                                color = Color.White,
                                                lineHeight = base.fontSize * 1.2f,
                                                platformStyle = PlatformTextStyle(includeFontPadding = true),
                                                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1f), blurRadius = 2f)
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(gapSmall))
                                    Box(modifier = Modifier.fillMaxWidth().height(valueBoxH), contentAlignment = Alignment.Center) {
                                        val baseStyle = MaterialTheme.typography.headlineMedium
                                        val bigSize = (baseStyle.fontSize.value * 1.5f).sp
                                        // 모든 값 텍스트를 흰색으로 변경하여 이미지 위 가독성 확보
                                        val bigStyle = baseStyle.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = bigSize,
                                            lineHeight = bigSize * 1.1f,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true),
                                            fontFeatureSettings = "tnum",
                                            shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 4f)
                                        )
                                        val unitStyle = baseStyle.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = baseStyle.fontSize,
                                            lineHeight = baseStyle.fontSize * 1.1f,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true),
                                            shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 2f)
                                        )
                                        val isMoney = currentIndicator == 2
                                        val isLifeGain = currentIndicator == 4
                                        if (isMoney) {
                                            // 다국어 통화 형식 처리
                                            val dollarMatch = Regex("""\$([0-9,]+(?:\.[0-9]+)?)""" ).find(valueText)
                                            val yenMatch = Regex("""¥([0-9,]+)""" ).find(valueText)

                                            when {
                                                dollarMatch != null -> {
                                                    // 달러 형식: $1,000.00
                                                    val numeric = dollarMatch.groupValues[1]
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                                        Text(text = "$", style = unitStyle, modifier = Modifier.alignByBaseline())
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                                    }
                                                }
                                                yenMatch != null -> {
                                                    // 엔화 형식: ¥1,000
                                                    val numeric = yenMatch.groupValues[1]
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                                        Text(text = "¥", style = unitStyle, modifier = Modifier.alignByBaseline())
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                                    }
                                                }
                                                else -> {
                                                    // 원화 형식: 1,000원 또는 ₩1,000
                                                    val numeric = valueText.replace("원", "").replace("₩", "").trim()
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = "원", style = unitStyle, modifier = Modifier.alignByBaseline())
                                                    }
                                                }
                                            }
                                        } else if (isLifeGain) {
                                            // 다국어 지원: "1일 2.5시간" 또는 "1 day(s) 2.5 hr(s)" 또는 "1日 2.5時間"
                                            val twoPart = Regex("""(\d+)\s*(?:일|日|day\(s\))\s*([0-9]+(?:\.[0-9]+)?)\s*(?:시간|時間|hr\(s\))""")
                                            val onePart = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:시간|時間|hr\(s\))""")
                                            val m1 = twoPart.find(valueText)
                                            val m2 = if (m1 == null) onePart.find(valueText) else null
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                                if (m1 != null) {
                                                    val dStr = m1.groupValues[1]
                                                    val hStr = m1.groupValues[2]
                                                    Text(text = dStr, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = stringResource(R.string.unit_day), style = unitStyle, modifier = Modifier.alignByBaseline())
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = hStr, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = stringResource(R.string.unit_hour), style = unitStyle, modifier = Modifier.alignByBaseline())
                                                } else if (m2 != null) {
                                                    val hStr = m2.groupValues[1]
                                                    Text(text = hStr, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = stringResource(R.string.unit_hour), style = unitStyle, modifier = Modifier.alignByBaseline())
                                                } else {
                                                    Text(text = valueText, style = bigStyle, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = valueText,
                                                style = bigStyle,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(gapMedium))
                                    Box(modifier = Modifier.fillMaxWidth().height(hintBoxH), contentAlignment = Alignment.Center) {
                                        val base = MaterialTheme.typography.labelMedium
                                        // hint text also white for consistency
                                        Text(
                                            text = stringResource(id = R.string.tap_to_switch_indicator),
                                            style = base.copy(
                                                color = Color.White,
                                                lineHeight = base.fontSize * 1.2f,
                                                platformStyle = PlatformTextStyle(includeFontPadding = true),
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        // 내부에 흰색 둥근 컨테이너를 추가하여 이전 모양(둥근 흰색 패널 안의 프로그레스바)을 복원
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light)),
                            tonalElevation = 0.dp
                        ) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RUN_CARD_CONTENT_HORIZONTAL_PADDING, vertical = RUN_CARD_CONTENT_VERTICAL_PADDING), horizontalAlignment = Alignment.CenterHorizontally) {
                                ModernProgressIndicatorSimple(progress = progress)
                            }
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
private fun ModernProgressIndicatorSimple(progress: Float) {
    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(1000); blink = !blink } }
    val alpha by animateFloatAsState(targetValue = if (blink) 1f else 0.3f, animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "blink")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = (progress * 100).toInt().coerceIn(0, 100).toString() + "%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colorResource(id = R.color.color_progress_primary)))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colorResource(id = R.color.color_progress_primary).copy(alpha = alpha)))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = colorResource(id = R.color.color_progress_primary),
            trackColor = colorResource(id = R.color.color_progress_track),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
        )
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
private fun RunStatChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    darkBackground: Boolean = false,
    lightStyle: Boolean = false,
    contentAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    // lightStyle: Start 화면 스타일처럼 연한 시안 배경에 선명한 블루 텍스트
    val bgColor = when {
        lightStyle -> Color(0xFFE8F8FB)
        darkBackground -> Color.Black.copy(alpha = 0.35f)
        else -> color.copy(alpha = 0.1f)
    }
    val valueColor = when {
        lightStyle -> Color(0xFF00AEEF) // 선명한 시안 블루
        darkBackground -> Color.White
        else -> color
    }
    val labelColor = if (lightStyle) valueColor.copy(alpha = 0.9f) else if (darkBackground) Color.White else colorResource(id = R.color.color_stat_title_gray)

    Surface(modifier = modifier.height(84.dp), shape = RoundedCornerShape(12.dp), color = bgColor, shadowElevation = if (lightStyle) 6.dp else 0.dp, tonalElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = contentAlignment) {
            val baseValue = MaterialTheme.typography.titleMedium
            val isTime = value.contains(":")
            val baseFactor = if (isTime) 0.92f else 0.98f
            // 값 텍스트를 기존 대비 약 18% 키움
            val valueSize = ((baseValue.fontSize.value * baseFactor) * 1.18f).sp
            val valueStyle = baseValue.copy(
                fontWeight = FontWeight.Bold,
                fontSize = valueSize,
                lineHeight = valueSize * 1.1f,
                platformStyle = PlatformTextStyle(includeFontPadding = true),
                fontFeatureSettings = "tnum",
                color = valueColor
            )
            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                AutoResizeSingleLineText(
                    text = value,
                    baseStyle = valueStyle,
                    minFontSizeSp = (baseValue.fontSize.value * 0.75f),
                    color = valueColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val baseLabel = MaterialTheme.typography.labelMedium
            // 라벨을 약간 키워 가독성 향상
            val labelSize = (baseLabel.fontSize.value * 1.05f).sp
            val labelStyle = baseLabel.copy(
                fontSize = labelSize,
                lineHeight = labelSize * 1.2f,
                platformStyle = PlatformTextStyle(includeFontPadding = true),
                color = labelColor
            )
            Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                AutoResizeSingleLineText(
                    text = title,
                    baseStyle = labelStyle,
                    minFontSizeSp = (baseLabel.fontSize.value * 0.85f),
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
