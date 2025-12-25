package kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.repository.RecordsDataLoader
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import kr.sweetapps.alcoholictimer.util.constants.Constants

/**
 * [NEW] Tab03(레벨 화면) 상태 관리 ViewModel
 * - 현재 시간 자동 업데이트
 * - 레벨 진행률 계산
 * - 과거 기록 + 현재 경과 시간 통합 관리
 * - [FIX] SharedPreferences 변경 감지 기능 추가
 * - [FIX] Race Condition 방지: 이전 계산 작업 취소
 */
class Tab03ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        "user_settings",
        Context.MODE_PRIVATE
    )

    private val LEVEL_VISITS_KEY = "level_visits"

    // [NEW] 계산 작업 Job (이전 작업 취소용)
    private var calculationJob: Job? = null

    // [NEW] 기록 삭제 시 캐시 초기화 콜백
    private val clearRecordsCallback: () -> Unit = {
        Log.d("Tab03ViewModel", "Records cleared - forcing cache reset")

        // [FIX] SharedPreferences도 함께 업데이트 (다른 ViewModel과 동기화)
        // RecordsDataLoader에서 이미 초기화했지만, 혹시 모를 상황을 대비해 재확인
        sharedPref.edit()
            .putLong(Constants.PREF_START_TIME, 0L)
            .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            .apply()

        // [FIX] 로컬 캐시도 즉시 초기화
        _startTime.value = 0L

        // 캐시된 데이터 즉시 초기화
        _totalElapsedTime.value = 0L
        _totalElapsedDaysFloat.value = 0f
        _levelDays.value = 0
        _currentLevel.value = LevelDefinitions.levels.first()

        // 재계산 트리거
        loadRecordsAndCalculateTotalTime()
    }

    // [NEW] SharedPreferences 변경 감지 리스너
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            // [FIX] 기록, 타이머 시작/완료 모두 감지하여 즉시 반영
            Constants.PREF_SOBRIETY_RECORDS,
            Constants.PREF_START_TIME,
            Constants.PREF_TIMER_COMPLETED -> {
                Log.d("Tab03ViewModel", "Data changed ($key), reloading...")

                // 1. 시작 시간 상태 최신화 (중요: 이걸 해야 UI가 즉시 '진행 중'으로 바뀜)
                _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)

                // 2. 전체 통계 재계산
                loadRecordsAndCalculateTotalTime()
            }
        }
    }

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
        // [FIX] 현재 시간 업데이트 - 가상 시간 누적 방식 (배속 적용)
        viewModelScope.launch {
            var lastRealTime = System.currentTimeMillis()
            while (true) {
                delay(1000L) // [OPTIMIZED] 1초마다 갱신 (성능 최적화)

                val currentRealTime = System.currentTimeMillis()
                val realDelta = currentRealTime - lastRealTime
                lastRealTime = currentRealTime

                // [NEW] 배속 계수 적용 (디버그 모드에서만)
                val factor = if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                    Constants.getTimeAcceleration(getApplication())
                } else {
                    1
                }

                // [FIX] 가상 시간 누적 (핵심: += 사용)
                val virtualDelta = (realDelta * factor).toLong()
                _currentTime.value += virtualDelta
            }
        }

        // [NEW] 과거 기록 로드 및 총 시간 계산
        loadRecordsAndCalculateTotalTime()

        // [NEW] 레벨 방문 횟수 증가
        incrementLevelVisits()

        // [NEW] SharedPreferences 변경 감지 시작
        sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        Log.d("Tab03ViewModel", "Preference change listener registered")

        // [NEW] RecordsDataLoader 기록 삭제 리스너 등록
        RecordsDataLoader.registerClearRecordsListener(clearRecordsCallback)
        Log.d("Tab03ViewModel", "Clear records callback registered")
    }

    /**
     * [NEW] 과거 기록을 로드하고 총 경과 시간 계산
     * [FIX] Race Condition 방지: 이전 작업을 취소하고 새 작업 시작
     */
    private fun loadRecordsAndCalculateTotalTime() {
        // 1. 이전 계산 작업 취소 (좀비 코루틴 제거)
        calculationJob?.cancel()
        Log.d("Tab03ViewModel", "Previous calculation job cancelled")

        // 2. 새 계산 작업 할당
        calculationJob = viewModelScope.launch {
            try {
                val pastRecords = RecordsDataLoader.loadSobrietyRecords(getApplication())
                val totalPastDuration = pastRecords.sumOf { record ->
                    (record.endTime - record.startTime)
                }

                // [NEW] 현재 경과 시간과 합산 (무한 collect)
                currentElapsedTime.collect { currentElapsed ->
                    val total = totalPastDuration + currentElapsed
                    _totalElapsedTime.value = total

                    // [FIX] 레벨 계산은 항상 고정 상수로 (배속 적용 안 함)
                    _totalElapsedDaysFloat.value = total / Constants.DAY_IN_MILLIS.toFloat()

                    val days = Constants.calculateLevelDays(total, Constants.DAY_IN_MILLIS)
                    _levelDays.value = days
                    _currentLevel.value = LevelDefinitions.getLevelInfo(days)

                    // [REMOVED] 무한 반복 로그 제거 (성능 최적화)
                }
            } catch (e: CancellationException) {
                // 취소 예외는 정상 동작이므로 전파
                Log.d("Tab03ViewModel", "Calculation job cancelled")
                throw e
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
            // [FIX] current.start가 1일이면 0.0일 시점부터 시작이므로 1을 빼줌
            // 예: 0.5일 경과, Lv.1(start=1) → 0.5 - (1-1) = 0.5 (50% 진행)
            val progressInLevel = _totalElapsedDaysFloat.value - (current.start - 1)
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

    /**
     * [NEW] ViewModel 파괴 시 리스너 해제 (메모리 누수 방지)
     */
    override fun onCleared() {
        super.onCleared()
        sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        RecordsDataLoader.unregisterClearRecordsListener(clearRecordsCallback)
        Log.d("Tab03ViewModel", "Preference change listener and clear records callback unregistered")
    }
}