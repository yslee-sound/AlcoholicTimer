package com.example.alcoholictimer

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : BaseActivity() {

    override fun getScreenTitle(): String = "설정"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    // SharedPreferences에서 저장된 값 불러오기
    var selectedCost by remember {
        mutableStateOf(sharedPref.getString("selected_cost", "중") ?: "중")
    }
    var selectedFrequency by remember {
        mutableStateOf(sharedPref.getString("selected_frequency", "주 2~3회") ?: "주 2~3회")
    }
    var selectedDuration by remember {
        mutableStateOf(sharedPref.getString("selected_duration", "보통") ?: "보통")
    }

    // 모던한 그라데이션 배경 (RunActivity와 동일)
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 타이틀 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            // fontScale 고정 타이틀
            val density = LocalDensity.current
            val fixedDensity = Density(density.density, 1f)
            CompositionLocalProvider(LocalDensity provides fixedDensity) {
                Text(
                    text = "설정",
                    fontSize = 20.sp, // 기존 32.sp → 20.sp로 조정
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // 음주 비용 설정 카드
        SettingsCard(
            title = "음주 비용",
            titleColor = Color(0xFFE91E63)
        ) {
            SettingsOptionGroup(
                selectedOption = selectedCost,
                options = listOf("저", "중", "고"),
                labels = listOf("저 (1만원 이하)", "중 (1~5만원)", "고 (5만원 이상)"),
                onOptionSelected = { selectedCost = it }
            )
        }

        // 음주 빈도 설정 카드
        SettingsCard(
            title = "음주 빈도",
            titleColor = Color(0xFF4CAF50)
        ) {
            SettingsOptionGroup(
                selectedOption = selectedFrequency,
                options = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                labels = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                onOptionSelected = { selectedFrequency = it }
            )
        }

        // 음주 시간 설정 카드
        SettingsCard(
            title = "음주 시간",
            titleColor = Color(0xFFFF9800)
        ) {
            SettingsOptionGroup(
                selectedOption = selectedDuration,
                options = listOf("짧음", "보통", "김"),
                labels = listOf("짧음 (2시간 이하)", "보통 (3~5시간)", "김 (6시간 이상)"),
                onOptionSelected = { selectedDuration = it }
            )
        }

        // 버튼 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                // 저장 버튼
                ModernButton(
                    text = "저장",
                    backgroundColor = Color(0xFF4CAF50),
                    textColor = Color.White,
                    onClick = {
                        // SharedPreferences에 설정값 저장
                        sharedPref.edit().apply {
                            putString("selected_cost", selectedCost)
                            putString("selected_frequency", selectedFrequency)
                            putString("selected_duration", selectedDuration)
                            apply()
                        }
                        // Toast 메시지 표시
                        android.widget.Toast.makeText(context, "설정이 저장되었습니다", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )

                // 리셋 버튼
                ModernButton(
                    text = "리셋",
                    backgroundColor = Color.Transparent,
                    textColor = Color(0xFFE53935),
                    borderColor = Color(0xFFE53935),
                    onClick = {
                        selectedCost = "중"
                        selectedFrequency = "주 2~3회"
                        selectedDuration = "보통"
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp, // 기존 20.sp → 16.sp로 조정
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsOptionGroup(
    selectedOption: String,
    options: List<String>,
    labels: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEachIndexed { index, option ->
            SettingsOptionItem(
                isSelected = selectedOption == option,
                label = labels[index],
                onSelected = { onOptionSelected(option) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOptionItem(
    isSelected: Boolean,
    label: String,
    onSelected: () -> Unit
) {
    Card(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF2196F3).copy(alpha = 0.1f)
            else
                Color(0xFFF5F5F5)
        ),
        border = if (isSelected)
            BorderStroke(2.dp, Color(0xFF2196F3))
        else
            BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2196F3),
                    unselectedColor = Color(0xFF757575)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF1976D2) else Color(0xFF424242)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = borderColor?.let { BorderStroke(2.dp, it) },
        elevation = CardDefaults.cardElevation(defaultElevation = if (backgroundColor == Color.Transparent) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Preview(name = "fontScale 1.0", fontScale = 1.0f, showBackground = true)
@Preview(name = "fontScale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
