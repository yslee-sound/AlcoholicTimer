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

        // Reset daily counter if day changed first
        resetIfDayChanged(activity)

        // If policy has been fetched and interstitials are disabled, skip showing but still keep daily counters updated.
        if (AdController.isPolicyFetchCompleted() && !AdController.isInterstitialEnabled()) {
            Log.d("HomeAdTrigger", "Interstitial ads are disabled by policy (policy fetched) - counting suppressed for display")
            return
        }

        val sp = prefs(activity)

        val current = sp.getInt(KEY_HOME_VISITS, 0) + 1
        sp.edit { putInt(KEY_HOME_VISITS, current) }
        Log.d("HomeAdTrigger", "Home visit recorded: $current/$VISIT_THRESHOLD (source=$source)")

        if (current >= VISIT_THRESHOLD) {
            Log.d("HomeAdTrigger", "VISIT_THRESHOLD reached. Attempting to show interstitial ad.")
            val canShow = AdController.canShowInterstitial(activity)
            if (canShow) {
                if (InterstitialAdManager.isLoaded()) {
                    val showed = InterstitialAdManager.maybeShowIfEligible(activity) {
                        Log.d("HomeAdTrigger", "Interstitial ad shown successfully. Resetting visit count.")
                        AdController.recordInterstitialShown(activity)
                    }
                    if (showed) {
                        sp.edit { putInt(KEY_HOME_VISITS, 0) }
                    } else {
                        Log.d("HomeAdTrigger", "Interstitial ad failed to show. Scheduling retry and keeping visit count.")
                        // Try to preload (ensure next attempt has a loaded ad)
                        InterstitialAdManager.preload(activity.applicationContext)
                        // Schedule a safety retry after a short delay (covers initial protection window)
                        try {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    Log.d("HomeAdTrigger", "Retrying interstitial show after delay (safety retry)")
                                    val retryShow = InterstitialAdManager.maybeShowIfEligible(activity) {
                                        Log.d("HomeAdTrigger", "Interstitial ad shown on retry. Resetting visit count.")
                                        AdController.recordInterstitialShown(activity)
                                        sp.edit { putInt(KEY_HOME_VISITS, 0) }
                                    }
                                    if (!retryShow) {
                                        Log.d("HomeAdTrigger", "Retry show still failed; keeping visit count.")
                                    }
                                } catch (t: Throwable) {
                                    Log.w("HomeAdTrigger", "Retry show failed: $t")
                                }
                            }, 65000L)
                        } catch (_: Throwable) {}
                    }
                } else {
                    Log.d("HomeAdTrigger", "Interstitial not loaded yet - registering load listener and preloading.")
                    // Register a one-shot load listener to attempt show when load completes
                    InterstitialAdManager.addLoadListener { success ->
                        try {
                            if (success) {
                                Log.d("HomeAdTrigger", "Load listener: interstitial loaded, attempting to show now.")
                                val showed = InterstitialAdManager.maybeShowIfEligible(activity) {
                                    Log.d("HomeAdTrigger", "Interstitial ad shown after load listener. Resetting visit count.")
                                    AdController.recordInterstitialShown(activity)
                                }
                                if (showed) {
                                    prefs(activity).edit { putInt(KEY_HOME_VISITS, 0) }
                                } else {
                                    Log.d("HomeAdTrigger", "Load listener: show attempt failed after load.")
                                }
                            } else {
                                Log.d("HomeAdTrigger", "Load listener: load failed.")
                            }
                        } catch (t: Throwable) {
                            Log.w("HomeAdTrigger", "Load listener handler threw: $t")
                        }
                    }
                    InterstitialAdManager.preload(activity.applicationContext)
                    // Also schedule a fallback safety retry in case of initial protection blocking or other transient failures
                    try {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                Log.d("HomeAdTrigger", "Fallback retry after preload delay: attempting to show interstitial.")
                                val retryShow = InterstitialAdManager.maybeShowIfEligible(activity) {
                                    Log.d("HomeAdTrigger", "Interstitial ad shown on fallback retry. Resetting visit count.")
                                    AdController.recordInterstitialShown(activity)
                                    prefs(activity).edit { putInt(KEY_HOME_VISITS, 0) }
                                }
                                if (!retryShow) Log.d("HomeAdTrigger", "Fallback retry show failed; keeping visit count.")
                            } catch (t: Throwable) {
                                Log.w("HomeAdTrigger", "Fallback retry failed: $t")
                            }
                        }, 65000L)
                    } catch (_: Throwable) {}
                }
            } else {
                Log.d("HomeAdTrigger", "AdController.canShowInterstitial returned false - policy or limit issue. Keeping visit count.")
                // Still attempt preload to have an ad ready when policy allows
                InterstitialAdManager.preload(activity.applicationContext)
            }
        } else if (!InterstitialAdManager.isLoaded()) {
            Log.d("HomeAdTrigger", "Below VISIT_THRESHOLD and interstitial not loaded -> preloading.")
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
