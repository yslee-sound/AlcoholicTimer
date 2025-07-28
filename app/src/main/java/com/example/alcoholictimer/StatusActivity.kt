package com.example.alcoholictimer

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.alcoholictimer.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton

class StatusActivity : BaseActivity() {
    private val levelMilestones = listOf(0, 7, 14, 30, 60, 120, 240, 365)
    private val levelTitles = listOf(
        "새싹 도전자",
        "첫걸음 성공",
        "의지의 시작",
        "한달의 기적",
        "습관의 탄생",
        "의지의 달인",
        "금주의 마스터",
        "절제의 달인"
    )
    private val levelColors = listOf(
        "#FF9E9E9E",  // Gray
        "#FF4CAF50",  // Green
        "#FF2196F3",  // Blue
        "#FF9C27B0",  // Purple
        "#FFFF9800",  // Orange
        "#FFFF5722",  // Deep Orange
        "#FF3F51B5",  // Indigo
        "#FFE91E63"   // Pink
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity에서 이미 햄버거 메뉴 및 네비게이션 기능 처리됨
    }

    override fun setupContentView() {
        // StatusActivity 고유의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        val view = LayoutInflater.from(this).inflate(R.layout.content_status, contentFrame, true)

        val tvDaysCount = view.findViewById<TextView>(R.id.tvDaysCount)
        val tvTimeUnit = view.findViewById<TextView>(R.id.tvTimeUnit)
        val tvLevel = view.findViewById<TextView>(R.id.tvLevel)
        val tvLevelTitle = view.findViewById<TextView>(R.id.tvLevelTitle)
        val tvNextLevel = view.findViewById<TextView>(R.id.tvNextLevel)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val progressLevel = view.findViewById<ProgressBar>(R.id.progressLevel)

        // 시간 단위 텍스트 설정
        tvTimeUnit.text = Constants.TIME_UNIT_TEXT

        // 중지 버튼 설정
        val btnStopSobriety = view.findViewById<FloatingActionButton>(R.id.btnStopSobriety)
        btnStopSobriety.setOnClickListener {
            // 기존 AlertDialog 대신 커스텀 다이얼로그 사용
            showCustomStopDialog()
        }

        // SharedPreferences에서 데이터 불러오기
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val targetDays = sharedPref.getInt("target_days", 30)

        // 경과 시간 계산 (테스트 모드에 따라 일 또는 분 단위로 계산)
        val timePassed = ((System.currentTimeMillis() - startTime) / Constants.TIME_UNIT_MILLIS).toInt()

        // UI 업데이트
        tvDaysCount.text = timePassed.toString()

        // 레벨 계산 - 테스트 모드에 따라 레벨 마일스톤 적용
        var currentLevel = 0
        val adjustedMilestones = if (Constants.TEST_MODE) {
            // 테스트용 분 단위 마일스톤 (빠르게 테스트 가능하도록)
            listOf(0, 1, 2, 5, 10, 15, 20, 30)
        } else {
            levelMilestones
        }

        for (i in adjustedMilestones.indices) {
            if (timePassed >= adjustedMilestones[i]) {
                currentLevel = i
            } else {
                break
            }
        }

        // 레벨 정보 표시
        tvLevel.text = "Lv. ${currentLevel + 1}"
        tvLevelTitle.text = levelTitles[currentLevel]
        tvLevelTitle.setTextColor(Color.parseColor(levelColors[currentLevel]))

        // 다음 레벨 정보
        if (currentLevel < adjustedMilestones.size - 1) {
            val nextLevelTime = adjustedMilestones[currentLevel + 1]
            val timeLeft = nextLevelTime - timePassed
            tvNextLevel.text = "다음 레벨까지 ${timeLeft}${Constants.TIME_UNIT_TEXT}"

            // 프로그레스바 업데이트
            val currentLevelTime = adjustedMilestones[currentLevel]
            val nextLevelThreshold = adjustedMilestones[currentLevel + 1]
            val progress = ((timePassed - currentLevelTime).toFloat() / (nextLevelThreshold - currentLevelTime)) * 100
            progressLevel.progress = progress.toInt()
        } else {
            tvNextLevel.text = "최고 레벨 달성!"
            progressLevel.progress = 100
        }

        // 목표 달성 여부 메시지
        if (timePassed >= targetDays) {
            tvMessage.text = "축하합니다! ${targetDays}${Constants.TIME_UNIT_TEXT} 목표를 달성했습니다!"
        } else {
            tvMessage.text = "목표까지 ${targetDays - timePassed}${Constants.TIME_UNIT_TEXT} 남았습니다. 힘내세요!"
        }
    }

    /**
     * 눈에 잘 보이는 버튼이 있는 커스텀 중지 확인 다이얼로그를 표시합니다.
     */
    private fun showCustomStopDialog() {
        // 커스텀 다이얼로그 생성
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_stop_sobriety)

        // 다이얼로그 배경을 투명하게 설정하고 외부 영역 클릭으로 닫히지 않게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        // 취소 버튼 클릭 리스너
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 확인 버튼 클릭 리스너
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            // SharedPreferences 데이터 초기화
            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear()  // 모든 데이터 삭제
                apply()
            }

            Toast.makeText(this, "금주가 초기화되었습니다.", Toast.LENGTH_SHORT).show()

            // 메인 화면으로 이동
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // 다이얼로그 표시
        dialog.show()
    }
}
