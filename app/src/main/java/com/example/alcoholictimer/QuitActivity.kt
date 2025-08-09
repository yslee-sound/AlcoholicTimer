package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt

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
    val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 상단 배경 영역 (아이콘)
        Text(
            text = "🍃",
            fontSize = 120.sp,
            modifier = Modifier.padding(bottom = 40.dp)
        )

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
                        value = String.format("%02d:%02d", elapsedHours, elapsedMinutes),
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

        // 컨트롤 버튼 영역
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
                        actualDays = elapsedDays,
                        isCompleted = false
                    )

                    // SharedPreferences 초기화
                    val editor = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE).edit()
                    editor.remove("start_time")
                    editor.putBoolean("timer_completed", true)
                    editor.apply()

                    // StartActivity로 이동
                    val intent = Intent(context, StartActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    (context as? QuitActivity)?.finish()
                }
            )

            // 계속 버튼
            ControlButton(
                backgroundColor = continueButtonColor,
                contentColor = Color.White,
                content = "▶",
                onClick = {
                    // RunActivity로 돌아가기
                    val intent = Intent(context, RunActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    (context as? QuitActivity)?.finish()
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
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
            .size(80.dp)
            .background(backgroundColor, CircleShape)
            .clickable { onClick() }
    ) {
        Text(
            text = content,
            fontSize = 32.sp, // 아이콘 크기 줄임 (48sp → 32sp)
            color = contentColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// 기록 저장 함수
private fun saveCompletedRecord(
    context: Context,
    startTime: Long,
    endTime: Long,
    targetDays: Float, // Int에서 Float로 변경
    actualDays: Int,
    isCompleted: Boolean
) {
    try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        val recordId = System.currentTimeMillis().toString()

        val record = JSONObject().apply {
            put("id", recordId)
            put("startTime", startTime)
            put("endTime", endTime)
            put("targetDays", targetDays.toDouble()) // Float를 Double로 변환하여 저장
            put("actualDays", actualDays)
            put("isCompleted", isCompleted)
            put("status", if (isCompleted) "완료" else "중지")
            put("createdAt", System.currentTimeMillis())
        }

        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val recordsList = try {
            JSONArray(recordsJson)
        } catch (e: Exception) {
            JSONArray()
        }

        recordsList.put(record)

        with(sharedPref.edit()) {
            putString("sobriety_records", recordsList.toString())
            apply()
        }

        val message = if (isCompleted) "금주 목표를 달성했습니다!" else "금주 기록이 저장되었습니다."
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
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

@Preview(showBackground = true)
@Composable
fun PreviewQuitScreen() {
    QuitScreen()
}
