package com.example.alcoholictimer

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alcoholictimer.utils.SharedPreferencesManager

class LevelActivity : BaseActivity() {

    private lateinit var layoutCurrentLevel: LinearLayout
    private lateinit var viewCurrentLevelBadge: View
    private lateinit var tvCurrentLevel: TextView
    private lateinit var tvNextLevelDays: TextView
    private lateinit var progressBarLevel: ProgressBar
    private lateinit var recyclerViewLevels: RecyclerView
    private lateinit var levelAdapter: LevelAdapter

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
        recyclerViewLevels = findViewById(R.id.recyclerViewLevels)

        recyclerViewLevels.layoutManager = LinearLayoutManager(this)
        levelAdapter = LevelAdapter()
        recyclerViewLevels.adapter = levelAdapter
    }

    private fun setupLevelData() {
        val currentDays = getCurrentAbstainDays()
        val currentLevel = getLevelFromDays(currentDays)
        val nextLevel = getNextLevel(currentLevel)

        // 현재 레벨 카드 배경색 설정
        layoutCurrentLevel.setBackgroundColor(android.graphics.Color.parseColor(currentLevel.color))

        // 현재 레벨 배지 색상 설정 (흰색으로 통일)
        viewCurrentLevelBadge.setBackgroundColor(android.graphics.Color.WHITE)

        // 현재 레벨 표시
        tvCurrentLevel.text = currentLevel.name

        // 프로그레스 바와 다음 레벨까지 남은 일수 계산
        if (nextLevel != null) {
            val levelRange = nextLevel.minDays - currentLevel.minDays
            val currentProgressInLevel = currentDays - currentLevel.minDays
            val daysToNext = nextLevel.minDays - currentDays

            // 현재 레벨 내에서의 진행도 계산
            val progress = if (levelRange > 0) {
                ((currentProgressInLevel.toFloat() / levelRange.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                100
            }

            progressBarLevel.progress = progress
            tvNextLevelDays.text = "다음 레벨까지 ${daysToNext}일 남음"
        } else {
            // 최고 레벨 달성
            progressBarLevel.progress = 100
            tvNextLevelDays.text = "최고 레벨 달성!"
        }

        // 레벨 리스트 설정
        val allLevels = getAllLevels()
        levelAdapter.updateLevels(allLevels, currentDays)
    }

    private fun getCurrentAbstainDays(): Int {
        val sharedPrefs = SharedPreferencesManager.getInstance(this)
        if (!sharedPrefs.getBoolean("isAbstaining", false)) {
            return 0
        }

        val startTime = sharedPrefs.getLong("abstainStartTime", 0L)
        if (startTime == 0L) return 0

        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - startTime
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun getLevelFromDays(days: Int): Level {
        val levels = getAllLevels()
        return levels.findLast { days >= it.minDays } ?: levels.first()
    }

    private fun getNextLevel(currentLevel: Level): Level? {
        val levels = getAllLevels()
        val currentIndex = levels.indexOf(currentLevel)
        return if (currentIndex < levels.size - 1) {
            levels[currentIndex + 1]
        } else {
            null
        }
    }

    private fun getAllLevels(): List<Level> {
        return listOf(
            Level("작심 7일", 0, 6, "#9E9E9E", "그레이", "첫 걸음을 시작했습니다"),
            Level("의지의 2주", 7, 13, "#FFEB3B", "옐로우", "의지가 단단해지고 있습니다"),
            Level("한달의 기적", 14, 29, "#FF9800", "오렌지", "한 달의 기적을 만들어가고 있습니다"),
            Level("습관의 탄생", 30, 59, "#4CAF50", "그린", "새로운 습관이 자리잡고 있습니다"),
            Level("계속되는 도전", 60, 119, "#2196F3", "블루", "꾸준한 도전이 계속되고 있습니다"),
            Level("거의 1년", 120, 239, "#9C27B0", "퍼플", "1년에 가까워지고 있습니다"),
            Level("금주 마스터", 240, 364, "#424242", "블랙", "금주의 마스터가 되었습니다"),
            Level("절제의 레전드", 365, Int.MAX_VALUE, "#FFD700", "골드", "전설적인 절제력을 보여주고 있습니다")
        )
    }
}

data class Level(
    val name: String,
    val minDays: Int,
    val maxDays: Int,
    val color: String,
    val colorName: String,
    val description: String
)
