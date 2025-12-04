package kr.sweetapps.alcoholictimer.ui.tab_03

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.data.repository.RecordsDataLoader
import kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions

/**
 * [NEW] Tab03(레벨 화면) 상태 관리 ViewModel
 * - 현재 시간 자동 업데이트
 * - 레벨 진행률 계산
 * - 과거 기록 + 현재 경과 시간 통합 관리
 */
class Tab03ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        "user_settings",
        Context.MODE_PRIVATE
    )

    private val LEVEL_VISITS_KEY = "level_visits"

    // [NEW] 현재 시간 상태
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // [NEW] 시작 시간 상태
    private val _startTime = MutableStateFlow(sharedPref.getLong("start_time", 0L))
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    // [NEW] 레벨 방문 횟수 상태
    private val _levelVisits = MutableStateFlow(sharedPref.getInt(LEVEL_VISITS_KEY, 0))
    val levelVisits: StateFlow<Int> = _levelVisits.asStateFlow()

    // [NEW] 현재 경과 시간 계산 (타이머 완료 시 0으로 설정)
    val currentElapsedTime: StateFlow<Long> = MutableStateFlow(0L).apply {
        viewModelScope.launch {
            currentTime.collect { time ->
                val start = _startTime.value
                // [FIX] 타이머 완료 상태 확인
                val isCompleted = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)
                // 타이머가 완료되었거나 시작 시간이 없으면 0
                value = if (start > 0 && !isCompleted) time - start else 0L
            }
        }
    }

    // [NEW] 총 경과 시간 (과거 기록 + 현재)
    private val _totalElapsedTime = MutableStateFlow(0L)
    val totalElapsedTime: StateFlow<Long> = _totalElapsedTime.asStateFlow()

    // [NEW] 총 경과 일수 (Float)
    private val _totalElapsedDaysFloat = MutableStateFlow(0f)
    val totalElapsedDaysFloat: StateFlow<Float> = _totalElapsedDaysFloat.asStateFlow()

    // [NEW] 레벨 일수 (정수)
    private val _levelDays = MutableStateFlow(0)
    val levelDays: StateFlow<Int> = _levelDays.asStateFlow()

    // [NEW] 현재 레벨 정보
    private val _currentLevel = MutableStateFlow(LevelDefinitions.levels.first())
    val currentLevel: StateFlow<LevelDefinitions.LevelInfo> = _currentLevel.asStateFlow()

    init {
        // [FIX] 현재 시간 업데이트 (1초마다)
        viewModelScope.launch {
            while (true) {
                delay(1000L) // 1초마다 업데이트
                _currentTime.value = System.currentTimeMillis()
            }
        }

        // [NEW] 과거 기록 로드 및 총 시간 계산
        loadRecordsAndCalculateTotalTime()

        // [NEW] 레벨 방문 횟수 증가
        incrementLevelVisits()
    }

    /**
     * [NEW] 과거 기록을 로드하고 총 경과 시간 계산
     */
    private fun loadRecordsAndCalculateTotalTime() {
        viewModelScope.launch {
            try {
                val pastRecords = RecordsDataLoader.loadSobrietyRecords(getApplication())
                val totalPastDuration = pastRecords.sumOf { record ->
                    (record.endTime - record.startTime)
                }

                // [NEW] 현재 경과 시간과 합산
                currentElapsedTime.collect { currentElapsed ->
                    val total = totalPastDuration + currentElapsed
                    _totalElapsedTime.value = total

                    // [FIX] 타이머 테스트 모드를 고려한 동적 DAY_IN_MILLIS
                    val dayInMillis = Constants.getDayInMillis(getApplication())
                    _totalElapsedDaysFloat.value = total / dayInMillis.toFloat()

                    val days = Constants.calculateLevelDays(total, dayInMillis)
                    _levelDays.value = days
                    _currentLevel.value = LevelDefinitions.getLevelInfo(days)

                    // [DEBUG] 레벨 업데이트 로그 출력
                    Log.d("Tab03ViewModel", "레벨 업데이트: " +
                            "total=${total}ms, dayInMillis=${dayInMillis}, days=$days, " +
                            "level=${_currentLevel.value.nameResId}, daysFloat=${_totalElapsedDaysFloat.value}")
                }
            } catch (e: Exception) {
                Log.e("Tab03ViewModel", "Failed to load records", e)
            }
        }
    }

    /**
     * [NEW] 레벨 화면 방문 횟수 증가
     */
    private fun incrementLevelVisits() {
        viewModelScope.launch {
            try {
                val prev = sharedPref.getInt(LEVEL_VISITS_KEY, 0)
                val next = prev + 1
                sharedPref.edit().putInt(LEVEL_VISITS_KEY, next).apply()
                _levelVisits.value = next
                Log.d("Tab03ViewModel", "LevelScreen visited; count=$next")
            } catch (e: Exception) {
                Log.e("Tab03ViewModel", "Failed to increment level visits", e)
            }
        }
    }

    /**
     * [NEW] 레벨 방문 횟수 초기화 (광고 표시 후)
     */
    fun resetLevelVisits() {
        viewModelScope.launch {
            try {
                sharedPref.edit().putInt(LEVEL_VISITS_KEY, 0).apply()
                _levelVisits.value = 0
                Log.d("Tab03ViewModel", "Level visits reset to 0")
            } catch (e: Exception) {
                Log.e("Tab03ViewModel", "Failed to reset level visits", e)
            }
        }
    }

    /**
     * [NEW] 다음 레벨 정보 가져오기
     */
    fun getNextLevel(): LevelDefinitions.LevelInfo? {
        val currentIndex = LevelDefinitions.levels.indexOf(_currentLevel.value)
        return if (currentIndex in 0 until LevelDefinitions.levels.size - 1) {
            LevelDefinitions.levels[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * [NEW] 다음 레벨까지의 진행률 계산
     */
    fun calculateProgress(): Float {
        val nextLevel = getNextLevel() ?: return 1f
        val current = _currentLevel.value

        return if (nextLevel.start > current.start) {
            val progressInLevel = _totalElapsedDaysFloat.value - current.start
            val totalNeeded = (nextLevel.start - current.start).toFloat()
            if (totalNeeded > 0f) {
                (progressInLevel / totalNeeded).coerceIn(0f, 1f)
            } else {
                0f
            }
        } else {
            0f
        }
    }
}

