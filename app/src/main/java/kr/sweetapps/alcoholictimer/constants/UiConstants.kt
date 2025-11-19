package kr.sweetapps.alcoholictimer.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiConstants {
    val BackIconStartPadding: Dp = 42.dp // 42, 이건 제목을 오른쪽으로
    val BackIconTouchArea: Dp = 56.dp
    // 백 아이콘(시각적) 위치를 조절하는 내부 패딩: 아이콘을 터치영역 내부에서 더 왼쪽/오른쪽으로 이동시키려면 이 값을 변경하세요.
    val BackIconInnerPadding: Dp = 14.dp // 14 픽스

    // Layout-related constants (migrated from core.ui/LayoutConstants)
    val SCREEN_HORIZONTAL_PADDING: Dp = 15.dp           // 1번 화면
    val FIRST_CARD_EXTERNAL_GAP: Dp = 15.dp             // 1번 화면
    val CARD_VERTICAL_SPACING: Dp = 15.dp // 12
    val STAT_ROW_SPACING: Dp = 12.dp
    val CARD_CORNER_RADIUS: Dp = 20.dp
    val CARD_PADDING: Dp = 20.dp

    val BANNER_TOP_GAP: Dp = 8.dp
    val BANNER_FIXED_HEIGHT: Dp = 64.dp
    val CLEARANCE_ABOVE_BUTTON: Dp = 32.dp
    val BUTTON_BOTTOM_OFFSET: Dp = 24.dp

    // Bottom navigation sizes (used in BottomNavBar)
    val BOTTOM_NAV_ICON_SIZE: Dp = 28.dp // 아이콘 자체 크기
    val BOTTOM_NAV_ITEM_SIZE: Dp = 35.dp // 아이콘을 감싸는 박스 크기
    val BOTTOM_NAV_BAR_HEIGHT: Dp = 60.dp // 바 전체 높이
    val BOTTOM_NAV_ITEM_GAP: Dp = 50.dp // 아이템 간격

    // (Records-specific constants migrated into RecordsScreen.kt)
}
