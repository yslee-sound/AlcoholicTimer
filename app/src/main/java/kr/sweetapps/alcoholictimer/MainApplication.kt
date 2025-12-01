package kr.sweetapps.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.AdController
import kr.sweetapps.alcoholictimer.ads.AppOpenAdManager
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import kr.sweetapps.alcoholictimer.consent.UmpConsentManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager as AdsUmpConsentManager
import com.google.firebase.FirebaseApp
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import java.lang.ref.WeakReference

class MainApplication : Application() {

    lateinit var umpConsentManager: UmpConsentManager
        private set

    companion object {
        // Avoid holding a strong reference to Activity in a static field to prevent leaks.
        @Volatile
        private var currentActivityRef: WeakReference<Activity>? = null

        var currentActivity: Activity?
            get() = currentActivityRef?.get()
            set(value) { currentActivityRef = value?.let { WeakReference(it) } }
    }

    override fun onCreate() {
        super.onCreate()

        // 📊 타이밍 진단: 앱 시작 시각 기록
        kr.sweetapps.alcoholictimer.ads.AdTimingLogger.logAppStart()

        // Firebase 초기화: google-services.json이 있으면 자동으로 구성되지만
        // 명시적으로 초기화하여 Firebase API 사용 시 안정성 확보
        try { FirebaseApp.initializeApp(this) } catch (_: Throwable) {}
        // Analytics 래퍼 초기화 (Firebase 초기화 이후에 호출)
        try { AnalyticsManager.initialize(this) } catch (_: Throwable) {}
        umpConsentManager = UmpConsentManager(this)

        // Initialize ads-side proxy so it loads persisted prefs and subscribes to primary manager as needed
        try { AdsUmpConsentManager.initialize(this) } catch (_: Throwable) {}

        // AdController 초기화 (Supabase 기반 광고 제어)
        AdController.initialize(this)

        // When policy fetch completes and interstitials are enabled, ensure interstitial is preloaded.
        // Use AdController's policy listener to react when the remote policy is available.
        AdController.addPolicyFetchListener { policy ->
            try {
                if (policy?.adInterstitialEnabled == true) {
                    android.util.Log.d("MainApplication", "Policy enables interstitial -> preloading interstitial")
                    // preload must run on main/UI thread (InterstitialAd.load enforces main thread)
                    Handler(Looper.getMainLooper()).post {
                        try { InterstitialAdManager.preload(applicationContext) } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}
        }

        // UMP / AdMob test device hashes: primary list comes from BuildConfig (injected from local.properties for debug)
        val configuredHashes = try { kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH } catch (_: Throwable) { "" }
        val fromConfig = configuredHashes.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val debugDefault = if (BuildConfig.DEBUG) listOf("33BE2250B43518CCDA7DE426D04EE231") else emptyList()
        val testDeviceIds = (fromConfig + debugDefault).distinct()

        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply { if (testDeviceIds.isNotEmpty()) setTestDeviceIds(testDeviceIds) }
            .build()
        MobileAds.setRequestConfiguration(config)

        // 앱 시작 시각 기록: 콜드 스타트 직후 초기 보호 창 동작
        InterstitialAdManager.noteAppStart()

        // App Open Ad 초기화 (메인앱에서 lifecycle 스케줄링을 담당하므로 내부 등록은 비활성화)
        AppOpenAdManager.initialize(this, registerLifecycle = true)
        AppOpenAdManager.noteAppStart()
        // Preload app-open early to increase chance it's ready at splash time
        runCatching { AppOpenAdManager.preload(this) }

        var suppressForegroundAfterShow = false

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                // track current activity for debug flows
                try { currentActivity = activity } catch (_: Throwable) {}
                try {
                    // Only invoke primary consent here if activity is not SplashScreen/overlay
                    try {
                        val clsName = activity.javaClass.simpleName
                        if (clsName == "SplashScreen" || clsName == "AppOpenOverlayActivity" || clsName == "MainActivity") {
                            android.util.Log.d("MainApplication", "App moved to foreground -> activity $clsName handles consent; skipping primary gather here")
                        } else {
                            android.util.Log.d("MainApplication", "App moved to foreground -> invoking primary consent gather (consent.UmpConsentManager)")
                            umpConsentManager.gatherConsent(activity) { canRequest ->
                                android.util.Log.d("MainApplication", "primary consent gather finished -> canRequestAds=$canRequest")
                                // Trigger ads-side request to sync state immediately after primary consent resolved
                                try { AdsUmpConsentManager.requestAndLoadIfRequired(activity) { _ -> } } catch (_: Throwable) {}
                            }
                        }
                    } catch (_: Throwable) {}

                    // If this is the very first activity start (cold start), skip scheduling here.
                } catch (t: Throwable) { android.util.Log.w("MainApplication", "onActivityStarted error: $t") }
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                try {
                    // If app fully backgrounded, clear suppression so future foregrounds can show again
                    if (suppressForegroundAfterShow) android.util.Log.d("MainApplication", "App fully backgrounded -> clearing suppressForegroundAfterShow")
                    suppressForegroundAfterShow = false
                    try { currentActivity = null } catch (_: Throwable) {}
                } catch (_: Throwable) {}
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // Debug-time: ensure system bar appearance is re-applied after activity resume
         if (BuildConfig.DEBUG) {
             registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                 override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                 override fun onActivityStarted(activity: Activity) {}
                 override fun onActivityResumed(activity: Activity) {
                     try {
                         if (activity is kr.sweetapps.alcoholictimer.core.ui.BaseActivity) {
                             activity.reapplySystemBars()
                         }
                     } catch (_: Throwable) {}
                 }
                 override fun onActivityPaused(activity: Activity) {}
                 override fun onActivityStopped(activity: Activity) {}
                 override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                 override fun onActivityDestroyed(activity: Activity) {}
             })
         }
     }
 }
