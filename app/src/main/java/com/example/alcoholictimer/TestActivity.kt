package com.example.alcoholictimer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    override fun getScreenTitle(): String = "테스트"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                BaseScreen {
                    TestScreen()
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 앱 종료 방지
            finish()
        }
    }
}

@Composable
fun TestScreen() {
    var selectedMode by remember { mutableStateOf(Constants.TEST_MODE_REAL) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 시간 모드 설정
        Text("시간 모드", fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_REAL,
                    onClick = { selectedMode = Constants.TEST_MODE_REAL }
                )
                Text("실제 시간 모드", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_MINUTE,
                    onClick = { selectedMode = Constants.TEST_MODE_MINUTE }
                )
                Text("분 단위 테스트 모드", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_SECOND,
                    onClick = { selectedMode = Constants.TEST_MODE_SECOND }
                )
                Text("초 단위 테스트 모드", fontSize = 16.sp)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Black
        )

        // 기록 초기화 버튼
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = {
                    val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()

                    // 기존 기록 확인
                    val beforeRecords = sharedPref.getString("sobriety_records", "[]")
                    android.util.Log.d("TestActivity", "초기화 전 기록: $beforeRecords")

                    // 모든 데이터 삭제
                    editor.clear()

                    // 명시적으로 sobriety_records 키도 삭제
                    editor.remove("sobriety_records")
                    editor.apply()

                    // 삭제 후 확인
                    val afterRecords = sharedPref.getString("sobriety_records", "[]")
                    android.util.Log.d("TestActivity", "초기화 후 기록: $afterRecords")

                    Toast.makeText(context, "모든 기록이 초기화되었습니다", Toast.LENGTH_SHORT).show()
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                Text("모든 기록 초기화", fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // 적용 버튼
            OutlinedButton(
                onClick = {
                    val sharedPref = context.getSharedPreferences("test_settings", android.content.Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putInt("test_mode", selectedMode)
                        apply()
                    }
                    Toast.makeText(context, "설정이 적용되었습니다", Toast.LENGTH_SHORT).show()
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                Text("적용", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = Color.Black
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color.Black else Color.Transparent,
            contentColor = if (selected) Color.White else Color.Black
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, fontSize = 16.sp)
    }
}

private fun getModeText(mode: Int): String {
    return when (mode) {
        Constants.TEST_MODE_REAL -> "실제 시간"
        Constants.TEST_MODE_MINUTE -> "분 단위 테스트"
        Constants.TEST_MODE_SECOND -> "초 단위 테스트"
        else -> "알 수 없음"
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTestScreen() {
    TestScreen()
}
