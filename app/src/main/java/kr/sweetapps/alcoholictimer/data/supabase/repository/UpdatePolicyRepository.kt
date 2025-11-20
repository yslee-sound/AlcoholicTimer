package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdateStats

/** Lightweight stub of UpdatePolicyRepository to remove Supabase dependency but keep local prefs behavior. */
class UpdatePolicyRepository(
    private val context: Context
) {
    companion object {
        private const val PREF_NAME = "update_policy_prefs"
        private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
        private const val KEY_LATER_COUNT_PREFIX = "update_later_count_"
        private const val KEY_LAST_DISMISSED_PREFIX = "update_last_dismissed_"
    }

    /**
     * 활성화된 업데이트 정책을 가져옵니다.
     *
     * @return 표시할 업데이트 정책 또는 null
     */
    suspend fun getActivePolicy(): UpdatePolicy? = null

    /**
     * 사용자가 "나중에" 버튼을 누른 버전을 저장합니다.
     *
     * @param version 무시할 버전
     */
    fun dismissVersion(version: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION_CODE, version).apply()
    }

    /**
     * 무시된 버전을 가져옵니다.
     */
    private fun getDismissedVersion(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISMISSED_VERSION_CODE, null)
    }

    /**
     * 무시 기록을 초기화합니다. (디버그용)
     */
    fun clearDismissedVersion() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISMISSED_VERSION_CODE).apply()
    }

    // UpdateStats helpers - stored per targetVersionCode
    fun getUpdateStats(versionCode: Long): UpdateStats {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = versionCode.toString()
        val later = prefs.getInt(KEY_LATER_COUNT_PREFIX + key, 0)
        val lastMs = prefs.getLong(KEY_LAST_DISMISSED_PREFIX + key, 0L)
        return UpdateStats(later, lastMs)
    }

    fun recordLaterClicked(versionCode: Long, nowMs: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = versionCode.toString()
        val keyCount = KEY_LATER_COUNT_PREFIX + key
        val prev = prefs.getInt(keyCount, 0)
        prefs.edit().putInt(keyCount, prev + 1).putLong(KEY_LAST_DISMISSED_PREFIX + key, nowMs).apply()
    }

    fun clearStats(versionCode: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = versionCode.toString()
        prefs.edit().remove(KEY_LATER_COUNT_PREFIX + key).remove(KEY_LAST_DISMISSED_PREFIX + key).apply()
    }
}
