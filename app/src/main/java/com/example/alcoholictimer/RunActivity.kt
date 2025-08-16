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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        // SharedPreferences에 저장
        sharedPref.edit().apply {
            putLong("start_time", currentTimeMillis)
            apply()
        }
        Log.w("RunActivity", "startTime이 0이어서 현재 시간으로 강����������������������� 설정: $currentTimeMillis")
        currentTimeMillis
    } else {
        startTime
    }

    // ���정��� 가져오기 (범주형 설정���)
    val selectedCost = sharedPref.getString("selected_cost", "중") ?: "중"
    val selectedFrequency = sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회"
    val selectedDuration = sharedPref.getString("selected_duration", "보통") ?: "보통"

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

    // 경과 시간 계산 (항상 ���제 시간 사용)
    val elapsedTime = if (actualStartTime > 0) currentTime - actualStartTime else 0L

    // 금주 진행은 항상 실제 시간으로 계산 (소수점 지원)
    val elapsedDaysFloat = (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
    val elapsedDays = elapsedDaysFloat.toInt()

    // 레벨 계산용 일수 (테스트 모드 적용)
    val levelDays = Constants.calculateLevelDays(elapsedTime)

    // 실�� 경과 시간 계산 (시:분:초 ���시용)
    val elapsedHours = ((elapsedTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedTime % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedTime % (60 * 1000)) / 1000).toInt()

    // 진행 중인 시간 포맷 (HH:MM:SS)
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

    // 디버깅 로그
    Log.d("RunActivity", "실제 경과일수: $elapsedDays, 레벨용 일수: $levelDays, 테스트모드: ${Constants.currentTestMode}")

    // 중앙 지표 순환 상태 (0: 일수, 1: 진���시���, 2: 레벨, 3: 금액, 4: 절약시간, 5: 수명) - 명세서 준수
    var currentIndicator by remember { mutableStateOf(0) }

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

    // 계산된 값들 (명세서 공식 적용)
    val weeks = elapsedDays / 7.0
    val savedMoney = (weeks * freqVal * costVal).roundToInt()
    val savedHours = (weeks * freqVal * (drinkHoursVal + hangoverHoursVal)).roundToInt()
    val lifeGainDays = ((elapsedDays / 30.0) * 1.0).roundToInt() // 30일→+1일 규칙

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 정보 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp), // 패딩 약간 증가
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // StatCard들을 균등하게 배치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top // 상단 정렬로 변경
                    ) {
                        // 목표일 카드
                        StatCard(
                            value = "$targetDays",
                            label = "목표일",
                            color = Color(0xFF2196F3)
                        )

                        // 레벨 카드
                        StatCard(
                            value = getLevelName(elapsedDays),
                            label = "Level",
                            color = LevelDefinitions.getLevelInfo(elapsedDays).color,
                            isLevel = true
                        )

                        // 진행 시간 카드
                        StatCard(
                            value = progressTimeText,
                            label = "시간",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 메인 지표 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
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
                    onIndicatorChange = { newIndicator -> currentIndicator = newIndicator }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 진행률 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "진행률",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ModernProgressIndicator(progress = progress)
                }
            }
        }

        // 모던한 중지 버튼 (하단 고정)
        ModernStopButton(
            onStop = {
                val intent = Intent(context, QuitActivity::class.java)
                context.startActivity(intent)
            }
        )
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    isLevel: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
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
    savedMoney: Int,
    savedHours: Int,
    lifeGainDays: Int,
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
            .padding(horizontal = 24.dp, vertical = 12.dp), // 패딩 축소
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // 중앙 정렬로 변경
    ) {
        // 지표 제목
        Text(
            text = when (currentIndicator) {
                0 -> "금주 일수"
                1 -> "진행 시간"
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
        // 메인 값 (애니메이션 적용) - 제목 바로 아래로 이동
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
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
                                "${years}년 ${remainingDays}일"
                            } else {
                                "${elapsedDays}일"
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    fontSize = 42.sp,
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
                                    fontSize = 42.sp,
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
                                    text = String.format(Locale.getDefault(), "%,d원", savedMoney),
                                    fontSize = 42.sp,
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
                                    text = "${savedHours}시간",
                                    fontSize = 42.sp,
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
                                    text = "${lifeGainDays}일",
                                    fontSize = 42.sp,
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
        // 터치 안내
        Text(
            text = "탭하여 다른 지표 보기",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(top = 8.dp, bottom = 0.dp) // 불필요한 ��백 제거
        )
    }
}

@Composable
fun ModernProgressIndicator(progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 퍼센트 텍스트
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
            .size(80.dp),
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
                modifier = Modifier.size(32.dp)
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

@Preview(showBackground = true)
@Composable
fun RunScreenAuto() {
    RunScreen()
}

// Preview용 가짜 데이터�� 사용하는 RunScreen 컴포넌트
@Composable
fun RunScreenPreview(
    targetDays: Float = 30f,
    elapsedDays: Int = 7,
    elapsedHours: Int = 15,
    elapsedMinutes: Int = 30,
    elapsedSeconds: Int = 45,
    progress: Float = 0.23f
) {
    // 가짜 계산값들
    val savedMoney = 280000 // 7일 기준 예시값
    val savedHours = 63 // 7일 기준 예시값
    val lifeGainDays = 0 // 7일이므로 아직 0

    var currentIndicator by remember { mutableStateOf(0) }
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

    // 모던한 그라데이션 배경
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE3F2FD),
            Color(0xFFF1F8E9)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 정보 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp), // 패딩 약간 증가
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // StatCard들을 균등하게 배치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top // 상단 정렬로 변경
                    ) {
                        // 목표일 카드
                        StatCard(
                            value = "$targetDays",
                            label = "목표일",
                            color = Color(0xFF2196F3)
                        )

                        // 레벨 카드
                        StatCard(
                            value = "새싹",
                            label = "Level",
                            color = Color(0xFF4CAF50),
                            isLevel = true
                        )

                        // 진행 ���간 카드
                        StatCard(
                            value = progressTimeText,
                            label = "시간",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 메인 지표 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
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
                    onIndicatorChange = { newIndicator -> currentIndicator = newIndicator }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    Text(
                        text = "진행률",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ModernProgressIndicator(progress = progress)
                }
            }
        }

        // 모던한 중지 버튼 (하단 고정)
        ModernStopButton(
            onStop = { /* Preview에서는 동작 안함 */ }
        )
    }
}

