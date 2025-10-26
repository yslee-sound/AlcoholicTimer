package com.sweetapps.alcoholictimer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sweetapps.alcoholictimer.core.ads.InterstitialAdManager
import com.sweetapps.alcoholictimer.core.ads.AppOpenAdManager

class MainApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()


        // ⚠️ 광고 설정 - 본인 광고 클릭 방지 매우 중요!
        //
        // 📱 본인 기기를 테스트 기기로 등록하는 방법:
        // 1. 앱 실행 후 Logcat에서 "Use RequestConfiguration.Builder().setTestDeviceIds()"
        //    메시지를 찾으세요. 예:
        //    I/Ads: Use RequestConfiguration.Builder()
        //          .setTestDeviceIds(Arrays.asList("ABC123DEF456"))
        //
        // 2. 해당 ID를 아래 리스트에 추가하세요:
        //    "ABC123DEF456"  // 본인 기기 ID
        //
        // 3. 본인 기기를 등록하면:
        //    ✅ 실수로 광고를 클릭해도 안전 (계정 정지 방지)
        //    ✅ 앱을 자유롭게 사용 가능
        //    ✅ 테스트 광고만 표시됨
        //
        // ⚠️ 주의: 본인 광고를 절대 클릭하지 마세요!
        //    → AdMob 계정 영구 정지 위험
        //    → 상세 가이드: docs/AD_SELF_CLICK_WARNING.md

        val testDeviceIds = if (BuildConfig.DEBUG) {
            // 디버그 빌드: 테스트 기기만
            listOf(
                "33BE2250B43518CCDA7DE426D04EE231",  // 샘플 테스트 기기
                "03BB67FC09646C26F9D03C998F4FC6C6"   // 본인 기기 (Logcat 확인)
            )
        } else {
            // ✅ 릴리즈 빌드: 본인 기기는 테스트 기기로 등록 (강력 권장!)
            //
            // 이유:
            //   1. 계정 안전: 본인이 실수로 광고 클릭해도 계정 정지 위험 없음
            //   2. Google 권장: AdMob 정책에서 개발자는 테스트 기기로 등록하라고 명시
            //   3. 수익 영향 미미: 본인 1명의 광고만 없어짐 (전체 수익의 0.001% 미만)
            //   4. 안전한 사용: 앱을 자유롭게 사용하면서 버그 확인 가능
            //
            // 결과:
            //   - 본인: 테스트 광고만 표시 (클릭해도 안전)
            //   - 다른 모든 사용자: 진짜 광고 표시 (수익 정상 발생)
            //
            // ⚠️ 만약 본인 기기를 등록하지 않으면:
            //   - 본인도 진짜 광고를 보게 됨
            //   - 실수로 클릭 시 계정 영구 정지 위험!
            //   - 본인은 앱을 사용할 수 없게 됨
            listOf(
                "03BB67FC09646C26F9D03C998F4FC6C6"   // 본인 기기 ID (Logcat 확인)
            )
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

            // MobileAds 초기화 완료 후 앱 오프닝 광고 로드
            appOpenAdManager.loadAd()
        }

        // 앱 오프닝 광고 매니저 초기화
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.resetColdStart()

        // 콜드 스타트 게이트 초기화(프로세스 시작 시 1회 노출 허용)
        InterstitialAdManager.resetColdStartGate()
    }
}
