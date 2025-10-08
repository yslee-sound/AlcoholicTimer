package com.example.alcoholictimer.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 앱 전역 디자인 토큰 (라이트 모드 고정 전제)
 * - Elevation: 단순화 (0 / 2 / 4)
 *   ZERO: 완전 평면
 *   CARD (2dp): 일반 카드 / 그룹 / 보조 영역
 *   CARD_HIGH (4dp): 주요 액션 / 주목도 높은 카드 (원형 시작/중지 버튼 포함)
 * 세밀 단계(3dp)는 복잡도 증가 대비 이득이 낮아 제거.
 */
object AppAlphas {
    const val SurfaceTint: Float = 0.1f
}

object AppElevation {
    val ZERO = 0.dp
    val CARD = 2.dp
    val CARD_HIGH = 4.dp
}

/**
 * 추가 색상 토큰: 반투명 surface(white 70% over light gray gradient) 근사 고정 불투명 값.
 * 이전 surface.copy(alpha=0.7f) 대체용 (#FBFBFC).
 */
object AppColors {
    val SurfaceOverlaySoft = Color(0xFFFBFBFC)
}
