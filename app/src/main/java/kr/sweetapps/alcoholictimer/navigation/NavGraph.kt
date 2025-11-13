package kr.sweetapps.alcoholictimer.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kr.sweetapps.alcoholictimer.feature.about.AboutLicensesScreen
import kr.sweetapps.alcoholictimer.feature.about.AboutScreen
import kr.sweetapps.alcoholictimer.feature.detail.DetailScreen
import kr.sweetapps.alcoholictimer.feature.level.LevelScreen
import kr.sweetapps.alcoholictimer.feature.records.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.feature.records.components.RecordsScreen
import kr.sweetapps.alcoholictimer.feature.run.QuitScreenComposable
import kr.sweetapps.alcoholictimer.feature.run.RunScreenComposable
import kr.sweetapps.alcoholictimer.feature.settings.SettingsScreen
import kr.sweetapps.alcoholictimer.feature.start.StartScreen
import kr.sweetapps.alcoholictimer.feature.profile.NicknameEditScreen
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord

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
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
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
            RunScreenComposable(
                onRequestQuit = {
                    navController.navigate(Screen.Quit.route) { launchSingleTop = true }
                },
                onCompletedNavigateToDetail = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onRequireBackToStart = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Run.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 종료 화면 추가
        composable(Screen.Quit.route) {
            QuitScreenComposable(
                onQuitConfirmed = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        // 기록 목록 화면
        composable(Screen.Records.route) {
            RecordsScreen(
                externalRefreshTrigger = 0,
                onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) },
                onNavigateToDetail = { record: SobrietyRecord ->
                    val route = Screen.Detail.createRoute(
                        startTime = record.startTime,
                        endTime = record.endTime,
                        targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                        actualDays = record.actualDays.coerceAtLeast(0),
                        isCompleted = record.isCompleted
                    )
                    navController.navigate(route)
                }
            )
        }

        // 전체 기록 화면
        composable(Screen.AllRecords.route) {
            AllRecordsScreen(
                onNavigateToDetail = { record: SobrietyRecord ->
                    val route = Screen.Detail.createRoute(
                        startTime = record.startTime,
                        endTime = record.endTime,
                        targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                        actualDays = record.actualDays.coerceAtLeast(0),
                        isCompleted = record.isCompleted
                    )
                    navController.navigate(route)
                }
            )
        }

        // 레벨 화면
        composable(Screen.Level.route) { LevelScreen() }

        // 설정 화면
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // 정보 화면
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateLicenses = { navController.navigate(Screen.AboutLicenses.route) },
                showDebug = kr.sweetapps.alcoholictimer.BuildConfig.DEBUG,
                onNavigateDebug = {},
                onNavigateEditNickname = { navController.navigate(Screen.NicknameEdit.route) }
            )
        }

        // 라이센스 화면
        composable(Screen.AboutLicenses.route) { AboutLicensesScreen() }

        // 닉네임 편집 화면
        composable(Screen.NicknameEdit.route) {
            NicknameEditScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // 기록 상세 화면 (Compose)
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("startTime") { type = NavType.LongType },
                navArgument("endTime") { type = NavType.LongType },
                navArgument("targetDays") { type = NavType.FloatType },
                navArgument("actualDays") { type = NavType.IntType },
                navArgument("isCompleted") { type = NavType.BoolType }
            )
        ) { entry ->
            val args = entry.arguments
            val startTime = args?.getLong("startTime") ?: 0L
            val endTime = args?.getLong("endTime") ?: System.currentTimeMillis()
            val targetDays = args?.getFloat("targetDays") ?: 30f
            val actualDays = args?.getInt("actualDays") ?: 0
            val isCompleted = args?.getBoolean("isCompleted") ?: false

            DetailScreen(
                startTime = startTime,
                endTime = endTime,
                targetDays = targetDays,
                actualDays = actualDays,
                isCompleted = isCompleted,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// RunActivity 제거 완료: RunScreenComposable는 NavHost 전용 화면으로 사용됨
