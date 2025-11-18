package kr.sweetapps.alcoholictimer.feature.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.StandardScreenWithBottomButton
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.util.FormatUtils

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
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, android.content.Context.MODE_PRIVATE)
    val targetDays = previewTargetDays ?: sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)

    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    // preview overrides: if provided, use them for rendering instead of internal state
    val showPressed = previewIsPressed ?: isPressed
    val showProgress = previewProgress ?: progress
    val coroutineScope = rememberCoroutineScope()

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiConstants.CARD_CORNER_RADIUS),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(UiConstants.CARD_PADDING),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                        Text("🤔", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
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

            Spacer(modifier = Modifier.height(UiConstants.CARD_VERTICAL_SPACING))

            // Indicators grid: total days, saved money, saved hours, life gain
            val start = previewStartTime ?: sharedPref.getLong(Constants.PREF_START_TIME, 0L)
            val now = System.currentTimeMillis()
            val elapsedMillis = if (start > 0L) now - start else 0L
            val elapsedDaysFloat = elapsedMillis / Constants.DAY_IN_MILLIS.toFloat()
            val weeks = elapsedDaysFloat / 7.0
            val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)
            val costVal = Constants.DrinkingSettings.getCostValue(selectedCost)
            val freqVal = Constants.DrinkingSettings.getFrequencyValue(selectedFrequency)
            val drinkHoursVal = Constants.DrinkingSettings.getDurationValue(selectedDuration)
            val savedMoney = weeks * freqVal * costVal
            val savedHours = weeks * freqVal * (drinkHoursVal + Constants.DrinkingSettings.HANGOVER_HOURS)
            val lifeGainDays = elapsedDaysFloat / 30.0

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard(title = stringResource(id = R.string.stat_total_days), value = String.format(Locale.getDefault(), "%.1f일", elapsedDaysFloat), color = colorResource(id = R.color.color_indicator_days), modifier = Modifier.weight(1f))
                    SmallStatCard(title = stringResource(id = R.string.indicator_title_saved_money), value = FormatUtils.formatMoney(context, savedMoney).replace(" ", ""), color = colorResource(id = R.color.color_indicator_money), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard(title = stringResource(id = R.string.indicator_title_saved_hours), value = FormatUtils.formatHoursValue(savedHours), color = colorResource(id = R.color.color_indicator_hours), modifier = Modifier.weight(1f))
                    // life gain: format like RunActivity
                    val formattedLifeGain = run {
                        val safe = if (lifeGainDays.isNaN() || lifeGainDays.isInfinite()) 0.0 else lifeGainDays.coerceAtLeast(0.0)
                        val dayPart = kotlin.math.floor(safe).toInt()
                        val frac = safe - dayPart
                        val hoursRaw = frac * 24.0
                        val hoursRounded = (kotlin.math.round(hoursRaw * 10.0) / 10.0)
                        if (dayPart == 0) String.format(Locale.getDefault(), "%.1f%s", hoursRounded, context.getString(R.string.unit_hour))
                        else String.format(Locale.getDefault(), "%d%s %.1f%s", dayPart, context.getString(R.string.unit_day), hoursRounded, context.getString(R.string.unit_hour))
                    }
                    SmallStatCard(title = stringResource(id = R.string.indicator_title_life_gain), value = formattedLifeGain, color = colorResource(id = R.color.color_indicator_life), modifier = Modifier.weight(1f))
                }
            }
        },
        bottomButton = {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(106.dp)) {
                        // 배경 원 (회색)
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(106.dp), color = Color(0xFFE0E0E0), strokeWidth = 4.dp, trackColor = Color.Transparent)
                        // 진행 상태 원 (빨간색)
                        if (showPressed) {
                            CircularProgressIndicator(progress = { showProgress }, modifier = Modifier.size(106.dp), color = Color(0xFFD32F2F), strokeWidth = 4.dp, trackColor = Color.Transparent)
                        }
                        // 중지 버튼
                        Card(
                            modifier = Modifier.size(96.dp).pointerInput(Unit) {
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
                                            // 완료 처리: 기록 저장 + 진행 상태 정리
                                            try {
                                                val start = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
                                                val actualDays = (((System.currentTimeMillis() - start) / Constants.DAY_IN_MILLIS)).toInt()
                                                saveCompletedRecord(
                                                    context = context,
                                                    startTime = start,
                                                    endTime = System.currentTimeMillis(),
                                                    targetDays = targetDays,
                                                    actualDays = actualDays
                                                )
                                                val editor = sharedPref.edit()
                                                editor.putBoolean(Constants.PREF_TIMER_COMPLETED, true)
                                                editor.remove(Constants.PREF_START_TIME)
                                                editor.apply()
                                            } catch (_: Throwable) { }
                                            onQuitConfirmed()
                                        }
                                    }
                                    waitForUpOrCancellation(); isPressed = false; job.cancel()
                                }
                            },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(id = R.string.cd_stop), tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                    // 취소 버튼 (즉시 이전 화면 복귀)
                    OutlinedButton(onClick = { onCancel() }) {
                        Text(text = stringResource(id = R.string.dialog_cancel))
                    }
                }
            }
        }
    )
}

