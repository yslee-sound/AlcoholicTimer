package kr.sweetapps.alcoholictimer.ui.tab_03.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager

/**
 * 통화 설정 화면 (tab_04에서 관리)
 */
@Composable
fun CurrencyScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    // 상태: 실제 선택된 통화 코드(해당 통화 코드)와 사용자가 선택한 키(AUTO 또는 명시적 코드)를 분리
    val prefs = context.getSharedPreferences("settings", 0)
    val isExplicit = prefs.getBoolean("currency_explicit", false)
    val selectedCodeState = remember { mutableStateOf(CurrencyManager.getSelectedCurrency(context).code) }
    val selectedKeyState = remember { mutableStateOf(if (isExplicit) selectedCodeState.value else "AUTO") }

    // [FIX] Scaffold로 감싸서 하단 시스템 바 투명화 방지
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White, // [FIX] 하단 비침 방지 (흰색 배경 고정)
        contentWindowInsets = WindowInsets.systemBars, // [FIX] 시스템 바 영역 침범 방지
        topBar = {
            BackTopBar(title = stringResource(R.string.settings_currency), onBack = onBack)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(modifier = Modifier.padding(vertical = 8.dp).weight(1f)) {
                // Auto / Locale-based 기본 통화 항목
                item {
                    val label = stringResource(R.string.settings_currency_auto)
                    val isSelected = selectedKeyState.value == "AUTO"
                    val onSelect: () -> Unit = {
                        // [NEW] Analytics: 설정 변경 추적 (2025-12-31)
                        val oldValue = selectedKeyState.value

                        // [FIX] 시스템 설정 모드로 저장 (explicit: false) (2026-01-02)
                        CurrencyManager.saveCurrency(context, "AUTO", explicit = false)
                        selectedKeyState.value = "AUTO"
                        // 업데이트: 현재 해석된 통화 코드도 갱신
                        selectedCodeState.value = CurrencyManager.getSelectedCurrency(context).code

                        // [NEW] Analytics 전송 (2025-12-31)
                        try {
                            kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logSettingsChange(
                                settingType = "currency",
                                oldValue = oldValue,
                                newValue = "AUTO"
                            )
                            android.util.Log.d("CurrencyScreen", "Analytics: settings_change sent (currency: $oldValue → AUTO)")
                        } catch (e: Exception) {
                            android.util.Log.e("CurrencyScreen", "Failed to log settings_change", e)
                        }
                        Unit
                    }
                    CurrencyOptionRow(isSelected = isSelected, label = label, onSelected = onSelect)
                }

                items(CurrencyManager.supportedCurrencies) { currency ->
                    val isSelected = selectedKeyState.value == currency.code
                    CurrencyOptionRow(
                        isSelected = isSelected,
                        label = context.getString(currency.nameResId),
                        onSelected = {
                            // [NEW] Analytics: 설정 변경 추적 (2025-12-31)
                            val oldValue = selectedKeyState.value

                            // [FIX] 명시적 통화 선택 (explicit: true) (2026-01-02)
                            CurrencyManager.saveCurrency(context, currency.code, explicit = true)
                            selectedKeyState.value = currency.code
                            selectedCodeState.value = currency.code

                            // [NEW] Analytics 전송 (2025-12-31)
                            try {
                                kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logSettingsChange(
                                    settingType = "currency",
                                    oldValue = oldValue,
                                    newValue = currency.code
                                )
                                android.util.Log.d("CurrencyScreen", "Analytics: settings_change sent (currency: $oldValue → ${currency.code})")
                            } catch (e: Exception) {
                                android.util.Log.e("CurrencyScreen", "Failed to log settings_change", e)
                            }
                        }
                    )
                }
            }

            // 추가: 현재 선택 통화 표시 (간단)
            Text(
                text = if (selectedKeyState.value == "AUTO")
                    stringResource(R.string.settings_currency_current_auto, selectedCodeState.value)
                else
                    stringResource(R.string.settings_currency_current, selectedCodeState.value),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MainPrimaryBlue, // [NEW] 프라이머리 블루 색상 적용
                modifier = Modifier.padding(16.dp)
            )
        }
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
                selectedColor = MainPrimaryBlue,  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                unselectedColor = colorResource(id = R.color.color_radio_unselected)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MainPrimaryBlue else MaterialTheme.colorScheme.onSurface  // [FIX] 메인 UI 색상 적용
        )
    }
}

