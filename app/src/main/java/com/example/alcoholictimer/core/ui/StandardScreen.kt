package com.example.alcoholictimer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.alcoholictimer.R

@Composable
fun StandardScreen(
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            colorResource(id = R.color.color_bg_gradient_start),
            colorResource(id = R.color.color_bg_gradient_mid),
            colorResource(id = R.color.color_bg_gradient_end)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(LayoutConstants.SCREEN_HORIZONTAL_PADDING)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
        content = content
    )
}

@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    imePaddingEnabled: Boolean = true
) {
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            colorResource(id = R.color.color_bg_gradient_start),
            colorResource(id = R.color.color_bg_gradient_mid),
            colorResource(id = R.color.color_bg_gradient_end)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    val rootModifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)

    Box(
        modifier = rootModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    top = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    bottom = 240.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
            content = topContent
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    end = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    bottom = 80.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            bottomButton()
        }
    }
}

