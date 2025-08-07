package com.example.alcoholictimer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.Constants

class TestActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                BaseScreen {
                    SettingsScreen()
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 앱 종료 방지
            finish()
        }
    }

    override fun getScreenTitle(): String = "설정"

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        var selectedMode by remember { mutableStateOf(Constants.TEST_MODE_REAL) }

        // 현재 저장된 설정 로드
        LaunchedEffect(Unit) {
            try {
                val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                selectedMode = sharedPref.getInt(Constants.PREF_KEY_TEST_MODE, Constants.TEST_MODE_REAL)
            } catch (_: Exception) {
                selectedMode = Constants.TEST_MODE_REAL
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "설정",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "테스트 모드 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeButton(
                    label = "실제 시간",
                    selected = selectedMode == Constants.TEST_MODE_REAL,
                    onClick = { selectedMode = Constants.TEST_MODE_REAL }
                )
                ModeButton(
                    label = "분 단위",
                    selected = selectedMode == Constants.TEST_MODE_MINUTE,
                    onClick = { selectedMode = Constants.TEST_MODE_MINUTE }
                )
                ModeButton(
                    label = "초 단위",
                    selected = selectedMode == Constants.TEST_MODE_SECOND,
                    onClick = { selectedMode = Constants.TEST_MODE_SECOND }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    try {
                        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putInt(Constants.PREF_KEY_TEST_MODE, selectedMode)
                            apply()
                        }
                        // Constants 업데이트
                        Constants.updateTestMode(selectedMode)
                        Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "설정 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("저장", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                contentColor = if (selected) Color.White else Color.Black
            ),
            modifier = Modifier.height(48.dp)
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewSettingsScreen() {
        BaseScreen {
            SettingsScreen()
        }
    }
}
