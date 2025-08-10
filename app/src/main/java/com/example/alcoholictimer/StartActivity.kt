package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

    @Composable
    private fun StartScreen() {
        val context = LocalContext.current
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = "5.0",
                    selection = TextRange(0, 3) // 초기에 전체 선택
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // 상단 아이콘
            Text(
                text = "🍃",
                fontSize = 150.sp,
                modifier = Modifier.padding(bottom = 60.dp)
            )

            // 입력 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 목표 입력 텍스트
                Text(
                    text = "목표 설정",
                    fontSize = 24.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 커스텀 입력 필드 (3자리 숫자 넓이)
                Box(
                    modifier = Modifier.width(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Box(
                            modifier = Modifier.fillMaxWidth()
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
                                            selection = TextRange(finalText.length) // 커서를 끝으로 이동
                                        )
                                        isTextSelected = false // 선택 상태 해제
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
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                    }
                            )
                        }

                        // 밑줄 (얇고 검은색)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        ) {
                            drawLine(
                                color = Color.Black,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Square
                            )
                        }
                    }
                }

                // 단위 표시
                Text(
                    text = "일",
                    fontSize = 20.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // 플레이 버튼
            FloatingActionButton(
                onClick = {
                    val targetTime = textFieldValue.text.toFloatOrNull() ?: 0f
                    if (targetTime > 0) {
                        // 극소수점 값도 보존하도록 소수점 자리수 증가
                        val formattedTargetTime = String.format("%.6f", targetTime).toFloat()
                        // Use formattedTargetTime for further processing
                        val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putFloat("target_days", formattedTargetTime) // Float로 저장
                            putLong("start_time", System.currentTimeMillis())
                            putBoolean("timer_completed", false)
                            apply()
                        }

                        // 디버깅 로그 추가
                        android.util.Log.d("StartActivity", "입력값: $targetTime")
                        android.util.Log.d("StartActivity", "포맷된 값: $formattedTargetTime")

                        val intent = Intent(context, RunActivity::class.java)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                containerColor = if (isValid) MaterialTheme.colorScheme.primary else Color.Gray
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "시작",
                    modifier = Modifier.size(50.dp),
                    tint = Color.White
                )
            }
        }
    }

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

    @Preview(showBackground = true)
    @Composable
    fun PreviewStartScreen() {
        BaseScreen {
            StartScreen()
        }
    }
}
