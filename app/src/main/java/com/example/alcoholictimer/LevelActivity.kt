package com.example.alcoholictimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.SobrietyRecord
import org.json.JSONArray

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
        currentDays: Int = 15, // 예시: 실제 데이터 연동 시 파라미터로 변경
        onBack: (() -> Unit)? = null
    ) {
        val context = LocalContext.current
        // 기록 불러오기
        val records = remember {
            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
            val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
            SobrietyRecord.fromJsonArray(recordsJson)
        }
        // 전체 달성 일수 계산
        val totalDays = records.sumOf { it.actualDays }
        // 성공(완료) 횟수 계산
        val completedCount = records.count { it.isCompleted }
        // 전체 시도 횟수
        val totalAttempts = records.size

        // 레벨 산정 예시 (단순화)
        val currentLevel = when {
            totalDays >= 365 -> Level("절제의 레전드", Color(0xFFFFF0DC), 365)
            totalDays >= 90 -> Level("3개월 클리어", Color(0xFFE8F5E8), 90)
            totalDays >= 30 -> Level("한 달 클리어", Color(0xFFE7F3FF), 30)
            totalDays >= 7 -> Level("작심 7일", Color(0xFFFFF3CD), 7)
            else -> Level("시작", Color(0xFFF5F5F5), 0)
        }
        val nextLevel = when {
            totalDays < 7 -> Level("작심 7일", Color(0xFFFFF3CD), 7)
            totalDays < 30 -> Level("한 달 클리어", Color(0xFFE7F3FF), 30)
            totalDays < 90 -> Level("3개월 클리어", Color(0xFFE8F5E8), 90)
            totalDays < 365 -> Level("절제의 레전드", Color(0xFFFFF0DC), 365)
            else -> null
        }
        val daysToNext = nextLevel?.threshold?.minus(totalDays) ?: 0
        val progress = if (nextLevel != null) totalDays.toFloat() / nextLevel.threshold else 1f

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
                        modifier = Modifier.fillMaxWidth(0.7f).height(10.dp),
                        color = currentLevel.color
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
            Divider(
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
                        currentDays = currentDays,
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = level.color.copy(alpha = if (enabled) 1f else 0.2f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = level.name,
                    fontSize = 18.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) Color.Black else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }

    // 레벨 데이터 클래스
    private data class Level(val name: String, val color: Color, val threshold: Int)

    // 최신 레벨 정보 리스트로 교체
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

    // 현재 레벨 인덱스 계산
    private val currentLevelIndex: Int
        get() {
            val context = this
            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
            val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
            val records = com.example.alcoholictimer.utils.SobrietyRecord.fromJsonArray(recordsJson)
            val totalDays = records.sumOf { it.actualDays }
            return levels.indexOfLast { totalDays >= it.start }
        }
}

@Preview(showBackground = true)
@Composable
fun PreviewLevelScreen() {
    LevelActivity().LevelScreen(currentDays = 15)
}
