package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class QuitActivity : BaseActivity() {

    override fun getScreenTitle(): String = "금주 종료"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // BaseScreen을 사용하지 않고 직접 UI 구성 (햄버거 메뉴 제거)
            QuitScreen()
        }
    }
}

@Composable
fun QuitScreen() {
    val context = LocalContext.current

    // SharedPreferences에서 데이터 가져오기
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val targetDays = sharedPref.getFloat("target_days", 30f)

    // 설정값 가져오기
    val selectedCost = sharedPref.getString("selected_cost", "중") ?: "중"
    val selectedFrequency = sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회"
    val selectedDuration = sharedPref.getString("selected_duration", "보통") ?: "보통"

    // 현재 시간과 경과 시간 계산
    val currentTime = System.currentTimeMillis()
    val elapsedTime = if (startTime > 0) currentTime - startTime else 0L
    val elapsedDays = (elapsedTime / (24 * 60 * 60 * 1000)).toInt()
    val elapsedHours = ((elapsedTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedTime % (60 * 60 * 1000)) / (60 * 1000)).toInt()

    // 계산된 값들
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
    val weeks = elapsedDays / 7.0
    val savedMoney = (weeks * freqVal * costVal).roundToInt()
    val savedHours = (weeks * freqVal * (drinkHoursVal + hangoverHoursVal)).roundToInt()
    val lifeGainDays = ((elapsedDays / 30.0) * 1.0).roundToInt()

    // 레벨에 따른 배경색
    val backgroundColor = when {
        elapsedDays < 7 -> Color(0xFFF5F5F5)
        elapsedDays < 30 -> Color(0xFFFFF3CD)
        elapsedDays < 90 -> Color(0xFFE7F3FF)
        elapsedDays < 365 -> Color(0xFFE8F5E8)
        else -> Color(0xFFFFF0DC)
    }

    // 계속 버튼 색상 (레벨에 따라)
    val continueButtonColor = when {
        elapsedDays < 7 -> Color(0xFF888888)
        elapsedDays < 30 -> Color(0xFFFFB74D)
        elapsedDays < 90 -> Color(0xFF42A5F5)
        elapsedDays < 365 -> Color(0xFF66BB6A)
        else -> Color(0xFFFFB74D)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.safeDrawing) // 안전 영역 패딩 추가
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 상단 배경 영역 (아이콘)
        val baseDensity = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 1f)) {
            Text(
                text = "🍃",
                fontSize = 120.sp,
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }

        // 금주 기록 요약 영역
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 행 (3개)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        value = "${elapsedDays}",
                        label = "금주 일수",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        value = getLevelName(elapsedDays),
                        label = "레벨명",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        value = String.format(Locale.getDefault(), "%02d:%02d", elapsedHours, elapsedMinutes),
                        label = "경과 시간",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 하단 행 (3개)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        value = String.format(Locale.getDefault(), "%,d", savedMoney),
                        label = "절약 금액",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        value = "${savedHours}",
                        label = "절약 시간",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        value = "+${lifeGainDays}일",
                        label = "기대 수명",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 구분선
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 버튼들을 하단에 고정하기 위한 가변 Spacer
        Spacer(modifier = Modifier.weight(1f))

        // 컨트롤 버튼 영역 (하단 고정)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 중지 버튼
            ControlButton(
                backgroundColor = Color.Black,
                contentColor = Color.White,
                content = "■",
                onClick = {
                    // 금주 중지 로직
                    saveCompletedRecord(
                        context = context,
                        startTime = startTime,
                        endTime = System.currentTimeMillis(),
                        targetDays = targetDays,
                        actualDays = elapsedDays
                    )

                    // SharedPreferences 초기화
                    sharedPref.edit {
                        remove("start_time")
                        putBoolean("timer_completed", true)
                    }

                    // StartActivity로 이동 (금주 설정 화면으로 한번만 이동)
                    val intent = Intent(context, StartActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    context.startActivity(intent)
                    // QuitActivity만 종료 (전환효과 없이 바로 이동)
                    (context as? QuitActivity)?.overridePendingTransition(0, 0)
                    (context as? QuitActivity)?.finish()
                }
            )

            // 계속 버튼
            ControlButton(
                backgroundColor = Color(0xFF8ABF33), // 시작버튼 색상 #8ABF33으로 변경
                contentColor = Color.White,
                content = "▶",
                onClick = {
                    // QuitActivity만 종료하여 RunActivity로 자연스럽게 돌아가도록 변경
                    (context as? QuitActivity)?.finish()
                }
            )
        }
    }
}

