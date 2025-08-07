package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
        var inputText by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf("") }
        val timeUnitText = Constants.TIME_UNIT_TEXT
        val isValid = inputText.toIntOrNull()?.let { it > 0 } ?: false

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 상단 아이콘만 유지
            Text(
                text = "🍃",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 80.dp)
            )

            // 입력 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 목표 입력 텍스트 (간소화)
                Text(
                    text = "목표 ${timeUnitText}",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 커스텀 입력 필드
                Box(
                    modifier = Modifier.width(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        BasicTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                errorText = ""
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isValid) MaterialTheme.colorScheme.primary else Color.Black
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        // 밑줄
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        ) {
                            drawLine(
                                color = if (!isValid && inputText.isNotEmpty())
                                    Color.Red
                                else if (isValid)
                                    androidx.compose.ui.graphics.Color(0xFF6200EA)
                                else
                                    Color.Gray,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // 힌트 텍스트만 유지
                if (inputText.isEmpty()) {
                    Text(
                        text = "예: 30",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // 에러 메시지 (최소화)
                if (!isValid && inputText.isNotEmpty()) {
                    Text(
                        text = "1 이상의 숫자를 입력하세요",
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // 플레이 버튼만 유지
            FloatingActionButton(
                onClick = {
                    val targetTime = inputText.toIntOrNull() ?: 0
                    if (targetTime > 0) {
                        val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putInt("target_days", targetTime)
                            putLong("start_time", System.currentTimeMillis())
                            putBoolean("timer_completed", false)
                            apply()
                        }
                        val intent = Intent(context, StatusActivity::class.java)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                containerColor = if (isValid) MaterialTheme.colorScheme.primary else Color.Gray
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "시작",
                    modifier = Modifier.size(40.dp),
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
        // SharedPreferences에서 현재 테스트 모드를 읽어옴
        val sharedPref = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val currentTestMode = sharedPref.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        // Constants의 현재 테스트 모드를 업데이트
        Constants.updateTestMode(currentTestMode)

        val timeUnitText = when (currentTestMode) {
            Constants.TEST_MODE_SECOND -> "금주 목표 초수"
            Constants.TEST_MODE_MINUTE -> "금주 목표 분수"
            else -> "금주 목표 일수"
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewStartScreen() {
        BaseScreen {
            StartScreen()
        }
    }
}
