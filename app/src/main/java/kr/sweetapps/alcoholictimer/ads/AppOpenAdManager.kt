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

    private fun currentUnitId(): String {
        // TODO: 실제 단위 ID 연결 시 교체. 현재는 테스트 ID로 동작
        return GOOGLE_TEST_APP_OPEN_ID
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

    fun initialize(application: Application) {
        if (app != null) return
        app = application
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        preload(application.applicationContext)
        Log.d(TAG, "✅ Initialized")
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

    fun preload(context: Context) {
        if (isLoading.get()) return
        if (appOpenAd != null) return
        isLoading.set(true)
        val request = AdRequest.Builder().build()
        val unitId = currentUnitId()
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
                    Log.d(TAG, "onAdLoaded app-open")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoading.set(false)
                    Log.w(TAG, "onAdFailedToLoad app-open: $error")
                }
            }
        )
    }

    private fun showIfAvailable(activity: Activity) {
        if (!canShowNow()) {
            preload(activity.applicationContext)
            return
        }
        val ad = appOpenAd
        if (ad == null) {
            Log.d(TAG, "AppOpen not loaded; preload")
            preload(activity.applicationContext)
            return
        }
        if (isShowing.get()) {
            Log.d(TAG, "AppOpen is already showing")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return

        isShowing.set(true)
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "AppOpen onAdShowed")
                // 배너 겹침 방지: 전면 상태로 간주
                AdController.setInterstitialShowing(true)
                lastShownAt = System.currentTimeMillis()
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "AppOpen onAdDismissed")
                AdController.setInterstitialShowing(false)
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "AppOpen onAdFailedToShow: $adError")
                AdController.setInterstitialShowing(false)
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
            }
        }
        Handler(Looper.getMainLooper()).post {
            try { ad.show(activity) } catch (t: Throwable) {
                Log.w(TAG, "AppOpen show failed: $t")
                AdController.setInterstitialShowing(false)
                appOpenAd = null
                isShowing.set(false)
                preload(activity.applicationContext)
            }
        }
    }

    // Lifecycle hooks
    override fun onStart(owner: LifecycleOwner) {
        val act = currentActivityRef?.get()
        if (act != null) {
            showIfAvailable(act)
        } else {
            // Activity 참조가 아직 세팅 전일 수 있으므로 짧은 지연 후 재시도
            Handler(Looper.getMainLooper()).postDelayed({
                currentActivityRef?.get()?.let { showIfAvailable(it) }
            }, 300)
        }
    }

    // Track current activity
    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        // 포어그라운드 진입 시점에 즉시 시도 (중복 방지는 isShowing/cooldown으로 해결)
        showIfAvailable(activity)
    }
    override fun onActivityResumed(activity: Activity) {
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