private fun saveCompletedRecord(context: android.content.Context, startTime: Long, endTime: Long, targetDays: Float, actualDays: Int) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, android.content.Context.MODE_PRIVATE)
        val recordId = System.currentTimeMillis().toString()
        val isCompleted = actualDays >= targetDays
        val status = if (isCompleted) "완료" else "중지"
        val record = org.json.JSONObject().apply {
            put("id", recordId); put("startTime", startTime); put("endTime", endTime); put("targetDays", targetDays.toInt()); put("actualDays", actualDays); put("isCompleted", isCompleted); put("status", status); put("createdAt", System.currentTimeMillis())
        }
        val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val list = try { org.json.JSONArray(recordsJson) } catch (_: Exception) { org.json.JSONArray() }
        list.put(record)
        val editor = sharedPref.edit()
        editor.putString(Constants.PREF_SOBRIETY_RECORDS, list.toString())
        editor.apply()
    } catch (_: Exception) { }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
fun QuitScreenPreview() {
    // Use the fully hardcoded preview to avoid resource/runtime failures in the IDE renderer
    QuitScreenFullPreview_Hardcoded()
}

@Composable
private fun SmallStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = colorResource(id = R.color.color_stat_title_gray), textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900, name = "Quit Full (Hardcoded)")
@Composable
fun QuitScreenFullPreview_Hardcoded() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🤔", fontSize = 48.sp)
                Text(text = "정말 멈추시겠어요?", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Text(text = "지금까지 잘 해오셨는데...", fontSize = 14.sp, color = Color(0xFF666666))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStatCard(title = "총 금주 일수", value = "12.4일", color = Color(0xFF2F80ED), modifier = Modifier.weight(1f))
            SmallStatCard(title = "절약한 금액", value = "43,393원", color = Color(0xFFEB5757), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStatCard(title = "절약한 시간", value = "28.2시간", color = Color(0xFFF2994A), modifier = Modifier.weight(1f))
            SmallStatCard(title = "기대 수명+", value = "1일 0.3시간", color = Color(0xFF9B51E0), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(106.dp)) {
                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(106.dp), color = Color(0xFFE0E0E0), strokeWidth = 4.dp, trackColor = Color.Transparent)
                CircularProgressIndicator(progress = { 0.6f }, modifier = Modifier.size(106.dp), color = Color(0xFFD32F2F), strokeWidth = 4.dp, trackColor = Color.Transparent)
                Card(modifier = Modifier.size(96.dp), shape = CircleShape, colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)), elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("✕", color = Color.White, fontSize = 36.sp) }
                }
            }
            OutlinedButton(onClick = {}) { Text(text = "취소") }
        }
    }
}
