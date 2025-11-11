package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.ui.unit.dp

object LayoutConstants {
    val SCREEN_HORIZONTAL_PADDING = 16.dp
    val CARD_SPACING = 16.dp
    val STAT_ROW_SPACING = 12.dp
    val CARD_CORNER_RADIUS = 20.dp
    val CARD_PADDING = 20.dp
    val BOTTOM_BUTTON_HORIZONTAL_PADDING = 32.dp
    val BOTTOM_BUTTON_VERTICAL_PADDING = 32.dp
    val BUTTON_SPACING = 24.dp

    // 배너 광고 관련: Anchored Adaptive 최소 높이는 보통 50dp (전화기 기준)
    val BANNER_MIN_HEIGHT = 50.dp
    // 전역 배너 위 간격 — 기본 8dp로 완충(기존 0dp)
    val BANNER_TOP_GAP = 8.dp
    // 고정 배너 높이: 안정적 레이아웃을 위해 한 번 결정된 높이를 모든 화면에서 사용
    // Anchored Adaptive가 대부분 50~70dp 사이 → 약간 넉넉하게 64dp 고정
    val BANNER_FIXED_HEIGHT = 64.dp
}
