@file:Suppress("unused")

package com.sweetapps.alcoholictimer.core.util

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

    // 언어 독립적인 설정 키 값 (내부 저장용)
    const val KEY_COST_LOW = "cost_low"
    const val KEY_COST_MEDIUM = "cost_medium"
    const val KEY_COST_HIGH = "cost_high"

    const val KEY_FREQUENCY_LOW = "frequency_low"
    const val KEY_FREQUENCY_MEDIUM = "frequency_medium"
    const val KEY_FREQUENCY_HIGH = "frequency_high"

    const val KEY_DURATION_SHORT = "duration_short"
    const val KEY_DURATION_MEDIUM = "duration_medium"
    const val KEY_DURATION_LONG = "duration_long"

    // 기본값 (언어 독립적)
    const val DEFAULT_COST = KEY_COST_LOW
    const val DEFAULT_FREQUENCY = KEY_FREQUENCY_LOW
    const val DEFAULT_DURATION = KEY_DURATION_SHORT

    // 음주 설정 값 상수
    object DrinkingSettings {
        // 음주 비용 (원)
        const val COST_LOW = 10000
        const val COST_MEDIUM = 40000
        const val COST_HIGH = 70000
        const val COST_DEFAULT = COST_MEDIUM

        // 음주 빈도 (주당 횟수)
        const val FREQUENCY_LOW = 1.0      // 주 1회 이하
        const val FREQUENCY_MEDIUM = 2.5   // 주 2~3회
        const val FREQUENCY_HIGH = 5.0     // 주 4회 이상
        const val FREQUENCY_DEFAULT = FREQUENCY_MEDIUM

        // 음주 시간 (시간)
        const val DURATION_SHORT = 1.5     // 짧음
        const val DURATION_MEDIUM = 4.0    // 보통
        const val DURATION_LONG = 6.0      // 길게
        const val DURATION_DEFAULT = DURATION_MEDIUM

        const val HANGOVER_HOURS = 5.0

        /**
         * 사용자가 선택한 음주 비용 키를 실제 금액으로 변환
         */
        fun getCostValue(cost: String): Int {
            return when (cost) {
                KEY_COST_LOW -> COST_LOW
                KEY_COST_MEDIUM -> COST_MEDIUM
                KEY_COST_HIGH -> COST_HIGH
                else -> COST_DEFAULT
            }
        }

        /**
         * 사용자가 선택한 음주 빈도 키를 실제 주당 횟수로 변환
         */
        fun getFrequencyValue(frequency: String): Double {
            return when (frequency) {
                KEY_FREQUENCY_LOW -> FREQUENCY_LOW
                KEY_FREQUENCY_MEDIUM -> FREQUENCY_MEDIUM
                KEY_FREQUENCY_HIGH -> FREQUENCY_HIGH
                else -> FREQUENCY_DEFAULT
            }
        }

        /**
         * 사용자가 선택한 음주 시간 키를 실제 시간으로 변환
         */
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
    const val TEST_MODE_MINUTE = 1
    const val TEST_MODE_SECOND = 2

    var currentTestMode = TEST_MODE_REAL

    val isTestMode: Boolean get() = currentTestMode != TEST_MODE_REAL
    val isSecondTestMode: Boolean get() = currentTestMode == TEST_MODE_SECOND
    val isMinuteTestMode: Boolean get() = currentTestMode == TEST_MODE_MINUTE

    const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24
    const val MINUTE_IN_MILLIS = 1000L * 60
    const val SECOND_IN_MILLIS = 1000L

    const val RESULT_SCREEN_DELAY = 2000
    const val DEFAULT_VALUE = 2000
    const val DEFAULT_HANGOVER_HOURS = 5

    val LEVEL_TIME_UNIT_MILLIS: Long get() = DAY_IN_MILLIS
    val LEVEL_TIME_UNIT_TEXT: String get() = "일"

    const val STATUS_COMPLETED = "완료"

    fun keyCurrentIndicator(startTime: Long): String = "current_indicator_${startTime}"

    fun calculateLevelDays(elapsedTimeMillis: Long): Int = (elapsedTimeMillis / DAY_IN_MILLIS).toInt()
    fun calculateLevelDaysFloat(elapsedTimeMillis: Long): Float = (elapsedTimeMillis / DAY_IN_MILLIS.toFloat())

    fun init(context: Context) { currentTestMode = TEST_MODE_REAL }

    fun updateTestMode(mode: Int) { currentTestMode = TEST_MODE_REAL }

    fun initializeUserSettings(context: Context) {
        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val isInitialized = sharedPref.getBoolean(PREF_SETTINGS_INITIALIZED, false)

        // 기존 한국어 설정값을 새로운 키로 마이그레이션
        val currentCost = sharedPref.getString(PREF_SELECTED_COST, null)
        val currentFrequency = sharedPref.getString(PREF_SELECTED_FREQUENCY, null)
        val currentDuration = sharedPref.getString(PREF_SELECTED_DURATION, null)

        // 마이그레이션: 한국어 -> 키
        val migratedCost = when (currentCost) {
            "저" -> KEY_COST_LOW
            "중" -> KEY_COST_MEDIUM
            "고" -> KEY_COST_HIGH
            else -> currentCost
        }
        val migratedFrequency = when (currentFrequency) {
            "주 1회 이하" -> KEY_FREQUENCY_LOW
            "주 2~3회" -> KEY_FREQUENCY_MEDIUM
            "주 4회 이상" -> KEY_FREQUENCY_HIGH
            else -> currentFrequency
        }
        val migratedDuration = when (currentDuration) {
            "짧음" -> KEY_DURATION_SHORT
            "보통" -> KEY_DURATION_MEDIUM
            "길게" -> KEY_DURATION_LONG
            else -> currentDuration
        }

        if (!isInitialized) {
            sharedPref.edit {
                putString(PREF_SELECTED_COST, migratedCost ?: DEFAULT_COST)
                putString(PREF_SELECTED_FREQUENCY, migratedFrequency ?: DEFAULT_FREQUENCY)
                putString(PREF_SELECTED_DURATION, migratedDuration ?: DEFAULT_DURATION)
                // 첫 실행: 혹시 백업/잔존 데이터로 start_time, target_days 등이 남아있어도 초기화
                remove(PREF_START_TIME)
                remove(PREF_TARGET_DAYS)
                putBoolean(PREF_TIMER_COMPLETED, false)
                putBoolean(PREF_SETTINGS_INITIALIZED, true)
            }
        } else {
            // 이미 초기화되었지만 한국어 값이 남아있다면 마이그레이션
            val needsMigration = (currentCost in listOf("저", "중", "고")) ||
                                (currentFrequency in listOf("주 1회 이하", "주 2~3회", "주 4회 이상")) ||
                                (currentDuration in listOf("짧음", "보통", "길게"))

            if (needsMigration) {
                sharedPref.edit {
                    putString(PREF_SELECTED_COST, migratedCost ?: DEFAULT_COST)
                    putString(PREF_SELECTED_FREQUENCY, migratedFrequency ?: DEFAULT_FREQUENCY)
                    putString(PREF_SELECTED_DURATION, migratedDuration ?: DEFAULT_DURATION)
                }
            }

            // 보정 로직: 비정상 상태(예: start_time 존재하지만 target_days 미설정, 혹은 future timestamp) 정리
            val startTime = sharedPref.getLong(PREF_START_TIME, 0L)
            val targetDays = sharedPref.getFloat(PREF_TARGET_DAYS, -1f)
            val now = System.currentTimeMillis()
            if (startTime > 0 && (targetDays <= 0f || startTime > now)) {
                sharedPref.edit {
                    remove(PREF_START_TIME)
                    remove(PREF_TARGET_DAYS)
                    putBoolean(PREF_TIMER_COMPLETED, false)
                }
            }
        }
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
        if (markerFile.exists()) return // 이미 한번 처리 끝

        val pm = context.packageManager
        val firstInstallTime = try {
            pm.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        val isRecentInstall = (System.currentTimeMillis() - firstInstallTime) < FRESH_INSTALL_WINDOW_MILLIS

        val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        if (isRecentInstall) {
            // 재설치 직후 복원된 진행 상태라면 모두 초기화
            sharedPref.edit {
                remove(PREF_START_TIME)
                remove(PREF_TARGET_DAYS)
                putBoolean(PREF_TIMER_COMPLETED, false)
            }
        }
        // marker 생성 (업데이트 첫 실행 시에도 생성되지만 wipe는 하지 않음)
        try { markerFile.writeText("1") } catch (_: Exception) { /* ignore */ }
    }
}
