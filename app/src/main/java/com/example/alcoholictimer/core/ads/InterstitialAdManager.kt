package com.example.alcoholictimer.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import java.util.concurrent.atomic.AtomicBoolean

object InterstitialAdManager {
    private const val TAG = "InterstitialAdManager"

    // Google's sample interstitial ad unit ID for testing
    private const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)
    private val hasShownThisColdStart = AtomicBoolean(false)

    fun preload(context: Context) {
        if (isLoading.get()) return
        if (interstitialAd != null) return
        isLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading.set(false)
                    Log.d(TAG, "onAdLoaded")
                    hookFullScreenCallbacks()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading.set(false)
                    Log.w(TAG, "onAdFailedToLoad: ${'$'}error")
                }
            }
        )
    }

    private fun hookFullScreenCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent")
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent")
                interstitialAd = null
                hasShownThisColdStart.set(true)
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "onAdFailedToShowFullScreenContent: ${'$'}adError")
                interstitialAd = null
            }
        }
    }

    fun maybeShowIfEligible(activity: Activity) {
        // 정책 가드: 콜드 스타트당 1회만 허용(프리미엄 예외 없음)
        if (hasShownThisColdStart.get()) {
            Log.d(TAG, "Blocked: already shown this cold start")
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Blocked: ad not loaded")
            return
        }
        // Activity 유효성 확인 후 UI 스레드로 표시
        if (activity.isFinishing || activity.isDestroyed) {
            Log.d(TAG, "Blocked: invalid activity state")
            return
        }
        Handler(Looper.getMainLooper()).post {
            try {
                ad.show(activity)
            } catch (t: Throwable) {
                Log.w(TAG, "Show failed: ${'$'}t")
            } finally {
                interstitialAd = null
            }
        }
    }

    fun resetColdStartGate() {
        hasShownThisColdStart.set(false)
    }
}

