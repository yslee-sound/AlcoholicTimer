// [NEW] Core UI 리팩토링: QuitScreen을 tab_01/screens로 이동
package kr.sweetapps.alcoholictimer.ui.tab_01.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit
import java.util.Locale
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import kr.sweetapps.alcoholictimer.ui.tab_01.components.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.ui.tab_01.components.MainActionButton
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation
import kr.sweetapps.alcoholictimer.util.utils.FormatUtils
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager
import kr.sweetapps.alcoholictimer.util.manager.TimerTimeManager
import kr.sweetapps.alcoholictimer.ui.common.rememberUserSettingsState
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.round

// Local UI constants for QuitScreen only (do not reference UiConstants)
private object QuitUiConstants {
    // Per-screen horizontal padding for all cards in this screen (defaults to global card padding)
    val TOP_CARD_TOP_PADDING = 15.dp
    // [NEW] 최상단 카드("정말 멈추시겠어요?") 아래 여백 (통계 카드와의 간격)
    val TOP_CARD_BOTTOM_SPACING = 15.dp
    // Use same card horizontal padding by default
    val CARDS_VERTICAL_SPACING = 10.dp
    val CARD_HORIZONTAL_PADDING = 20.dp
    // STATS_HORIZONTAL_PADDING removed - not used
    // spacing between the four stat cards (horizontal gap inside rows)
    val STAT_CARD_GAP = 10.dp
    // Per-screen vertical spacing between cards (use global default unless overridden)
    // (removed STATS_ROWS_VERTICAL_SPACING; use CARDS_VERTICAL_SPACING for all card vertical gaps)
    // Reduced height to make cards more compact and place icon left of text
    val STAT_CARD_HEIGHT = 96.dp
    val STAT_CARD_CORNER = 12.dp
    val STAT_CARD_BORDER_ALPHA = 0.08f
    // local-only constants; keep minimal and used
    // [SIZE_REDUCTION] Main button 80% size (96dp → 77dp)
    val MAIN_BUTTON_SIZE = 77.dp
    val MAIN_ICON_SIZE = 39.dp
    val MAIN_BUTTON_ELEVATION = AppElevation.CARD_HIGH
    // Ring / progress indicator size around the main stop button (80% of 106dp = 85dp)
    val MAIN_RING_SIZE = 85.dp
}

