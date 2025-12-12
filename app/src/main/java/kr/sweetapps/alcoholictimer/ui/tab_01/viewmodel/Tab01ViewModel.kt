package kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.util.manager.TimerTimeManager

/**
 * Tab01 (Start/Run screen) state management ViewModel
 * [REFACTORED] 이제 TimerTimeManager를 사용하여 중앙 집중식 시간 관리
 * [FIX] SharedPreferences 리스너 추가하여 타이머 재시작 시 실시간 동기화
 * [FIX] 타이머 완료 이벤트 구독하여 자동 저장 및 화면 전환
 * [CRITICAL FIX] SharedPreferences 파일명을 "user_settings" 문자열 리터럴로 직접 사용
 *                (AppNavHost.kt와 동일한 파일을 참조하여 데이터 공유 보장)
 */
class Tab01ViewModel(application: Application) : AndroidViewModel(application) {

    // [CRITICAL FIX] 상수 대신 "user_settings" 문자열 리터럴 직접 사용
    // AppNavHost.kt에서도 동일하게 "user_settings"를 사용하므로 데이터 불일치 문제 해결
    private val sharedPref = application.getSharedPreferences(
        "user_settings",
        Context.MODE_PRIVATE
    )

    // [NEW] Zombie 이벤트 방지 - 타이머 시작 시각 기록 (Debounce용)
    private var lastTimerStartRequestTime: Long = 0L

    // Timer start time state
    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    // Target days state
    private val _targetDays = MutableStateFlow(30f)
    val targetDays: StateFlow<Float> = _targetDays.asStateFlow()

    // Timer completion status state
    private val _timerCompleted = MutableStateFlow(false)
    val timerCompleted: StateFlow<Boolean> = _timerCompleted.asStateFlow()

    // [REFACTORED] Elapsed time - 이제 TimerTimeManager에서 직접 가져옴
    val elapsedMillis: StateFlow<Long> = TimerTimeManager.elapsedMillis

