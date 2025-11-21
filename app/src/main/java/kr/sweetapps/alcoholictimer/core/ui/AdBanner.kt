package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.ump.UserMessagingPlatform
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.delay

private const val TAG = "AdmobBanner"

// AdMob 표준 에러 코드 상수 (간단 매핑)
private const val ERROR_CODE_INTERNAL_ERROR = 0
private const val ERROR_CODE_INVALID_REQUEST = 1
private const val ERROR_CODE_NETWORK_ERROR = 2
private const val ERROR_CODE_NO_FILL = 3

private sealed class BannerLoadState { object Loading: BannerLoadState(); object Success: BannerLoadState(); data class Failed(val code:Int, val message:String): BannerLoadState() }

data class BannerFailure(val code: Int, val message: String)

interface BannerAdAnalytics {
    fun onFirstLoaded() {}
    fun onRetryScheduled(attempt: Int, delayMs: Long) {}
    fun onFailure(code: Int, message: String, willRetry: Boolean, attempt: Int) {}
    fun onAllRetriesFailed(code: Int, message: String) {}
    fun onConsentTimeout() {}
    fun onNoFillSequence(sequence: Int) {}
}
object NoOpBannerAdAnalytics : BannerAdAnalytics

/** 재시도 구성 값 */
data class BannerRetryConfig(
    val maxRetry: Int = 3,
    val retryDelaysMs: List<Long> = listOf(2000, 5000, 10000),
    val maxConsecutiveNoFill: Int = 2,
    val retryOnInvalidRequest: Boolean = false,
    val retryOnInternalError: Boolean = true,
    val retryOnNetworkError: Boolean = true,
    val retryOnNoFill: Boolean = true,
)

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    reserveSpaceWhenDisabled: Boolean = true,
    retryConfig: BannerRetryConfig = BannerRetryConfig(),
    onFirstLoaded: (() -> Unit)? = null,
    onAllRetriesFailed: ((BannerFailure) -> Unit)? = null,
    analytics: BannerAdAnalytics = NoOpBannerAdAnalytics,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    // 화면 폭(dp) 계산 (containerSize 우선, fallback: resources)
    val screenWidthDp = remember(windowInfo.containerSize) {
        val dm = context.resources.displayMetrics
        val px = windowInfo.containerSize.width
        val d = density.density
        if (px > 0) (px / d).toInt() else (dm.widthPixels / dm.density).toInt()
    }

    // Anchored Adaptive 높이 계산 (기기별로 50/60/…)
    val predictedHeight: Dp = remember(screenWidthDp) {
        try {
            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            with(density) { adaptive.getHeightInPixels(context).toDp() }
        } catch (_: Throwable) { 50.dp }
    }

    // 정책/전면 상태 구독
    // Subscribe to runtime policy so Compose recomposes when Supabase toggles the banner flag
    val isPolicyEnabledState = remember { mutableStateOf(kr.sweetapps.alcoholictimer.ads.AdController.isBannerEnabled()) }
    DisposableEffect(Unit) {
        val listener: (kr.sweetapps.alcoholictimer.ads.AdController.Policy?) -> Unit = { p -> isPolicyEnabledState.value = p?.adBannerEnabled ?: false }
        kr.sweetapps.alcoholictimer.ads.AdController.addPolicyFetchListener(listener)
        onDispose { kr.sweetapps.alcoholictimer.ads.AdController.removePolicyFetchListener(listener) }
    }
    val isPolicyEnabled by remember { derivedStateOf { isPolicyEnabledState.value } }
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ads.AdController.isInterstitialShowingNow()
    val isFullScreenAdShowing = kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing()
    val shouldShowBanner = isPolicyEnabled && !isInterstitialShowing && !isFullScreenAdShowing
    val placeholderColor = if (isInterstitialShowing) Color.Black else MaterialTheme.colorScheme.surface
    LaunchedEffect(shouldShowBanner) { Log.d(TAG, "banner visible=$shouldShowBanner h=$predictedHeight") }

    // 비표시 시에도 공간 예약 여부
    if (!shouldShowBanner) {
        if (reserveSpaceWhenDisabled) {
            androidx.compose.material3.Surface(
                modifier = modifier.fillMaxWidth().height(predictedHeight),
                color = placeholderColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) { }
        }
        return
    }

    // 상태
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var loadState by remember { mutableStateOf<BannerLoadState>(BannerLoadState.Loading) }
    var retryCount by remember { mutableStateOf(0) }
    var hasSuccessfulLoad by remember { mutableStateOf(false) }
    var finalFailureEmitted by remember { mutableStateOf(false) }
    var consecutiveNoFill by remember { mutableStateOf(0) }

    val maxRetry = retryConfig.maxRetry.coerceAtLeast(0)
    val retryDelays = retryConfig.retryDelaysMs.take(maxRetry.coerceAtLeast(1))

    // 컨테이너: Anchored Adaptive height로 고정 (레이아웃 시프트 방지 + 잘림 방지)
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth().height(predictedHeight),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        val resolvedUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                        val unitId = if (resolvedUnitId.isNullOrBlank() || resolvedUnitId.contains("REPLACE_WITH_REAL_BANNER")) "ca-app-pub-3940256099942544/6300978111" else resolvedUnitId
                        setAdUnitId(unitId)
                        try {
                            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp)
                            setAdSize(adaptive)
                        } catch (_: Throwable) { try { setAdSize(AdSize.BANNER) } catch (ignored: Throwable) { Log.w(TAG, "Fallback setAdSize failed: ${ignored.message}") } }
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d(TAG, "Banner onAdLoaded retryCount=$retryCount")
                                val first = !hasSuccessfulLoad
                                hasSuccessfulLoad = true
                                consecutiveNoFill = 0
                                loadState = BannerLoadState.Success
                                if (first) {
                                    onFirstLoaded?.invoke()
                                    analytics.onFirstLoaded()
                                }
                            }
                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                val code = error.code
                                val msg = error.message
                                Log.w(TAG, "Banner failed code=$code msg=$msg retryCount=$retryCount noFillSeq=$consecutiveNoFill")
                                if (code == ERROR_CODE_NO_FILL) {
                                    consecutiveNoFill += 1
                                    analytics.onNoFillSequence(consecutiveNoFill)
                                } else consecutiveNoFill = 0
                                loadState = BannerLoadState.Failed(code, msg)
                                val canRetry = shouldRetry(code, retryCount, maxRetry, retryConfig, consecutiveNoFill)
                                analytics.onFailure(code, msg, canRetry, retryCount)
                                if (canRetry) retryCount += 1 else if (!finalFailureEmitted && !hasSuccessfulLoad) {
                                    finalFailureEmitted = true
                                    onAllRetriesFailed?.invoke(BannerFailure(code, msg))
                                    analytics.onAllRetriesFailed(code, msg)
                                }
                            }
                        }
                        adViewRef = this
                        Log.d(TAG, "AdView factory created unit=$unitId")
                    }
                },
                update = { adView ->
                    Log.d(TAG, "AndroidView update called; adViewRef=${adViewRef != null} loadState=$loadState isPolicyEnabled=$isPolicyEnabled")
                    val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(adView.context) }.getOrNull()
                    val canRequest = (consentInfo?.canRequestAds() == true) || BuildConfig.DEBUG
                    // Only request/load banner if policy allows (Supabase-controlled)
                    if (isPolicyEnabled && canRequest && !hasSuccessfulLoad && loadState is BannerLoadState.Loading) {
                        Log.d(TAG, "Issuing initial banner loadAd (canRequest=$canRequest debug=${BuildConfig.DEBUG})")
                        runCatching { adView.loadAd(AdRequest.Builder().build()) }.onFailure { e -> Log.w(TAG, "initial loadAd threw: ${e.message}") }
                    } else {
                        Log.d(TAG, "Banner load skipped (isPolicyEnabled=$isPolicyEnabled canRequest=$canRequest hasSuccessfulLoad=$hasSuccessfulLoad loadState=$loadState)")
                    }
                }
            )
            when (loadState) {
                BannerLoadState.Loading ->
                    Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)))
                is BannerLoadState.Failed ->
                    Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)))
                BannerLoadState.Success -> Unit
            }
        }
    }

    // 재시도 루프
    LaunchedEffect(retryCount) {
        if (retryCount in 1..maxRetry && !hasSuccessfulLoad) {
            val delayMs = retryDelays.getOrNull(retryCount - 1) ?: retryDelays.lastOrNull() ?: 4000L
            analytics.onRetryScheduled(retryCount, delayMs)
            Log.d(TAG, "Scheduled retry #$retryCount in ${delayMs}ms")
            delay(delayMs)
            val view = adViewRef
            if (view != null && loadState is BannerLoadState.Failed && !hasSuccessfulLoad) {
                val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
                if (consentInfo?.canRequestAds() == true) {
                    loadState = BannerLoadState.Loading
                    runCatching { view.loadAd(AdRequest.Builder().build()) }.onFailure { e ->
                        val msg = e.message ?: "error"
                        Log.w(TAG, "Retry immediate load error: $msg")
                        loadState = BannerLoadState.Failed(-3, msg)
                        if (!finalFailureEmitted && !hasSuccessfulLoad) {
                            finalFailureEmitted = true
                            onAllRetriesFailed?.invoke(BannerFailure(-3, msg))
                            analytics.onAllRetriesFailed(-3, msg)
                        }
                    }
                }
            }
        }
    }

    // 동의 지연 폴백 (최초 로딩 대기)
    LaunchedEffect(adViewRef) {
        val view = adViewRef ?: return@LaunchedEffect
        var attempts = 0
        while (!hasSuccessfulLoad && loadState is BannerLoadState.Loading && attempts < 15) {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            if (consentInfo?.canRequestAds() == true) {
                Log.d(TAG, "Consent available in fallback loop; requesting load")
                runCatching { view.loadAd(AdRequest.Builder().build()) }
                break
            }
            attempts++
            delay(1000)
        }
        if (!hasSuccessfulLoad && loadState is BannerLoadState.Loading && attempts >= 15) {
            val timeoutFailure = BannerLoadState.Failed(-2, "consent timeout")
            loadState = timeoutFailure
            if (!finalFailureEmitted) {
                finalFailureEmitted = true
                onAllRetriesFailed?.invoke(BannerFailure(timeoutFailure.code, timeoutFailure.message))
                analytics.onConsentTimeout()
                analytics.onAllRetriesFailed(timeoutFailure.code, timeoutFailure.message)
            }
        }
    }

    // destroy
    // Destroy AdView only when this composable leaves composition (prevent premature destroy on adViewRef changes)
    DisposableEffect(Unit) { onDispose { runCatching { adViewRef?.destroy() }; Log.d(TAG, "AdView destroyed") } }
}

