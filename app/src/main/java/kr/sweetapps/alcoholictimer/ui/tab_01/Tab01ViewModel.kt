// [NEW] Tab01 리팩토링: Start/Run 화면 ViewModel 추가
package kr.sweetapps.alcoholictimer.ui.tab_01

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.constants.Constants

/**
 * [NEW] Tab01(시작/실행 화면) 상태 관리 ViewModel
 * - 타이머 상태 관리 (시작 시간, 목표 일수, 완료 여부)
 * - SharedPreferences 읽기/쓰기 처리
 */
class Tab01ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        Constants.USER_SETTINGS_PREFS,
        android.content.Context.MODE_PRIVATE
    )

    // [NEW] 타이머 시작 시간 상태
    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    // [NEW] 목표 일수 상태
    private val _targetDays = MutableStateFlow(30f)
    val targetDays: StateFlow<Float> = _targetDays.asStateFlow()

    // [NEW] 타이머 완료 여부 상태
    private val _timerCompleted = MutableStateFlow(false)
    val timerCompleted: StateFlow<Boolean> = _timerCompleted.asStateFlow()

    init {
        // [NEW] 초기 타이머 상태 로드
        loadTimerState()
    }

    /**
     * [NEW] SharedPreferences에서 타이머 상태 불러오기
     */
    private fun loadTimerState() {
        _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
        _targetDays.value = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 30f)
        _timerCompleted.value = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

        Log.d("Tab01ViewModel", "타이머 상태 로드: startTime=${_startTime.value}, targetDays=${_targetDays.value}, completed=${_timerCompleted.value}")
    }

    /**
     * [NEW] 타이머 시작
     */
    fun startTimer(targetDays: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _startTime.value = now
            _targetDays.value = targetDays
            _timerCompleted.value = false

            sharedPref.edit()
                .putLong(Constants.PREF_START_TIME, now)
                .putFloat(Constants.PREF_TARGET_DAYS, targetDays)
                .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                .apply()

            Log.d("Tab01ViewModel", "타이머 시작: targetDays=$targetDays")
        }
    }

    /**
     * [NEW] 타이머 중지
     */
    fun stopTimer() {
        viewModelScope.launch {
            _startTime.value = 0L
            _timerCompleted.value = false

            sharedPref.edit()
                .remove(Constants.PREF_START_TIME)
                .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                .apply()

            Log.d("Tab01ViewModel", "타이머 중지")
        }
    }

    /**
     * [NEW] 타이머 완료 처리
     */
    fun completeTimer() {
        viewModelScope.launch {
            _timerCompleted.value = true

            sharedPref.edit()
                .putBoolean(Constants.PREF_TIMER_COMPLETED, true)
                .apply()

            Log.d("Tab01ViewModel", "타이머 완료")
        }
    }

    /**
     * [NEW] 타이머 상태 새로고침
     */
    fun refreshTimerState() {
        loadTimerState()
    }
}

