package com.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val TAG = "AdmobBanner"

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp), // 최소 높이 보장(보다 유연한 adaptive size로 실제 높이는 달라질 수 있음)
        factory = { context ->
            AdView(context).apply {
                // ViewGroup params를 명시적으로 설정해 Compose 측에서 0 높이로 측정되는 것을 방지
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // Sample Ad unit ID (테스트 전용)
                setAdUnitId("ca-app-pub-3940256099942544/6300978111")

                // 화면 너비 기준으로 adaptive 배너 사이즈 계산 및 한 번만 설정
                try {
                    val density = context.resources.displayMetrics.density
                    val adWidth = (context.resources.displayMetrics.widthPixels / density).toInt()
                    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                    setAdSize(adaptiveSize)
                } catch (t: Throwable) {
                    // fallback
                    try {
                        setAdSize(AdSize.BANNER)
                    } catch (ignored: Throwable) {
                        Log.w(TAG, "Failed to set ad size in factory: ${'$'}{ignored.message}")
                    }
                }

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Banner onAdLoaded")
                    }
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        Log.w(TAG, "Banner onAdFailedToLoad code=${'$'}{error.code} message=${'$'}{error.message}")
                    }
                    override fun onAdOpened() { Log.d(TAG, "Banner onAdOpened") }
                    override fun onAdClicked() { Log.d(TAG, "Banner onAdClicked") }
                    override fun onAdClosed() { Log.d(TAG, "Banner onAdClosed") }
                    override fun onAdImpression() { Log.d(TAG, "Banner onAdImpression") }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // setAdSize는 한 번만 허용되므로 update 블록에서는 직접 호출하지 않습니다.
            // 회전 등으로 사이즈가 크게 달라졌을 때 재로딩이 필요하면 여기서 로직을 추가할 수 있습니다.
            try {
                // 간단한 체크: 현재 adView.getAdSize()가 null인지 확인하고 로그 남김
                val current = try { adView.adSize } catch (t: Throwable) { null }
                Log.d(TAG, "AdView update called; currentAdSize=${'$'}{current}")
            } catch (t: Throwable) {
                Log.w(TAG, "Error during adView update: ${'$'}{t.message}")
            }
        }
    )
}
