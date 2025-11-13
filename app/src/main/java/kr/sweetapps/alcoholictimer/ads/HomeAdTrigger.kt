package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * 홈(1번: 시작/진행) 화면 재진입 횟수를 기반으로 전면 광고 트리거.
 * - 초기 시간 쿨다운 제거
 * - 1.5초 디바운스로 Start→Run 연속 진입 시 중복 카운트 방지
 */
object HomeAdTrigger {
    private const val PREFS_NAME = "home_ad_trigger_prefs"
    private const val KEY_HOME_VISITS = "home_visits_count"
    private const val KEY_LAST_RESET_DAY = "home_visits_day"

    // 중복 방지용 메타
    private const val KEY_LAST_SOURCE = "last_home_source"
    private const val KEY_LAST_TS = "last_home_ts"
    private const val MIN_INTERVAL_MS = 1500L

    private const val VISIT_THRESHOLD = 3

    private fun dayKey(): String = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 홈 화면이 실제로 표시되었을 때 호출. source는 "start" | "run" | 기타 식별자 */
    fun registerHomeVisit(activity: Activity, source: String) {
        if (!AdController.isInterstitialEnabled()) {
            resetIfDayChanged(activity)
            return
        }
        resetIfDayChanged(activity)
        val sp = prefs(activity)

        // 디바운스: 직전 홈 노출이 동일/상이든 관계없이 너무 근접하면 스킵
        val now = System.currentTimeMillis()
        val lastTs = sp.getLong(KEY_LAST_TS, 0L)
        val lastSource = sp.getString(KEY_LAST_SOURCE, null)
        if (lastTs > 0L && now - lastTs < MIN_INTERVAL_MS) {
            Log.d("HomeAdTrigger", "Debounced home visit from '$source' (last=$lastSource, dt=${now - lastTs}ms)")
            sp.edit { putString(KEY_LAST_SOURCE, source); putLong(KEY_LAST_TS, now) }
            return
        }

        // 상태 갱신 (마지막 홈 노출 정보 기록)
        sp.edit { putString(KEY_LAST_SOURCE, source); putLong(KEY_LAST_TS, now) }

        val current = sp.getInt(KEY_HOME_VISITS, 0) + 1
        sp.edit { putInt(KEY_HOME_VISITS, current) }
        Log.d("HomeAdTrigger", "Home visit recorded: $current/$VISIT_THRESHOLD (source=$source)")

        if (current >= VISIT_THRESHOLD) {
            sp.edit { putInt(KEY_HOME_VISITS, 0) }
            AdHelpers.preloadThenShowOr(activity, timeoutMs = 1200) {
                InterstitialAdManager.preload(activity.applicationContext)
            }
        } else if (!InterstitialAdManager.isLoaded()) {
            InterstitialAdManager.preload(activity.applicationContext)
        }
    }

    private fun resetIfDayChanged(context: Context) {
        val sp = prefs(context)
        val lastDay = sp.getString(KEY_LAST_RESET_DAY, null)
        val today = dayKey()
        if (lastDay != today) {
            sp.edit {
                putString(KEY_LAST_RESET_DAY, today)
                putInt(KEY_HOME_VISITS, 0)
                putString(KEY_LAST_SOURCE, null)
                putLong(KEY_LAST_TS, 0L)
            }
        }
    }
}