    // [REFACTORED] 네비게이션 이벤트 (화면 전환 요청) - Success/GiveUp 명확히 분리
    sealed class NavigationEvent {
        object NavigateToSuccess : NavigationEvent() // [REFACTORED] 타이머 목표 달성 시 성공 화면으로
        object NavigateToGiveUp : NavigationEvent()  // [REFACTORED] 타이머 중단 시 위로 화면으로
        data class NavigateToDetail(val startTime: Long, val endTime: Long, val targetDays: Float, val actualDays: Int) : NavigationEvent()
    }

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    // [FIX] SharedPreferences 변경 감지 리스너
    // [CRITICAL] PREF_TIMER_COMPLETED는 제외 - ViewModel이 직접 변경하므로 리스너에서 재반응하면 플리커 발생
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Constants.PREF_START_TIME,
            Constants.PREF_TARGET_DAYS -> {
                Log.d("Tab01ViewModel", "[FIX] Timer data changed ($key) -> reloading state")

                // 1. 최신 데이터 로드
                loadTimerState()

                // 2. TimerTimeManager에도 최신 상태 반영
                val newStartTime = _startTime.value
                val newTargetDays = _targetDays.value
                val newCompleted = _timerCompleted.value

                if (newStartTime > 0 && !newCompleted) {
                    // [FIX] 타이머가 시작되었으면 TimerTimeManager에 목표 시간과 함께 반영
                    TimerTimeManager.restoreState(getApplication(), newStartTime, newTargetDays, false)
                    Log.d("Tab01ViewModel", "[FIX] TimerTimeManager synced: startTime=$newStartTime, targetDays=$newTargetDays")
                } else if (newStartTime == 0L || newCompleted) {
                    // 타이머가 중지되거나 완료되었으면 TimerTimeManager도 중지
                    TimerTimeManager.stopTimer()
                    Log.d("Tab01ViewModel", "[FIX] TimerTimeManager stopped (reset or completed)")
                }
            }
        }
    }

    init {
        // Load initial timer state
        loadTimerState()


        // [REFACTORED] TimerTimeManager에 상태 복원
        val currentStartTime = _startTime.value
        val currentTargetDays = _targetDays.value
        if (currentStartTime > 0 && !_timerCompleted.value) {
            TimerTimeManager.restoreState(getApplication(), currentStartTime, currentTargetDays, false)
            Log.d("Tab01ViewModel", "Timer restored to TimerTimeManager: startTime=$currentStartTime, targetDays=$currentTargetDays")
        }

        // [FIX] SharedPreferences 리스너 등록
        sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        Log.d("Tab01ViewModel", "[FIX] SharedPreferences listener registered")

        // [NEW] 타이머 완료 이벤트 구독
        subscribeToTimerFinishEvent()
    }

    /**
     * [NEW] 타이머 완료 이벤트 구독
     * - TimerTimeManager에서 타이머 완료 신호를 받으면 자동으로 저장하고 화면 전환
     */
    private fun subscribeToTimerFinishEvent() {
        viewModelScope.launch {
            TimerTimeManager.timerFinishEvent.collect {
                Log.d("Tab01ViewModel", "⏰ Timer finish event received!")
                handleTimerCompletion()
            }
        }
    }

    /**
     * [NEW] 타이머 완료 처리
     * - 기록 저장
     * - SharedPreferences 업데이트
     * - 네비게이션 이벤트 발행
     * [FIX] Zombie 이벤트 방지 로직 추가
     * [CRITICAL] StateFlow를 가장 먼저 업데이트하여 UI 플리커 방지
     */
    private suspend fun handleTimerCompletion() {
        try {
            // 1. [Zombie Event 방지] 타이머 시작 후 2초(2000ms) 내에 들어오는 완료 이벤트는 무시
            val timeSinceStart = System.currentTimeMillis() - lastTimerStartRequestTime
            if (timeSinceStart < 2000L) {
                Log.w("Tab01ViewModel", "⚠️ Premature timer finish event ignored (Debounce active: ${timeSinceStart}ms since start)")
                return
            }

            // 2. [Double Check] 이미 완료 처리가 된 상태라면 중복 처리 방지
            if (_timerCompleted.value) {
                Log.d("Tab01ViewModel", "Timer already completed, skipping duplicate event")
                return
            }

            val startTime = _startTime.value
            val targetDays = _targetDays.value
            val elapsedMillis = TimerTimeManager.elapsedMillis.value
            val endTime = startTime + elapsedMillis
            val actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt()

            Log.d("Tab01ViewModel", "Handling timer completion: startTime=$startTime, endTime=$endTime, targetDays=$targetDays, actualDays=$actualDays")

            // [CRITICAL FIX] 1. UI 상태를 가장 먼저 업데이트 (메모리 상태 Lock)
            // 이렇게 해야 디스크 I/O가 진행되는 동안 UI가 올바른 상태를 유지함
            _timerCompleted.value = true
            TimerTimeManager.markCompleted()
            Log.d("Tab01ViewModel", "✅ StateFlow updated FIRST: _timerCompleted = true")

            // 2. 기록 저장 (느린 디스크 작업)
            saveCompletedRecord(startTime, endTime, targetDays, actualDays)

            // 3. SharedPreferences 업데이트 (느린 디스크 작업)
            sharedPref.edit().apply {
                remove(Constants.PREF_START_TIME)
                putBoolean(Constants.PREF_TIMER_COMPLETED, true)

                // 완료된 기록 정보 저장 (DetailScreen에서 사용)
                putLong("completed_start_time", startTime)
                putLong("completed_end_time", endTime)
                putFloat("completed_target_days", targetDays)
                putInt("completed_actual_days", actualDays)
                apply()
            }

            // 4. TimerStateRepository 업데이트
            try {
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(true)
                kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
            } catch (e: Exception) {
                Log.e("Tab01ViewModel", "Failed to update TimerStateRepository", e)
            }


            // 5. Analytics 로그
            try {
                AnalyticsManager.logTimerFinish(targetDays.toInt(), actualDays, startTime, endTime)
            } catch (e: Exception) {
                Log.e("Tab01ViewModel", "Failed to log analytics", e)
            }

            // 6. [REFACTORED] 네비게이션 이벤트 발행 - Success 화면으로
            _navigationEvent.tryEmit(NavigationEvent.NavigateToSuccess)
            Log.d("Tab01ViewModel", "Navigation event emitted to SuccessScreen (celebration)")

        } catch (e: Exception) {
            Log.e("Tab01ViewModel", "Error handling timer completion", e)
        }
    }

    /**
     * [NEW] 완료된 기록 저장 (SharedPreferences 기반)
     */
    private suspend fun saveCompletedRecord(startTime: Long, endTime: Long, targetDays: Float, actualDays: Int) {
        try {
            val recordId = System.currentTimeMillis().toString()
            val isCompleted = actualDays >= targetDays
            val status = if (isCompleted) "completed" else "in_progress"

            val record = org.json.JSONObject().apply {
                put("id", recordId)
                put("startTime", startTime)
                put("endTime", endTime)
                put("targetDays", targetDays.toInt())
                put("actualDays", actualDays)
                put("isCompleted", isCompleted)
                put("status", status)
                put("createdAt", System.currentTimeMillis())
            }

            val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
            val list = try {
                org.json.JSONArray(recordsJson)
            } catch (_: Exception) {
                org.json.JSONArray()
            }

            list.put(record)
            sharedPref.edit().putString(Constants.PREF_SOBRIETY_RECORDS, list.toString()).apply()

            Log.d("Tab01ViewModel", "Record saved successfully: id=$recordId, actualDays=$actualDays")

        } catch (e: Exception) {
            Log.e("Tab01ViewModel", "Failed to save record", e)
            throw e
        }
    }


    private fun loadTimerState() {
        _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
        _targetDays.value = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)
        _timerCompleted.value = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

        Log.d("Tab01ViewModel", "Timer state loaded: startTime=${_startTime.value}, targetDays=${_targetDays.value}, completed=${_timerCompleted.value}")
    }

    /**
     * Start timer
     * [REFACTORED] TimerTimeManager에 시작 시간과 목표 일수 전달
     * [FIX] Zombie 이벤트 방지를 위한 시작 시각 기록
     */
    fun startTimer(targetDays: Float) {
        // [FIX] Zombie 이벤트 방지 - 시작 요청 시각 기록
        lastTimerStartRequestTime = System.currentTimeMillis()
        Log.d("Tab01ViewModel", "Timer start requested at: $lastTimerStartRequestTime")

        val now = System.currentTimeMillis()
        _startTime.value = now
        _targetDays.value = targetDays
        _timerCompleted.value = false

        sharedPref.edit()
            .putLong(Constants.PREF_START_TIME, now)
            .putFloat(Constants.PREF_TARGET_DAYS, targetDays)
            .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            .apply()

        // [FIX] TimerTimeManager에 목표 일수와 함께 시작 알림
        TimerTimeManager.setStartTime(now, targetDays, getApplication())

        Log.d("Tab01ViewModel", "Timer started: targetDays=$targetDays, debounce active for 2 seconds")
    }

    /**
     * Stop timer
     * [REFACTORED] TimerTimeManager에도 중지 알림
     */
    fun stopTimer() {
        _startTime.value = 0L
        _timerCompleted.value = false

        sharedPref.edit()
            .remove(Constants.PREF_START_TIME)
            .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            .apply()

        // [REFACTORED] TimerTimeManager 중지
        TimerTimeManager.stopTimer()

        Log.d("Tab01ViewModel", "Timer stopped")
    }

    /**
     * [NEW] Give up timer - User voluntarily quits
     * Records the attempt and navigates to GiveUp screen
     * [FIX] 데이터 저장 우선 -> 타이머 정지 -> 화면 이동 순서로 논리 수정
     */
    fun giveUpTimer() {
        viewModelScope.launch {
            try {
                // [STEP 1] 현재 타이머 데이터 스냅샷 저장 (초기화 전에 먼저!)
                val startTime = _startTime.value
                val targetDays = _targetDays.value
                val elapsedMillis = TimerTimeManager.elapsedMillis.value
                val endTime = startTime + elapsedMillis
                val actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt()

                Log.d("Tab01ViewModel", "[GiveUp STEP 1] 데이터 스냅샷: startTime=$startTime, endTime=$endTime, targetDays=$targetDays, actualDays=$actualDays")

                // [STEP 2] "user_settings" 파일에 포기 기록 저장 (AppNavHost와 동일한 파일)
                val editor = sharedPref.edit()
                editor.putLong("completed_start_time", startTime)
                editor.putLong("completed_end_time", endTime)
                editor.putFloat("completed_target_days", targetDays)
                editor.putInt("completed_actual_days", actualDays)
                editor.putBoolean("completed_is_give_up", true) // [NEW] 포기 플래그
                editor.apply() // 즉시 저장

                Log.d("Tab01ViewModel", "[GiveUp STEP 2] 데이터 저장 완료 -> user_settings 파일")

                // [STEP 3] DB에 기록 저장 (중단으로 표시)
                saveCompletedRecord(startTime, endTime, targetDays, actualDays)
                Log.d("Tab01ViewModel", "[GiveUp STEP 3] DB 저장 완료")

                // [STEP 4] 타이머 상태 초기화 (이제 데이터는 이미 저장됨)
                sharedPref.edit().apply {
                    remove(Constants.PREF_START_TIME)
                    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                    apply()
                }

                _startTime.value = 0L
                _timerCompleted.value = false
                TimerTimeManager.stopTimer()

                Log.d("Tab01ViewModel", "[GiveUp STEP 4] 타이머 정지 완료")

                // [STEP 5] GiveUp 화면으로 이동
                _navigationEvent.tryEmit(NavigationEvent.NavigateToGiveUp)
                Log.d("Tab01ViewModel", "[GiveUp STEP 5] 화면 이동 이벤트 발행 완료 🍃")

            } catch (e: Exception) {
                Log.e("Tab01ViewModel", "[GiveUp ERROR] 포기 처리 중 오류 발생", e)
            }
        }
    }

    /**
     * Mark timer as completed
     * [REFACTORED] TimerTimeManager에도 완료 알림
     */
    fun completeTimer() {
        _timerCompleted.value = true

        sharedPref.edit()
            .putBoolean(Constants.PREF_TIMER_COMPLETED, true)
            .apply()

        // [REFACTORED] TimerTimeManager 완료 표시
        TimerTimeManager.markCompleted()

        Log.d("Tab01ViewModel", "Timer completed")
    }

    /**
     * Refresh timer state
     */
    fun refreshTimerState() {
        loadTimerState()
    }

    /**
     * [FIX] ViewModel 정리 시 리스너 해제 (메모리 누수 방지)
     */
    override fun onCleared() {
        super.onCleared()
        sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        Log.d("Tab01ViewModel", "[FIX] SharedPreferences listener unregistered")
    }
}