package kr.sweetapps.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.AdController

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // AdController 초기화 (Supabase 기반 광고 제어)
        AdController.initialize(this)

        // 릴리즈에서도 본인 기기는 항상 테스트로 유지 + 디버그에서만 샘플 테스트 ID 추가
        // 중복 가능성 대비하여 distinct() 적용
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

        InterstitialAdManager.resetColdStartGate()
    }
}
