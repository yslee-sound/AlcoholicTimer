package kr.sweetapps.alcoholictimer.util.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.util.constants.Constants

/**
 * [NEW] 중앙 집중식 타이머 시간 관리자 (Singleton)
 * - 모든 ViewModel과 화면이 이 하나의 시계를 공유
 * - 시간 배속(Acceleration)을 중앙에서 관리하여 일관성 보장
 * - [FIX] 타이머 완료 감지 및 자동 종료 처리 추가
 * - Application 생명주기 동안 유지됨
 */
object TimerTimeManager {
    private const val TAG = "TimerTimeManager"

    // Application scope coroutine (ViewModel과 무관하게 유지)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 타이머 시작 시간
    private var startTime: Long = 0L

    // [NEW] 목표 시간 (밀리초)
    private var targetMillis: Long = 0L

    // 타이머 완료 여부
    private var isCompleted: Boolean = false

    // 경과 시간 (배속 적용됨)
    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    // 타이머 활성 상태
    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    // [NEW] 타이머 완료 이벤트 (SharedFlow - 일회성 이벤트)
    private val _timerFinishEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinishEvent: SharedFlow<Unit> = _timerFinishEvent.asSharedFlow()

    // 루프 실행 여부 (중복 실행 방지)
    private var isLoopRunning = false

    /**
     * [FIX] 타이머 시작 (목표 시간 추가)
     * @param startTime 타이머 시작 시간 (밀리초)
     * @param targetDays 목표 일수
     * @param context Context (배속 설정 읽기용)
     */
    fun setStartTime(startTime: Long, targetDays: Float, context: Context) {
        Log.d(TAG, "setStartTime: $startTime, targetDays=$targetDays")
        this.startTime = startTime
        this.targetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
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
        targetMillis = 0L
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
     * [FIX] SharedPreferences에서 상태 복원 (목표 시간 추가)
     */
    fun restoreState(context: Context, startTime: Long, targetDays: Float, completed: Boolean) {
        Log.d(TAG, "restoreState: startTime=$startTime, targetDays=$targetDays, completed=$completed")
        this.startTime = startTime
        this.targetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
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
     * - [FIX] 목표 시간 도달 시 자동 완료 처리
     */
    private fun startTimerLoop(context: Context) {
        if (isLoopRunning) {
            Log.w(TAG, "Timer loop already running, skipping")
            return
        }

        isLoopRunning = true
        Log.d(TAG, "Starting timer loop with targetMillis=$targetMillis")

        scope.launch {
            while (true) {
                delay(100L) // 0.1초마다 갱신

                if (startTime == 0L || isCompleted) {
                    // 타이머가 없거나 완료되면 0으로 유지
                    _elapsedMillis.value = 0L
                    continue
                }

                val currentRealTime = System.currentTimeMillis()

                // [REMOVED] 배속 계수 제거 - 항상 실제 시간만 사용 (2025-12-26)

                // [핵심] 실제 경과 시간 계산 (실제 시간만 사용)
                val realElapsed = currentRealTime - startTime

                // [FIX] 목표 시간 도달 확인
                if (targetMillis > 0 && realElapsed >= targetMillis) {
                    // [중요] 시간을 목표 시간에 고정 (Clamp)
                    _elapsedMillis.value = targetMillis
                    isCompleted = true
                    _isTimerActive.value = false

                    Log.d(TAG, "⏰ Timer finished! realElapsed=$realElapsed, targetMillis=$targetMillis")

                    // [NEW] 타이머 완료 이벤트 발행
                    _timerFinishEvent.tryEmit(Unit)

                    // 루프는 계속 실행하지만 완료 상태 유지
                    continue
                }

                _elapsedMillis.value = realElapsed
            }
        }
    }
}

