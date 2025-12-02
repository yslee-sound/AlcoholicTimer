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
            apply()
        }
        Log.d(TAG, "타이머 초기화 완료")
    }

    // [NEW] 타이머 테스트 모드 관련 함수들
    private const val PREF_IS_TIMER_TEST_MODE_ENABLED = "IS_TIMER_TEST_MODE_ENABLED"

    /**
     * 타이머 테스트 모드 설정 저장
     * @param isEnabled true이면 N일 → N초, false이면 정상 모드
     */
    fun setTimerTestModeEnabled(isEnabled: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(PREF_IS_TIMER_TEST_MODE_ENABLED, isEnabled)
            apply()
        }
        Log.d(TAG, "타이머 테스트 모드 설정: $isEnabled (${if (isEnabled) "N일 → N초" else "정상 모드"})")
    }

    /**
     * 타이머 테스트 모드 상태 확인
     * @return true이면 테스트 모드 활성화 (N일 → N초), false이면 정상 모드
     */
    fun isTimerTestModeEnabled(): Boolean {
        val isEnabled = sharedPreferences?.getBoolean(PREF_IS_TIMER_TEST_MODE_ENABLED, false) ?: false
        Log.d(TAG, "타이머 테스트 모드 확인: $isEnabled")
        return isEnabled
    }

    /**
     * 타이머 시간 스케일링 팩터 반환
     * @return 테스트 모드면 1L (초), 정상 모드면 86400L (1일의 초)
     */
    fun getTimeScalingFactor(): Long {
        val isTestMode = isTimerTestModeEnabled()
        val factor = if (isTestMode) 1L else 86400L
        Log.d(TAG, "시간 스케일링 팩터: $factor (${if (isTestMode) "테스트 모드: 1초" else "정상 모드: 1일"})")
        return factor
    }

    /**
     * 입력된 일수를 실제 초로 변환
     * @param inputDays 사용자가 입력한 일수
     * @return 실제 타이머에 사용될 총 초 (테스트 모드면 inputDays초, 정상 모드면 inputDays*86400초)
     */
    fun convertDaysToSeconds(inputDays: Long): Long {
        val scalingFactor = getTimeScalingFactor()
        val totalSeconds = inputDays * scalingFactor
        Log.d(TAG, "일수 변환: $inputDays 일 → $totalSeconds 초 (팩터: $scalingFactor)")
        return totalSeconds
    }
}