@Composable
fun StatisticItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .width(100.dp), // 고정 너비로 정렬 보장
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp), // 고정 높이로 숫자 영역 통일
            contentAlignment = Alignment.Center
        ) {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 1f)) {
                Text(
                    text = value,
                    fontSize = 24.sp, // 크기 조절하여 일관성 확보
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    lineHeight = 26.sp,
                    maxLines = 2 // 긴 텍스트 처리
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp), // 고정 높이로 라벨 영역 통일
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ControlButton(
    backgroundColor: Color,
    contentColor: Color,
    content: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp) // 버튼 크기 통일
            .shadow(8.dp, CircleShape) // 그림자 효과 추가
            .background(backgroundColor, CircleShape)
            .clickable { onClick() }
    ) {
        val baseDensity = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 1f)) {
            Text(
                text = content,
                fontSize = 40.sp, // 아이콘 크기 통일
                color = contentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 기록 저장 함수
private fun saveCompletedRecord(
    context: Context,
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int
) {
    try {
        Log.d("QuitActivity", "========== 기록 저장 시작 ==========")
        Log.d("QuitActivity", "startTime: $startTime")
        Log.d("QuitActivity", "endTime: $endTime")
        Log.d("QuitActivity", "targetDays: $targetDays")
        Log.d("QuitActivity", "actualDays: $actualDays")

        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        val recordId = System.currentTimeMillis().toString()
        Log.d("QuitActivity", "생성된 기록 ID: $recordId")

        // 목표 달성률 계산
        val achievementRate = if (targetDays > 0) {
            (actualDays.toFloat() / targetDays) * 100
        } else {
            0f
        }

        // 목표 달성 여부 확인
        val isCompleted = achievementRate >= 100f
        val status = if (isCompleted) "완료" else "중지"

        Log.d("QuitActivity", "달성률: ${achievementRate}%, 완료 여부: $isCompleted, 상태: $status")

        val record = JSONObject().apply {
            put("id", recordId)
            put("startTime", startTime)
            put("endTime", endTime)
            put("targetDays", targetDays.toInt()) // Double에서 Int로 변경
            put("actualDays", actualDays)
            put("isCompleted", isCompleted)
            put("status", status)
            put("createdAt", System.currentTimeMillis())
        }

        Log.d("QuitActivity", "생성된 기록 JSON: $record")

        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        Log.d("QuitActivity", "기존 기록들: $recordsJson")

        val recordsList = try {
            JSONArray(recordsJson)
        } catch (e: Exception) {
            Log.w("QuitActivity", "기존 기록 파싱 실패, 새로운 배열 생성: ${e.message}")
            JSONArray()
        }

        recordsList.put(record)
        Log.d("QuitActivity", "기록 추가 후 배열: $recordsList")

        val finalJson = recordsList.toString()
        sharedPref.edit {
            putString("sobriety_records", finalJson)
        }

        // 저장 확인
        val savedJson = sharedPref.getString("sobriety_records", "[]")
        Log.d("QuitActivity", "저장 확인 - 저장된 데이터: $savedJson")
        Log.d("QuitActivity", "========== 기록 저장 완료 ==========")

        val message = "금주 기록이 저장되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        Log.e("QuitActivity", "기록 저장 중 오류 발생", e)
        Log.e("QuitActivity", "오류 상세: ${e.message}")
        Log.e("QuitActivity", "스택 트레이스: ${e.stackTraceToString()}")
        Toast.makeText(context, "기록 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
    }
}

// 레벨명 함수
private fun getLevelName(days: Int): String {
    return when {
        days < 7 -> "시작"
        days < 30 -> "작심 7일"
        days < 90 -> "한 달 클리어"
        days < 365 -> "3개월 클리어"
        else -> "절제의 레전드"
    }
}

@Preview(showBackground = true, name = "QuitScreen fontScale 1.0", fontScale = 1.0f)
@Preview(showBackground = true, name = "QuitScreen fontScale 2.0", fontScale = 2.0f)
@Composable
fun PreviewQuitScreen() {
    QuitScreen()
}
