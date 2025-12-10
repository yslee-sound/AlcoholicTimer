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
    // Main green start-style button (match StartScreen.ModernStartButton)
    val MAIN_BUTTON_SIZE = 96.dp
    val MAIN_ICON_SIZE = 48.dp
    val MAIN_BUTTON_ELEVATION = AppElevation.CARD_HIGH
    // Ring / progress indicator size around the main stop button
    val MAIN_RING_SIZE = 106.dp
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
    val targetDays = previewTargetDays ?: sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)

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
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(36.dp)
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
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(QuitUiConstants.TOP_CARD_BOTTOM_SPACING))

            // Indicators grid: total days, saved money, saved hours, life gain
            val start = previewStartTime ?: sharedPref.getLong(Constants.PREF_START_TIME, 0L)
            val now = System.currentTimeMillis()
            val elapsedMillis = if (start > 0L) now - start else 0L
            // [FIX] 통계 계산은 실제 시간 기준 (배속 적용 안 함)
            val elapsedDaysFloat = elapsedMillis / Constants.DAY_IN_MILLIS.toFloat()
            val weeks = elapsedDaysFloat / 7.0
            val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)
            val costVal = Constants.DrinkingSettings.getCostValue(selectedCost)
            val freqVal = Constants.DrinkingSettings.getFrequencyValue(selectedFrequency)
            val drinkHoursVal = Constants.DrinkingSettings.getDurationValue(selectedDuration)
            val savedMoney = weeks * freqVal * costVal
            val savedHours = weeks * freqVal * drinkHoursVal
            val lifeGainDays = elapsedDaysFloat / 30.0

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QuitUiConstants.STAT_CARD_GAP)) {
                    // 총 금주 일수: 소수점 1자리 (단위 포함, DetailScreen과 동일 포맷)
                    SmallStatCard(
                        title = stringResource(id = R.string.stat_total_days),
                        value = String.format(Locale.getDefault(), "%.1f%s", elapsedDaysFloat, context.getString(R.string.unit_day)),
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
            Box(
                modifier = Modifier.fillMaxWidth().height(QuitUiConstants.MAIN_RING_SIZE),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(QuitUiConstants.MAIN_RING_SIZE)
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
                                            try {
                                                val start = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
                                                val endTime = System.currentTimeMillis()
                                                // [FIX] 기록 저장은 실제 날짜 기준 (배속 적용 안 함)
                                                val actualDays = (((endTime - start) / Constants.DAY_IN_MILLIS)).toInt()

                                                saveCompletedRecord(
                                                    context = context,
                                                    startTime = start,
                                                    endTime = endTime,
                                                    targetDays = targetDays,
                                                    actualDays = actualDays
                                                )
                                                // [FIX] 포기 시 완료 상태를 false로 설정 (취소는 완료가 아님)
                                                sharedPref.edit {
                                                    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                                                    remove(Constants.PREF_START_TIME)
                                                }

                                                // [FIX] TimerStateRepository에도 명확히 취소 상태 저장
                                                try {
                                                    kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                                                    kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
                                                    android.util.Log.d("QuitScreen", "타이머 취소: 완료 상태 false로 설정")
                                                } catch (t: Throwable) {
                                                    android.util.Log.e("QuitScreen", "타이머 상태 저장 실패", t)
                                                }
                                            } catch (_: Throwable) { }
                                            onQuitConfirmed()
                                        }
                                    }
                                    waitForUpOrCancellation(); isPressed = false; job.cancel()
                                }
                            }
                    ) {
                        // 배경 원 (회색)
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(QuitUiConstants.MAIN_RING_SIZE), color = Color(0xFFE0E0E0), strokeWidth = 4.dp, trackColor = Color.Transparent)
                        // 진행 상태 원 (빨간색)
                        if (showPressed) {
                            CircularProgressIndicator(progress = { showProgress }, modifier = Modifier.size(QuitUiConstants.MAIN_RING_SIZE), color = Color(0xFFD32F2F), strokeWidth = 4.dp, trackColor = Color.Transparent)
                        }
                        // 중지 버튼 (터치 핸들러는 외부 Box로 옮겨짐)
                        Card(
                            modifier = Modifier.size(QuitUiConstants.MAIN_BUTTON_SIZE),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(id = R.string.cd_stop), tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                    // 취소 버튼 자리에 시작화면의 ModernStartButton과 동일한 크기/디자인의 버튼을 배치
                    // use shared MainActionButton (same as Start screen)
                    MainActionButton(onClick = { onCancel() }, size = QuitUiConstants.MAIN_BUTTON_SIZE, iconSize = QuitUiConstants.MAIN_ICON_SIZE, elevationDp = QuitUiConstants.MAIN_BUTTON_ELEVATION)
                 }
             }
         }
     )
 }

private fun saveCompletedRecord(context: Context, startTime: Long, endTime: Long, targetDays: Float, actualDays: Int) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val recordId = System.currentTimeMillis().toString()
        val isCompleted = actualDays >= targetDays
        val status = if (isCompleted) "완료" else "중지"

        // Log analytics event based on completion status
        try {
            if (isCompleted) {
                AnalyticsManager.logTimerFinish(
                    targetDays = targetDays.toInt(),
                    actualDays = actualDays,
                    startTs = startTime,
                    endTs = endTime
                )
            } else {
                AnalyticsManager.logTimerEnd(
                    targetDays = targetDays.toInt(),
                    actualDays = actualDays,
                    reason = "user_quit",
                    startTs = startTime,
                    endTs = endTime
                )
            }
        } catch (_: Throwable) {
            // Best-effort logging, do not crash the app
        }

        val record = JSONObject().apply {
            put("id", recordId); put("startTime", startTime); put("endTime", endTime); put("targetDays", targetDays.toInt()); put("actualDays", actualDays); put("isCompleted", isCompleted); put("status", status); put("createdAt", System.currentTimeMillis())
        }
        val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val list = try {
            JSONArray(recordsJson)
        } catch (_: Exception) {
            JSONArray()
        }
        list.put(record)
        sharedPref.edit {
            putString(Constants.PREF_SOBRIETY_RECORDS, list.toString())
        }
    } catch (_: Exception) { }
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
    Card(
        modifier = modifier.height(QuitUiConstants.STAT_CARD_HEIGHT),
        shape = RoundedCornerShape(QuitUiConstants.STAT_CARD_CORNER),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(1.dp, accentColor.copy(alpha = QuitUiConstants.STAT_CARD_BORDER_ALPHA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null || iconRes != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg ?: accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    } else {
                        iconRes?.let { res ->
                            Image(painter = painterResource(id = res), contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.fillMaxHeight().weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = value,
                    // slightly smaller, more compact typography to avoid clipping at large system font sizes
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 22.sp),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(id = R.color.color_stat_title_gray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
         }
     }
 }

