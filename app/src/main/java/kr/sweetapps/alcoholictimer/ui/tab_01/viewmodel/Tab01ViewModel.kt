package kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.util.manager.TimerTimeManager

/**
 * Tab01 (Start/Run screen) state management ViewModel
 * [REFACTORED] 이제 TimerTimeManager를 사용하여 중앙 집중식 시간 관리
 */
class Tab01ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        Constants.USER_SETTINGS_PREFS,
        Context.MODE_PRIVATE
    )

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

    init {
        // Load initial timer state
        loadTimerState()

        // Self-Healing: Detect and fix zombie state
        performSelfHealing()

        // [REFACTORED] TimerTimeManager에 상태 복원
        val currentStartTime = _startTime.value
        if (currentStartTime > 0 && !_timerCompleted.value) {
            TimerTimeManager.restoreState(getApplication(), currentStartTime, false)
            Log.d("Tab01ViewModel", "Timer restored to TimerTimeManager: startTime=$currentStartTime")
        }
    }

    /**
     * Self-Healing Logic: Automatic state recovery
     */
    private fun performSelfHealing() {
        val isCompleted = _timerCompleted.value
        val hasStartTime = _startTime.value > 0

        if (isCompleted) {
            val completedRecordExists = checkCompletedRecordExists()
            if (completedRecordExists) {
                Log.w("Tab01ViewModel", "[Self-Healing] Zombie state detected - resetting completed flag")
                resetCompletedFlag()
            } else {
                Log.d("Tab01ViewModel", "[Self-Healing] Timer completion is fresh - keeping state")
            }
        } else if (!hasStartTime) {
            Log.d("Tab01ViewModel", "[Self-Healing] Idle state - no action needed")
        }
    }

    private fun checkCompletedRecordExists(): Boolean {
        val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
        return completedStartTime > 0L
    }

    private fun resetCompletedFlag() {
        _timerCompleted.value = false
        sharedPref.edit()
            .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            .apply()
    }

    private fun loadTimerState() {
        _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
        _targetDays.value = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)
        _timerCompleted.value = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

        Log.d("Tab01ViewModel", "Timer state loaded: startTime=${_startTime.value}, targetDays=${_targetDays.value}, completed=${_timerCompleted.value}")
    }

    /**
     * Start timer
     * [REFACTORED] TimerTimeManager에 시작 시간 전달
     */
    fun startTimer(targetDays: Float) {
        val now = System.currentTimeMillis()
        _startTime.value = now
        _targetDays.value = targetDays
        _timerCompleted.value = false

        sharedPref.edit()
            .putLong(Constants.PREF_START_TIME, now)
            .putFloat(Constants.PREF_TARGET_DAYS, targetDays)
            .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            .apply()

        // [REFACTORED] TimerTimeManager에 시작 알림
        TimerTimeManager.setStartTime(now, getApplication())

        Log.d("Tab01ViewModel", "Timer started: targetDays=$targetDays")
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
}