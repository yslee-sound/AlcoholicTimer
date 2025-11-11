package kr.sweetapps.alcoholictimer.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.feature.start.StartScreen
import kr.sweetapps.alcoholictimer.feature.run.RunScreen

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
                gateNavigation = true, // Navigation 사용 중
                onNavigateToRun = {
                    navController.navigate(Screen.Run.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                }
            )
        }

        // 금주 진행 화면 ✅ 구현 완료
        composable(Screen.Run.route) {
            RunScreen(
                onNavigateToStart = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Run.route) { inclusive = true }
                    }
                }
            )
        }

        // 금주 진행 화면
        composable(Screen.Run.route) {
            // TODO: RunScreen Composable 구현
            Text("Run Screen - 구현 예정")
        }

        // 기록 목록 화면
        composable(Screen.Records.route) {
            // TODO: RecordsScreen Composable 구현
            Text("Records Screen - 구현 예정")
        }

        // 전체 기록 화면
        composable(Screen.AllRecords.route) {
            // TODO: AllRecordsScreen Composable 구현
            Text("All Records Screen - 구현 예정")
        }

        // 레벨 화면
        composable(Screen.Level.route) {
            // TODO: LevelScreen Composable 구현
            Text("Level Screen - 구현 예정")
        }

        // 설정 화면
        composable(Screen.Settings.route) {
            // TODO: SettingsScreen Composable 구현
            Text("Settings Screen - 구현 예정")
        }

        // 정보 화면
        composable(Screen.About.route) {
            // TODO: AboutScreen Composable 구현
            Text("About Screen - 구현 예정")
        }

        // 닉네임 편집 화면
        composable(Screen.NicknameEdit.route) {
            // TODO: NicknameEditScreen Composable 구현
            Text("Nickname Edit Screen - 구현 예정")
        }
    }
}

