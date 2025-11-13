package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.ui.unit.dp

object LayoutConstants {
    val SCREEN_HORIZONTAL_PADDING = 10.dp


    val FIRST_CARD_EXTERNAL_GAP = 10.dp // # 시작 -> 진행 -> 종료 의 첫카드
    val LEVEL_FIRST_CARD_EXTERNAL_GAP = 10.dp // # 레벨 화면
    val RECORDS_TOP_SECTION_EXTERNAL_GAP = 10.dp // - 배너/상단으로부터 탭 카드까지의 외부 간격

    // 시작 화면


    @Deprecated("Use FIRST_CARD_EXTERNAL_GAP instead")
    val HEADER_TO_FIRST_CARD_GAP = FIRST_CARD_EXTERNAL_GAP
    // 첫 카드 내부 상단 패딩(콘텐츠와 카드 상단 테두리 간격) - 필요 시 줄여 시각적 상단 밀착 효과
    val FIRST_CARD_TOP_INNER_PADDING = 40.dp // 12.dp, start 화면의 대형 카드

    // 화면별 커스터마이즈 가능 상수 추가 (초기값은 공통값과 동일하게 설정)
    val START_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    val RUN_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    val RECORDS_FIRST_CARD_EXTERNAL_GAP = FIRST_CARD_EXTERNAL_GAP
    // 레벨 화면은 공통값과 독립적으로 조절할 수 있도록 별도의 기본값 사용


    // Records 화면 최상단 섹션(탭 카드) 상단 여백과 내부 간격 제어용 상수
    // - 탭 카드와 아래 기간 선택 카드(드롭다운) 사이 간격
    val RECORDS_SELECTION_TO_PICKER_GAP = 8.dp

    // Records 화면: 기간(주/월/년/전체)별 첫 카드 외부(top) 간격 개별 제어 (통계 카드에 적용)
    val RECORDS_WEEK_FIRST_CARD_EXTERNAL_GAP = RECORDS_FIRST_CARD_EXTERNAL_GAP
    val RECORDS_MONTH_FIRST_CARD_EXTERNAL_GAP = RECORDS_FIRST_CARD_EXTERNAL_GAP
    val RECORDS_YEAR_FIRST_CARD_EXTERNAL_GAP = RECORDS_FIRST_CARD_EXTERNAL_GAP
    val RECORDS_ALL_FIRST_CARD_EXTERNAL_GAP = RECORDS_FIRST_CARD_EXTERNAL_GAP

    val RECORDS_FIRST_CARD_TOP_INNER_PADDING = FIRST_CARD_TOP_INNER_PADDING
    val LEVEL_FIRST_CARD_TOP_INNER_PADDING = FIRST_CARD_TOP_INNER_PADDING

    val CARD_SPACING = 20.dp
    val STAT_ROW_SPACING = 12.dp
    val CARD_CORNER_RADIUS = 20.dp
    val CARD_PADDING = 20.dp
    val BOTTOM_BUTTON_HORIZONTAL_PADDING = 32.dp
    val BOTTOM_BUTTON_VERTICAL_PADDING = 32.dp
    val BUTTON_SPACING = 24.dp

    // 배너 광고 관련: Anchored Adaptive 배너 높이는 보통 64~70dp
    // 로딩 중에도 충분한 공간을 확보하여 배너가 커지는 느낌 방지
    val BANNER_MIN_HEIGHT = 64.dp
    // 전역 배너 위 간격 — 기본 8dp로 완충(기존 0dp)
    val BANNER_TOP_GAP = 8.dp
    // 고정 배너 높이: 안정적 레이아웃을 위해 한 번 결정된 높이를 모든 화면에서 사용
    // Anchored Adaptive가 대부분 50~70dp 사이 → 64dp 고정
    val BANNER_FIXED_HEIGHT = 64.dp

    // 버튼 위쪽으로 콘텐츠가 확보할 안전 여유(버튼 상단과 마지막 카드 사이)
    // 사진 기준으로 24~32dp가 자연스러워 32dp로 고정
    val CLEARANCE_ABOVE_BUTTON = 32.dp
    // 버튼을 하단(인셋/배너)에서 띄우는 간격
    val BUTTON_BOTTOM_OFFSET = 24.dp

    // Run 화면 카드 간격: 10dp로 고정 (상단 외부 간격과 시각적으로 일치)
    val RUN_SCREEN_CARD_SPACING = 10.dp



    @Deprecated("Use FIRST_CARD_EXTERNAL_GAP for header-to-first-card spacing")
    val RECORDS_HEADER_TO_STATS_CARD_GAP = FIRST_CARD_EXTERNAL_GAP
    // 통계 카드 내부: 헤더(제목/Spacer 제거됨)와 첫번째 Row 사이 간격
    val RECORDS_STATS_INTERNAL_TOP_GAP = 12.dp
    // 통계 카드 내부: 두 개의 통계 Row 사이 간격
    val RECORDS_STATS_ROW_SPACING = 12.dp
    // 통계 카드 내부: 마지막 Row 아래 누적일 표시 전 간격 (현재 12dp)
    val RECORDS_STATS_BOTTOM_GAP = 12.dp
    // 한 Row 안에서 카드들 사이 간격 (SpaceEvenly 제거 후 적용)
    val RECORDS_CARD_IN_ROW_SPACING = 12.dp
    // 개별 StatCard 좌우 내부 padding (legacy StatisticsCardsSection)
    val RECORDS_CARD_HORIZONTAL_PADDING = 8.dp
}
