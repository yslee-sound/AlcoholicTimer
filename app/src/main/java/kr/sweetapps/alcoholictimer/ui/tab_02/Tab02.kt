// [NEW] Tab02 리팩토링: 기록 화면을 tab_02 구조로 리팩토링
package kr.sweetapps.alcoholictimer.ui.tab_02

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme

/**
 * [NEW] Tab02 기록 화면 Activity
 * - RecordsScreen을 감싸는 Activity 래퍼
 * - BaseActivity를 상속받아 통일된 UI 제공
 */
class RecordsActivity : BaseActivity() {
    override fun getScreenTitleResId(): Int = R.string.records_title

    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    override fun getScreenTitle(): String = getString(R.string.records_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(content = { Tab02Screen() })
        }
    }
}

/**
 * [NEW] Tab02 메인 화면 Composable
 * - Stateful Container: ViewModel과 연결하여 상태를 관리하고 RecordsScreen에 전달
 * - DiaryViewModel을 통해 Room DB 데이터를 관찰하고 전달
 * ViewModel을 Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
 */
@Composable
fun Tab02Screen(
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onNavigateToAllDiaries: () -> Unit = {},
    onAddRecord: () -> Unit = {},
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {},
    // [NEW] Phase 2: 레벨 파라미터
    currentLevel: kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {},
    viewModel: Tab02ViewModel = viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity
    )
) {
    // [NEW] ViewModel 데이터 구독
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedDetailPeriod by viewModel.selectedDetailPeriod.collectAsState()
    val selectedWeekRange by viewModel.selectedWeekRange.collectAsState()
    val statsData by viewModel.statsState.collectAsState() // [NEW] 실시간 통계 데이터

    // [NEW] DiaryViewModel을 통해 Room DB의 일기 데이터를 실시간으로 관찰
    // Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
    val diaryViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel = viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity
    )
    val allDiaries by diaryViewModel.uiState.collectAsState()

    // [NEW] 최신 3개의 일기만 추출 (이미 timestamp 내림차순 정렬됨)
    val recentDiaries = remember(allDiaries) {
        allDiaries.take(3)
    }

    // [NEW] Context와 초기 값 설정
    val context = LocalContext.current
    val periodWeek = context.getString(R.string.records_period_week)
    val periodMonth = context.getString(R.string.records_period_month)
    val periodYear = context.getString(R.string.records_period_year)

    // 1. "All" 텍스트 리소스 가져오기
    // (R.string.records_period_all 이 없다면 strings.xml에 추가하거나, 임시로 "All" 하드코딩)
    val periodAll = stringResource(id = R.string.records_period_all)

    // 2. 날짜 계산 로직 삭제 (All은 날짜가 필요 없음)

    // [NEW] 화면 진입 시 데이터 로딩 및 초기 기간 설정
    LaunchedEffect(Unit) {
        // 3. 'All'을 기본값으로 초기화 요청
        // ViewModel 내부의 if문 덕분에, 이미 다른 탭을 보고 있었다면 이 요청은 무시됨 (세션 유지)
        viewModel.initializePeriod(periodAll)

        viewModel.loadRecords()
    }

    val filteredRecords = remember(records, selectedPeriod, selectedDetailPeriod, selectedWeekRange) {
        viewModel.getFilteredRecords(periodWeek, periodMonth, periodYear)
    }

    RecordsScreen(
        records = filteredRecords, // 필터링된 기록 (화면 표시용)
        allRecords = records, // [NEW] 전체 기록 (선택기용)
        isLoading = isLoading,
        selectedPeriod = selectedPeriod,
        selectedDetailPeriod = selectedDetailPeriod,
        selectedWeekRange = selectedWeekRange,
        onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
        onDetailPeriodSelected = { viewModel.updateSelectedDetailPeriod(it) },
        onWeekRangeSelected = { viewModel.updateSelectedWeekRange(it) },
        recentDiaries = recentDiaries,
        statsData = statsData, // [NEW] 실시간 통계 데이터 전달
        // [NEW] Phase 2: 레벨 데이터 전달
        currentLevel = currentLevel,
        currentDays = currentDays,
        levelProgress = levelProgress,
        onNavigateToLevelDetail = onNavigateToLevelDetail,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToAllRecords = onNavigateToAllRecords,
        onNavigateToAllDiaries = onNavigateToAllDiaries,
        onAddRecord = onAddRecord,
        onDiaryClick = onDiaryClick
    )
}

/**
 * [NEW] Tab02Screen 프리뷰
 * - RecordsScreen을 있는 그대로 보여주는 프리뷰
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Tab02ScreenPreview() {
    AlcoholicTimerTheme {
        Tab02Screen()
    }
}
