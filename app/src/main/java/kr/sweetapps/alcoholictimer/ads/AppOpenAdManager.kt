package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.UserMessagingPlatform
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * App Open Ad Manager
 * - 콜드 스타트 및 조건부 Resume 경로에서 AppOpen을 제어
 * - AdController 정책을 준수하고 Interstitial과 충돌을 방지
 */
object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private const val TAG = "AppOpenAdManager"

    // Google's sample App Open Ad unit id (테스트/폴백)
    private const val GOOGLE_TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/3419835294"
    private const val PROD_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921" // 실제 운영 광고 단위 ID로 교체

    private fun currentUnitId(): String {
        // 운영 광고 단위 ID를 항상 반환
        return PROD_APP_OPEN_ID
    }

    private var app: Application? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    // App start timestamp to limit AppOpen display to immediate startup window
    private var appStartMs: Long = 0L
    fun noteAppStart() {
        appStartMs = System.currentTimeMillis()
        Log.d(TAG, "noteAppStart: appStartMs=$appStartMs")
    }
    // Allow AppOpen only within this window after cold start (10s)
    private const val APP_OPEN_ALLOWED_WINDOW_MS = 10 * 1000L // 10 seconds

    private var appOpenAd: AppOpenAd? = null
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)
    // 라이프사이클 기반 자동 표시를 제어하는 플래그
    private val allowAutoShow = AtomicBoolean(false)

    fun setAutoShowEnabled(enabled: Boolean) {
        allowAutoShow.set(enabled)
        Log.d(TAG, "setAutoShowEnabled=$enabled")
    }

    private var lastLoadedAt: Long = 0L
    private var lastShownAt: Long = 0L

    // 간단 쿨다운: 표시 후 60초 내에는 다시 시도하지 않음 (과도 노출 방지)
    private const val SHOW_COOLDOWN_MS = 60 * 1000L

    private var healthCheckHandler: Handler? = null
    private val healthRunnable = object : Runnable {
        override fun run() {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(app!!.applicationContext) }.getOrNull()
            val canRequest = consentInfo?.canRequestAds() == true
            Log.d(TAG, "healthCheck: adLoaded=${appOpenAd!=null} isLoading=${isLoading.get()} isShowing=${isShowing.get()} canRequest=$canRequest lastLoadedAt=$lastLoadedAt lastShownAt=$lastShownAt")
            if (!isShowing.get() && appOpenAd == null && !isLoading.get() && canRequest) {
                // Only preload during startup window; avoid loading AppOpen long after cold start
                try {
                    if (appStartMs > 0L) {
                        val sinceStart = System.currentTimeMillis() - appStartMs
                        if (sinceStart > APP_OPEN_ALLOWED_WINDOW_MS) {
                            Log.d(TAG, "healthCheck: skipping preload outside startup window (sinceStart=${sinceStart}ms)")
                        } else {
                            Log.d(TAG, "healthCheck: triggering preload (within startup window)")
                            app?.let { preload(it.applicationContext) }
                        }
                    } else {
                        Log.d(TAG, "healthCheck: triggering preload (no appStart recorded)")
                        app?.let { preload(it.applicationContext) }
                    }
                } catch (_: Throwable) {
                    Log.d(TAG, "healthCheck: triggering preload (fallback)")
                    app?.let { preload(it.applicationContext) }
                }
            }
            healthCheckHandler?.postDelayed(this, 30_000)
        }
    }

    fun initialize(application: Application, registerLifecycle: Boolean = true) {
        if (app != null) return
        app = application
        Log.d(TAG, "initialize: application set. registerLifecycle=$registerLifecycle")
        if (registerLifecycle) startLifecycleMonitoring(application)
        // 초기 preload는 UMP 동의 전이므로 스킵, UMP 완료 후 onConsentUpdated에서 시작
        Log.d(TAG, "✅ Initialized (preload deferred until UMP consent)")
        healthCheckHandler = Handler(Looper.getMainLooper())
        healthCheckHandler?.postDelayed(healthRunnable, 30_000)
    }

    fun startLifecycleMonitoring(application: Application) {
        if (app == null) app = application
        try {
            Log.d(TAG, "startLifecycleMonitoring: registering lifecycle callbacks")
            application.registerActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (t: Throwable) {
            Log.w(TAG, "startLifecycleMonitoring failed: $t")
        }
    }

    fun preload(context: Context) {
        // 정책 확인: 정책이 비활성화되어 있으면 프리로드 차단
        val policyEnabled = try { kr.sweetapps.alcoholictimer.ads.AdController.isAppOpenEnabled() } catch (_: Throwable) { true }
        if (!policyEnabled) {
            Log.d(TAG, "preload skipped: app-open disabled by policy")
            return
        }
        if (isLoading.get()) { Log.d(TAG, "preload skipped: already loading @${System.currentTimeMillis()}"); return }
        if (appOpenAd != null) { Log.d(TAG, "preload skipped: already have ad @${System.currentTimeMillis()}"); return }

        val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(context) }.getOrNull()
        val canRequest = consentInfo?.canRequestAds() == true
        Log.d(TAG, "preload attempt @${System.currentTimeMillis()} canRequest=$canRequest consentStatus=${consentInfo?.consentStatus}")
        if (!canRequest) {
            Log.d(TAG, "preload skipped: consent not granted (will recheck in 10s)")
            Handler(Looper.getMainLooper()).postDelayed({
                if (appOpenAd == null && !isLoading.get()) preload(context.applicationContext)
            }, 10_000)
            return
        }

        isLoading.set(true)
        try { AdController.setAppOpenLoading(true) } catch (_: Throwable) {}
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            currentUnitId(),
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    // If we are outside the startup window, discard the loaded AppOpen ad to avoid late presentation
                    try {
                        if (appStartMs > 0L) {
                            val sinceStart = System.currentTimeMillis() - appStartMs
                            if (sinceStart > APP_OPEN_ALLOWED_WINDOW_MS) {
                                Log.d(TAG, "onAdLoaded: discarding app-open ad because outside startup window (sinceStart=${sinceStart}ms)")
                                isLoading.set(false)
                                lastLoadedAt = System.currentTimeMillis()
                                try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                                onAdLoadedListener?.invoke()
                                return
                            }
                        }
                    } catch (_: Throwable) {}
                    val policyOk = try { kr.sweetapps.alcoholictimer.ads.AdController.isPolicyFetchCompleted() && kr.sweetapps.alcoholictimer.ads.AdController.isAppOpenEnabled() } catch (_: Throwable) { true }
                    if (!policyOk) {
                        appOpenAd = null
                        isLoading.set(false)
                        try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                        lastLoadedAt = System.currentTimeMillis()
                        Log.d(TAG, "onAdLoaded app-open but discarded due to policy at @$lastLoadedAt")
                        onAdLoadedListener?.invoke()
                        return
                    }
                    appOpenAd = ad
                    isLoading.set(false)
                    try { AdController.setAppOpenLoaded(true); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                    lastLoadedAt = System.currentTimeMillis()
                    Log.d(TAG, "onAdLoaded app-open @${lastLoadedAt}")
                    currentActivityRef?.get()?.let { act ->
                        if (allowAutoShow.get()) {
                            Log.d(TAG, "onAdLoaded: auto-show enabled -> showing ad")
                            showIfAvailable(act)
                        } else {
                            Log.d(TAG, "onAdLoaded: auto-show suppressed, waiting for manual show")
                            onAdLoadedListener?.invoke()
                        }
                    } ?: Log.d(TAG, "show skip: no current activity ref")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoading.set(false)
                    try { AdController.setAppOpenLastError(error.toString()); AdController.setAppOpenLoading(false) } catch (_: Throwable) {}
                    Log.w(TAG, "onAdFailedToLoad app-open: $error @${System.currentTimeMillis()}")
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (appOpenAd == null && !isLoading.get()) preload(context.applicationContext)
                    }, 30_000)
                    onAdFinishedListener?.invoke()
                }
            }
        )
    }

    private val hasAutoShown = AtomicBoolean(false)

    private fun canShowNow(startupWindowRequired: Boolean = true): Boolean {
        try {
            if (startupWindowRequired && appStartMs > 0L) {
                val sinceStart = System.currentTimeMillis() - appStartMs
                if (sinceStart > APP_OPEN_ALLOWED_WINDOW_MS) { Log.d(TAG, "AppOpen blocked: outside startup window (sinceStart=$sinceStart)"); return false }
            }
        } catch (_: Throwable) {}
        try { if (AdController.isFullScreenAdShowing()) { Log.d(TAG, "AppOpen blocked: full-screen ad showing"); return false } } catch (_: Throwable) {}
        if (isOtherAdShowing.get()) { Log.d(TAG, "AppOpen blocked: another ad is showing"); return false }
        val enabled = try { AdController.isAppOpenEnabled() } catch (_: Throwable) { false }
        if (!enabled) { Log.d(TAG, "AppOpen disabled by policy"); return false }
        val now = System.currentTimeMillis()
        if (lastShownAt > 0 && now - lastShownAt < SHOW_COOLDOWN_MS) { Log.d(TAG, "AppOpen cooldown active"); return false }
        return true
    }

    private var isOtherAdShowing = AtomicBoolean(false)
    fun setOtherAdShowing(showing: Boolean) { isOtherAdShowing.set(showing); Log.d(TAG, "setOtherAdShowing: $showing") }

    fun showIfAvailable(activity: Activity) {
        showIfAvailableInternal(activity, allowNonSplash = false, bypassAutoShowGate = false, allowOutsideStartupWindow = false)
    }

    private fun showIfAvailableInternal(activity: Activity, allowNonSplash: Boolean, bypassAutoShowGate: Boolean, allowOutsideStartupWindow: Boolean) {
        Log.d(TAG, "showIfAvailableInternal allowNonSplash=$allowNonSplash bypassAutoShowGate=$bypassAutoShowGate allowOutsideStartupWindow=$allowOutsideStartupWindow")
        // Safety check: only allow non-splash callers if explicitly allowed
        if (!allowNonSplash) {
            try {
                val className = activity::class.qualifiedName ?: activity::class.java.name
                if (!className.contains(".feature.start.StartActivity") && !className.endsWith("StartActivity") && !className.contains(".ui.screens.SplashScreen") && !className.endsWith("SplashScreen")) {
                    Log.w(TAG, "showIfAvailable blocked: caller not StartActivity/SplashScreen")
                    preload(activity.applicationContext)
                    return
                }
            } catch (_: Throwable) { preload(activity.applicationContext); return }
        }

        if (!allowOutsideStartupWindow) {
            if (!canShowNow(true)) { Log.d(TAG, "showIfAvailable abort: canShowNow=false"); preload(activity.applicationContext); onAdFinishedListener?.invoke(); return }
        } else {
            if (!canShowNow(false)) { Log.d(TAG, "showIfAvailable abort: canShowNow(bypass)=false"); preload(activity.applicationContext); onAdFinishedListener?.invoke(); return }
        }

        // Policy-based per-hour/day check
        try { if (!kr.sweetapps.alcoholictimer.ads.AdController.canShowAppOpen(activity.applicationContext)) { Log.d(TAG, "showIfAvailable abort: AppOpen limit reached by policy"); onAdFinishedListener?.invoke(); return } } catch (_: Throwable) {}

        val ad = appOpenAd
        if (ad == null) { Log.d(TAG, "showIfAvailable abort: ad=null -> preload"); preload(activity.applicationContext); onAdFinishedListener?.invoke(); return }
        if (isShowing.get()) { Log.d(TAG, "showIfAvailable abort: already showing"); return }
        if (activity.isFinishing || activity.isDestroyed) { Log.d(TAG, "showIfAvailable abort: activity invalid"); return }

        // If bypassAutoShowGate=false, enforce the one-time auto-show per session
        if (!bypassAutoShowGate) {
            if (!hasAutoShown.compareAndSet(false, true)) { Log.d(TAG, "auto-show skipped: already attempted this session"); return }
        }

        isShowing.set(true)
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent")
                // 배너 겹침 방지: 전면 상태로 간주
                AdController.setInterstitialShowing(true)
                AdController.setFullScreenAdShowing(true)
                // 광고를 실제로 보여줄 때는 더 이상 로드된 상태가 아님
                try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLoading(false) } catch (_: Throwable) {}
                lastShownAt = System.currentTimeMillis()
                // notify listeners that ad is now visible
                onAdShownListener?.invoke()
                // 기록: 광고가 실제로 보였음을 기록(시간/일 제한 계산용)
                try {
                    currentActivityRef?.get()?.let { act ->
                        kr.sweetapps.alcoholictimer.ads.AdController.recordAppOpenShown(act.applicationContext)
                    }
                } catch (_: Throwable) {}
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent")
                AdController.setInterstitialShowing(false)
                AdController.setFullScreenAdShowing(false)
                try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
                onAdFinishedListener?.invoke()
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "onAdFailedToShowFullScreenContent: $adError")
                AdController.setInterstitialShowing(false)
                AdController.setFullScreenAdShowing(false)
                try { AdController.setAppOpenLastError(adError.toString()) } catch (_: Throwable) {}
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
                onAdFinishedListener?.invoke()
            }
        }
        Handler(Looper.getMainLooper()).post {
            try {
                // mark full-screen intent to avoid concurrent full-screen ads
                try { AdController.setFullScreenAdShowing(true); AdController.setInterstitialShowing(true) } catch (_: Throwable) {}
                ad.show(activity)
            } catch (t: Throwable) {
                Log.w(TAG, "show exception: $t")
                try { AdController.setInterstitialShowing(false); AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
            }
        }
    }

    // Resume-path: show on background->foreground if eligible and no conflict
    fun showOnResumeIfEligible(activity: Activity) {
        Log.d(TAG, "showOnResumeIfEligible called @${System.currentTimeMillis()}")
        // If other full-screen ad or interstitial is showing, skip
        try {
            if (AdController.isFullScreenAdShowing()) {
                Log.d(TAG, "showOnResumeIfEligible: blocked - full-screen ad already showing")
                return
            }
        } catch (_: Throwable) {}
        // If an interstitial is loaded and likely to be shown, avoid showing AppOpen to prevent conflict
        try {
            if (kr.sweetapps.alcoholictimer.ads.InterstitialAdManager.isLoaded()) {
                Log.d(TAG, "showOnResumeIfEligible: blocked - interstitial loaded, prefer to avoid conflict")
                return
            }
        } catch (_: Throwable) {}

        // Try to show ignoring the cold-start-only gate but honoring cooldown and policy
        showIfAvailableInternal(activity, allowNonSplash = true, bypassAutoShowGate = true, allowOutsideStartupWindow = true)
    }

    // Lifecycle
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "ProcessLifecycle onStart")
        currentActivityRef?.get()?.let { act -> if (allowAutoShow.get()) tryAutoShowIfFirstTime(act) }
    }

    private fun tryAutoShowIfFirstTime(activity: Activity) {
        if (!hasAutoShown.compareAndSet(false, true)) return
        showIfAvailable(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted ${activity::class.java.simpleName} @${System.currentTimeMillis()}")
        currentActivityRef = WeakReference(activity)
        // 포어그라운드 진입 시점에 즉시 시도 (중복 방지는 isShowing/cooldown으로 해결)
        if (allowAutoShow.get()) tryAutoShowIfFirstTime(activity) else Log.d(TAG, "onActivityStarted: auto-show suppressed for ${activity::class.java.simpleName}")
        // Also attempt resume-path show when coming from background
        try {
            showOnResumeIfEligible(activity)
        } catch (_: Throwable) { Log.w(TAG, "showOnResumeIfEligible failed: ${'$'}_") }
    }
    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed ${activity::class.java.simpleName} @${System.currentTimeMillis()}")
        currentActivityRef = WeakReference(activity)
        // 재개 시에도 한 번 더 시도 (조건 불충족 시 내부에서 빠르게 return)
        if (allowAutoShow.get()) tryAutoShowIfFirstTime(activity) else Log.d(TAG, "onActivityResumed: auto-show suppressed for ${activity::class.java.simpleName}")
        // Try resume-path show as well
        try {
            showOnResumeIfEligible(activity)
        } catch (_: Throwable) { Log.w(TAG, "showOnResumeIfEligible failed: ${'$'}_") }
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    // 광고 종료/실패 시 호출되는 콜백
    private var onAdFinishedListener: (() -> Unit)? = null
    fun setOnAdFinishedListener(listener: (() -> Unit)?) {
        onAdFinishedListener = listener
    }

    // 광고 로드 완료 시 호출되는 콜백 (StartActivity가 리스너로 등록하여 수동 표시 가능)
    private var onAdLoadedListener: (() -> Unit)? = null
    fun setOnAdLoadedListener(listener: (() -> Unit)?) { onAdLoadedListener = listener }

    // 광고가 실제로 화면에 나타날 때 호출되는 콜백
    private var onAdShownListener: (() -> Unit)? = null
    fun setOnAdShownListener(listener: (() -> Unit)?) { onAdShownListener = listener }

    // 정책 비활성화 등에서 이미 로드된 AppOpen을 안전하게 제거
    fun clearLoadedAd() {
        try {
            appOpenAd = null
            isLoading.set(false)
            isShowing.set(false)
            try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
            Log.d(TAG, "clearLoadedAd: app-open cleared by policy change")
        } catch (t: Throwable) {
            Log.w(TAG, "clearLoadedAd failed: $t")
        }
    }

    // Called by UMP consent manager when consent status changes.
    // If consent is granted, attempt to preload AppOpen; if revoked, clear any loaded AppOpen.
    fun onConsentUpdated(canRequestAds: Boolean) {
        Log.d(TAG, "onConsentUpdated: canRequestAds=$canRequestAds")
        try {
            if (canRequestAds) {
                // If we have an Application reference, try to preload immediately
                app?.applicationContext?.let { ctx ->
                    if (appOpenAd == null && !isLoading.get()) {
                        Log.d(TAG, "onConsentUpdated: consent granted -> preload")
                        preload(ctx)
                    }
                }
            } else {
                // Consent revoked: clear any loaded/queued AppOpen and update state
                Log.d(TAG, "onConsentUpdated: consent revoked -> clear loaded ad and reset state")
                clearLoadedAd()
            }
        } catch (_: Throwable) { Log.w(TAG, "onConsentUpdated handling failed") }
    }

    fun isShowingAd(): Boolean = isShowing.get()

    /** 현재 AppOpen 광고가 로드되어 있는지 여부 */
    fun isLoaded(): Boolean = appOpenAd != null

}
