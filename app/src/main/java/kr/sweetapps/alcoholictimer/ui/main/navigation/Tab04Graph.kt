package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_04.HabitScreen

/**
 * Tab 04: 습관 관리 네비게이션 그래프
 * - More/Habit: 습관 설정 화면
 */
fun NavGraphBuilder.addTab04Graph(navController: NavHostController) {
    composable(Screen.More.route) {
        HabitScreen(
            onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) },
            onApplyAndGoHome = {
                // Navigate to Start (Tab1) and recreate Start so its LaunchedEffect runs
                try {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } catch (_: Throwable) {}
            }
        )
    }
}


