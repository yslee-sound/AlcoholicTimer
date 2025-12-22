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
            1 -> Color(0xFFC8E6C9)  // Very Light Green (매우 안전 - Very Safe)
            2 -> Color(0xFFA3D9A5)  // Light Green (안전 - Safe)
            3 -> Color(0xFF81C784)  // Medium Green (약간 안전 - Mildly Safe)
            4 -> Color(0xFFFFF176)  // Light Yellow (중립 - Neutral)
            5 -> Color(0xFFFFD54F)  // Muted Yellow/Amber (약간 주의 - Slightly Elevated)
            6 -> Color(0xFFFFB300)  // Vibrant Yellow/Orange (주의 - Caution)
            7 -> Color(0xFFE0BBE4)  // Light Lavender/Purple (증가된 주의 - Increased Caution)
            8 -> Color(0xFFE57373)  // Soft Red (경고 - Warning)
            9 -> Color(0xFFEF5350)  // Medium Red (강한 경고 - Strong Warning)
            10 -> Color(0xFFD32F2F) // Deep Red (매우 경고 - Severe Warning)
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

