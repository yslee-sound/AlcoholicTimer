package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DetailActivity : ComponentActivity() {  // BaseActivity에서 ComponentActivity로 변경

    companion object {
        private const val TAG = "DetailActivity"

        fun start(
            context: Context,
            startTime: Long,
            endTime: Long,
            targetDays: Float,
            actualDays: Int,
            isCompleted: Boolean
        ) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("start_time", startTime)
                putExtra("end_time", endTime)
                putExtra("target_days", targetDays)
                putExtra("actual_days", actualDays)
                putExtra("is_completed", isCompleted)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태표시줄 표시 설정
        window.statusBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        Log.d(TAG, "===== DetailActivity onCreate 시작 =====")

        try {
            // Intent에서 데이터 받기 (안전한 방식으로)
            val startTime = intent.getLongExtra("start_time", 0L)
            val endTime = intent.getLongExtra("end_time", System.currentTimeMillis())
            val targetDays = intent.getFloatExtra("target_days", 30f)
            val actualDays = intent.getIntExtra("actual_days", 0)
            val isCompleted = intent.getBooleanExtra("is_completed", false)

            Log.d(TAG, "수신된 데이터: startTime=$startTime, endTime=$endTime, targetDays=$targetDays, actualDays=$actualDays, isCompleted=$isCompleted")

            // 데이터 유효성 검사 (관대하게)
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

            setContent {
                DetailScreen(
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
            Log.d(TAG, "===== DetailActivity onCreate 완료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "DetailActivity 초기화 중 오류", e)
            Log.e(TAG, "오류 스택트레이스: ${e.stackTraceToString()}")
            finish()
        }
    }
}

@Composable
fun DetailScreen(
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int,
    isCompleted: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 날짜/시간 포맷
    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd - a h:mm", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    val displayDateTime = if (startTime > 0) {
        dateTimeFormat.format(Date(startTime))
    } else {
        "오늘 - ${SimpleDateFormat("a h:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(Date())}"
    }

    // 기록 제목 자동 생성
    val recordTitle = if (isCompleted) {
        "금주 ${actualDays}일 달성 기록"
    } else {
        "금주 ${actualDays}일차 중단 기록"
    }

    // 정확한 금주 기간 계산 (시간 단위까지)
    val totalDurationMillis = if (startTime > 0) {
        endTime - startTime
    } else {
        actualDays * 24 * 60 * 60 * 1000L
    }
    val totalHours = totalDurationMillis / (60 * 60 * 1000.0)
    val totalDays = totalHours / 24.0

    // 금주 기간 표시 형식 결정
    val durationDisplay = when {
        totalDays >= 1.0 -> {
            val days = totalDays.toInt()
            val remainingHours = ((totalDays - days) * 24).toInt()
            if (remainingHours > 0) {
                "${days}일 ${remainingHours}시간"
            } else {
                "${days}일"
            }
        }
        totalHours >= 1.0 -> {
            "${totalHours.toInt()}시간"
        }
        else -> {
            val minutes = (totalDurationMillis / (60 * 1000)).toInt()
            "${minutes}분"
        }
    }

    // 설정값 가져오기 (절약 금액/시간 계산용)
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val selectedCost = sharedPref.getString("selected_cost", "중") ?: "중"
    val selectedFrequency = sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회"
    val selectedDuration = sharedPref.getString("selected_duration", "보통") ?: "보통"

    // 절약 금액/시간 계산 (정확한 시간 기반)
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

    // 정확한 주 단위 계산 (시간 기반)
    val exactWeeks = totalHours / (24.0 * 7.0)
    val savedMoney = (exactWeeks * freqVal * costVal).roundToInt()
    val savedHours = (exactWeeks * freqVal * (drinkHoursVal + hangoverHoursVal)).roundToInt()

    // 정확한 목표 달성률 계산 (실제 금주 기간 기반)
    val achievementRate = ((totalDays / targetDays) * 100.0).let { rate ->
        if (rate > 100) 100.0 else rate
    }

    // 기대 수명 증가 계산 (소수점 표기)
    val lifeExpectancyIncrease = totalDays / 30.0

    // 레벨에 따른 배경색
    val backgroundColor = when {
        actualDays < 7 -> Color(0xFFF5F5F5)
        actualDays < 30 -> Color(0xFFFFF3CD)
        actualDays < 90 -> Color(0xFFE7F3FF)
        actualDays < 365 -> Color(0xFFE8F5E8)
        else -> Color(0xFFFFF0DC)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // 헤더 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, top = 24.dp), // 위쪽 패딩 24dp 추가
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 뒤로가기 버튼 (화살표 아이콘만)
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        onBack()
                    }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }

            // 타이틀 + 삭제 아이콘 그룹을 Row로 묶어서 최대 너비로 확장
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val baseDensity = LocalDensity.current
                val fixedFontDensity = Density(baseDensity.density, 1f)
                CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
                    Text(
                        text = "금주 기록 상세",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp) // 왼쪽 패딩 추가
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 0.dp)
                        .size(40.dp) // 전체 클릭 영역 크기
                        .clip(CircleShape) // 원형으로 클리핑하여 리플 효과도 원형으로 만듦
                        .clickable { showDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "기록 삭제",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp) // 아이콘 자체 크기는 유지
                    )
                }
            }
        }

        // 삭제 경고 다이얼로그
        if (showDeleteDialog) {
            val baseDensity = LocalDensity.current
            val fixedFontDensity = Density(baseDensity.density, 1f)
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("기록 삭제") },
                text = { Text("정말로 이 금주 기록을 삭제하시겠습니까?\n삭제 후 복구할 수 없습니다.") },
                confirmButton = {
                    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
                        TextButton(onClick = {
                            // 다이얼로그 먼저 닫기
                            showDeleteDialog = false
                            // 삭제 실행
                            try {
                                deleteRecord(context, startTime, endTime)
                            } catch (e: Exception) {
                                Log.e("DetailActivity", "삭제 중 오류", e)
                            }
                            // 즉시 액티비티 종료
                            (context as? DetailActivity)?.finish()
                        }) {
                            Text("삭제", color = Color.Red)
                        }
                    }
                },
                dismissButton = {
                    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("취소")
                        }
                    }
                }
            )
        }

        // 구분선 추가
        Spacer(modifier = Modifier.height(16.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )

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

        // 기록 제목 아래 구분선 추가
        Spacer(modifier = Modifier.height(16.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFE0E0E0)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 주요 통계 영역 - 트로피 아이콘 부분 제거
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start // SpaceBetween에서 Start로 변경
        ) {
            // 대형 숫자만 표시 (오른쪽 트로피 아이콘 제거)
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                val baseDensity = LocalDensity.current
                val fixedFontDensity = Density(baseDensity.density, 1f)
                CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
                    Text(
                        text = actualDays.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 72.sp,
                        modifier = Modifier.padding(start = 0.dp) // 왼쪽 패딩 추가
                    )
                    Text(
                        text = "일",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 0.dp) // 왼쪽 패딩 추가
                    )
                }
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
                SubStatItem(
                    value = durationDisplay,
                    label = "총 금주 기간",
                    modifier = Modifier.weight(1f)
                )
                SubStatItem(
                    value = String.format("%,d원", savedMoney),
                    label = "절약한 금액",
                    modifier = Modifier.weight(1.3f)
                )
                SubStatItem(
                    value = "${savedHours}시간",
                    label = "절약한 시간",
                    modifier = Modifier.weight(0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 두 번째 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SubStatItem(
                    value = String.format("%.1f%%", achievementRate),
                    label = "목표 달성률",
                    modifier = Modifier.weight(1f)
                )
                SubStatItem(
                    value = getLevelName(actualDays),
                    label = "달성 레벨",
                    modifier = Modifier.weight(1.3f)
                )
                SubStatItem(
                    value = String.format("%.1f일", lifeExpectancyIncrease),
                    label = "기대 수명 증가",
                    modifier = Modifier.weight(0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 확인 버튼 제거 - 하단 여백만 유지
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SubStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false // 정렬 방향 파라미터 추가
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start // 정렬 방향 선택
    ) {
        val baseDensity = LocalDensity.current
        val fixedFontDensity = Density(baseDensity.density, 1f)
        CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start // 정렬 방향 선택
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start // 정렬 방향 선택
            )
        }
    }
}

