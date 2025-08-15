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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
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
        // 레벨 테스트 모드 설정 (금주 진행에는 영향 없음)
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
            Text("레벨 테스트 모드", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "※ 이 설정은 레벨 계산에만 영향을 미치며, 실제 금주 진행 시간은 변경되지 않습니다.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_REAL,
                    onClick = { selectedMode = Constants.TEST_MODE_REAL }
                )
                Column {
                    Text("실제 시간 모드", fontSize = 16.sp)
                    Text("레벨 계산: 1일 = 24시간", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_MINUTE,
                    onClick = { selectedMode = Constants.TEST_MODE_MINUTE }
                )
                Column {
                    Text("분 단위 레벨 테스트", fontSize = 16.sp)
                    Text("레벨 계산: 1분 = 1일 (테스트용)", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedMode == Constants.TEST_MODE_SECOND,
                    onClick = { selectedMode = Constants.TEST_MODE_SECOND }
                )
                Column {
                    Text("초 단위 레벨 테스트", fontSize = 16.sp)
                    Text("레벨 계산: 1초 = 1일 (테스트용)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Black
        )

        // 기록 초기화 버튼
        var showDialog by remember { mutableStateOf(false) }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(
                onClick = {
                    showDialog = true
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                    Text("모든 기록 초기화")
                }
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("기록 초기화") },
                text = { Text("모든 기록을 초기화하시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = {
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        val beforeRecords = sharedPref.getString("sobriety_records", "[]")
                        android.util.Log.d("TestActivity", "초기화 전 기록: $beforeRecords")
                        editor.clear()
                        editor.remove("sobriety_records")
                        editor.apply()
                        val afterRecords = sharedPref.getString("sobriety_records", "[]")
                        android.util.Log.d("TestActivity", "초기화 후 기록: $afterRecords")
                        Toast.makeText(context, "모든 기록이 초기화되었습니다", Toast.LENGTH_SHORT).show()
                        showDialog = false
                    }) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }

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

@Preview(showBackground = true, name = "fontScale 1.0", fontScale = 1.0f)
@Preview(showBackground = true, name = "fontScale 2.0", fontScale = 2.0f)
@Composable
fun PreviewTestScreen() {
    TestScreen()
}
