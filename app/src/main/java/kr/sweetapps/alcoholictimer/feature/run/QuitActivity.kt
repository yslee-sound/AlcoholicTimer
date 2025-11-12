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
import kr.sweetapps.alcoholictimer.core.ui.LayoutConstants
import kr.sweetapps.alcoholictimer.core.util.Constants
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.AppElevation

@Composable
fun QuitScreenComposable(
    onQuitConfirmed: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, android.content.Context.MODE_PRIVATE)
    val targetDays = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)

    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LayoutConstants.CARD_CORNER_RADIUS),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(LayoutConstants.CARD_PADDING),
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
                        if (isPressed) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(106.dp), color = Color(0xFFD32F2F), strokeWidth = 4.dp, trackColor = Color.Transparent)
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

@Preview(showBackground = true)
@Composable
fun QuitScreenPreview() { QuitScreenComposable({}, {}) }
