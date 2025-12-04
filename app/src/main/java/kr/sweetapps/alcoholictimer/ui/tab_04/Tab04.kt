package kr.sweetapps.alcoholictimer.ui.tab_04

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.MainApplication
import android.util.Log
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.ui.theme.LocalDimens

class SettingsActivity : BaseActivity() {
    // [NEW] Tab04은 '더보기'로 표시되어야 하므로 제목 리소스를 more_title로 변경
    override fun getScreenTitleResId(): Int = R.string.more_title
    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    override fun getScreenTitle(): String = getString(R.string.more_title)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 뒤로가기 버튼: 메인 홈(Start/Run)으로 이동
            BackHandler(enabled = true) {
                navigateToMainHome()
            }

            // AdmobBanner is moved to MainActivity's BaseScaffold during Phase-1 migration
            BaseScreen(content = { SettingsScreen() })
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateCurrencySettings: () -> Unit = {},
    onApplyAndGoHome: () -> Unit = {}, // [NEW] 호출 시 Tab1으로 이동
    viewModel: Tab04ViewModel = viewModel()
) {
    val context = LocalContext.current

    // [NEW] ViewModel에서 상태 구독
    val selectedCost by viewModel.selectedCost.collectAsState()
    val selectedFrequency by viewModel.selectedFrequency.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()

    // [NEW] 로컬 임시 상태: 사용자는 옵션을 선택해도 즉시 저장되지 않고 Apply 버튼으로 저장됨
    var tempCost by remember { mutableStateOf(selectedCost) }
    var tempFrequency by remember { mutableStateOf(selectedFrequency) }
    var tempDuration by remember { mutableStateOf(selectedDuration) }

    val safePadding = LocalSafeContentPadding.current
    val scrollState = rememberScrollState()

    val application = context.applicationContext as MainApplication
    val umpConsentManager = application.umpConsentManager

    // UMP consent manager available as `umpConsentManager`. If you need reactive
    // consent state in this screen later, re-enable collection from
    // `umpConsentManager.isPrivacyOptionsRequired` / `isPersonalizedAdsAllowed`.


    // 전체 바탕 흰색 + 스크롤 가능한 목록형 레이아웃
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Content area (scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 8.dp)
                .padding(safePadding)
                // reserve bottom space so content is not obscured by the sticky button
                .padding(bottom = safePadding.calculateBottomPadding() + 92.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsSection(
                title = stringResource(R.string.settings_drinking_cost),
                titleColor = Color.Black
            ) {
                SettingsOptionGroup(
                    selectedOption = tempCost,
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
                        // [NEW] 임시 상태만 변경 (저장은 Apply 버튼에서)
                        tempCost = newValue
                    }
                )
            }

            // 통화 설정 메뉴를 음주 비용 섹션 하단으로 이동
            // Add a small spacing before the currency row and keep only a single thin divider below
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            SimpleAboutRow(
                title = stringResource(R.string.settings_currency),
                onClick = onNavigateCurrencySettings,
                trailing = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            // 얇은 구분선: 아래 섹션과 구분 (single thin line)
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            SettingsSection(
                title = stringResource(R.string.settings_drinking_frequency),
                titleColor = Color.Black
            ) {
                SettingsOptionGroup(
                    selectedOption = tempFrequency,
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
                        tempFrequency = newValue
                    }
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            SettingsSection(
                title = stringResource(R.string.settings_drinking_duration),
                titleColor = Color.Black
            ) {
                SettingsOptionGroup(
                    selectedOption = tempDuration,
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
                        tempDuration = newValue
                    }
                )
            }
        }

        // [NEW] 하단 고정 Apply 버튼 — 스크롤 영역 밖에 배치되어 항상 표시
        val activity = (context as? android.app.Activity)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val hasChanges = tempCost != selectedCost || tempFrequency != selectedFrequency || tempDuration != selectedDuration

                Button(
                    onClick = {
                        Log.d("SettingsScreen", "Apply clicked: tempCost=$tempCost tempFrequency=$tempFrequency tempDuration=$tempDuration")

                        // 1) 저장
                        viewModel.updateCost(tempCost)
                        viewModel.updateFrequency(tempFrequency)
                        viewModel.updateDuration(tempDuration)
                        Log.d("SettingsScreen", "Saved to ViewModel (async)")

                        // [NEW] 저장 완료 Toast 메시지 (광고 전에 표시)
                        android.widget.Toast.makeText(
                            context,
                            "설정이 저장되었습니다.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        // 2) 플래그 설정: Tab1에서 스낵바 표시
                        try {
                            val sp = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                            sp.edit().putBoolean("settings_applied_snackbar_pending", true).apply()
                            Log.d("SettingsScreen", "Set settings_applied_snackbar_pending=true in sharedPref")
                        } catch (_: Throwable) {}

                        // 3) [수익화] 광고 정책 체크 및 전면광고 호출 후 Tab1으로 이동
                        val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                        // 화면 종료 함수
                        val finishAndGoHome: () -> Unit = {
                            Log.d("SettingsScreen", "설정 완료 -> Tab1으로 이동")
                            onApplyAndGoHome()
                        }

                        if (shouldShowAd && activity != null) {
                            Log.d("SettingsScreen", "광고 정책 통과 -> 전면 광고 노출")
                            if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                                kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { success ->
                                    Log.d("SettingsScreen", "광고 결과: success=$success -> Tab1 이동")
                                    finishAndGoHome()
                                }
                            } else {
                                Log.d("SettingsScreen", "광고 로드 안됨 -> 즉시 Tab1 이동")
                                finishAndGoHome()
                            }
                        } else {
                            Log.d("SettingsScreen", "광고 쿨타임 중 또는 activity null -> 즉시 Tab1 이동")
                            finishAndGoHome()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    enabled = hasChanges,
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.color_accent_blue))
                ) {
                    Text(text = "적용하고 절약 금액 확인하기 >", color = Color.White)
                }
            }
        }
    }
}


@Composable
fun SettingsSection(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            // 상단/하단 패딩을 통일하여 각 섹션 간 간격을 일정하게 함
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
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
    // 그룹 단위로 수평 패딩을 적용하고, 옵션 사이의 간격을 8.dp로 통일
    Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
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
            .height(56.dp)
            .clickable(
                role = Role.RadioButton,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelected() },
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
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else colorResource(
                id = R.color.color_text_primary_dark
            )
        )
    }
}

@Composable
fun SettingsMenuWithSwitch(
    title: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val dims = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.component.listItemHeight)
            .clickable { onClick() }
            .padding(horizontal = dims.padding.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        // Switch의 기본 minHeight가 큰 편이라 Row 높이를 밀어냄 -> 크기를 줄여 일치시킵니다
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.height(36.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() { SettingsScreen() }

@Composable
fun SimpleAboutRow(
    title: String,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    val dims = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.component.listItemHeight)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = dims.padding.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        }
    }
}
