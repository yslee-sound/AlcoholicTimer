package com.example.alcoholictimer

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.utils.RecordsDataLoader

class LevelActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                LevelScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "금주 레벨"

    override fun onResume() {
        super.onResume()
        // 테스트 모드 설정 업데이트
        val testModePrefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val currentTestMode = testModePrefs.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)
        Constants.updateTestMode(currentTestMode)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen() {
    val context = LocalContext.current

    // SharedPreferences에서 현재 진행 상황 가져오기
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val currentTime = System.currentTimeMillis()

    // 현재 진행 중인 금주 시간
    val currentElapsedTime = if (startTime > 0) currentTime - startTime else 0L

    // 과거 금주 기록들의 누적 시간 계산
    val pastRecords = RecordsDataLoader.loadSobrietyRecords(context)
    val totalPastDuration = pastRecords.sumOf { record ->
        // 완료된 기록과 미완료 기록 모두 실제 진행한 시간만큼 반영
        (record.endTime - record.startTime).toLong()
    }

    // 총 누적 금주 시간 = 과거 기록들의 누적 시간 + 현재 진행 중인 시간
    val totalElapsedTime = totalPastDuration + currentElapsedTime

    // 레벨 계산용 일수 (테스트 모드 적용, 누적 시간 기반)
    val levelDays = Constants.calculateLevelDays(totalElapsedTime)
    val currentLevel = LevelDefinitions.getLevelInfo(levelDays)

    // 모던한 그라데이션 배경 (StartActivity와 동일)
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE3F2FD),
            Color(0xFFF1F8E9)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 현재 레벨 카드
        CurrentLevelCard(currentLevel = currentLevel, currentDays = levelDays)

        Spacer(modifier = Modifier.height(24.dp))

        // 전체 레벨 목록
        LevelListCard(currentLevel = currentLevel, currentDays = levelDays)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CurrentLevelCard(currentLevel: LevelDefinitions.LevelInfo, currentDays: Int) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 레벨 아이콘 - 이모지 대신 레벨명의 첫 글자 사용
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                currentLevel.color.copy(alpha = 0.8f),
                                currentLevel.color
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLevel.name.take(2), // 레벨명의 첫 2글자 표시
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 현재 레벨 텍스트
            Text(
                text = "현재 레벨",
                fontSize = 16.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentLevel.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = currentLevel.color,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 누적 금주 일수 표시 (수정)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$currentDays",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "일차",
                    fontSize = 18.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }

            // 과거 기록 정보 추가 표시
            val pastRecords = RecordsDataLoader.loadSobrietyRecords(context)
            if (pastRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "과거 기록 ${pastRecords.size}회 포함",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    fontWeight = FontWeight.Normal
                )
            }

            // 다음 레벨까지의 진행률
            val nextLevel = getNextLevel(currentLevel)
            if (nextLevel != null) {
                Spacer(modifier = Modifier.height(24.dp))

                val progress = if (nextLevel.start > currentLevel.start) {
                    val progressInLevel = currentDays - currentLevel.start
                    val totalNeeded = nextLevel.start - currentLevel.start
                    if (totalNeeded > 0) (progressInLevel.toFloat() / totalNeeded.toFloat()).coerceIn(0f, 1f) else 0f
                } else 0f

                ProgressToNextLevel(
                    nextLevel = nextLevel,
                    progress = progress,
                    remainingDays = (nextLevel.start - currentDays).coerceAtLeast(0)
                )
            }
        }
    }
}

@Composable
private fun ProgressToNextLevel(nextLevel: LevelDefinitions.LevelInfo, progress: Float, remainingDays: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "다음 레벨까지",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 진행률 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 1000)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                nextLevel.color.copy(alpha = 0.7f),
                                nextLevel.color
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )

            Text(
                text = "${remainingDays}일 남음",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

        }
    }
}

@Composable
private fun LevelListCard(currentLevel: LevelDefinitions.LevelInfo, currentDays: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "전체 레벨",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LevelDefinitions.levels.forEach { level ->
                LevelItem(
                    level = level,
                    isCurrent = level == currentLevel,
                    isAchieved = currentDays >= level.start,
                    isNext = level == getNextLevel(currentLevel)
                )

                if (level != LevelDefinitions.levels.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LevelItem(
    level: LevelDefinitions.LevelInfo,
    isCurrent: Boolean,
    isAchieved: Boolean,
    isNext: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> level.color.copy(alpha = 0.1f)
                isAchieved -> level.color.copy(alpha = 0.1f) // 완료된 레벨도 색상 배경 적용
                else -> Color(0xFFFAFAFA)
            }
        ),
        border = when {
            isCurrent -> androidx.compose.foundation.BorderStroke(2.dp, level.color)
            isAchieved -> androidx.compose.foundation.BorderStroke(1.dp, level.color.copy(alpha = 0.5f)) // 완료된 레벨도 테두리 적용
            else -> null
        }
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
                    .background(
                        if (isAchieved) level.color else Color(0xFFE0E0E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 현재 진행중이든 완료된 레벨이든 모두 첫글자 표시
                Text(
                    text = level.name.take(1), // 레벨명의 첫 1글자 표시
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAchieved) Color.White else Color(0xFF757575)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = level.name,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAchieved) level.color else Color(0xFF757575)
                )

                val rangeText = if (level.end == Int.MAX_VALUE) {
                    "${level.start}일 이상"
                } else {
                    "${level.start}~${level.end}일"
                }

                Text(
                    text = rangeText,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            if (isCurrent) {
                // 현재 진행중 표시
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "현재 레벨",
                    tint = level.color,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isAchieved) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "달성 완료",
                    tint = level.color,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "미달성",
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 다음 레벨 계산
private fun getNextLevel(currentLevel: LevelDefinitions.LevelInfo): LevelDefinitions.LevelInfo? {
    val currentIndex = LevelDefinitions.levels.indexOf(currentLevel)
    return if (currentIndex < LevelDefinitions.levels.size - 1) {
        LevelDefinitions.levels[currentIndex + 1]
    } else null
}

// Preview 컴포넌트들
@Preview(
    showBackground = true,
    name = "LevelScreen - 기본",
    widthDp = 360,
    heightDp = 800
)
@Composable
fun LevelScreenPreview() {
    // Preview에서는 BaseScreen을 직접 참조할 수 없으므로, Scaffold 등으로 대체
    Scaffold {
        LevelScreen()
    }
}
