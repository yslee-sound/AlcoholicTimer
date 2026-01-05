@file:Suppress("ComposableInvocation", "UiComposableInvocation", "ComposableInvocationsCheck")

package kr.sweetapps.alcoholictimer.ui.components

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import kr.sweetapps.alcoholictimer.data.source.remote.AdRequestFactory
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdValue
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.delay

// BuildConfig ?�전 ?�근?? ?�적분석 경고�?줄이�??�해 ?�정 ?�드�??�음
private fun getAdmobBannerUnitId(): String {
    return try {
        val cls = Class.forName("kr.sweetapps.alcoholictimer.BuildConfig")
        val field = cls.getField("ADMOB_BANNER_UNIT_ID")
        (field.get(null) as? String) ?: ""
    } catch (_: Throwable) {
        ""
    }
}

private fun isDebugBuild(): Boolean {
    return try {
        val cls = Class.forName("kr.sweetapps.alcoholictimer.BuildConfig")
        val field = cls.getField("DEBUG")
        (field.get(null) as? Boolean) ?: false
    } catch (_: Throwable) {
        false
    }
}

private const val TAG = "AdmobBanner"

private const val ERROR_CODE_INTERNAL_ERROR = 0
private const val ERROR_CODE_INVALID_REQUEST = 1
private const val ERROR_CODE_NETWORK_ERROR = 2
private const val ERROR_CODE_NO_FILL = 3

private sealed class BannerLoadState {
    object Loading : BannerLoadState()
    object Success : BannerLoadState()
    data class Failed(val code: Int, val message: String) : BannerLoadState()
}

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

