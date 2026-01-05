// [NEW] Refactored from RunScreen.kt (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * [NEW] 타이머 카드별 그라데이션 생성 함수 (2026-01-05)
 * [UPDATED] 색채 심리학 기반으로 재설계 (2026-01-05)
 * [UPDATED] 첫 번째 카드를 이전 2번 카드의 블루와 동일하게 변경 (2026-01-05)
 * [REFACTORED] RunScreen.kt에서 분리 (2026-01-05)
 *
 * @param page 페이지 인덱스 (0, 1, 2)
 * @return 페이지별 그라데이션 Brush
 *
 * 색상 테마 (색채 심리학 기반):
 * - Card 0 (금주): Deep Blue (맑고 깨끗한 정신)
 * - Card 1 (금연): Healing Green (폐의 정화, 건강한 숨)
 * - Card 2 (커스텀): Mystic Purple (다양한 목표, 고급스러움)
 */
fun getCardGradient(page: Int): Brush {
    return when (page) {
        0 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6), // Deep Blue (딥 블루)
                Color(0xFF1D4ED8)  // Royal Blue (로열 블루)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        1 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF10B981), // Emerald Green (에메랄드 그린)
                Color(0xFF14B8A6)  // Teal (틸)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5CF6), // Vivid Purple (생생한 퍼플)
                Color(0xFF7C3AED)  // Deep Purple (딥 퍼플)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

