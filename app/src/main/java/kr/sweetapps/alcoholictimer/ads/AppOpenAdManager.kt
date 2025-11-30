package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.CopyOnWriteArraySet
import android.os.Handler
import android.os.Looper
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager

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
    private val loadedListeners = CopyOnWriteArraySet<() -> Any?>()
    private val shownListeners = CopyOnWriteArraySet<() -> Any?>()
    private val finishedListeners = CopyOnWriteArraySet<() -> Any?>()
    private val loadFailedListeners = CopyOnWriteArraySet<() -> Any?>()

    @Volatile private var onLoadedListener: (() -> Any?)? = null
    @Volatile private var onShownListener: (() -> Any?)? = null
    @Volatile private var onFinishedListener: (() -> Any?)? = null
    @Volatile private var onLoadFailedListener: (() -> Any?)? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(application: Application, registerLifecycle: Boolean = true) {
        applicationRef = application
        firebaseAnalytics = FirebaseAnalytics.getInstance(application)
        Log.d(TAG, "initialize: application set. registerLifecycle=$registerLifecycle")
    }

    fun noteAppStart() { /* optional hook for lifecycle-based flows */ }

    fun setAutoShowEnabled(enabled: Boolean) {
        autoShowEnabled = enabled
        Log.d(TAG, "setAutoShowEnabled=$enabled")
    }

    private fun validateOrFallbackUnit(candidate: String?): String {
        if (candidate.isNullOrBlank()) return "ca-app-pub-3940256099942544/9257395921"
        val trimmed = candidate.trim()
        return if (trimmed.contains("/") && trimmed.contains("ca-app-pub-")) trimmed
        else {
            Log.w(TAG, "Invalid ad unit id provided: '$candidate' -> falling back to test unit")
            "ca-app-pub-3940256099942544/9257395921"
        }
    }

    fun preload(context: Context) {
        try {
            if (applicationRef == null && context.applicationContext is Application) {
                applicationRef = context.applicationContext as Application
                Log.d(TAG, "preload: applicationRef was null - set from context.applicationContext")
            }
        } catch (_: Throwable) {
            Log.w(TAG, "preload: couldn't set applicationRef from context")
        }

        if (appOpenAd != null || isLoading) {
            Log.d(TAG, "preload skipped: already loading/have ad")
            return
        }

        val unitIdCandidate = try {
            val cls = Class.forName(context.packageName + ".BuildConfig")
            val field = cls.getDeclaredField("ADMOB_APP_OPEN_UNIT_ID")
            field.get(null) as? String
        } catch (_: Throwable) {
            null
        }

        val debugOverride = try {
            val cls = Class.forName(context.packageName + ".BuildConfig")
            val dbgField = cls.getDeclaredField("DEBUG")
            (dbgField.get(null) as? Boolean) == true
        } catch (_: Throwable) {
            false
        }

        var unitId = validateOrFallbackUnit(unitIdCandidate)
        if (debugOverride) {
            unitId = "ca-app-pub-3940256099942544/9257395921"
            Log.d(TAG, "preload: DEBUG build detected - overriding app open unit to test id: $unitId")
        }

        Log.d(TAG, "preload: loading unit=$unitId")
        isLoading = true
        val loadContext: Context = applicationRef?.applicationContext ?: context.applicationContext
        val request = AdRequestFactory.create(loadContext)

        AppOpenAd.load(
            loadContext,
            unitId,
            request,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "onAdLoaded app-open")
                    appOpenAd = ad
                    isLoading = false
                    ad.onPaidEventListener = com.google.android.gms.ads.OnPaidEventListener { adValue ->
                        val bundle = Bundle()
                        bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob")
                        bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName)
                        bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, "app_open")
                        bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, ad.adUnitId)
                        bundle.putDouble(FirebaseAnalytics.Param.VALUE, adValue.valueMicros / 1000000.0)
                        bundle.putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                        firebaseAnalytics.logEvent("ad_revenue", bundle)
                    }
                    runCatching { onLoadedListener?.invoke() }
                    for (l in loadedListeners) runCatching { l.invoke() }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open failed to load: message=${loadAdError.message} | code=${loadAdError.code} | domain=${loadAdError.domain} | responseInfo=${loadAdError.responseInfo}")
                    isLoading = false
                    runCatching { onLoadFailedListener?.invoke() }
                    for (l in loadFailedListeners) runCatching { l.invoke() }
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
        onFinishedListener = listener
    }
    fun setOnAdLoadedListener(listener: (() -> Unit)?) {
        onLoadedListener = listener
        if (listener != null && appOpenAd != null) {
            Log.d(TAG, "setOnAdLoadedListener: ad already loaded -> invoking listener immediately")
            runCatching { listener.invoke() }
        }
    }
    fun setOnAdShownListener(listener: (() -> Unit)?) {
        onShownListener = listener
    }
    fun setOnAdLoadFailedListener(listener: (() -> Unit)?) {
        onLoadFailedListener = listener
    }
    fun addOnAdLoadedListener(listener: () -> Any?) { loadedListeners.add(listener); if (appOpenAd != null) runCatching { listener.invoke() } }
    fun removeOnAdLoadedListener(listener: () -> Any?) { loadedListeners.remove(listener) }

    fun addOnAdShownListener(listener: () -> Any?) { shownListeners.add(listener) }
    fun removeOnAdShownListener(listener: () -> Any?) { shownListeners.remove(listener) }

    fun addOnAdFinishedListener(listener: () -> Any?) { finishedListeners.add(listener) }
    fun removeOnAdFinishedListener(listener: () -> Any?) { finishedListeners.remove(listener) }

    fun addOnAdLoadFailedListener(listener: () -> Any?) { loadFailedListeners.add(listener) }
    fun removeOnAdLoadFailedListener(listener: () -> Any?) { loadFailedListeners.remove(listener) }


    fun onConsentUpdated(canRequestAds: Boolean) {
        Log.d(TAG, "onConsentUpdated: canRequestAds=$canRequestAds")
        try { kr.sweetapps.alcoholictimer.ads.AdController.triggerBannerReload() } catch (_: Throwable) {}
        if (canRequestAds) {
            applicationRef?.let { preload(it.applicationContext) }
        }
    }

    fun showIfAvailable(activity: Activity): Boolean {
        Log.d(TAG, "showIfAvailable called - loaded=${appOpenAd != null} showing=$isShowing autoShow=$autoShowEnabled activity=${activity.javaClass.simpleName} finishing=${activity.isFinishing} destroyed=${if (android.os.Build.VERSION.SDK_INT>=17) activity.isDestroyed else false}")
        try {
            // Additional debug tag for easier logcat filtering in devs' environment
            try { Log.d("DebugScreenVM", "showIfAvailable called - loaded=${appOpenAd != null} showing=$isShowing autoShow=$autoShowEnabled activity=${activity.javaClass.simpleName}") } catch (_: Throwable) {}
            val privacyRequired = try { UmpConsentManager.isPrivacyOptionsRequired(activity) } catch (_: Throwable) { false }
            if (privacyRequired && !UmpConsentManager.consentChecked) {
                Log.d(TAG, "showIfAvailable suppressed: UMP privacy required and consent not yet checked")
                try { Log.d("DebugScreenVM", "suppress_reason=privacy_required_and_consent_not_checked privacyRequired=$privacyRequired consentChecked=${UmpConsentManager.consentChecked}") } catch (_: Throwable) {}
                return false
            }
            try {
                if (UmpConsentManager.isFormShowing()) {
                    Log.d(TAG, "showIfAvailable suppressed: UMP consent form is currently showing")
                    try { Log.d("DebugScreenVM", "suppress_reason=form_showing") } catch (_: Throwable) {}
                    return false
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}

        if (appOpenAd == null || isShowing) return false
        if (appOpenAd == null) {
            try { Log.d("DebugScreenVM", "suppress_reason=no_loaded_ad appOpenAd=null") } catch (_: Throwable) {}
            return false
        }
        if (isShowing) {
            try { Log.d("DebugScreenVM", "suppress_reason=already_showing isShowing=true") } catch (_: Throwable) {}
            return false
        }
        try {
            val lastDismissFromController = try { AdController.getLastFullScreenDismissedAt() } catch (_: Throwable) { 0L }
            val lastAppOpenShown = try { AdController.getLastAppOpenShownAt() } catch (_: Throwable) { 0L }
            val lastDismiss = when {
                lastDismissFromController > 0L -> lastDismissFromController
                lastAppOpenShown > 0L -> lastAppOpenShown
                else -> lastDismissedAt
            }
            if (lastDismiss > 0L) {
                val serverGapSec = try { AdController.getMinFullscreenGapSeconds() } catch (_: Throwable) { 30 }
                val minBufferMs = 5_000L
                val gapMs = (serverGapSec.coerceAtLeast(1) * 1000L).coerceAtLeast(minBufferMs)
                val elapsed = System.currentTimeMillis() - lastDismiss
                if (elapsed < gapMs) {
                    Log.d(TAG, "showIfAvailable: suppressed because recent full-screen ad dismissed (elapsed=${elapsed}ms < ${gapMs}ms). controllerDismiss=$lastDismissFromController appOpenShown=$lastAppOpenShown local=$lastDismissedAt")
                    try { Log.d("DebugScreenVM", "suppress_reason=recent_fullscreen elapsed=${elapsed} gapMs=${gapMs} lastDismiss=$lastDismiss") } catch (_: Throwable) {}
                    return false
                }
            } else {
                val startupSuppressionMs = 5_000L
                val sinceProcessStart = try {
                    android.os.SystemClock.uptimeMillis()
                } catch (_: Throwable) { 0L }
                if (sinceProcessStart < startupSuppressionMs) {
                    Log.d(TAG, "showIfAvailable: applying small startup suppression (${startupSuppressionMs}ms) due to missing timestamps")
                    try { Log.d("DebugScreenVM", "suppress_reason=startup_suppression sinceProcessStart=${sinceProcessStart}ms") } catch (_: Throwable) {}
                    return false
                }
            }
        } catch (_: Throwable) {}
        try {
            try {
                if (!AdController.canShowAppOpen(activity)) {
                    Log.d(TAG, "showIfAvailable: policy disallows app-open -> returning false")
                    try { Log.d("DebugScreenVM", "suppress_reason=policy_disallow canShowAppOpen=false") } catch (_: Throwable) {}
                    return false
                }
                if (AdController.isFullScreenAdShowing() || AdController.isInterstitialShowingNow()) {
                    Log.d(TAG, "showIfAvailable: another full-screen/interstitial is active -> skipping app-open")
                    try { Log.d("DebugScreenVM", "suppress_reason=other_fullscreen_active") } catch (_: Throwable) {}
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
                    try { AdController.recordAppOpenShown(activity) } catch (_: Throwable) {}
                    try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
                    for (l in finishedListeners) runCatching { l.invoke() }
                    runCatching { onFinishedListener?.invoke() }
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
                    try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { AdController.triggerBannerReload() } catch (_: Throwable) {}
                    for (l in loadFailedListeners) runCatching { l.invoke() }
                    runCatching { onLoadFailedListener?.invoke() }
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AppOpen onAdShowedFullScreenContent")
                    isShowing = true
                    isShowScheduled = false
                    try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                    for (l in shownListeners) runCatching { l.invoke() }
                    runCatching { onShownListener?.invoke() }
                    // Analytics: log impression
                    try { runCatching { AnalyticsManager.logAdImpression("app_open") } } catch (_: Throwable) {}
                }

                override fun onAdClicked() {
                    Log.d(TAG, "AppOpen onAdClicked")
                    try { runCatching { AnalyticsManager.logAdClick("app_open") } } catch (_: Throwable) {}
                }
            }

            Log.d(TAG, "Attempting to show AppOpen ad now (safely on UI thread)")
            if (isShowScheduled) {
                Log.d(TAG, "showIfAvailable: already scheduled -> returning false")
                return false
            }
            isShowScheduled = true
            var shown = false
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
                                try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                                // Extra guard: if UMP consent form is showing or controller indicates full-screen, cancel show
                                try {
                                    if (UmpConsentManager.isFormShowing() || AdController.isFullScreenAdShowing()) {
                                        Log.d(TAG, "showIfAvailable cancelled: consent form or other full-screen active -> aborting show")
                                        isShowScheduled = false
                                        try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                                        return@Runnable
                                    }
                                } catch (_: Throwable) {}
                                ad.show(activity)
                                shown = true
                                Log.d(TAG, "ad.show() invoked via decorView.post")
                            } catch (t: Throwable) {
                                Log.w(TAG, "ad.show threw: $t")
                                isShowScheduled = false
                                try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
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
                    activity.runOnUiThread(decorRunnable)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "scheduling ad.show failed: $t")
                isShowScheduled = false
            }
            if (!shown) {
                Log.d(TAG, "showIfAvailable: ad not shown immediately and overlay fallback suppressed")
            }
            Log.d(TAG, "ad.show() returned (scheduled). shownFlag=$shown, awaiting callbacks")
            return shown
        } catch (t: Throwable) {
            Log.w(TAG, "showIfAvailable failed: $t")
            isShowing = false
            isShowScheduled = false
            for (l in loadFailedListeners) runCatching { l.invoke() }
            runCatching { onLoadFailedListener?.invoke() }
            return false
        }
    }

    internal fun notifyAdFinished() {
        Log.d(TAG, "notifyAdFinished -> calling finished listeners")
        isShowing = false
        for (l in finishedListeners) runCatching { l.invoke() }
        runCatching { onFinishedListener?.invoke() }
    }

    fun wasRecentlyShown(): Boolean {
        return try {
            AdController.isAppOpenInCooldown()
        } catch (_: Throwable) {
            val t = lastDismissedAt
            if (t == 0L) return false
            (System.currentTimeMillis() - t) < 60_000L
        }
    }
}
