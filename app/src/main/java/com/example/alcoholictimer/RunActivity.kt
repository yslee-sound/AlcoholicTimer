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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.ui.StandardScreenWithBottomButton
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.animation.SizeTransform

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
    val activity = context as? RunActivity

    // SharedPreferences에서 데이터 가져오기
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val targetDays = sharedPref.getFloat("target_days", 30f)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    // 금주가 완전히 완료되었거나 아직 시작하지 않은 경우에만 시작 화면으로 이동
    val isPreview = LocalInspectionMode.current
    if (!isPreview && (timerCompleted || (startTime == 0L && !timerCompleted))) {
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

    // 시작 시간이 0인 경우 강제로 현재 시간으로 설정 (임시 해결책)
    val actualStartTime = if (startTime == 0L) {
        val currentTimeMillis = System.currentTimeMillis()
        // 프리뷰가 아닐 때만 SharedPreferences에 저장
        if (!isPreview) {
            sharedPref.edit().apply {
                putLong("start_time", currentTimeMillis)
                apply()
            }
            Log.w("RunActivity", "startTime이 0이어서 현재 시간으로 설정: $currentTimeMillis")
        }
        currentTimeMillis
    } else {
        startTime
    }

    // 설정값 가져오기 (Constants를 통해 안전하게 가져오기)
    val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)

    // 테스트 모드 설정 로드 및 적용 (레벨 ��산용)
    val testModePrefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val currentTestMode = testModePrefs.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)
    Constants.updateTestMode(currentTestMode)

    // 실시간 시간 업데이트
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 1초마다 업데이트
            currentTime = System.currentTimeMillis()
        }
    }

    // 경과 시간 계산 (항상 실제 시간 사용)
    val elapsedTime = remember(currentTime, actualStartTime) {
        if (actualStartTime > 0) currentTime - actualStartTime else 0L
    }

    // 금주 진행은 항상 실제 시간으로 계산 (소수점 지원)
    val elapsedDaysFloat = remember(elapsedTime) {
        (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
    }
    val elapsedDays = elapsedDaysFloat.toInt()

    // 레벨 계산용 일수 (테스트 모드 적용) - 과거 기록 + 현재 진행 시간 총합
    val levelDays = remember(elapsedTime) {
        calculateTotalLevelDays(context, elapsedTime)
    }

    // 레벨 정보를 실시간로 계산
    val currentLevelInfo = remember(levelDays) {
        LevelDefinitions.getLevelInfo(levelDays)
    }

    val currentLevelName = remember(levelDays) {
        LevelDefinitions.getLevelName(levelDays)
    }

    // 실�� 경과 시간 계산 (시:분:초 ���시용)
    val elapsedHours = ((elapsedTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedTime % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedTime % (60 * 1000)) / 1000).toInt()

    // 진행 중인 시간 포맷 (HH:MM) - 시간:분 단위로 변경
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d", elapsedHours, elapsedMinutes)

    // 중앙 지표 순환 상태 (0: 일수, 1: 진행시간, 2: 레벨, 3: 금액, 4: 절약시간, 5: 수명) - 명세서 준수
    // 앱 진행 중에는 사용자가 마지막으로 본 지표를 유지, 앱 재시작 시에는 항상 0(금주 일수)부터 시작
    var currentIndicator by remember {
        mutableStateOf(
            if (!isPreview) {
                // 실제 앱에서는 진행 중인 세션의 마지막 선택 지표를 복원
                sharedPref.getInt("current_indicator_${actualStartTime}", 0)
            } else {
                // 프리뷰에서는 항상 0부터 시작
                0
            }
        )
    }

    // 내부 매핑값 계산 (명��서 기준)
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

    val hangoverHoursVal = 5 // 기본 숙취 시간

    // 계산된 값들 (명세서 공식 적용) - 실시간 업데이트를 위해 remember로 감싸기
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
        ((elapsedDaysFloat / 30.0) * 1.0) // 30일→+1일 규칙, 소수점 유지
    }

    // 디버깅 로그
    Log.d("RunActivity", "실제 경과일수: $elapsedDays, 레벨용 일수: $levelDays, 테스트모드: ${Constants.currentTestMode}")

    // 절약한 시간/금액 디버깅 로그 추가
    Log.d("RunActivity", "=== 절약한 시간/금액 계산 ===")
    Log.d("RunActivity", "elapsedTime: ${elapsedTime}ms")
    Log.d("RunActivity", "elapsedDaysFloat: $elapsedDaysFloat")
    Log.d("RunActivity", "weeks: ${elapsedDaysFloat / 7.0}")
    Log.d("RunActivity", "freqVal: $freqVal")
    Log.d("RunActivity", "costVal: $costVal")
    Log.d("RunActivity", "drinkHoursVal: $drinkHoursVal")
    Log.d("RunActivity", "hangoverHoursVal: $hangoverHoursVal")
    Log.d("RunActivity", "savedMoney: $savedMoney")
    Log.d("RunActivity", "savedHours: $savedHours")
    Log.d("RunActivity", "lifeGainDays: $lifeGainDays")
    Log.d("RunActivity", "===============================")

    // 목표 달성 감지 및 자동 저장을 위한 상태 변수들 (먼저 선언)
    var hasCompleted by remember { mutableStateOf(false) }
    var shouldNavigateToDetail by remember { mutableStateOf(false) }

    // 진행률 계산 (실제 시간으로 고정) - 더 상세한 디버깅
    val totalTargetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
    val progress = if (totalTargetMillis > 0) {
        val rawProgress = elapsedTime.toFloat() / totalTargetMillis.toFloat()
        rawProgress.coerceAtMost(1.0f)
    } else 0f

    // 매우 상세한 디버깅 로그
    Log.d("RunActivity", "========== 초정밀 디버깅 ==========")
    Log.d("RunActivity", "startTime 원본: $startTime")
    Log.d("RunActivity", "actualStartTime: $actualStartTime")
    Log.d("RunActivity", "currentTime: $currentTime")
    Log.d("RunActivity", "elapsedTime: ${elapsedTime}ms")
    Log.d("RunActivity", "targetDays: $targetDays")
    Log.d("RunActivity", "Constants.DAY_IN_MILLIS: ${Constants.DAY_IN_MILLIS}")
    Log.d("RunActivity", "totalTargetMillis: ${totalTargetMillis}ms")
    Log.d("RunActivity", "목표까지 남은 시��: ${totalTargetMillis - elapsedTime}ms")
    Log.d("RunActivity", "rawProgress 계산: $elapsedTime / $totalTargetMillis = ${elapsedTime.toFloat() / totalTargetMillis.toFloat()}")
    Log.d("RunActivity", "최종 progress: $progress")
    Log.d("RunActivity", "progress 백분율: ${(progress * 100)}%")
    Log.d("RunActivity", "elapsedDaysFloat: $elapsedDaysFloat")
    Log.d("RunActivity", "목표 달성 여부: ${elapsedDaysFloat >= targetDays}")
    Log.d("RunActivity", "hasCompleted: $hasCompleted")

    // 0.0001일 테스트를 위한 특별 계산
    if (targetDays < 0.001f) {
        val targetSeconds = targetDays * 24 * 60 * 60
        val elapsedSeconds = elapsedTime / 1000.0
        Log.d("RunActivity", "=== 극소수점 테스트 모드 ===")
        Log.d("RunActivity", "목표 초수: ${targetSeconds}초")
        Log.d("RunActivity", "경과 초수: ${elapsedSeconds}초")
        Log.d("RunActivity", "남은 ���수: ${targetSeconds - elapsedSeconds}초")
        Log.d("RunActivity", "========================")
    }
    Log.d("RunActivity", "=====================================")

    // 목표 달성 감지 및 자동 저장 LaunchedEffect
    LaunchedEffect(elapsedDaysFloat, targetDays) {
        if (elapsedDaysFloat >= targetDays && targetDays > 0 && actualStartTime > 0 && !hasCompleted) {
            hasCompleted = true // 중복 실행 방지

            try {
                // 목표 달성 시 자동으로 기록 저장
                val isCompleted = elapsedDaysFloat >= targetDays && targetDays > 0 && actualStartTime > 0
                saveCompletedRecord(
                    context = context,
                    startTime = actualStartTime,
                    endTime = System.currentTimeMillis(),
                    targetDays = targetDays.toInt(),
                    actualDays = elapsedDays,
                    isCompleted = isCompleted
                )

                // SharedPreferences 초기화
                val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
                sharedPref.edit().apply {
                    remove("start_time")
                    putBoolean("timer_completed", true)
                    apply()
                }

                // 바로 DetailActivity로 이동
                shouldNavigateToDetail = true

            } catch (e: Exception) {
                // 오류 발생 시 로그 출력
                Toast.makeText(context, "목표 달성 처리 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // DetailActivity로 이동 처리
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

    // 모던한 그라데이�� 배경
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE3F2FD),
            Color(0xFFF1F8E9)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    // StandardScreenWithBottomButton 사용
    StandardScreenWithBottomButton(
        topContent = {
            // 상단 정보 카드
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
                    // 통계 그리드
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 목표일
                        RunStatisticItem(
                            title = "목표일",
                            value = "${targetDays.toInt()}일",
                            color = Color(0xFF8D6E63),
                            modifier = Modifier.weight(1f)
                        )

                        // 레벨
                        RunStatisticItem(
                            title = "Level",
                            value = currentLevelName.take(2),
                            color = currentLevelInfo.color,
                            modifier = Modifier.weight(1f)
                        )

                        // 진행 시간
                        RunStatisticItem(
                            title = "시간",
                            value = progressTimeText,
                            color = Color(0xFFE74C3C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 메인 지표 카드
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
                    savedMoney = savedMoney.toDouble(),
                    savedHours = savedHours.toDouble(),
                    lifeGainDays = lifeGainDays.toDouble(),
                    onIndicatorChange = { newIndicator ->
                        currentIndicator = newIndicator
                        if (!isPreview) {
                            sharedPref.edit().putInt("current_indicator_${actualStartTime}", newIndicator).apply()
                        }
                    }
                )
            }

            // 진행률 카드
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
            // 하단 버튼 영역 - 안전한 크기와 패딩으로 수정
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
                            putExtra("saved_money", savedMoney.toDouble())
                            putExtra("saved_hours", savedHours.toDouble())
                            putExtra("life_gain_days", lifeGainDays.toDouble())
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF636E72),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier, // modifier 파라미터 추가
    isLevel: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier // 기존 Modifier 대신 파라미터로 받은 modifier 사용
            .padding(horizontal = 4.dp)
            .width(100.dp) // 카드 너비 통일
    ) {
        // 값 텍스트 - 고정 높이 Box 사용
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = 1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp), // 모든 카드 동일한 높이
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp, // 모든 카드 동일한 크기
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold, // 모든 카드 동일한 굵기
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 라벨 - 고정 높이 Box 사용
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp), // 라벨 높이도 고정
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
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
    var scale by remember { mutableStateOf(1f) }
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
        // 중앙 정렬을 위해 세 요소를 한 Column에 배치
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 0.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 메인 값 (애니메이션 적용)
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
                                        text = String.format(Locale.getDefault(), "%,.1f", savedMoney),
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
                                        text = String.format(Locale.getDefault(), "%.1f", lifeGainDays),
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
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 0.dp, bottom = 0.dp)
            )
        }
    }
}