@Preview(
    showBackground = true,
    name = "RunScreen - 시작 단계",
    widthDp = 360,
    heightDp = 800
)
@Composable
fun RunScreenStartPreview() {
    RunScreenPreview(
        targetDays = 30f,
        elapsedDays = 1,
        elapsedHours = 8,
        elapsedMinutes = 30,
        elapsedSeconds = 15,
        progress = 0.03f
    )
}

@Preview(
    showBackground = true,
    name = "RunScreen - 진행 중",
    widthDp = 360,
    heightDp = 800
)
@Composable
fun RunScreenProgressPreview() {
    RunScreenPreview(
        targetDays = 30f,
        elapsedDays = 15,
        elapsedHours = 12,
        elapsedMinutes = 45,
        elapsedSeconds = 30,
        progress = 0.5f
    )
}

@Preview(
    showBackground = true,
    name = "RunScreen - 거의 완료",
    widthDp = 360,
    heightDp = 800
)
@Composable
fun RunScreenNearCompletePreview() {
    RunScreenPreview(
        targetDays = 30f,
        elapsedDays = 28,
        elapsedHours = 6,
        elapsedMinutes = 20,
        elapsedSeconds = 45,
        progress = 0.93f
    )
}

@Preview(
    showBackground = true,
    name = "RunScreen - 다크 모드",
    widthDp = 360,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun RunScreenDarkPreview() {
    RunScreenPreview(
        targetDays = 30f,
        elapsedDays = 7,
        elapsedHours = 15,
        elapsedMinutes = 30,
        elapsedSeconds = 45,
        progress = 0.23f
    )
}

@Preview(
    showBackground = true,
    name = "RunScreen - 큰 폰트",
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.5f
)
@Composable
fun RunScreenLargeFontPreview() {
    RunScreenPreview(
        targetDays = 30f,
        elapsedDays = 7,
        elapsedHours = 15,
        elapsedMinutes = 30,
        elapsedSeconds = 45,
        progress = 0.23f
    )
}

// 개별 컴포넌트 Preview들
@Preview(showBackground = true, name = "StatCard Preview")
@Composable
fun StatCardPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly // SpaceEvenly로 변경
    ) {
        StatCard(
            value = "30.0",
            label = "목표일",
            color = Color(0xFF2196F3)
        )
        StatCard(
            value = "새싹",
            label = "Level",
            color = Color(0xFF4CAF50),
            isLevel = true
        )
        StatCard(
            value = "15:30:45",
            label = "시간",
            color = Color(0xFF4CAF50)
        )
    }
}

@Preview(showBackground = true, name = "ModernProgressIndicator Preview")
@Composable
fun ModernProgressIndicatorPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernProgressIndicator(progress = 0.0f)
        ModernProgressIndicator(progress = 0.25f)
        ModernProgressIndicator(progress = 0.5f)
        ModernProgressIndicator(progress = 0.75f)
        ModernProgressIndicator(progress = 1.0f)
    }
}

@Preview(showBackground = true, name = "ModernStopButton Preview")
@Composable
fun ModernStopButtonPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernStopButton(onStop = {})
    }
}

@Preview(
    showBackground = true,
    name = "MainIndicatorCard Preview",
    widthDp = 360,
    heightDp = 300
)
@Composable
fun MainIndicatorCardPreview() {
    var currentIndicator by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        MainIndicatorCard(
            currentIndicator = currentIndicator,
            elapsedDays = 15,
            elapsedHours = 12,
            elapsedMinutes = 30,
            elapsedSeconds = 45,
            savedMoney = 600000,
            savedHours = 135,
            lifeGainDays = 0,
            onIndicatorChange = { newIndicator -> currentIndicator = newIndicator }
        )
    }
}
