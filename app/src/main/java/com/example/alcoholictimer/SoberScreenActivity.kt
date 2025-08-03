package com.example.alcoholictimer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alcoholictimer.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class SoberScreenActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isTimerRunning = false
    private lateinit var tvTimeDetail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_status)

        tvTimeDetail = findViewById(R.id.tvTimeDetail)

        // 타이머 시작 버튼 설정
        val btnStartTimer = findViewById<FloatingActionButton>(R.id.btnStopSobriety)
        btnStartTimer.setOnClickListener {
            if (!isTimerRunning) {
                startTimer()
            }
        }
    }

    private fun startTimer() {
        elapsedSeconds = 0 // 타이머 초기화
        isTimerRunning = true // 타이머 상태 변경
        handler.post(timerRunnable) // Runnable 실행
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                tvTimeDetail.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                elapsedSeconds++
                handler.postDelayed(this, 1000) // 1초마다 업데이트
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable) // 핸들러 콜백 제거
        isTimerRunning = false // 타이머 상태 초기화
    }

    // 결과 화면으로 넘어가기 위한 딜레이 코드 예시
    private fun navigateToResultScreen() {
        handler.postDelayed({
            // ...화면 전환 코드...
        }, Constants.RESULT_SCREEN_DELAY.toLong()) // 상수 사용
    }
}
