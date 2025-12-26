package kr.sweetapps.alcoholictimer.util.constants

import android.content.Context
import androidx.core.content.edit
import java.io.File

object Constants {
    const val PREFS_NAME = "AlcoholicTimerPrefs"
    const val USER_SETTINGS_PREFS = "user_settings"
    const val PREF_KEY_TEST_MODE = "test_mode"
    const val PREF_TEST_MODE = "test_mode"
    const val PREF_START_TIME = "start_time"
    const val PREF_TARGET_DAYS = "target_days"
    const val PREF_RECORDS = "records"
    const val PREF_TIMER_COMPLETED = "timer_completed"
    const val PREF_SOBRIETY_RECORDS = "sobriety_records"

    const val PREF_SELECTED_COST = "selected_cost"
    const val PREF_SELECTED_FREQUENCY = "selected_frequency"
    const val PREF_SELECTED_DURATION = "selected_duration"
    const val PREF_SETTINGS_INITIALIZED = "settings_initialized"

    const val KEY_COST_LOW = "cost_low"
    const val KEY_COST_MEDIUM = "cost_medium"
    const val KEY_COST_HIGH = "cost_high"

    const val KEY_FREQUENCY_LOW = "frequency_low"
    const val KEY_FREQUENCY_MEDIUM = "frequency_medium"
    const val KEY_FREQUENCY_HIGH = "frequency_high"

    const val KEY_DURATION_SHORT = "duration_short"
    const val KEY_DURATION_MEDIUM = "duration_medium"
    const val KEY_DURATION_LONG = "duration_long"

    const val DEFAULT_COST = KEY_COST_MEDIUM
    const val DEFAULT_FREQUENCY = KEY_FREQUENCY_MEDIUM
    const val DEFAULT_DURATION = KEY_DURATION_MEDIUM

    // UI 관련 상수는 `UiConstants.kt`로 이동했습니다.

    object DrinkingSettings {
        const val COST_LOW = 10000
        const val COST_MEDIUM = 40000
        const val COST_HIGH = 70000
        const val COST_DEFAULT = COST_MEDIUM

        const val FREQUENCY_LOW = 1.0
        const val FREQUENCY_MEDIUM = 2.5
        const val FREQUENCY_HIGH = 5.0
        const val FREQUENCY_DEFAULT = FREQUENCY_MEDIUM

        const val DURATION_SHORT = 1.5
        const val DURATION_MEDIUM = 4.0
        const val DURATION_LONG = 6.0
        const val DURATION_DEFAULT = DURATION_MEDIUM

        // [NEW] 칼로리 계산 상수 맥주500 = 250kcal, 소주1병 = 400kcal
        const val CALORIES_PER_DRINK = 300.0 // 1병당 평균 칼로리 (kcal)

        // [REMOVED] 숙취 시간 상수 - 계산식에서 제거됨
        // const val HANGOVER_HOURS = 5.0

        fun getCostValue(cost: String): Int {
            return when (cost) {
                KEY_COST_LOW -> COST_LOW
                KEY_COST_MEDIUM -> COST_MEDIUM
                KEY_COST_HIGH -> COST_HIGH
                else -> COST_DEFAULT
            }
        }

        fun getFrequencyValue(frequency: String): Double {
            return when (frequency) {
                KEY_FREQUENCY_LOW -> FREQUENCY_LOW
                KEY_FREQUENCY_MEDIUM -> FREQUENCY_MEDIUM
                KEY_FREQUENCY_HIGH -> FREQUENCY_HIGH
                else -> FREQUENCY_DEFAULT
            }
        }

        fun getDurationValue(duration: String): Double {
            return when (duration) {
                KEY_DURATION_SHORT -> DURATION_SHORT
                KEY_DURATION_MEDIUM -> DURATION_MEDIUM
                KEY_DURATION_LONG -> DURATION_LONG
                else -> DURATION_DEFAULT
            }
        }
    }

    const val TEST_MODE_REAL = 0

    var currentTestMode = TEST_MODE_REAL



