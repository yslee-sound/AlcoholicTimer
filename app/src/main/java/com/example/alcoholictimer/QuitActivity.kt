package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuitScreen() {
    val context = LocalContext.current

    // Intent에서 RunActivity로부터 전달받은 데이터 가져오기
    val activity = context as? QuitActivity
    val intent = activity?.intent

    // RunActivity에서 전달받은 중지 시점의 데이터 사용
    val elapsedDays = intent?.getIntExtra("elapsed_days", 0) ?: 0
    val elapsedHours = intent?.getIntExtra("elapsed_hours", 0) ?: 0
    val elapsedMinutes = intent?.getIntExtra("elapsed_minutes", 0) ?: 0
    val savedMoney = intent?.getDoubleExtra("saved_money", 0.0) ?: 0.0
    val savedHours = intent?.getDoubleExtra("saved_hours", 0.0) ?: 0.0
    val lifeGainDays = intent?.getDoubleExtra("life_gain_days", 0.0) ?: 0.0
    val levelName = intent?.getStringExtra("level_name") ?: "새싹"
    val levelColorValue = intent?.getLongExtra("level_color", 0L) ?: 0L
    val quitTimestamp = intent?.getLongExtra("quit_timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()

    // 레벨 색상 복원
    val levelColor = if (levelColorValue != 0L) {
        Color(levelColorValue.toULong())
    } else {
        Color(0xFF4CAF50) // 기본값
    }

    // SharedPreferences에서 기본 설정만 가져오기 (실시간 계산하지 않음)
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val targetDays = sharedPref.getFloat("target_days", 30f)

    // 계산된 값들
    val costVal = when(sharedPref.getString("selected_cost", "중") ?: "중") {
        "저" -> 10000
        "중" -> 40000
        "고" -> 70000
        else -> 40000
    }

    val freqVal = when(sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회") {
        "주 1회 이하" -> 1.0
        "주 2~3회" -> 2.5
        "주 4회 이상" -> 5.0
        else -> 2.5
    }

    val drinkHoursVal = when(sharedPref.getString("selected_duration", "보통") ?: "보통") {
        "짧음" -> 2
        "보통" -> 4
        "김" -> 6
        else -> 4
    }

    val hangoverHoursVal = 5
    val weeks = elapsedDays / 7.0
    val savedMoneyCalc = (weeks * freqVal * costVal)
    val savedHoursCalc = (weeks * freqVal * (drinkHoursVal + hangoverHoursVal))
    val lifeGainDaysCalc = ((elapsedDays / 30.0) * 1.0)

    // 모던한 그라데이션 배경 (RunActivity와 동일)
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
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 16.dp), // 상하 패딩 줄임
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false) // fill = false로 변경하여 필요한 만큼만 공간 사용
        ) {
            Spacer(modifier = Modifier.height(12.dp)) // 줄임

            // 상단 메시지 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp), // 32dp에서 20dp로 줄임
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 이모지 아이콘
                    CompositionLocalProvider(
                        LocalDensity provides Density(LocalDensity.current.density, 1f)
                    ) {
                        Text(
                            text = "🤔",
                            fontSize = 48.sp, // 60sp에서 48sp로 줄임
                            modifier = Modifier.padding(bottom = 12.dp) // 16dp에서 12dp로 줄임
                        )
                    }

                    Text(
                        text = "정말 멈추시겠어요?",
                        fontSize = 22.sp, // 24sp에서 22sp로 약간 줄임
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp) // 8dp에서 6dp로 줄임
                    )

                    Text(
                        text = "지금까지 잘 해오셨는데...",
                        fontSize = 14.sp, // 16sp에서 14sp로 줄임
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // 24dp에서 16dp로 줄임

            // 통계 카드들 - RunActivity에서 전달받은 데이터 사용
            StatisticsCardsSection(
                elapsedDays = elapsedDays,
                elapsedHours = elapsedHours,
                elapsedMinutes = elapsedMinutes,
                savedMoney = savedMoney, // RunActivity에서 전달받은 값 사용
                savedHours = savedHours, // RunActivity에서 전달받은 값 사용
                lifeGainDays = lifeGainDays, // RunActivity에서 전달받은 값 사용
                levelName = levelName, // RunActivity에서 전달받은 레벨명 사용
                levelColor = levelColor // RunActivity에서 전달받은 레벨 색상 사용
            )
        }

        // 버튼 영역과 상단 콘텐츠 사이의 최소 간격 보장
        Spacer(modifier = Modifier.height(16.dp))

        // 버튼 영역 (하단 고정)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 중지 버튼
            var isPressed by remember { mutableStateOf(false) }
            var progress by remember { mutableFloatStateOf(0f) }
            val coroutineScope = rememberCoroutineScope()

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // 배경 원형 진행 바
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(80.dp),
                    color = Color(0xFFE0E0E0),
                    strokeWidth = 4.dp,
                    trackColor = Color.Transparent
                )

                // 진행 상태 원형 진행 바
                if (isPressed) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(80.dp),
                        color = Color(0xFFD32F2F),
                        strokeWidth = 4.dp,
                        trackColor = Color.Transparent
                    )
                }

                Card(
                    modifier = Modifier
                        .size(100.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                isPressed = true
                                progress = 0f
                                Log.d("QuitActivity", "터치 시작")

                                // 1.5초 동안 진행 바 채우기
                                val progressJob = coroutineScope.launch {
                                    val duration = 1500L // 1.5초
                                    val startTime = System.currentTimeMillis()

                                    while (progress < 1f && isPressed) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                        delay(16) // 60fps
                                    }

                                    if (progress >= 1f && isPressed) {
                                        // 진행 바가 완전히 채워짐 - 중지 처리
                                        Log.d("QuitActivity", "진행 바 완료 - 중지 처리 시작")
                                        saveCompletedRecord(
                                            context = context,
                                            startTime = startTime,
                                            endTime = System.currentTimeMillis(),
                                            targetDays = targetDays,
                                            actualDays = elapsedDays
                                        )
                                        sharedPref.edit {
                                            remove("start_time")
                                            putBoolean("timer_completed", true)
                                        }
                                        val intent = Intent(context, StartActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        context.startActivity(intent)
                                        (context as? QuitActivity)?.finish()
                                    }
                                }

                                // 터치 해제 대기
                                val result = waitForUpOrCancellation()

                                // 터치가 해제되면 진행 바 리셋
                                isPressed = false
                                progressJob.cancel()

                                if (result != null && progress < 1f) {
                                    // 진행 바가 완료되기 전에 손을 뗐음
                                    Log.d("QuitActivity", "진행 바 미완료로 취소됨: ${String.format("%.1f", progress * 100)}%")
                                    Toast.makeText(context, "길게 눌러서 진행 바를 채워주세요 (${String.format("%.0f", progress * 100)}%)", Toast.LENGTH_SHORT).show()
                                }

                                // 진행 바 리셋 애니메이션
                                coroutineScope.launch {
                                    while (progress > 0f) {
                                        progress = (progress - 0.1f).coerceAtLeast(0f)
                                        delay(16)
                                    }
                                }
                            }
                        },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPressed) Color(0xFFD32F2F) else Color(0xFFE53935)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "중지",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(48.dp))

            // 계속 버튼 (RunActivity와 동일한 방식)
            Card(
                onClick = {
                    (context as? QuitActivity)?.finish()
                },
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "계속",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 100dp에서 24dp로 대폭 줄임
    }
}

