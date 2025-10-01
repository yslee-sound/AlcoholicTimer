package com.example.alcoholictimer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.alcoholictimer.ui.LayoutConstants
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.utils.FormatUtils
import com.example.alcoholictimer.ui.theme.BluePrimaryLight
import com.example.alcoholictimer.ui.theme.AmberSecondaryLight
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.example.alcoholictimer.components.DetailStatCard

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

        // 상태표시줄/내비게이션바 설정 - 표준 API 사용
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            )
        )

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
    // 성공/실패 상태에 따른 포인트 컬러
    val accentColor = if (isCompleted) com.example.alcoholictimer.ui.theme.BluePrimaryLight else com.example.alcoholictimer.ui.theme.AmberSecondaryLight

    // 날짜/시간 포맷
    val dateTimeFormat = java.text.SimpleDateFormat("yyyy-MM-dd - a h:mm", java.util.Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    val displayDateTime = if (startTime > 0) {
        dateTimeFormat.format(java.util.Date(startTime))
    } else {
        val nowFormatted = java.text.SimpleDateFormat("a h:mm", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date())
        stringResource(id = R.string.detail_today_time, nowFormatted)
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

    // 설정값 가져오기 (절약 금액/시간 계산용) - Constants를 통해 안전하게 가져오기
    val (selectedCost, selectedFrequency, selectedDuration) = Constants.getUserSettings(context)

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
    val savedHoursExact = (exactWeeks * freqVal * (drinkHoursVal + hangoverHoursVal))

    // 정확한 목표 달성률 계산 (실제 금주 기간 기반)
    val achievementRate = ((totalDays / targetDays) * 100.0).let { rate ->
        if (rate > 100) 100.0 else rate
    }

    // 기대 수명 증가 계산 (소수점 표기)
    val lifeExpectancyIncrease = totalDays / 30.0

    // 전체 텍스트를 10% 축소 (fontScale 0.9배)
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = density.fontScale * 0.9f)) {
        // 모던한 그라데이션 배경
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color(0xFFE9ECEF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                // 헤더 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 좌측: 뒤로가기 + 제목
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onBack() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(id = R.string.cd_navigate_back),
                                tint = Color(0xFF2D3748),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
                            val base = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            val scaled = base.copy(fontSize = (base.fontSize.value * 1.3f).sp)
                            Text(
                                text = stringResource(id = R.string.detail_title),
                                style = scaled,
                                color = Color(0xFF2D3748),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 우측: 삭제 버튼
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.dialog_delete_title),
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 메인 정보 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LayoutConstants.CARD_CORNER_RADIUS),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(LayoutConstants.CARD_PADDING)
                    ) {
                        // 시작 날짜 및 시간
                        Text(
                            text = "${stringResource(id = R.string.detail_start_label)} $displayDateTime",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF718096)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 종료 날짜 및 시간
                        Text(
                            text = "${stringResource(id = R.string.detail_end_label)} ${dateTimeFormat.format(Date(endTime))}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF718096)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 대형 숫자 표시
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), "%.1f", totalDays),
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = accentColor
                                )
                                Text(
                                    text = stringResource(id = R.string.unit_day),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF718096)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 목표 달성률 프로그레스바
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_progress_rate),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF718096)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", achievementRate),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 프로그레스바
                            LinearProgressIndicator(
                                progress = { (achievementRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = Color(0xFFE2E8F0)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 목표일 정보
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(id = R.string.detail_progress_current, String.format(Locale.getDefault(), "%.1f", totalDays)),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF718096)
                                )
                                Text(
                                    text = stringResource(id = R.string.detail_progress_target, targetDays.toInt()),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 통계 카드들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LayoutConstants.STAT_ROW_SPACING)
                ) {
                    DetailStatCard(
                        value = String.format(Locale.getDefault(), "%.1f일", totalDays),
                        label = stringResource(id = R.string.stat_total_days),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_days)
                    )
                    DetailStatCard(
                        value = String.format(Locale.getDefault(), "%,.0f원", savedMoney.toDouble()),
                        label = stringResource(id = R.string.stat_saved_money_short),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_money)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LayoutConstants.STAT_ROW_SPACING)
                ) {
                    DetailStatCard(
                        value = String.format(Locale.getDefault(), "%.1f시간", savedHoursExact),
                        label = stringResource(id = R.string.stat_saved_hours_short),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_hours)
                    )
                    DetailStatCard(
                        value = FormatUtils.daysToDayHourString(lifeExpectancyIncrease, 2),
                        label = stringResource(id = R.string.indicator_title_life_gain),
                        modifier = Modifier.weight(1f),
                        valueColor = colorResource(id = R.color.color_indicator_life)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 삭제 경고 다이얼로그
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            stringResource(id = R.string.dialog_delete_title),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748)
                        )
                    },
                    text = {
                        Text(
                            stringResource(id = R.string.dialog_delete_message),
                            color = Color(0xFF4A5568)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                try {
                                    deleteRecord(context, startTime, endTime)
                                } catch (e: Exception) {
                                    Log.e("DetailActivity", "삭제 중 오류", e)
                                }
                                val activity = (context as? DetailActivity)
                                activity?.setResult(Activity.RESULT_OK)
                                activity?.finish()
                            }
                        ) {
                            Text(stringResource(id = R.string.dialog_delete_confirm), color = Color(0xFFE53E3E), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(id = R.string.dialog_cancel), color = Color(0xFF718096))
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "fontScale 1.0", fontScale = 1.0f)
@Preview(showBackground = true, name = "fontScale 2.0", fontScale = 2.0f)
@Composable
fun PreviewDetailScreen() {
    DetailScreen(
        startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
        endTime = System.currentTimeMillis(),
        targetDays = 30f,
        actualDays = 7,
        isCompleted = true,
        onBack = {}
    )
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
            sharedPref.edit {
                putString("sobriety_records", newArray.toString())
            }
            Log.d("DetailActivity", "삭제 완료. 남은 기록 수: ${newArray.length()}")
        } else {
            Log.w("DetailActivity", "삭제할 기록을 찾지 못함: startTime=$startTime, endTime=$endTime")
        }
    } catch (e: Exception) {
        Log.e("DetailActivity", "기록 삭제 중 오류", e)
    }
}
