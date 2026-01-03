package kr.sweetapps.alcoholictimer.ui.tab_02

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryDetailFeedScreen
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
    onNavigateToDiaryWrite: (Long?) -> Unit = {}, // [FIX] 선택된 날짜 타임스탬프 전달 (2025-12-22)
    onNavigateToDiaryDetail: (String) -> Unit = {}, // [NEW] 일기 수정용 네비게이션 (2025-12-23)
    onAddRecord: () -> Unit = {},
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {},
    // [NEW] Phase 2: 레벨 파라미터
    currentLevel: LevelDefinitions.LevelInfo? = null,
    currentDays: Int = 0,
    levelProgress: Float = 0f,
    onNavigateToLevelDetail: () -> Unit = {},
    viewModel: Tab02ViewModel = viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity
    )
) {
    // [DEBUG v18] 리컴포지션 추적 (2026-01-03)
    android.util.Log.d("Tab02Screen", "🔄 RECOMPOSITION!")

    // [CRITICAL] 일기 상세 피드 화면 표시 상태 - remember로 변경하여 탭 이동 시 자동 초기화 (2025-12-27)
    var selectedDetailDiaryId by remember { mutableStateOf<Long?>(null) }

    // [REMOVED] LaunchedEffect 제거 - BottomNavBar의 restoreState 제어로 충분 (2025-12-27)

    // [NEW] ViewModel 데이터 구독
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedDetailPeriod by viewModel.selectedDetailPeriod.collectAsState()
    val selectedWeekRange by viewModel.selectedWeekRange.collectAsState()
    val statsData by viewModel.statsState.collectAsState() // [NEW] 실시간 통계 데이터
    val realTimeLevelState by viewModel.levelState.collectAsState() // [CHANGED] 누적 일수 기준 레벨 상태 (과거 기록 + 현재 타이머) (2025-12-25)
    val startTime by viewModel.startTime.collectAsState() // [NEW] 타이머 시작 시각 (인디케이터 표시용) (2026-01-02)
    val isTimerCompleted by viewModel.isTimerCompleted.collectAsState() // [NEW] 타이머 완료 여부 (인디케이터 색상 제어) (2026-01-02)

    // [NEW] DiaryViewModel을 통해 Room DB의 일기 데이터를 실시간으로 관찰
    // Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
    val diaryViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel = viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity
    )
    val allDiaries by diaryViewModel.uiState.collectAsState() // [중요] collectAsState()로 실시간 관찰 (2025-12-22)

    // [NEW] 최신 3개의 일기만 추출 (이미 timestamp 내림차순 정렬됨)
    val recentDiaries = remember(allDiaries) {
        allDiaries.take(3)
    }

    // [NEW] Context와 초기 값 설정
    val context = LocalContext.current

    // [FIX v16] 리소스 문자열 캐싱으로 리컴포지션 시 재계산 방지 (2026-01-03)
    val periodWeek = remember { context.getString(R.string.records_period_week) }
    val periodMonth = remember { context.getString(R.string.records_period_month) }
    val periodYear = remember { context.getString(R.string.records_period_year) }
    val periodAll = remember { context.getString(R.string.records_period_all) }

    // 2. 날짜 계산 로직 삭제 (All은 날짜가 필요 없음)

    // [FIX v15] 화면 진입 시 데이터 로딩 및 초기 기간 설정 (2026-01-03)
    // loadRecordsOnInit으로 변경하여 탭 전환 시 깜빡임 방지
    LaunchedEffect(Unit) {
        // 3. 'All'을 기본값으로 초기화 요청
        // ViewModel 내부의 if문 덕분에, 이미 다른 탭을 보고 있었다면 이 요청은 무시됨 (세션 유지)
        viewModel.initializePeriod(periodAll)

        // [FIX v15] 초기화 체크 후 로딩 (이미 데이터가 있으면 로딩 스킵)
        viewModel.loadRecordsOnInit()
    }

    val filteredRecords = remember(records, selectedPeriod, selectedDetailPeriod, selectedWeekRange) {
        viewModel.getFilteredRecords(periodWeek, periodMonth, periodYear)
    }

    // [NEW] Box로 감싸서 일기 상세 피드 화면을 덮어씌울 수 있도록 구성 (2025-12-22)
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 기본 기록 화면 (캘린더 포함)
        RecordsScreen(
            records = filteredRecords,
            allRecords = records,
            isLoading = isLoading,
            selectedPeriod = selectedPeriod,
            selectedDetailPeriod = selectedDetailPeriod,
            selectedWeekRange = selectedWeekRange,
            onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
            onDetailPeriodSelected = { viewModel.updateSelectedDetailPeriod(it) },
            onWeekRangeSelected = { viewModel.updateSelectedWeekRange(it) },
            recentDiaries = recentDiaries,
            allDiaries = allDiaries, // [NEW] 전체 일기 전달 (캘린더용) (2025-12-22)
            statsData = statsData,
            // [CHANGED] 레벨 데이터는 realTimeLevelState에서 가져옴 (전체 누적 일수 기준) (2025-12-25)
            currentLevel = realTimeLevelState.currentLevel,
            currentDays = realTimeLevelState.currentDays,
            levelProgress = realTimeLevelState.progress,
            startTime = startTime, // [NEW] 인디케이터 표시용 (2026-01-02)
            isTimerCompleted = isTimerCompleted, // [NEW] 인디케이터 색상 제어용 (2026-01-02)
            onNavigateToLevelDetail = onNavigateToLevelDetail,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToAllRecords = onNavigateToAllRecords,
            onNavigateToAllDiaries = onNavigateToAllDiaries,
            onNavigateToDiaryWrite = onNavigateToDiaryWrite, // [NEW] 일기 작성 콜백 전달
            onAddRecord = onAddRecord,
            onDiaryClick = onDiaryClick,
            onNavigateToDiaryDetail = { id ->
                // [NEW] 일기 상세 피드 화면 트리거 (2025-12-22)
                selectedDetailDiaryId = id
            }
        )

        // 2. [NEW] 일기 상세 피드 화면 (ID가 있을 때만 덮어씌움) (2025-12-22)
        if (selectedDetailDiaryId != null) {
            DiaryDetailFeedScreen(
                targetDiaryId = selectedDetailDiaryId!!,
                onBack = {
                    // 뒤로가기 시 상세 화면 닫기
                    selectedDetailDiaryId = null
                },
                onEditClick = { id ->
                    // [FIX] 일기 수정: DiaryDetail 라우트로 이동 (selectedDetailDiaryId 유지) (2025-12-23)
                    // selectedDetailDiaryId를 유지하여 수정 후 다시 상세 화면으로 돌아옴
                    val route = kr.sweetapps.alcoholictimer.ui.main.Screen.DiaryDetail.createRoute(id.toString())
                    onNavigateToDiaryDetail(route)
                },
                onDeleteClick = { id ->
                    // [FIX] 삭제 로직 - 화면 유지, Toast만 표시 (2025-12-23)
                    diaryViewModel.deleteDiary(id)
                    android.widget.Toast.makeText(
                        context,
                        "일기가 삭제되었습니다",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // selectedDetailDiaryId는 유지하여 화면이 닫히지 않도록 함
                },
                diaryViewModel = diaryViewModel
            )
        }
    }
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
