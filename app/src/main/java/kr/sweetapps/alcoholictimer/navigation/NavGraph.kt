@file:Suppress("UNUSED_IMPORT", "UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
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
import kr.sweetapps.alcoholictimer.ui.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.feature.run.QuitScreenComposable
import kr.sweetapps.alcoholictimer.feature.run.RunScreenComposable
import kr.sweetapps.alcoholictimer.feature.settings.SettingsScreen
import kr.sweetapps.alcoholictimer.feature.start.StartScreen
import kr.sweetapps.alcoholictimer.feature.profile.NicknameEditScreen
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ads.HomeAdTrigger
import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import kr.sweetapps.alcoholictimer.feature.addrecord.AddRecordScreenComposable

/**
 * Navigation Graph
 */
@Composable
fun AlcoholicTimerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    // Records immediate refresh counter. Increase when AddRecord finishes.
    var recordsRefreshCounter by remember { mutableStateOf<Int>(0) }
    val activity = (LocalView.current.context as? Activity)
    // 홈 그룹 진입 이벤트 기반 카운트: 비홈→홈으로 전환될 때만 1회 증가
    // 최초 앱 진입의 첫 이벤트(대개 홈)는 카운트에서 제외한다.
    LaunchedEffect(Unit) {
        var wasHome = false
        var firstEmissionSkipped = false
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            val isHome = isHomeRoute(route)

            // 앱 시작 직후 첫 이벤트 스킵 (초기 홈 진입을 카운트하지 않음)
            if (!firstEmissionSkipped) {
                firstEmissionSkipped = true
                wasHome = isHome
                return@collect
            }

            if (activity != null && isHome && !wasHome) {
                HomeAdTrigger.registerHomeVisit(activity, source = route ?: "home")
            }
            wasHome = isHome
        }
    }

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
                gateNavigation = false,
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

        // 종료 화면 추가 (홈 그룹: Quit)
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
                externalRefreshTrigger = recordsRefreshCounter,
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
                },
                onAddRecord = { navController.navigate(Screen.AddRecord.route) }
            )
        }

        // 기록 추가 화면 (Compose 하위 페이지)
        composable(Screen.AddRecord.route) {
            AddRecordScreenComposable(
                onFinished = {
                    // signal RecordsScreen to refresh
                    recordsRefreshCounter = recordsRefreshCounter + 1
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        // 전체 기록 화면
        composable(Screen.AllRecords.route) {
            AllRecordsScreen(
                onNavigateBack = { if (!navController.popBackStack()) navController.navigate(Screen.Records.route) },
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
        composable(Screen.Settings.route) { SettingsScreen() }

        // 정보 화면
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateLicenses = { navController.navigate(Screen.AboutLicenses.route) },
                onNavigateEditNickname = { navController.navigate(Screen.NicknameEdit.route) },
                onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) },
                showBack = false
            )
        }

        // 라이센스 화면
        composable(Screen.AboutLicenses.route) { AboutLicensesScreen(onBack = { navController.popBackStack() }) }

        // 닉네임 편집 화면
        composable(Screen.NicknameEdit.route) {
            NicknameEditScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // 통화 설정 화면
        composable(Screen.CurrencySettings.route) {
            kr.sweetapps.alcoholictimer.feature.about.CurrencySettingsScreen(onBack = { navController.popBackStack() })
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

private fun isHomeRoute(route: String?): Boolean {
    return route == Screen.Start.route || route == Screen.Run.route || route == Screen.Quit.route
}
