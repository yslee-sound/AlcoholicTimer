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
import com.google.firebase.FirebaseApp
import kr.sweetapps.alcoholictimer.analytics.AnalyticsLogger

class MainApplication : Application() {

    lateinit var umpConsentManager: UmpConsentManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Firebase 초기화: google-services.json이 있으면 자동으로 구성되지만
        // 명시적으로 초기화하여 Firebase API 사용 시 안정성 확보
        try { FirebaseApp.initializeApp(this) } catch (_: Throwable) {}
        // Analytics 래퍼 초기화 (Firebase 초기화 이후에 호출)
        try { AnalyticsLogger.initialize(this) } catch (_: Throwable) {}
        umpConsentManager = UmpConsentManager(this)

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

        val testDeviceIds = (
            listOf("03BB67FC09646C26F9D03C998F4FC6C6") +
            if (BuildConfig.DEBUG) listOf("33BE2250B43518CCDA7DE426D04EE231") else emptyList()
        ).distinct()

        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply { if (testDeviceIds.isNotEmpty()) setTestDeviceIds(testDeviceIds) }
            .build()
        MobileAds.setRequestConfiguration(config)

        // 앱 시작 시각 기록: 콜드 스타트 직후 초기 보호 창 동작
        InterstitialAdManager.noteAppStart()

        // App Open Ad 초기화
        AppOpenAdManager.initialize(this)
        // AppOpen은 initialize 이후에 앱 시작 시각을 기록해야 함
        AppOpenAdManager.noteAppStart()
        // Preload app-open early to increase chance it's ready at splash time
        runCatching { AppOpenAdManager.preload(this) }

        // Ensure AppOpen ad is shown when app returns from background -> foreground
        // Track activity start/resume to detect foreground transition and attempt safe show
        val mainHandler = Handler(Looper.getMainLooper())
         var startedCount = 0
         var isFirstActivityStart = true
         var suppressForegroundAfterShow = false
         var pendingForegroundActivity: Activity? = null

        // If an AppOpen ad finishes loading while app is in foreground and we have a pending activity,
        // attempt to show it then. Do not clear pendingForegroundActivity until ad is shown.
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener {
             try {
                 mainHandler.post {
                     try {
                        val act = pendingForegroundActivity
                        if (act != null) {
                            // if an ad was just shown recently, skip to avoid immediate re-show
                            if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.wasRecentlyShown()) {
                                android.util.Log.d("MainApplication", "onAdLoaded listener -> recent ad shown; skipping show for ${act.javaClass.simpleName}")
                                return@post
                            }
                            android.util.Log.d("MainApplication", "onAdLoaded listener -> attempting show for ${act.javaClass.simpleName}")
                            val shown = runCatching { kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.showIfAvailable(act) }.getOrDefault(false)
                            android.util.Log.d("MainApplication", "onAdLoaded listener -> showIfAvailable returned=$shown")
                            if (shown) pendingForegroundActivity = null
                        }
                     } catch (_: Throwable) {}
                 }
              } catch (_: Throwable) {}
          }

        // Ensure that when an AppOpen ad finishes (dismissed), any pending scheduled foreground show is cleared
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener {
            try {
                mainHandler.post { try { pendingForegroundActivity = null; android.util.Log.d("MainApplication", "onAdFinished -> cleared pendingForegroundActivity") } catch (_: Throwable) {} }
            } catch (_: Throwable) {}
        }

        // When an AppOpen ad is shown, suppress the next foreground-triggered show until app backgrounds
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdShownListener {
            try {
                mainHandler.post {
                    try {
                        suppressForegroundAfterShow = true
                        android.util.Log.d("MainApplication", "onAdShown -> suppressForegroundAfterShow=true")
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
             override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
             override fun onActivityStarted(activity: Activity) {
                 try {
                     startedCount++
                     // transition from 0 -> 1 means app moved to foreground
                     if (startedCount == 1) {
                         // If this is the very first activity start (cold start), skip scheduling here.
                         if (isFirstActivityStart) {
                             isFirstActivityStart = false
                             android.util.Log.d("MainApplication", "App moved to foreground (cold start) -> skipping AppOpen schedule for ${activity.javaClass.simpleName}")
                         } else {
                             if (suppressForegroundAfterShow) {
                                android.util.Log.d("MainApplication", "App moved to foreground -> suppressed due to recent ad; skipping schedule for ${activity.javaClass.simpleName}")
                            } else {
                             // avoid scheduling when ad was just shown (lifecycle triggered by ad)
                             val cls = activity.javaClass.simpleName
                             if (cls == "SplashScreen" || cls == "AppOpenOverlayActivity") {
                                 android.util.Log.d("MainApplication", "onActivityStarted: ignoring overlay/splash activity start for $cls")
                             } else if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.wasRecentlyShown()) {
                                 android.util.Log.d("MainApplication", "App moved to foreground -> recent ad dismissed; skipping schedule for ${activity.javaClass.simpleName}")
                             } else {
                                 android.util.Log.d("MainApplication", "App moved to foreground -> scheduling AppOpen attempt for ${activity.javaClass.simpleName}")
                                 pendingForegroundActivity = activity
                             }
                            }
                         }
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
                             // keep pendingForegroundActivity intact so ad can be shown later if appropriate
                             return
                         }

                         mainHandler.post {
                             try {
                                 // guard against immediately re-showing when ad was just dismissed
                                 if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.wasRecentlyShown()) {
                                     android.util.Log.d("MainApplication", "onActivityResumed -> recent ad shown; skipping show for ${activity.javaClass.simpleName}")
                                     return@post
                                 }
                                 android.util.Log.d("MainApplication", "Attempting AppOpenAdManager.showIfAvailable for ${activity.javaClass.simpleName}")
                                 val shown = runCatching { AppOpenAdManager.showIfAvailable(activity) }.getOrDefault(false)
                                 android.util.Log.d("MainApplication", "AppOpen showIfAvailable returned=$shown")
                                 if (shown) pendingForegroundActivity = null
                             } catch (t: Throwable) {
                                 android.util.Log.w("MainApplication", "Foreground AppOpen attempt failed: $t")
                                 // keep pendingForegroundActivity so onAdLoaded listener can retry
                             }
                         }
                     } else {
                         // no-op: not the pending activity
                     }
                 } catch (t: Throwable) { android.util.Log.w("MainApplication", "onActivityResumed error: $t") }
             }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                try {
                    startedCount = (startedCount - 1).coerceAtLeast(0)
                    // If app fully backgrounded, clear suppression so future foregrounds can show again
                    if (startedCount == 0) {
                        if (suppressForegroundAfterShow) android.util.Log.d("MainApplication", "App fully backgrounded -> clearing suppressForegroundAfterShow")
                        suppressForegroundAfterShow = false
                    }
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
