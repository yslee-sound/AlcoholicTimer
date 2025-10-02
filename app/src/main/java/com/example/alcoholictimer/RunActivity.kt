package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.edit
import java.util.Locale
import com.example.alcoholictimer.ui.StandardScreenWithBottomButton
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.LevelDefinitions
import com.example.alcoholictimer.utils.FormatUtils
import kotlinx.coroutines.delay

class RunActivity : BaseActivity() {

    override fun getScreenTitle(): String = getString(R.string.run_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RunScreen()
            }
        }
    }
}

@Composable
private fun RunScreen() {
    val context = LocalContext.current

    // 현재 진행 상태 로드
    val sp = remember { context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE) }
    val startTime = remember { sp.getLong(Constants.PREF_START_TIME, 0L) }
    val targetDays = remember { sp.getFloat(Constants.PREF_TARGET_DAYS, 30f) }
    val timerCompleted = remember { sp.getBoolean(Constants.PREF_TIMER_COMPLETED, false) }

    // 잘못된 상태면 시작 화면으로 이동
    LaunchedEffect(startTime, timerCompleted) {
        if (timerCompleted || startTime == 0L) {
            context.startActivity(Intent(context, StartActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            (context as? RunActivity)?.finish()
        }
    }

    // 진행 업데이트용 타이머
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
    val elapsedDays = remember(elapsedDaysFloat) { elapsedDaysFloat.toInt() }

    // 레벨 계산(항상 일 단위)
    val levelDays = remember(elapsedMillis) { Constants.calculateLevelDays(elapsedMillis) }
    val levelInfo = remember(levelDays) { LevelDefinitions.getLevelInfo(levelDays) }
    val levelName = levelInfo.name

    // 경과 시:시:초 텍스트
    val elapsedHours = ((elapsedMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedMillis % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedMillis % (60 * 1000)) / 1000).toInt()
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

    // 사용자 설정 기반 통계 (절약 금액/시간/수명)
    val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)
    val costVal = when (selectedCost) {
        "저" -> 10000
        "중" -> 40000
        "고" -> 70000
        else -> 40000
    }
    val freqVal = when (selectedFrequency) {
        "주 1회 이하" -> 1.0
        "주 2~3회" -> 2.5
        "주 4회 이상" -> 5.0
        else -> 2.5
    }
    val drinkHoursVal = when (selectedDuration) {
        "짧음" -> 2
        "보통" -> 4
        "김" -> 6
        else -> 4
    }
    val weeks = elapsedDaysFloat / 7.0
    val savedMoney = remember(weeks, freqVal, costVal) { weeks * freqVal * costVal }
    val savedHours = remember(weeks, freqVal, drinkHoursVal) { weeks * freqVal * (drinkHoursVal + Constants.DEFAULT_HANGOVER_HOURS) }
    val lifeGainDays = remember(elapsedDaysFloat) { elapsedDaysFloat / 30.0 }

    // 목표 진행률
    val totalTargetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
    val progress = remember(elapsedMillis, totalTargetMillis) {
        if (totalTargetMillis > 0) (elapsedMillis.toFloat() / totalTargetMillis).coerceIn(0f, 1f) else 0f
    }

    // 현재 지표 유지 (세션별 키)
    val indicatorKey = remember(startTime) { Constants.keyCurrentIndicator(startTime) }
    var currentIndicator by remember {
        mutableIntStateOf(sp.getInt(indicatorKey, 0))
    }

    fun toggleIndicator() {
        val next = (currentIndicator + 1) % 5
        currentIndicator = next
        sp.edit { putInt(indicatorKey, next) }
    }

    // 목표 달성 처리 + 기록 저장 + 상세 화면 이동
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
                sp.edit {
                    remove(Constants.PREF_START_TIME)
                    putBoolean(Constants.PREF_TIMER_COMPLETED, true)
                }
                hasCompleted = true
                Toast.makeText(context, context.getString(R.string.toast_goal_completed), Toast.LENGTH_SHORT).show()
                DetailActivity.start(
                    context = context,
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    targetDays = targetDays,
                    actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt(),
                    isCompleted = true
                )
                (context as? RunActivity)?.finish()
            } catch (_: Exception) { /* 토스트는 생략 */ }
        }
    }

    StandardScreenWithBottomButton(
        topContent = {
            // 상단 통계(3개)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RunStatChip(
                            title = stringResource(id = R.string.stat_goal_days),
                            value = "${targetDays.toInt()}일",
                            color = colorResource(id = R.color.color_stat_goal),
                            modifier = Modifier.weight(1f)
                        )
                        RunStatChip(
                            title = stringResource(id = R.string.stat_level),
                            value = levelName.take(6),
                            color = levelInfo.color,
                            modifier = Modifier.weight(1f)
                        )
                        RunStatChip(
                            title = stringResource(id = R.string.stat_time),
                            value = progressTimeText,
                            color = colorResource(id = R.color.color_stat_time),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 메인 지표 카드 (탭하여 전환) - 고정 높이
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clickable { toggleIndicator() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val labelBoxH = 24.dp
                    val valueBoxH = 66.dp // 44.dp -> 66.dp (1.5배로 증가)
                    val hintBoxH = 20.dp
                    val gapSmall = 6.dp
                    val gapMedium = 8.dp

                    val (label, valueText, valueColor) = when (currentIndicator) {
                        0 -> Triple(
                            stringResource(id = R.string.indicator_title_days),
                            String.format(Locale.getDefault(), "%.1f", elapsedDaysFloat),
                            colorResource(id = R.color.color_indicator_days)
                        )
                        1 -> Triple(
                            stringResource(id = R.string.indicator_title_time),
                            progressTimeText,
                            colorResource(id = R.color.color_indicator_time)
                        )
                        2 -> Triple(
                            stringResource(id = R.string.indicator_title_saved_money),
                            String.format(Locale.getDefault(), "%,.0f원", savedMoney).replace(" ", ""),
                            colorResource(id = R.color.color_indicator_money)
                        )
                        3 -> Triple(
                            stringResource(id = R.string.indicator_title_saved_hours),
                            String.format(Locale.getDefault(), "%.1f", savedHours),
                            colorResource(id = R.color.color_indicator_hours)
                        )
                        else -> Triple(
                            stringResource(id = R.string.indicator_title_life_gain),
                            FormatUtils.daysToDayHourString(lifeGainDays, 2),
                            colorResource(id = R.color.color_indicator_life)
                        )
                    }

                    val noPad = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 라벨 슬롯
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(labelBoxH),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium
                                    .copy(color = colorResource(id = R.color.color_indicator_label_gray))
                                    .merge(noPad)
                            )
                        }
                        Spacer(modifier = Modifier.height(gapSmall))
                        // 값 슬롯
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(valueBoxH),
                            contentAlignment = Alignment.Center
                        ) {
                            val baseStyle = MaterialTheme.typography.headlineMedium
                            Text(
                                text = valueText,
                                style = baseStyle
                                    .copy(
                                        fontWeight = FontWeight.Bold,
                                        color = valueColor,
                                        // 중앙 숫자만 1.5배 크게
                                        fontSize = (baseStyle.fontSize.value * 1.5f).sp
                                    )
                                    .merge(noPad),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(gapMedium))
                        // 힌트 슬롯
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hintBoxH),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.tap_to_switch_indicator),
                                style = MaterialTheme.typography.labelMedium
                                    .copy(color = colorResource(id = R.color.color_hint_gray))
                                    .merge(noPad)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 진행률 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ModernProgressIndicatorSimple(progress = progress)
                }
            }
        },
        bottomButton = {
            ModernStopButtonSimple(onStop = {
                val intent = Intent(context, QuitActivity::class.java).apply {
                    putExtra("elapsed_days", elapsedDays)
                    putExtra("elapsed_hours", elapsedHours)
                    putExtra("elapsed_minutes", elapsedMinutes)
                    putExtra("saved_money", savedMoney)
                    putExtra("saved_hours", savedHours)
                    putExtra("life_gain_days", lifeGainDays)
                    putExtra("level_name", levelName)
                    putExtra("level_color", levelInfo.color.value.toLong())
                    putExtra("quit_timestamp", System.currentTimeMillis())
                }
                context.startActivity(intent)
            })
        }
    )
}

