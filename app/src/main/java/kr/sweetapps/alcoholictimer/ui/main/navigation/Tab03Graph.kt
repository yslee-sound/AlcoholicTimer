package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_03.LevelScreen

/**
 * Tab 03: 레벨 시스템 네비게이션 그래프
 * - Level: 레벨 확인 화면
 */
fun NavGraphBuilder.addTab03Graph(navController: NavHostController) {
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
}


