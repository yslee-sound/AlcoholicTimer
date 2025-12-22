package kr.sweetapps.alcoholictimer.util

import androidx.compose.ui.graphics.Color

/**
 * [NEW] 갈증 수치(1~10) 색상 관리 유틸리티 (2025-12-22)
 * - 10단계 그라데이션 색상 시스템
 * - 수치가 높아질수록 붉은색 계열로 변화
 * - 앱 전체에서 일관된 색상 사용 보장
 */
object ThirstColorUtil {
    /**
     * 갈증 수치에 따른 색상 반환
     * @param level 갈증 수치 (1~10)
     * @return 해당 수치의 색상
     */
    fun getColor(level: Int): Color {
        return when (level) {
            1 -> Color(0xFF4CAF50)  // Green 500 (안정)
            2 -> Color(0xFF8BC34A)  // Light Green 500
            3 -> Color(0xFFCDDC39)  // Lime 500
            4 -> Color(0xFFD4E157)  // Lime 400 (조금 더 노란빛)
            5 -> Color(0xFFFFCA28)  // Amber 400 (주의 시작)
            6 -> Color(0xFFFFB300)  // Amber 600
            7 -> Color(0xFFFB8C00)  // Orange 600
            8 -> Color(0xFFF4511E)  // Deep Orange 600 (위험 진입)
            9 -> Color(0xFFE53935)  // Red 600
            10 -> Color(0xFFB71C1C) // Red 900 (매우 위험)
            else -> Color(0xFF9E9E9E) // 기본 회색 (0 or Error)
        }
    }

    /**
     * 갈증 수치에 따른 상태 텍스트 반환 (옵션)
     * @param level 갈증 수치 (1~10)
     * @return 상태 설명 문자열
     */
    fun getStatusText(level: Int): String {
        return when (level) {
            in 1..2 -> "매우 안정"
            in 3..4 -> "안정"
            in 5..6 -> "주의"
            in 7..8 -> "위험"
            in 9..10 -> "매우 위험"
            else -> "알 수 없음"
        }
    }
}

