@file:Suppress("UNUSED_IMPORT", "UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.navigation

import android.os.Bundle
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen
import kr.sweetapps.alcoholictimer.ui.screens.DetailScreen
import kr.sweetapps.alcoholictimer.feature.level.LevelScreen
import kr.sweetapps.alcoholictimer.feature.records.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.ui.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.screens.QuitScreenComposable
import kr.sweetapps.alcoholictimer.feature.run.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_04.SettingsScreen
import kr.sweetapps.alcoholictimer.ui.screens.StartScreen
import kr.sweetapps.alcoholictimer.feature.profile.NicknameEditScreen
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ads.HomeAdTrigger
import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import kr.sweetapps.alcoholictimer.feature.addrecord.AddRecordScreenComposable
import kr.sweetapps.alcoholictimer.feature.debug.DebugAdsScreen
import kr.sweetapps.alcoholictimer.ui.tab_04.screens.CurrencySettingsScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DebugScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.policy.DocumentScreen

/**
 * Navigation Graph
 */
@Composable
fun AlcoholicTimerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    var recordsRefreshCounter by remember { mutableStateOf<Int>(0) }
    val activity = (LocalView.current.context as? Activity)
    val context = LocalContext.current
    val firebaseAnalytics = runCatching { Firebase.analytics }.getOrNull()

    LaunchedEffect(Unit) {
        var wasHome = false
        var firstEmissionSkipped = false
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            val isHome = isHomeRoute(route)

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
        composable(Screen.Start.route) {
            StartScreen(
                gateNavigation = false,
                onStart = {
                    targetDays ->
                    val bundle = Bundle()
                    bundle.putInt("target_days", targetDays)
                    firebaseAnalytics?.logEvent("start_timer", bundle)
                    navController.navigate(Screen.Run.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

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

        composable(Screen.AddRecord.route) {
            AddRecordScreenComposable(
                onFinished = {
                    recordsRefreshCounter = recordsRefreshCounter + 1
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

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

        composable(Screen.Level.route) {
            LevelScreen(onNavigateBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) })
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateLicenses = { navController.navigate(Screen.AboutLicenses.route) },
                onNavigatePrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateEditNickname = { navController.navigate(Screen.NicknameEdit.route) },
                onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) },
                onNavigateDebug = { navController.navigate(Screen.Debug.route) },
                showBack = false
            )
        }

        composable(Screen.AboutLicenses.route) {
            DocumentScreen(
                resName = "open_source_license",
                onBack = { navController.popBackStack() },
                titleResId = R.string.document_title_open_source
            )
        }

        composable(Screen.Privacy.route) {
            DocumentScreen(
                resName = "privacy_policy_bilingual",
                onBack = { navController.popBackStack() },
                titleResId = R.string.document_title_privacy
            )
        }

        composable(Screen.NicknameEdit.route) {
            NicknameEditScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.CurrencySettings.route) {
            CurrencySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.DebugAds.route) {
            DebugAdsScreen()
        }

        composable(Screen.Debug.route) {
            DebugScreen(onBack = { navController.popBackStack() })
        }

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
                onBack = { navController.popBackStack() },
                onDeleted = { recordsRefreshCounter = recordsRefreshCounter + 1 }
            )
        }
    }
}

private fun isHomeRoute(route: String?): Boolean {
    return route == Screen.Start.route || route == Screen.Run.route || route == Screen.Quit.route
}
