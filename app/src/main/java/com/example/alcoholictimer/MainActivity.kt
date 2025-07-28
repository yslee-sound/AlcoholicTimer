package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.alcoholictimer.utils.Constants

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 금주 타이머가 진행 중인지 확인
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val isSobrietyStarted = sharedPref.contains("start_time")

        // 금주가 진행 중이면 StatusActivity로 자동 이동
        if (isSobrietyStarted) {
            val intent = Intent(this, StatusActivity::class.java)
            startActivity(intent)
            // MainActivity는 종료하지 않고 백스택에 남겨둠
        }
    }

    override fun setupContentView() {
        // MainActivity의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        LayoutInflater.from(this).inflate(R.layout.content_main, contentFrame, true)
    }
}
