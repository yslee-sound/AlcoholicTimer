package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.ump.UserMessagingPlatform
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.delay
import kr.sweetapps.alcoholictimer.core.ads.AdLoadState

private const val TAG = "AdmobBanner"

private sealed class BannerLoadState { object Loading: BannerLoadState(); object Success: BannerLoadState(); data class Failed(val code:Int, val message:String): BannerLoadState() }

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    var hasRequested by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var loadState by remember { mutableStateOf<BannerLoadState>(BannerLoadState.Loading) }

    Box(modifier = modifier.fillMaxWidth().height(LayoutConstants.BANNER_FIXED_HEIGHT)) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxWidth(),
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
                        override fun onAdLoaded() {
                            Log.d(TAG, "Banner onAdLoaded")
                            loadState = BannerLoadState.Success
                        }
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            Log.w(TAG, "Banner onAdFailedToLoad code=${error.code} message=${error.message}")
                            loadState = BannerLoadState.Failed(error.code, error.message ?: "")
                        }
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
                if (canRequest && !hasRequested && !AdLoadState.bannerRequested.get()) {
                    runCatching {
                        adView.loadAd(AdRequest.Builder().build())
                        hasRequested = true
                        AdLoadState.bannerRequested.set(true)
                        Log.d(TAG, "Banner load requested from update (consent granted)")
                    }.onFailure { e ->
                        Log.w(TAG, "Banner load request failed in update: ${e.message}")
                    }
                } else if (canRequest && AdLoadState.bannerRequested.get() && !hasRequested) {
                    // 전역 플래그가 이미 true라면 로드를 건너뛰었다는 로그를 남겨 추적 가능하게 함
                    Log.d(TAG, "Banner load skipped in update: global bannerRequested=true")
                    hasRequested = true // 로컬은 재시도 방지를 위해 true로 설정
                }
            }
        )

        when (val state = loadState) {
            is BannerLoadState.Loading -> {
                Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
            is BannerLoadState.Failed -> {
                Box(Modifier.matchParentSize().background(Color(0xFFECECEC)), contentAlignment = Alignment.Center) {
                    Text(text = "배너 로딩 실패", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
            }
            BannerLoadState.Success -> { /* No overlay */ }
        }
    }

    // 폴백: 업데이트가 발생하지 않더라도 동의 완료 후 일정 시간 내 자동 로드되도록 주기 확인
    LaunchedEffect(adViewRef) {
        val view = adViewRef ?: return@LaunchedEffect
        var attempts = 0
        while (!hasRequested && attempts < 15) {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            val canRequest = consentInfo?.canRequestAds() == true
            if (canRequest) {
                if (!AdLoadState.bannerRequested.get()) {
                    runCatching {
                        view.loadAd(AdRequest.Builder().build())
                        hasRequested = true
                        AdLoadState.bannerRequested.set(true)
                        Log.d(TAG, "Banner load requested from effect (consent granted)")
                    }.onFailure { e ->
                        Log.w(TAG, "Banner load request failed in effect: ${e.message}")
                        loadState = BannerLoadState.Failed(-1, e.message ?: "")
                    }
                } else {
                    Log.d(TAG, "Banner load skipped in effect: global bannerRequested=true")
                    hasRequested = true
                }
                break
            }
            attempts++
            delay(1000)
        }
        if (!hasRequested && loadState is BannerLoadState.Loading) {
            loadState = BannerLoadState.Failed(-2, "consent timeout")
        }
    }
}
