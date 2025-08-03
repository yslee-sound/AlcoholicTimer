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
        // 저장된 금주 기록들을 불러와서 통계 계산
        loadSobrietyRecords()
    }

    private fun loadSobrietyRecords() {
        // SharedPreferences에서 저장된 금주 기록 불러오기
        val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
        val recordsJson = sharedPref.getString("records", "[]")
        val records = SobrietyRecord.fromJsonArray(recordsJson ?: "[]")

        // 통계 계산 및 UI 업데이트
        updateStatisticsFromRecords(records)
        updateLevelHistoryFromRecords(records)
        loadRecentActivitiesFromRecords(records)
    }

    private fun updateStatisticsFromRecords(records: List<SobrietyRecord>) {
        if (records.isEmpty()) {
            // 기록이 없는 경우 기본값 표시
            tvWeeklyCount.text = "0일"
            tvMonthlyCount.text = "0일"
            tvTotalCount.text = "0일"
            tvSummary.text = "아직 완료된 금주 기록이 없습니다."
            tvTotalAbstinence.text = "전체 누적 금주 시간: 0일"
            tvLongestStreak.text = "최장 연속 금주 기록: 0일"
            tvLastFailure.text = "기록 없음"
            return
        }

        // 완료된 기록들만 필터링
        val completedRecords = records.filter { it.isCompleted }

        // 전체 누적 금주 시간 계산 (모든 완료된 기록의 duration 합계)
        val totalDuration = completedRecords.sumOf { it.duration }

        // 최장 연속 금주 기록 찾기
        val longestStreak = completedRecords.maxOfOrNull { it.duration } ?: 0

        // 최근 완료 기록 수 계산
        val today = LocalDate.now()
        val thisWeekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val thisMonthStart = today.withDayOfMonth(1)

        val weeklyCount = completedRecords.count { record ->
            try {
                val endDate = LocalDate.parse(record.endDate.substring(0, 10))
                !endDate.isBefore(thisWeekStart)
            } catch (e: Exception) {
                false
            }
        }

        val monthlyCount = completedRecords.count { record ->
            try {
                val endDate = LocalDate.parse(record.endDate.substring(0, 10))
                !endDate.isBefore(thisMonthStart)
            } catch (e: Exception) {
                false
            }
        }

        // UI 업데이트 - 테스트 모드에 따른 단위 표시
        val timeUnit = getTimeUnitForDisplay()
        tvWeeklyCount.text = "${weeklyCount}회"
        tvMonthlyCount.text = "${monthlyCount}회"
        tvTotalCount.text = "${completedRecords.size}회"

        // 요약 메시지
        val summaryMessage = when {
            completedRecords.size >= 10 -> "10회 이상 금주를 완료하셨네요! 정말 대단합니다! 🎉"
            completedRecords.size >= 5 -> "5회 이상 금주를 성공하셨어요! 👏"
            completedRecords.size >= 3 -> "꾸준히 금주를 실천하고 계시네요! 💪"
            completedRecords.size >= 1 -> "금주를 성공적으로 완료하셨어요! ⭐"
            else -> "아직 완료된 금주 기록이 없습니다."
        }
        tvSummary.text = summaryMessage

        // 최고 기록 업데이트
        tvTotalAbstinence.text = "전체 누적 금주 시간: $totalDuration$timeUnit"
        tvLongestStreak.text = "최장 연속 금주 기록: $longestStreak$timeUnit"

        // 마지막 실패 기록 (중도 포기한 기록 중 가장 최근)
        val lastFailure = records.filter { !it.isCompleted }
            .maxByOrNull { it.endDate }
        tvLastFailure.text = if (lastFailure != null) {
            "마지막 금주 중단: ${lastFailure.endDate.substring(0, 10)}"
        } else {
            "중단 기록 없음"
        }
    }

    private fun getTimeUnitForDisplay(): String {
        return when (com.example.alcoholictimer.utils.Constants.currentTestMode) {
            com.example.alcoholictimer.utils.Constants.TEST_MODE_SECOND -> "초"
            com.example.alcoholictimer.utils.Constants.TEST_MODE_MINUTE -> "분"
            else -> "일"
        }
    }

    private fun updateLevelHistoryFromRecords(records: List<SobrietyRecord>) {
        val levelHistory = mutableListOf<LevelHistoryItem>()

        // 완료된 기록들을 날짜순으로 정렬
        val completedRecords = records.filter { it.isCompleted }
            .sortedBy { it.endDate }

        completedRecords.forEach { record ->
            val endDate = record.endDate.substring(0, 10) // "yyyy-MM-dd" 형식으로 자르기
            val levelTitle = if (record.levelTitle.isNotEmpty()) {
                record.levelTitle
            } else {
                "레벨 ${record.achievedLevel}"
            }

            levelHistory.add(LevelHistoryItem(
                endDate,
                "$levelTitle 달성 (${record.duration}${getTimeUnitForDisplay()})"
            ))
        }

        // 기록이 없는 경우 안내 메시지 추가
        if (levelHistory.isEmpty()) {
            levelHistory.add(LevelHistoryItem(
                LocalDate.now().toString(),
                "아직 달성한 레벨이 없습니다"
            ))
        }

        // RecyclerView에 어댑터 설정
        rvLevelHistory.adapter = LevelHistoryAdapter(levelHistory)
    }

    private fun loadRecentActivitiesFromRecords(records: List<SobrietyRecord>) {
        // 모든 기록을 최근 활동으로 변환 (최근 5개)
        val recentActivities = records.sortedByDescending { it.endDate }
            .take(5)
            .map { record ->
                RecentActivity(
                    startDate = record.startDate,
                    endDate = record.endDate,
                    duration = record.duration,
                    isCompleted = record.isCompleted
                )
            }

        // 기록이 없는 경우 안내 메시지
        if (recentActivities.isEmpty()) {
            // 빈 상태 표시를 위한 더미 데이터
            val emptyActivity = RecentActivity(
                startDate = LocalDate.now().toString(),
                endDate = LocalDate.now().toString(),
                duration = 0,
                isCompleted = false
            )
            recentActivityAdapter = RecentActivityAdapter(listOf(emptyActivity))
        } else {
            recentActivityAdapter = RecentActivityAdapter(recentActivities)
        }

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
