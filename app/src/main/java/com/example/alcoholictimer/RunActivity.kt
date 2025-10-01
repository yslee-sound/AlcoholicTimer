package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.ui.StandardScreenWithBottomButton
import kotlinx.coroutines.delay
import java.util.*
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.animation.SizeTransform
import androidx.core.content.edit

class RunActivity : BaseActivity() {

    override fun getScreenTitle(): String = "금주 진행"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RunScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen() {
    val context = LocalContext.current

    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val targetDays = sharedPref.getFloat("target_days", 30f)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    val isPreview = LocalInspectionMode.current
    // 단순화: timerCompleted || (startTime == 0L && !timerCompleted) -> timerCompleted || startTime == 0L
    if (!isPreview && (timerCompleted || startTime == 0L)) {
        LaunchedEffect(Unit) {
            val intent = Intent(context, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
            if (context is RunActivity) {
                context.finish()
            }
        }
        return
    }

    val actualStartTime = if (startTime == 0L) {
        val currentTimeMillis = System.currentTimeMillis()
        // preview에서만 도달 가능한 경로이므로 저장 사이드이펙트 제거
        currentTimeMillis
    } else {
        startTime
    }

    val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)

    val testModePrefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val currentTestMode = testModePrefs.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)
    Constants.updateTestMode(currentTestMode)

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val elapsedTime = remember(currentTime, actualStartTime) {
        if (actualStartTime > 0) currentTime - actualStartTime else 0L
    }