@Composable
fun StatisticsCardsSection(
    elapsedDays: Int,
    elapsedHours: Int,
    elapsedMinutes: Int,
    savedMoney: Double,
    savedHours: Double,
    lifeGainDays: Double,
    levelName: String, // 추가: 레벨명
    levelColor: Color // 추가: 레벨 색상
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 첫 번째 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard(
                value = "${elapsedDays}",
                label = "금주 일수",
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            ModernStatCard(
                value = levelName, // 전달받은 레벨명 사용
                label = "레벨",
                color = levelColor, // 전달받은 레벨 색상 사용
                modifier = Modifier.weight(1f)
            )
        }

        // 두 번째 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard(
                value = String.format(Locale.getDefault(), "%02d:%02d", elapsedHours, elapsedMinutes),
                label = "경과 시간",
                color = Color(0xFF388E3C),
                modifier = Modifier.weight(1f)
            )
            ModernStatCard(
                value = String.format(Locale.getDefault(), "%.1f", savedMoney / 10000),
                label = "절약 금액",
                color = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
        }

        // 세 번째 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard(
                value = String.format(Locale.getDefault(), "%.1f", savedHours),
                label = "절약 시간",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            ModernStatCard(
                value = String.format(Locale.getDefault(), "%.1f", lifeGainDays),
                label = "기대 수명+",
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ModernStatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(15.dp), //15
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 1f)
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        // onClick이 있는 경우 RunActivity와 동일한 방식 사용
        Card(
            onClick = onClick,
            modifier = modifier.size(80.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    } else {
        // onClick이 없는 경우 (Preview용) 기존 방식 사용
        Card(
            modifier = modifier.size(80.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
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
    return LevelDefinitions.getLevelName(days)
}

@Preview(showBackground = true)
@Composable
fun QuitScreenAuto() {
    QuitScreen()
}

@Preview(showBackground = true, name = "QuitScreen fontScale 1.0", fontScale = 1.0f)
@Preview(showBackground = true, name = "QuitScreen fontScale 2.0", fontScale = 2.0f)
@Composable
fun PreviewQuitScreen() {
    QuitScreenPreview()
}

// Preview 전용 컴포넌트들
@Composable
fun QuitScreenPreview() {
    // 가짜 데이터로 프리뷰
    val elapsedDays = 15
    val elapsedHours = 12
    val elapsedMinutes = 30
    val savedMoney = 600000.0
    val savedHours = 135.0
    val lifeGainDays = 0.5
    val levelName = "새싹"
    val levelColor = Color(0xFF4CAF50)

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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 상단 메시지 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f) //0.95f
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤔",
                        fontSize = 60.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "정말 멈추시겠어요?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "지금까지 잘 해오셨는데...",
                        fontSize = 16.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 통계 카드들
            StatisticsCardsSection(
                elapsedDays = elapsedDays,
                elapsedHours = elapsedHours,
                elapsedMinutes = elapsedMinutes,
                savedMoney = savedMoney,
                savedHours = savedHours,
                lifeGainDays = lifeGainDays,
                levelName = levelName,
                levelColor = levelColor
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // 버튼 영역 (하단 고정)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 중지 버튼
            ModernControlButton(
                icon = Icons.Default.Close,
                backgroundColor = Color(0xFFE53935),
                contentDescription = "중지"
            )

            Spacer(modifier = Modifier.width(48.dp))

            // 계속 버튼 (Preview용 - 클릭 이벤트 없음)
            ModernControlButton(
                icon = Icons.Default.PlayArrow,
                backgroundColor = Color(0xFF4CAF50),
                contentDescription = "계속"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(
    showBackground = true,
    name = "QuitScreen - 시작 단계",
    widthDp = 360,
    heightDp = 800
)
@Composable
fun QuitScreenStartPreview() {
    QuitScreenPreview()
}

@Preview(
    showBackground = true,
    name = "QuitScreen - 다크 모드",
    widthDp = 360,
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun QuitScreenDarkPreview() {
    QuitScreenPreview()
}

@Preview(
    showBackground = true,
    name = "QuitScreen - 큰 폰트",
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.5f
)
@Composable
fun QuitScreenLargeFontPreview() {
    QuitScreenPreview()
}

@Preview(showBackground = true, name = "StatisticsCardsSection Preview")
@Composable
fun StatisticsCardsSectionPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatisticsCardsSection(
            elapsedDays = 15,
            elapsedHours = 12,
            elapsedMinutes = 30,
            savedMoney = 600000.0,
            savedHours = 135.5,
            lifeGainDays = 0.5,
            levelName = "새싹",
            levelColor = Color(0xFF4CAF50)
        )
    }
}

@Preview(showBackground = true, name = "ModernStatCard Preview")
@Composable
fun ModernStatCardPreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernStatCard(
            value = "15일",
            label = "금주 일수",
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(1f)
        )
        ModernStatCard(
            value = "새싹",
            label = "레벨",
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, name = "ModernControlButton Preview")
@Composable
fun ModernControlButtonPreview() {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernControlButton(
            icon = Icons.Default.Close,
            backgroundColor = Color(0xFFE53935),
            contentDescription = "중지",
            onClick = {
                scope.launch {
                    delay(3000)
                    // 여기에 중지 동작을 넣으세요 (예: Log, Toast 등)
                    println("중지 버튼 동작 실행됨")
                }
            }
        )
        ModernControlButton(
            icon = Icons.Default.PlayArrow,
            backgroundColor = Color(0xFF4CAF50),
            contentDescription = "계속"
            // onClick 필���시 추가
        )
    }
}
