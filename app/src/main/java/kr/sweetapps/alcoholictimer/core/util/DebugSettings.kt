package kr.sweetapps.alcoholictimer.core.util

import android.content.Context
import android.content.SharedPreferences

object DebugSettings {
    private const val PREFS_NAME = "debug_settings"
    private const val KEY_DEMO_MODE = "debug_demo_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDemoModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEMO_MODE, false)
    }

    fun setDemoModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
    }
}
