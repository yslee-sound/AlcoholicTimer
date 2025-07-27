package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class StartActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity에서 이미 햄버거 메뉴 및 네비게이션 기능 처리됨
    }

    override fun setupContentView() {
        // StartActivity 고유의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        val view = LayoutInflater.from(this).inflate(R.layout.content_start, contentFrame, true)

        // 버튼 및 기타 UI 요소에 대한 이벤트 처리
        val btnStart = view.findViewById<Button>(R.id.btnStart)
        val editTextDays = view.findViewById<EditText>(R.id.editTextDays)

        btnStart.setOnClickListener {
            val days = editTextDays.text.toString().toIntOrNull() ?: 0

            if (days > 0) {
                // 사용자 설정을 SharedPreferences에 저장
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt("target_days", days)
                    putLong("start_time", System.currentTimeMillis())
                    apply()
                }

                Toast.makeText(this, "${days}일 동안 금주를 시작합니다!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, StatusActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "1일 이상의 숫자를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
