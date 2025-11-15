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
import kr.sweetapps.alcoholictimer.BuildConfig
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * App Open Ad Manager
 * - 앱이 포어그라운드로 돌아올 때(App onStart) App Open Ad를 표시합니다.
 * - AdController의 정책을 준수합니다.
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
                Log.d(TAG, "healthCheck: triggering preload")
                app?.let { preload(it.applicationContext) }
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

    private var isOtherAdShowing = AtomicBoolean(false)

    fun setOtherAdShowing(showing: Boolean) {
        isOtherAdShowing.set(showing)
        Log.d(TAG, "setOtherAdShowing: $showing")
    }

    private fun canShowNow(): Boolean {
        // 중앙 상태에서 이미 전체 광고가 표시 중이면 차단
        try {
            if (AdController.isFullScreenAdShowing()) {
                Log.d(TAG, "AppOpen blocked: AdController reports full-screen ad showing")
                return false
            }
        } catch (_: Throwable) {}
        if (isOtherAdShowing.get()) {
            Log.d(TAG, "AppOpen blocked: another ad is showing")
            return false
        }
        // 정책 확인
        val enabled = try { AdController.isAppOpenEnabled() } catch (_: Throwable) { false }
        if (!enabled) {
            Log.d(TAG, "AppOpen disabled by policy")
            return false
        }
        // 최근 표시 쿨다운
        val now = System.currentTimeMillis()
        if (lastShownAt > 0 && now - lastShownAt < SHOW_COOLDOWN_MS) {
            Log.d(TAG, "AppOpen cooldown: ${(SHOW_COOLDOWN_MS - (now - lastShownAt))}ms remain")
            return false
        }
        Log.d(TAG, "AppOpen canShowNow = true (policy enabled, cooldown ok)")
        return true
    }

    fun onConsentUpdated(canRequestAds: Boolean) {
        Log.d(TAG, "onConsentUpdated canRequestAds=$canRequestAds adLoaded=${appOpenAd!=null} isLoading=${isLoading.get()}")
        if (canRequestAds) {
            // 즉시 프리로드 시도 (이미 로드/로딩 아니면)
            if (appOpenAd == null && !isLoading.get()) {
                app?.let { preload(it.applicationContext) }
            }
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
            // 짧은 지연 후 재확인 시도
            Handler(Looper.getMainLooper()).postDelayed({
                // 재귀 호출 전에 로드 상태 다시 확인
                if (appOpenAd == null && !isLoading.get()) preload(context.applicationContext)
            }, 10_000)
            return
        }

        isLoading.set(true)
        // 중앙 상태 업데이트: AppOpen 로드 시작
        try { AdController.setAppOpenLoading(true) } catch (_: Throwable) {}
        val unitId = currentUnitId()
        Log.d(TAG, "preload start: unitId=$unitId @${System.currentTimeMillis()}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    // 정책 상태를 확인: 정책이 비활성화이면 로드된 광고를 보관하지 않고 리스너만 호출
                    val policyEnabled = try { kr.sweetapps.alcoholictimer.ads.AdController.isPolicyFetchCompleted() && kr.sweetapps.alcoholictimer.ads.AdController.isAppOpenEnabled() } catch (_: Throwable) { true }
                    if (!policyEnabled) {
                        appOpenAd = null
                        isLoading.set(false)
                        try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                        lastLoadedAt = System.currentTimeMillis()
                        Log.d(TAG, "onAdLoaded app-open but discarded due to policy at @$lastLoadedAt")
                        // 알림만 전달하여 StartActivity가 스플래시를 해제하도록 함
                        onAdLoadedListener?.invoke()
                        return
                    }
                     appOpenAd = ad
                     isLoading.set(false)
                     // 중앙 상태 업데이트: 로드 성공
                     try { AdController.setAppOpenLoaded(true); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                     lastLoadedAt = System.currentTimeMillis()
                     Log.d(TAG, "onAdLoaded app-open @${lastLoadedAt}")
                     // 포어그라운드 상태라면 즉시 표시 시도
                     currentActivityRef?.get()?.let { act ->
                         if (allowAutoShow.get()) {
                             Log.d(TAG, "onAdLoaded: auto-show enabled -> showing ad")
                             showIfAvailable(act)
                         } else {
                             Log.d(TAG, "onAdLoaded: auto-show suppressed, waiting for manual show")
                             // 알림: 수동 표시를 위해 등록된 리스너 호출
                             onAdLoadedListener?.invoke()
                         }
                     } ?: Log.d(TAG, "show skip: no current activity ref")
                 }
                // 광고 로딩 실패 시에도 콜백 호출
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoading.set(false)
                    // 중앙 상태 업데이트: 로드 실패
                    try { AdController.setAppOpenLastError(error.toString()); AdController.setAppOpenLoading(false) } catch (_: Throwable) {}
                    Log.w(TAG, "onAdFailedToLoad app-open: $error @${System.currentTimeMillis()}")
                    // 403 등 서버 거부 시 지연 재시도 (30초)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (appOpenAd == null && !isLoading.get()) preload(context.applicationContext)
                    }, 30_000)
                    onAdFinishedListener?.invoke()
                }
            }
        )
    }

    /** 정책 비활성화 시 이미 로드된 AppOpen 광고 제거 */
    fun clearLoadedAd() {
        try {
            appOpenAd = null
            isLoading.set(false)
            isShowing.set(false)
            try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLoading(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
            Log.d(TAG, "clearLoadedAd: app-open cleared by policy change")
            // 정책으로 인해 광고가 제거되었음을 StartActivity 등 리스너에 알림
            try { onAdLoadedListener?.invoke() } catch (_: Throwable) {}
        } catch (t: Throwable) {
            Log.w(TAG, "clearLoadedAd failed: $t")
        }
    }

    // 광고 종료/실패 시 호출되는 콜백
    private var onAdFinishedListener: (() -> Unit)? = null
    fun setOnAdFinishedListener(listener: (() -> Unit)?) {
        onAdFinishedListener = listener
    }

    // 광고 로드 완료 시 호출되는 콜백 (StartActivity가 리스너로 등록하여 수동 표시 가능)
    private var onAdLoadedListener: (() -> Unit)? = null
    fun setOnAdLoadedListener(listener: (() -> Unit)?) {
        onAdLoadedListener = listener
    }

    // 광고가 실제로 화면에 나타날 때 호출되는 콜백
    private var onAdShownListener: (() -> Unit)? = null
    fun setOnAdShownListener(listener: (() -> Unit)?) {
        onAdShownListener = listener
    }

    fun showIfAvailable(activity: Activity) {
        Log.d(TAG, "showIfAvailable called @${System.currentTimeMillis()} adLoaded=${appOpenAd!=null} isShowing=${isShowing.get()} activityFinishing=${activity.isFinishing}")
        if (!canShowNow()) {
            Log.d(TAG, "showIfAvailable abort: canShowNow=false")
            preload(activity.applicationContext)
            onAdFinishedListener?.invoke() // abort 시에도 리스너 호출하여 스플래시 해제
            return
        }
        // AppOpen 빈도 제한 확인 (시간/일 기준)
        try {
            if (!kr.sweetapps.alcoholictimer.ads.AdController.canShowAppOpen(activity.applicationContext)) {
                Log.d(TAG, "showIfAvailable abort: AppOpen limit reached by policy")
                onAdFinishedListener?.invoke() // abort 시에도 리스너 호출하여 스플래시 해제
                return
            }
        } catch (_: Throwable) {}
        val ad = appOpenAd
        if (ad == null) {
            Log.d(TAG, "showIfAvailable abort: ad=null -> preload")
            preload(activity.applicationContext)
            onAdFinishedListener?.invoke() // abort 시에도 리스너 호출하여 스플래시 해제
            return
        }
        if (isShowing.get()) { Log.d(TAG, "showIfAvailable abort: already showing"); return }
        if (activity.isFinishing || activity.isDestroyed) { Log.d(TAG, "showIfAvailable abort: activity finishing/destroyed"); return }

        isShowing.set(true)
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent @${System.currentTimeMillis()}")
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
                // Ensure system bars are re-applied shortly after ad shows (SystemUI may alter them)
                try {
                    currentActivityRef?.get()?.let { a ->
                        if (a is kr.sweetapps.alcoholictimer.core.ui.BaseActivity) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try { a.reapplySystemBars() } catch (_: Throwable) {}
                            }, 150)
                        }
                    }
                } catch (_: Throwable) {}
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent @${System.currentTimeMillis()}")
                AdController.setInterstitialShowing(false)
                AdController.setFullScreenAdShowing(false)
                try { AdController.setAppOpenLoaded(false); AdController.setAppOpenLastError(null) } catch (_: Throwable) {}
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
                onAdFinishedListener?.invoke()
                // Re-apply system bars after ad is dismissed to override any SystemUI changes
                try {
                    currentActivityRef?.get()?.let { a ->
                        if (a is kr.sweetapps.alcoholictimer.core.ui.BaseActivity) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try { a.reapplySystemBars() } catch (_: Throwable) {}
                            }, 150)
                        }
                    }
                } catch (_: Throwable) {}
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "onAdFailedToShowFullScreenContent: $adError @${System.currentTimeMillis()}")
                AdController.setInterstitialShowing(false)
                AdController.setFullScreenAdShowing(false)
                try { AdController.setAppOpenLastError(adError.toString()) } catch (_: Throwable) {}
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
                onAdFinishedListener?.invoke()
                // Ensure system bars are re-applied after failure as well
                try {
                    currentActivityRef?.get()?.let { a ->
                        if (a is kr.sweetapps.alcoholictimer.core.ui.BaseActivity) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try { a.reapplySystemBars() } catch (_: Throwable) {}
                            }, 150)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
        Handler(Looper.getMainLooper()).post {
            try { ad.show(activity) } catch (t: Throwable) {
                Log.w(TAG, "show exception: $t @${System.currentTimeMillis()}")
                AdController.setInterstitialShowing(false)
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
            }
        }
    }

    // Lifecycle hooks
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "ProcessLifecycle onStart @${System.currentTimeMillis()}")
        val act = currentActivityRef?.get()
        if (act != null) {
            if (allowAutoShow.get()) showIfAvailable(act) else Log.d(TAG, "onStart: auto-show suppressed")
        } else {
            // Activity 참조가 아직 세팅 전일 수 있으므로 짧은 지연 후 재시도
            Handler(Looper.getMainLooper()).postDelayed({
                currentActivityRef?.get()?.let { if (allowAutoShow.get()) showIfAvailable(it) else Log.d(TAG, "Delayed onStart retry: auto-show suppressed") } ?: Log.d(TAG, "Delayed onStart retry: still no activity")
            }, 300)
        }
    }

    // Track current activity
    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted ${activity::class.java.simpleName} @${System.currentTimeMillis()}")
        currentActivityRef = WeakReference(activity)
        // 포어그라운드 진입 시점에 즉시 시도 (중복 방지는 isShowing/cooldown으로 해결)
        if (allowAutoShow.get()) showIfAvailable(activity) else Log.d(TAG, "onActivityStarted: auto-show suppressed for ${activity::class.java.simpleName}")
    }
    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed ${activity::class.java.simpleName} @${System.currentTimeMillis()}")
        currentActivityRef = WeakReference(activity)
        // 재개 시에도 한 번 더 시도 (조건 불충족 시 내부에서 빠르게 return)
        if (allowAutoShow.get()) showIfAvailable(activity) else Log.d(TAG, "onActivityResumed: auto-show suppressed for ${activity::class.java.simpleName}")
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    // 광고가 현재 표시 중인지 여부 반환
    fun isShowingAd(): Boolean = isShowing.get()

    /** 현재 AppOpen 광고가 로드되어 있는지 여부 */
    fun isLoaded(): Boolean = appOpenAd != null

}
