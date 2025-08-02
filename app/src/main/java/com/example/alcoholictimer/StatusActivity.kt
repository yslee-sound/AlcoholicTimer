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
import com.example.alcoholictimer.utils.Constants
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity에서 이미 햄버거 메뉴 및 네비게이션 기능 처리됨
    }

    override fun onResume() {
        super.onResume()
        // 화면이 보일 때 즉시 UI 업데이트 후 타이머 시작
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
        // UI 즉시 갱신
        updateUI()
    }

    override fun handleNewIntent(intent: Intent?) {
        // BaseActivity의 handleNewIntent 구현
        updateUI()
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
        }, 1000, 1000) // 1초 후부터 1초마다 업데이트 (초기 UI 업데이트는 onResume에서 처리)
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

        // 타이머가 이미 완료되었는지 확인
        val completionFlag = sharedPref.getBoolean("timer_completed", false)

        // 타이머가 이미 완료된 상태면 목표 일수를 보여주고 즉시 리턴
        if (completionFlag) {
            tvDaysCount.text = targetDays.toString()
            tvDaysCount.setTextColor(resources.getColor(android.R.color.holo_orange_dark, theme))
            tvMessage.text = "축하합니다! ${targetDays}${Constants.TIME_UNIT_TEXT} 목표를 달성했습니다!"

            // 완료 상태에서는 프로그레스바를 100%로 설정
            progressLevel.progress = 100
            return
        }

        // 테스트 모드에 따른 일/분/초 단위의 경과 시간 계산
        val timePassed = ((System.currentTimeMillis() - startTime) / Constants.TIME_UNIT_MILLIS).toInt()

        // 초 단위 경과 시간 계산 (모든 모드에서 초 단위로 계산)
        val secondsPassed = (System.currentTimeMillis() - startTime) / 1000L

        // 테스트 모드에 따라 목표 시간을 초 단위로 변환
        val targetSeconds = when {
            Constants.isSecondTestMode -> targetDays.toLong() // 이미 초 단위
            Constants.isMinuteTestMode -> targetDays.toLong() * 60 // 분을 초로 변환
            else -> targetDays.toLong() * 24 * 60 * 60 // 일을 초로 변환
        }

        // UI 업데이트 - 마지막 목표 숫자에 도달하면 숫자를 증가시키지 않고 색상 변경
        if (timePassed >= targetDays) {
            tvDaysCount.text = targetDays.toString()
            // 목표 달성 시 주황색으로 변경
            tvDaysCount.setTextColor(resources.getColor(android.R.color.holo_orange_dark, theme))

            // 프로그레스바를 100%로 강제 설정
            progressLevel.progress = 100

            // 타이머가 완료되었음을 저장
            with(sharedPref.edit()) {
                putBoolean("timer_completed", true)
                apply()
            }

            // 목표 달성 시 타이머 즉시 중지
            stopTimer()

            // 목표 달성 여부 메시지
            tvMessage.text = "축하합니다! ${targetDays}${Constants.TIME_UNIT_TEXT} 목표를 달성했습니다!"

            // 목표 달성 시 처리 (한 번만 실행되도록)
            if (!goalAchievementChecked) {
                goalAchievementChecked = true

                // 테스트 모드에 따라 적절한 마일스톤 선택
                val adjustedMilestones = when {
                    Constants.isSecondTestMode -> secondTestMilestones
                    Constants.isMinuteTestMode -> minuteTestMilestones
                    else -> levelMilestones
                }

                // 레벨 계산 (기록 목적으로만 사용)
                var currentLevel = 0
                for (i in adjustedMilestones.indices) {
                    if (timePassed >= adjustedMilestones[i]) {
                        currentLevel = i
                    } else {
                        break
                    }
                }

                // 기록 저장 및 완료 처리
                val recordId = saveCompletedRecord(startTime, System.currentTimeMillis(), targetDays, currentLevel + 1)

                // 토스트 메시지 표시
                Toast.makeText(this, "목표 달성! 금주가 완료되었습니다.", Toast.LENGTH_SHORT).show()

                // 요약 화면으로 이동 (3초 딜레이로 변경)
                handler.postDelayed({
                    val intent = Intent(this, RecordSummaryActivity::class.java)
                    intent.putExtra("record_id", recordId)
                    startActivity(intent)
                    finish()
                }, 3000) // 3초 후 이동 (기존 1초에서 변경)
            }
        } else {
            // 아직 목표 달성 전이면 일반적인 숫자 표시 (1일차부터 시작)
            tvDaysCount.text = (timePassed + 1).toString()
            // 기본 색상으로 설정
            tvDaysCount.setTextColor(resources.getColor(android.R.color.black, theme))

            // 테스트 모드에 따라 적절한 마일스톤 선택
            val adjustedMilestones = when {
                Constants.isSecondTestMode -> secondTestMilestones
                Constants.isMinuteTestMode -> minuteTestMilestones
                else -> levelMilestones
            }

            // 레벨 계산 (기록 목적으로만 사용)
            var currentLevel = 0
            for (i in adjustedMilestones.indices) {
                if (timePassed >= adjustedMilestones[i]) {
                    currentLevel = i
                } else {
                    break
                }
            }

            // 프로그레스바 업데이트 - 초 단위로 계산하여 부드러운 진행을 구현
            val progressPercentage = (secondsPassed.toFloat() / targetSeconds) * 100
            progressLevel.progress = progressPercentage.toInt().coerceIn(0, 100)

            // 남은 일수 메시지에도 +1 적용하지 않음 (실제 목표까지 남은 날짜를 정확하게 표시)
            tvMessage.text = "목표까지 ${targetDays - timePassed}${Constants.TIME_UNIT_TEXT} 남았습니다. 힘내세요!"
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

    private fun navigateToRecords() {
        val intent = Intent(this, RecordsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
