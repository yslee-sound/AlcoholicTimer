/**
 * Tab 04: Community Screen (커뮤니티 - 익명 응원 챌린지)
 *
 * [REFACTORED 2025-12-19]
 * - 폴더명: tab_04 (변경하지 않음 - 안전성 우선)
 * - 실제 의미: Community (커뮤니티)
 * - 기능: 익명 게시글 작성/조회, 이미지 업로드, 24시간 자동 삭제
 * - 접근 경로: 하단 탭 3번 (커뮤니티)
 *
 * 하위 화면:
 * - CommunityScreen: 커뮤니티 피드 (메인)
 * - WritePostScreen: 게시글 작성
 */
package kr.sweetapps.alcoholictimer.ui.tab_03.screens.settings

import android.os.Bundle
import androidx.activity.compose.BackHandler
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab04ViewModel
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
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
    viewModel: Tab04ViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val context = LocalContext.current

    // [REFACTORED] ViewModel 상태 직접 사용 (임시 변수 제거)
    val selectedCost by viewModel.selectedCost.collectAsState()
    val selectedFrequency by viewModel.selectedFrequency.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()

    // [REMOVED] 통화 설정 관련 상태 제거 - Tab05로 이동됨

    // [NEW] 화면 진입 시 전면 광고 미리 로드 (성능 최적화)
    LaunchedEffect(Unit) {
        Log.d("HabitScreen", "Entering Settings -> Preloading Interstitial Ad")
        try {
            InterstitialAdManager.preload(context)
        } catch (e: Exception) {
            Log.e("HabitScreen", "Failed to preload ad: ${e.message}")
        }
    }

    // [REFACTORED] Scaffold - '적용' 버튼 제거, 제목만 표시
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111)
                )
            )
        }
    ) { innerPadding ->
        // [REFACTORED] ViewModel 직접 연결 (즉시 저장)
        HabitScreenContent(
            innerPadding = innerPadding,
            selectedCost = selectedCost,
            selectedFrequency = selectedFrequency,
            selectedDuration = selectedDuration,
            onCostChange = { newValue ->
                viewModel.updateCost(newValue)
                Log.d("HabitScreen", "비용 즉시 저장: $newValue")
            },
            onFrequencyChange = { newValue ->
                viewModel.updateFrequency(newValue)
                Log.d("HabitScreen", "빈도 즉시 저장: $newValue")
            },
            onDurationChange = { newValue ->
                viewModel.updateDuration(newValue)
                Log.d("HabitScreen", "시간 즉시 저장: $newValue")
            }
        )
    }

    // [REMOVED] 통화 선택 BottomSheet 제거 - Tab05의 독립 메뉴로 이동됨
}

// [REFACTORED] HabitScreen 콘텐츠 - 즉시 저장 방식
@Composable
fun HabitScreenContent(
    innerPadding: PaddingValues,
    selectedCost: String,
    selectedFrequency: String,
    selectedDuration: String,
    onCostChange: (String) -> Unit,
    onFrequencyChange: (String) -> Unit,
    onDurationChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    // [FIX] innerPadding을 전체에 적용하지 않고, 상단/하단을 Spacer로 분리하여 적용
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // [1] 상단 여백 확보 (TopBar 높이만큼)
        Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))

        HabitSection(
            title = stringResource(R.string.settings_drinking_cost),
            titleColor = Color.Black
        ) {
            HabitOptionGroup(
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
                onOptionSelected = onCostChange // 즉시 저장
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

        HabitSection(
            title = stringResource(R.string.settings_drinking_frequency),
            titleColor = Color.Black
        ) {
            HabitOptionGroup(
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
                onOptionSelected = onFrequencyChange // 즉시 저장
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

        HabitSection(
            title = stringResource(R.string.settings_drinking_duration),
            titleColor = Color.Black
        ) {
            HabitOptionGroup(
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
                onOptionSelected = onDurationChange // 즉시 저장
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

        // [REMOVED] 통화 설정 섹션 제거 - Tab05의 독립 메뉴로 이동됨
        // [2] 하단 여백 확보 (BottomBar 높이 + 추가 여유 50dp)
        Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 50.dp))
    }
}

// [REMOVED] CurrencyOptionRow 함수 삭제 - CurrencyScreen.kt에 이미 정의되어 있음

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
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.height(36.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HabitScreenPreview() { HabitScreen() }

// [NEW] Tab05에서 사용할 독립 습관 설정 화면 (뒤로가기 TopBar 포함)
// 기존 Tab04의 HabitScreen 로직을 재사용하되, DocumentScreen 스타일의 TopBar 추가
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitSettingsScreen(
    onBack: () -> Unit,
    onNavigateCurrencySettings: () -> Unit = {},
    viewModel: Tab04ViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val context = LocalContext.current

    // [REFACTORED] ViewModel 상태 직접 사용
    val selectedCost by viewModel.selectedCost.collectAsState()
    val selectedFrequency by viewModel.selectedFrequency.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()

    // [REMOVED] 통화 설정 관련 상태 제거 - Tab05 독립 메뉴로 이동됨

    // [NEW] 화면 진입 시 전면 광고 미리 로드
    LaunchedEffect(Unit) {
        Log.d("HabitSettingsScreen", "Entering Settings -> Preloading Interstitial Ad")
        try {
            InterstitialAdManager.preload(context)
        } catch (e: Exception) {
            Log.e("HabitSettingsScreen", "Failed to preload ad: ${e.message}")
        }
    }

    // [NEW] Scaffold with BackTopBar (DocumentScreen 스타일)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White, // [FIX] 하단 비침 방지 (흰색 배경 고정)
        contentWindowInsets = WindowInsets.systemBars, // [FIX] 시스템 바 영역 침범 방지
        topBar = {
            BackTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        // 기존 HabitScreenContent 재사용
        HabitScreenContent(
            innerPadding = innerPadding,
            selectedCost = selectedCost,
            selectedFrequency = selectedFrequency,
            selectedDuration = selectedDuration,
            onCostChange = { newValue ->
                viewModel.updateCost(newValue)
                Log.d("HabitSettingsScreen", "비용 즉시 저장: $newValue")
            },
            onFrequencyChange = { newValue ->
                viewModel.updateFrequency(newValue)
                Log.d("HabitSettingsScreen", "빈도 즉시 저장: $newValue")
            },
            onDurationChange = { newValue ->
                viewModel.updateDuration(newValue)
                Log.d("HabitSettingsScreen", "시간 즉시 저장: $newValue")
            }
        )
    }

    // [REMOVED] 통화 선택 BottomSheet 제거 - Tab05의 독립 메뉴로 이동됨
}
// [REMOVED] SimpleAboutRow 함수 삭제 - SettingsScreen.kt에 이미 정의되어 있음
