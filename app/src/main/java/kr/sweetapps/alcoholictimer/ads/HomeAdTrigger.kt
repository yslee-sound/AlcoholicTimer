package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * 홈(1번: 시작/진행/종료) 그룹 진입 횟수를 기반으로 전면 광고 트리거.
 * - NavGraph에서 비홈→홈 진입 이벤트 시에만 호출됨
 */
object HomeAdTrigger {
    private const val PREFS_NAME = "home_ad_trigger_prefs"
    private const val KEY_HOME_VISITS = "home_visits_count"
    private const val KEY_LAST_RESET_DAY = "home_visits_day"

    private const val VISIT_THRESHOLD = 3

    private fun dayKey(): String = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 홈 그룹으로 실제 진입했을 때 호출. source는 라우트명("start"|"run"|"quit") 등 식별자 */
    fun registerHomeVisit(activity: Activity, source: String) {
        Log.d("HomeAdTrigger", "registerHomeVisit called with source=$source")
        if (!AdController.isInterstitialEnabled()) {
            Log.d("HomeAdTrigger", "Interstitial ads are disabled by policy")
            resetIfDayChanged(activity)
            return
        }
        resetIfDayChanged(activity)
        val sp = prefs(activity)

        val current = sp.getInt(KEY_HOME_VISITS, 0) + 1
        sp.edit { putInt(KEY_HOME_VISITS, current) }
        Log.d("HomeAdTrigger", "Home visit recorded: $current/$VISIT_THRESHOLD (source=$source)")

        if (current >= VISIT_THRESHOLD) {
            Log.d("HomeAdTrigger", "VISIT_THRESHOLD reached. Attempting to show interstitial ad.")
            val canShow = AdController.canShowInterstitial(activity)
            if (canShow && InterstitialAdManager.isLoaded()) {
                val showed = InterstitialAdManager.maybeShowIfEligible(activity) {
                    Log.d("HomeAdTrigger", "Interstitial ad shown successfully. Resetting visit count.")
                    AdController.recordInterstitialShown(activity)
                }
                if (showed) {
                    sp.edit { putInt(KEY_HOME_VISITS, 0) }
                } else {
                    Log.d("HomeAdTrigger", "Interstitial ad failed to show. Keeping visit count.")
                    InterstitialAdManager.preload(activity.applicationContext)
                }
            } else {
                Log.d("HomeAdTrigger", "Interstitial ad not ready or policy restricted. Keeping visit count.")
                InterstitialAdManager.preload(activity.applicationContext)
            }
        } else if (!InterstitialAdManager.isLoaded()) {
            Log.d("HomeAdTrigger", "Below VISIT_THRESHOLD. Preloading interstitial ad.")
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
            }
        }
    }
}
