package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 금주 타이머가 진행 중인지 확인하고 적절한 화면으로 이동
        checkCurrentStateAndNavigate()
    }

    override fun setupContentView() {
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        LayoutInflater.from(this).inflate(R.layout.content_main, contentFrame, true)
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
