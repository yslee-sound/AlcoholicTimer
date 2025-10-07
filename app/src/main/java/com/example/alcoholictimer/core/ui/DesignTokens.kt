package com.example.alcoholictimer.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 앱 전역 디자인 토큰 (라이트 모드 고정 전제)
 * - Alpha: 장식/배경 tint 용도는 0.1f 로 통일
 * - Elevation: 최소 단순화 (0 / 2 / 4)
 *   ZERO: 완전 평면
 *   CARD: 보조/컨트롤 그룹 카드
 *   CARD_HIGH: 주요 콘텐츠 카드 / 리스트 아이템
 *
 * Disabled 상태 등 가독성 목적의 알파(0.5f, 0.8f 등)는 기능적 의미가 있어 유지.
 * 아래 SurfaceTint 값만 일관 적용 대상으로 삼음.
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
