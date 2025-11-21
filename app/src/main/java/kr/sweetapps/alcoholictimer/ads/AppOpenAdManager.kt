package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.concurrent.CopyOnWriteArraySet
import android.os.Handler
import android.os.Looper

/**
 * Real App Open Ad manager using Google Mobile Ads SDK.
 * - preload(context): loads an AppOpenAd
 * - showIfAvailable(activity): shows loaded ad and returns true when shown
 * - listeners: onAdLoaded, onAdShown, onAdFinished, onAdLoadFailed
 *
 * This keeps the same lightweight API used by `SplashScreen` so integration is minimal.
 */
object AppOpenAdManager {
    private const val TAG = "AppOpenAdManager"

    private var applicationRef: Application? = null
    @Volatile private var autoShowEnabled: Boolean = true

    @Volatile private var appOpenAd: AppOpenAd? = null
    @Volatile private var isLoading: Boolean = false
    @Volatile private var isShowing: Boolean = false
    @Volatile private var isShowScheduled: Boolean = false
    @Volatile private var lastDismissedAt: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val onLoadedListeners = CopyOnWriteArraySet<() -> Unit>()
    private val onShownListeners = CopyOnWriteArraySet<() -> Unit>()
    private val onFinishedListeners = CopyOnWriteArraySet<() -> Unit>()
    private val onLoadFailedListeners = CopyOnWriteArraySet<() -> Unit>()

    fun initialize(application: Application, registerLifecycle: Boolean = true) {
        applicationRef = application
        Log.d(TAG, "initialize: application set. registerLifecycle=$registerLifecycle")
    }

    fun noteAppStart() { /* optional hook for lifecycle-based flows */ }

    fun setAutoShowEnabled(enabled: Boolean) {
        autoShowEnabled = enabled
        Log.d(TAG, "setAutoShowEnabled=$enabled")
    }

    private fun validateOrFallbackUnit(candidate: String?): String {
        // Very simple validation: must look like ca-app-pub-.../... (contains '/')
        if (candidate.isNullOrBlank()) return "ca-app-pub-3940256099942544/9257395921" // use user's test app-open id by default
        val trimmed = candidate.trim()
        return if (trimmed.contains("/") && trimmed.contains("ca-app-pub-")) trimmed
        else {
            Log.w(TAG, "Invalid ad unit id provided: '$candidate' -> falling back to test unit")
            "ca-app-pub-3940256099942544/9257395921"
        }
    }

