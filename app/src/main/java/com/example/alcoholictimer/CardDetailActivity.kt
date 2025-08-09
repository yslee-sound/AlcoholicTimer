package com.example.alcoholictimer

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * 금주 기록 카드의 상세 정보를 표시하는 액티비티
 * DetailActivity와 구분하여 카드 클릭 시 표시되는 화면으로 사용
 */
class CardDetailActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CardDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "===== CardDetailActivity onCreate 시작 =====")

        try {
            // Intent에서 데이터 받기 (안전한 방식으로)
            val startTime = intent.getLongExtra("start_time", 0L)
            val endTime = intent.getLongExtra("end_time", System.currentTimeMillis())
            val targetDays = intent.getFloatExtra("target_days", 30f)
            val actualDays = intent.getIntExtra("actual_days", 0)
            val isCompleted = intent.getBooleanExtra("is_completed", false)

            Log.d(TAG, "수신된 데이터: startTime=$startTime, endTime=$endTime, targetDays=$targetDays, actualDays=$actualDays, isCompleted=$isCompleted")

            // 데이터 유효성 검사 (더 관대하게)
            if (actualDays < 0) {
                Log.e(TAG, "잘못된 데이터: actualDays=$actualDays")
                finish()
                return
            }

            // targetDays가 0 이하인 경우 기본값으로 설정
            val safeTargetDays = if (targetDays <= 0) 30f else targetDays
            // actualDays가 0인 경우도 허용하되, 최소 1로 계산에 사용
            val safeActualDays = if (actualDays <= 0) 1 else actualDays

            Log.d(TAG, "안전한 값들: targetDays=$safeTargetDays, actualDays=$safeActualDays")

            Log.d(TAG, "setContent 호출 시작...")
            setContent {
                CardDetailScreen(
                    startTime = startTime,
                    endTime = endTime,
                    targetDays = safeTargetDays,
                    actualDays = safeActualDays,
                    isCompleted = isCompleted,
                    onBack = {
                        Log.d(TAG, "뒤로가기 버튼 클릭")
                        finish()
                    }
                )
            }
            Log.d(TAG, "setContent 호출 완료")
            Log.d(TAG, "===== CardDetailActivity onCreate 완료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "CardDetailActivity 초기화 중 오류", e)
            Log.e(TAG, "오류 스택트레이스: ${e.stackTraceToString()}")
            finish()
        }
    }
}

@Composable
fun CardDetailScreen(
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int,
    isCompleted: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 날짜/시간 포맷
    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd - a h:mm", Locale.getDefault())
    val displayDateTime = if (startTime > 0) {
        dateTimeFormat.format(Date(startTime))
    } else {
        "오늘 - ${SimpleDateFormat("a h:mm", Locale.getDefault()).format(Date())}"
    }

    // 기록 제목 자동 생성
    val recordTitle = if (isCompleted) {
        "금주 ${actualDays}일 달성 기록"
    } else {
        "금주 ${actualDays}일차 중단 기록"
    }

    // 금주 기간 계산
    val totalDuration = if (startTime > 0) {
        ((endTime - startTime) / (24 * 60 * 60 * 1000)).toInt()
    } else {
        actualDays
    }

    // 설정값 가져오기 (절약 금액/시간 계산용)
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val selectedCost = sharedPref.getString("selected_cost", "중") ?: "중"
    val selectedFrequency = sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회"
    val selectedDuration = sharedPref.getString("selected_duration", "보통") ?: "보통"

    // 절약 금액/시간 계산
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
    val weeks = actualDays / 7.0
    val savedMoney = (weeks * freqVal * costVal).roundToInt()
    val savedHours = (weeks * freqVal * (drinkHoursVal + hangoverHoursVal)).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // 헤더 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 뒤로가기 버튼
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.LightGray, CircleShape)
                    .clickable { onBack() }
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "금주 기록 상세",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(40.dp)) // 균형 맞추기
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 날짜 및 시간
        Text(
            text = displayDateTime,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 기록 제목
        Text(
            text = recordTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 주요 통계 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 왼쪽 대형 숫자
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = actualDays.toString(),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 72.sp
                )
                Text(
                    text = "일",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            // 오른쪽 성취 아이콘
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (isCompleted) "🏆" else "⏸️",
                    fontSize = 60.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isCompleted) "목표 달성" else "중단됨",
                    fontSize = 14.sp,
                    color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 서브 통계 (2줄 배치)
        Column {
            // 첫 번째 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CardSubStatItem(
                    value = "${totalDuration}일",
                    label = "총 금주 기간",
                    modifier = Modifier.weight(1f)
                )
                CardSubStatItem(
                    value = String.format(Locale.getDefault(), "%,d원", savedMoney),
                    label = "절약한 금액",
                    modifier = Modifier.weight(1f)
                )
                CardSubStatItem(
                    value = "${savedHours}시간",
                    label = "절약한 시간",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 두 번째 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CardSubStatItem(
                    value = "${((actualDays.toFloat() / targetDays) * 100).roundToInt()}%",
                    label = "목표 달성률",
                    modifier = Modifier.weight(1f)
                )
                CardSubStatItem(
                    value = getLevelName(actualDays),
                    label = "달성 레벨",
                    modifier = Modifier.weight(1f)
                )
                CardSubStatItem(
                    value = "${(actualDays / 30.0).roundToInt()}일",
                    label = "기대 수명 증가",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 하단 버튼
        Button(
            onClick = { onBack() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            )
        ) {
            Text(
                text = "확인",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CardSubStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
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
fun PreviewCardDetailScreen() {
    CardDetailScreen(
        startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
        endTime = System.currentTimeMillis(),
        targetDays = 30f,
        actualDays = 7,
        isCompleted = true,
        onBack = {}
    )
}