/** ?�시??구성 �?*/
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
    val configuration = LocalConfiguration.current

    // ?�면 ??dp) 계산: WindowMetrics API�??�선 ?�용?�여 Lint 경고 ?�거
    // (API ?�벨/?�외 ?�황?�서??Configuration.screenWidthDp�?fallback?�로 ?�용)
    val screenWidthDp: Int = remember(configuration) {
        val widthPx = try {
            // avoid API-23+ getSystemService(Class) usage: use string constant and cast
            val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager
            if (wm != null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        wm.currentWindowMetrics.bounds.width()
                    } else {
                        try { context.resources.displayMetrics.widthPixels } catch (_: Throwable) { 0 }
                    }
                } catch (_: Throwable) { 0 }
            } else 0
        } catch (_: Throwable) { 0 }
        val d = density.density
        // ?�선 LocalWindowInfo containerSize(px) ?�용, ?�으�?configuration fallback
        val fallbackPx = windowInfo.containerSize.width
        if (widthPx > 0) (widthPx / d).toInt() else if (fallbackPx > 0) (fallbackPx / d).toInt() else configuration.screenWidthDp
    }

    // Anchored Adaptive ?�이 계산
    val predictedHeight: Dp = remember(screenWidthDp) {
        try {
            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            with(density) { adaptive.getHeightInPixels(context).toDp() }
        } catch (_: Throwable) { 50.dp }
    }

    // ?�태
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var loadState by remember { mutableStateOf<BannerLoadState>(BannerLoadState.Loading) }
    var retryCount by remember { mutableStateOf(0) }
    var hasSuccessfulLoad by remember { mutableStateOf(false) }
    var finalFailureEmitted by remember { mutableStateOf(false) }
    var consecutiveNoFill by remember { mutableStateOf(0) }

    val maxRetry = retryConfig.maxRetry.coerceAtLeast(0)
    val retryDelays = retryConfig.retryDelaysMs.take(maxRetry.coerceAtLeast(1))

    // ?�책/?�면 ?�태 구독 (간단??
    val isPolicyEnabledState = remember { mutableStateOf(kr.sweetapps.alcoholictimer.ui.ad.AdController.isBannerEnabled()) }
    DisposableEffect(Unit) {
        val listener: (kr.sweetapps.alcoholictimer.ui.ad.AdController.Policy?) -> Unit = { p -> isPolicyEnabledState.value = p?.adBannerEnabled ?: false }
        kr.sweetapps.alcoholictimer.ui.ad.AdController.addPolicyFetchListener(listener)
        onDispose { kr.sweetapps.alcoholictimer.ui.ad.AdController.removePolicyFetchListener(listener) }
    }

    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ui.ad.AdController.isInterstitialShowingNow()
    // Observe reactive StateFlow from AdController so banner visibility updates reliably
    val isFullScreenAdShowing by kr.sweetapps.alcoholictimer.ui.ad.AdController.fullScreenAdShowingFlow.collectAsState(
        initial = kr.sweetapps.alcoholictimer.ui.ad.AdController.isFullScreenAdShowing()
    )
    val isBannerForceHidden by kr.sweetapps.alcoholictimer.ui.ad.AdController.bannerForceHiddenFlow.collectAsState(initial = false)

    // ?�� ?�계 ?��? ?�결�? 조건부 ?�더�?
    // ?�면광고가 ?�시 중이거나 강제 ?��? ?�태�?배너�??�예 ?�더링하지 ?�음
    // ??AdView가 메모리에 ?�으므�?겹칠 ???�음!
    val shouldRenderBanner = isPolicyEnabledState.value &&
                            !isInterstitialShowing &&
                            !isFullScreenAdShowing &&
                            !isBannerForceHidden

    if (!shouldRenderBanner) {
        Log.d(TAG, "Banner NOT rendered (policy=${isPolicyEnabledState.value} interstitial=$isInterstitialShowing fullScreen=$isFullScreenAdShowing forceHidden=$isBannerForceHidden)")
        // ?�무것도 ?�더링하지 ?�음 - ?�벽??겹침 방�?
        return
    }

    // ?�기?��??�는 배너�??�더�?
    Log.d(TAG, "Banner WILL be rendered")

    // Observe banner reload tick to retry loads immediately on demand
    val bannerReloadTick by kr.sweetapps.alcoholictimer.ui.ad.AdController.bannerReloadTick.collectAsState(initial = 0L)

    // ?�� 조건부 ?�더�?방식?�서??LaunchedEffect�?visibility ?�어 불필??
    // Compose가 ?�동?�로 ?�더�??�거�?처리??

    // Trigger immediate retry when bannerReloadTick updates (emitted by AdController)
    LaunchedEffect(bannerReloadTick) {
        val view = adViewRef
        if (view == null) return@LaunchedEffect
        try {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            val canRequestNow = (consentInfo?.canRequestAds() == true) || isDebugBuild()


            if (!hasSuccessfulLoad && isPolicyEnabledState.value && canRequestNow) {
                Log.d(TAG, "bannerReloadTick -> triggering immediate banner load")
                runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e ->
                    Log.w(TAG, "bannerReloadTick loadAd threw: ${e.message}")
                }
            } else {
                Log.d(TAG, "bannerReloadTick -> no load (policy=${isPolicyEnabledState.value} canRequest=$canRequestNow hasSuccess=$hasSuccessfulLoad)")
            }
        } catch (_: Throwable) {}
    }

    val shouldShowBanner = isPolicyEnabledState.value && !isInterstitialShowing && !isFullScreenAdShowing && !isBannerForceHidden
    // ?�약 공간??비활???�태???�도 배경???�색???�도�?surface�?기본?�로 ?�용?�니??
    val placeholderColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(shouldShowBanner) {
        Log.d(TAG, "banner visible=$shouldShowBanner h=$predictedHeight (policy=${isPolicyEnabledState.value} interstitial=$isInterstitialShowing fullScreen=$isFullScreenAdShowing forceHidden=$isBannerForceHidden)")
    }

    // shouldShowBanner 변�???즉시 AdView visibility 강제 ?�데?�트
    LaunchedEffect(adViewRef, shouldShowBanner, hasSuccessfulLoad, isBannerForceHidden, isFullScreenAdShowing) {
        val view = adViewRef
        if (view != null) {
            val targetVisibility = when {
                !shouldShowBanner -> View.GONE
                hasSuccessfulLoad -> View.VISIBLE
                else -> View.INVISIBLE
            }
            if (view.visibility != targetVisibility) {
                view.visibility = targetVisibility
                val visStr = when (targetVisibility) {
                    View.VISIBLE -> "VISIBLE"
                    View.INVISIBLE -> "INVISIBLE"
                    else -> "GONE"
                }
                Log.d(TAG, "LaunchedEffect(shouldShow=$shouldShowBanner, hasLoad=$hasSuccessfulLoad, forceHidden=$isBannerForceHidden, fullScreen=$isFullScreenAdShowing) -> forcing visibility=$visStr")
            }
        } else {
            Log.d(TAG, "LaunchedEffect skipped - adViewRef is null")
        }
    }

    // Always compose the banner container so AdView instance survives full-screen overlay.
    Surface(modifier = modifier.fillMaxWidth().height(predictedHeight), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 0.dp, shadowElevation = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        try { setBackgroundColor(android.graphics.Color.TRANSPARENT) } catch (_: Throwable) {}
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        val resolvedUnitId = getAdmobBannerUnitId()
                        val unitId = if (resolvedUnitId.isBlank() || resolvedUnitId.contains("REPLACE_WITH_REAL_BANNER")) "ca-app-pub-3940256099942544/9214589741" else resolvedUnitId
                        setAdUnitId(unitId)
                        try {
                            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp)
                            setAdSize(adaptive)
                        } catch (_: Throwable) { try { setAdSize(AdSize.BANNER) } catch (_: Throwable) {} }
                        // 초기?�는 INVISIBLE�??�작 (GONE???�님 - ?�이?�웃 측정?� ?�도�?
                        try { visibility = View.INVISIBLE } catch (_: Throwable) {}

                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d(TAG, "Banner onAdLoaded retryCount=$retryCount")

                                // ?�� ?�?�밍 진단: 배너 광고 로드 ?�료 ?�각 기록 + Activity ?�태 ?�인
                                val currentActivity = try {
                                    (context as? android.app.Activity)
                                } catch (_: Throwable) { null }
                                val isActivityFinishing = currentActivity?.isFinishing ?: false
                                kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logBannerLoadComplete(isActivityFinishing)

                                val first = !hasSuccessfulLoad
                                hasSuccessfulLoad = true
                                consecutiveNoFill = 0
                                loadState = BannerLoadState.Success

                                // 광고 로드 ?�공 ??즉시 VISIBLE 처리
                                try {
                                    if (!kr.sweetapps.alcoholictimer.ui.ad.AdController.isFullScreenAdShowing() &&
                                        !kr.sweetapps.alcoholictimer.ui.ad.AdController.bannerForceHiddenFlow.value) {
                                        visibility = View.VISIBLE
                                        Log.d(TAG, "Banner onAdLoaded -> set VISIBLE")
                                    } else {
                                        Log.d(TAG, "Banner onAdLoaded but keeping hidden (fullscreen or forceHidden)")
                                    }
                                } catch (_: Throwable) {}

                                if (first) {
                                    onFirstLoaded?.invoke()
                                    analytics.onFirstLoaded()
                                    // Analytics: log banner impression (first successful load)
                                    try { AnalyticsManager.logAdImpression("banner") } catch (_: Throwable) {}
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

                            override fun onAdClicked() {
                                Log.d(TAG, "Banner onAdClicked")
                                try { AnalyticsManager.logAdClick("banner") } catch (_: Throwable) {}
                            }
                        }

                        adViewRef = this
                        // Paid event listener: report ad revenue to AnalyticsManager
                        try {
                            setOnPaidEventListener { adValue: AdValue ->
                                try {
                                    val value = (adValue.valueMicros / 1_000_000.0)
                                    val currency = adValue.currencyCode
                                    AnalyticsManager.logAdRevenue(value, currency, "banner")
                                } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {}
                        Log.d(TAG, "AdView factory created unit=$unitId visibility=${visibility}")
                    }
                },
                update = { adView ->
                    try {
                        // ?�체?�면 광고??강제 ?��? ?�태 ?�인
                        val shouldHide = kr.sweetapps.alcoholictimer.ui.ad.AdController.isFullScreenAdShowing() || isBannerForceHidden

                        if (shouldHide) {
                            if (adView.visibility != View.GONE) {
                                adView.visibility = View.GONE
                                Log.d(TAG, "Banner update -> GONE (fullscreen=$isFullScreenAdShowing forceHidden=$isBannerForceHidden)")
                            }
                            try { adView.pause() } catch (_: Throwable) {}
                        } else {
                            // 광고가 로드?�었?�면 VISIBLE, ?�니�?INVISIBLE
                            val targetVisibility = if (hasSuccessfulLoad) View.VISIBLE else View.INVISIBLE
                            if (adView.visibility != targetVisibility) {
                                adView.visibility = targetVisibility
                                Log.d(TAG, "Banner update -> ${if (targetVisibility == View.VISIBLE) "VISIBLE" else "INVISIBLE"} (hasLoad=$hasSuccessfulLoad)")
                            }
                            try { adView.resume() } catch (_: Throwable) {}
                        }
                    } catch (_: Throwable) {}

                    val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(adView.context) }.getOrNull()
                    val consentDebug = consentInfoDebug(consentInfo)
                    val publisherMisconfigured = consentDebug.contains("Publisher misconfiguration", ignoreCase = true) || consentDebug.contains("no form", ignoreCase = true)
                    if (publisherMisconfigured) Log.e(TAG, "UMP publisher misconfiguration detected; suppressing loads until fixed")
                    val canRequest = (consentInfo?.canRequestAds() == true) || isDebugBuild()

                    if (isPolicyEnabledState.value && canRequest && !hasSuccessfulLoad && loadState is BannerLoadState.Loading && !publisherMisconfigured) {
                        Log.d(TAG, "Issuing initial banner loadAd (canRequest=$canRequest debug=${isDebugBuild()})")

                        // ?�� ?�?�밍 진단: 배너 광고 로드 ?�청 ?�각 기록
                        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logBannerLoadRequest()

                        runCatching { adView.loadAd(AdRequestFactory.create(adView.context)) }.onFailure { e -> Log.w(TAG, "initial loadAd threw: ${e.message}") }
                    } else {
                        Log.d(TAG, "Banner load skipped (policy=${isPolicyEnabledState.value} canRequest=$canRequest hasSuccess=$hasSuccessfulLoad state=$loadState publisherMisconfigured=$publisherMisconfigured)")
                    }
                }
            )

            // Register a persistent full-screen listener tied to adViewRef so banner is hidden immediately when full-screen starts.
            DisposableEffect(adViewRef) {
                val view = adViewRef
                if (view != null) {
                    val fsListener: (Boolean) -> Unit = { showing ->
                        try {
                            view.post {
                                try {
                                    if (showing) {
                                        try { view.pause() } catch (_: Throwable) {}
                                        try { view.visibility = View.GONE } catch (_: Throwable) {}
                                    } else {
                                        try { view.visibility = View.VISIBLE } catch (_: Throwable) {}
                                        try { view.resume() } catch (_: Throwable) {}
                                    }
                                } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {}
                    }
                    val bfListener: (Boolean) -> Unit = { hidden ->
                        try {
                            view.post {
                                try {
                                    if (hidden) {
                                        try { view.pause() } catch (_: Throwable) {}
                                        try { view.visibility = View.GONE } catch (_: Throwable) {}
                                    } else {
                                        try { view.visibility = View.VISIBLE } catch (_: Throwable) {}
                                        try { view.resume() } catch (_: Throwable) {}
                                    }
                                } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {}
                    }
                    try { kr.sweetapps.alcoholictimer.ui.ad.AdController.addFullScreenShowListener(fsListener) } catch (_: Throwable) {}
                    try { kr.sweetapps.alcoholictimer.ui.ad.AdController.addBannerForceHiddenListener(bfListener) } catch (_: Throwable) {}
                    onDispose {
                        try { kr.sweetapps.alcoholictimer.ui.ad.AdController.removeFullScreenShowListener(fsListener) } catch (_: Throwable) {}
                        try { kr.sweetapps.alcoholictimer.ui.ad.AdController.removeBannerForceHiddenListener(bfListener) } catch (_: Throwable) {}
                    }
                } else {
                    onDispose { }
                }
            }

            // If banner should be hidden but we must reserve space, render placeholder overlay (use surface -> typically white)
            if (!shouldShowBanner && reserveSpaceWhenDisabled) {
                Box(Modifier.matchParentSize().background(placeholderColor)) {}
            }

            // For loading/failed states keep overlays transparent so UI doesn't show gray shading
            when (loadState) {
                BannerLoadState.Loading -> Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0f)))
                is BannerLoadState.Failed -> Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0f)))
                BannerLoadState.Success -> Unit
            }
        }
    }

    // ?�시??루프
    LaunchedEffect(retryCount) {
        if (retryCount in 1..maxRetry && !hasSuccessfulLoad) {
            val delayMs = retryDelays.getOrNull(retryCount - 1) ?: retryDelays.lastOrNull() ?: 4000L
            analytics.onRetryScheduled(retryCount, delayMs)
            Log.d(TAG, "Scheduled retry #$retryCount in ${delayMs}ms")
            delay(delayMs)
            val view = adViewRef
            if (view != null && loadState is BannerLoadState.Failed && !hasSuccessfulLoad) {
                val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
                Log.d(TAG, "ConsentInfo(retryLoop): ${consentInfoDebug(consentInfo)}")
                val canRequestRetry = (consentInfo?.canRequestAds() == true) || isDebugBuild()
                if (canRequestRetry) {
                    loadState = BannerLoadState.Loading
                    runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e ->
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

    // 초기 ?�의 ?��?�?주기??체크??LaunchedEffect?�서 ?��?
    LaunchedEffect(adViewRef) {
        val view = adViewRef ?: return@LaunchedEffect
        var attempts = 0
        while (!hasSuccessfulLoad && loadState is BannerLoadState.Loading && attempts < 60) {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            val consentDebug = consentInfoDebug(consentInfo)
            Log.d(TAG, "ConsentInfo(initialFallback): $consentDebug attempts=$attempts")
            val publisherMisconfigured = consentDebug.contains("Publisher misconfiguration", ignoreCase = true) || consentDebug.contains("no form", ignoreCase = true)
            if (publisherMisconfigured) {
                Log.e(TAG, "Initial fallback: detected UMP publisher misconfiguration; aborting initial wait and deferring to periodic checks")
                break
            }
            if (consentInfo?.canRequestAds() == true) {
                Log.d(TAG, "Consent available in fallback loop; requesting load")
                runCatching { view.loadAd(AdRequestFactory.create(view.context)) }
                break
            }
            attempts++
            delay(1000)
        }
        if (!hasSuccessfulLoad && loadState is BannerLoadState.Loading && attempts >= 60) {
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

    // [CRITICAL FIX] 무한 루프 제거 - 최대 재시도 제한 추가 (2026-01-05)
    // 기존: 광고 로드 성공할 때까지 5초마다 무한 반복 (60초 = 12회 요청)
    // 수정: 최대 5회(25초)까지만 시도 후 중단
    LaunchedEffect(adViewRef, loadState) {
        val view = adViewRef ?: return@LaunchedEffect
        var periodicRetryCount = 0
        val MAX_PERIODIC_RETRIES = 5 // 최대 5회 (5 × 5초 = 25초)

        while (!hasSuccessfulLoad && periodicRetryCount < MAX_PERIODIC_RETRIES) {
            try {
                val failedDueToConsent = loadState is BannerLoadState.Failed && (loadState as BannerLoadState.Failed).code == -2
                if (failedDueToConsent) {
                    val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
                    val consentDebug = consentInfoDebug(consentInfo)
                    Log.d(TAG, "ConsentInfo(periodic #$periodicRetryCount): $consentDebug")
                    val publisherMisconfigured = consentDebug.contains("Publisher misconfiguration", ignoreCase = true) || consentDebug.contains("no form", ignoreCase = true)
                    if (publisherMisconfigured) {
                        Log.e(TAG, "Periodic check: UMP publisher misconfiguration still present; will not attempt loads until configuration fixed")
                    }
                    val canRequest = (consentInfo?.canRequestAds() == true) || isDebugBuild()
                    if (canRequest && !publisherMisconfigured) {
                        periodicRetryCount++
                        Log.d(TAG, "Periodic check #$periodicRetryCount/$MAX_PERIODIC_RETRIES: consent available -> retrying banner load")
                        loadState = BannerLoadState.Loading
                        runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e -> Log.w(TAG, "Periodic loadAd threw: ${e.message}") }
                    }
                }
            } catch (_: Throwable) {}
            delay(5_000)
        }

        if (periodicRetryCount >= MAX_PERIODIC_RETRIES && !hasSuccessfulLoad) {
            Log.w(TAG, "AdBanner: 최대 주기적 재시도 횟수(${MAX_PERIODIC_RETRIES}회) 도달. 더 이상 재시도하지 않음.")
        }
    }

    // adView 리소???�제
    DisposableEffect(adViewRef) {
        onDispose {
            try { adViewRef?.destroy() } catch (_: Throwable) {}
            adViewRef = null
        }
    }
}

private fun shouldRetry(code: Int, currentAttempt: Int, maxRetry: Int, cfg: BannerRetryConfig, consecutiveNoFill: Int): Boolean {
    if (currentAttempt >= maxRetry) return false
    return when (code) {
        ERROR_CODE_NO_FILL -> cfg.retryOnNoFill && (consecutiveNoFill < cfg.maxConsecutiveNoFill)
        ERROR_CODE_INVALID_REQUEST -> cfg.retryOnInvalidRequest
        ERROR_CODE_INTERNAL_ERROR -> cfg.retryOnInternalError
        ERROR_CODE_NETWORK_ERROR -> cfg.retryOnNetworkError
        else -> false
    }
}

private fun consentInfoDebug(consentInfo: com.google.android.ump.ConsentInformation?): String {
    return try {
        if (consentInfo == null) return "ConsentInformation=null"
        val isForm = try { consentInfo.isConsentFormAvailable } catch (_: Throwable) { "?" }
        val canReq = try { consentInfo.canRequestAds() } catch (_: Throwable) { "?" }
        val dbgGeo = try {
            val f = consentInfo::class.java.getMethod("getDebugGeography")
            f.invoke(consentInfo)?.toString() ?: "na"
        } catch (_: Throwable) { "na" }
        "isFormAvailable=$isForm canRequestAds=$canReq debugGeography=$dbgGeo"
    } catch (t: Throwable) {
        "consentDebugError=${t.message}"
    }
}
