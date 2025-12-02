// [NEW] Core UI 리팩토링: StandardScreen을 tab_01/components로 이동
package kr.sweetapps.alcoholictimer.ui.tab_01.components

import kr.sweetapps.alcoholictimer.BuildConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding

private val MaxContentWidth: Dp = 600.dp


@Composable
fun predictAnchoredBannerHeightDp(): Dp {
    // Use the fixed banner placeholder height to avoid UI shifts when ad size becomes available.
    return UiConstants.BANNER_FIXED_HEIGHT
}

// NOTE: single implementation of StandardScreenWithBottomButton retained.
@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    imePaddingEnabled: Boolean = false,
    // When true, ignore WindowInsets.ime in layout calculations (useful when showing IME without moving UI)
    ignoreImeInsets: Boolean = false,
    backgroundDecoration: @Composable BoxScope.() -> Unit = {},
    bottomAd: (@Composable () -> Unit)? = null,
    reserveSpaceForBottomAd: Boolean = false,
    // Allow callers to override the top padding applied to the content area for per-screen control.
    topPadding: Dp = UiConstants.FIRST_CARD_EXTERNAL_GAP,
    // Allow callers to override horizontal padding applied to both sides (default preserved)
    horizontalPadding: Dp = UiConstants.SCREEN_HORIZONTAL_PADDING,
    // Allow callers to override the content max width (default 600.dp)
    contentMaxWidth: Dp = MaxContentWidth,
    // If true, inner content Column uses fillMaxWidth() instead of centering with a max width.
    forceFillMaxWidth: Boolean = false,
    // Optional background color for the screen. If null, the default surface variant color is used.
    screenBackground: Color? = null,
    // New: per-screen vertical spacing between cards
    cardVerticalSpacing: Dp = UiConstants.CARD_VERTICAL_SPACING
) {
    // banner visibility handled externally
    // Ensure debug-only hiding uses BuildConfig.DEBUG guard per release validation
    var shouldHideBanner by remember { mutableStateOf(if (BuildConfig.DEBUG) false else false) }

    val effectiveBottomAd = if (shouldHideBanner) null else bottomAd

    val rootModifier = Modifier
        .fillMaxSize()
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)
        .background(screenBackground ?: MaterialTheme.colorScheme.surfaceVariant)

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val navBarBottom = navBarPaddingValues.calculateBottomPadding()
    val imeBottom = if (ignoreImeInsets) 0.dp else WindowInsets.ime.asPaddingValues().calculateBottomPadding()
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

    val isPreview = LocalInspectionMode.current

    // In Preview we want to avoid reserving large bottom space and disable scroll so
    // designers can see the full content without interacting with the Preview.
    val previewReservedBottom = if (isPreview) 0.dp else reservedBottom
    val useScroll = !isPreview

    Box(modifier = rootModifier) {
        backgroundDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = topPadding,
                    bottom = previewReservedBottom
                )
                .then(if (useScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(cardVerticalSpacing)
        ) {
            val innerColumnModifier = if (forceFillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = contentMaxWidth)
            }

            Column(
                modifier = innerColumnModifier,
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

        // Use the same horizontalPadding as the content area so bottom button aligns with cards.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = buttonBottomPadding)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = MaxContentWidth),
            contentAlignment = Alignment.Center
        ) { bottomButton() }
    }
}
