package kr.sweetapps.alcoholictimer.ui.main

import android.app.Activity
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

            LaunchedEffect(recordsRefreshCounter) {
                if (recordsRefreshCounter > 0) {
                    tabNavController.popBackStack(Screen.Records.route, inclusive = false)
                }
            }

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

        composable(Screen.Success.route) {
            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedSuccessScreen(
                onBack = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Success.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onResultCheck = {
                    android.util.Log.d("NavGraph", "[Success] 결과 확인 클릭 -> 타이머 완료 상태 초기화")
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_TIMER_COMPLETED, false)
                            .remove(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_START_TIME)
                            .apply()
                        android.util.Log.d("NavGraph", "타이머 상태 초기화 완료 -> 광고 정책 체크")
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
                                android.util.Log.d("NavGraph", "[Success] -> Result 이동: $resultRoute")
                                navController.navigate(resultRoute) {
                                    popUpTo(Screen.Success.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                android.util.Log.w("NavGraph", "완료 기록 없음 -> Records 화면으로 이동")
                                navController.navigate(Screen.Records.route) {
                                    popUpTo(Screen.Success.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (t: Throwable) {
                            android.util.Log.e("NavGraph", "결과 확인 실패", t)
                            navController.navigate(Screen.Records.route) {
                                popUpTo(Screen.Success.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    if (shouldShowAd && activity != null) {
                        android.util.Log.d("NavGraph", "광고 정책 통과 -> 전면 광고 노출")
                        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { proceedToDetail() }
                        } else {
                            android.util.Log.d("NavGraph", "광고 로드 안됨 -> 즉시 Detail로 이동")
                            proceedToDetail()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "광고 쿨타임 중 -> 즉시 Detail로 이동")
                        proceedToDetail()
                    }
                },
                onNewTimerStart = {
                    android.util.Log.d("NavGraph", "[Success] 새 타이머 시작 -> Start 화면으로 이동")
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_TIMER_COMPLETED, false)
                            .remove(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_START_TIME)
                            .apply()
                    } catch (e: Exception) {
                        android.util.Log.e("NavGraph", "타이머 리셋 중 오류 발생", e)
                    }

                    navController.navigate(Screen.Start.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.GiveUp.route) {
            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedGiveUpScreen(
                onBack = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.GiveUp.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onResultCheck = {
                    android.util.Log.d("NavGraph", "[GiveUp] 결과 확인 버튼 클릭 - 데이터 읽기 시작")

                    val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                    val proceedToDetail: () -> Unit = {
                        try {
                            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                            val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                            val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                            val completedTargetDays = sharedPref.getFloat("completed_target_days", 21f)
                            val completedActualDays = sharedPref.getFloat("completed_actual_days", 0f).toInt()
                            val isGiveUp = sharedPref.getBoolean("completed_is_give_up", false)

                            if (completedStartTime > 0 && completedEndTime > 0) {
                                val resultRoute = Screen.Result.createRoute(
                                    startTime = completedStartTime,
                                    endTime = completedEndTime,
                                    targetDays = completedTargetDays,
                                    actualDays = completedActualDays,
                                    isCompleted = false
                                )

                                navController.navigate(resultRoute) {
                                    popUpTo(Screen.GiveUp.route) { inclusive = true }
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
                                } catch (e: Exception) {
                                    android.util.Log.e("NavGraph", "[GiveUp] 임시 데이터 정리 실패", e)
                                }
                            } else {
                                android.util.Log.e("NavGraph", "[GiveUp ERROR] 데이터 유효하지 않음 ✗")
                                navController.navigate(Screen.Records.route) {
                                    popUpTo(Screen.GiveUp.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } catch (t: Throwable) {
                            android.util.Log.e("NavGraph", "[GiveUp CRITICAL ERROR] 예외 발생", t)
                            navController.navigate(Screen.Records.route) {
                                popUpTo(Screen.GiveUp.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    if (shouldShowAd && activity != null) {
                        android.util.Log.d("NavGraph", "[GiveUp] 광고 정책 통과 -> 전면 광고 노출")
                        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { proceedToDetail() }
                        } else {
                            android.util.Log.d("NavGraph", "[GiveUp] 광고 로드 안됨 -> 즉시 Detail로 이동")
                            proceedToDetail()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "[GiveUp] 광고 쿨타임 중 -> 즉시 Detail로 이동")
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
