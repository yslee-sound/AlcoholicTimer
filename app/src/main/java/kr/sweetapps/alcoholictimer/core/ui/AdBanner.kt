@file:Suppress("ComposableInvocation", "UiComposableInvocation", "ComposableInvocationsCheck")

package kr.sweetapps.alcoholictimer.core.ui

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
import kr.sweetapps.alcoholictimer.ads.AdRequestFactory
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdValue
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.delay

// BuildConfig 안전 접근자: 정적분석 경고를 줄이기 위해 특정 필드만 읽음
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
    val configuration = LocalConfiguration.current

    // 화면 폭(dp) 계산: WindowMetrics API를 우선 사용하여 Lint 경고 제거
    // (API 레벨/예외 상황에서는 Configuration.screenWidthDp를 fallback으로 사용)
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
        // 우선 LocalWindowInfo containerSize(px) 사용, 없으면 configuration fallback
        val fallbackPx = windowInfo.containerSize.width
        if (widthPx > 0) (widthPx / d).toInt() else if (fallbackPx > 0) (fallbackPx / d).toInt() else configuration.screenWidthDp
    }

    // Anchored Adaptive 높이 계산
    val predictedHeight: Dp = remember(screenWidthDp) {
        try {
            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            with(density) { adaptive.getHeightInPixels(context).toDp() }
        } catch (_: Throwable) { 50.dp }
    }

    // 상태
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var loadState by remember { mutableStateOf<BannerLoadState>(BannerLoadState.Loading) }
    var retryCount by remember { mutableStateOf(0) }
    var hasSuccessfulLoad by remember { mutableStateOf(false) }
    var finalFailureEmitted by remember { mutableStateOf(false) }
    var consecutiveNoFill by remember { mutableStateOf(0) }
    // 앱 시작 시 기본적으로 숨김. 앱오프닝 광고가 완전히 닫힌 후에만 표시/로딩을 허용합니다.
    var hideUntilFirstDismiss by remember { mutableStateOf(true) }

    val maxRetry = retryConfig.maxRetry.coerceAtLeast(0)
    val retryDelays = retryConfig.retryDelaysMs.take(maxRetry.coerceAtLeast(1))

    // 정책/전면 상태 구독 (간단화)
    val isPolicyEnabledState = remember { mutableStateOf(kr.sweetapps.alcoholictimer.ads.AdController.isBannerEnabled()) }
    DisposableEffect(Unit) {
        val listener: (kr.sweetapps.alcoholictimer.ads.AdController.Policy?) -> Unit = { p -> isPolicyEnabledState.value = p?.adBannerEnabled ?: false }
        kr.sweetapps.alcoholictimer.ads.AdController.addPolicyFetchListener(listener)
        onDispose { kr.sweetapps.alcoholictimer.ads.AdController.removePolicyFetchListener(listener) }
    }

    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ads.AdController.isInterstitialShowingNow()
    // Observe reactive StateFlow from AdController so banner visibility updates reliably
    val isFullScreenAdShowing by kr.sweetapps.alcoholictimer.ads.AdController.fullScreenAdShowingFlow.collectAsState(
        initial = kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing()
    )
    val isBannerForceHidden by kr.sweetapps.alcoholictimer.ads.AdController.bannerForceHiddenFlow.collectAsState(initial = false)

    // Observe banner reload tick to retry loads immediately on demand
    val bannerReloadTick by kr.sweetapps.alcoholictimer.ads.AdController.bannerReloadTick.collectAsState(initial = 0L)

    // isFullScreenAdShowing 변경에 따라 adViewRef의 pause/resume 및 visibility를 안전하게 처리
    LaunchedEffect(isFullScreenAdShowing) {
        try {
            val view = adViewRef
            if (view != null) {
                if (isFullScreenAdShowing) {
                    try { view.pause() } catch (_: Throwable) {}
                    try { view.visibility = View.GONE } catch (_: Throwable) {}
                } else {
                    // If we are still in startup-hide mode, check whether a full-screen dismiss event occurred.
                    val lastDismiss = try { kr.sweetapps.alcoholictimer.ads.AdController.getLastFullScreenDismissedAt() } catch (_: Throwable) { 0L }
                    if (hideUntilFirstDismiss) {
                        if (lastDismiss > 0L) {
                            // First real full-screen dismiss observed -> reveal and load
                            hideUntilFirstDismiss = false
                            try {
                                if (!kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing()) try { view.visibility = View.VISIBLE } catch (_: Throwable) {}
                            } catch (_: Throwable) {}
                            // small delay to stabilize UI
                            kotlinx.coroutines.delay(200L)
                            try {
                                if (!kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing()) runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e -> Log.w(TAG, "initial show loadAd threw: ${e?.message}") }
                            } catch (_: Throwable) {}
                        } else {
                            // still waiting for first dismiss; keep hidden
                            try { view.visibility = View.GONE } catch (_: Throwable) {}
                        }
                    } else {
                        // normal behavior after initial dismiss: restore view and attempt load if needed
                        kotlinx.coroutines.delay(300L)
                        try { view.resume() } catch (_: Throwable) {}
                        try {
                            if (!kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing()) try { view.visibility = View.VISIBLE } catch (_: Throwable) {}
                        } catch (_: Throwable) {}
                        try {
                            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
                            val canRequestNow = (consentInfo?.canRequestAds() == true) || isDebugBuild()
                            if (!kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing() && isPolicyEnabledState.value && canRequestNow && !hasSuccessfulLoad && loadState is BannerLoadState.Loading) {
                                Log.d(TAG, "fullScreenHidden -> triggering immediate banner load")
                                runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e -> Log.w(TAG, "fullScreenHidden loadAd threw: ${e.message}") }
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    // Trigger immediate retry when bannerReloadTick updates (emitted by AdController)
    LaunchedEffect(bannerReloadTick) {
        val view = adViewRef
        if (view == null) return@LaunchedEffect
        try {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            val canRequestNow = (consentInfo?.canRequestAds() == true) || isDebugBuild()
            // Do not trigger banner load before we've seen the first full-screen dismiss event.
            if (hideUntilFirstDismiss || kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing() || isBannerForceHidden) {
                Log.d(TAG, "bannerReloadTick -> suppressed because full-screen present or initial hide or forceHidden=$isBannerForceHidden")
                return@LaunchedEffect
            }

            if (!kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing() && !isBannerForceHidden && isPolicyEnabledState.value && canRequestNow && !hasSuccessfulLoad && loadState is BannerLoadState.Loading) {
                Log.d(TAG, "bannerReloadTick -> triggering immediate banner load")
                runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e -> Log.w(TAG, "bannerReloadTick loadAd threw: ${e.message}") }
            } else {
                Log.d(TAG, "bannerReloadTick -> no load (policy=${isPolicyEnabledState.value} canRequest=$canRequestNow hasSuccess=$hasSuccessfulLoad state=$loadState)")
            }
        } catch (_: Throwable) {}
    }

    val shouldShowBanner = isPolicyEnabledState.value && !isInterstitialShowing && !isFullScreenAdShowing && !isBannerForceHidden
    // 예약 공간이 비활성 상태일 때도 배경이 흰색이 되도록 surface를 기본으로 사용합니다.
    val placeholderColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(shouldShowBanner) { Log.d(TAG, "banner visible=$shouldShowBanner h=$predictedHeight") }

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
                        try { visibility = View.GONE } catch (_: Throwable) {}

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
                        if (kr.sweetapps.alcoholictimer.ads.AdController.isFullScreenAdShowing() || isBannerForceHidden) {
                            if (adView.visibility != View.GONE) adView.visibility = View.GONE
                            try { adView.pause() } catch (_: Throwable) {}
                        } else {
                            if (adView.visibility != View.VISIBLE) adView.visibility = View.VISIBLE
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
                                        val lastDismiss = try { kr.sweetapps.alcoholictimer.ads.AdController.getLastFullScreenDismissedAt() } catch (_: Throwable) { 0L }
                                        if (hideUntilFirstDismiss && lastDismiss <= 0L) {
                                            try { view.visibility = View.GONE } catch (_: Throwable) {}
                                        } else {
                                            try { view.visibility = View.VISIBLE } catch (_: Throwable) {}
                                            try { view.resume() } catch (_: Throwable) {}
                                        }
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
                                        val lastDismiss = try { kr.sweetapps.alcoholictimer.ads.AdController.getLastFullScreenDismissedAt() } catch (_: Throwable) { 0L }
                                        if (hideUntilFirstDismiss && lastDismiss <= 0L) {
                                            try { view.visibility = View.GONE } catch (_: Throwable) {}
                                        } else {
                                            try { view.visibility = View.VISIBLE } catch (_: Throwable) {}
                                            try { view.resume() } catch (_: Throwable) {}
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {}
                    }
                    try { kr.sweetapps.alcoholictimer.ads.AdController.addFullScreenShowListener(fsListener) } catch (_: Throwable) {}
                    try { kr.sweetapps.alcoholictimer.ads.AdController.addBannerForceHiddenListener(bfListener) } catch (_: Throwable) {}
                    onDispose {
                        try { kr.sweetapps.alcoholictimer.ads.AdController.removeFullScreenShowListener(fsListener) } catch (_: Throwable) {}
                        try { kr.sweetapps.alcoholictimer.ads.AdController.removeBannerForceHiddenListener(bfListener) } catch (_: Throwable) {}
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

    // 초기 동의 대기 및 주기적 체크는 LaunchedEffect에서 유지
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

    LaunchedEffect(adViewRef, loadState) {
        val view = adViewRef ?: return@LaunchedEffect
        while (!hasSuccessfulLoad) {
            try {
                val failedDueToConsent = loadState is BannerLoadState.Failed && (loadState as BannerLoadState.Failed).code == -2
                if (failedDueToConsent) {
                    val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
                    val consentDebug = consentInfoDebug(consentInfo)
                    Log.d(TAG, "ConsentInfo(periodic): $consentDebug")
                    val publisherMisconfigured = consentDebug.contains("Publisher misconfiguration", ignoreCase = true) || consentDebug.contains("no form", ignoreCase = true)
                    if (publisherMisconfigured) {
                        Log.e(TAG, "Periodic check: UMP publisher misconfiguration still present; will not attempt loads until configuration fixed")
                    }
                    val canRequest = (consentInfo?.canRequestAds() == true) || isDebugBuild()
                    if (canRequest && !publisherMisconfigured) {
                        Log.d(TAG, "Periodic check: consent available -> retrying banner load")
                        loadState = BannerLoadState.Loading
                        runCatching { view.loadAd(AdRequestFactory.create(view.context)) }.onFailure { e -> Log.w(TAG, "Periodic loadAd threw: ${e.message}") }
                    }
                }
            } catch (_: Throwable) {}
            delay(5_000)
        }
    }

    // adView 리소스 해제
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
