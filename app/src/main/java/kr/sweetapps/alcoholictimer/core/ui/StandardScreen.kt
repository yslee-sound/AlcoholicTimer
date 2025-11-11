package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.google.android.gms.ads.AdSize
import androidx.compose.material3.Surface

private val MaxContentWidth: Dp = 600.dp

@Composable
fun StandardScreen(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(LayoutConstants.SCREEN_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
        content = content
    )
}

@Composable
fun predictAnchoredBannerHeightDp(): Dp {
    val context = LocalContext.current
    val conf = LocalConfiguration.current
    val density = LocalDensity.current
    // 컨테이너는 화면 풀폭으로 사용하므로 screenWidthDp 전체 사용
    val availableWidthDp = conf.screenWidthDp
    return try {
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
        with(density) { adSize.getHeightInPixels(context).toDp() }.coerceAtLeast(LayoutConstants.BANNER_MIN_HEIGHT)
    } catch (_: Throwable) {
        LayoutConstants.BANNER_MIN_HEIGHT
    }
}

@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    imePaddingEnabled: Boolean = false,
    backgroundDecoration: @Composable BoxScope.() -> Unit = {},
    bottomAd: (@Composable () -> Unit)? = null,
    reserveSpaceForBottomAd: Boolean = false,
    showDebugOverlay: Boolean = false
) {
    // 디버그 모드에서만 배너 숨김 상태 확인 (릴리즈에서는 항상 false)
    var shouldHideBanner by remember { mutableStateOf(if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) DebugAdHelper.bannerHiddenFlow.value else false) }

    // Flow 변경사항을 LaunchedEffect로 명시적으로 구독 (디버그 빌드에서만)
    if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            DebugAdHelper.bannerHiddenFlow.collect { hidden ->
                android.util.Log.e("StandardScreen", "Flow collected: hidden=$hidden")
                shouldHideBanner = hidden
            }
        }
    }

    val effectiveBottomAd = if (shouldHideBanner) null else bottomAd

    android.util.Log.e("StandardScreen", "StandardScreenWithBottomButton: shouldHideBanner=$shouldHideBanner, bottomAd=${bottomAd != null}, effectiveBottomAd=${effectiveBottomAd != null}")

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val navBarBottom = navBarPaddingValues.calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val effectiveBottom = if (navBarBottom > imeBottom) navBarBottom else imeBottom
    // Global safe padding provided by BaseActivity (may be 0 when manageBottomAreaExternally=true)
    val globalSafeBottom = LocalSafeContentPadding.current.calculateBottomPadding()
    // Use the larger of effectiveBottom and globalSafeBottom to avoid double-counting
    val effectiveBottomAdjusted = if (effectiveBottom > globalSafeBottom) effectiveBottom else globalSafeBottom

    val buttonSize = 96.dp
    val buttonBottomGap = 24.dp
    val adTopGap = LayoutConstants.BANNER_TOP_GAP

    // 모든 화면에서 동일한 버튼 위치를 보장하기 위해 예측 높이를 사용해 공간 예약
    val predictedBannerH = LayoutConstants.BANNER_FIXED_HEIGHT
    val reservedBannerH = if (effectiveBottomAd != null || reserveSpaceForBottomAd) predictedBannerH else 0.dp

    // Compute a minimal content bottom padding so the main content doesn't overlap the floating button.
    // The content should NOT reserve the banner + system inset space; that space is handled by the
    // banner container and the button positioning. This avoids double-reserving the bottom area.
    val reservedBottom by remember(buttonSize, buttonBottomGap, adTopGap) {
        mutableStateOf((buttonSize / 2) + buttonBottomGap + adTopGap)
    }

    SideEffect {
        try {
            android.util.Log.e("StandardScreen", "computed: effectiveBottom=$effectiveBottom, globalSafeBottom=$globalSafeBottom, effectiveBottomAdjusted=$effectiveBottomAdjusted, reservedBannerH=$reservedBannerH, reservedBottom=$reservedBottom")
        } catch (_: Throwable) {}
    }

    Box(modifier = rootModifier) {
        backgroundDecoration()

        // Debug overlay: show computed padding values on-screen for quick verification
        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG && showDebugOverlay) {
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .align(Alignment.TopEnd),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "rb=${reservedBottom}, gb=${globalSafeBottom}, eb=${effectiveBottomAdjusted}, bh=${reservedBannerH}",
                    modifier = Modifier.padding(6.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    top = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    bottom = reservedBottom
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = MaxContentWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
                content = topContent
            )
        }

        if (effectiveBottomAd != null) {
            // 배너 상단 간격을 회색으로 채워 구분감 부여
            if (adTopGap > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(adTopGap)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            // 배너 상단 헤어라인
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                thickness = AppBorder.Hairline,
                color = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.White,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                val bannerBottomPadding = if (globalSafeBottom > 0.dp) globalSafeBottom else effectiveBottomAdjusted
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bannerBottomPadding)
                        .height(predictedBannerH),
                    contentAlignment = Alignment.Center
                ) { effectiveBottomAd() }
            }
        }

        val insetForReservation = if (globalSafeBottom > 0.dp) 0.dp else effectiveBottomAdjusted
        val buttonBottomPadding by remember(reservedBannerH, adTopGap, insetForReservation) {
            mutableStateOf(insetForReservation + (reservedBannerH + adTopGap) + buttonBottomGap)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING)
                .padding(bottom = buttonBottomPadding)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = MaxContentWidth),
            contentAlignment = Alignment.Center
        ) { bottomButton() }
    }
}
