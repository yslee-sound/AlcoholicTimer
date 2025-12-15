package kr.sweetapps.alcoholictimer.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.ui.ad.HomeAdTrigger
import kr.sweetapps.alcoholictimer.ui.common.BaseScaffold
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab01Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab02DetailGraph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab02ListGraph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab03Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab04Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab05Graph

/**
 * Navigation Host (Root-level Navigation Graph)
 * 수정사항: Success/GiveUp 화면에서 Start로 이동 시 popUpTo(0)을 사용하여 스택 전체 초기화 (좀비 Run 방지)
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    var recordsRefreshCounter by remember { mutableStateOf(0) }
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
        startDestination = Screen.Start.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Screen.Start.route) {
            val tabNavController = rememberNavController()

            BaseScaffold(navController = tabNavController) {
                NavHost(
                    navController = tabNavController,
                    startDestination = startDestination,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    addTab01Graph(tabNavController, activity, context, firebaseAnalytics)

                    addTab02ListGraph(
                        navController = tabNavController,
                        activity = activity,
                        onNavigateToDetail = { route -> navController.navigate(route) },
                        onNavigateToDiaryWrite = { navController.navigate(Screen.DiaryWrite.route) },
                        onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) },
                        onNavigateToAllDiaries = { navController.navigate(Screen.AllDiary.route) },
                        onNavigateToDiaryDetail = { route -> navController.navigate(route) },
                        refreshSignal = recordsRefreshCounter
                    )

                    addTab03Graph(tabNavController)
                    addTab04Graph(tabNavController)
                    addTab05Graph(tabNavController)
                }
            }
        }

        addTab02DetailGraph(navController, activity, context) {
            recordsRefreshCounter++
        }

        // [FIXED] Success Screen (성공 화면)
        composable(Screen.Success.route) {
            // [NEW] 홈으로 이동하는 공통 함수 (스택 완전 초기화)
            val navigateToHome = {
                navController.navigate(Screen.Start.route) {
                    // 0번(그래프 시작점)까지 다 지워서 Run 화면 제거
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            // [NEW] 시스템 뒤로가기 버튼 눌렀을 때도 홈으로 이동
            BackHandler(onBack = { navigateToHome() })

            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedSuccessScreen(
                onBack = { navigateToHome() }, // UI 뒤로가기 버튼
                onResultCheck = {
                    android.util.Log.d("NavGraph", "[Success] 결과 확인 클릭")

                    // 타이머 상태 초기화 로직
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_TIMER_COMPLETED, false)
                            .remove(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_START_TIME)
                            .apply()
                    } catch (t: Throwable) {
                        android.util.Log.e("NavGraph", "타이머 상태 초기화 실패", t)
                    }

                    val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                    val proceedToDetail: () -> Unit = {
                        try {
                            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                            val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                            val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                            val completedTargetDays = sharedPref.getFloat("completed_target_days", 21f)
                            val completedActualDays = sharedPref.getFloat("completed_actual_days", 0f).toInt()

                            if (completedStartTime > 0 && completedEndTime > 0) {
                                val resultRoute = Screen.Result.createRoute(
                                    startTime = completedStartTime,
                                    endTime = completedEndTime,
                                    targetDays = completedTargetDays,
                                    actualDays = completedActualDays,
                                    isCompleted = true
                                )
                                navController.navigate(resultRoute) {
                                    // 결과 화면으로 갈 때도 스택 초기화 (뒤로가기 시 Start로 가도록)
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(Screen.Records.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (t: Throwable) {
                            navController.navigate(Screen.Records.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    if (shouldShowAd && activity != null) {
                        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { proceedToDetail() }
                        } else {
                            proceedToDetail()
                        }
                    } else {
                        proceedToDetail()
                    }
                },
                onNewTimerStart = {
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_TIMER_COMPLETED, false)
                            .remove(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_START_TIME)
                            .apply()
                    } catch (_: Exception) {}

                    navController.navigate(Screen.Start.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // [FIXED] GiveUp Screen (포기 화면)
        composable(Screen.GiveUp.route) {
            // [NEW] 홈으로 이동하는 공통 함수
            val navigateToHome = {
                navController.navigate(Screen.Start.route) {
                    // 0번(그래프 시작점)까지 다 지워서 Run 화면 제거
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            // [NEW] 시스템 뒤로가기 버튼 처리
            BackHandler(onBack = { navigateToHome() })

            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedGiveUpScreen(
                onBack = { navigateToHome() },
                onResultCheck = {
                    val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                    val proceedToDetail: () -> Unit = {
                        try {
                            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                            val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                            val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                            val completedTargetDays = sharedPref.getFloat("completed_target_days", 21f)
                            val completedActualDays = sharedPref.getFloat("completed_actual_days", 0f).toInt()

                            if (completedStartTime > 0 && completedEndTime > 0) {
                                val resultRoute = Screen.Result.createRoute(
                                    startTime = completedStartTime,
                                    endTime = completedEndTime,
                                    targetDays = completedTargetDays,
                                    actualDays = completedActualDays,
                                    isCompleted = false
                                )

                                navController.navigate(resultRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }

                                try {
                                    sharedPref.edit()
                                        .remove("completed_start_time")
                                        .remove("completed_end_time")
                                        .remove("completed_target_days")
                                        .remove("completed_actual_days")
                                        .remove("completed_is_give_up")
                                        .apply()
                                } catch (_: Exception) {}

                            } else {
                                navController.navigate(Screen.Records.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (t: Throwable) {
                            navController.navigate(Screen.Records.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    if (shouldShowAd && activity != null) {
                        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { proceedToDetail() }
                        } else {
                            proceedToDetail()
                        }
                    } else {
                        proceedToDetail()
                    }
                },
                onNewTimerStart = {
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_TIMER_COMPLETED, false)
                            .remove(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_START_TIME)
                            .apply()
                    } catch (_: Exception) {}

                    navController.navigate(Screen.Start.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private fun isHomeRoute(route: String?): Boolean {
    return route == Screen.Start.route ||
            route == Screen.Run.route ||
            route == Screen.Quit.route ||
            route == Screen.Records.route ||
            route == "records_list"
}