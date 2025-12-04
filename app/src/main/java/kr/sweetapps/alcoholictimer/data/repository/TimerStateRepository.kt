// [NEW] 타이머 상태 관리 레포지토리
package kr.sweetapps.alcoholictimer.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kr.sweetapps.alcoholictimer.constants.Constants

/**
 * 타이머 만료 상태를 관리하는 싱글톤 레포지토리
 * SharedPreferences를 통해 영구적으로 상태를 저장합니다.
 */
object TimerStateRepository {
    private const val TAG = "TimerStateRepository"

    private var sharedPreferences: SharedPreferences? = null

    // [NEW] SharedPreferences 키 상수
    private const val KEY_IS_TIMER_ACTIVE = "is_timer_active"

    /**
     * 레포지토리 초기화 (Application.onCreate에서 호출)
     */
    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(
                Constants.USER_SETTINGS_PREFS,
                Context.MODE_PRIVATE
            )
            Log.d(TAG, "TimerStateRepository 초기화 완료")
        }
    }

    /**
     * 타이머 만료 상태 저장
     * @param isFinished true이면 타이머 만료, false이면 작동 중
     */
    fun setTimerFinished(isFinished: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(Constants.PREF_TIMER_COMPLETED, isFinished)
            apply()
        }
        Log.d(TAG, "타이머 만료 상태 저장: $isFinished")
    }

    /**
     * 타이머 만료 상태 확인
     * @return true이면 타이머 만료, false이면 작동 중 (기본값: false)
     */
    fun isTimerFinished(): Boolean {
        val isFinished = sharedPreferences?.getBoolean(Constants.PREF_TIMER_COMPLETED, false) ?: false
        Log.d(TAG, "타이머 만료 상태 확인: $isFinished")
        return isFinished
    }

    /**
     * 타이머 작동 중 상태 저장
     * @param isActive true이면 타이머 작동 중, false이면 중지/설정 전
     */
    fun setTimerActive(isActive: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_TIMER_ACTIVE, isActive)
            apply()
        }
        Log.d(TAG, "타이머 작동 중 상태 저장: $isActive")
    }

    /**
     * 타이머 작동 중 상태 확인
     * @return true이면 타이머 작동 중, false이면 중지/설정 전 (기본값: false)
     */
    fun isTimerActive(): Boolean {
        val isActive = sharedPreferences?.getBoolean(KEY_IS_TIMER_ACTIVE, false) ?: false
        Log.d(TAG, "타이머 작동 중 상태 확인: $isActive")
        return isActive
    }

    /**
     * 타이머 시작 시간 저장
     */
    fun setStartTime(startTime: Long) {
        sharedPreferences?.edit()?.apply {
            putLong(Constants.PREF_START_TIME, startTime)
            apply()
        }
        Log.d(TAG, "타이머 시작 시간 저장: $startTime")
    }

    /**
     * 타이머 시작 시간 가져오기
     */
    fun getStartTime(): Long {
        return sharedPreferences?.getLong(Constants.PREF_START_TIME, 0L) ?: 0L
    }

    /**
     * 타이머가 실행 중인지 확인
     * @return true이면 타이머 실행 중, false이면 중지 상태
     */
    fun isTimerRunning(): Boolean {
        val startTime = getStartTime()
        val isRunning = startTime > 0L && !isTimerFinished()
        Log.d(TAG, "타이머 실행 중 확인: $isRunning (startTime=$startTime)")
        return isRunning
    }

    /**
     * 타이머 초기화 (새로운 타이머 시작 시)
     */
    fun resetTimer() {
        sharedPreferences?.edit()?.apply {
            putLong(Constants.PREF_START_TIME, 0L)
            putBoolean(Constants.PREF_TIMER_COMPLETED, false)
            putBoolean(KEY_IS_TIMER_ACTIVE, false)
            apply()
        }
        Log.d(TAG, "타이머 초기화 완료")
    }
}

