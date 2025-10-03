@file:Suppress("unused")

package com.example.alcoholictimer.core.util

import android.content.Context
import androidx.core.content.edit

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

    const val DEFAULT_COST = "저"
    const val DEFAULT_FREQUENCY = "주 1회 이하"
    const val DEFAULT_DURATION = "짧음"

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
        if (!isInitialized) {
            sharedPref.edit {
                putString(PREF_SELECTED_COST, DEFAULT_COST)
                putString(PREF_SELECTED_FREQUENCY, DEFAULT_FREQUENCY)
                putString(PREF_SELECTED_DURATION, DEFAULT_DURATION)
                putBoolean(PREF_SETTINGS_INITIALIZED, true)
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
}

