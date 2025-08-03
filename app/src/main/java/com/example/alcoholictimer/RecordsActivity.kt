package com.example.alcoholictimer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alcoholictimer.adapters.LevelHistoryAdapter
import com.example.alcoholictimer.adapters.RecentActivityAdapter
import com.example.alcoholictimer.models.LevelHistoryItem
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.SobrietyRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

class RecordsActivity : BaseActivity() {

    private lateinit var tvWeeklyCount: TextView
    private lateinit var tvMonthlyCount: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvPeriodSummary: TextView
    private lateinit var tvTotalAbstinence: TextView
    private lateinit var tvLongestStreak: TextView
    private lateinit var tvLastFailure: TextView
    private lateinit var chartContainer: FrameLayout
    private lateinit var rvLevelHistory: RecyclerView
    private lateinit var rvRecentActivities: RecyclerView

    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private lateinit var btnYear: Button
    private lateinit var btnAll: Button

    private lateinit var recentActivityAdapter: RecentActivityAdapter

    private var startDate: LocalDate? = null
    private var currentPeriod = Period.ALL

    private enum class Period {
        WEEK, MONTH, YEAR, ALL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 데이터 로드 및 표시
        loadAndDisplayData()
    }

    override fun setupContentView() {
        // RecordsActivity의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        val view = LayoutInflater.from(this).inflate(R.layout.activity_records, contentFrame, true)

        // View 초기화
        initViews(view)

        // 버튼 클릭 리스너 설정
        setupButtonListeners()
    }

    private fun initViews(view: View) {
        tvWeeklyCount = view.findViewById(R.id.tvWeeklyCount)
        tvMonthlyCount = view.findViewById(R.id.tvMonthlyCount)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvSummary = view.findViewById(R.id.tvSummary)
        tvPeriodSummary = view.findViewById(R.id.tvPeriodSummary)
        tvTotalAbstinence = view.findViewById(R.id.tvTotalAbstinence)
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak)
        tvLastFailure = view.findViewById(R.id.tvLastFailure)
        chartContainer = view.findViewById(R.id.chartContainer)
        rvLevelHistory = view.findViewById(R.id.rvLevelHistory)

        btnWeek = view.findViewById(R.id.btnWeek)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnYear = view.findViewById(R.id.btnYear)
        btnAll = view.findViewById(R.id.btnAll)

        // RecyclerView 설정
        rvLevelHistory.layoutManager = LinearLayoutManager(this)