@Composable
private fun ModernProgressIndicatorSimple(progress: Float) {
    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            blink = !blink
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (blink) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "blink"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = (progress * 100).toInt().coerceIn(0, 100).toString() + "%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.color_progress_primary)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.color_progress_primary).copy(alpha = alpha))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = colorResource(id = R.color.color_progress_primary),
            trackColor = colorResource(id = R.color.color_progress_track),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
private fun RunStatChip(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = colorResource(id = R.color.color_stat_title_gray), maxLines = 1)
        }
    }
}

private fun saveCompletedRecord(
    context: Context,
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int
) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val recordId = System.currentTimeMillis().toString()
        val isCompleted = actualDays >= targetDays
        val status = if (isCompleted) "완료" else "중지"
        val record = org.json.JSONObject().apply {
            put("id", recordId)
            put("startTime", startTime)
            put("endTime", endTime)
            put("targetDays", targetDays.toInt())
            put("actualDays", actualDays)
            put("isCompleted", isCompleted)
            put("status", status)
            put("createdAt", System.currentTimeMillis())
        }
        val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val list = try { org.json.JSONArray(recordsJson) } catch (_: Exception) { org.json.JSONArray() }
        list.put(record)
        sharedPref.edit { putString(Constants.PREF_SOBRIETY_RECORDS, list.toString()) }
    } catch (_: Exception) {
        // 무음 처리
    }
}
