package kr.sweetapps.alcoholictimer.ui.tab_04

import android.os.Bundle
import androidx.activity.compose.BackHandler
import kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel.Tab04ViewModel
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import android.util.Log
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.theme.LocalDimens
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue  // [NEW] 메인 UI 색상

class HabitActivity : BaseActivity() {
    // [FIX] BaseActivity의 TopBar를 숨김
    override fun getScreenTitleResId(): Int? = null
    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    override fun getScreenTitle(): String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [REMOVED] supportActionBar는 ComponentActivity에 없으므로 제거

        setContent {
            // 뒤로가기 버튼: 메인 홈(Start/Run)으로 이동
            BackHandler(enabled = true) {
                navigateToMainHome()
            }

            // [FIX] BaseScreen을 사용하지 않고 HabitScreen이 자체 Scaffold를 가짐
            HabitScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    onNavigateCurrencySettings: () -> Unit = {},
    onApplyAndGoHome: () -> Unit = {},
    viewModel: Tab04ViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as androidx.activity.ComponentActivity
    )
) {
    val context = LocalContext.current

    // [NEW] ViewModel에서 상태 구독
    val selectedCost by viewModel.selectedCost.collectAsState()
    val selectedFrequency by viewModel.selectedFrequency.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()

    // [NEW] 로컬 임시 상태
    var tempCost by remember { mutableStateOf(selectedCost) }
    var tempFrequency by remember { mutableStateOf(selectedFrequency) }
    var tempDuration by remember { mutableStateOf(selectedDuration) }

    // [NEW] 통화 설정 임시 상태
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val isExplicit = prefs.getBoolean("currency_explicit", false)
    val initialCurrency = if (isExplicit) {
        prefs.getString("selected_currency", "AUTO") ?: "AUTO"
    } else {
        "AUTO"
    }
    var tempCurrency by remember { mutableStateOf(initialCurrency) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // [NEW] 변경사항 감지
    val hasChanges = tempCost != selectedCost ||
                     tempFrequency != selectedFrequency ||
                     tempDuration != selectedDuration ||
                     tempCurrency != initialCurrency

    // [NEW] 적용 버튼 클릭 핸들러
    val onApplySettings: () -> Unit = {
        Log.d("HabitScreen", "Apply clicked: tempCost=$tempCost tempFrequency=$tempFrequency tempDuration=$tempDuration tempCurrency=$tempCurrency")

        // [MOD] 설정 저장 로직
        viewModel.updateCost(tempCost)
        viewModel.updateFrequency(tempFrequency)
        viewModel.updateDuration(tempDuration)

        // [NEW] 통화 설정 저장
        try {
            val sp = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            if (tempCurrency == "AUTO") {
                sp.edit().apply {
                    putString("selected_currency", "AUTO")
                    putBoolean("currency_explicit", false)
                    apply()
                }
            } else {
                sp.edit().apply {
                    putString("selected_currency", tempCurrency)
                    putBoolean("currency_explicit", true)
                    apply()
                }
            }
            kr.sweetapps.alcoholictimer.util.manager.CurrencyManager.saveCurrency(context, tempCurrency)
        } catch (e: Exception) {
            Log.e("HabitScreen", "Failed to save currency: ${e.message}")
        }

        Log.d("HabitScreen", "설정이 저장되었습니다 (화면 유지)")

        // [MOD] 화면 이동 제거 -> Toast로 피드백 제공
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.settings_apply_button),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // [NEW] 화면 진입 시 전면 광고 미리 로드 (성능 최적화)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        Log.d("HabitScreen", "Entering Settings -> Preloading Interstitial Ad")
        try {
            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.preload(context)
        } catch (e: Exception) {
            Log.e("HabitScreen", "Failed to preload ad: ${e.message}")
        }
    }

    // [NEW] Scaffold with TopAppBar - 단일 제목줄 + 적용 버튼
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF111111)
                    )
                },
                actions = {
                    TextButton(
                        onClick = onApplySettings,
                        enabled = hasChanges
                    ) {
                        Text(
                            text = stringResource(R.string.settings_apply_button),
                            color = if (hasChanges) MainPrimaryBlue else Color.Gray, // [FIX] 상태에 따른 색상
                            style = MaterialTheme.typography.titleMedium // [FIX] 표준 타이포그래피
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111)
                )
            )
        }
    ) { innerPadding ->
        // [FIX] 기존 Content를 innerPadding으로 감싸서 TopAppBar와 겹치지 않도록 함
        HabitScreenContent(
            innerPadding = innerPadding,
            tempCost = tempCost,
            tempFrequency = tempFrequency,
            tempDuration = tempDuration,
            tempCurrency = tempCurrency,
            onCostChange = { tempCost = it },
            onFrequencyChange = { tempFrequency = it },
            onDurationChange = { tempDuration = it },
            onShowCurrencySheet = { showCurrencySheet = it }
        )
    }

    // [NEW] 통화 선택 BottomSheet
    if (showCurrencySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCurrencySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_currency),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                // AUTO 옵션
                CurrencyOptionRow(
                    isSelected = tempCurrency == "AUTO",
                    label = stringResource(R.string.settings_currency_auto),
                    onSelected = {
                        tempCurrency = "AUTO"
                        showCurrencySheet = false
                    }
                )

                // 지원 통화 목록
                kr.sweetapps.alcoholictimer.util.manager.CurrencyManager.supportedCurrencies.forEach { currency ->
                    CurrencyOptionRow(
                        isSelected = tempCurrency == currency.code,
                        label = context.getString(currency.nameResId),
                        onSelected = {
                            tempCurrency = currency.code
                            showCurrencySheet = false
                        }
                    )
                }
            }
        }
    }
}

