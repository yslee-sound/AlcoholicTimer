package kr.sweetapps.alcoholictimer.ui.tab_04.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BackTopBar
import kr.sweetapps.alcoholictimer.core.util.CurrencyManager

/**
 * 통화 설정 화면 (tab_04에서 관리)
 */
@Composable
fun CurrencyScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        BackTopBar(title = "통화 설정", onBack = onBack)

        // 상태: 실제 선택된 통화 코드(해당 통화 코드)와 사용자가 선택한 키(AUTO 또는 명시적 코드)를 분리
        val prefs = context.getSharedPreferences("settings", 0)
        val isExplicit = prefs.getBoolean("currency_explicit", false)
        val selectedCodeState = remember { mutableStateOf(CurrencyManager.getSelectedCurrency(context).code) }
        val selectedKeyState = remember { mutableStateOf(if (isExplicit) selectedCodeState.value else "AUTO") }

        LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
            // Auto / Locale-based 기본 통화 항목
            item {
                val label = "자동(지역 기반)"
                val isSelected = selectedKeyState.value == "AUTO"
                val onSelect = {
                    // 저장: AUTO 모드로 변경
                    CurrencyManager.saveCurrency(context, "AUTO")
                    prefs.edit { putBoolean("currency_explicit", false) }
                    selectedKeyState.value = "AUTO"
                    // 업데이트: 현재 해석된 통화 코드도 갱신
                    selectedCodeState.value = CurrencyManager.getSelectedCurrency(context).code
                }
                CurrencyOptionRow(isSelected = isSelected, label = label, onSelected = onSelect)
            }

            items(CurrencyManager.supportedCurrencies) { currency ->
                val isSelected = selectedKeyState.value == currency.code
                CurrencyOptionRow(
                    isSelected = isSelected,
                    label = context.getString(currency.nameResId),
                    onSelected = {
                        CurrencyManager.saveCurrency(context, currency.code)
                        // mark explicit selection
                        prefs.edit { putBoolean("currency_explicit", true) }
                        selectedKeyState.value = currency.code
                        selectedCodeState.value = currency.code
                    }
                )
            }
        }

        // 추가: 현재 선택 통화 표시 (간단)
        Text(
            text = if (selectedKeyState.value == "AUTO") "현재 선택: ${selectedCodeState.value} (자동)" else "현재 선택: ${selectedCodeState.value}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * CurrencyOptionRow는 원래 tab_05에 있던 것을 tab_04에서 관리하도록 이동했습니다.
 */
@Composable
fun CurrencyOptionRow(
    isSelected: Boolean,
    label: String,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelected() }
            .height(56.dp)
            .padding(horizontal = 16.dp),
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
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else MaterialTheme.colorScheme.onSurface
        )
    }
}

