package kr.sweetapps.alcoholictimer.util.debug

import android.content.Context
import android.content.SharedPreferences

object DebugSettings {
    private const val PREFS_NAME = "debug_settings"
    private const val KEY_DEMO_MODE = "debug_demo_mode"
    private const val KEY_UMP_FORCE_EEA = "debug_ump_force_eea"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDemoModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEMO_MODE, false)
    }

    fun setDemoModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
    }

    fun isUmpForceEeaEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_UMP_FORCE_EEA, false)
    }

    fun setUmpForceEeaEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UMP_FORCE_EEA, enabled).apply()
    }
}