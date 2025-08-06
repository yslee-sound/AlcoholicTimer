package com.example.alcoholictimer

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

enum class DrinkLevel(
    val levelName: String,
    val startDays: Int,
    val endDays: Int,
    @ColorRes val colorRes: Int,
    val description: String
) {
    RESOLUTION_7_DAYS(
        "작심 7일",
        0,
        6,
        android.R.color.darker_gray,
        "첫 걸음을 시작했습니다"
    ),
    WILL_2_WEEKS(
        "의지의 2주",
        7,
        13,
        android.R.color.holo_orange_light,
        "의지가 단단해지고 있습니다"
    ),
    MONTH_MIRACLE(
        "한달의 기적",
        14,
        29,
        android.R.color.holo_orange_dark,
        "한 달의 기적을 만들어가고 있습니다"
    ),
    HABIT_BIRTH(
        "습관의 탄생",
        30,
        59,
        android.R.color.holo_green_light,
        "새로운 습관이 자리잡고 있습니다"
    ),
    CONTINUING_CHALLENGE(
        "계속되는 도전",
        60,
        119,
        android.R.color.holo_blue_light,
        "꾸준한 도전이 계속되고 있습니다"
    ),
    ALMOST_1_YEAR(
        "거의 1년",
        120,
        239,
        android.R.color.holo_purple,
        "1년에 가까워지고 있습니다"
    ),
    ABSTAIN_MASTER(
        "금주 마스터",
        240,
        364,
        android.R.color.black,
        "금주의 마스터가 되었습니다"
    ),
    LEGEND_OF_RESTRAINT(
        "절제의 레전드",
        365,
        Int.MAX_VALUE,
        android.R.color.holo_orange_light, // 골드 대신 사용
        "전설적인 절제력을 보여주고 있습니다"
    );

    val levelDisplayName: String = levelName

    fun getDayRange(): String {
        return if (endDays == Int.MAX_VALUE) {
            "${startDays}일 이상"
        } else {
            "${startDays}~${endDays}일"
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDrinkLevel() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DrinkLevel.values().forEach { level ->
                Text(text = "${level.levelName}: ${level.description}")
            }
        }
    }
}