    val elapsedDaysFloat = remember(elapsedTime) {
        (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
    }
    val elapsedDays = elapsedDaysFloat.toInt()

    val levelDays: Int = remember(elapsedTime) {
        calculateTotalLevelDays(context, elapsedTime)
    }

    val currentLevelInfo = remember(levelDays) {
        LevelDefinitions.getLevelInfo(levelDays)
    }

    val currentLevelName = remember(levelDays) {
        LevelDefinitions.getLevelName(levelDays)
    }

    val elapsedHours = ((elapsedTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedTime % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedTime % (60 * 1000)) / 1000).toInt()

    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d", elapsedHours, elapsedMinutes)

    var currentIndicator by remember {
        mutableIntStateOf(
            if (!isPreview) {
                sharedPref.getInt("current_indicator_${actualStartTime}", 0)
            } else {
                0
            }
        )
    }

    val costVal = when(selectedCost) {
        "저" -> 10000
        "중" -> 40000
        "고" -> 70000
        else -> 40000
    }

    val freqVal = when(selectedFrequency) {
        "주 1회 이하" -> 1.0
        "주 2~3회" -> 2.5
        "주 4회 이상" -> 5.0
        else -> 2.5
    }

    val drinkHoursVal = when(selectedDuration) {
        "짧음" -> 2
        "보통" -> 4
        "김" -> 6
        else -> 4
    }

    val hangoverHoursVal = 5

    val savedMoney = remember(elapsedTime) {
        val elapsedDaysFloat = (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
        val weeks = elapsedDaysFloat / 7.0
        (weeks * freqVal * costVal)
    }

    val savedHours = remember(elapsedTime) {
        val elapsedDaysFloat = (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
        val weeks = elapsedDaysFloat / 7.0
        (weeks * freqVal * (drinkHoursVal + hangoverHoursVal))
    }

    val lifeGainDays = remember(elapsedTime) {
        val elapsedDaysFloat = (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
        ((elapsedDaysFloat / 30.0) * 1.0)
    }

    Log.d("RunActivity", "실제 경과일수: $elapsedDays, 레벨용 일수: $levelDays, 테스트모드: ${Constants.currentTestMode}")

    val totalTargetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
    val progress = if (totalTargetMillis > 0) {
        val rawProgress = elapsedTime.toFloat() / totalTargetMillis.toFloat()
        rawProgress.coerceAtMost(1.0f)
    } else 0f

    var hasCompleted by remember { mutableStateOf(false) }
    var shouldNavigateToDetail by remember { mutableStateOf(false) }

    if (targetDays < 0.001f) {
        val targetSeconds = targetDays * 24 * 60 * 60
        val elapsedSeconds = elapsedTime / 1000.0
        Log.d("RunActivity", "=== 극소수점 테스트 모드 ===")
        Log.d("RunActivity", "목표 초수: ${targetSeconds}초")
        Log.d("RunActivity", "경과 초수: ${elapsedSeconds}초")
        Log.d("RunActivity", "남은 초수: ${targetSeconds - elapsedSeconds}초")
    }

    LaunchedEffect(elapsedDaysFloat, targetDays) {
        if (elapsedDaysFloat >= targetDays && targetDays > 0 && actualStartTime > 0 && !hasCompleted) {
            hasCompleted = true

            try {
                // 블록 내부에서 항상 true가 되는 조건들을 제거하고 명시적으로 완료 처리
                saveCompletedRecord(
                    context = context,
                    startTime = actualStartTime,
                    endTime = System.currentTimeMillis(),
                    targetDays = targetDays.toInt(),
                    actualDays = elapsedDays
                )

                val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
                sharedPref.edit {
                    remove("start_time")
                    putBoolean("timer_completed", true)
                }

                shouldNavigateToDetail = true

            } catch (e: Exception) {
                Toast.makeText(context, "목표 달성 처리 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(shouldNavigateToDetail) {
        if (shouldNavigateToDetail) {
            DetailActivity.start(
                context = context,
                startTime = actualStartTime,
                endTime = System.currentTimeMillis(),
                targetDays = targetDays,
                actualDays = elapsedDays,
                isCompleted = true
            )
            (context as? RunActivity)?.finish()
        }
    }

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RunStatisticItem(
                            title = "목표일",
                            value = "${targetDays.toInt()}일",
                            color = Color(0xFF8D6E63),
                            modifier = Modifier.weight(1f)
                        )

                        RunStatisticItem(
                            title = "Level",
                            value = currentLevelName.take(2),
                            color = currentLevelInfo.color,
                            modifier = Modifier.weight(1f)
                        )

                        RunStatisticItem(
                            title = "시간",
                            value = progressTimeText,
                            color = Color(0xFFE74C3C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .wrapContentSize(Alignment.Center),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                MainIndicatorCard(
                    currentIndicator = currentIndicator,
                    elapsedDays = elapsedDays,
                    elapsedHours = elapsedHours,
                    elapsedMinutes = elapsedMinutes,
                    elapsedSeconds = elapsedSeconds,
                    savedMoney = savedMoney,
                    savedHours = savedHours,
                    lifeGainDays = lifeGainDays,
                    onIndicatorChange = { newIndicator ->
                        currentIndicator = newIndicator
                        if (!isPreview) {
                            sharedPref.edit { putInt("current_indicator_${actualStartTime}", newIndicator) }
                        }
                    }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModernProgressIndicator(progress = progress)
                }
            }
        },
        bottomButton = {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .wrapContentSize(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                ModernStopButton(
                    onStop = {
                        val intent = Intent(context, QuitActivity::class.java).apply {
                            putExtra("elapsed_days", elapsedDays)
                            putExtra("elapsed_hours", elapsedHours)
                            putExtra("elapsed_minutes", elapsedMinutes)
                            putExtra("saved_money", savedMoney)
                            putExtra("saved_hours", savedHours)
                            putExtra("life_gain_days", lifeGainDays)
                            putExtra("level_name", currentLevelName)
                            putExtra("level_color", currentLevelInfo.color.value.toLong())
                            putExtra("quit_timestamp", System.currentTimeMillis())
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    )
}

@Composable
private fun RunStatisticItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF636E72),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MainIndicatorCard(
    currentIndicator: Int,
    elapsedDays: Int,
    elapsedHours: Int,
    elapsedMinutes: Int,
    elapsedSeconds: Int,
    savedMoney: Double,
    savedHours: Double,
    lifeGainDays: Double,
    onIndicatorChange: (Int) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var isAnimating by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(
            durationMillis = 150,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        )
    )

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            scale = 0.85f
            delay(150)
            scale = 1f
            delay(150)
            onIndicatorChange((currentIndicator + 1) % 5)
            isAnimating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (currentIndicator) {
                    0 -> "금주 일수"
                    1 -> "시간"
                    2 -> "절약한 금액"
                    3 -> "절약한 시간"
                    4 -> "기대 수명+"
                    else -> "금주 일수"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF666666)),
                modifier = Modifier.padding(bottom = 0.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .scale(animatedScale)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (!isAnimating) {
                            isAnimating = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentIndicator,
                    transitionSpec = {
                        slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { height -> height }
                        ).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(300),
                                targetOffsetY = { height -> -height }
                            )
                        ).using(
                            SizeTransform(clip = true)
                        )
                    },
                    label = "indicator_animation"
                ) { indicator ->
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = 1f)) {
                        when (indicator) {
                            0 -> {
                                val displayText = if (elapsedDays >= 365) {
                                    val years = elapsedDays / 365
                                    val remainingDays = elapsedDays % 365
                                    "${years} ${remainingDays}"
                                } else {
                                    "${elapsedDays}"
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayText,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            1 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF388E3C),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            2 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%,.0f원", savedMoney),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE91E63),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            3 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f", savedHours),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            4 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f", lifeGainDays),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9C27B0),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "탭하여 다른 지표 보기",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF999999)),
                modifier = Modifier.padding(top = 0.dp, bottom = 0.dp)
            )
        }
    }
}

@Composable
fun ModernProgressIndicator(progress: Float) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            isVisible = !isVisible
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "indicator_blink"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = com.example.alcoholictimer.utils.PercentUtils.roundPercentFromRatio(progress.toDouble()).toString() + "%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = alpha))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFFE8F5E8),
            strokeCap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernStopButton(onStop: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onStop,
        modifier = modifier
            .size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE53935)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "정지",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// 금주 기록 저장 함수
private fun saveCompletedRecord(
    context: Context,
    startTime: Long,
    endTime: Long,
    targetDays: Int,
    actualDays: Int
) {
    try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        // 기록 ID 생성
        val recordId = System.currentTimeMillis().toString()

        // 기록 데이터 생성 (JSONObject 사용)
        val record = JSONObject().apply {
            put("id", recordId)
            put("startTime", startTime)
            put("endTime", endTime)
            put("targetDays", targetDays)
            put("actualDays", actualDays)
            put("isCompleted", true)
            put("status", "완료")
            put("createdAt", System.currentTimeMillis())
        }

        // 기존 기록들 가져오기
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val recordsList = try {
            JSONArray(recordsJson)
        } catch (e: Exception) {
            Log.e("RunActivity", "Error parsing recordsJson", e)
            JSONArray()
        }

        // 새 기록 추가
        recordsList.put(record)

        // 저장
        sharedPref.edit { putString("sobriety_records", recordsList.toString()) }

        // 사용자에게 알림
        val message = "금주 목표를 달성했습니다!"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        Log.e("RunActivity", "Error saving record", e)
        Toast.makeText(context, "기록 저장 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// 과거 기록 + 현재 진행 시간을 모두 포함한 총 레벨 계산 함수
private fun calculateTotalLevelDays(context: Context, currentElapsedTime: Long): Int {
    return try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val recordsList = try {
            JSONArray(recordsJson)
        } catch (e: Exception) {
            Log.e("RunActivity", "Error parsing records for level calculation", e)
            JSONArray()
        }

        var totalPastTime = 0L
        for (i in 0 until recordsList.length()) {
            try {
                val obj = recordsList.getJSONObject(i)
                val s = obj.optLong("startTime", -1L)
                val e = obj.optLong("endTime", -1L)
                if (s > 0 && e > s) {
                    totalPastTime += (e - s)
                }
            } catch (_: Exception) {
            }
        }
        val totalMillis = totalPastTime + currentElapsedTime
        (totalMillis / Constants.DAY_IN_MILLIS).toInt()
    } catch (_: Exception) {
        (currentElapsedTime / Constants.DAY_IN_MILLIS).toInt()
    }
}
