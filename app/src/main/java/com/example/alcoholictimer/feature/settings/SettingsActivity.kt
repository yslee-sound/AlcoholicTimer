package com.sweetapps.alcoholictimer.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sweetapps.alcoholictimer.core.ui.BaseActivity
import com.sweetapps.alcoholictimer.core.util.Constants
import com.sweetapps.alcoholictimer.R
import com.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import com.sweetapps.alcoholictimer.core.ui.LayoutConstants
import androidx.compose.material3.RadioButtonDefaults
import com.sweetapps.alcoholictimer.core.ui.AdmobBanner

class SettingsActivity : BaseActivity() {
    override fun getScreenTitleResId(): Int = R.string.settings_title
    override fun getScreenTitle(): String = getString(R.string.settings_title)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 뒤로가기 버튼: 메인 홈(Start/Run)으로 이동
            BackHandler(enabled = true) {
                navigateToMainHome()
            }

            BaseScreen(bottomAd = { AdmobBanner() }) { SettingsScreen() }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val (initialCost, initialFrequency, initialDuration) = Constants.getUserSettings(context)
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)

    var selectedCost by remember { mutableStateOf(initialCost) }
    var selectedFrequency by remember { mutableStateOf(initialFrequency) }
    var selectedDuration by remember { mutableStateOf(initialDuration) }

    val safePadding = LocalSafeContentPadding.current

    // 전체 바탕 흰색 + 목록형(비스크롤) 레이아웃
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    top = 8.dp
                )
                .padding(safePadding)
                // 배너 바로 위 최소 8dp 완충
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSection(title = "음주 비용", titleColor = colorResource(id = R.color.color_indicator_money)) {
                SettingsOptionGroup(
                    selectedOption = selectedCost,
                    options = listOf("저", "중", "고"),
                    labels = listOf("저 (1만원 이하)", "중 (1~5만원)", "고 (5만원 이상)"),
                    onOptionSelected = { newValue ->
                        selectedCost = newValue
                        sharedPref.edit { putString("selected_cost", newValue) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = "음주 빈도", titleColor = colorResource(id = R.color.color_progress_primary)) {
                SettingsOptionGroup(
                    selectedOption = selectedFrequency,
                    options = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                    labels = listOf("주 1회 이하", "주 2~3회", "주 4회 이상"),
                    onOptionSelected = { newValue ->
                        selectedFrequency = newValue
                        sharedPref.edit { putString("selected_frequency", newValue) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = "음주 시간", titleColor = colorResource(id = R.color.color_indicator_hours)) {
                SettingsOptionGroup(
                    selectedOption = selectedDuration,
                    options = listOf("짧음", "보통", "길게"),
                    labels = listOf("짧음 (2시간 이하)", "보통 (3~5시간)", "길게 (6시간 이상)"),
                    onOptionSelected = { newValue ->
                        selectedDuration = newValue
                        sharedPref.edit { putString("selected_duration", newValue) }
                    }
                )
            }
        }
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        thickness = 1.dp,
        color = colorResource(id = R.color.color_border_light)
    )
}

@Composable
fun SettingsSection(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        )
        content()
    }
}

@Composable
fun SettingsOptionGroup(
    selectedOption: String,
    options: List<String>,
    labels: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, option ->
            SettingsOptionItem(
                isSelected = selectedOption == option,
                label = labels[index],
                onSelected = { onOptionSelected(option) }
            )
        }
    }
}

@Composable
fun SettingsOptionItem(
    isSelected: Boolean,
    label: String,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clickable(
                role = Role.RadioButton,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelected() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(id = R.color.color_accent_blue),
                unselectedColor = colorResource(id = R.color.color_radio_unselected)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else colorResource(id = R.color.color_text_primary_dark)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() { SettingsScreen() }
