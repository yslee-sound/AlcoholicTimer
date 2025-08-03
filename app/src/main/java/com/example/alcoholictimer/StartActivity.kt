package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.alcoholictimer.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton

class StartActivity : BaseActivity() {
    private lateinit var tvDaysLabel: TextView  // tvTimeUnit을 tvDaysLabel로 변경

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity에서 이미 햄버거 메뉴 및 네비게이션 기능 처리됨
    }

    override fun setupContentView() {
        // StartActivity 고유의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        val view = LayoutInflater.from(this).inflate(R.layout.content_start, contentFrame, true)

        // UI 요소 초기화
        tvDaysLabel = view.findViewById(R.id.tvDaysLabel)  // ID 변경
        val editTextDays = view.findViewById<EditText>(R.id.editTextDays)
        val btnStart = view.findViewById<FloatingActionButton>(R.id.btnStart)

        // 초기 시간 단위 텍스트 설정
        updateTimeModeDisplay()

        // 시작 버튼 클릭 처리
        btnStart.setOnClickListener {
            val targetTime = editTextDays.text.toString().toIntOrNull() ?: 0

            if (targetTime > 0) {
                // 사용자 설정을 SharedPreferences에 저장
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt("target_days", targetTime)

                    // 현재 시간을 시작 시간으로 저장
                    putLong("start_time", System.currentTimeMillis())
                    apply()
                }

                Toast.makeText(this, "${targetTime}${Constants.TIME_UNIT_TEXT} 동안 금주를 시작합니다!", Toast.LENGTH_SHORT).show()
                navigateToStatus()
            } else {
                Toast.makeText(this, "1${Constants.TIME_UNIT_TEXT} 이상의 숫자를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToStatus() {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        updateTimeModeDisplay()
    }

    private fun updateTimeModeDisplay() {
        val timeUnitText = when {
            Constants.isSecondTestMode -> "금주 목표 초수"
            Constants.isMinuteTestMode -> "금주 목표 분수"
            else -> "금주 목표 일수"
        }
        tvDaysLabel.text = timeUnitText
    }
}
