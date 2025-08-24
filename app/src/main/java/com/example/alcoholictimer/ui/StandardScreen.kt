package com.example.alcoholictimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 모든 Activity에서 사용할 표준 화면 레이아웃
 */
@Composable
fun StandardScreen(
    content: @Composable ColumnScope.() -> Unit
) {
    // 모던한 그라데이션 배경 (모든 화면 동일)
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE3F2FD),
            Color(0xFFF1F8E9)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(LayoutConstants.SCREEN_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
        content = content
    )
}

/**
 * 하단 버튼이 있는 화면용 레이아웃
 */
@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit
) {
    // 모던한 그라데이션 배경
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE3F2FD),
            Color(0xFFF1F8E9)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(LayoutConstants.SCREEN_HORIZONTAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 콘텐츠 (가변 크기)
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
                content = topContent
            )
        }

        // 하단 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    end = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    bottom = LayoutConstants.BOTTOM_BUTTON_VERTICAL_PADDING
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomButton()
        }
    }
}
