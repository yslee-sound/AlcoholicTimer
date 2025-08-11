package com.example.alcoholictimer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LevelScreen(
        currentDays: Int = 0, // 기본값을 0으로 변경
        onBack: (() -> Unit)? = null
    ) {
        val context = LocalContext.current
        // 기록 불러오기
        val records = remember {
            try {
                val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
                SobrietyRecord.fromJsonArray(recordsJson)
            } catch (e: Exception) {
                // Preview 환경에서는 빈 리스트 반환
                emptyList()
            }
        }
        // 전체 달성 일수 계산 - 모든 기록의 actualDays를 합산
        val totalDays = records.sumOf { it.actualDays }

        // 현재 레벨 인덱스 계산 (안전한 방식)
        val currentLevelIndex = remember(totalDays) {
            levels.indexOfFirst { totalDays in it.start..it.end }.coerceAtLeast(0)
        }

        // 레벨 산정 예시 (단순화)
        val currentLevel = levels[currentLevelIndex]
        val nextLevel = levels.getOrNull(currentLevelIndex + 1)
        val daysToNext = nextLevel?.start?.minus(totalDays) ?: 0
        val progress = when {
            totalDays < currentLevel.start -> 0f
            totalDays > currentLevel.end -> 1f
            else -> (totalDays - currentLevel.start + 1).toFloat() / (currentLevel.end - currentLevel.start + 1)
        }

        Column(
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            // 현재 레벨 영역 (상단 1/3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.33f)
                    .background(Color.White), // 배경을 흰색으로 변경
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentLevel.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)), // 모서리 둥글게
                        color = currentLevel.color, // 레벨별 색상
                        trackColor = Color(0xFFE0E0E0) // 배경을 연한 회색으로 명확하게 지정
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (nextLevel != null) {
                        Text(
                            text = "다음 레벨까지 ${daysToNext}일 남음",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "최고 레벨 달성!",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            }
            // 구분선
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.LightGray)
            )
            // 전체 레벨 리스트 (하단 2/3)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
            ) {
                Spacer(modifier = Modifier.height(16.dp)) // 첫 레벨 위에 여백 추가
                levels.forEachIndexed { idx, level ->
                    val isAchieved = idx <= currentLevelIndex
                    LevelCard(
                        level = level,
                        currentDays = totalDays, // currentDays 대신 totalDays 사용
                        enabled = isAchieved
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    data class LevelInfo(val name: String, val start: Int, val end: Int, val color: Color)

    @Composable
    fun LevelCard(level: LevelInfo, currentDays: Int, enabled: Boolean) {
        val isCurrent = currentDays in level.start..level.end
        val dateText = if (level.name == "절제의 레전드") "1년 이상" else "${level.start}~${level.end}일"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = if (isCurrent) 8.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    Color.White
                } else {
                    Color(0xFFF8F9FA)
                }
            ),
            border = if (isCurrent) {
                BorderStroke(2.dp, level.color)
            } else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 레벨 인디케이터
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (enabled) level.color else Color(0xFFE0E0E0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "현재 레벨",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = (levels.indexOf(level) + 1).toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) Color.White else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 텍스트 영역
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = level.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.Black else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateText,
                        fontSize = 14.sp,
                        color = if (enabled) Color(0xFF6B7280) else Color.Gray
                    )

                    if (isCurrent) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "현재 진행 중",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = level.color
                        )
                    }
                }

                // 상태 아이콘
                if (enabled && !isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 최신 레벨 정의 (기획서 기준)
    private val levels = listOf(
        LevelInfo("작심 7일", 0, 6, Color(0xFFBDBDBD)),
        LevelInfo("의지의 2주", 7, 13, Color(0xFFFFEB3B)),
        LevelInfo("한달의 기적", 14, 29, Color(0xFFFF9800)),
        LevelInfo("습관의 탄생", 30, 59, Color(0xFF4CAF50)),
        LevelInfo("계속되는 도전", 60, 119, Color(0xFF2196F3)),
        LevelInfo("거의 1년", 120, 239, Color(0xFF9C27B0)),
        LevelInfo("금주 마스터", 240, 364, Color(0xFF212121)),
        LevelInfo("절제의 레전드", 365, Int.MAX_VALUE, Color(0xFFFFD700))
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLevelScreen() {
    LevelActivity().LevelScreen(currentDays = 15)
}
