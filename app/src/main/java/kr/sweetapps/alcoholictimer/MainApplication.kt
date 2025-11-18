package kr.sweetapps.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.AdController
import kr.sweetapps.alcoholictimer.ads.AppOpenAdManager

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // AdController 초기화 (Supabase 기반 광고 제어)
        AdController.initialize(this)

        // When policy fetch completes and interstitials are enabled, ensure interstitial is preloaded.
        AdController.addPolicyFetchListener { policy ->
            try {
                if (policy?.adInterstitialEnabled == true) {
                    android.util.Log.d("MainApplication", "Policy enables interstitial -> preloading interstitial")
                    InterstitialAdManager.preload(applicationContext)
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
        InterstitialAdManager.noteAppStart()

        // AppOpen도 앱 시작 시각을 기록하여 'startup window' 검사에 사용
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.noteAppStart()

        // App Open Ad 초기화
        AppOpenAdManager.initialize(this)

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
