package com.example.alcoholictimer

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.roundToInt

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
    val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val targetDays = sharedPref.getInt("target_days", 30)

    // 설정값 가져오기 (범주형 설정값)
    val selectedCost = sharedPref.getString("selected_cost", "중") ?: "중"
    val selectedFrequency = sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회"
    val selectedDuration = sharedPref.getString("selected_duration", "보통") ?: "보통"

    // 실시간 시간 업데이트
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // 1초마다 업데이트
            currentTime = System.currentTimeMillis()
        }
    }

    // 경과 시간 계산 (startTime이 0이면 아직 시작되지 않음)
    val elapsedTime = if (startTime > 0) currentTime - startTime else 0L
    val elapsedDays = (elapsedTime / (24 * 60 * 60 * 1000)).toInt()
    val elapsedHours = ((elapsedTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedTime % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedTime % (60 * 1000)) / 1000).toInt()

    // 진행 중인 시간 포맷 (HH:MM:SS)
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)

    // 디버그용 로그 (실제 배포시 제거)
    // println("DEBUG: startTime=$startTime, currentTime=$currentTime, elapsedTime=$elapsedTime")
    // println("DEBUG: days=$elapsedDays, hours=$elapsedHours, minutes=$elapsedMinutes, seconds=$elapsedSeconds")

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

    // 진행률 계산
    val progress = if (targetDays > 0) (elapsedDays.toFloat() / targetDays).coerceAtMost(1.0f) else 0f

    // 레벨에 따른 배경색 (명세서 기준)
    val backgroundColor = when {
        elapsedDays < 7 -> Color(0xFFF5F5F5) // Gray
        elapsedDays < 30 -> Color(0xFFFFF3CD) // Light Yellow
        elapsedDays < 90 -> Color(0xFFE7F3FF) // Light Blue
        elapsedDays < 365 -> Color(0xFFE8F5E8) // Light Green
        else -> Color(0xFFFFF0DC) // Light Gold
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
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
                text = getLevelName(elapsedDays),
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

            // 중앙 메인 지표 (StartActivity 스타일)
            Box(
                modifier = Modifier.width(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 메인 숫자 표시 (클릭 가능, 애니메이션 없음)
                    Box(
                        modifier = Modifier
                            .clickable {
                                currentIndicator = (currentIndicator + 1) % 6
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        when (currentIndicator) {
                            0 -> {
                                // 금주 일수
                                Text(
                                    text = if (elapsedHours > 0) "${elapsedDays}일 ${elapsedHours}시간" else "${elapsedDays}",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                            1 -> {
                                // 진행 시간 (시:분:초 형식)
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds),
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                            2 -> {
                                // 현재 레벨
                                Text(
                                    text = getLevelName(elapsedDays),
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                            3 -> {
                                // 절약한 금액 (천단위 구분)
                                Text(
                                    text = String.format(Locale.getDefault(), "%,d", savedMoney),
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                            4 -> {
                                // 절약한 시간
                                Text(
                                    text = "${savedHours}",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                            5 -> {
                                // 기대 수명
                                Text(
                                    text = "${lifeGainDays}",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // 단위 표시 (StartActivity 스타일)
            Text(
                text = when (currentIndicator) {
                    0 -> if (elapsedHours > 0) "" else "일"
                    1 -> ""  // 시:분은 단위 표시 없음
                    2 -> ""  // 레벨명은 단위 표시 없음
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
                    // TODO: 금주 중지 로직 구현
                    // 1. 현재 기록 저장
                    // 2. 시작 시간 리셋
                    // 3. StartActivity로 이동
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

        LinearProgressIndicator(
            progress = progress,
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

// 레벨명 함수 (기��� 레벨 테이블 기준)
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
