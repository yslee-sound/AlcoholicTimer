package com.example.alcoholictimer

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.Constants

class StartActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                StartScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "금주 설정"

    override fun onResume() {
        super.onResume()
        updateTimeModeDisplay()
    }

    private fun updateTimeModeDisplay() {
        // 테스트 모드는 레벨에만 영향을 미치므로 항상 "일수"로 표시
        // SharedPreferences에서 현재 테스트 모드를 읽어옴 (레벨 계산용으로만 사용)
        val sharedPref = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val currentTestMode = sharedPref.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        // Constants의 현재 테스트 모드를 업데이트 (레벨 계산용)
        Constants.updateTestMode(currentTestMode)

        // 목표 입력은 항상 일수로 표시 (금주 진행은 실제 시간 기준)
        val timeUnitText = "금주 목표 일수"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen() {
    val context = LocalContext.current
    val activity = context as? StartActivity

    // SharedPreferences에서 금주 진행 여부 확인
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    // 이미 금주가 진행 중이면 RunActivity로 이동
    if (startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            val intent = Intent(context, RunActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
            if (context is StartActivity) {
                context.finish()
            }
        }
        return
    }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = "30",
                selection = TextRange(0, 2) // 초기에 전체 선택
            )
        )
    }
    val isValid = textFieldValue.text.toFloatOrNull()?.let { it > 0 } ?: false

    // 텍스트가 선택된 상태인지 추적
    var isTextSelected by remember { mutableStateOf(true) }

    // 포커스 상태 추적
    var isFocused by remember { mutableStateOf(false) }

    // 포커스가 변경될 때 텍스트 선택 처리
    LaunchedEffect(isFocused) {
        if (isFocused) {
            kotlinx.coroutines.delay(50) // 약간의 지연 후 선택
            textFieldValue = textFieldValue.copy(
                selection = TextRange(0, textFieldValue.text.length)
            )
            isTextSelected = true
        }
    }

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
            .windowInsetsPadding(WindowInsets.safeDrawing) // 추가
            .padding(horizontal = 16.dp, vertical = 16.dp), // 통일된 패딩
    ) {
        // 표준 레이아웃 사용
        activity?.StandardScreenLayout(
            topContent = {
                Spacer(modifier = Modifier.height(0.dp))

                // 목표 설정 카드
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
                    // 중앙 정렬을 위해 Column에 fillMaxWidth와 horizontalAlignment 추가
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "목표 기간 설정",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 32.dp)
                        )

                        // 입력 필드 영역
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            // 커스텀 입력 필드
                            Card(
                                modifier = Modifier.width(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = textFieldValue,
                                        onValueChange = { newValue ->
                                            // 소수점 입력을 허용하는 필터링
                                            val filteredValue = newValue.text.filter { it.isDigit() || it == '.' }

                                            // 소수점이 여러 개 있는지 검사
                                            val dotCount = filteredValue.count { it == '.' }
                                            val finalFilteredValue = if (dotCount <= 1) filteredValue else textFieldValue.text

                                            if (isTextSelected && finalFilteredValue.isNotEmpty()) {
                                                // 전체 선택 상태에서 새 숫자 입력 시 완전 교체
                                                val finalText = if (finalFilteredValue.length > 1 && finalFilteredValue.startsWith("0") && !finalFilteredValue.startsWith("0.")) {
                                                    finalFilteredValue.substring(1)
                                                } else {
                                                    finalFilteredValue
                                                }
                                                textFieldValue = TextFieldValue(
                                                    text = finalText,
                                                    selection = TextRange(finalText.length)
                                                )
                                                isTextSelected = false
                                            } else {
                                                // 일반적인 편집
                                                val finalText = if (finalFilteredValue.isEmpty()) {
                                                    "0"
                                                } else if (finalFilteredValue.length > 1 && finalFilteredValue.startsWith("0") && !finalFilteredValue.startsWith("0.")) {
                                                    finalFilteredValue.substring(1)
                                                } else {
                                                    finalFilteredValue
                                                }
                                                textFieldValue = TextFieldValue(
                                                    text = finalText,
                                                    selection = TextRange(finalText.length)
                                                )
                                                isTextSelected = false
                                            }
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF1976D2)
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Color(0xFF1976D2)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                isFocused = focusState.isFocused
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // 단위 표시
                            Text(
                                text = "일",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )
                        }

                        // 안내 메시지
                        Text(
                            text = "금주할 목표 기간을 입력해주세요",
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            },
            bottomButtons = {
                ModernStartButton(
                    isEnabled = isValid,
                    onStart = {
                        val targetTime = textFieldValue.text.toFloatOrNull() ?: 0f
                        if (targetTime > 0) {
                            val formattedTargetTime = String.format("%.6f", targetTime).toFloat()
                            val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
                            sharedPref.edit().apply {
                                putFloat("target_days", formattedTargetTime)
                                putLong("start_time", System.currentTimeMillis())
                                putBoolean("timer_completed", false)
                                apply()
                            }

                            val intent = Intent(context, RunActivity::class.java)
                            context.startActivity(intent)
                            (context as? StartActivity)?.finish()
                        }
                    }
                )
            }
        )
    }
}

@Composable
fun ModernStartButton(
    isEnabled: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { if (isEnabled) onStart() },
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFCCCCCC)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEnabled) 8.dp else 2.dp
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "시작",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// Preview 컴포넌트들

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    StartScreen()
}
