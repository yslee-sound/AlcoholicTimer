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
        var inputText by remember { mutableStateOf("0") }
        var errorText by remember { mutableStateOf("") }
        val timeUnitText = Constants.TIME_UNIT_TEXT
        val isValid = inputText.toIntOrNull()?.let { it > 0 } ?: false

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
                        BasicTextField(
                            value = inputText,
                            onValueChange = { newValue ->
                                // 숫자만 허용하고, 첫 번째 문자가 0이면서 길이가 1보다 크면 0을 제거
                                val filteredValue = newValue.filter { it.isDigit() }
                                inputText = if (filteredValue.length > 1 && filteredValue.startsWith("0")) {
                                    filteredValue.substring(1)
                                } else {
                                    filteredValue
                                }
                                errorText = ""
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 48.sp,
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
