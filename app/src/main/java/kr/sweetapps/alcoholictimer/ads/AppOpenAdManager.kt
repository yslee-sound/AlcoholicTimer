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
    private const val PROD_APP_OPEN_ID = "여기에_운영_광고_단위_ID_입력" // 실제 운영 광고 단위 ID로 교체

    private fun currentUnitId(): String {
        return BuildConfig.ADMOB_APP_OPEN_UNIT_ID
    }

    private var app: Application? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    private var appOpenAd: AppOpenAd? = null
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)

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

    fun initialize(application: Application) {
        if (app != null) return
        app = application
        Log.d(TAG, "initialize: application set. starting lifecycle hooks")
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // 초기 preload는 UMP 동의 전이므로 스킵, UMP 완료 후 onConsentUpdated에서 시작
        Log.d(TAG, "✅ Initialized (preload deferred until UMP consent)")
        healthCheckHandler = Handler(Looper.getMainLooper())
        healthCheckHandler?.postDelayed(healthRunnable, 30_000)
    }

    private fun canShowNow(): Boolean {
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
        val unitId = currentUnitId()
        Log.d(TAG, "preload start: unitId=$unitId @${System.currentTimeMillis()}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            unitId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoading.set(false)
                    lastLoadedAt = System.currentTimeMillis()
                    Log.d(TAG, "onAdLoaded app-open @${lastLoadedAt}")
                    // 포어그라운드 상태라면 즉시 표시 시도
                    currentActivityRef?.get()?.let { act ->
                        showIfAvailable(act)
                    } ?: Log.d(TAG, "show skip: no current activity ref")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoading.set(false)
                    Log.w(TAG, "onAdFailedToLoad app-open: $error @${System.currentTimeMillis()}")
                    // 403 등 서버 거부 시 지연 재시도 (30초)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (appOpenAd == null && !isLoading.get()) preload(context.applicationContext)
                    }, 30_000)
                }
            }
        )
    }

    fun showIfAvailable(activity: Activity) {
        Log.d(TAG, "showIfAvailable called @${System.currentTimeMillis()} adLoaded=${appOpenAd!=null} isShowing=${isShowing.get()} activityFinishing=${activity.isFinishing}")
        if (!canShowNow()) {
            Log.d(TAG, "showIfAvailable abort: canShowNow=false")
            preload(activity.applicationContext)
            return
        }
        val ad = appOpenAd
        if (ad == null) {
            Log.d(TAG, "showIfAvailable abort: ad=null -> preload")
            preload(activity.applicationContext)
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
                lastShownAt = System.currentTimeMillis()
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent @${System.currentTimeMillis()}")
                AdController.setInterstitialShowing(false)
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "onAdFailedToShowFullScreenContent: $adError @${System.currentTimeMillis()}")
                AdController.setInterstitialShowing(false)
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
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
            showIfAvailable(act)
        } else {
            // Activity 참조가 아직 세팅 전일 수 있으므로 짧은 지연 후 재시도
            Handler(Looper.getMainLooper()).postDelayed({
                currentActivityRef?.get()?.let { showIfAvailable(it) } ?: Log.d(TAG, "Delayed onStart retry: still no activity")
            }, 300)
        }
    }

    // Track current activity
    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted ${activity::class.java.simpleName} @${System.currentTimeMillis()}")
        currentActivityRef = WeakReference(activity)
        // 포어그라운드 진입 시점에 즉시 시도 (중복 방지는 isShowing/cooldown으로 해결)
        showIfAvailable(activity)
    }
    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed ${activity::class.java.simpleName} @${System.currentTimeMillis()}")
        currentActivityRef = WeakReference(activity)
        // 재개 시에도 한 번 더 시도 (조건 불충족 시 내부에서 빠르게 return)
        showIfAvailable(activity)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
