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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class QuitActivity : BaseActivity() {

    override fun getScreenTitle(): String = "금주 종료"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // BaseScreen을 사용하지 않고 직접 UI 구성 (햄버거 메뉴 제거하되 동일한 여백 확보)
            QuitScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuitScreen() {
    val context = LocalContext.current
    val activity = context as? QuitActivity
    val intent = activity?.intent
    val elapsedDays = intent?.getIntExtra("elapsed_days", 0) ?: 0
    val elapsedHours = intent?.getIntExtra("elapsed_hours", 0) ?: 0
    val elapsedMinutes = intent?.getIntExtra("elapsed_minutes", 0) ?: 0
    val savedMoney = intent?.getDoubleExtra("saved_money", 0.0) ?: 0.0
    val savedHours = intent?.getDoubleExtra("saved_hours", 0.0) ?: 0.0
    val lifeGainDays = intent?.getDoubleExtra("life_gain_days", 0.0) ?: 0.0
    val levelName = intent?.getStringExtra("level_name") ?: "새싹"
    val levelColorValue = intent?.getLongExtra("level_color", 0L) ?: 0L
    val levelColor = if (levelColorValue != 0L) Color(levelColorValue.toULong()) else Color(0xFF4CAF50)
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val targetDays = sharedPref.getFloat("target_days", 30f)

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFF8F9FA), Color(0xFFE3F2FD), Color(0xFFF1F8E9)),
        start = Offset(0f, 0f), end = Offset(1000f, 1000f)
    )

    val topContent: @Composable ColumnScope.() -> Unit = {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                    Text("🤔", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
                Text(
                    text = "정말 멈추시겠어요?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "지금까지 잘 해오셨는데...",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(16.dp))
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
    }

    val bottomButtons: @Composable RowScope.() -> Unit = {
        var isPressed by remember { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()

        // 중지 버튼 그룹
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(106.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(106.dp),
                color = Color(0xFFE0E0E0),
                strokeWidth = 4.dp,
                trackColor = Color.Transparent
            )
            if (isPressed) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(106.dp),
                    color = Color(0xFFD32F2F),
                    strokeWidth = 4.dp,
                    trackColor = Color.Transparent
                )
            }
            Card(
                modifier = Modifier
                    .size(96.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            isPressed = true
                            progress = 0f
                            val job = coroutineScope.launch {
                                val duration = 1500L
                                val startMs = System.currentTimeMillis()
                                while (progress < 1f && isPressed) {
                                    val elapsed = System.currentTimeMillis() - startMs
                                    progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                    delay(16)
                                }
                                if (progress >= 1f && isPressed) {
                                    saveCompletedRecord(
                                        context = context,
                                        startTime = System.currentTimeMillis() - (elapsedDays * 24L * 60 * 60 * 1000),
                                        endTime = System.currentTimeMillis(),
                                        targetDays = targetDays,
                                        actualDays = elapsedDays
                                    )
                                    sharedPref.edit {
                                        remove("start_time")
                                        putBoolean("timer_completed", true)
                                    }
                                    context.startActivity(Intent(context, StartActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    })
                                    (context as? QuitActivity)?.finish()
                                }
                            }
                            val up = waitForUpOrCancellation()
                            isPressed = false
                            job.cancel()
                            if (up != null && progress < 1f) {
                                Toast.makeText(
                                    context,
                                    "길게 눌러 완료하세요 (${String.format(Locale.getDefault(),"%d", (progress * 100).toInt())}%)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            coroutineScope.launch {
                                while (progress > 0f) {
                                    progress = (progress - 0.1f).coerceAtLeast(0f)
                                    delay(16)
                                }
                            }
                        }
                    },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = if (isPressed) Color(0xFFD32F2F) else Color(0xFFE53935)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "중지", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
        }
        // 계속 버튼
        Card(
            onClick = { (context as? QuitActivity)?.finish() },
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = "계속", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (activity != null) {
            activity.StandardScreenLayout(topContent = topContent, bottomButtons = bottomButtons)
        } else {
            // Preview fallback: 동일 구성 (임시 데이터 생성 없음)
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxWidth()) { topContent() }
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { bottomButtons() }
            }
        }
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
            modifier = modifier.size(90.dp),
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
                    modifier = Modifier.size(45.dp)
                )
            }
        }
    } else {
        // onClick이 없는 경우 (Preview용) 기존 방식 사용
        Card(
            modifier = modifier.size(90.dp),
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
                    modifier = Modifier.size(45.dp)
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

// Preview 함수 추가
@Preview(showBackground = true)
@Composable
fun QuitScreenPreview() { QuitScreen() }
