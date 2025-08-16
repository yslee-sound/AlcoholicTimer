package com.example.alcoholictimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember

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

    // SharedPreferences에서 데이터 가져오기
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val targetDays = sharedPref.getFloat("target_days", 30f)

    // 설정값 가져오기
    val selectedCost = sharedPref.getString("selected_cost", "�����") ?: "중"
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
            .padding(horizontal = 16.dp, vertical = 24.dp),
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
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 이모지 아이콘
                    CompositionLocalProvider(
                        LocalDensity provides Density(LocalDensity.current.density, 1f)
                    ) {
                        Text(
                            text = "🤔",
                            fontSize = 60.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

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
                lifeGainDays = lifeGainDays
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
                contentDescription = "중지",
                modifier = Modifier.customLongPress(
                    durationMillis = 3000L, // 3초
                    onClick = {
                        Toast.makeText(context, "중지하려면 길게 누르세요", Toast.LENGTH_SHORT).show()
                    },
                    onLongPress = {
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

                        // StartActivity로 이동
                        val intent = Intent(context, StartActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        context.startActivity(intent)
                        (context as? QuitActivity)?.overridePendingTransition(0, 0)
                        (context as? QuitActivity)?.finish()
                    }
                )
            )

            Spacer(modifier = Modifier.width(48.dp))

            // 계속 버튼용 InteractionSource
            val continueButtonInteractionSource = remember { MutableInteractionSource() }

            // 계속 버튼
            ModernControlButton(
                icon = Icons.Default.PlayArrow,
                backgroundColor = Color(0xFF4CAF50),
                contentDescription = "계속",
                interactionSource = continueButtonInteractionSource,
                modifier = Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()

                        // Press interaction 시작
                        val pressInteraction = PressInteraction.Press(down.position)
                        continueButtonInteractionSource.tryEmit(pressInteraction)

                        val upOrCancel = waitForUpOrCancellation()

                        // Press interaction 종료
                        continueButtonInteractionSource.tryEmit(
                            PressInteraction.Release(pressInteraction)
                        )

                        // 클릭 처리
                        if (upOrCancel != null) {
                            (context as? QuitActivity)?.finish()
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun StatisticsCardsSection(
    elapsedDays: Int,
    elapsedHours: Int,
    elapsedMinutes: Int,
    savedMoney: Int,
    savedHours: Int,
    lifeGainDays: Int
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
                value = "${elapsedDays}일",
                label = "금주 일수",
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            ModernStatCard(
                value = getLevelName(elapsedDays),
                label = "레벨",
                color = LevelDefinitions.getLevelInfo(elapsedDays).color,
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
                value = String.format(Locale.getDefault(), "%,d원", savedMoney),
                label = "절약 금액",
                color = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
        }

        // ��� ���째 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard(
                value = "${savedHours}시간",
                label = "절약 시간",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            ModernStatCard(
                value = "${lifeGainDays}일",
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Card(
        modifier = modifier
            .size(80.dp)
            .indication(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 40.dp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            ),
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

// 기록 저장 함수
private fun saveCompletedRecord(
    context: Context,
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int
) {
    try {
        Log.d("QuitActivity", "========== 기록 저��� 시작 ==========")
        Log.d("QuitActivity", "startTime: $startTime")
        Log.d("QuitActivity", "endTime: $endTime")
        Log.d("QuitActivity", "targetDays: $targetDays")
        Log.d("QuitActivity", "actualDays: $actualDays")

        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        val recordId = System.currentTimeMillis().toString()
        Log.d("QuitActivity", "생성된 ����록 ID: $recordId")

        // 목표 달성률 계산
        val achievementRate = if (targetDays > 0) {
            (actualDays.toFloat() / targetDays) * 100
        } else {
            0f
        }

        // 목표 달성 여부 확인
        val isCompleted = achievementRate >= 100f
        val status = if (isCompleted) "완료" else "중지"

        Log.d("QuitActivity", "달성����: ${achievementRate}%, 완료 여부: $isCompleted, 상태: $status")

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
        Log.d("QuitActivity", "����� 기록들: $recordsJson")

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

        val message = "금주 ���록이 저장되었습니다."
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
    val savedMoney = 600000
    val savedHours = 135
    val lifeGainDays = 0

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
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 상����� ��시��� 카드
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
                lifeGainDays = lifeGainDays
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // 버튼 영���
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
            savedMoney = 600000,
            savedHours = 135,
            lifeGainDays = 0
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
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernControlButton(
            icon = Icons.Default.Close,
            backgroundColor = Color(0xFFE53935),
            contentDescription = "중지"
        )
        ModernControlButton(
            icon = Icons.Default.PlayArrow,
            backgroundColor = Color(0xFF4CAF50),
            contentDescription = "계속"
        )
    }
}

// 롱프레스 Modifier 확장 함수 - 시각적 피드백 포함
fun Modifier.customLongPress(
    durationMillis: Long = 3000L,
    onLongPress: () -> Unit,
    onClick: () -> Unit = {},
    interactionSource: MutableInteractionSource? = null
): Modifier = this.then(
    Modifier.pointerInput(durationMillis, interactionSource) {
        awaitEachGesture {
            val down = awaitFirstDown()

            // Press interaction 시작 - 시각적 피드백을 위해
            val pressInteraction = PressInteraction.Press(down.position)
            interactionSource?.tryEmit(pressInteraction)

            val longPressResult = withTimeoutOrNull(durationMillis) {
                waitForUpOrCancellation()
            }

            // Press interaction 종료
            interactionSource?.tryEmit(
                PressInteraction.Release(pressInteraction)
            )

            if (longPressResult == null) {
                // 롱프레스 성공
                onLongPress()
            } else {
                // 일반 클릭
                onClick()
            }
        }
    }
)
