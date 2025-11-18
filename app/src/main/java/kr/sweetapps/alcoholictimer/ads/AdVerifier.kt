package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * Debug-only verifier for ad configuration and runtime behavior.
 * - Meant to be called from a debug screen/activity to produce a reproducible verification log.
 * - Should not be enabled in production builds.
 */
object AdVerifier {
    private const val TAG = "AdVerifier"

    data class Result(val name: String, val ok: Boolean, val message: String)

    /**
     * Run a verification pass. Runs lightweight checks and a couple of safe simulations.
     * This will not attempt to show real ads. It may toggle internal state flags for testing.
     */
    fun runChecks(activity: Activity, callback: (List<Result>) -> Unit) {
        // Run in coroutine to avoid blocking caller
        CoroutineScope(Dispatchers.Main).launch {
            val results = mutableListOf<Result>()

            // 1) Policy fetch status
            val policyFetched = AdController.isPolicyFetchCompleted()
            results.add(Result("PolicyFetchCompleted", policyFetched, if (policyFetched) "Policy has been fetched" else "Policy fetch not yet completed"))

            // 2) Supabase policy flags via public helpers
            val interstitialEnabled = AdController.isInterstitialEnabled()
            val appOpenEnabled = AdController.isAppOpenEnabled()
            results.add(Result("InterstitialEnabled", interstitialEnabled, "AdController.isInterstitialEnabled() -> $interstitialEnabled"))
            results.add(Result("AppOpenEnabled", appOpenEnabled, "AdController.isAppOpenEnabled() -> $appOpenEnabled"))

            // 3) Interstitial load state
            try {
                val interstitialLoaded = InterstitialAdManager.isLoaded()
                results.add(Result("InterstitialLoaded", interstitialLoaded, "InterstitialAdManager.isLoaded() -> $interstitialLoaded"))
            } catch (t: Throwable) {
                results.add(Result("InterstitialLoaded", false, "Exception: ${t.message}"))
            }

            // 4) AppOpen load state (best effort, manager may expose isLoaded)
            try {
                val appOpenLoaded = try { AppOpenAdManager.isLoaded() } catch (_: Throwable) { false }
                results.add(Result("AppOpenLoaded", appOpenLoaded, "AppOpenAdManager.isLoaded() -> $appOpenLoaded"))
            } catch (t: Throwable) {
                results.add(Result("AppOpenLoaded", false, "Exception: ${t.message}"))
            }

            // 5) Frequency limits (canShowInterstitial) - this checks policy + runtime caps
            try {
                val canShow = AdController.canShowInterstitial(activity)
                results.add(Result("CanShowInterstitial", canShow, "AdController.canShowInterstitial() -> $canShow"))
            } catch (t: Throwable) {
                results.add(Result("CanShowInterstitial", false, "Exception: ${t.message}"))
            }

            // 6) Mutual exclusion test (simulate full-screen ad showing flag)
            try {
                val beforeFull = AdController.isFullScreenAdShowing()
                // simulate another full-screen ad
                AdController.setFullScreenAdShowing(true)
                val bannerDisabled = !AdController.isBannerEnabled()
                AdController.setFullScreenAdShowing(false)
                results.add(Result("MutualExclusionBannerHide", bannerDisabled, "Banner disabled while full-screen ad flagged: $bannerDisabled (was $beforeFull)"))
            } catch (t: Throwable) {
                results.add(Result("MutualExclusionBannerHide", false, "Exception: ${t.message}"))
            }

            // 7) Preload/Retry wiring check - register a one-shot load listener and call preload
            val preloadCheck = runPreloadWiring(activity)
            results.add(preloadCheck)

            // 8) Home trigger integration sanity (read stored counter)
            try {
                val sp = activity.getSharedPreferences("home_ad_trigger_prefs", Context.MODE_PRIVATE)
                val count = sp.getInt("home_visits_count", -1)
                val day = sp.getString("home_visits_day", null)
                results.add(Result("HomeVisitsPrefs", count >= 0, "home_visits_count=$count, home_visits_day=$day"))
            } catch (t: Throwable) {
                results.add(Result("HomeVisitsPrefs", false, "Exception: ${t.message}"))
            }

            // Log and callback
            for (r in results) {
                Log.d(TAG, "[${r.name}] ok=${r.ok} - ${r.message}")
            }
            callback(results)
        }
    }

    private suspend fun delayed(timeoutMs: Long) {
        delay(timeoutMs)
    }

    private fun runPreloadWiring(activity: Activity): Result {
        return try {
            var called = false
            val listener: (Boolean) -> Unit = { success ->
                called = true
                Log.d(TAG, "Preload listener invoked: success=$success")
            }
            // register listener and call preload. This is a non-blocking check; listener may fire asynchronously.
            InterstitialAdManager.addLoadListener(listener)
            InterstitialAdManager.preload(activity.applicationContext)
            // we can't synchronously wait here; just report that listener was registered and preload invoked.
            Result("PreloadWiring", true, "registered load listener and invoked preload (async). Check logs for 'Preload listener invoked'")
        } catch (t: Throwable) {
            Result("PreloadWiring", false, "Exception during preload wiring: ${t.message}")
        }
    }
}

