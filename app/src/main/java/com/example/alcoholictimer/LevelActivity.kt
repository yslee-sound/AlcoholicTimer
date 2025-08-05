package com.example.alcoholictimer

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class LevelActivity : BaseActivity() {

    private lateinit var layoutCurrentLevel: LinearLayout
    private lateinit var viewCurrentLevelBadge: View
    private lateinit var tvCurrentLevel: TextView
    private lateinit var tvNextLevelDays: TextView
    private lateinit var progressBarLevel: ProgressBar

    // 개별 레벨 카드들
    private lateinit var levelCard1: LinearLayout
    private lateinit var levelCard2: LinearLayout
    private lateinit var levelCard3: LinearLayout
    private lateinit var levelCard4: LinearLayout
    private lateinit var levelCard5: LinearLayout
    private lateinit var levelCard6: LinearLayout
    private lateinit var levelCard7: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
        initViews()
        setupLevelData()
    }

    override fun setupContentView() {
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        layoutInflater.inflate(R.layout.activity_level, contentFrame, true)
    }

    private fun initViews() {
        layoutCurrentLevel = findViewById(R.id.layoutCurrentLevel)
        viewCurrentLevelBadge = findViewById(R.id.viewCurrentLevelBadge)
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel)
        tvNextLevelDays = findViewById(R.id.tvNextLevelDays)
        progressBarLevel = findViewById(R.id.progressBarLevel)

        // 개별 레벨 카드들 초기화
        levelCard1 = findViewById(R.id.levelCard1)
        levelCard2 = findViewById(R.id.levelCard2)
        levelCard3 = findViewById(R.id.levelCard3)
        levelCard4 = findViewById(R.id.levelCard4)
        levelCard5 = findViewById(R.id.levelCard5)
        levelCard6 = findViewById(R.id.levelCard6)
        levelCard7 = findViewById(R.id.levelCard7)
    }

    private fun setupLevelData() {
        val sharedPrefs = getSharedPreferences("alcoholic_timer", Context.MODE_PRIVATE)
        val totalDays = calculateTotalDays(sharedPrefs)

        // 현재 레벨 계산
        val currentLevel = calculateCurrentLevel(totalDays)
        val nextLevelDays = getNextLevelDays(currentLevel)
        val progress = calculateProgress(totalDays, currentLevel)

        // 현재 레벨 정보 표시
        updateCurrentLevelInfo(currentLevel, nextLevelDays, progress)

        // 모든 레벨 카드 상태 업데이트
        updateAllLevelCards(totalDays)
    }

    private fun calculateTotalDays(sharedPrefs: android.content.SharedPreferences): Int {
        // 모든 완료된 기록들의 총 일수를 계산
        val recordsJson = sharedPrefs.getString("abstain_records", "[]")
        var totalDays = 0

        try {
            // 현재 진행 중인 금주가 있다면 추가
            val isAbstaining = sharedPrefs.getBoolean("is_abstaining", false)
            if (isAbstaining) {
                val startTime = sharedPrefs.getLong("abstain_start_time", 0)
                if (startTime > 0) {
                    val currentTime = System.currentTimeMillis()
                    val elapsedDays = ((currentTime - startTime) / (24 * 60 * 60 * 1000)).toInt()
                    totalDays += elapsedDays
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return totalDays
    }

    private fun calculateCurrentLevel(totalDays: Int): Int {
        return when {
            totalDays >= 365 -> 7  // 1년
            totalDays >= 100 -> 6  // 100일
            totalDays >= 30 -> 5   // 한달
            totalDays >= 14 -> 4   // 2주
            totalDays >= 7 -> 3    // 일주일
            totalDays >= 3 -> 2    // 3일
            totalDays >= 1 -> 1    // 첫걸음
            else -> 0              // 시작 전
        }
    }

    private fun getNextLevelDays(currentLevel: Int): Int {
        return when (currentLevel) {
            0 -> 1
            1 -> 3
            2 -> 7
            3 -> 14
            4 -> 30
            5 -> 100
            6 -> 365
            else -> 0
        }
    }

    private fun calculateProgress(totalDays: Int, currentLevel: Int): Int {
        val currentLevelStart = when (currentLevel) {
            0 -> 0
            1 -> 1
            2 -> 3
            3 -> 7
            4 -> 14
            5 -> 30
            6 -> 100
            else -> 365
        }

        val nextLevelStart = getNextLevelDays(currentLevel)

        if (nextLevelStart == 0) return 100 // 최고 레벨

        val progressInLevel = totalDays - currentLevelStart
        val levelRange = nextLevelStart - currentLevelStart

        return if (levelRange > 0) {
            ((progressInLevel.toFloat() / levelRange.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }
    }

    private fun updateCurrentLevelInfo(level: Int, nextLevelDays: Int, progress: Int) {
        val levelName = when (level) {
            0 -> "시작 준비"
            1 -> "첫걸음 성공"
            2 -> "의지 다지기"
            3 -> "일주일 챌린지"
            4 -> "2주 달성"
            5 -> "한달 마스터"
            6 -> "100일 영웅"
            7 -> "1년 전설"
            else -> "시작 준비"
        }

        tvCurrentLevel.text = levelName

        if (nextLevelDays > 0) {
            tvNextLevelDays.text = "다음 레벨까지 ${nextLevelDays}일 남음"
        } else {
            tvNextLevelDays.text = "최고 레벨 달성!"
        }

        progressBarLevel.progress = progress
    }

    private fun updateAllLevelCards(totalDays: Int) {
        // 각 레벨 카드의 상태를 업데이트
        updateLevelCard(levelCard1, 1, totalDays >= 1)
        updateLevelCard(levelCard2, 2, totalDays >= 3)
        updateLevelCard(levelCard3, 3, totalDays >= 7)
        updateLevelCard(levelCard4, 4, totalDays >= 14)
        updateLevelCard(levelCard5, 5, totalDays >= 30)
        updateLevelCard(levelCard6, 6, totalDays >= 100)
        updateLevelCard(levelCard7, 7, totalDays >= 365)
    }

    private fun updateLevelCard(card: LinearLayout, level: Int, isAchieved: Boolean) {
        // 달성하지 못한 레벨은 그레이 처리
        if (isAchieved) {
            card.alpha = 1.0f
            card.setBackgroundColor(resources.getColor(android.R.color.white, null))
        } else {
            card.alpha = 0.5f
            card.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }
    }
}
