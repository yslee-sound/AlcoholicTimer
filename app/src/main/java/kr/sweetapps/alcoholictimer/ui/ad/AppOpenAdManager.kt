package kr.sweetapps.alcoholictimer.ui.ad

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
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
        if (registerLifecycle) {
            try {
                var startedCount = 0
                var pendingShowAttempt = false
                // Observe process lifecycle to handle resume-from-background centrally
                try {
                    ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
                        try {
                            if (event == Lifecycle.Event.ON_START) {
                                Log.d(TAG, "processLifecycle: ON_START (app foregrounded)")
                                try {
                                    // if we have a loaded ad ready, attempt to show it using current activity if possible
                                    val act = try { kr.sweetapps.alcoholictimer.MainApplication.currentActivity } catch (_: Throwable) { null }
                                    if (act != null && autoShowEnabled && !wasRecentlyShown() && isLoaded()) {
                                        Log.d(TAG, "processLifecycle: ready-to-show AppOpen -> will attempt show and hide banner before show")
                                        // ensure banner hidden immediately
                                        try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                                        runCatching { showIfAvailable(act, true) }.onFailure { Log.w(TAG, "processLifecycle show failed: ${it.message}") }
                                    }
                                } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {}
                    })
                } catch (_: Throwable) {}
                application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                    override fun onActivityStarted(activity: android.app.Activity) {
                        try {
                            startedCount++
                            if (startedCount == 1) {
                                // app moved to foreground
                                Log.d(TAG, "lifecycle: app moved to foreground -> attempting post-foreground AppOpen show (autoShow=$autoShowEnabled)")
                                if (autoShowEnabled) {
                                    // Use MainApplication.currentActivity if available, else activity param
                                    val act = try { kr.sweetapps.alcoholictimer.MainApplication.currentActivity } catch (_: Throwable) { null } ?: activity
                                    // avoid showing on splash/overlay activities
                                    val cls = act.javaClass.simpleName
                                    if (cls == "SplashScreen" || cls == "AppOpenOverlayActivity") {
                                        Log.d(TAG, "lifecycle: foreground activity is $cls -> skip automatic AppOpen show")
                                        return
                                    }
                                    if (!wasRecentlyShown()) {
                                        if (isLoaded()) {
                                            runCatching { showIfAvailable(act, true) }.onFailure { Log.w(TAG, "lifecycle: showIfAvailable failed: ${it.message}") }
                                        } else {
                                            Log.d(TAG, "lifecycle: no loaded ad -> triggering preload and scheduling short retry")
                                            try {
                                                // trigger preload for next show attempt
                                                application.applicationContext?.let { ctx -> preload(ctx) }
                                                if (!pendingShowAttempt) {
                                                    pendingShowAttempt = true
                                                    // short delay to allow load to start/complete; 700ms is a balance between UX and load time
                                                    mainHandler.postDelayed({
                                                        try {
                                                            pendingShowAttempt = false
                                                            val curAct = try { kr.sweetapps.alcoholictimer.MainApplication.currentActivity } catch (_: Throwable) { null } ?: act
                                                            if (!wasRecentlyShown() && isLoaded()) {
                                                                Log.d(TAG, "lifecycle: retry show after preload delay on ${curAct.javaClass.simpleName}")
                                                                runCatching { showIfAvailable(curAct, true) }.onFailure { Log.w(TAG, "lifecycle retry show failed: ${it.message}") }
                                                            } else {
                                                                Log.d(TAG, "lifecycle: retry show aborted (not loaded or recently shown)")
                                                            }
                                                        } catch (_: Throwable) { pendingShowAttempt = false }
                                                    }, 700L)
                                                }
                                            } catch (_: Throwable) {
                                                Log.d(TAG, "lifecycle: preload trigger failed")
                                            }
                                        }
                                    } else {
                                        Log.d(TAG, "lifecycle: recently shown -> skip")
                                    }
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                    override fun onActivityResumed(activity: android.app.Activity) {}
                    override fun onActivityPaused(activity: android.app.Activity) {}
                    override fun onActivityStopped(activity: android.app.Activity) {
                        try {
                            startedCount = (startedCount - 1).coerceAtLeast(0)
                            if (startedCount == 0) {
                                Log.d(TAG, "lifecycle: app moved to background -> scheduling preload for next foreground")
                                try {
                                    // don't preload if an ad is currently showing
                                    if (!isShowing && !isLoading && !loaded) {
                                        application.applicationContext?.let { ctx ->
                                            mainHandler.post { try { preload(ctx) } catch (_: Throwable) {} }
                                        }
                                    } else {
                                        Log.d(TAG, "lifecycle: skip preload (isShowing=$isShowing isLoading=$isLoading loaded=$loaded)")
                                    }
                                } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {}
                    }
                    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                    override fun onActivityDestroyed(activity: android.app.Activity) {}
                })
            } catch (_: Throwable) {}
        }
    }

    fun noteAppStart() {
        Log.d(TAG, "noteAppStart called")
    }

    fun preload(context: Context) {
        // ?�� ?�?�밍 진단: AppOpen 광고 로드 ?�청 ?�각 기록
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logAppOpenLoadRequest()

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

                    // ?�� ?�?�밍 진단: AppOpen 광고 로드 ?�료 ?�각 기록
                    kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logAppOpenLoadComplete()

                    appOpenAd = ad
                    loaded = true
                    isLoading = false
                    try { onLoadedListener?.invoke() } catch (_: Throwable) {}
                    for (l in loadedListeners) runCatching { l.invoke() }

                    // If the app is currently foreground and autoShow is enabled, try to show immediately.
                    try {
                        if (autoShowEnabled) {
                            mainHandler.post {
                                try {
                                    // Use MainApplication.currentActivity if available, else skip
                                    val act = try { kr.sweetapps.alcoholictimer.MainApplication.currentActivity } catch (_: Throwable) { null }
                                    if (act != null) {
                                        val cls = act.javaClass.simpleName
                                        if (cls != "SplashScreen" && cls != "AppOpenOverlayActivity") {
                                          if (!wasRecentlyShown() && isLoaded()) {
                                                Log.d(TAG, "onAdLoaded: app in foreground -> attempting immediate show on ${act.javaClass.simpleName}")
                                                runCatching { showIfAvailable(act, true) }.onFailure { Log.w(TAG, "onAdLoaded immediate show failed: ${it.message}") }
                                            } else {
                                                Log.d(TAG, "onAdLoaded: not showing now (recently shown or not loaded)")
                                            }
                                        } else {
                                            Log.d(TAG, "onAdLoaded: current activity is $cls -> skip immediate show")
                                        }
                                    } else {
                                        Log.d(TAG, "onAdLoaded: no current activity -> skip immediate show")
                                    }
                                } catch (_: Throwable) {}
                            }
                        }
                    } catch (_: Throwable) {}

                    // assign fullscreen callback to manage show/dismiss events
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "AppOpen onAdShowedFullScreenContent")
                            isShowing = true
                            lastShownAt = System.currentTimeMillis()
                            // Record shown in central controller so policy counters update
                            try { applicationRef?.let { AdController.recordAppOpenShown(it.applicationContext) } } catch (_: Throwable) {}
                            // Note: setBannerForceHidden(true)???��? show() ?�출 ?�에 ?�행??(중복 방�?)
                            try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                            try { Log.d(TAG, "onAdShowed -> AdController.debugSnapshot=${AdController.debugSnapshot()}") } catch (_: Throwable) {}
                            try { onShownListener?.invoke() } catch (_: Throwable) {}
                            for (l in shownListeners) runCatching { l.invoke() }
                        }

                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            Log.w(TAG, "AppOpen onAdFailedToShowFullScreenContent: ${error.message}")
                            // treat as finished to continue app flow
                            isShowing = false
                            appOpenAd = null
                            loaded = false

                            // ?�� ?�발 방�?: 배너 복구�??�실?�게 보장
                            try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                            try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                            try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
                            try { AdController.ensureBannerVisible("appOpenFailedToShow") } catch (_: Throwable) {}
                            performFinishFlow()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "AppOpen onAdDismissedFullScreenContent")
                            isShowing = false
                            appOpenAd = null
                            loaded = false
                            lastDismissedAt = System.currentTimeMillis()

                            // ?�� ?�발 방�?: 배너 복구�??�실?�게 보장 (?�서 중요!)
                            try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                            try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                            try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
                            try { AdController.ensureBannerVisible("appOpenDismissed") } catch (_: Throwable) {}
                            try { Log.d(TAG, "onAdDismissed -> AdController.debugSnapshot=${AdController.debugSnapshot()}") } catch (_: Throwable) {}
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
                    // schedule a short retry to handle transient network/no-fill situations
                    try {
                        applicationRef?.applicationContext?.let { ctx ->
                            val retryMs = 1000L
                            Log.d(TAG, "onAdFailedToLoad -> scheduling retry preload in ${retryMs}ms")
                            mainHandler.postDelayed({ try { preload(ctx) } catch (_: Throwable) {} }, retryMs)
                        }
                    } catch (_: Throwable) {}
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "preload exception: ${t.message}")
            isLoading = false
        }
    }

    fun isLoaded(): Boolean = loaded
    fun isShowingAd(): Boolean = isShowing
    fun isLoading(): Boolean = isLoading

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

        // Consult central policy controller before showing
        try {
            val can = try { AdController.canShowAppOpen(activity) } catch (_: Throwable) { false }
            Log.d(TAG, "showIfAvailable: AdController.canShowAppOpen=$can; debug=${try { AdController.debugSnapshot() } catch (_: Throwable) { "<err>" }}")
            if (!can) {
                Log.d(TAG, "showIfAvailable: AdController denies app-open by policy")
                return false
            }
        } catch (_: Throwable) {}

        // Basic suppression: if recently shown within configured gap, treat as recent
        if (!bypassRecentFullscreenSuppression && wasRecentlyShown()) {
            Log.d(TAG, "showIfAvailable: suppressed due to recent show")
            return false
        }

        // ?�� AdMob ?�책 준?? show() ?�출 직전??배너�?즉시 ?�겨??겹침 방�?
        try {
            Log.d(TAG, "showIfAvailable: hiding banner IMMEDIATELY before show() to prevent overlap (AdMob policy)")
            try { AdController.hideBannerImmediately("appOpenBeforeShow") } catch (_: Throwable) {}

            // 추�? ?�전?�치: StateFlow???�데?�트
            try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(true) } catch (_: Throwable) {}
            try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}

            // [수정] 150ms 지연 제거 - 광고를 즉시 표시하여 스플래시 화면이 먼저 해제되는 것 방지
            // AdMob 정책: 광고가 완전히 표시되고 사용자가 닫을 때까지 다른 화면으로 이동 금지
            try {
                appOpenAd?.show(activity)
                Log.d(TAG, "showIfAvailable: appOpenAd.show() called immediately (AdMob policy compliance)")
            } catch (t: Throwable) {
                Log.w(TAG, "show failed: ${t.message}")
                try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                try { AdController.ensureBannerVisible("appOpenShowException") } catch (_: Throwable) {}
                return false
            }
             return true
         } catch (t: Throwable) {
             Log.w(TAG, "showIfAvailable: failed to show app open ad: ${t.message}")
             // Revert full-screen flag if show failed immediately
             try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
             try { AdController.ensureBannerVisible("appOpenShowException") } catch (_: Throwable) {}
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
                try {
                    val debug = try { AdController.debugSnapshot() } catch (_: Throwable) { "<err>" }
                    Log.d(TAG, "performFinishFlow -> AdController.debugSnapshot before scheduling preload: $debug")
                    // Prepare next load quickly: preload shortly after ad dismissal so future foregrounds have a loaded ad.
                    val delayMs = 0L
                    Log.d(TAG, "performFinishFlow -> scheduling immediate preload after ${delayMs}ms (prepare next ad)")
                    mainHandler.postDelayed({ try { preload(ctx) } catch (_: Throwable) {} }, delayMs)
                 } catch (_: Throwable) {
                     mainHandler.postDelayed({ try { preload(ctx) } catch (_: Throwable) {} }, 30_000L)
                 }
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
        return try {
            val minGapSec = AdController.getMinFullscreenGapSeconds()
            elapsed < (minGapSec.coerceAtLeast(1).toLong() * 1000L)
        } catch (_: Throwable) {
            // fallback to 5s
            elapsed < 5_000L
        }
    }
}
