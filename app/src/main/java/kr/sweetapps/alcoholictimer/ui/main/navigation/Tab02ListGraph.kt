package kr.sweetapps.alcoholictimer.ui.main.navigation

import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.main.Screen

/**
 * Tab 02 - 목록 화면 (탭 내부용)
 * - Records List: 기록 목록 화면만 포함 (탭바 표시)
 */
fun NavGraphBuilder.addTab02ListGraph(
    navController: NavHostController,
    activity: Activity?,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToDiaryWrite: (Long?) -> Unit, // [FIX] 선택된 날짜 파라미터 추가 (2025-12-22)
    onNavigateToAllRecords: () -> Unit,
    onNavigateToAllDiaries: () -> Unit,
    onNavigateToDiaryDetail: (String) -> Unit,
    onNavigateToLevelDetail: () -> Unit, // [NEW] Phase 2: 레벨 상세 화면 이동
    refreshSignal: Int
) {
    // Tab 2: 중첩 그래프 (목록만)
    navigation(startDestination = "records_list", route = Screen.Records.route) {

        composable("records_list") {
            val tab02ViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel =
                (activity as? ViewModelStoreOwner)?.let { owner ->
                    viewModel(viewModelStoreOwner = owner)
                } ?: viewModel()

            // [NEW] Phase 2: Tab03ViewModel에서 레벨 데이터 가져오기
            val tab03ViewModel: kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab03ViewModel =
                (activity as? androidx.activity.ComponentActivity)?.let { owner ->
                    viewModel(viewModelStoreOwner = owner)
                } ?: viewModel()

            val currentLevel by tab03ViewModel.currentLevel.collectAsState()
            val levelDays by tab03ViewModel.levelDays.collectAsState()

            val pendingRoute by tab02ViewModel.pendingDetailRoute.collectAsState()

            LaunchedEffect(pendingRoute) {
                pendingRoute?.let { route ->
                    // [NEW] 루트 네비게이션으로 detail 이동
                    android.util.Log.d("NavGraph", "[목록] pendingRoute 감지 -> root 이동: $route")
                    onNavigateToDetail(route)
                    tab02ViewModel.consumePendingDetailRoute()
                }
            }

            LaunchedEffect(refreshSignal) {
                if (refreshSignal > 0) {
                    // [NEW] 상세 화면에서 돌아온 후 목록 새로고침
                    tab02ViewModel.loadRecords()
                }
            }

            kr.sweetapps.alcoholictimer.ui.tab_02.Tab02Screen(
                onNavigateToDetail = { record: SobrietyRecord ->
                    try { AnalyticsManager.logViewRecordDetail(record.id) } catch (_: Throwable) {}
                    val route = Screen.Detail.createRoute(
                        startTime = record.startTime,
                        endTime = record.endTime,
                        targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                        actualDays = kotlin.math.round(record.actualDays).toInt().coerceAtLeast(0),
                        isCompleted = record.isCompleted
                    )
                    onNavigateToDetail(route)
                },
                onNavigateToAllRecords = { onNavigateToAllRecords() },
                onNavigateToAllDiaries = { onNavigateToAllDiaries() },
                onNavigateToDiaryWrite = { selectedDate -> onNavigateToDiaryWrite(selectedDate) }, // [FIX] 선택된 날짜 전달 (2025-12-22)
                onNavigateToDiaryDetail = { route -> onNavigateToDiaryDetail(route) }, // [NEW] 일기 수정용 전달 (2025-12-23)
                onAddRecord = {
                    // 예전 FAB 콜백 - 필요시 유지
                    onNavigateToDiaryWrite(null) // [FIX] 오늘 날짜로 작성 (2025-12-22)
                },
                onDiaryClick = { diary ->
                    val route = Screen.DiaryDetail.createRoute(diary.id.toString())
                    onNavigateToDiaryDetail(route)
                },
                currentLevel = currentLevel,
                currentDays = levelDays,
                levelProgress = tab03ViewModel.calculateProgress(),
                onNavigateToLevelDetail = onNavigateToLevelDetail
            )
        }
    }
}
