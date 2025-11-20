package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Stub HomeAdTrigger: records visits but does not attempt to show ads.
 */
object HomeAdTrigger {
    private const val PREFS_NAME = "home_ad_trigger_prefs"
    private const val KEY_HOME_VISITS = "home_visits_count"
    private const val KEY_LAST_RESET_DAY = "home_visits_day"

    private fun dayKey(): String = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 홈 그룹으로 실제 진입했을 때 호출. source는 라우트명("start"|"run"|"quit") 등 식별자 */
    fun registerHomeVisit(activity: Activity, source: String) {
        Log.d("HomeAdTrigger", "registerHomeVisit stub: source=$source")

        // Reset daily counter if day changed first
        resetIfDayChanged(activity)

        val sp = prefs(activity)

        val current = sp.getInt(KEY_HOME_VISITS, 0) + 1
        // avoid using androidx.core.content.edit extension to keep compatibility
        sp.edit().putInt(KEY_HOME_VISITS, current).apply()
        Log.d("HomeAdTrigger", "Home visit recorded: $current (source=$source)")
    }

    private fun resetIfDayChanged(context: Context) {
        val sp = prefs(context)
        val lastDay = sp.getString(KEY_LAST_RESET_DAY, null)
        val today = dayKey()
        if (lastDay != today) {
            // avoid using kotlin extension lambda to prevent overload resolution issues
            sp.edit().putString(KEY_LAST_RESET_DAY, today).putInt(KEY_HOME_VISITS, 0).apply()
        }
    }
}

