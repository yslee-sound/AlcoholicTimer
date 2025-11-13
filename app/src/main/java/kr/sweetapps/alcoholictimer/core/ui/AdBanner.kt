package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

private sealed class BannerLoadState { object Loading: BannerLoadState(); object Success: BannerLoadState(); data class Failed(val code:Int, val message:String): BannerLoadState() }

/**
 * Admob 배너 컴포저블.
 *
 * 정책(원격 설정, 사용자 동의 거부, 내부 AdPolicy, 프리미엄 결제, 나이/민감 카테고리 제한 등)으로 인해
 * 실제 광고 로드 자체를 시도하지 말아야 하는 경우 배너를 "정책상 숨김" 처리한다고 표현합니다.
 * 즉, 광고 SDK에 불필요한 요청을 하지 않고 UI 레벨에서만 배너가 없는 형태로 표시합니다.
 *
 * reserveSpaceWhenDisabled=true 로 주면 정책상 숨김이어도(광고 비활성) 동일한 높이 placeholder 를 유지하여
 * 화면 전환/동적 토글 시 레이아웃 점프(위·아래 콘텐츠 밀림)를 방지할 수 있습니다.
 */
@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    reserveSpaceWhenDisabled: Boolean = true, // 항상 공간 확보 기본값으로 변경
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 화면 폭(dp)
    val screenWidthDp = configuration.screenWidthDp

    // Adaptive Anchored 예측 높이 (비활성화라도 placeholder 용으로 필요할 수 있어 선계산)
    val predictedHeight: Dp = remember(screenWidthDp) {
        try {
            val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            with(density) { adaptive.getHeightInPixels(context).toDp() }
        } catch (_: Throwable) { 50.dp }
    }

    // 정책/동의 등으로 실제 광고 노출 여부
    val isBannerEnabled = kr.sweetapps.alcoholictimer.ads.AdController.isBannerEnabled()
    LaunchedEffect(isBannerEnabled) { Log.d(TAG, "🔍 Banner enabled: $isBannerEnabled (reserveSpaceWhenDisabled=$reserveSpaceWhenDisabled)") }

    if (!isBannerEnabled) {
        if (reserveSpaceWhenDisabled) {
            // 공간만 확보 (시각적 위치 안정성)
            Box(modifier = modifier.fillMaxWidth().height(predictedHeight))
        } else {
            // 완전 제거
            Box(modifier = modifier.fillMaxWidth())
        }
        return
    }

    // 로컬 상태들
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var loadState by remember { mutableStateOf<BannerLoadState>(BannerLoadState.Loading) }
    var realHeight by remember { mutableStateOf(predictedHeight) }
    var retryCount by remember { mutableStateOf(0) }
    val maxRetry = 3
    val retryDelays = listOf(2000L, 5000L, 10000L) // ms
    var hasSuccessfulLoad by remember { mutableStateOf(false) }

    // 높이 애니메이션 목표 계산 (성공 후 더 크게만 확장, 축소는 별도 Effect 로 처리)
    val targetHeight = remember(predictedHeight, realHeight) {
        if (realHeight > predictedHeight + 1.dp) realHeight else predictedHeight
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "bannerHeightAnim"
    )

    Box(modifier = modifier.fillMaxWidth().height(animatedHeight)) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val resolvedUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                    val unitId = if (resolvedUnitId.isNullOrBlank() || resolvedUnitId.contains("REPLACE_WITH_REAL_BANNER")) {
                        "ca-app-pub-3940256099942544/6300978111"
                    } else resolvedUnitId
                    setAdUnitId(unitId)

                    try {
                        val adaptive = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp)
                        setAdSize(adaptive)
                    } catch (_: Throwable) {
                        try { setAdSize(AdSize.BANNER) } catch (ignored: Throwable) { Log.w(TAG, "Fallback setAdSize failed: ${ignored.message}") }
                    }

                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d(TAG, "Banner onAdLoaded (retryCount=$retryCount)")
                            hasSuccessfulLoad = true
                            runCatching {
                                val hPx = adSize?.getHeightInPixels(ctx) ?: 0
                                val hDp = with(density) { hPx.toDp() }
                                if (hDp > 0.dp) realHeight = hDp
                            }
                            loadState = BannerLoadState.Success
                        }
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            val msg = error.message ?: "error"
                            Log.w(TAG, "Banner onAdFailedToLoad code=${error.code} message=$msg retryCount=$retryCount")
                            loadState = BannerLoadState.Failed(error.code, msg)
                            if (!hasSuccessfulLoad && retryCount < maxRetry) {
                                retryCount += 1
                            }
                        }
                    }
                    adViewRef = this
                }
            },
            update = { adView ->
                // 최초/재시도 로드 조건: 성공 전 && 현재 Loading/Fault 상태 && 동의 가능 && 아직 재시도 직후 대기 아님
                val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(adView.context) }.getOrNull()
                val canRequest = consentInfo?.canRequestAds() == true
                if (canRequest && !hasSuccessfulLoad && loadState is BannerLoadState.Loading) {
                    Log.d(TAG, "Issuing initial banner loadAd")
                    runCatching { adView.loadAd(AdRequest.Builder().build()) }
                }
            }
        )

        when (loadState) {
            BannerLoadState.Loading -> Box(
                Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(strokeWidth = 2.dp) }
            is BannerLoadState.Failed -> Box(
                Modifier.matchParentSize().background(Color(0xFFECECEC)),
                contentAlignment = Alignment.Center
            ) { Text("배너 로딩 실패 재시도 중(${retryCount}/${maxRetry})", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray) }
            BannerLoadState.Success -> Unit
        }
    }

    // 재시도 루프 (실패했을 때만 동작)
    LaunchedEffect(retryCount, loadState) {
        if (retryCount in 1..maxRetry && !hasSuccessfulLoad) {
            // 직전 실패 후 대기 → Loading 으로 전환하여 update 블록이 다시 load 하지 않도록 방지, 직접 요청
            val delayMs = retryDelays.getOrElse(retryCount - 1) { 10000L }
            Log.d(TAG, "Retry #$retryCount scheduled in ${delayMs}ms")
            delay(delayMs)
            val view = adViewRef
            if (view != null) {
                val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
                val canRequest = consentInfo?.canRequestAds() == true
                if (canRequest && !hasSuccessfulLoad) {
                    loadState = BannerLoadState.Loading
                    runCatching { view.loadAd(AdRequest.Builder().build()) }.onFailure { e ->
                        val msg = e.message ?: "error"
                        Log.w(TAG, "Retry loadAd threw: $msg")
                        loadState = BannerLoadState.Failed(-3, msg)
                    }
                }
            }
        }
    }

    // 동의 지연 폴백 (최초 시나리오): 기존 로직 단순화 → 동의 가능해질 때까지 15초(1s 간격) 폴링 후 자동 로드
    LaunchedEffect(adViewRef) {
        val view = adViewRef ?: return@LaunchedEffect
        var attempts = 0
        while (!hasSuccessfulLoad && loadState is BannerLoadState.Loading && attempts < 15) {
            val consentInfo = runCatching { UserMessagingPlatform.getConsentInformation(view.context) }.getOrNull()
            val canRequest = consentInfo?.canRequestAds() == true
            if (canRequest) {
                Log.d(TAG, "Consent became available in fallback loop; issuing loadAd")
                runCatching { view.loadAd(AdRequest.Builder().build()) }
                break
            }
            attempts++
            delay(1000)
        }
        if (!hasSuccessfulLoad && loadState is BannerLoadState.Loading && attempts >= 15) {
            loadState = BannerLoadState.Failed(-2, "consent timeout")
        }
    }

    // AdView 자원 정리 (메모리 누수 방지)
    DisposableEffect(adViewRef) {
        onDispose {
            try { adViewRef?.destroy(); Log.d(TAG, "AdView destroyed") } catch (_: Throwable) {}
        }
    }
}
