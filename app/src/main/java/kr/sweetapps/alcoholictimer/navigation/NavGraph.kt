package kr.sweetapps.alcoholictimer.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.feature.start.StartScreen
import kr.sweetapps.alcoholictimer.feature.run.RunScreenComposable
import kr.sweetapps.alcoholictimer.feature.records.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.feature.records.components.RecordsScreen
import kr.sweetapps.alcoholictimer.feature.settings.SettingsScreen

/**
 * Navigation Graph
 *
 * TODO: 각 화면을 점진적으로 Composable로 전환
 */
@Composable
fun AlcoholicTimerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 금주 시작 화면 ✅ 구현 완료
        composable(Screen.Start.route) {
            StartScreen(
                gateNavigation = false, // Navigation 사용 허용
                onStart = {
                    navController.navigate(Screen.Run.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 금주 진행 화면 ✅ 구현 완료
        composable(Screen.Run.route) {
            RunScreenComposable()
        }

        // 기록 목록 화면
        composable(Screen.Records.route) {
            RecordsScreen(
                externalRefreshTrigger = 0,
                onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) },
                onNavigateToDetail = { /* TODO detail route */ }
            )
        }

        // 전체 기록 화면
        composable(Screen.AllRecords.route) {
            AllRecordsScreen(onNavigateToDetail = { /* TODO detail */ })
        }

        // 레벨 화면
        composable(Screen.Level.route) {
            Text("Level (TODO)")
        }

        // 설정 화면
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // 정보 화면
        composable(Screen.About.route) {
            Text("About (TODO)")
        }

        // 닉네임 편집 화면
        composable(Screen.NicknameEdit.route) {
            Text("Nickname Edit (TODO)")
        }
    }
}
