package kr.sweetapps.alcoholictimer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 앱 전역 디자인 토큰 (라이트 모드 고정 전제)
 * - Elevation: 플랫 기본(0) + 강조(2)
 *   ZERO: 완전 평면(0dp)
 *   CARD (0dp): 일반 카드 / 컨테이너의 기본값
 *   CARD_HIGH (2dp): 주요 액션 / 주목도 높은 원형 버튼 등
 */
object AppAlphas {
    const val SurfaceTint: Float = 0.1f
}

object AppElevation {
    val ZERO = 0.dp
    val CARD = 0.dp
    val CARD_HIGH = 2.dp
}

/** 전역 테두리 두께 표준 */
object AppBorder {
    // 가이드라인에 따른 헤어라인 두께
    // Hairline: 가장 얇은 테두리 / 선
    // 0.75dp로 설정하여 미세한 구분선 표현
    val Hairline = 0.75.dp
}

/**
 * 선택된 항목 하이라이트용 소프트 그레이 배경.
 * 배경이 흰색(#FFFFFF)일 때도 충분히 식별되도록 명도 대비를 높인 톤입니다.
 */
object AppColors {
    // 기존: Color(0xFFFBFBFC) -> 거의 흰색이라 시인성이 낮았음
    // 변경: 약한 블루-그레이 톤으로 대비 강화
    val SurfaceOverlaySoft = Color(0xFFE9EEF5)
}
