package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.CopyOnWriteArraySet
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager

object AppOpenAdManager {
    private const val TAG = "AppOpenAdManager"

    private var applicationRef: Application? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var isShowing: Boolean = false
    @Volatile private var loaded: Boolean = false
    @Volatile private var isLoading: Boolean = false
    @Volatile private var autoShowEnabled: Boolean = true

    fun setAutoShowEnabled(enabled: Boolean) { autoShowEnabled = enabled }

    fun isAutoShowEnabled(): Boolean = autoShowEnabled

    private var appOpenAd: AppOpenAd? = null

    private val loadedListeners = CopyOnWriteArraySet<() -> Any?>()
    private val shownListeners = CopyOnWriteArraySet<() -> Any?>()
    private val finishedListeners = CopyOnWriteArraySet<() -> Any?>()
    private val loadFailedListeners = CopyOnWriteArraySet<() -> Any?>()

    @Volatile private var onLoadedListener: (() -> Unit)? = null
    @Volatile private var onShownListener: (() -> Unit)? = null
    @Volatile private var onFinishedListener: (() -> Unit)? = null
    @Volatile private var onLoadFailedListener: (() -> Unit)? = null

    // Timestamps for suppression logic
    @Volatile private var lastShownAt: Long = 0L
    @Volatile private var lastDismissedAt: Long = 0L

    fun initialize(application: Application, registerLifecycle: Boolean = true) {
        applicationRef = application
        try {
            MobileAds.initialize(application.applicationContext) { initializationStatus ->
                Log.d(TAG, "MobileAds initialized: $initializationStatus")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "MobileAds.initialize failed: ${t.message}")
        }
        Log.d(TAG, "initialize: application set. registerLifecycle=$registerLifecycle")
    }

    fun noteAppStart() {
        Log.d(TAG, "noteAppStart called")
    }

    fun preload(context: Context) {
        // don't start loading if already loading or loaded
        if (loaded || isLoading) {
            Log.d(TAG, "preload: already loaded or loading")
            return
        }
        isLoading = true
        // BuildConfig is in the application package; reference fully-qualified to avoid unresolved reference
        val adUnitId = try { kr.sweetapps.alcoholictimer.BuildConfig.ADMOB_APP_OPEN_UNIT_ID } catch (_: Throwable) { "" }
        if (adUnitId.isBlank()) {
            Log.w(TAG, "preload: missing App Open ad unit id")
            isLoading = false
            return
        }
        val request = AdRequest.Builder().build()
        Log.d(TAG, "preload: loading unit=$adUnitId")
        try {
            AppOpenAd.load(context, adUnitId, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "onAdLoaded app-open")
                    appOpenAd = ad
                    loaded = true
                    isLoading = false
                    try { onLoadedListener?.invoke() } catch (_: Throwable) {}
                    for (l in loadedListeners) runCatching { l.invoke() }

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "AppOpen onAdShowedFullScreenContent")
                            isShowing = true
                            lastShownAt = System.currentTimeMillis()
                            try { onShownListener?.invoke() } catch (_: Throwable) {}
                            for (l in shownListeners) runCatching { l.invoke() }
                        }

                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            Log.w(TAG, "AppOpen onAdFailedToShowFullScreenContent: ${error.message}")
                            // treat as finished to continue app flow
                            isShowing = false
                            appOpenAd = null
                            loaded = false
                            performFinishFlow()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "AppOpen onAdDismissedFullScreenContent")
                            isShowing = false
                            appOpenAd = null
                            loaded = false
                            lastDismissedAt = System.currentTimeMillis()
                            performFinishFlow()
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "onAdFailedToLoad app-open: ${loadAdError.message}")
                    isLoading = false
                    loaded = false
                    appOpenAd = null
                    try { onLoadFailedListener?.invoke() } catch (_: Throwable) {}
                    for (l in loadFailedListeners) runCatching { l.invoke() }
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "preload exception: ${t.message}")
            isLoading = false
        }
    }

    fun isLoaded(): Boolean = loaded
    fun isShowingAd(): Boolean = isShowing

    fun clearLoadedAd() {
        appOpenAd = null
        loaded = false
    }

    fun setOnAdFinishedListener(listener: (() -> Unit)?) { onFinishedListener = listener }
    fun setOnAdLoadedListener(listener: (() -> Unit)?) {
        onLoadedListener = listener
        if (listener != null && loaded) {
            try { listener.invoke() } catch (_: Throwable) {}
        }
    }
    fun setOnAdShownListener(listener: (() -> Unit)?) { onShownListener = listener }
    fun setOnAdLoadFailedListener(listener: (() -> Unit)?) { onLoadFailedListener = listener }

    fun addOnAdLoadedListener(listener: () -> Any?) { loadedListeners.add(listener); if (loaded) runCatching { listener.invoke() } }
    fun removeOnAdLoadedListener(listener: () -> Any?) { loadedListeners.remove(listener) }

    fun addOnAdShownListener(listener: () -> Any?) { shownListeners.add(listener) }
    fun removeOnAdShownListener(listener: () -> Any?) { shownListeners.remove(listener) }

    fun addOnAdFinishedListener(listener: () -> Any?) { finishedListeners.add(listener) }
    fun removeOnAdFinishedListener(listener: () -> Any?) { finishedListeners.remove(listener) }

    fun addOnAdLoadFailedListener(listener: () -> Any?) { loadFailedListeners.add(listener) }
    fun removeOnAdLoadFailedListener(listener: () -> Any?) { loadFailedListeners.remove(listener) }

    fun onConsentUpdated(canRequestAds: Boolean) {
        Log.d(TAG, "onConsentUpdated: canRequestAds=$canRequestAds")
        if (canRequestAds) applicationRef?.let { preload(it.applicationContext) }
    }

    fun showIfAvailable(activity: Activity, bypassRecentFullscreenSuppression: Boolean = false): Boolean {
        Log.d(TAG, "showIfAvailable called - loaded=$loaded isShowing=$isShowing activity=${activity.javaClass.simpleName}")
        if (!loaded || isShowing) return false

        // Basic suppression: if recently shown within 5s, treat as recent
        if (!bypassRecentFullscreenSuppression && wasRecentlyShown()) {
            Log.d(TAG, "showIfAvailable: suppressed due to recent show")
            return false
        }

        // Show the loaded AppOpenAd
        try {
            appOpenAd?.show(activity)
            Log.d(TAG, "showIfAvailable: appOpenAd.show() called")
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "showIfAvailable: failed to show app open ad: ${t.message}")
            return false
        }
    }

    private fun performFinishFlow() {
        try {
            Log.d(TAG, "performFinishFlow -> finishing ad flow")
            for (l in finishedListeners) runCatching { l.invoke() }
            try { onFinishedListener?.invoke() } catch (_: Throwable) {}
            try { AnalyticsManager.logAdImpression("app_open") } catch (_: Throwable) {}
            applicationRef?.applicationContext?.let { ctx ->
                mainHandler.postDelayed({ try { preload(ctx) } catch (_: Throwable) {} }, 30_000L)
            }
        } catch (_: Throwable) {}
    }

    /** Called by external overlay activity (if any) to notify ad finished. */
    fun notifyAdFinishedFromOverlay() {
        try {
            Log.d(TAG, "notifyAdFinishedFromOverlay called")
            performFinishFlow()
        } catch (_: Throwable) {}
    }

    fun wasRecentlyShown(): Boolean {
        val last = lastShownAt
        if (last <= 0L) return false
        val elapsed = System.currentTimeMillis() - last
        return elapsed < 5_000L
    }
}
