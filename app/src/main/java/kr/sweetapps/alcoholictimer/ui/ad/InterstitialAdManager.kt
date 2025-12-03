@file:Suppress("unused", "UNUSED_PARAMETER")
// [NEW] 클린 아키텍처 리팩토링: InterstitialAdManager를 ui/ad로 이동
package kr.sweetapps.alcoholictimer.ui.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.data.source.remote.AdRequestFactory
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager

object InterstitialAdManager {
    private const val TAG = "InterstitialAdManager"

    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-8420908105703273/2270912481"

    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var isLoading = false
    @Volatile private var isShowing = false

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private fun adUnitId(): String = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    fun preload(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        try {
            MobileAds.initialize(context) { Log.d(TAG, "MobileAds initialized: $it") }
            loadInterstitial(context)
        } catch (t: Throwable) {
            Log.e(TAG, "preload failed", t)
        }
    }

    private fun loadInterstitial(context: Context) {
        if (interstitial != null || isLoading) return
        try {
            isLoading = true
            val adRequest = AdRequestFactory.create(context)
            InterstitialAd.load(context, adUnitId(), adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "onAdLoaded")
                    interstitial = ad
                    isLoading = false
                    ad.onPaidEventListener = com.google.android.gms.ads.OnPaidEventListener { adValue ->
                        // Use AnalyticsManager wrapper so parameter names match our tracking guide
                        try {
                            val value = adValue.valueMicros / 1000000.0
                            val currency = adValue.currencyCode
                            runCatching { AnalyticsManager.logAdRevenue(value, currency, "interstitial") }
                        } catch (_: Throwable) {}
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "onAdFailedToLoad: ${loadAdError.message}")
                    interstitial = null
                    isLoading = false
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "loadInterstitial failed", t)
            isLoading = false
        }
    }

    fun isLoaded(): Boolean = interstitial != null

    fun clearLoadedAd() { interstitial = null }

    fun isShowingAd(): Boolean = isShowing

    // [NEW] 전면 광고를 표시하는 간단한 메서드 (카운트다운 시작 전 사용)
    fun show(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        val ad = interstitial
        if (ad == null) {
            Log.d(TAG, "show: 광고가 로드되지 않음")
            onComplete(false)
            return
        }

        try {
            isShowing = true

            // 배너 숨기기
            try {
                Log.d(TAG, "show: 배너 숨김 시작")
                AdController.hideBannerImmediately("interstitialBeforeShow")
                AdController.setBannerForceHidden(true)
                AdController.setInterstitialShowing(true)
                AdController.setFullScreenAdShowing(true)
            } catch (_: Throwable) {}

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "show: 광고 닫힘")
                    isShowing = false
                    interstitial = null

                    // 배너 복구
                    try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                    try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                    try { AdController.ensureBannerVisible("interstitialDismissed") } catch (_: Throwable) {}

                    onComplete(true)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "show: 광고 표시 실패 - ${adError.message}")
                    isShowing = false
                    interstitial = null

                    // 배너 복구
                    try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                    try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                    try { AdController.ensureBannerVisible("interstitialFailedToShow") } catch (_: Throwable) {}

                    onComplete(false)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "show: 광고 표시 완료")
                    try {
                        AnalyticsManager.logAdImpression("interstitial")
                        // [NEW] 광고 표시 성공 시 쿨타임 시작
                        kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.markInterstitialShown(activity)
                    } catch (_: Throwable) {}
                }

                override fun onAdClicked() {
                    Log.d(TAG, "show: 광고 클릭됨")
                    try { AnalyticsManager.logAdClick("interstitial") } catch (_: Throwable) {}
                }
            }

            ad.show(activity)
            Log.d(TAG, "show: ad.show() 호출 완료")
        } catch (t: Throwable) {
            Log.e(TAG, "show: 예외 발생", t)
            isShowing = false
            interstitial = null

            // 배너 복구
            try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
            try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
            try { AdController.ensureBannerVisible("interstitialShowException") } catch (_: Throwable) {}

            onComplete(false)
        }
    }

    fun addLoadListener(listener: (Boolean) -> Unit) { /* optional: could be implemented */ }

    fun resetColdStartGate() { /* no-op for now */ }

    fun noteAppStart() { Log.d(TAG, "noteAppStart called") }

    fun maybeShowIfEligible(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean {
        try {
            Log.d(TAG, "maybeShowIfEligible called")
            try {
                if (AdController.isFullScreenAdShowing() || AdController.isInterstitialShowingNow()) {
                    Log.d(TAG, "maybeShowIfEligible: another full-screen/interstitial active -> skip show")
                    return false
                }
            } catch (_: Throwable) {}

            try {
                if (!AdController.canShowInterstitial(activity)) {
                    Log.d(TAG, "maybeShowIfEligible: Blocked by policy: interstitial_rate_limit")
                    return false
                }
            } catch (_: Throwable) {}

            val ad = interstitial
            if (ad != null) {
                try {
                    val reserved = AdController.reserveInterstitialSlot()
                    if (!reserved) {
                        Log.d(TAG, "maybeShowIfEligible: reservation failed -> rate limit reached")
                        return false
                    }
                } catch (_: Throwable) {
                    Log.w(TAG, "maybeShowIfEligible: reserveInterstitialSlot threw")
                    return false
                }

                tryShowAd(activity, ad, onDismiss)
                return true
            }

            preload(activity.applicationContext)
            if (!isLoading) loadInterstitial(activity.applicationContext)
            return false
        } catch (t: Throwable) {
            Log.e(TAG, "maybeShowIfEligible failed", t)
            return forceShowDebug(activity, onDismiss)
        }
    }

    private fun tryShowAd(activity: Activity, ad: InterstitialAd, onDismiss: (() -> Unit)?) {
        try {
            isShowing = true

            // ?�� AdMob ?�책 준?? show() 직전??배너�?즉시 ?�겨??겹침 방�?
            try {
                Log.d(TAG, "tryShowAd: hiding banner IMMEDIATELY before show() to prevent overlap")
                AdController.hideBannerImmediately("interstitialBeforeShow")
            } catch (_: Throwable) {}

            // 추�? ?�전?�치
            try { AdController.setBannerForceHidden(true) } catch (_: Throwable) {}
            try { AdController.setInterstitialShowing(true); AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "onAdDismissedFullScreenContent")
                    isShowing = false
                    interstitial = null

                    // ?�� Interstitial 종료 ??배너 복구 (AppOpen�??�일??처리)
                    try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                    try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                    try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
                    try { AdController.ensureBannerVisible("interstitialDismissed") } catch (_: Throwable) {}
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: ${adError.message}")
                    isShowing = false
                    interstitial = null

                    // ?�� Interstitial ?�시 ?�패 ??배너 복구
                    try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                    try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                    try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
                    try { AdController.ensureBannerVisible("interstitialFailedToShow") } catch (_: Throwable) {}
                    try { AdController.unreserveInterstitialSlot() } catch (_: Throwable) {}
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "onAdShowedFullScreenContent")
                    // Analytics: ad impression for interstitial
                    try { runCatching { AnalyticsManager.logAdImpression("interstitial") } } catch (_: Throwable) {}
                }

                override fun onAdClicked() {
                    Log.d(TAG, "onAdClicked")
                    try { runCatching { AnalyticsManager.logAdClick("interstitial") } } catch (_: Throwable) {}
                }
            }
            try {
                if (!AdController.canShowInterstitial(activity)) {
                    Log.d(TAG, "tryShowAd: final policy check denied -> unreserve and skip show")
                    try { AdController.unreserveInterstitialSlot() } catch (_: Throwable) {}
                    try { AdController.setFullScreenAdShowing(false); AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                    // ?�� ?�책?�로 차단??경우 배너 복구
                    try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                    try { AdController.ensureBannerVisible("interstitialPolicyDenied") } catch (_: Throwable) {}
                    isShowing = false
                    interstitial = null
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                    return
                }
            } catch (_: Throwable) { 
                try { AdController.unreserveInterstitialSlot() } catch (_: Throwable) {}
                try { AdController.setFullScreenAdShowing(false); AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                // ?�� ?�외 발생 ??배너 복구
                try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                try { AdController.ensureBannerVisible("interstitialException") } catch (_: Throwable) {}
                isShowing = false
                interstitial = null
                try { onDismiss?.invoke() } catch (_: Throwable) {}
                return
            }

            ad.show(activity)
        } catch (t: Throwable) {
            Log.e(TAG, "show failed", t)
            isShowing = false
            interstitial = null
            // ?�� show ?�패 ??배너 복구
            try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
            try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
            try { AdController.ensureBannerVisible("interstitialShowException") } catch (_: Throwable) {}
            try { AdController.unreserveInterstitialSlot() } catch (_: Throwable) {}
            forceShowDebug(activity, onDismiss)
        }
    }

    fun forceShowDebug(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean {
        return try {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Debug Interstitial")
                .setMessage("This simulates an interstitial ad for testing. Press Close to continue.")
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ ->
                    try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    try { AdController.setInterstitialShowing(false) } catch (_: Throwable) {}
                    // ?�� Debug Interstitial 종료 ?�에??배너 복구
                    try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
                    try { AdController.ensureBannerVisible("debugInterstitialClosed") } catch (_: Throwable) {}
                    try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }
                .show()
            Log.d(TAG, "forceShowDebug: shown")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "forceShowDebug failed", t)
            false
        }
    }
}
