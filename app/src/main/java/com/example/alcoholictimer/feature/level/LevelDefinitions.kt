package com.sweetapps.alcoholictimer.feature.level

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.sweetapps.alcoholictimer.R

/**
 * 금주 레벨 정의를 위한 공통 객체
 */
object LevelDefinitions {
    data class LevelInfo(val nameResId: Int, val start: Int, val end: Int, val color: Color)

    val levels = listOf(
        LevelInfo(R.string.level_0, 0, 6, Color(0xFF4FC3F7)),      // 연한 하늘색
        LevelInfo(R.string.level_1, 7, 13, Color(0xFF00ACC1)),    // 청록색
        LevelInfo(R.string.level_2, 14, 29, Color(0xFF81C784)),   // 연두색
        LevelInfo(R.string.level_3, 30, 59, Color(0xFF43A047)),   // 밝은 초록
        LevelInfo(R.string.level_4, 60, 119, Color(0xFFFDD835)), // 노랑
        LevelInfo(R.string.level_5, 120, 239, Color(0xFFFB8C00)),   // 주황
        LevelInfo(R.string.level_6, 240, 364, Color(0xFFE53935)), // 빨강
        LevelInfo(R.string.level_7, 365, Int.MAX_VALUE, Color(0xFF8E24AA)) // 보라
    )

    fun getLevelName(context: Context, days: Int): String {
        val levelInfo = levels.firstOrNull { days in it.start..it.end } ?: levels.first()
        return context.getString(levelInfo.nameResId)
    }

    fun getLevelInfo(days: Int): LevelInfo {
        return levels.firstOrNull { days in it.start..it.end } ?: levels.first()
    }
}

