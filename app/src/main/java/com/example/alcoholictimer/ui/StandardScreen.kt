package com.example.alcoholictimer.ui

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
            .imePadding(), // 키패드 대응 추가
        verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
        content = content
    )
}

/**
 * 하단 버튼이 있는 화면용 레이아웃
 * 하단 버튼이 잘리지 않도록 적절한 패딩과 안전 영역을 제공
 * 키패드가 나타나면 자동으로 버튼이 위로 올라감
 */
@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    imePaddingEnabled: Boolean = true
) {
    // 모던한 그라데이션 배경
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
        // 상단 콘텐츠 영역 - 하단 버튼 공간을 훨씬 더 넉넉하게 확보
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    top = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    // 하단은 버튼 높이(120dp) + 여유 공간(120dp) = 240dp 확보
                    bottom = 240.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
            content = topContent
        )

        // 하단 버튼 - 화면 하단에서 훨씬 더 위쪽에 고정 배치
        // imePadding()으로 키패드가 나타나면 자동으로 위로 올라감
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    end = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    // 하단 패딩을 훨씬 더 넉넉하게 설정 (80dp)
                    bottom = 80.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            bottomButton()
        }
    }
}
