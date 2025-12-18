package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_03.LevelScreen

/**
 * [DEPRECATED] Tab 03: 레벨 시스템 네비게이션 그래프
 * - 3탭 구조로 변경되면서 더 이상 사용되지 않음
 * - 레벨은 이제 Tab 2의 레벨 배너를 통해 LevelDetail 화면으로 진입
 * - 이 파일은 호환성을 위해 남겨두지만 AppNavHost에서 호출되지 않음
 */
@Deprecated("Tab 3 removed - Use LevelDetail screen instead", ReplaceWith("Screen.LevelDetail.route"))
fun NavGraphBuilder.addTab03Graph(navController: NavHostController) {
    // [REMOVED] Screen.Level이 주석 처리되어 더 이상 사용할 수 없음
    // LevelDetail 화면을 대신 사용하세요
    /*
    composable(Screen.Level.route) {
        LevelScreen(onNavigateBack = {
            if (!navController.popBackStack()) {
                navController.navigate(Screen.Start.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        })
    }
    */
}