    fun preload(context: Context) {
        // If we don't already have an applicationRef, try to set it from the provided context
        try {
            if (applicationRef == null && context.applicationContext is Application) {
                applicationRef = context.applicationContext as Application
                Log.d(TAG, "preload: applicationRef was null - set from context.applicationContext")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "preload: couldn't set applicationRef from context: $t")
        }

        if (appOpenAd != null || isLoading) {
            Log.d(TAG, "preload skipped: already loading/have ad")
            return
        }
        val unitIdCandidate = try {
            // Try to resolve BuildConfig constant if present
            val cls = Class.forName(context.packageName + ".BuildConfig")
            val field = cls.getDeclaredField("ADMOB_APP_OPEN_UNIT_ID")
            field.get(null) as? String
        } catch (t: Throwable) {
            null
        }

        // If debug build, prefer explicit debug test unit id to ensure predictable behavior
        val debugOverride = try {
            val cls = Class.forName(context.packageName + ".BuildConfig")
            val dbgField = cls.getDeclaredField("DEBUG")
            (dbgField.get(null) as? Boolean) == true
        } catch (t: Throwable) {
            false
        }

        var unitId = validateOrFallbackUnit(unitIdCandidate)
        if (debugOverride) {
            // Use the explicit test app-open id (user-provided) for debug builds
            unitId = "ca-app-pub-3940256099942544/9257395921"
            Log.d(TAG, "preload: DEBUG build detected - overriding app open unit to test id: $unitId")
        }

        Log.d(TAG, "preload: loading unit=$unitId")
        isLoading = true
        val request = AdRequest.Builder().build()
        // Prefer application context when loading ads per guidance
        val loadContext: Context = applicationRef?.applicationContext ?: context.applicationContext
        AppOpenAd.load(
            loadContext,
            unitId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "onAdLoaded app-open")
                    appOpenAd = ad
                    isLoading = false
                    for (l in onLoadedListeners) runCatching { l.invoke() }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Include full details for easier debugging
                    Log.e(TAG, "App Open failed to load: message=${loadAdError.message} | code=${loadAdError.code} | domain=${loadAdError.domain} | responseInfo=${loadAdError.responseInfo} | full=${loadAdError}")
                    isLoading = false
                    for (l in onLoadFailedListeners) runCatching { l.invoke() }
                }
            }
        )
    }

    fun isLoaded(): Boolean = appOpenAd != null
    fun isShowingAd(): Boolean = isShowing

    fun clearLoadedAd() {
        appOpenAd = null
    }

    fun setOnAdFinishedListener(listener: (() -> Unit)?) {
        if (listener == null) onFinishedListeners.clear() else onFinishedListeners.add(listener)
    }
    fun setOnAdLoadedListener(listener: (() -> Unit)?) {
        if (listener == null) onLoadedListeners.clear() else {
            onLoadedListeners.add(listener)
            // 만약 리스너 등록 시점에 이미 광고가 로드되어 있다면 즉시 호출하여 늦게 등록된 리스너가 이벤트를 놓치지 않도록 함
            if (appOpenAd != null) {
                Log.d(TAG, "setOnAdLoadedListener: ad already loaded -> invoking listener immediately")
                runCatching { listener.invoke() }
            }
        }
    }
    fun setOnAdShownListener(listener: (() -> Unit)?) {
        if (listener == null) onShownListeners.clear() else onShownListeners.add(listener)
    }
    fun setOnAdLoadFailedListener(listener: (() -> Unit)?) {
        if (listener == null) onLoadFailedListeners.clear() else onLoadFailedListeners.add(listener)
    }

    fun onConsentUpdated(canRequestAds: Boolean) {
        Log.d(TAG, "onConsentUpdated: canRequestAds=$canRequestAds")
        if (canRequestAds) {
            applicationRef?.let { preload(it.applicationContext) }
        }
    }

    /**
     * Show the app open ad if available. Returns true if ad was shown.
     */
    fun showIfAvailable(activity: Activity): Boolean {
        Log.d(TAG, "showIfAvailable called - loaded=${appOpenAd != null} showing=$isShowing autoShow=$autoShowEnabled activity=${activity.javaClass.simpleName} finishing=${activity.isFinishing} destroyed=${try{ if (android.os.Build.VERSION.SDK_INT>=17) activity.isDestroyed else false } catch (_:Throwable){"unknown"}}")
        if (appOpenAd == null || isShowing) return false
        try {
            // Check policy via AdController before attempting to show
            try {
                if (!AdController.canShowAppOpen(activity)) {
                    Log.d(TAG, "showIfAvailable: policy disallows app-open -> returning false")
                    return false
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Policy check failed, proceeding to attempt show: $t")
            }

            val ad = appOpenAd ?: return false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AppOpen onAdDismissedFullScreenContent")
                    isShowing = false
                    isShowScheduled = false
                    appOpenAd = null
                    try { lastDismissedAt = System.currentTimeMillis() } catch (_: Throwable) {}
                    // record that ad was shown and finished
                    try { AdController.recordAppOpenShown(activity) } catch (_: Throwable) {}
                    for (l in onFinishedListeners) runCatching { l.invoke() }
                    // After dismissal, schedule preload after server-controlled cooldown to avoid immediate reload->show
                    try {
                        val appCtx = applicationRef?.applicationContext
                        if (appCtx != null) {
                            val delayMs = try { (AdController.getAppOpenCooldownSeconds().coerceAtLeast(1) * 1000L) } catch (_: Throwable) { 30_000L }
                            mainHandler.postDelayed({ try { preload(appCtx) } catch (_: Throwable) {} }, delayMs)
                        }
                    } catch (_: Throwable) {}
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AppOpen failed to show: ${adError.message} | details=${adError}")
                    isShowing = false
                    isShowScheduled = false
                    appOpenAd = null
                    for (l in onLoadFailedListeners) runCatching { l.invoke() }
                    // On failure, schedule reload after server cooldown to avoid immediate retry->show
                    try {
                        val appCtx = applicationRef?.applicationContext
                        if (appCtx != null) {
                            val delayMs = try { (AdController.getAppOpenCooldownSeconds().coerceAtLeast(1) * 1000L) } catch (_: Throwable) { 30_000L }
                            mainHandler.postDelayed({ try { preload(appCtx) } catch (_: Throwable) {} }, delayMs)
                        }
                    } catch (_: Throwable) {}
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AppOpen onAdShowedFullScreenContent")
                    isShowing = true
                    isShowScheduled = false
                    for (l in onShownListeners) runCatching { l.invoke() }
                }
            }
            // Show (guard against concurrent scheduling)
            Log.d(TAG, "Attempting to show AppOpen ad now (safely on UI thread)")
            if (isShowScheduled) {
                Log.d(TAG, "showIfAvailable: already scheduled -> returning false")
                return false
            }
            isShowScheduled = true
            var shown = false
            // Prefer posting to the activity's decorView so the ad is shown when the window is ready
            try {
                val decorRunnable = Runnable {
                    try {
                        val finishing = runCatching { activity.isFinishing }.getOrDefault(false)
                        val destroyed = runCatching {
                            if (android.os.Build.VERSION.SDK_INT >= 17) activity.isDestroyed else false
                        }.getOrDefault(false)
                        Log.d(TAG, "showIfAvailable -> decor-post pre-show check finishing=$finishing destroyed=$destroyed isShowing=$isShowing isShowScheduled=$isShowScheduled")
                        if (!finishing && !destroyed && !isShowing) {
                            try {
                                ad.show(activity)
                                shown = true
                                Log.d(TAG, "ad.show() invoked via decorView.post")
                            } catch (t: Throwable) {
                                Log.w(TAG, "ad.show threw: $t")
                                isShowScheduled = false
                            }
                        } else {
                            Log.w(TAG, "ad.show skipped because activity not valid (finishing=$finishing destroyed=$destroyed isShowing=$isShowing)")
                            isShowScheduled = false
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "ad.show (decor) threw: $t")
                        isShowScheduled = false
                    }
                }
                val posted = try {
                    activity.window?.decorView?.post(decorRunnable) ?: false
                } catch (t: Throwable) {
                    Log.w(TAG, "decorView.post failed: $t")
                    false
                }
                if (!posted) {
                    // Fallback to runOnUiThread if decorView wasn't available
                    activity.runOnUiThread(decorRunnable)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "scheduling ad.show failed: $t")
                isShowScheduled = false
            }
            // Do NOT auto-launch overlay activity as a fallback; leave decision to caller.
            if (!shown) {
                Log.d(TAG, "showIfAvailable: ad not shown immediately and overlay fallback suppressed")
            }
            Log.d(TAG, "ad.show() returned (scheduled). shownFlag=$shown, awaiting callbacks")
            return shown
        } catch (t: Throwable) {
            Log.w(TAG, "showIfAvailable failed: $t")
            isShowing = false
            isShowScheduled = false
            for (l in onLoadFailedListeners) runCatching { l.invoke() }
            return false
        }
    }

    /**
     * Internal: notify that ad finished (compat with previous simulated impl)
     */
    internal fun notifyAdFinished() {
        Log.d(TAG, "notifyAdFinished -> calling finished listeners")
        isShowing = false
        for (l in onFinishedListeners) runCatching { l.invoke() }
    }

    fun wasRecentlyShown(): Boolean {
        return try {
            // Prefer server-controlled cooldown via AdController
            AdController.isAppOpenInCooldown()
        } catch (_: Throwable) {
            // Fallback to local timestamp if AdController unavailable
            val t = lastDismissedAt
            if (t == 0L) return false
            (System.currentTimeMillis() - t) < 60_000L
        }
    }
}
