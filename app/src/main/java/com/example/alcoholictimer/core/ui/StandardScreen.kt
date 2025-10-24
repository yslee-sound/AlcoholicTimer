package com.sweetapps.alcoholictimer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

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
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    imePaddingEnabled: Boolean = false,
    backgroundDecoration: @Composable BoxScope.() -> Unit = {},
    // 선택적 하단 광고 슬롯(버튼 아래, 화면 하단에 표시)
    bottomAd: (@Composable () -> Unit)? = null,
    // 광고가 없더라도 공간을 예약하여 버튼 위치를 동일하게 유지할지 여부(예: 금주 설정 화면)
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
    val adMinHeight = if (bottomAd != null || reserveSpaceForBottomAd) LayoutConstants.BANNER_MIN_HEIGHT else 0.dp

    // 콘텐츠 하단 여유: 버튼이 반쯤 겹치는 레이아웃 특성을 반영 + 버튼 아래 배너 영역까지 고려
    val reservedBottom = (buttonSize / 2) + buttonBottomGap + adTopGap + adMinHeight + effectiveBottom

    Box(
        modifier = rootModifier
    ) {
        // 배경 장식 레이어(워터마크 등)
        backgroundDecoration()

        // Centered column with max width constraint
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

        // 하단 광고: 화면 하단(시스템 바/IME 위)에 배치. 없으면 reserveSpaceForBottomAd가 true일 때 공간만 예약됨.
        if (bottomAd != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING)
                    .padding(bottom = effectiveBottom)
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = MaxContentWidth),
                contentAlignment = Alignment.Center
            ) {
                bottomAd()
            }
        }

        // 버튼: 광고 영역 위로 올려 배치
        val buttonBottomPadding = effectiveBottom + (adMinHeight + adTopGap) + buttonBottomGap
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING)
                .padding(bottom = buttonBottomPadding)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = MaxContentWidth),
            contentAlignment = Alignment.Center
        ) {
            bottomButton()
        }
    }
}
