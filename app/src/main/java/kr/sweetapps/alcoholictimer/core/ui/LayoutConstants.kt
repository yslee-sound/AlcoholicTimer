package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.ui.unit.dp

object LayoutConstants {
    val SCREEN_HORIZONTAL_PADDING = 10.dp

    // 공통 외부 간격(첫 카드)
    val FIRST_CARD_EXTERNAL_GAP = 10.dp // 시작/진행/기록 통계 기본값
    // Records 기본 외부 간격(하위 탭별 값의 기본값으로 사용) — 호환성 alias
    val RECORDS_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    // 레벨 화면 상단 여백(공통과 독립)
    val LEVEL_FIRST_CARD_EXTERNAL_GAP = 10.dp
    // 기록 화면 상단(탭 카드) 외부 간격
    val RECORDS_TOP_SECTION_EXTERNAL_GAP = 10.dp

    // 시작 화면 내부 패딩(첫 카드 상단 내부)
    val FIRST_CARD_TOP_INNER_PADDING = 40.dp

    // 기록 화면: 탭 카드와 기간 선택 카드 사이 간격
    val RECORDS_SELECTION_TO_PICKER_GAP = 8.dp

    // 기록 화면: 기간별 첫 통계 카드 외부 간격
    val RECORDS_WEEK_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_MONTH_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_YEAR_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_ALL_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP

    // Run 화면 전용: 상단 3개 칩(Row) 간 간격 및 카드 내부 패딩
    val RUN_SCREEN_CARD_SPACING = 10.dp // Run 화면 카드간의 위아래 간격
    val RUN_STAT_CHIP_SPACING = 10.dp // 3-chip 칩과 칩사이의 간격
    val RUN_TOP_CARD_HORIZONTAL_PADDING = 10.dp // chip 외부 가로 패딩
    val RUN_TOP_CARD_VERTICAL_PADDING = 10.dp // chip 외부 세로 패딩

    // 공통 UI 토큰
    val CARD_SPACING = 20.dp
    val STAT_ROW_SPACING = 12.dp
    val CARD_CORNER_RADIUS = 20.dp
    val CARD_PADDING = 20.dp
    val BOTTOM_BUTTON_HORIZONTAL_PADDING = 120.dp

    // 배너 관련
    val BANNER_MIN_HEIGHT = 64.dp
    val BANNER_TOP_GAP = 8.dp
    val BANNER_FIXED_HEIGHT = 64.dp
    val CLEARANCE_ABOVE_BUTTON = 32.dp
    val BUTTON_BOTTOM_OFFSET = 24.dp




    // 기록 통계 내부 간격
    val RECORDS_STATS_INTERNAL_TOP_GAP = 12.dp
    val RECORDS_STATS_ROW_SPACING = 12.dp
    val RECORDS_CARD_IN_ROW_SPACING = 12.dp
    val RECORDS_CARD_HORIZONTAL_PADDING = 8.dp
}