    const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24
    const val MINUTE_IN_MILLIS = 1000L * 60
    const val SECOND_IN_MILLIS = 1000L

    // [REMOVED] 시간 배속 설정 - 타임머신 기능과 충돌 방지를 위해 제거 (2025-12-26)

    const val RESULT_SCREEN_DELAY = 2000
    const val DEFAULT_VALUE = 2000
    // [REMOVED] 숙취 시간 기본값 - 더 이상 사용하지 않음
    // const val DEFAULT_HANGOVER_HOURS = 5

    val LEVEL_TIME_UNIT_MILLIS: Long get() = DAY_IN_MILLIS
    val LEVEL_TIME_UNIT_TEXT: String get() = "일"

    const val STATUS_COMPLETED = "완료"

    fun keyCurrentIndicator(startTime: Long): String = "current_indicator_${startTime}"

    // [CHANGED] 레벨 계산: '꽉 채운 일수' 기준 (floor 방식, +1 제거) (2025-12-25)
    // 예: 2.7일 → floor(2.7) = 2일, 0.5일 → floor(0.5) = 0일
    fun calculateLevelDays(elapsedTimeMillis: Long): Int {
        if (elapsedTimeMillis <= 0L) return 0 // [FIX] 기록 없으면 0일

        val days = (elapsedTimeMillis / DAY_IN_MILLIS).toInt() // floor 연산 (소수점 버림)
        return days // [CHANGED] +1 제거 - 꽉 채운 일수만 표시
    }

    // [CHANGED] 레벨 계산: 동적 dayInMillis를 받는 오버로드 (테스트 모드 대응)
    fun calculateLevelDays(elapsedTimeMillis: Long, dayInMillis: Long): Int {
        if (elapsedTimeMillis <= 0L) return 0 // [FIX] 기록 없으면 0일

        val days = (elapsedTimeMillis / dayInMillis).toInt() // floor 연산 (소수점 버림)
        return days // [CHANGED] +1 제거 - 꽉 채운 일수만 표시
    }

    fun calculateLevelDaysFloat(elapsedTimeMillis: Long): Float = (elapsedTimeMillis / DAY_IN_MILLIS.toFloat())

    fun init(context: Context) { currentTestMode = TEST_MODE_REAL }

    fun updateTestMode(mode: Int) { currentTestMode = TEST_MODE_REAL }

    fun initializeUserSettings(context: Context) {
        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val isInitialized = sharedPref.getBoolean(PREF_SETTINGS_INITIALIZED, false)
        // ...existing code...
    }

    fun getUserSettings(context: Context): Triple<String, String, String> {
        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val cost = sharedPref.getString(PREF_SELECTED_COST, DEFAULT_COST) ?: DEFAULT_COST
        val frequency = sharedPref.getString(PREF_SELECTED_FREQUENCY, DEFAULT_FREQUENCY) ?: DEFAULT_FREQUENCY
        val duration = sharedPref.getString(PREF_SELECTED_DURATION, DEFAULT_DURATION) ?: DEFAULT_DURATION
        return Triple(cost, frequency, duration)
    }

    private const val INSTALL_MARKER_NAME = "install_marker_v1"
    private const val FRESH_INSTALL_WINDOW_MILLIS = 60 * 60 * 1000L // 1시간 내 설치면 재설치로 간주

    fun ensureInstallMarkerAndResetIfReinstalled(context: Context) {
        val markerFile = File(context.noBackupFilesDir, INSTALL_MARKER_NAME)
        if (markerFile.exists()) return

        val pm = context.packageManager
        val firstInstallTime = try {
            pm.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        val isRecentInstall = (System.currentTimeMillis() - firstInstallTime) < FRESH_INSTALL_WINDOW_MILLIS

        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        if (isRecentInstall) {
            sharedPref.edit {
                remove(PREF_START_TIME)
                remove(PREF_TARGET_DAYS)
                putBoolean(PREF_TIMER_COMPLETED, false)
            }
        }
        try { markerFile.writeText("1") } catch (_: Exception) { }
    }
}