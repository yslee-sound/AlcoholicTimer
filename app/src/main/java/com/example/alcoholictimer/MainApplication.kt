package com.sweetapps.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sweetapps.alcoholictimer.core.ads.InterstitialAdManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 광고 설정 (에뮬레이터는 자동으로 테스트 기기로 인식됨)
        // 실제 기기에서 테스트하려면 Logcat에서 "Use RequestConfiguration.Builder().setTestDeviceIds()"
        // 메시지를 확인하고 해당 ID를 아래에 추가하세요
        val testDeviceIds = if (BuildConfig.DEBUG) {
            listOf(
                "33BE2250B43518CCDA7DE426D04EE231" // 필요 시 실제 테스트 기기 ID 추가
            )
        } else {
            emptyList()
        }

        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply {
                if (testDeviceIds.isNotEmpty()) {
                    setTestDeviceIds(testDeviceIds)
                }
            }
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this) { initStatus ->
            android.util.Log.d("MainApplication", "MobileAds initialized: $initStatus")
        }
        // 콜드 스타트 게이트 초기화(프로세스 시작 시 1회 노출 허용)
        InterstitialAdManager.resetColdStartGate()
    }
}
