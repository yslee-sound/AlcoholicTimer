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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.google.android.gms.ads.AdSize
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val MaxContentWidth: Dp = 600.dp


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
    // DebugAdHelper removed: banner is controlled remotely; always show by default
    var shouldHideBanner by remember { mutableStateOf(false) }

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

    // 버튼과 콘텐츠 사이 여유: 버튼 전체 높이 + 추가 여백(중첩 방지)
    val buttonSize = 96.dp
    val adTopGap = LayoutConstants.BANNER_TOP_GAP

    // 모든 화면에서 동일한 버튼 위치를 보장하기 위해 예측 높이를 사용해 공간 예약
    val predictedBannerH = LayoutConstants.BANNER_FIXED_HEIGHT
    val reservedBannerH = if (effectiveBottomAd != null || reserveSpaceForBottomAd) predictedBannerH else 0.dp

    val insetForReservation = if (globalSafeBottom > 0.dp) 0.dp else effectiveBottomAdjusted
    val buttonBottomPadding by remember(reservedBannerH, adTopGap, insetForReservation) {
        mutableStateOf(insetForReservation + (reservedBannerH + adTopGap) + LayoutConstants.BUTTON_BOTTOM_OFFSET)
    }

    // 콘텐츠 하단 보호 패딩: 버튼 상단과 마지막 카드 사이 여유를 버튼 반높이 + CLEARANCE 로 보장
    val reservedBottom by remember(buttonSize, buttonBottomPadding) {
        mutableStateOf(buttonBottomPadding + (buttonSize / 2) + LayoutConstants.CLEARANCE_ABOVE_BUTTON)
    }

    Box(modifier = rootModifier) {
        backgroundDecoration()

        // 스크롤 가능한 콘텐츠 컬럼 (작은 화면에서 잘림 방지)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    top = LayoutConstants.FIRST_CARD_EXTERNAL_GAP,
                    bottom = reservedBottom
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = MaxContentWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
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

        // 버튼 배치 (계산된 padding 사용)
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
