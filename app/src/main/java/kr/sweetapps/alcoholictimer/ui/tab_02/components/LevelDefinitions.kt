// [NEW] Tab03 리팩토링: LevelDefinitions를 tab_03/components로 이동
package kr.sweetapps.alcoholictimer.ui.tab_02.components

import android.content.Context
import androidx.compose.ui.graphics.Color
import kr.sweetapps.alcoholictimer.R

/**
 * 금주 레벨 정의를 위한 공통 객체
 * [개편] 8단계 → 11단계 확장, 1일 차 기준으로 변경 (2025-12-03)
 */
object LevelDefinitions {
    data class LevelInfo(val nameResId: Int, val start: Int, val end: Int, val color: Color)

    // [NEW] 11단계 레벨 시스템 (Lv.1 ~ Legend)
    // currentDay는 사용자가 금주를 시작한 지 며칠째인지를 나타냄 (시작일 = 1일)
    val levels = listOf(
        LevelInfo(R.string.level_0, 1, 3, Color(0xFFE53935)),         // Lv.1: 알코올 스톱 (빨강)
        LevelInfo(R.string.level_1, 4, 7, Color(0xFFFF6F00)),         // Lv.2: 3일 컷 통과 (진한 주황)
        LevelInfo(R.string.level_2, 8, 14, Color(0xFFFFA726)),        // Lv.3: 1주 클리어 (주황)
        LevelInfo(R.string.level_3, 15, 21, Color(0xFFFFCA28)),       // Lv.4: 피부의 변화 (노랑)
        LevelInfo(R.string.level_4, 22, 30, Color(0xFF9CCC65)),       // Lv.5: 습관 형성 (21일) (연두)
        LevelInfo(R.string.level_5, 31, 60, Color(0xFF66BB6A)),       // Lv.6: 한달의 기적 (초록)
        LevelInfo(R.string.level_6, 61, 99, Color(0xFF42A5F5)),       // Lv.7: 달라진 핏(Fit) (하늘)
        LevelInfo(R.string.level_7, 100, 179, Color(0xFF1E88E5)),     // Lv.8: 100일, 프로 금주러 (파랑)
        LevelInfo(R.string.level_8, 180, 299, Color(0xFF5E35B1)),     // Lv.9: 플러스 통장 (보라)
        LevelInfo(R.string.level_9, 300, 364, Color(0xFF8E24AA)),     // Lv.10: 금주 마스터 (진한 보라)
        LevelInfo(R.string.level_10, 365, Int.MAX_VALUE, Color(0xFFFFD700)) // Legend: 전설의 레전드 (금색)
    )

    /**
     * 주어진 일수에 해당하는 레벨 이름 반환
     * @param days 금주 시작 후 경과 일수 (1일 차부터 시작)
     */
    fun getLevelName(context: Context, days: Int): String {
        val levelInfo = levels.firstOrNull { days in it.start..it.end } ?: levels.first()
        return context.getString(levelInfo.nameResId)
    }

    /**
     * 주어진 일수에 해당하는 레벨 정보 반환
     * @param days 금주 시작 후 경과 일수 (1일 차부터 시작)
     */
    fun getLevelInfo(days: Int): LevelInfo {
        return levels.firstOrNull { days in it.start..it.end } ?: levels.first()
    }

    /**
     * 주어진 일수에 해당하는 레벨 번호 반환 (0-indexed)
     * @param days 금주 시작 후 경과 일수 (1일 차부터 시작)
     * @return 0 (Lv.1) ~ 10 (Legend)
     */
    fun getLevelNumber(days: Int): Int {
        return levels.indexOfFirst { days in it.start..it.end }.takeIf { it >= 0 } ?: 0
    }
}
