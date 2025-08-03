package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.utils.RecordManager
import com.example.alcoholictimer.utils.SobrietyRecord
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class StatusActivity : BaseActivity() {
    // 이전에 목표 달성 여부를 확인했는지 체크하는 플래그
    private var goalAchievementChecked = false

    // 일 단위 레벨 마일스톤 (실제 운영)
    private val levelMilestones = listOf(0, 7, 14, 30, 60, 120, 240, 365)

    // 분 단위 레벨 마일스톤 (테스트 모드)
    private val minuteTestMilestones = listOf(0, 1, 2, 5, 10, 15, 20, 30)

    // 초 단위 레벨 마일스톤 (초 단위 테스트 모드)
    private val secondTestMilestones = listOf(0, 7, 14, 30, 60, 90, 120, 180)

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

    // UI 업데이트를 위한 타이머 및 핸들러
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    // UI 요소 참조 저장 변수
    private lateinit var tvDaysCount: TextView
    private lateinit var tvTimeUnit: TextView
    private lateinit var tvMessage: TextView
    private lateinit var progressLevel: ProgressBar
    private lateinit var tvTimeDetail: TextView
    private lateinit var tvHoursDisplay: TextView // 시간 표시용 TextView 추가

    // 금주 시작 시간 저장 변수
    private var abstainStartTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity에서 이미 햄버거 메뉴 및 네비게이션 기능 처리됨
    }

    override fun onResume() {
        super.onResume()
        updateTimeModeDisplay()  // 모드 변경사항 업데이트
        updateUI()
        startTimer()

        // 목표 달성 플래그 초기화
        goalAchievementChecked = false
    }

    override fun onPause() {
        super.onPause()
        // 화면이 보이지 않을 때 타이머 정지
        stopTimer()
    }

    // Activity 클래스의 onNewIntent를 오버라이드
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 새 인텐트로 액티비티가 재사용될 때 수행할 작업
        setIntent(intent)
        updateTimeModeDisplay()  // 모드 변경사항 업데이트
        // UI 즉시 갱신
        updateUI()
    }

    override fun handleNewIntent(intent: Intent?) {
        // BaseActivity의 handleNewIntent 구현
        updateUI()
    }

    private fun startTimer() {
        stopTimer()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    updateTimeDisplay()
                }
            }
        }, 0, 100)  // 100ms 간격으로 업데이트
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun updateTimeDisplay() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val currentTime = System.currentTimeMillis()
        val secondsPassed = (currentTime - startTime) / 1000L

        // 시간 계산
        val hours = (secondsPassed / 3600) % 24

        // 시간 표시 업데이트
        tvHoursDisplay.text = String.format("%02d시간", hours)

        // 테스트 모드별 시간 표시 업데이트
        val timeText = when {
            Constants.isSecondTestMode -> {
                val totalSeconds = secondsPassed
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
            Constants.isMinuteTestMode -> {
                val totalSeconds = secondsPassed
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
            else -> {
                val totalSeconds = secondsPassed
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
        }

        tvTimeDetail.text = timeText
    }

    override fun setupContentView() {
        // StatusActivity 고유의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        val view = LayoutInflater.from(this).inflate(R.layout.content_status, contentFrame, true)

        // UI 요소 초기화
        tvDaysCount = view.findViewById(R.id.tvDaysCount)
        tvTimeUnit = view.findViewById(R.id.tvTimeUnit)
        tvMessage = view.findViewById(R.id.tvMessage)
        progressLevel = view.findViewById(R.id.progressLevel)
        tvTimeDetail = view.findViewById(R.id.tvTimeDetail)
        tvHoursDisplay = view.findViewById(R.id.tvHoursDisplay) // 시간 표시 TextView 초기화
        tvTimeDetail.text = "00:00:00"  // 초기값 변경

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

    private fun displayTimeDetails(timePassed: Int, secondsPassed: Long) {
        val timeText = when {
            Constants.isSecondTestMode -> {
                val seconds = secondsPassed % 60
                val millis = (System.currentTimeMillis() % 1000) / 10
                String.format("%02d:%02d", seconds, millis)
            }
            Constants.isMinuteTestMode -> {
                val minutes = secondsPassed / 60
                val seconds = secondsPassed % 60
                String.format("%02d:%02d", minutes, seconds)
            }
            else -> {
                val hours = (secondsPassed / 3600) % 24
                val minutes = (secondsPassed / 60) % 60
                String.format("%02d:%02d", hours, minutes)
            }
        }

        tvTimeDetail.text = timeText
    }

    /**
     * UI를 업데이트하는 메서드 - 타이머에 의해 주기적으로 호출됨
     */
    private fun updateUI() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val targetDays = sharedPref.getInt("target_days", 30)
        val completionFlag = sharedPref.getBoolean("timer_completed", false)

        if (completionFlag) {
            tvDaysCount.text = targetDays.toString()
            tvDaysCount.setTextColor(resources.getColor(android.R.color.holo_orange_dark, theme))
            tvMessage.text = "축하합니다! ${targetDays}${Constants.TIME_UNIT_TEXT} 목표를 달성했습니다!"
            progressLevel.progress = 100
            return
        }

        val currentTime = System.currentTimeMillis()
        val timePassed = ((currentTime - startTime) / Constants.TIME_UNIT_MILLIS).toInt() + 1
        val secondsPassed = (currentTime - startTime) / 1000L

        // 진행률 계산
        val targetSeconds = when {
            Constants.isSecondTestMode -> targetDays.toLong()
            Constants.isMinuteTestMode -> targetDays.toLong() * 60
            else -> targetDays.toLong() * 24 * 60 * 60
        }

        val progress = ((secondsPassed.toFloat() / targetSeconds.toFloat()) * 100).toInt()
        progressLevel.progress = progress.coerceIn(0, 100)

        // 타이머 표시 업데이트
        displayTimeDetails(timePassed, secondsPassed)

        if (timePassed > targetDays) {
            tvDaysCount.text = targetDays.toString()
            tvDaysCount.setTextColor(resources.getColor(android.R.color.holo_orange_dark, theme))
            progressLevel.progress = 100
            handleGoalCompletion(targetDays)
        } else {
            tvDaysCount.text = timePassed.toString()
            tvDaysCount.setTextColor(resources.getColor(android.R.color.black, theme))
            // 남은 시간 계산 및 메시지 업데이트
            val remainingDays = targetDays - timePassed + 1
            tvMessage.text = "목표까지 ${remainingDays}${Constants.TIME_UNIT_TEXT} 남았습니다. 힘내세요!"
        }
    }

    private fun handleGoalCompletion(targetDays: Int) {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val endTime = System.currentTimeMillis()

        with(sharedPref.edit()) {
            putBoolean("timer_completed", true)
            apply()
        }

        if (!goalAchievementChecked) {
            goalAchievementChecked = true
            stopTimer()

            // 3초 후에 기록 화면으로 이동
            Handler(Looper.getMainLooper()).postDelayed({
                // 활동 기록 저장
                saveActivity(true)
                // 기록 화면으로 이동
                navigateToRecords()
            }, 3000)
        }
    }

    /**
     * 완료된 금주 기록을 저장합니다
     * @return 저장된 기록의 ID
     */
    private fun saveCompletedRecord(startTime: Long, endTime: Long, targetDays: Int, level: Int): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startDate = dateFormat.format(Date(startTime))
        val endDate = dateFormat.format(Date(endTime))

        // 기록 ID 생성
        val recordId = System.currentTimeMillis()

        // 기록 객체 생성
        val record = SobrietyRecord(
            id = recordId,
            startDate = startDate,
            endDate = endDate,
            duration = targetDays,
            achievedLevel = level,
            levelTitle = levelTitles[level - 1],
            isCompleted = true
        )

        // 기존 기록 불러오기
        val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
        val recordsJson = sharedPref.getString("records", "[]")
        val records = SobrietyRecord.fromJsonArray(recordsJson ?: "[]").toMutableList()

        // 새 기록 추가
        records.add(record)

        // 기록 저장
        with(sharedPref.edit()) {
            putString("records", SobrietyRecord.toJsonArray(records))
            apply()
        }

        // 현재 진행중인 금주 데이터 초기화
        with(getSharedPreferences("user_settings", MODE_PRIVATE).edit()) {
            clear()
            apply()
        }

        return recordId
    }

    private fun saveActivity(isCompleted: Boolean) {
        val activity = RecentActivity(
            startDate = abstainStartTime,
            endDate = getCurrentDate(),
            duration = calculateDuration(),
            isCompleted = isCompleted
        )
        RecordManager.addActivity(activity)
    }

    private fun onGoalCompleted() {
        saveActivity(true)
        navigateToRecords()
    }

    private fun stopAbstaining() {
        saveActivity(false)
        // ...existing stop logic...
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun calculateDuration(): Int {
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(abstainStartTime)
        val currentDate = Date()
        return ((currentDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
    }

    private fun navigateToRecords() {
        val intent = Intent(this, RecordsActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * 눈에 잘 보이는 버튼이 있는 커스텀 중지 확인 다이얼로그를 표시합니다.
     */
    private fun showCustomStopDialog() {
        // 커스텀 다이얼로그 생성
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(com.example.alcoholictimer.R.layout.dialog_stop_sobriety)

        // 다이얼로그 배경을 투명하게 설정하고 외부 영역 클릭으로 닫히지 않게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        // 취소 버튼 클릭 리스너
        val btnCancel = dialog.findViewById<android.widget.Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 확인 버튼 클릭 리스너
        val btnConfirm = dialog.findViewById<android.widget.Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            dialog.dismiss()

            // SharedPreferences 초기화
            with(getSharedPreferences("user_settings", MODE_PRIVATE).edit()) {
                clear()
                apply()
            }

            // 시작 화면으로 이동
            val intent = Intent(this@StatusActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()

            // 토스트 메시지 표시
            Toast.makeText(this@StatusActivity, "금주가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 다이얼로그 표시
        dialog.show()
    }

    private fun navigateToStart() {
        val intent = Intent(this, StartActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun updateTimeModeDisplay() {
        val timeUnitText = when {
            Constants.isSecondTestMode -> "금주 목표 초수"
            Constants.isMinuteTestMode -> "금주 목표 분수"
            else -> "금주 목표 일수"
        }
        tvTimeUnit.text = timeUnitText
    }
}
