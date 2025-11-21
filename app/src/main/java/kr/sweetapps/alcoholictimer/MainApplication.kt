package kr.sweetapps.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.AdController
import kr.sweetapps.alcoholictimer.ads.AppOpenAdManager
import android.app.Activity
import android.os.Handler
import android.os.Looper

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // AdController 초기화 (Supabase 기반 광고 제어)
        AdController.initialize(this)

        // When policy fetch completes and interstitials are enabled, ensure interstitial is preloaded.
        AdController.addPolicyFetchListener { policy: AdController.Policy? ->
            try {
                if (policy?.adInterstitialEnabled == true) {
                    android.util.Log.d("MainApplication", "Policy enables interstitial -> preloading interstitial")
                    kr.sweetapps.alcoholictimer.ads.InterstitialAdManager.preload(applicationContext)
                }
            } catch (_: Throwable) {}
        }

        val testDeviceIds = (
            listOf("03BB67FC09646C26F9D03C998F4FC6C6") +
            if (BuildConfig.DEBUG) listOf("33BE2250B43518CCDA7DE426D04EE231") else emptyList()
        ).distinct()

        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply { if (testDeviceIds.isNotEmpty()) setTestDeviceIds(testDeviceIds) }
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(this) { initStatus ->
            android.util.Log.d("MainApplication", "MobileAds initialized: $initStatus")
        }

        // 앱 시작 시각 기록: 콜드 스타트 직후 초기 보호 창 동작
        kr.sweetapps.alcoholictimer.ads.InterstitialAdManager.noteAppStart()

        // AppOpen도 앱 시작 시각을 기록하여 'startup window' 검사에 사용
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.noteAppStart()

        // App Open Ad 초기화
        AppOpenAdManager.initialize(this)
        // Preload app-open early to increase chance it's ready at splash time
        runCatching { kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.preload(this) }

        // Ensure AppOpen ad is shown when app returns from background -> foreground
        // Track activity start/resume to detect foreground transition and attempt safe show
        val mainHandler = Handler(Looper.getMainLooper())
        var startedCount = 0
        var pendingForegroundActivity: Activity? = null

        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                try {
                    startedCount++
                    // transition from 0 -> 1 means app moved to foreground
                    if (startedCount == 1) {
                        android.util.Log.d("MainApplication", "App moved to foreground -> scheduling AppOpen attempt for ${activity.javaClass.simpleName}")
                        pendingForegroundActivity = activity
                    }
                } catch (t: Throwable) { android.util.Log.w("MainApplication", "onActivityStarted error: $t") }
            }
            override fun onActivityResumed(activity: Activity) {
                try {
                    // Only attempt show when this resumed activity matches the pending one
                    if (pendingForegroundActivity == activity) {
                        // Avoid showing on SplashScreen or overlay activity (they handle their own flows)
                        val cls = activity.javaClass.simpleName
                        if (cls == "SplashScreen" || cls == "AppOpenOverlayActivity") {
                            android.util.Log.d("MainApplication", "Skipping AppOpen show on $cls (handled elsewhere)")
                            pendingForegroundActivity = null
                            return
                        }

                        mainHandler.post {
                            try {
                                android.util.Log.d("MainApplication", "Attempting AppOpenAdManager.showIfAvailable for ${activity.javaClass.simpleName}")
                                val shown = runCatching { AppOpenAdManager.showIfAvailable(activity) }.getOrDefault(false)
                                android.util.Log.d("MainApplication", "AppOpen showIfAvailable returned=$shown")
                                pendingForegroundActivity = null
                            } catch (t: Throwable) {
                                android.util.Log.w("MainApplication", "Foreground AppOpen attempt failed: $t")
                                pendingForegroundActivity = null
                            }
                        }
                    }
                } catch (t: Throwable) { android.util.Log.w("MainApplication", "onActivityResumed error: $t") }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                try { startedCount = (startedCount - 1).coerceAtLeast(0) } catch (_: Throwable) {}
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // Debug-time: ensure system bar appearance is re-applied after activity resume
         if (BuildConfig.DEBUG) {
             registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                 override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                 override fun onActivityStarted(activity: android.app.Activity) {}
                 override fun onActivityResumed(activity: android.app.Activity) {
                     try {
                         if (activity is kr.sweetapps.alcoholictimer.core.ui.BaseActivity) {
                             activity.reapplySystemBars()
                         }
                     } catch (_: Throwable) {}
                 }
                 override fun onActivityPaused(activity: android.app.Activity) {}
                 override fun onActivityStopped(activity: android.app.Activity) {}
                 override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                 override fun onActivityDestroyed(activity: android.app.Activity) {}
             })
         }
     }
 }
