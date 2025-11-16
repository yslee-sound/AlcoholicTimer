package kr.sweetapps.alcoholictimer.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiConstants {
    val BackIconStartPadding: Dp = 42.dp // 42, 이건 제목을 오른쪽으로
    val BackIconTouchArea: Dp = 56.dp
    // 백 아이콘(시각적) 위치를 조절하는 내부 패딩: 아이콘을 터치영역 내부에서 더 왼쪽/오른쪽으로 이동시키려면 이 값을 변경하세요.
    val BackIconInnerPadding: Dp = 14.dp // 14 픽스

    // Layout-related constants (migrated from core.ui/LayoutConstants)
    val SCREEN_HORIZONTAL_PADDING: Dp = 12.dp // 12가 적당함
    val RECORDS_SCREEN_HORIZONTAL_PADDING: Dp = 10.dp
    val LEVEL_SCREEN_HORIZONTAL_PADDING: Dp = 20.dp
    // 첫번째(메인) 레벨 카드의 상단(Top) 외부 패딩을 조정할 수 있는 상수
    // 이 값을 통해 레벨 화면의 첫 카드와 상단 요소(상단 바 등) 사이의 간격을 제어합니다.
    val LEVEL_FIRST_CARD_TOP_PADDING: Dp = 20.dp

    // Level screen specific bottom padding (overrides the generic scroll bottom padding for the level screen)
    // Use this to adjust only the Level screen's final scroll gap without affecting other screens (e.g., Records).
    val LEVEL_SCREEN_BOTTOM_PADDING: Dp = 20.dp
    // Records screen specific bottom padding. Previously code used safePadding + 12.dp; expose
    // a single constant so the Records screen can be controlled from one place.
    val RECORDS_SCREEN_BOTTOM_PADDING: Dp = 20.dp

    val START_BRAND_TITLE_TOP_GAP: Dp = 12.dp
    val START_BRAND_TITLE_BOTTOM_GAP: Dp = 12.dp

    val FIRST_CARD_EXTERNAL_GAP: Dp = 20.dp
    val RECORDS_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val LEVEL_FIRST_CARD_EXTERNAL_GAP: Dp = 10.dp
    val RECORDS_TOP_SECTION_EXTERNAL_GAP: Dp = 10.dp

    val FIRST_CARD_TOP_INNER_PADDING: Dp = 50.dp
    val RECORDS_SELECTION_TO_PICKER_GAP: Dp = 8.dp
    val RECORDS_WEEK_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_MONTH_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_YEAR_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_ALL_FIRST_CARD_EXTERNAL_GAP: Dp = FIRST_CARD_EXTERNAL_GAP

    val RUN_STAT_CHIP_SPACING: Dp = 10.dp
    val RUN_TOP_CARD_HORIZONTAL_PADDING: Dp = 10.dp
    val RUN_TOP_CARD_VERTICAL_PADDING: Dp = 10.dp

    // Global vertical spacing between cards across the app.
    // Use this constant for any card-to-card vertical gap to ensure uniform spacing.
    // To change spacing for all cards update CARD_VERTICAL_SPACING here.
    val CARD_VERTICAL_SPACING: Dp = 15.dp // 12
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

    // 하단 내비게이션 아이콘 관련 크기 (일괄 조절용)
    val BOTTOM_NAV_ICON_SIZE: Dp = 28.dp // 아이콘 자체의 크기 (32)
    val BOTTOM_NAV_ITEM_SIZE: Dp = 35.dp // 아이콘을 감싸는 박스 (56)
    // 하단 내비게이션 바 전체 높이 (Surface 높이)
    val BOTTOM_NAV_BAR_HEIGHT: Dp = 60.dp // 아이콘 박스의 상하 크기 (80)
    // 레벨 화면 전용: 호스트가 예약(reserve)하는 하단 공간(예: BottomNavBar 높이)
    // 이 값은 레벨 화면에만 영향을 주며, 전역 BOTTOM_NAV_BAR_HEIGHT와 분리해 관리합니다.
    // 기본적으로 호스트의 바텀 내비 높이를 사용하도록 설정합니다. 필요시 화면별로 오버라이드하세요.
    val LEVEL_SCREEN_HOST_BOTTOM_RESERVE: Dp = BOTTOM_NAV_BAR_HEIGHT
    // 하단 내비 내 아이콘 간격: 아이콘 박스 간의 수평 간격을 조절합니다
    val BOTTOM_NAV_ITEM_GAP: Dp = 50.dp // 아이콘 사이의 간격 (중앙에서 펼치거나 좁힐 때 사용)
}
