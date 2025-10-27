package com.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.ump.UserMessagingPlatform
import com.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.delay

private const val TAG = "AdmobBanner"

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    // 배너 로드 시도 여부 (동의가 늦게 true가 되는 경우 update에서 최초 1회 로드)
    var hasRequested by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LayoutConstants.BANNER_MIN_HEIGHT),
        factory = { context ->
            AdView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                val resolvedUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                val unitId = if (resolvedUnitId.isNullOrBlank() || resolvedUnitId.contains("REPLACE_WITH_REAL_BANNER")) {
                    "ca-app-pub-3940256099942544/6300978111"
                } else resolvedUnitId
                setAdUnitId(unitId)

                // 화면 풀폭 기준으로 Adaptive 사이즈 계산(상한/좌우 패딩 제거)
                try {
                    val dm = context.resources.displayMetrics
                    val density = dm.density
                    val screenWidthDp = (dm.widthPixels / density).toInt()
                    val adWidthDp = screenWidthDp.coerceAtLeast(0)
                    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
                    setAdSize(adaptiveSize)
                } catch (_: Throwable) {
                    try { setAdSize(AdSize.BANNER) } catch (ignored: Throwable) {
                        Log.w(TAG, "Failed to set ad size in factory: ${ignored.message}")
                    }
                }

                adListener = object : AdListener() {
                    override fun onAdLoaded() { Log.d(TAG, "Banner onAdLoaded") }
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        Log.w(TAG, "Banner onAdFailedToLoad code=${error.code} message=${error.message}")
                        // 실패 시 재시도를 무한히 하지 않기 위해 hasRequested는 그대로 유지
                    }
                    override fun onAdOpened() { Log.d(TAG, "Banner onAdOpened") }
                    override fun onAdClicked() { Log.d(TAG, "Banner onAdClicked") }
                    override fun onAdClosed() { Log.d(TAG, "Banner onAdClosed") }
                    override fun onAdImpression() { Log.d(TAG, "Banner onAdImpression") }
                }

                // 공장 단계에서는 로드를 수행하지 않음 (동의가 늦게 확보될 수 있으므로 update/Effect에서 처리)
                adViewRef = this
            }
        },
        update = { adView ->
            // 필요 시 회전 등에서 크기 재계산/재로딩 로직을 여기에 둘 수 있음.
            try {
                val current = try { adView.adSize } catch (_: Throwable) { null }
                Log.d(TAG, "AdView update; currentAdSize=$current")
            } catch (_: Throwable) { }

            // UMP 동의 상태가 true가 되는 시점에 최초 1회 로드를 시도 (업데이트로 들어오는 경우)
            val consentInfo = try { UserMessagingPlatform.getConsentInformation(adView.context) } catch (_: Throwable) { null }
            val canRequest = consentInfo?.canRequestAds() == true
            if (canRequest && !hasRequested) {
                runCatching {
                    adView.loadAd(AdRequest.Builder().build())
                    hasRequested = true
                    Log.d(TAG, "Banner load requested from update (consent granted)")
                }.onFailure { e ->
                    Log.w(TAG, "Banner load request failed in update: ${e.message}")
                }
            }
        }
    )

    // 폴백: 업데이트가 발생하지 않더라도 동의 완료 후 일정 시간 내 자동 로드되도록 주기 확인
    LaunchedEffect(adViewRef) {
        val view = adViewRef ?: return@LaunchedEffect
        var attempts = 0
        while (!hasRequested && attempts < 20) { // 최대 약 20초 대기
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            val canRequest = consentInfo?.canRequestAds() == true
            if (canRequest) {
                runCatching {
                    view.loadAd(AdRequest.Builder().build())
                    hasRequested = true
                    Log.d(TAG, "Banner load requested from effect (consent granted)")
                }.onFailure { e ->
                    Log.w(TAG, "Banner load request failed in effect: ${e.message}")
                }
                break
            }
            attempts++
            delay(1000)
        }
        if (!hasRequested) {
            Log.d(TAG, "Banner load skipped after waiting: consent not granted within timeout")
        }
    }
}
