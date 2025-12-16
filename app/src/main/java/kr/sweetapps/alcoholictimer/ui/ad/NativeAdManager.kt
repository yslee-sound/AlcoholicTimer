package kr.sweetapps.alcoholictimer.ui.ad

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * Phase 3: 네이티브 광고 관리자
 * Tab 4 커뮤니티 피드에서 사용
 */
object NativeAdManager {
    private const val TAG = "NativeAdManager"

    // Phase 3: 테스트 광고 단위 ID (실제 배포 시 변경 필요)
    private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" // Google 테스트 ID

    /**
     * 네이티브 광고 로드
     *
     * @param context Context
     * @param onAdLoaded 광고 로드 성공 콜백
     * @param onAdFailed 광고 로드 실패 콜백
     */
    fun loadNativeAd(
        context: Context,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: () -> Unit
    ) {
        val adLoader = AdLoader.Builder(context, TEST_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad loaded successfully")
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Failed to load native ad: ${loadAdError.message}")
                    onAdFailed()
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}

