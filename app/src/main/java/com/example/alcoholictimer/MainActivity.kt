package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 금주 타이머가 진행 중인지 확인하고 적절한 화면으로 이동
        checkCurrentStateAndNavigate()
    }

    override fun getScreenTitle(): String = "금주 타이머"

    @Composable
    private fun MainScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alcoholic Timer",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = {
                val intent = Intent(this@MainActivity, StartActivity::class.java)
                startActivity(intent)
            }) {
                Text("시작하기")
            }
        }
    }

    private fun checkCurrentStateAndNavigate() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val isSobrietyStarted = sharedPref.contains("start_time")

        if (isSobrietyStarted) {
            // 금주가 진행 중이면 StatusActivity로 이동
            val intent = Intent(this, StatusActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else {
            // 금주가 시작되지 않았으면 StartActivity로 이동
            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        // MainActivity는 백스택에서 제거
        finish()
        overridePendingTransition(0, 0)
    }
}
