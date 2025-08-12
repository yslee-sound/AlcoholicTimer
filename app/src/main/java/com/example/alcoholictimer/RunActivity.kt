package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.Constants
import java.util.*
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

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
        Log.w("RunActivity", "startTime이 0이어서 현재 시간으로 강제 설정: $currentTimeMillis")
        currentTimeMillis
    } else {
        startTime
    }

    // 설정값 가져오기 (범주형 설정값)
    val selectedCost = sharedPref.getString("selected_cost", "중") ?: "중"
    val selectedFrequency = sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회"
    val selectedDuration = sharedPref.getString("selected_duration", "보통") ?: "보통"

    // 테스트 모드 설정 로드 및 적용 (레벨 계산용)
    val testModePrefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val currentTestMode = testModePrefs.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)
    Constants.updateTestMode(currentTestMode)

    // 실시간 시간 업데이트
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // 1초마다 업데이트
            currentTime = System.currentTimeMillis()
        }
    }

    // 경과 시간 계산 (항상 실제 시간 사용)
    val elapsedTime = if (actualStartTime > 0) currentTime - actualStartTime else 0L

    // 금주 진행은 항상 실제 시간으로 계산 (소수점 지원)
    val elapsedDaysFloat = (elapsedTime / Constants.DAY_IN_MILLIS.toFloat())
    val elapsedDays = elapsedDaysFloat.toInt()

    // 레벨 계산용 일수 (테스트 모드 적용)
    val levelDays = Constants.calculateLevelDays(elapsedTime)

    // 실제 경과 시간 계산 (시:분:초 표시용)
    val elapsedHours = ((elapsedTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedTime % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedTime % (60 * 1000)) / 1000).toInt()

    // 진행 중인 시간 포맷 (HH:MM:SS)
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

    // 디버깅 로그
    Log.d("RunActivity", "실제 경과일수: $elapsedDays, 레벨용 일수: $levelDays, 테스트모드: ${Constants.currentTestMode}")

    // 중앙 지표 순환 상태 (0: 일수, 1: 진행시간, 2: 레벨, 3: 금액, 4: 절약시간, 5: 수명) - 명세서 준수
    var currentIndicator by remember { mutableStateOf(0) }

    // 내부 매핑값 계산 (명세서 기준)
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
    Log.d("RunActivity", "목표까지 남은 시간: ${totalTargetMillis - elapsedTime}ms")
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
        Log.d("RunActivity", "남은 초수: ${targetSeconds - elapsedSeconds}초")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp)
    ) {
        // 상단 정보
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "목표: ${targetDays}일",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = getLevelName(levelDays), // 실제 일수 대신 레벨용 일수 사용
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // 현재 진행되고 있는 시간 표시 (HH:MM:SS 형식)
            Text(
                text = progressTimeText,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }

        // 중앙 메인 영역
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 현재 지표에 따른 제목 텍스트
            Text(
                text = when (currentIndicator) {
                    0 -> "금주 일수"
                    1 -> "진행 시간"
                    2 -> "현재 레벨"
                    3 -> "절약한 금액"
                    4 -> "절약한 시간"
                    5 -> "기대 수명"
                    else -> "금주 일수"
                },
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 중앙 메인 지표 (고정 크기 컨테이너)
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(120.dp), // 고정 높이 추가
                contentAlignment = Alignment.Center
            ) {
                // 메인 숫자 표시 (클릭 가능, 크기 고정)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            currentIndicator = (currentIndicator + 1) % 6
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentIndicator) {
                        0 -> {
                            // 금주 일수
                            Text(
                                text = if (elapsedHours > 0) "${elapsedDays}일 ${elapsedHours}시간" else "${elapsedDays}",
                                fontSize = 48.sp, // 크기 축소
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 2, // 최대 2줄
                                lineHeight = 52.sp
                            )
                        }
                        1 -> {
                            // 진행 시간 (시:분:초 형식)
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds),
                                fontSize = 42.sp, // 긴 텍스트이므로 더 작게
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 1,
                                lineHeight = 46.sp
                            )
                        }
                        2 -> {
                            // 현재 레벨 (테스트 모드 적용)
                            Text(
                                text = getLevelName(levelDays), // 실제 일수 대신 레벨용 일수 사용
                                fontSize = 36.sp, // 레벨명은 더 작게
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 2,
                                lineHeight = 40.sp
                            )
                        }
                        3 -> {
                            // 절약한 금액 (천단위 구분)
                            Text(
                                text = String.format(Locale.getDefault(), "%,d", savedMoney),
                                fontSize = 42.sp, // 숫자이므로 적당한 크기
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 1,
                                lineHeight = 46.sp
                            )
                        }
                        4 -> {
                            // 절약한 시간
                            Text(
                                text = "${savedHours}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 1,
                                lineHeight = 52.sp
                            )
                        }
                        5 -> {
                            // 기대 수명
                            Text(
                                text = "+${lifeGainDays}일",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 1,
                                lineHeight = 46.sp
                            )
                        }
                    }
                }
            }

            // 단위 표시 (모든 지표에 일관된 단위 표시)
            Text(
                text = when (currentIndicator) {
                    0 -> if (elapsedHours > 0) "" else "일"
                    1 -> "시간"  // 진행 시간에 단위 추가
                    2 -> "레벨"  // 현재 레벨에 단위 추가
                    3 -> "원"
                    4 -> "시간"
                    5 -> "일"
                    else -> "일"
                },
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 진행률 바
            ProgressIndicator(progress = progress)

            Spacer(modifier = Modifier.height(60.dp))

            // 중지 버튼 (StartActivity 스타일)
            StopButton(
                onStop = {
                    // QuitActivity로 이동 (중지 확인 화면)
                    val intent = Intent(context, QuitActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun ProgressIndicator(progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "진행률: ${(progress * 100).roundToInt()}%",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 디버깅용 추가 정보 표시
        Text(
            text = "Progress value: $progress",
            fontSize = 12.sp,
            color = Color.Red,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LinearProgressIndicator(
            progress = progress, // 람다 대신 직접 값 전달로 변경
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFFE0E0E0),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun StopButton(onStop: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(120.dp)
            .background(Color.Black, CircleShape)
            .clickable { onStop() }
    ) {
        Text(
            text = "■",
            fontSize = 48.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
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
        val message = if (isCompleted) "금주 목표를 달성했습니다!" else "금주 기록이 저장되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        // Log the exception for debugging purposes
        Log.e("RunActivity", "Error saving record", e)
        Toast.makeText(context, "기록 저장 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// 레벨명 함수 (기존 레벨 테이블 기준)
private fun getLevelName(days: Int): String {
    return when {
        days < 7 -> "시작"
        days < 30 -> "작심 7일"
        days < 90 -> "한 달 클리어"
        days < 365 -> "3개월 클리어"
        else -> "절제의 레전드"
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRunScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        RunScreen()
    }
}