@Composable
fun QuitScreenComposable(
    onQuitConfirmed: () -> Unit,
    onCancel: () -> Unit,
    // optional overrides used only for Preview (or tests)
    previewStartTime: Long? = null,
    previewTargetDays: Float? = null,
    previewIsPressed: Boolean? = null,
    previewProgress: Float? = null
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val targetDays = previewTargetDays ?: sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 1f) // [CHANGED] 기본값 30 -> 1 (2025-12-25)

    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    // preview overrides: if provided, use them for rendering instead of internal state
    val showPressed = previewIsPressed ?: isPressed
    val showProgress = previewProgress ?: progress
    val coroutineScope = rememberCoroutineScope()

    StandardScreenWithBottomButton(
        // Overlay: match RunScreen's subtle top highlight and bottom darkening
        backgroundDecoration = {
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.25f to Color.White.copy(alpha = 0.18f),
                        0.7f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.12f)
                    )
                )
            )
        },
        screenBackground = Color(0xFFEEEDE9),
        topPadding = QuitUiConstants.TOP_CARD_TOP_PADDING,
        horizontalPadding = QuitUiConstants.CARD_HORIZONTAL_PADDING,
        forceFillMaxWidth = true,
        topContent = {
            // [FIXED_SIZE] 폰트 스케일의 영향을 받지 않는 고정 크기 적용
            val density = androidx.compose.ui.platform.LocalDensity.current
            val warningIconSizePx = with(density) { 72.dp.toPx() }
            val warningIconSize = with(density) { (warningIconSizePx / density.density).dp }
            val innerIconSizePx = with(density) { 36.dp.toPx() }
            val innerIconSize = with(density) { (innerIconSizePx / density.density).dp }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiConstants.CARD_CORNER_RADIUS),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = UiConstants.CARD_PADDING),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon badge
                    Box(modifier = Modifier.padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .requiredSize(warningIconSize)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.requiredSize(innerIconSize)
                            )
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.quit_confirm_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.quit_confirm_subtitle),
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp, // [NEW] 줄 간격 추가 - 두 줄 이상일 때 겹치지 않게 (2025-12-26)
                        modifier = Modifier
                            .padding(horizontal = 16.dp) // [NEW] 좌우 여백 추가 - 카드 테두리에 붙지 않게 (2025-12-26)
                            .padding(top = 4.dp) // 상단 여백 미세 조정
                    )
                }
            }

            Spacer(modifier = Modifier.height(QuitUiConstants.TOP_CARD_BOTTOM_SPACING))

            // [REFACTORED] TimerTimeManager에서 경과 시간 가져오기 (배속 적용됨)
            val elapsedMillisFromManager by TimerTimeManager.elapsedMillis.collectAsState()

            // [REFACTORED] TimerTimeManager가 계산한 시간 사용 (배속 이미 적용됨)
            val elapsedMillis = if (previewStartTime != null) {
                // Preview 모드: 기존 계산 방식 사용
                val now = System.currentTimeMillis()
                if (previewStartTime > 0L) now - previewStartTime else 0L
            } else {
                // 실제 모드: TimerTimeManager 값 사용 (배속 적용됨)
                elapsedMillisFromManager
            }

            // [FIX] Tab 1, Tab 2, Tab 3와 동일하게 '순수 경과 일수(Duration)'로 통일
            // 기존의 +1.0 보정 제거 (0-based 순수 경과 시간)
            val elapsedDaysFloat = (elapsedMillis / Constants.DAY_IN_MILLIS.toFloat())
            val weeks = elapsedDaysFloat / 7.0

            // [NEW] 실시간 설정 변경 감지 - 탭4에서 설정을 바꾸면 즉시 반영됨
            val userSettings by rememberUserSettingsState(context)
            val costVal = Constants.DrinkingSettings.getCostValue(userSettings.cost)
            val freqVal = Constants.DrinkingSettings.getFrequencyValue(userSettings.frequency)
            val drinkHoursVal = Constants.DrinkingSettings.getDurationValue(userSettings.duration)

            val savedMoney = weeks * freqVal * costVal
            val savedHours = weeks * freqVal * drinkHoursVal
            val lifeGainDays = elapsedDaysFloat / 30.0

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QuitUiConstants.STAT_CARD_GAP)) {
                    // [CHANGED] 총 금주 일수: Floor(내림) 방식으로 변경 - 레벨 카드와 동기화 (2025-12-25)
                    // [REFACTORED] FormatUtils.formatDaysWithUnit 사용하여 중앙 집중식 포맷팅 (2025-12-26)
                    // 예: 1.96일 -> "1.9 Hari", 2.00일 -> "2.0 Hari"
                    val displayDays = kotlin.math.floor(elapsedDaysFloat * 10.0) / 10.0
                    SmallStatCard(
                        title = stringResource(id = R.string.stat_total_days),
                        value = FormatUtils.formatDaysWithUnit(context, displayDays),
                        accentColor = colorResource(id = R.color.color_indicator_days),
                        iconRes = R.drawable.calendar_blank,
                        iconBg = Color(0xFFD6E8FF),
                        modifier = Modifier.weight(1f)
                    )
                    // 절약한 금액: 소수점 없이 로케일/통화 규칙에 따라 포맷 (DetailScreen과 동일)
                    val savedMoneyRounded = round(savedMoney)
                    val savedMoneyStr = CurrencyManager.formatMoneyNoDecimals(savedMoneyRounded, context)
                    SmallStatCard(
                        title = stringResource(id = R.string.indicator_title_saved_money),
                        value = savedMoneyStr,
                        accentColor = colorResource(id = R.color.color_indicator_money),
                        iconRes = R.drawable.currency_dollar_simple,
                        iconBg = Color(0xFFFFE6EC),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(QuitUiConstants.CARDS_VERTICAL_SPACING))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QuitUiConstants.STAT_CARD_GAP)) {
                    // 절약한 시간: 소수점 1자리 + 단위 (DetailScreen과 동일)
                    val savedHoursStr = FormatUtils.formatHoursWithUnitFixed(context, savedHours, 1)
                    SmallStatCard(
                        title = stringResource(id = R.string.indicator_title_saved_hours),
                        value = savedHoursStr,
                        accentColor = colorResource(id = R.color.color_indicator_hours),
                        iconRes = R.drawable.clock,
                        iconBg = Color(0xFFFFF3E0),
                        modifier = Modifier.weight(1f)
                    )
                    // 기대 수명+: 일+시간 포맷, 소수점1자리 (DetailScreen과 동일)
                    val lifeGainStr = FormatUtils.daysToDayHourStringFixed(context, lifeGainDays, 1)
                    SmallStatCard(
                        title = stringResource(id = R.string.indicator_title_life_gain),
                        value = lifeGainStr,
                        accentColor = colorResource(id = R.color.color_indicator_life),
                        iconRes = kr.sweetapps.alcoholictimer.R.drawable.heartbeat,
                        iconBg = Color(0xFFF0E8FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        bottomButton = {
            // [FIXED_SIZE] 시스템 폰트 스케일의 영향을 받지 않는 고정 크기 적용
            val density = androidx.compose.ui.platform.LocalDensity.current
            val ringSizePx = with(density) { QuitUiConstants.MAIN_RING_SIZE.toPx() }
            val ringSize = with(density) { (ringSizePx / density.density).dp }
            val buttonSizePx = with(density) { QuitUiConstants.MAIN_BUTTON_SIZE.toPx() }
            val buttonSize = with(density) { (buttonSizePx / density.density).dp }
            val iconSizePx = with(density) { 39.dp.toPx() } // [SIZE_REDUCTION] 48dp → 39dp
            val iconSize = with(density) { (iconSizePx / density.density).dp }

            Box(
                modifier = Modifier.fillMaxWidth().height(ringSize),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(ringSize)
                    ) {
                        // 배경 원 (회색)
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(ringSize), color = Color(0xFFE0E0E0), strokeWidth = 4.dp, trackColor = Color.Transparent)
                        // 진행 상태 원 (빨간색)
                        if (showPressed) {
                            CircularProgressIndicator(progress = { showProgress }, modifier = Modifier.size(ringSize), color = Color(0xFFD32F2F), strokeWidth = 4.dp, trackColor = Color.Transparent)
                        }
                        // [FIX] 중지 버튼 - 길게 누르기 효과를 위해 Surface + pointerInput 사용
                        Surface(
                            modifier = Modifier
                                .requiredSize(buttonSize)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(); isPressed = true; progress = 0f
                                        val job = coroutineScope.launch {
                                            val duration = 1500L
                                            val startMs = System.currentTimeMillis()
                                            while (progress < 1f && isPressed) {
                                                val elapsed = System.currentTimeMillis() - startMs
                                                progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                                delay(16)
                                            }
                                            if (progress >= 1f && isPressed) {
                                                android.util.Log.d("QuitScreen", "🔴 [QUIT] 포기 버튼 길게 누름 완료! onQuitConfirmed() 호출")
                                                onQuitConfirmed()
                                                android.util.Log.d("QuitScreen", "🔴 [QUIT] onQuitConfirmed() 호출 완료")
                                            }
                                        }
                                        waitForUpOrCancellation(); isPressed = false; job.cancel()
                                    }
                                },
                            shape = CircleShape,
                            color = Color(0xFFD32F2F),
                            shadowElevation = 6.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.cd_stop),
                                    tint = Color.White,
                                    modifier = Modifier.requiredSize(iconSize)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                    // [FAB_UNIFIED] 취소 버튼을 FloatingActionButton으로 변경
                    FloatingActionButton(
                        onClick = { onCancel() },
                        modifier = Modifier.requiredSize(buttonSize),
                        containerColor = colorResource(id = R.color.color_progress_primary),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.requiredSize(iconSize)
                        )
                    }
                 }
             }
         }
     )
 }


