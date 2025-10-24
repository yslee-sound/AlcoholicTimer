package com.example.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.example.alcoholictimer.core.ads.InterstitialAdManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this) {}
        // 콜드 스타트 게이트 초기화(프로세스 시작 시 1회 노출 허용)
        InterstitialAdManager.resetColdStartGate()
    }
}
