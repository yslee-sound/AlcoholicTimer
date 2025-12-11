package kr.sweetapps.alcoholictimer.util.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.util.constants.Constants

/**
 * [NEW] 중앙 집중식 타이머 시간 관리자 (Singleton)
 * - 모든 ViewModel과 화면이 이 하나의 시계를 공유
 * - 시간 배속(Acceleration)을 중앙에서 관리하여 일관성 보장
 * - Application 생명주기 동안 유지됨
 */
object TimerTimeManager {
    private const val TAG = "TimerTimeManager"

    // Application scope coroutine (ViewModel과 무관하게 유지)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 타이머 시작 시간
    private var startTime: Long = 0L

    // 타이머 완료 여부
    private var isCompleted: Boolean = false

    // 경과 시간 (배속 적용됨)
    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    // 타이머 활성 상태
    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    // 루프 실행 여부 (중복 실행 방지)
    private var isLoopRunning = false

    /**
     * 타이머 시작
     * @param startTime 타이머 시작 시간 (밀리초)
     * @param context Context (배속 설정 읽기용)
     */
    fun setStartTime(startTime: Long, context: Context) {
        Log.d(TAG, "setStartTime: $startTime")
        this.startTime = startTime
        this.isCompleted = false
        _isTimerActive.value = true

        if (!isLoopRunning) {
            startTimerLoop(context)
        }
    }

    /**
     * 타이머 중지
     */
    fun stopTimer() {
        Log.d(TAG, "stopTimer")
        startTime = 0L
        isCompleted = false
        _elapsedMillis.value = 0L
        _isTimerActive.value = false
    }

    /**
     * 타이머 완료 표시
     */
    fun markCompleted() {
        Log.d(TAG, "markCompleted")
        isCompleted = true
        _isTimerActive.value = false
    }

    /**
     * SharedPreferences에서 상태 복원
     */
    fun restoreState(context: Context, startTime: Long, completed: Boolean) {
        Log.d(TAG, "restoreState: startTime=$startTime, completed=$completed")
        this.startTime = startTime
        this.isCompleted = completed

        if (startTime > 0 && !completed) {
            _isTimerActive.value = true
            if (!isLoopRunning) {
                startTimerLoop(context)
            }
        } else {
            _isTimerActive.value = false
            _elapsedMillis.value = 0L
        }
    }

    /**
     * [핵심] 실시간 타이머 루프
     * - Application 생명주기 동안 계속 실행
     * - 0.1초마다 배속을 적용하여 시간 업데이트
     */
    private fun startTimerLoop(context: Context) {
        if (isLoopRunning) {
            Log.w(TAG, "Timer loop already running, skipping")
            return
        }

        isLoopRunning = true
        Log.d(TAG, "Starting timer loop")

        scope.launch {
            var lastRealTime = System.currentTimeMillis()

            while (true) {
                delay(100L) // 0.1초마다 갱신

                if (startTime == 0L || isCompleted) {
                    // 타이머가 없거나 완료되면 0으로 유지
                    _elapsedMillis.value = 0L
                    continue
                }

                val currentRealTime = System.currentTimeMillis()
                val realDelta = currentRealTime - lastRealTime
                lastRealTime = currentRealTime

                // 배속 계수 가져오기 (Debug 모드일 때만)
                val accelerationFactor = if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                    try {
                        Constants.getTimeAcceleration(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get acceleration factor", e)
                        1
                    }
                } else {
                    1
                }

                // [핵심] 실제 경과 시간 계산 (배속 적용)
                val realElapsed = currentRealTime - startTime
                val virtualElapsed = realElapsed * accelerationFactor

                _elapsedMillis.value = virtualElapsed
            }
        }
    }
}

