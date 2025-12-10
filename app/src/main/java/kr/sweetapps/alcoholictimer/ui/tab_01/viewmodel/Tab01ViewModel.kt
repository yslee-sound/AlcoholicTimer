package kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.util.constants.Constants

/**
 * [NEW] Tab01 (Start/Run screen) state management ViewModel
 * - Timer state management (start time, target days, completion status)
 * - SharedPreferences read/write operations
 */
class Tab01ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        Constants.USER_SETTINGS_PREFS,
        Context.MODE_PRIVATE
    )

    // [NEW] Timer start time state
    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    // [NEW] Target days state
    private val _targetDays = MutableStateFlow(30f)
    val targetDays: StateFlow<Float> = _targetDays.asStateFlow()

    // [NEW] Timer completion status state
    private val _timerCompleted = MutableStateFlow(false)
    val timerCompleted: StateFlow<Boolean> = _timerCompleted.asStateFlow()

    // [NEW] Current virtual time (with acceleration applied)
    private val _currentVirtualTime = MutableStateFlow(System.currentTimeMillis())
    val currentVirtualTime: StateFlow<Long> = _currentVirtualTime.asStateFlow()

    // [NEW] Elapsed time in milliseconds (with acceleration applied)
    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    init {
        // [NEW] Load initial timer state
        loadTimerState()

        // [FIX] Initialize virtual time correctly for running timer
        // If timer is already running, set virtual time to current real time
        // (acceleration will be applied in the loop)
        _currentVirtualTime.value = System.currentTimeMillis()

        // [FIX] Calculate initial elapsed time if timer is running
        val currentStartTime = _startTime.value
        if (currentStartTime > 0 && !_timerCompleted.value) {
            // Timer is running - calculate elapsed time
            _elapsedMillis.value = System.currentTimeMillis() - currentStartTime
            Log.d("Tab01ViewModel", "Timer is running - initial elapsed: ${_elapsedMillis.value}ms")
        } else {
            _elapsedMillis.value = 0L
            Log.d("Tab01ViewModel", "Timer is not running - elapsed: 0ms")
        }

        // [FIX] Self-Healing: Detect and fix zombie state
        // If timer is marked as completed but app is restarted, auto-reset to prevent ghost touch
        performSelfHealing()

        // [NEW] Start real-time timer loop in viewModelScope
        // This runs independently of UI lifecycle - survives tab switches
        startTimerLoop()
    }

    /**
     * [NEW] Real-time timer loop running in viewModelScope
     * Continuously updates elapsed time based on actual time difference
     * Runs independently of UI - survives screen rotations and tab switches
     */
    private fun startTimerLoop() {
        viewModelScope.launch {
            var lastRealTime = System.currentTimeMillis()

            while (true) {
                kotlinx.coroutines.delay(100L) // Update every 0.1 seconds for smooth animation

                val currentRealTime = System.currentTimeMillis()
                val realDelta = currentRealTime - lastRealTime
                lastRealTime = currentRealTime

                // [NEW] Get acceleration factor (debug mode only)
                val factor = if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                    try {
                        Constants.getTimeAcceleration(getApplication())
                    } catch (e: Exception) {
                        1 // Fallback to 1x if error
                    }
                } else {
                    1
                }

                // [FIX] Update elapsed time if timer is running
                // Use real elapsed time from start, not accumulated virtual delta
                val currentStartTime = _startTime.value
                if (currentStartTime > 0 && !_timerCompleted.value) {
                    // Calculate actual elapsed time from start
                    val realElapsed = currentRealTime - currentStartTime
                    // Apply acceleration factor
                    val virtualElapsed = realElapsed * factor
                    _elapsedMillis.value = virtualElapsed
                } else {
                    _elapsedMillis.value = 0L
                }
            }
        }
    }

    /**
     * [NEW] Self-Healing Logic: Automatic state recovery
     * Prevents "ghost touch" bug caused by zombie state
     */
    private fun performSelfHealing() {
        val isCompleted = _timerCompleted.value
        val hasStartTime = _startTime.value > 0

        if (isCompleted) {
            // Case 1: Timer is marked as completed
            // Check if user already acknowledged (more than 10 seconds passed)
            val completedRecordExists = checkCompletedRecordExists()

            if (completedRecordExists) {
                // User already saw the result screen, reset the flag
                Log.w("Tab01ViewModel", "[Self-Healing] Zombie state detected - resetting completed flag")
                resetCompletedFlag()
            } else {
                Log.d("Tab01ViewModel", "[Self-Healing] Timer completion is fresh - keeping state")
            }
        } else if (!hasStartTime) {
            // Case 2: No timer running and not completed - this is normal idle state
            Log.d("Tab01ViewModel", "[Self-Healing] Idle state - no action needed")
        }
    }

    /**
     * Check if completed record data exists in SharedPreferences
     */
    private fun checkCompletedRecordExists(): Boolean {
        val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
        return completedStartTime > 0L
    }

    /**
     * Reset completed flag to allow new timer start
     */
    private fun resetCompletedFlag() {
        _timerCompleted.value = false
        sharedPref.edit()
            .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            .apply()
    }

    /**
     * [NEW] Load timer state from SharedPreferences
     */
    private fun loadTimerState() {
        _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
        _targetDays.value = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)
        _timerCompleted.value = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

        Log.d("Tab01ViewModel", "Timer state loaded: startTime=${_startTime.value}, targetDays=${_targetDays.value}, completed=${_timerCompleted.value}")
    }

    /**
     * [NEW] Start timer
     */
    fun startTimer(targetDays: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _startTime.value = now
            _targetDays.value = targetDays
            _timerCompleted.value = false
            _currentVirtualTime.value = now // [FIX] Reset virtual time to current time
            _elapsedMillis.value = 0L

            sharedPref.edit()
                .putLong(Constants.PREF_START_TIME, now)
                .putFloat(Constants.PREF_TARGET_DAYS, targetDays)
                .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                .apply()

            Log.d("Tab01ViewModel", "Timer started: targetDays=$targetDays")
        }
    }

    /**
     * [NEW] Stop timer
     */
    fun stopTimer() {
        viewModelScope.launch {
            _startTime.value = 0L
            _timerCompleted.value = false
            _elapsedMillis.value = 0L

            sharedPref.edit()
                .remove(Constants.PREF_START_TIME)
                .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                .apply()

            Log.d("Tab01ViewModel", "Timer stopped")
        }
    }

    /**
     * [NEW] Mark timer as completed
     */
    fun completeTimer() {
        viewModelScope.launch {
            _timerCompleted.value = true

            sharedPref.edit()
                .putBoolean(Constants.PREF_TIMER_COMPLETED, true)
                .apply()

            Log.d("Tab01ViewModel", "Timer completed")
        }
    }

    /**
     * [NEW] Refresh timer state
     */
    fun refreshTimerState() {
        loadTimerState()
    }
}