package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

// 외부 콜백에 노출할 공개 실패 정보 (private BannerLoadState 누출 방지)
data class BannerFailure(val code: Int, val message: String)

// Analytics 이벤트 훅 (다른 앱에서도 재사용 가능)
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
    // 연속 NO_FILL 허용 최대(도달 시 더 이상 재시도하지 않음)
    val maxConsecutiveNoFill: Int = 2,
    // INVALID_REQUEST(1) 이후 재시도 여부 (보통 의미 없어 false 권장)
    val retryOnInvalidRequest: Boolean = false,
    // INTERNAL_ERROR(0) 는 재시도 허용
    val retryOnInternalError: Boolean = true,
    // NETWORK_ERROR(2) 는 재시도 허용
    val retryOnNetworkError: Boolean = true,
    // NO_FILL(3) 재시도 허용 여부 (true 시 위 maxConsecutiveNoFill 로 제한)
    val retryOnNoFill: Boolean = true,
)

/**
 * Admob 배너 컴포저블 (상단 고정 / 점프 없는 Placeholder 전략)
 * 기능:
 *  - Adaptive Anchored 높이 예측 + 공간 선확보
 *  - 에러 코드별 재시도 전략
 *  - 콜백: 첫 성공, 최종 실패
 *  - 자원 정리(destroy)
 */
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

    // 화면 폭(dp) 계산 수정: Density 객체 자체 나눗셈 제거, Float density 값 사용
    val screenWidthDp = remember(windowInfo.containerSize) {
        val dm = context.resources.displayMetrics
        val px = windowInfo.containerSize.width
        val d = density.density
        if (px > 0) (px / d).toInt() else (dm.widthPixels / dm.density).toInt()
    }

    // Adaptive 높이 예측
    val predictedHeight: Dp = remember(screenWidthDp) {
        try {
            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            with(density) { adaptive.getHeightInPixels(context).toDp() }
        } catch (_: Throwable) { 50.dp }
    }

    val isBannerEnabled = kr.sweetapps.alcoholictimer.ads.AdController.isBannerEnabled()
    LaunchedEffect(isBannerEnabled) { Log.d(TAG, "🔍 Banner enabled: $isBannerEnabled reserve=$reserveSpaceWhenDisabled") }

    if (!isBannerEnabled) {
        if (reserveSpaceWhenDisabled) Box(modifier = modifier.fillMaxWidth().height(predictedHeight)) else Box(modifier = modifier.fillMaxWidth())
        return
    }

    // 상태들
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var loadState by remember { mutableStateOf<BannerLoadState>(BannerLoadState.Loading) }
    var realHeight by remember { mutableStateOf(predictedHeight) }
    var retryCount by remember { mutableStateOf(0) }
    var hasSuccessfulLoad by remember { mutableStateOf(false) }
    var finalFailureEmitted by remember { mutableStateOf(false) }
    var consecutiveNoFill by remember { mutableStateOf(0) }

    val maxRetry = retryConfig.maxRetry.coerceAtLeast(0)
    val retryDelays = retryConfig.retryDelaysMs.take(maxRetry.coerceAtLeast(1))

    val targetHeight = remember(predictedHeight, realHeight) { if (realHeight > predictedHeight + 1.dp) realHeight else predictedHeight }
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f), label = "bannerHeightAnim")

    Box(modifier = modifier.fillMaxWidth().height(animatedHeight)) {
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
                            runCatching {
                                val hPx = adSize?.getHeightInPixels(ctx) ?: 0
                                val hDp = with(density) { hPx.toDp() }
                                if (hDp > 0.dp) realHeight = hDp
                            }
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
                }
            },
            update = { adView ->
                val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(adView.context) }.getOrNull()
                if (consentInfo?.canRequestAds() == true && !hasSuccessfulLoad && loadState is BannerLoadState.Loading) {
                    Log.d(TAG, "Issuing initial banner loadAd")
                    runCatching { adView.loadAd(AdRequest.Builder().build()) }
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
    DisposableEffect(adViewRef) { onDispose { runCatching { adViewRef?.destroy() }; Log.d(TAG, "AdView destroyed") } }
}

// 동일 패턴 하단 배치용 래퍼 (필요 시 디자인 다르게 확장 가능)
@Suppress("unused") // 아직 실제 화면에서 사용하지 않지만 향후 하단 배너 도입 시 활용 예정
@Composable
fun BottomBannerAd(
    modifier: Modifier = Modifier,
    retryConfig: BannerRetryConfig = BannerRetryConfig(),
    analytics: BannerAdAnalytics = NoOpBannerAdAnalytics,
    onFirstLoaded: (() -> Unit)? = null,
    onAllRetriesFailed: ((BannerFailure) -> Unit)? = null,
) = AdmobBanner(
    modifier = modifier,
    reserveSpaceWhenDisabled = true,
    retryConfig = retryConfig,
    onFirstLoaded = onFirstLoaded,
    onAllRetriesFailed = onAllRetriesFailed,
    analytics = analytics,
)

private fun shouldRetry(
    code: Int,
    retryCount: Int,
    maxRetry: Int,
    config: BannerRetryConfig,
    consecutiveNoFill: Int,
): Boolean {
    if (retryCount >= maxRetry) return false
    return when (code) {
        ERROR_CODE_INVALID_REQUEST -> config.retryOnInvalidRequest
        ERROR_CODE_INTERNAL_ERROR -> config.retryOnInternalError
        ERROR_CODE_NETWORK_ERROR -> config.retryOnNetworkError
        ERROR_CODE_NO_FILL -> config.retryOnNoFill && consecutiveNoFill < config.maxConsecutiveNoFill
        else -> true
    }
}