@Suppress("unused")
@Composable
fun BottomBannerAd(
    modifier: Modifier = Modifier,
    retryConfig: BannerRetryConfig = BannerRetryConfig(),
    analytics: BannerAdAnalytics = NoOpBannerAdAnalytics,
    onFirstLoaded: (() -> Unit)? = null,
    onAllRetriesFailed: ((BannerFailure) -> Unit)? = null,
) = AdmobBanner(
    modifier = modifier,
    retryConfig = retryConfig,
    analytics = analytics,
    onFirstLoaded = onFirstLoaded,
    onAllRetriesFailed = onAllRetriesFailed,
)

// 내부 함수들 (변경 없음)
private fun shouldRetry(code: Int, attempt: Int, maxRetry: Int, cfg: BannerRetryConfig, noFillSeq: Int): Boolean {
    val retryableError = when (code) {
        ERROR_CODE_INTERNAL_ERROR -> cfg.retryOnInternalError
        ERROR_CODE_NETWORK_ERROR -> cfg.retryOnNetworkError
        ERROR_CODE_INVALID_REQUEST -> cfg.retryOnInvalidRequest
        ERROR_CODE_NO_FILL -> cfg.retryOnNoFill && noFillSeq < cfg.maxConsecutiveNoFill
        else -> false
    }
    return retryableError && attempt < maxRetry
}