        // 최근 활동 RecyclerView 초기화
        rvRecentActivities = view.findViewById(R.id.rvRecentActivities)
        rvRecentActivities.layoutManager = LinearLayoutManager(this)
    }

    private fun setupButtonListeners() {
        btnWeek.setOnClickListener {
            currentPeriod = Period.WEEK
            updatePeriodUI()
            loadAndDisplayData()
        }

        btnMonth.setOnClickListener {
            currentPeriod = Period.MONTH
            updatePeriodUI()
            loadAndDisplayData()
        }

        btnYear.setOnClickListener {
            currentPeriod = Period.YEAR
            updatePeriodUI()
            loadAndDisplayData()
        }

        btnAll.setOnClickListener {
            currentPeriod = Period.ALL
            updatePeriodUI()
            loadAndDisplayData()
        }
    }

    private fun updatePeriodUI() {
        // 모든 버튼 기본 스타일로 초기화
        btnWeek.setBackgroundColor(Color.LTGRAY)
        btnMonth.setBackgroundColor(Color.LTGRAY)
        btnYear.setBackgroundColor(Color.LTGRAY)
        btnAll.setBackgroundColor(Color.LTGRAY)

        // 선택된 버튼 강조
        when(currentPeriod) {
            Period.WEEK -> {
                btnWeek.setBackgroundColor(Color.DKGRAY)
                tvPeriodSummary.text = "선택 기간: 주"
            }
            Period.MONTH -> {
                btnMonth.setBackgroundColor(Color.DKGRAY)
                tvPeriodSummary.text = "선택 기간: 월"
            }
            Period.YEAR -> {
                btnYear.setBackgroundColor(Color.DKGRAY)
                tvPeriodSummary.text = "선택 기간: 년"
            }
            Period.ALL -> {
                btnAll.setBackgroundColor(Color.DKGRAY)
                tvPeriodSummary.text = "선택 기간: 전체"
            }
        }
    }

    private fun loadAndDisplayData() {
        // SharedPreferences에서 시작일 불러오기
        val sharedPref = getSharedPreferences("AlcoholicPrefs", MODE_PRIVATE)
        val startDateStr = sharedPref.getString("start_date", null)

        if (startDateStr != null) {
            startDate = LocalDate.parse(startDateStr)
            val today = LocalDate.now()

            // 기본 통계 업데이트
            updateBasicStatistics(today)

            // 기간별 통계 업데이트
            updatePeriodStatistics(today)

            // 레벨 히스토리 업데이트
            updateLevelHistory()

            // 최근 활동 데이터 로드 및 표시
            loadRecentActivities()
        }
    }

    private fun updateBasicStatistics(today: LocalDate) {
        startDate?.let { start ->
            // 전체 금주 일수
            val totalDays = ChronoUnit.DAYS.between(start, today).toInt() + 1 // 오늘 포함

            // 이번 주 금주 일수
            val weekFields = WeekFields.of(Locale.getDefault())
            val thisWeekStart = today.with(weekFields.dayOfWeek(), 1)
            val weeklyDays = if (start.isAfter(thisWeekStart)) {
                ChronoUnit.DAYS.between(start, today).toInt() + 1
            } else {
                ChronoUnit.DAYS.between(thisWeekStart, today).toInt() + 1
            }

            // 이번 달 금주 일수
            val thisMonthStart = today.withDayOfMonth(1)
            val monthlyDays = if (start.isAfter(thisMonthStart)) {
                ChronoUnit.DAYS.between(start, today).toInt() + 1
            } else {
                ChronoUnit.DAYS.between(thisMonthStart, today).toInt() + 1
            }

            // UI 업데이트
            tvWeeklyCount.text = "${weeklyDays}일"
            tvMonthlyCount.text = "${monthlyDays}일"
            tvTotalCount.text = "${totalDays}일"

            // 요약 메시지
            val summaryMessage = when {
                totalDays >= 365 -> "1년 이상 금주를 실천하셨네요! 정말 대단합니다! 🎉"
                totalDays >= 180 -> "6개월 이상 금주를 지속하고 계시네요! 👏"
                totalDays >= 90 -> "3개월 동안 꾸준히 실천하셨어요! 💪"
                totalDays >= 30 -> "한 달 동안 잘 해내고 계세요! ⭐"
                else -> "금주를 시작한지 ${totalDays}일이 지났습니다."
            }
            tvSummary.text = summaryMessage

            // 최고 기록 업데이트
            tvTotalAbstinence.text = "전체 누적 금주 일수: ${totalDays}일"
            tvLongestStreak.text = "최장 연속 금주 기록: ${totalDays}일" // 예제에서는 실패 없이 연속으로 진행
            tvLastFailure.text = "마지막 금주 실패: 없음" // 예제에서는 실패가 없다고 가정
        }
    }

    private fun updatePeriodStatistics(today: LocalDate) {
        // 여기서 기간별 통계를 표시할 수 있습니다
        // 이 예제에서는 단순화를 위해 코드를 생략합니다
        // 실제로는 기간에 따른 그래프 등을 표시할 수 있습니다
    }

    private fun updateLevelHistory() {
        // 예시 데이터 (실제로는 SharedPreferences나 DB에서 가져와야 함)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val levelHistory = mutableListOf<LevelHistoryItem>()

        startDate?.let { start ->
            val today = LocalDate.now()
            var currentDate = start
            var level = 1

            // 레벨 달성 예시 데이터 생성
            // 실제 앱에서는 저장된 레벨 달성 히스토리를 가져와야 함
            while (!currentDate.isAfter(today) && level <= 7) {
                // 7일마다 레벨업 (예시)
                val daysBetween = ChronoUnit.DAYS.between(start, currentDate)
                if (daysBetween % 7L == 0L) {
                    levelHistory.add(LevelHistoryItem(
                        currentDate.format(formatter),
                        "Level ${level} 달성!"
                    ))
                    level++
                }
                // 일부 날짜는 금주 성공 기록 추가
                else if (daysBetween % 3L == 0L) {
                    levelHistory.add(LevelHistoryItem(
                        currentDate.format(formatter),
                        "금주 성공"
                    ))
                }

                currentDate = currentDate.plusDays(1)
            }

            // RecyclerView에 어댑터 설정
            rvLevelHistory.adapter = LevelHistoryAdapter(levelHistory)
        }
    }

    private fun loadRecentActivities() {
        // SharedPreferences에서 저장된 기록 불러오기
        val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
        val recordsJson = sharedPref.getString("records", "[]")
        val records = SobrietyRecord.fromJsonArray(recordsJson ?: "[]")

        // 최근 활동 목록 생성
        val recentActivities = records.map { record ->
            RecentActivity(
                startDate = record.startDate,
                endDate = record.endDate,
                duration = record.duration,
                isCompleted = record.isCompleted
            )
        }.sortedByDescending { it.endDate }.take(3)  // 최근 3개만 표시

        // 어댑터 설정
        recentActivityAdapter = RecentActivityAdapter(recentActivities)
        rvRecentActivities.adapter = recentActivityAdapter
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
    }
}
