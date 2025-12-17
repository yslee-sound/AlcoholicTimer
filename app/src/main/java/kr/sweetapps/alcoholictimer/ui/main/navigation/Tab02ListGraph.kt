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
    onNavigateToDiaryWrite: () -> Unit,
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
                    // [NEW] 루트 네비게이션으로 전달하여 탭 위에 상세 화면 표시
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
                onAddRecord = {
                    // [NEW] 일기 작성 진입도 루트 콜백으로 분리
                    onNavigateToDiaryWrite()
                },
                onDiaryClick = { diary ->
                    val route = Screen.DiaryDetail.createRoute(diary.id.toString())
                    onNavigateToDiaryDetail(route)
                },
                // [NEW] Phase 2: 레벨 데이터 전달
                currentLevel = currentLevel,
                currentDays = levelDays,
                levelProgress = tab03ViewModel.calculateProgress(),
                onNavigateToLevelDetail = onNavigateToLevelDetail
            )
        }
    }
}