// [NEW] HabitScreen 콘텐츠 분리
@Composable
fun HabitScreenContent(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    tempCost: String,
    tempFrequency: String,
    tempDuration: String,
    tempCurrency: String,
    onCostChange: (String) -> Unit,
    onFrequencyChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onShowCurrencySheet: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    // [FIX] innerPadding을 전체에 적용하지 않고, 상단/하단을 Spacer로 분리하여 적용
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState),
        // [REMOVED] .padding(innerPadding) 제거 - 전체 적용 방식의 문제
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
            // [1] 상단 여백 확보 (TopBar 높이만큼)
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))

            HabitSection(
                title = stringResource(R.string.settings_drinking_cost),
                titleColor = Color.Black
            ) {
                HabitOptionGroup(
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
                        onCostChange(newValue)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)


            HabitSection(
                title = stringResource(R.string.settings_drinking_frequency),
                titleColor = Color.Black
            ) {
                HabitOptionGroup(
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
                        onFrequencyChange(newValue)
                    }
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            HabitSection(
                title = stringResource(R.string.settings_drinking_duration),
                titleColor = Color.Black
            ) {
                HabitOptionGroup(
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
                        onDurationChange(newValue)
                    }
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // [MOD] 통화 설정 섹션 (음주 시간 아래로 이동)
            HabitSection(
                title = stringResource(R.string.settings_currency),
                titleColor = Color.Black
            ) {
                // 통화 선택 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onShowCurrencySheet(true) }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (tempCurrency == "AUTO") stringResource(R.string.settings_currency_auto) else tempCurrency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.color_text_primary_dark)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_caret_right),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // [2] 하단 여백 확보 (BottomBar 높이 + 추가 여유 20dp)
            // 스크롤 끝까지 내렸을 때 탭바에 가려지지 않도록
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 20.dp))
    }
}

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
            .padding(horizontal = 20.dp),
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


@Composable
fun HabitSection(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            // 상단/하단 패딩을 통일하여 각 섹션 간 간격을 일정하게 함
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun HabitOptionGroup(
    selectedOption: String,
    options: List<String>,
    labels: List<String>,
    onOptionSelected: (String) -> Unit
) {
    // 그룹 단위로 수평 패딩을 적용하고, 옵션 사이의 간격을 8.dp로 통일
    Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
        options.forEachIndexed { index, option ->
            HabitOptionItem(
                isSelected = selectedOption == option,
                label = labels[index],
                onSelected = { onOptionSelected(option) }
            )
        }
    }
}

@Composable
fun HabitOptionItem(
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
                selectedColor = MainPrimaryBlue,  // [FIX] 메인 UI 색상 적용 (#1E40AF)
                unselectedColor = colorResource(id = R.color.color_radio_unselected)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MainPrimaryBlue else colorResource(  // [FIX] 메인 UI 색상 적용
                id = R.color.color_text_primary_dark
            )
        )
    }
}

@Composable
fun HabitMenuWithSwitch(
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
            .padding(horizontal = 20.dp),
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
fun HabitScreenPreview() { HabitScreen() }

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
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        }
    }
}
