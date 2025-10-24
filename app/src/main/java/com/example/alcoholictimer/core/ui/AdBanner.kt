package com.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.ump.UserMessagingPlatform
import com.sweetapps.alcoholictimer.BuildConfig

private const val TAG = "AdmobBanner"

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp), // Adaptive 높이는 기기/회전에 따라 달라짐. 최소 보장만 둠.
        factory = { context ->
            AdView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // BuildConfig에서 읽고, 비었거나 플레이스홀더면 테스트 ID로 폴백
                val resolvedUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                val unitId = if (resolvedUnitId.isNullOrBlank() || resolvedUnitId.contains("REPLACE_WITH_REAL_BANNER")) {
                    "ca-app-pub-3940256099942544/6300978111" // Google 테스트 배너 ID
                } else resolvedUnitId
                setAdUnitId(unitId)

                // 화면 너비 기준으로 adaptive 배너 사이즈 계산(회전 시에는 재생성 타이밍에 다시 계산 권장)
                try {
                    val density = context.resources.displayMetrics.density
                    val adWidth = (context.resources.displayMetrics.widthPixels / density).toInt()
                    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                    setAdSize(adaptiveSize)
                } catch (t: Throwable) {
                    try { setAdSize(AdSize.BANNER) } catch (ignored: Throwable) {
                        Log.w(TAG, "Failed to set ad size in factory: ${ignored.message}")
                    }
                }

                adListener = object : AdListener() {
                    override fun onAdLoaded() { Log.d(TAG, "Banner onAdLoaded") }
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        Log.w(TAG, "Banner onAdFailedToLoad code=${error.code} message=${error.message}")
                    }
                    override fun onAdOpened() { Log.d(TAG, "Banner onAdOpened") }
                    override fun onAdClicked() { Log.d(TAG, "Banner onAdClicked") }
                    override fun onAdClosed() { Log.d(TAG, "Banner onAdClosed") }
                    override fun onAdImpression() { Log.d(TAG, "Banner onAdImpression") }
                }

                // UMP 동의 상태 확인 후에만 로드
                val consentInfo = try { UserMessagingPlatform.getConsentInformation(context) } catch (_: Throwable) { null }
                val canRequest = consentInfo?.canRequestAds() == true
                if (canRequest) {
                    loadAd(AdRequest.Builder().build())
                } else {
                    Log.d(TAG, "Consent not granted or not required yet. Skipping banner load.")
                }
            }
        },
        update = { adView ->
            // 필요 시 회전 등에서 크기 재계산/재로딩 로직을 여기에 둘 수 있음.
            try {
                val current = try { adView.adSize } catch (_: Throwable) { null }
                Log.d(TAG, "AdView update; currentAdSize=$current")
            } catch (_: Throwable) { }
        }
    )
}
