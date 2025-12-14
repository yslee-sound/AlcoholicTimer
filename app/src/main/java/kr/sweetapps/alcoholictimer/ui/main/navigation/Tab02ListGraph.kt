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
import androidx.navigation.compose.currentBackStackEntryAsState
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
    activity: Activity?
) {
    // Tab 2: 중첩 그래프 (목록만)
    navigation(startDestination = "records_list", route = Screen.Records.route) {

        composable("records_list") {
            val tab02ViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel =
                (activity as? ViewModelStoreOwner)?.let { owner ->
                    viewModel(viewModelStoreOwner = owner)
                } ?: viewModel()

            val pendingRoute by tab02ViewModel.pendingDetailRoute.collectAsState()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            LaunchedEffect(pendingRoute, currentRoute) {
                pendingRoute?.let { route ->
                    if (currentRoute != route) {
                        android.util.Log.d("NavGraph", "[목록] pendingRoute 감지: $route (현재: $currentRoute) -> 자동 이동")
                        navController.navigate(route)
                    } else {
                        android.util.Log.d("NavGraph", "[목록] 이미 해당 route에 있음 -> navigate 건너뜀")
                    }
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
                    navController.navigate(route)
                },
                onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) },
                onNavigateToAllDiaries = { navController.navigate(Screen.AllDiary.route) },
                onAddRecord = { navController.navigate(Screen.DiaryWrite.route) },
                onDiaryClick = { diary ->
                    val route = Screen.DiaryDetail.createRoute(diary.id.toString())
                    navController.navigate(route)
                }
            )
        }
    }
}

