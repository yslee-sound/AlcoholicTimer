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
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kr.sweetapps.alcoholictimer.constants.UiConstants

private val MaxContentWidth: Dp = 600.dp


@Composable
fun predictAnchoredBannerHeightDp(): Dp {
    // Use the fixed banner placeholder height to avoid UI shifts when ad size becomes available.
    return UiConstants.BANNER_FIXED_HEIGHT
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
    // banner visibility handled externally
    var shouldHideBanner by remember { mutableStateOf(false) }

    val effectiveBottomAd = if (shouldHideBanner) null else bottomAd

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val navBarBottom = navBarPaddingValues.calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val effectiveBottom = if (navBarBottom > imeBottom) navBarBottom else imeBottom
    val globalSafeBottom = LocalSafeContentPadding.current.calculateBottomPadding()
    val effectiveBottomAdjusted = if (effectiveBottom > globalSafeBottom) effectiveBottom else globalSafeBottom

    val buttonSize = 96.dp
    val adTopGap = UiConstants.BANNER_TOP_GAP

    val predictedBannerH = UiConstants.BANNER_FIXED_HEIGHT
    val reservedBannerH = if (effectiveBottomAd != null || reserveSpaceForBottomAd) predictedBannerH else 0.dp

    val insetForReservation = if (globalSafeBottom > 0.dp) 0.dp else effectiveBottomAdjusted
    val buttonBottomPadding by remember(reservedBannerH, adTopGap, insetForReservation) {
        mutableStateOf(insetForReservation + (reservedBannerH + adTopGap) + UiConstants.BUTTON_BOTTOM_OFFSET)
    }

    val reservedBottom by remember(buttonSize, buttonBottomPadding) {
        mutableStateOf(buttonBottomPadding + (buttonSize / 2) + UiConstants.CLEARANCE_ABOVE_BUTTON)
    }

    Box(modifier = rootModifier) {
        backgroundDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = UiConstants.SCREEN_HORIZONTAL_PADDING,
                    end = UiConstants.SCREEN_HORIZONTAL_PADDING,
                    top = UiConstants.FIRST_CARD_EXTERNAL_GAP,
                    bottom = reservedBottom
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiConstants.CARD_SPACING)
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
            if (adTopGap > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(adTopGap)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = UiConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING)
                .padding(bottom = buttonBottomPadding)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = MaxContentWidth),
            contentAlignment = Alignment.Center
        ) { bottomButton() }
    }
}
