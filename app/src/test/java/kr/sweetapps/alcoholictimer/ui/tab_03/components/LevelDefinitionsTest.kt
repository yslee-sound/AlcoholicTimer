// [테스트] 레벨 시스템 로직 검증
package kr.sweetapps.alcoholictimer.ui.tab_03.components

import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import org.junit.Test
import org.junit.Assert.*

/**
 * LevelDefinitions 레벨 계산 로직 단위 테스트
 *
 * 새로운 기획안에 따라 1일 차부터 시작하는 11단계 시스템 검증
 */
class LevelDefinitionsTest {

    @Test
    fun `레벨 1 - 알코올 스톱 (1~3일)`() {
        // 1일 차
        val level1_day1 = LevelDefinitions.getLevelInfo(1)
        assertEquals(1, level1_day1.start)
        assertEquals(3, level1_day1.end)
        assertEquals(0, LevelDefinitions.getLevelNumber(1)) // 0-indexed

        // 3일 차 (마지막 날)
        val level1_day3 = LevelDefinitions.getLevelInfo(3)
        assertEquals(1, level1_day3.start)
        assertEquals(3, level1_day3.end)
        assertEquals(0, LevelDefinitions.getLevelNumber(3))
    }

    @Test
    fun `레벨 2 - 3일 컷 통과 (4~7일)`() {
        // 4일 차 (첫날)
        val level2_day4 = LevelDefinitions.getLevelInfo(4)
        assertEquals(4, level2_day4.start)
        assertEquals(7, level2_day4.end)
        assertEquals(1, LevelDefinitions.getLevelNumber(4))

        // 7일 차 (마지막 날)
        val level2_day7 = LevelDefinitions.getLevelInfo(7)
        assertEquals(4, level2_day7.start)
        assertEquals(7, level2_day7.end)
        assertEquals(1, LevelDefinitions.getLevelNumber(7))
    }

    @Test
    fun `레벨 3 - 1주 클리어 (8~14일)`() {
        val level3_day8 = LevelDefinitions.getLevelInfo(8)
        assertEquals(8, level3_day8.start)
        assertEquals(14, level3_day8.end)
        assertEquals(2, LevelDefinitions.getLevelNumber(8))

        val level3_day14 = LevelDefinitions.getLevelInfo(14)
        assertEquals(2, LevelDefinitions.getLevelNumber(14))
    }

    @Test
    fun `레벨 4 - 피부의 변화 (15~21일)`() {
        val level4_day15 = LevelDefinitions.getLevelInfo(15)
        assertEquals(15, level4_day15.start)
        assertEquals(21, level4_day15.end)
        assertEquals(3, LevelDefinitions.getLevelNumber(15))

        val level4_day21 = LevelDefinitions.getLevelInfo(21)
        assertEquals(3, LevelDefinitions.getLevelNumber(21))
    }

    @Test
    fun `레벨 5 - 습관 형성 21일 (22~30일)`() {
        val level5_day22 = LevelDefinitions.getLevelInfo(22)
        assertEquals(22, level5_day22.start)
        assertEquals(30, level5_day22.end)
        assertEquals(4, LevelDefinitions.getLevelNumber(22))

        val level5_day30 = LevelDefinitions.getLevelInfo(30)
        assertEquals(4, LevelDefinitions.getLevelNumber(30))
    }

    @Test
    fun `레벨 6 - 한달의 기적 (31~60일)`() {
        val level6_day31 = LevelDefinitions.getLevelInfo(31)
        assertEquals(31, level6_day31.start)
        assertEquals(60, level6_day31.end)
        assertEquals(5, LevelDefinitions.getLevelNumber(31))

        val level6_day60 = LevelDefinitions.getLevelInfo(60)
        assertEquals(5, LevelDefinitions.getLevelNumber(60))
    }

    @Test
    fun `레벨 7 - 달라진 핏 (61~99일)`() {
        val level7_day61 = LevelDefinitions.getLevelInfo(61)
        assertEquals(61, level7_day61.start)
        assertEquals(99, level7_day61.end)
        assertEquals(6, LevelDefinitions.getLevelNumber(61))

        val level7_day99 = LevelDefinitions.getLevelInfo(99)
        assertEquals(6, LevelDefinitions.getLevelNumber(99))
    }