// 레벨명 함수
private fun getLevelName(days: Int): String {
    return when {
        days in 0..6 -> "작심 7일"
        days in 7..13 -> "의지의 2주"
        days in 14..29 -> "한달의 기적"
        days in 30..59 -> "습관의 탄생"
        days in 60..119 -> "계속되는 도전"
        days in 120..239 -> "거의 1년"
        days in 240..364 -> "금주 마스터"
        else -> "절제의 레전드"
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDetailScreen() {
    DetailScreen(
        startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
        endTime = System.currentTimeMillis(),
        targetDays = 30f,
        actualDays = 7,
        isCompleted = true,
        onBack = {} // 누락된 onBack 파라미터 추가
    )
}

@Preview(showBackground = true, name = "fontScale 1.0")
@Composable
fun PreviewDetailScreenFont1_0() {
    val baseDensity = LocalDensity.current
    val fixedFontDensity = Density(baseDensity.density, 1f)
    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        DetailScreen(
            startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
            endTime = System.currentTimeMillis(),
            targetDays = 30f,
            actualDays = 7,
            isCompleted = true,
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "fontScale 1.5")
@Composable
fun PreviewDetailScreenFont1_5() {
    val baseDensity = LocalDensity.current
    val fixedFontDensity = Density(baseDensity.density, 1.5f)
    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        DetailScreen(
            startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
            endTime = System.currentTimeMillis(),
            targetDays = 30f,
            actualDays = 7,
            isCompleted = true,
            onBack = {}
        )
    }
}

// 기록 삭제 함수 - 최적화
private fun deleteRecord(context: Context, startTime: Long, endTime: Long) {
    try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val recordsArray = org.json.JSONArray(recordsJson)
        val newArray = org.json.JSONArray()

        var deleted = false
        for (i in 0 until recordsArray.length()) {
            val record = recordsArray.getJSONObject(i)
            val recordStartTime = record.optLong("startTime", -1L)
            val recordEndTime = record.optLong("endTime", -1L)

            // 정확히 일치하는 기록만 삭제
            if (recordStartTime == startTime && recordEndTime == endTime) {
                deleted = true
                Log.d("DetailActivity", "기록 삭제됨: startTime=$startTime, endTime=$endTime")
            } else {
                newArray.put(record)
            }
        }

        if (deleted) {
            sharedPref.edit().putString("sobriety_records", newArray.toString()).apply()
            Log.d("DetailActivity", "삭제 완료. 남은 기록 수: ${newArray.length()}")
        } else {
            Log.w("DetailActivity", "삭제할 기록을 찾지 못함: startTime=$startTime, endTime=$endTime")
        }
    } catch (e: Exception) {
        Log.e("DetailActivity", "기록 삭제 중 오류", e)
    }
}
