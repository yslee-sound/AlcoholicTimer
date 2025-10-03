package com.example.alcoholictimer.core.ui

import androidx.compose.ui.unit.dp

/**
 * 앱 전체에서 사용하는 레이아웃 상수들
 */
object LayoutConstants {
    // 화면 가로 패딩
    val SCREEN_HORIZONTAL_PADDING = 16.dp

    // 카드 간격
    val CARD_SPACING = 16.dp

    // 통계 행 간격(Detail/Quit 공용)
    val STAT_ROW_SPACING = 12.dp

    // 카드 공통 스타일
    val CARD_CORNER_RADIUS = 20.dp
    val CARD_PADDING = 20.dp

    // 하단 버튼 가로 패딩 (화면 가장자리에서 떨어진 거리)
    val BOTTOM_BUTTON_HORIZONTAL_PADDING = 32.dp

    // 하단 버튼 세로 패딩 (화면 하단에서 떨어진 거리) - 더 넉넉하게 설정
    val BOTTOM_BUTTON_VERTICAL_PADDING = 32.dp

    // 버튼 사이 간격 (QuitActivity의 2개 버튼용)
    val BUTTON_SPACING = 24.dp
}
