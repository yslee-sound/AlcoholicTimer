package kr.sweetapps.alcoholictimer.ui.tab_02.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation

/**
 * 전체 레벨 리스트 카드
 * 모든 레벨 정보를 표시하는 리스트 컴포넌트
 */
@Composable
fun LevelListCard(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            // [FIX] 분홍색 배경 제거 - 투명하게 설정
    ) {
        Column(modifier = Modifier.padding(top = 0.dp)) {
            Text(
                text = context.getString(R.string.level_all_levels),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                ),
                modifier = Modifier.padding(start = 2.dp, bottom = 12.dp) // [FIX] 24.dp → 12.dp (제목-콘텐츠 간격 표준)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LevelDefinitions.levels.forEachIndexed { index, level ->
                    // [CHANGED] 현재 레벨 확인 (2025-12-25)
                    val isCurrentLevel = (level == currentLevel)

                    // [CHANGED] 달성 조건: currentDays가 레벨 시작일 이상이면 달성 (2025-12-25)
                    // 예: 0일 → Lv.1 달성, 3일 → Lv.2 달성
                    val isAchieved = currentDays >= level.start

                    LevelItem(
                        level = level,
                        isCurrent = isCurrentLevel,
                        isAchieved = isAchieved,
                        isNext = level == getNextLevel(currentLevel)
                    )
                }
            }

            // [NEW] 하단 여백 추가 - 스크롤 시 여유 공간 확보
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/**
 * 개별 레벨 아이템
 */
@Composable
private fun LevelItem(
    level: LevelDefinitions.LevelInfo,
    isCurrent: Boolean,
    isAchieved: Boolean,
    isNext: Boolean
) {
    val context = LocalContext.current
    val itemElevation = AppElevation.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // [FIX] 모든 카드를 흰색으로 통일
        ),
        border = when {
            isCurrent -> BorderStroke(1.5.dp, level.color)
            isAchieved -> BorderStroke(1.dp, level.color.copy(alpha = 0.6f))
            else -> BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = itemElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAchieved) level.color else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                val levelNumber = LevelDefinitions.levels.indexOf(level) + 1
                val levelText = if (levelNumber == 11) "L" else "$levelNumber"
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isAchieved) Color.White else Color(0xFF757575)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = context.getString(level.nameResId),
                    style = (if (isCurrent) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium)
                        .copy(color = if (isAchieved) level.color else Color(0xFF757575))
                )

                val dayUnit = context.getString(R.string.level_day_unit)
                val rangeText = if (level.end == Int.MAX_VALUE) "${level.start}$dayUnit+" else "${level.start}~${level.end}$dayUnit"
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF666666))
                )
            }

            when {
                isCurrent -> Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "현재 레벨",
                    tint = level.color,
                    modifier = Modifier.size(20.dp)
                )
                isAchieved -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "달성 완료",
                    tint = level.color,
                    modifier = Modifier.size(20.dp)
                )
                else -> Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "미달성",
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 다음 레벨 계산 헬퍼 함수
 */
private fun getNextLevel(currentLevel: LevelDefinitions.LevelInfo): LevelDefinitions.LevelInfo? {
    val currentIndex = LevelDefinitions.levels.indexOf(currentLevel)
    return if (currentIndex in 0 until LevelDefinitions.levels.size - 1) {
        LevelDefinitions.levels[currentIndex + 1]
    } else {
        null
    }
}

