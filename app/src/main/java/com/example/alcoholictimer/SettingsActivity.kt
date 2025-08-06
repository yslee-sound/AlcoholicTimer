package com.example.alcoholictimer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.alcoholictimer.utils.Constants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                SettingsScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "설정"

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        var selectedMode by remember { mutableStateOf(Constants.TEST_MODE_REAL) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "테스트 모드 선택",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_REAL,
                    onClick = { selectedMode = Constants.TEST_MODE_REAL }
                )
                Text("실제 모드")
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_MINUTE,
                    onClick = { selectedMode = Constants.TEST_MODE_MINUTE }
                )
                Text("분 모드")
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_SECOND,
                    onClick = { selectedMode = Constants.TEST_MODE_SECOND }
                )
                Text("초 모드")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                saveSettings(selectedMode)
                Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }) {
                Text("저장", fontSize = 18.sp)
            }
        }
    }

    private fun saveSettings(selectedMode: Int) {
        val preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(Constants.PREF_KEY_TEST_MODE, selectedMode)
        val success = editor.commit() // 동기 저장으로 변경하고 성공 여부 확인
        Log.d("SettingsActivity", "Settings saved. Mode: $selectedMode, Success: $success")

        // Constants 클래스의 동적 설정 업데이트
        Constants.updateTestMode(selectedMode)
    }

    @Composable
    fun SettingsScreenPreview() {
        BaseScreen {
            SettingsScreen()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewSettingsScreen() {
        SettingsScreenPreview()
    }
}
