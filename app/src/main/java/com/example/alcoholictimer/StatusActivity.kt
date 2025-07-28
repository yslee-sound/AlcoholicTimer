package com.example.alcoholictimer

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.alcoholictimer.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Timer
import java.util.TimerTask

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

    // UI 업데이트를 위한 타이머 및 핸들러
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    // UI 요소 참조 저장 변수
    private lateinit var tvDaysCount: TextView
    private lateinit var tvTimeUnit: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvLevelTitle: TextView
    private lateinit var tvNextLevel: TextView
    private lateinit var tvMessage: TextView
    private lateinit var progressLevel: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity에서 이미 햄버거 메뉴 및 네비게이션 기능 처리됨
    }

    override fun onResume() {
        super.onResume()
        // 화면이 보일 때 타이머 시작
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        // 화면이 보이지 않을 때 타이머 정지
        stopTimer()
    }

    private fun startTimer() {
        // 기존 타이머가 있다면 중지
        stopTimer()

        // 새 타이머 생성 및 시작
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // UI 업데이트는 메인 스레드에서 수행해야 함
                handler.post {
                    updateUI()
                }
            }
        }, 0, 1000) // 1초마다 업데이트
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    override fun setupContentView() {
        // StatusActivity 고유의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        val view = LayoutInflater.from(this).inflate(R.layout.content_status, contentFrame, true)

        // UI 요소 초기화
        tvDaysCount = view.findViewById(R.id.tvDaysCount)
        tvTimeUnit = view.findViewById(R.id.tvTimeUnit)
        tvLevel = view.findViewById(R.id.tvLevel)
        tvLevelTitle = view.findViewById(R.id.tvLevelTitle)
        tvNextLevel = view.findViewById(R.id.tvNextLevel)
        tvMessage = view.findViewById(R.id.tvMessage)
        progressLevel = view.findViewById(R.id.progressLevel)

        // 시간 단위 텍스트 설정
        tvTimeUnit.text = Constants.TIME_UNIT_TEXT

        // 중지 버튼 설정
        val btnStopSobriety = view.findViewById<FloatingActionButton>(R.id.btnStopSobriety)
        btnStopSobriety.setOnClickListener {
            // 기존 AlertDialog 대신 커스텀 다이얼로그 사용
            showCustomStopDialog()
        }

        // 최초 UI 업데이트
        updateUI()
    }

    /**
     * UI를 업데이트하는 메서드 - 타이머에 의해 주기적으로 호출됨
     */
    private fun updateUI() {
        // SharedPreferences에서 데이터 불러오기
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val targetDays = sharedPref.getInt("target_days", 30)

        // 경과 시간 계산 (테스트 모드에 따라 일 또는 분 단위로 계산)
        val timePassed = ((System.currentTimeMillis() - startTime) / Constants.TIME_UNIT_MILLIS).toInt()

        // 프로그레스바를 위한 초 단위 경과 시간 계산
        val secondsPassed = ((System.currentTimeMillis() - startTime) / Constants.PROGRESS_TIME_UNIT_MILLIS).toInt()

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

            // 프로그레스바 업데이트 - 초 단위로 계산
            val currentLevelTimeInSeconds = if (Constants.PROGRESS_TEST_MODE) {
                adjustedMilestones[currentLevel] * (Constants.TIME_UNIT_MILLIS / Constants.SECOND_IN_MILLIS).toInt()
            } else {
                adjustedMilestones[currentLevel]
            }

            val nextLevelThresholdInSeconds = if (Constants.PROGRESS_TEST_MODE) {
                adjustedMilestones[currentLevel + 1] * (Constants.TIME_UNIT_MILLIS / Constants.SECOND_IN_MILLIS).toInt()
            } else {
                adjustedMilestones[currentLevel + 1]
            }

            val progressValue = if (Constants.PROGRESS_TEST_MODE) {
                ((secondsPassed - currentLevelTimeInSeconds).toFloat() / (nextLevelThresholdInSeconds - currentLevelTimeInSeconds)) * 100
            } else {
                ((timePassed - adjustedMilestones[currentLevel]).toFloat() / (adjustedMilestones[currentLevel + 1] - adjustedMilestones[currentLevel])) * 100
            }

            progressLevel.progress = progressValue.toInt().coerceIn(0, 100)
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
