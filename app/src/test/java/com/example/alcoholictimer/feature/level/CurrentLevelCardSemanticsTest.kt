package com.example.alcoholictimer.feature.level

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import com.example.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CurrentLevelCardSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun main_level_card_elements_exist_and_texts_match() {
        // Robolectric 환경 지문 설정 (Compose 테스트 러너가 Robolectric 환경을 인식하도록)
        ShadowBuild.setFingerprint("robolectric")

        val days = 19
        val levelInfo = LevelDefinitions.getLevelInfo(days)
        composeRule.setContent {
            AlcoholicTimerTheme(darkTheme = false) {
                Surface {
                    CurrentLevelCard(
                        currentLevel = levelInfo,
                        currentDays = days,
                        elapsedDaysFloat = days.toFloat(),
                        startTime = 0L
                    )
                }
            }
        }
        // 존재 확인
        composeRule.onNodeWithTag("main_level_card_content").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_title").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_days_row").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_days_value").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_days_label").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_progress").assertIsDisplayed()
        composeRule.onNodeWithTag("main_level_progress_fill").assertIsDisplayed()
        // 텍스트 매칭
        composeRule.onNodeWithText(levelInfo.name).assertExists()
        composeRule.onNodeWithText(days.toString()).assertExists()
        composeRule.onNodeWithText("일차").assertExists()
    }
}
