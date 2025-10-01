package com.example.alcoholictimer

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.alcoholictimer.utils.Constants

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

    // Constants를 통해 설정값 가져오기 (항상 저장된 값 사용)
    val (initialCost, initialFrequency, initialDuration) = Constants.getUserSettings(context)

    // SharedPreferences 참조 (즉시 저장용)
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)

    // 상태 관리 (초기값은 저장된 값으로 설정)
    var selectedCost by remember { mutableStateOf(initialCost) }
    var selectedFrequency by remember { mutableStateOf(initialFrequency) }
    var selectedDuration by remember { mutableStateOf(initialDuration) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 음주 비용 설정 카드
        SettingsCard(
            title = "음주 비용",
            titleColor = colorResource(id = R.color.color_indicator_money)
        ) {
            SettingsOptionGroup(
                selectedOption = selectedCost,
                options = listOf("저", "중", "고"),
                labels = listOf("저 (1만원 이하)", "중 (1~5만원)", "고 (5만원 이상)"),
                onOptionSelected = { newValue ->
                    selectedCost = newValue
                    // 즉시 저장 (KTX 확장)
                    sharedPref.edit { putString("selected_cost", newValue) }
                }
            )
        }

        // 음주 빈도 설정 카드
        SettingsCard(
            title = "음주 빈도",
            titleColor = colorResource(id = R.color.color_progress_primary)
        ) {
            SettingsOptionGroup(
                selectedOption = selectedFrequency,
                options = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                labels = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                onOptionSelected = { newValue ->
                    selectedFrequency = newValue
                    // 즉시 저장 (KTX 확장)
                    sharedPref.edit { putString("selected_frequency", newValue) }
                }
            )
        }

        // 음주 시간 설정 카드
        SettingsCard(
            title = "음주 시간",
            titleColor = colorResource(id = R.color.color_indicator_hours)
        ) {
            SettingsOptionGroup(
                selectedOption = selectedDuration,
                options = listOf("짧음", "보통", "김"),
                labels = listOf("짧음 (2시간 이하)", "보통 (3~5시간)", "김 (6시간 이상)"),
                onOptionSelected = { newValue ->
                    selectedDuration = newValue
                    // 즉시 저장 (KTX 확장)
                    sharedPref.edit { putString("selected_duration", newValue) }
                }
            )
        }

        // 하단 여백 추가
        Spacer(modifier = Modifier.height(32.dp))
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp) // 20.dp → 16.dp로 축소
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                modifier = Modifier.padding(bottom = 12.dp) // 16.dp → 12.dp로 축소
            )
            content()
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
                colorResource(id = R.color.color_accent_blue).copy(alpha = 0.1f)
            else
                colorResource(id = R.color.color_bg_card_light)
        ),
        border = if (isSelected)
            BorderStroke(2.dp, colorResource(id = R.color.color_accent_blue))
        else
            BorderStroke(1.dp, colorResource(id = R.color.color_border_light))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // 16.dp → 12.dp로 축소
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorResource(id = R.color.color_accent_blue),
                    unselectedColor = colorResource(id = R.color.color_radio_unselected)
                )
            )
            Spacer(modifier = Modifier.width(8.dp)) // 12.dp → 8.dp로 축소
            Text(
                text = label,
                style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        else MaterialTheme.typography.bodyLarge,
                color = if (isSelected) colorResource(id = R.color.color_indicator_days) else colorResource(id = R.color.color_text_primary_dark)
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // 12.dp → 8.dp로 축소
        options.forEachIndexed { index, option ->
            SettingsOptionItem(
                isSelected = selectedOption == option,
                label = labels[index],
                onSelected = { onOptionSelected(option) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
