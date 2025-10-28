package com.sweetapps.alcoholictimer.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
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
    var selectedCurrency by remember {
        mutableStateOf(com.sweetapps.alcoholictimer.core.util.CurrencyManager.getSelectedCurrency(context).code)
    }

    val safePadding = LocalSafeContentPadding.current
    val scrollState = rememberScrollState()

    // 전체 바탕 흰색 + 스크롤 가능한 목록형 레이아웃
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
            SettingsSection(title = stringResource(R.string.settings_drinking_cost), titleColor = colorResource(id = R.color.color_indicator_money)) {
                SettingsOptionGroup(
                    selectedOption = selectedCost,
                    options = listOf(
                        Constants.KEY_COST_LOW,
                        Constants.KEY_COST_MEDIUM,
                        Constants.KEY_COST_HIGH
                    ),
                    labels = listOf(
                        stringResource(R.string.settings_cost_low_label),
                        stringResource(R.string.settings_cost_medium_label),
                        stringResource(R.string.settings_cost_high_label)
                    ),
                    onOptionSelected = { newValue ->
                        selectedCost = newValue
                        sharedPref.edit { putString(Constants.PREF_SELECTED_COST, newValue) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = stringResource(R.string.settings_drinking_frequency), titleColor = colorResource(id = R.color.color_progress_primary)) {
                SettingsOptionGroup(
                    selectedOption = selectedFrequency,
                    options = listOf(
                        Constants.KEY_FREQUENCY_LOW,
                        Constants.KEY_FREQUENCY_MEDIUM,
                        Constants.KEY_FREQUENCY_HIGH
                    ),
                    labels = listOf(
                        stringResource(R.string.settings_frequency_low),
                        stringResource(R.string.settings_frequency_medium),
                        stringResource(R.string.settings_frequency_high)
                    ),
                    onOptionSelected = { newValue ->
                        selectedFrequency = newValue
                        sharedPref.edit { putString(Constants.PREF_SELECTED_FREQUENCY, newValue) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = stringResource(R.string.settings_drinking_duration), titleColor = colorResource(id = R.color.color_indicator_hours)) {
                SettingsOptionGroup(
                    selectedOption = selectedDuration,
                    options = listOf(
                        Constants.KEY_DURATION_SHORT,
                        Constants.KEY_DURATION_MEDIUM,
                        Constants.KEY_DURATION_LONG
                    ),
                    labels = listOf(
                        stringResource(R.string.settings_duration_short_label),
                        stringResource(R.string.settings_duration_medium_label),
                        stringResource(R.string.settings_duration_long_label)
                    ),
                    onOptionSelected = { newValue ->
                        selectedDuration = newValue
                        sharedPref.edit { putString(Constants.PREF_SELECTED_DURATION, newValue) }
                    }
                )
            }
            SectionDivider()

            SettingsSection(title = stringResource(R.string.settings_currency), titleColor = colorResource(id = R.color.color_indicator_money)) {
                SettingsCurrencyGroup(
                    selectedCurrency = selectedCurrency,
                    onCurrencySelected = { newCurrency ->
                        selectedCurrency = newCurrency
                        com.sweetapps.alcoholictimer.core.util.CurrencyManager.saveCurrency(context, newCurrency)
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
fun SettingsCurrencyGroup(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        com.sweetapps.alcoholictimer.core.util.CurrencyManager.supportedCurrencies.forEach { currency ->
            SettingsOptionItem(
                isSelected = selectedCurrency == currency.code,
                label = stringResource(currency.nameResId),
                onSelected = { onCurrencySelected(currency.code) }
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
