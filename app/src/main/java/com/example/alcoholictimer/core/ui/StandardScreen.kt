package com.sweetapps.alcoholictimer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
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
private fun predictAnchoredBannerHeightDp(): Dp {
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
    reserveSpaceForBottomAd: Boolean = false
) {
    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val navBarBottom = navBarPaddingValues.calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val effectiveBottom = if (navBarBottom > imeBottom) navBarBottom else imeBottom

    val buttonSize = 96.dp
    val buttonBottomGap = 24.dp
    val adTopGap = LayoutConstants.BANNER_TOP_GAP

    // 모든 화면에서 동일한 버튼 위치를 보장하기 위해 예측 높이를 사용해 공간 예약
    val predictedBannerH = predictAnchoredBannerHeightDp()
    val reservedBannerH = if (bottomAd != null || reserveSpaceForBottomAd) predictedBannerH else 0.dp

    val reservedBottom by remember(reservedBannerH, adTopGap, effectiveBottom) {
        mutableStateOf((buttonSize / 2) + buttonBottomGap + adTopGap + reservedBannerH + effectiveBottom)
    }

    Box(modifier = rootModifier) {
        backgroundDecoration()

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

        if (bottomAd != null) {
            // 원복: 상단 Divider 없이, 기존 Surface 컨테이너만 사용
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.White,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = effectiveBottom)
                        .heightIn(min = LayoutConstants.BANNER_MIN_HEIGHT),
                    contentAlignment = Alignment.Center
                ) { bottomAd() }
            }
        }

        val buttonBottomPadding by remember(reservedBannerH, adTopGap, effectiveBottom) {
            mutableStateOf(effectiveBottom + (reservedBannerH + adTopGap) + buttonBottomGap)
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
