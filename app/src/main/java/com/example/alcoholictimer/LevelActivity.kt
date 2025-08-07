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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        val levels = listOf(
            LevelInfo("작심 7일", 0, 6, Color(0xFFBDBDBD)),
            LevelInfo("의지의 2주", 7, 13, Color(0xFFFFEB3B)),
            LevelInfo("한달의 기적", 14, 29, Color(0xFFFF9800)),
            LevelInfo("습관의 탄생", 30, 59, Color(0xFF4CAF50)),
            LevelInfo("계속되는 도전", 60, 119, Color(0xFF2196F3)),
            LevelInfo("거의 1년", 120, 239, Color(0xFF9C27B0)),
            LevelInfo("금주 마스터", 240, 364, Color(0xFF212121)),
            LevelInfo("절제의 레전드", 365, Int.MAX_VALUE, Color(0xFFFFD700)),
        )
        val currentLevel = levels.firstOrNull { currentDays in it.start..it.end } ?: levels.last()

        Column(
            modifier = Modifier.fillMaxSize().background(Color.White),
        ) {
            // 상단바
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(28.dp).clickable { onBack?.invoke() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "금주 레벨",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 레벨 카드 리스트
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                levels.forEach { level ->
                    LevelCard(level, currentDays)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    data class LevelInfo(val name: String, val start: Int, val end: Int, val color: Color)

    @Composable
    fun LevelCard(level: LevelInfo, currentDays: Int) {
        val isCurrent = currentDays in level.start..level.end
        val progress = when {
            currentDays < level.start -> 0f
            currentDays > level.end -> 1f
            else -> (currentDays - level.start + 1).toFloat() / (level.end - level.start + 1)
        }
        val dateText = if (level.name == "절제의 레전드") "1년 이상" else "${level.start}~${level.end}일"
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = level.color.copy(alpha = if (isCurrent) 1f else 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = level.name,
                    fontSize = 18.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) Color.Black else Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (isCurrent) Color.Black else Color.Gray
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "현재 진행도: ${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLevelScreen() {
    LevelActivity().LevelScreen(currentDays = 15)
}
