package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.ads.AdController

object InterstitialAdManager {
    private const val TAG = "InterstitialAdManager"

    // Test and production unit IDs (user provided)
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-8420908105703273/2270912481"

    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var isLoading = false
    @Volatile private var isShowing = false

    private fun adUnitId(): String = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    /** Initialize MobileAds (idempotent) and start loading */
    fun preload(context: Context) {
        try {
            MobileAds.initialize(context) { Log.d(TAG, "MobileAds initialized: $it") }
            loadInterstitial(context)
        } catch (t: Throwable) {
            Log.e(TAG, "preload failed", t)
        }
    }

    private fun loadInterstitial(context: Context) {
        if (interstitial != null || isLoading) return
        try {
            isLoading = true
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(context, adUnitId(), adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "onAdLoaded")
                    interstitial = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "onAdFailedToLoad: ${loadAdError.message}")
                    interstitial = null
                    isLoading = false
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "loadInterstitial failed", t)
            isLoading = false
        }
    }

    fun isLoaded(): Boolean = interstitial != null

    fun clearLoadedAd() { interstitial = null }

    fun isShowingAd(): Boolean = isShowing

    fun addLoadListener(listener: (Boolean) -> Unit) { /* optional: could be implemented */ }

    fun resetColdStartGate() { /* no-op for now */ }

    fun noteAppStart() { Log.d(TAG, "noteAppStart called") }

    /** Attempts to show interstitial if eligible. Returns true if an ad will be shown (now or soon). */
    fun maybeShowIfEligible(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean {
        try {
            Log.d(TAG, "maybeShowIfEligible called")
            // Avoid collision: do not show if a higher-priority full-screen ad is active
            try {
                if (AdController.isFullScreenAdShowing() || AdController.isInterstitialShowingNow()) {
                    Log.d(TAG, "maybeShowIfEligible: another full-screen/interstitial active -> skip show")
                    return false
                }
            } catch (_: Throwable) {}

            // If an ad is already loaded, show it immediately
            val ad = interstitial
            if (ad != null) {
                tryShowAd(activity, ad, onDismiss)
                return true
            }

            // Not loaded: start loading and show when ready. Return true to indicate we will attempt to show.
            // If loading fails, we'll call onDismiss automatically.
            preload(activity.applicationContext)
            // Attach a temporary load callback by reloading with a custom callback to show when loaded.
            if (!isLoading) loadInterstitial(activity.applicationContext)
            // We will show when loaded via the global interstitial reference if possible. To ensure
            // the caller does not navigate away, return true indicating an ad attempt.
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "maybeShowIfEligible failed", t)
            // Fallback to debug dialog
            return forceShowDebug(activity, onDismiss)
        }
    }

    private fun tryShowAd(activity: Activity, ad: InterstitialAd, onDismiss: (() -> Unit)?) {
        try {
            isShowing = true
            // mark interstitial and full-screen state so banner hides and other flows respect priority
            try { AdController.setInterstitialShowing(true); AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "onAdDismissedFullScreenContent")
                    isShowing = false
                    interstitial = null
                    // clear interstitial/full-screen flags
                    try { AdController.setInterstitialShowing(false); AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: ${adError.message}")
                    isShowing = false
                    interstitial = null
                    // clear flags on failure
                    try { AdController.setInterstitialShowing(false); AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "onAdShowedFullScreenContent")
                }
            }
            ad.show(activity)
        } catch (t: Throwable) {
            Log.e(TAG, "show failed", t)
            isShowing = false
            interstitial = null
            // fallback dialog
            forceShowDebug(activity, onDismiss)
        }
    }

    // DEBUG: fallback debug dialog used in case ad framework isn't available
    fun forceShowDebug(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean {
        return try {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Debug Interstitial")
                .setMessage("This simulates an interstitial ad for testing. Press Close to continue.")
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ ->
                    // clear flags for debug dialog flow as well
                    try { AdController.setInterstitialShowing(false); AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }
                .show()
            Log.d(TAG, "forceShowDebug: shown")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "forceShowDebug failed", t)
            false
        }
    }
}
