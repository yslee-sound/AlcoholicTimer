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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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

    // SharedPreferences 접근은 런타임에서만 수행 (Preview 안전성)
    val sp = if (isPreview) null else context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val startTime = if (isPreview) (System.currentTimeMillis() - (2 * Constants.DAY_IN_MILLIS)) else sp!!.getLong(Constants.PREF_START_TIME, 0L)
    val targetDays = if (isPreview) 30f else sp!!.getFloat(Constants.PREF_TARGET_DAYS, 30f)
    val timerCompleted = if (isPreview) false else sp!!.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

    if (!isPreview) {
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
    var currentIndicator by remember { mutableIntStateOf(if (isPreview) 0 else sp!!.getInt(indicatorKey, 0)) }

    fun toggleIndicator() {
        val next = (currentIndicator + 1) % 5
        currentIndicator = next
        if (!isPreview) {
            sp!!.edit().putInt(indicatorKey, next).apply()
        }
    }

    var hasCompleted by remember { mutableStateOf(false) }
    if (!isPreview) {
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
                    sp!!.edit().remove(Constants.PREF_START_TIME).putBoolean(Constants.PREF_TIMER_COMPLETED, true).apply()
                    hasCompleted = true

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
                            icon = Icons.Filled.CalendarToday,
                            iconBg = Color(0xFFEAF3FF),
                            contentAlignment = runStatAlignments[0]
                        )

                        // Center: pastel mint circle, green icon
                        RunStatChip(
                            title = stringResource(id = R.string.stat_level),
                            value = levelDisplayText,
                            color = Color(0xFF06AD6A),
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.EmojiEvents,
                            iconBg = Color(0xFFE6FFF4),
                            contentAlignment = runStatAlignments[1]
                        )

                        // Right: pastel lilac circle, indigo icon
                        RunStatChip(
                            title = stringResource(id = R.string.stat_time),
                            value = progressTimeTextHM,
                            color = Color(0xFF5873D6),
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.AccessTime,
                            iconBg = Color(0xFFEEF2FF),
                            contentAlignment = runStatAlignments[2]
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().height(168.dp).clickable { toggleIndicator() },
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
                                // 힌트 텍스트 클리핑 방지: hintBoxH를 늘리고 간격을 약간 조정
                                val labelBoxH = 28.dp; val valueBoxH = 64.dp; val hintBoxH = 20.dp; val gapSmall = 4.dp; val gapMedium = 2.dp
                                val (label, valueText, _) = when (currentIndicator) {
                                    0 -> Triple(stringResource(id = R.string.indicator_title_days), String.format(Locale.getDefault(), "%.1f", elapsedDaysFloat), colorResource(id = R.color.color_indicator_days))
                                    1 -> Triple(stringResource(id = R.string.indicator_title_time), progressTimeText, colorResource(id = R.color.color_indicator_time))
                                    2 -> Triple(stringResource(id = R.string.indicator_title_saved_money), FormatUtils.formatMoney(context, savedMoney).replace(" ", ""), colorResource(id = R.color.color_indicator_money))
                                    3 -> Triple(stringResource(id = R.string.indicator_title_saved_hours), FormatUtils.formatHoursValue(savedHours), colorResource(id = R.color.color_indicator_hours))
                                    else -> Triple(stringResource(id = R.string.indicator_title_life_gain), formattedLifeGain, colorResource(id = R.color.color_indicator_life))
                                }
                                // Layout: use full card height so main value centers correctly
                                Column(modifier = Modifier.fillMaxSize().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Top label (fixed height)
                                    Box(modifier = Modifier.height(labelBoxH).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        val base = MaterialTheme.typography.titleMedium
                                        Text(
                                            text = label,
                                            style = base.copy(
                                                color = Color.White,
                                                lineHeight = base.fontSize * 1.2f,
                                                fontWeight = FontWeight.Bold,
                                                platformStyle = PlatformTextStyle(includeFontPadding = true),
                                                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1f), blurRadius = 2f)
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Middle: expand and center main value (use Box for strict centering)
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        val baseStyle = MaterialTheme.typography.headlineMedium
                                        val bigSize = (baseStyle.fontSize.value * 2.0f).sp
                                        val bigStyle = baseStyle.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = bigSize,
                                            lineHeight = bigSize * 1.05f,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            fontFeatureSettings = "tnum",
                                            shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 4f)
                                        )
                                        val unitStyle = baseStyle.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = baseStyle.fontSize * 1.0f,
                                            lineHeight = baseStyle.fontSize * 1.05f,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 2f)
                                        )

                                        val isMoney = currentIndicator == 2
                                        val isLifeGain = currentIndicator == 4
                                        if (isMoney) {
                                            val dollarMatch = Regex("""\$([0-9,]+(?:\.[0-9]+)?)""").find(valueText)
                                            val yenMatch = Regex("""¥([0-9,]+)""").find(valueText)
                                            when {
                                                dollarMatch != null -> {
                                                    val numeric = dollarMatch.groupValues[1]
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                        Text(text = "$", style = unitStyle)
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                    }
                                                }
                                                yenMatch != null -> {
                                                    val numeric = yenMatch.groupValues[1]
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                        Text(text = "¥", style = unitStyle)
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                    }
                                                }
                                                else -> {
                                                    val numeric = valueText.replace("원", "").replace("₩", "").trim()
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                        Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(text = "원", style = unitStyle)
                                                    }
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
                                            Text(text = valueText, style = bigStyle, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                                        }
                                    }

                                    // Bottom hint (fixed height)
                                    Box(modifier = Modifier.height(hintBoxH).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        val base = MaterialTheme.typography.labelMedium
                                        Text(
                                            text = stringResource(id = R.string.tap_to_switch_indicator),
                                            style = base.copy(
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 16.sp,
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
                Text(text = "진행 상황", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF111111))
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

        // bottom labels: start / target
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "시작", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF718096)))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "목표: ${targetDays.toInt()}일", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF718096)))
        }
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
            if (icon != null) {
                Box(modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg ?: color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Large value
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111)),
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
