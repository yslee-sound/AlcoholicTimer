package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
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

    override fun getScreenTitle(): String = "목표 설정"

    @Composable
    private fun StartScreen() {
        val context = LocalContext.current
        var targetTime by remember { mutableStateOf(0) }
        var inputText by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf("") }
        val timeUnitText = Constants.TIME_UNIT_TEXT

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "금주 목표 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "목표 ${timeUnitText} 입력",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    targetTime = it.toIntOrNull() ?: 0
                },
                label = { Text("목표 ${timeUnitText}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (errorText.isNotEmpty()) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (targetTime > 0) {
                    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putInt("target_days", targetTime)
                        putLong("start_time", System.currentTimeMillis())
                        putBoolean("timer_completed", false)
                        apply()
                    }
                    // StatusActivity로 이동
                    val intent = Intent(context, StatusActivity::class.java)
                    context.startActivity(intent)
                } else {
                    errorText = "목표 ${timeUnitText}를 올바르게 입력하세요."
                }
            }) {
                Text("시작", fontSize = 18.sp)
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
}
