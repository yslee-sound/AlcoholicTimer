package kr.sweetapps.alcoholictimer.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiConstants {
    val BackIconStartPadding: Dp = 42.dp // 42, 이건 제목을 오른쪽으로
    val BackIconTouchArea: Dp = 56.dp
    // 백 아이콘(시각적) 위치를 조절하는 내부 패딩: 아이콘을 터치영역 내부에서 더 왼쪽/오른쪽으로 이동시키려면 이 값을 변경하세요.
    val BackIconInnerPadding: Dp = 14.dp // 14 픽스

    // Layout-related constants (migrated from core.ui/LayoutConstants)
    val SCREEN_HORIZONTAL_PADDING: Dp = 10.dp
    val RECORDS_SCREEN_HORIZONTAL_PADDING: Dp = 10.dp
    val LEVEL_SCREEN_HORIZONTAL_PADDING: Dp = 10.dp

    val START_BRAND_TITLE_TOP_GAP: Dp = 12.dp
    val START_BRAND_TITLE_BOTTOM_GAP: Dp = 12.dp

    val FIRST_CARD_EXTERNAL_GAP: Dp = 10.dp
    val RECORDS_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val LEVEL_FIRST_CARD_EXTERNAL_GAP: Dp = 10.dp
    val RECORDS_TOP_SECTION_EXTERNAL_GAP: Dp = 10.dp

    val FIRST_CARD_TOP_INNER_PADDING: Dp = 50.dp
    val RECORDS_SELECTION_TO_PICKER_GAP: Dp = 8.dp
    val RECORDS_WEEK_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_MONTH_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_YEAR_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_ALL_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP

    val RUN_SCREEN_CARD_SPACING: Dp = 10.dp
    val RUN_STAT_CHIP_SPACING: Dp = 10.dp
    val RUN_TOP_CARD_HORIZONTAL_PADDING: Dp = 10.dp
    val RUN_TOP_CARD_VERTICAL_PADDING: Dp = 10.dp

    val CARD_SPACING: Dp = 20.dp
    val STAT_ROW_SPACING: Dp = 12.dp
    val CARD_CORNER_RADIUS: Dp = 20.dp
    val CARD_PADDING: Dp = 20.dp
    val BOTTOM_BUTTON_HORIZONTAL_PADDING: Dp = 120.dp

    val BANNER_MIN_HEIGHT: Dp = 64.dp
    val BANNER_TOP_GAP: Dp = 8.dp
    val BANNER_FIXED_HEIGHT: Dp = 64.dp
    val CLEARANCE_ABOVE_BUTTON: Dp = 32.dp
    val BUTTON_BOTTOM_OFFSET: Dp = 24.dp

    val RECORDS_STATS_INTERNAL_TOP_GAP: Dp = 12.dp
    val RECORDS_STATS_ROW_SPACING: Dp = 12.dp
    val RECORDS_CARD_IN_ROW_SPACING: Dp = 12.dp
    val RECORDS_CARD_HORIZONTAL_PADDING: Dp = 8.dp
}