@Composable
fun ModernProgressIndicator(progress: Float) {
    // 깜박임 애니메이션을 위한 상태
    var isVisible by remember { mutableStateOf(true) }

    // 2초마다 깜박이는 효과
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 2초 대기
            isVisible = !isVisible
        }
    }

    // 투명도 애니메이션
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
        // 퍼센트 텍스트와 인디케이터
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${com.example.alcoholictimer.utils.PercentUtils.roundPercentFromRatio(progress.toDouble())}%", // 수정된 부분
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 깜박이는 인디케이터 점
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 진행률 바
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
    actualDays: Int,
    isCompleted: Boolean
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
            put("isCompleted", isCompleted)
            put("status", if (isCompleted) "완료" else "중지")
            put("createdAt", System.currentTimeMillis())
        }

        // 기존 기록들 가져오기
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val recordsList = try {
            JSONArray(recordsJson)
        } catch (e: Exception) {
            // Log the exception for debugging purposes
            Log.e("RunActivity", "Error parsing recordsJson", e)
            JSONArray()
        }

        // 새 기록 추가
        recordsList.put(record)

        // 저장
        sharedPref.edit().apply {
            putString("sobriety_records", recordsList.toString())
            apply()
        }

        // 사용자에게 알림
        val message = if (isCompleted) "금주 목표를 달성했습니다!" else "��주 기록이 저장되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        // Log the exception for debugging purposes
        Log.e("RunActivity", "Error saving record", e)
        Toast.makeText(context, "기록 저장 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// 레벨명 함수 (기존 ���벨 테이블 기준)
private fun getLevelName(days: Int): String {
    return LevelDefinitions.getLevelName(days)
}

// 과거 기록 + 현재 진행 시간을 모두 포함한 총 레벨 계산 함수
private fun calculateTotalLevelDays(context: Context, currentElapsedTime: Long): Int {
    try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        // 과거 완료된 기록들의 총 시간 계산
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val recordsList = try {
            JSONArray(recordsJson)
        } catch (e: Exception) {
            Log.e("RunActivity", "Error parsing records for level calculation", e)
            JSONArray()
        }

        var totalPastTime = 0L

        // 모든 과거 기록들의 실제 지속 시간을 합산
        for (i in 0 until recordsList.length()) {
            try {
                val record = recordsList.getJSONObject(i)
                val startTime = record.getLong("startTime")
                val endTime = record.getLong("endTime")
                val duration = endTime - startTime

                if (duration > 0) {
                    totalPastTime += duration
                }

                Log.d("RunActivity", "과거 기록 $i: ${duration}ms 추가, 누적: ${totalPastTime}ms")
            } catch (e: Exception) {
                Log.e("RunActivity", "Error processing record $i for level calculation", e)
            }
        }

        // 과거 기록 총합 + 현재 진행 시간 = 전체 누적 시간
        val totalTime = totalPastTime + currentElapsedTime

        // 테스트 모드를 적용하여 레벨 계산
        val levelDays = Constants.calculateLevelDays(totalTime)

        Log.d("RunActivity", "=== 레벨 계산 상세 ===")
        Log.d("RunActivity", "과거 기록 총 시간: ${totalPastTime}ms (${totalPastTime / Constants.DAY_IN_MILLIS.toFloat()}일)")
        Log.d("RunActivity", "현재 진행 시간: ${currentElapsedTime}ms (${currentElapsedTime / Constants.DAY_IN_MILLIS.toFloat()}일)")
        Log.d("RunActivity", "전체 누적 시간: ${totalTime}ms (${totalTime / Constants.DAY_IN_MILLIS.toFloat()}일)")
        Log.d("RunActivity", "테스트 모드 적용 레벨용 일수: $levelDays")
        Log.d("RunActivity", "최종 레벨: ${LevelDefinitions.getLevelName(levelDays)}")
        Log.d("RunActivity", "=====================")

        return levelDays

    } catch (e: Exception) {
        Log.e("RunActivity", "Error calculating total level days", e)
        // 오류 발생 시 현재 진행 시간만으로 계산
        return Constants.calculateLevelDays(currentElapsedTime)
    }
}

@Preview(showBackground = true)
@Composable
fun RunScreenPreview() {
    RunScreen()
}
