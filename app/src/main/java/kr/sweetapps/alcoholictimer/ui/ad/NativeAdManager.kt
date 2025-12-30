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
 * [REFACTORED] 네이티브 광고 관리자 - 스크롤 시 재로드 방지 (2025-12-31)
 *
 * 주요 개선사항:
 * - 광고 객체를 캐시에 저장하여 화면 스크롤 시 재사용
 * - 이미 로드된 광고가 있으면 AdLoader를 다시 호출하지 않음
 * - 생명주기 관리: destroy()를 통한 메모리 누수 방지
 */
object NativeAdManager {
    private const val TAG = "NativeAdManager"

    // Phase 3: 테스트 광고 단위 ID (실제 배포 시 변경 필요)
    private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" // Google 테스트 ID

    // [NEW] 광고 캐시 저장소 (key: 화면 식별자)
    private val adCache = mutableMapOf<String, NativeAd>()

    // [NEW] 광고 로딩 상태 관리 (중복 요청 방지)
    private val loadingStates = mutableMapOf<String, Boolean>()

    /**
     * [NEW] 캐싱된 광고 가져오기 (재로드 방지)
     *
     * @param screenKey 화면 식별자 (예: "community", "diary_feed")
     * @return 캐싱된 광고 객체 또는 null
     */
    fun getCachedAd(screenKey: String): NativeAd? {
        return adCache[screenKey]
    }

    /**
     * [NEW] 광고 로드 또는 캐시 반환
     *
     * @param context Context
     * @param screenKey 화면 식별자
     * @param onAdReady 광고 준비 완료 콜백 (캐시 또는 새로 로드)
     * @param onAdFailed 광고 로드 실패 콜백
     */
    fun getOrLoadAd(
        context: Context,
        screenKey: String,
        onAdReady: (NativeAd) -> Unit,
        onAdFailed: () -> Unit
    ) {
        // [STEP 1] 캐시된 광고가 있으면 즉시 반환
        adCache[screenKey]?.let { cachedAd ->
            Log.d(TAG, "[$screenKey] Returning cached native ad (no reload)")
            onAdReady(cachedAd)
            return
        }

        // [STEP 2] 이미 로딩 중이면 중복 요청 방지
        if (loadingStates[screenKey] == true) {
            Log.d(TAG, "[$screenKey] Ad is already loading, skipping duplicate request")
            return
        }

        // [STEP 3] 새로운 광고 로드
        loadingStates[screenKey] = true
        Log.d(TAG, "[$screenKey] Loading new native ad...")

        val adLoader = AdLoader.Builder(context, TEST_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "[$screenKey] Native ad loaded successfully")
                adCache[screenKey] = nativeAd // 캐시에 저장
                loadingStates[screenKey] = false
                onAdReady(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "[$screenKey] Failed to load native ad: ${loadAdError.message}")
                    loadingStates[screenKey] = false
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

    /**
     * [LEGACY] 기존 loadNativeAd 메서드 유지 (하위 호환성)
     * @deprecated getOrLoadAd() 사용 권장
     */
    @Deprecated(
        message = "Use getOrLoadAd() for better caching support",
        replaceWith = ReplaceWith("getOrLoadAd(context, \"default\", onAdLoaded, onAdFailed)")
    )
    fun loadNativeAd(
        context: Context,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: () -> Unit
    ) {
        getOrLoadAd(context, "default", onAdLoaded, onAdFailed)
    }

    /**
     * [NEW] 특정 화면의 광고 캐시 삭제 및 메모리 해제
     * Activity/Fragment onDestroy()에서 호출 권장
     *
     * @param screenKey 화면 식별자
     */
    fun destroyAd(screenKey: String) {
        adCache[screenKey]?.let { ad ->
            Log.d(TAG, "[$screenKey] Destroying cached native ad")
            ad.destroy() // 메모리 누수 방지
            adCache.remove(screenKey)
        }
        loadingStates.remove(screenKey)
    }

    /**
     * [NEW] 모든 광고 캐시 삭제 (앱 종료 시)
     */
    fun destroyAllAds() {
        Log.d(TAG, "Destroying all cached native ads (${adCache.size} items)")
        adCache.values.forEach { it.destroy() }
        adCache.clear()
        loadingStates.clear()
    }
}