@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
fun QuitScreenPreview() {
    QuitScreenComposable(
        onQuitConfirmed = {},
        onCancel = {}
    )
}

@Composable
private fun SmallStatCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, iconRes: Int? = null, iconBg: Color? = null) {
    // [FIXED_SIZE] 폰트 스케일의 영향을 받지 않는 고정 크기 적용
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cardHeightPx = with(density) { QuitUiConstants.STAT_CARD_HEIGHT.toPx() }
    val cardHeight = with(density) { (cardHeightPx / density.density).dp }
    val iconSizePx = with(density) { 40.dp.toPx() }
    val iconSize = with(density) { (iconSizePx / density.density).dp }
    val innerIconSizePx = with(density) { 18.dp.toPx() }
    val innerIconSize = with(density) { (innerIconSizePx / density.density).dp }

    Card(
        modifier = modifier.requiredHeight(cardHeight),
        shape = RoundedCornerShape(QuitUiConstants.STAT_CARD_CORNER),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(1.dp, accentColor.copy(alpha = QuitUiConstants.STAT_CARD_BORDER_ALPHA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp), // [FIX] 4dp → 12dp (여백 확대)
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null || iconRes != null) {
                Box(
                    modifier = Modifier
                        .requiredSize(iconSize)
                        .clip(CircleShape)
                        .background(iconBg ?: accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.requiredSize(innerIconSize))
                    } else {
                        iconRes?.let { res ->
                            Image(painter = painterResource(id = res), contentDescription = null, modifier = Modifier.requiredSize(innerIconSize))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp)) // [FIX] 6dp → 8dp (아이콘-텍스트 간격 확대)
            }

            Column(
                modifier = Modifier.fillMaxHeight().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End // [FIX] 텍스트 우측 정렬
            ) {
                // [FIX] TextMeasurer 기반 사전 계산으로 숫자 잘림 방지
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                    val maxPixels = with(density) { maxWidth.toPx() }

                    val baseStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp
                    )

                    // 사전 계산: 텍스트 너비가 maxWidth에 들어올 때까지 폰트 축소
                    val calculatedSize = remember(value, maxPixels) {
                        var currentSize = 17f // [FIX] 20f → 17f (시작 크기 감소)
                        val minSize = 9f // 최소 9sp

                        while (currentSize > minSize) {
                            val result = textMeasurer.measure(
                                text = androidx.compose.ui.text.AnnotatedString(value),
                                style = baseStyle.copy(fontSize = currentSize.sp)
                            )
                            if (result.size.width <= maxPixels * 0.95f) { // 5% 여유
                                break
                            }
                            currentSize -= 1f // 1sp씩 정밀 축소
                        }
                        currentSize.coerceAtLeast(minSize).sp
                    }

                    Text(
                        text = value,
                        style = baseStyle.copy(fontSize = calculatedSize),
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.End, // [FIX] 우측 정렬
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), // [FIX] 폰트 크기 명시
                    color = colorResource(id = R.color.color_stat_title_gray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End // [FIX] 우측 정렬
                )
            }
         }
     }
 }

