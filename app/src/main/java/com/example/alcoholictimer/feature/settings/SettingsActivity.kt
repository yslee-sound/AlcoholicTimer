package com.sweetapps.alcoholictimer.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sweetapps.alcoholictimer.core.ui.AppElevation
import com.sweetapps.alcoholictimer.core.ui.AppBorder
import com.sweetapps.alcoholictimer.core.ui.BaseActivity
import com.sweetapps.alcoholictimer.core.util.Constants
import com.sweetapps.alcoholictimer.R
import com.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import com.sweetapps.alcoholictimer.core.ui.LayoutConstants
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.RadioButtonDefaults
import com.sweetapps.alcoholictimer.core.ui.AdmobBanner

class SettingsActivity : BaseActivity() {
    override fun getScreenTitle(): String = "설정"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaseScreen(bottomAd = { AdmobBanner() }) { SettingsScreen() } }
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

    // 실측 기반 스크롤 판정
    val density = LocalDensity.current
    val gapPx = with(density) { 12.dp.roundToPx() }
    var viewportH by remember { mutableStateOf(0) }
    var costH by remember { mutableStateOf(0) }
    var freqH by remember { mutableStateOf(0) }
    var durH by remember { mutableStateOf(0) }
    val allowScroll by remember { derivedStateOf { (costH + freqH + durH + gapPx * 2) > viewportH } }
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                top = 8.dp,
                bottom = 8.dp
            )
            .padding(safePadding)
            .onSizeChanged { viewportH = it.height }
    ) {
        val listContent: @Composable () -> Unit = {
            LazyColumn(
                state = listState,
                userScrollEnabled = allowScroll,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(Modifier.onSizeChanged { costH = it.height }) {
                        SettingsCard(
                            title = "음주 비용",
                            titleColor = colorResource(id = R.color.color_indicator_money)
                        ) {
                            SettingsOptionGroup(
                                selectedOption = selectedCost,
                                options = listOf("저", "중", "고"),
                                labels = listOf(
                                    "저 (1만원 이하)", "중 (1~5만원)", "고 (5만원 이상)"
                                ),
                                onOptionSelected = { newValue ->
                                    selectedCost = newValue
                                    sharedPref.edit { putString("selected_cost", newValue) }
                                }
                            )
                        }
                    }
                }
                item {
                    Box(Modifier.onSizeChanged { freqH = it.height }) {
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
                                    sharedPref.edit { putString("selected_frequency", newValue) }
                                }
                            )
                        }
                    }
                }
                item {
                    Box(Modifier.onSizeChanged { durH = it.height }) {
                        SettingsCard(
                            title = "음주 시간",
                            titleColor = colorResource(id = R.color.color_indicator_hours)
                        ) {
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
        }

        if (allowScroll) {
            listContent()
        } else {
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                listContent()
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                modifier = Modifier.padding(bottom = 8.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.RadioButton,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelected() }
            .padding(horizontal = 4.dp),
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
            style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyLarge,
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else colorResource(id = R.color.color_text_primary_dark)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() { SettingsScreen() }