    @Test
    fun `레벨 8 - 100일 프로 금주러 (100~179일)`() {
        val level8_day100 = LevelDefinitions.getLevelInfo(100)
        assertEquals(100, level8_day100.start)
        assertEquals(179, level8_day100.end)
        assertEquals(7, LevelDefinitions.getLevelNumber(100))

        val level8_day179 = LevelDefinitions.getLevelInfo(179)
        assertEquals(7, LevelDefinitions.getLevelNumber(179))
    }

    @Test
    fun `레벨 9 - 플러스 통장 (180~299일)`() {
        val level9_day180 = LevelDefinitions.getLevelInfo(180)
        assertEquals(180, level9_day180.start)
        assertEquals(299, level9_day180.end)
        assertEquals(8, LevelDefinitions.getLevelNumber(180))

        val level9_day299 = LevelDefinitions.getLevelInfo(299)
        assertEquals(8, LevelDefinitions.getLevelNumber(299))
    }

    @Test
    fun `레벨 10 - 금주 마스터 (300~364일)`() {
        val level10_day300 = LevelDefinitions.getLevelInfo(300)
        assertEquals(300, level10_day300.start)
        assertEquals(364, level10_day300.end)
        assertEquals(9, LevelDefinitions.getLevelNumber(300))

        val level10_day364 = LevelDefinitions.getLevelInfo(364)
        assertEquals(9, LevelDefinitions.getLevelNumber(364))
    }

    @Test
    fun `Legend - 전설의 레전드 (365일+)`() {
        val legend_day365 = LevelDefinitions.getLevelInfo(365)
        assertEquals(365, legend_day365.start)
        assertEquals(Int.MAX_VALUE, legend_day365.end)
        assertEquals(10, LevelDefinitions.getLevelNumber(365))

        // 1년 이상
        val legend_day1000 = LevelDefinitions.getLevelInfo(1000)
        assertEquals(10, LevelDefinitions.getLevelNumber(1000))
    }

    @Test
    fun `경계값 테스트 - 레벨 전환 시점`() {
        // Lv.1 → Lv.2 경계
        assertEquals(0, LevelDefinitions.getLevelNumber(3))  // 3일 차: Lv.1
        assertEquals(1, LevelDefinitions.getLevelNumber(4))  // 4일 차: Lv.2

        // Lv.2 → Lv.3 경계
        assertEquals(1, LevelDefinitions.getLevelNumber(7))  // 7일 차: Lv.2
        assertEquals(2, LevelDefinitions.getLevelNumber(8))  // 8일 차: Lv.3

        // Lv.7 → Lv.8 경계 (100일)
        assertEquals(6, LevelDefinitions.getLevelNumber(99))   // 99일: Lv.7
        assertEquals(7, LevelDefinitions.getLevelNumber(100))  // 100일: Lv.8

        // Lv.10 → Legend 경계
        assertEquals(9, LevelDefinitions.getLevelNumber(364))  // 364일: Lv.10
        assertEquals(10, LevelDefinitions.getLevelNumber(365)) // 365일: Legend
    }

    @Test
    fun `총 레벨 개수 확인`() {
        assertEquals(11, LevelDefinitions.levels.size)
    }

    @Test
    fun `레벨 표시 번호 확인 (1-indexed)`() {
        // getLevelNumber는 0-indexed이므로 +1 해서 표시
        assertEquals("Lv.1", "Lv.${LevelDefinitions.getLevelNumber(1) + 1}")
        assertEquals("Lv.2", "Lv.${LevelDefinitions.getLevelNumber(4) + 1}")
        assertEquals("Lv.8", "Lv.${LevelDefinitions.getLevelNumber(100) + 1}")
        assertEquals("Lv.11", "Lv.${LevelDefinitions.getLevelNumber(365) + 1}")
    }
}

