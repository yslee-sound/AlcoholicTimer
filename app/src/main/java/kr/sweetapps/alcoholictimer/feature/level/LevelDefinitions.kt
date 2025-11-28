package kr.sweetapps.alcoholictimer.feature.level

import android.content.Context
import androidx.compose.ui.graphics.Color
import kr.sweetapps.alcoholictimer.R

/**
 * 금주 레벨 정의를 위한 공통 객체
 */
object LevelDefinitions {
    data class LevelInfo(val nameResId: Int, val start: Int, val end: Int, val color: Color)

    val levels = listOf(
        // Colors set to: 빨, 주, 노, 초, 파, 남, 보, 검정 (displayed as LV.1 .. LV.8)
        LevelInfo(R.string.level_0, 0, 6, Color(0xFFE53935)),      // 빨 (red)
        LevelInfo(R.string.level_1, 7, 13, Color(0xFFFB8C00)),     // 주 (orange)
        LevelInfo(R.string.level_2, 14, 29, Color(0xFFFBC02D)),    // 노 (yellow)
        LevelInfo(R.string.level_3, 30, 59, Color(0xFF43A047)),    // 초 (green)
        LevelInfo(R.string.level_4, 60, 119, Color(0xFF1E88E5)),   // 파 (blue)
        LevelInfo(R.string.level_5, 120, 239, Color(0xFF3949AB)),  // 남 (indigo/navy)
        LevelInfo(R.string.level_6, 240, 364, Color(0xFF8E24AA)),  // 보 (purple)
        LevelInfo(R.string.level_7, 365, Int.MAX_VALUE, Color(0xFF000000)) // 검정 (black)
    )

    fun getLevelName(context: Context, days: Int): String {
        val levelInfo = levels.firstOrNull { days in it.start..it.end } ?: levels.first()
        return context.getString(levelInfo.nameResId)
    }

    fun getLevelInfo(days: Int): LevelInfo {
        return levels.firstOrNull { days in it.start..it.end } ?: levels.first()
    }

    fun getLevelNumber(days: Int): Int {
        return levels.indexOfFirst { days in it.start..it.end }.takeIf { it >= 0 } ?: 0
    }
}
