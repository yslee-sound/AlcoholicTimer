
package kr.sweetapps.alcoholictimer.ui.main

import android.os.Bundle
// ...existing imports...
import androidx.navigation.navArgument
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.DetailScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.LevelScreen // [FIX] Tab03의 ViewModel 사용 LevelScreen으로 변경
import kr.sweetapps.alcoholictimer.ui.tab_02.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.QuitScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_04.HabitScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.NicknameEditScreen
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.ad.HomeAdTrigger
import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.AddRecordScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_04.screens.CurrencyScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DebugScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.policy.DocumentScreen
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab01Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab02Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab03Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab04Graph
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab05Graph
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen
import kr.sweetapps.alcoholictimer.ui.main.navigation.addTab03Graph
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen

/**
 * Navigation Host (App-level Navigation Graph)
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    var recordsRefreshCounter by remember { mutableStateOf<Int>(0) }
    val activity = (LocalView.current.context as? Activity)
    val context = LocalContext.current
    val firebaseAnalytics = runCatching { Firebase.analytics }.getOrNull()

    // Home 화면 방문 감지 (광고 트리거)
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
        // [Refactored] Tab 01: 금주 타이머 관련 화면 (Start, Run, Quit)
        addTab01Graph(navController, activity, context, firebaseAnalytics)

        // [Refactored] Tab 02: 기록 관련 화면 (Records, Detail, Result, Diary 등)
        addTab02Graph(navController, activity, context, recordsRefreshCounter) {
            recordsRefreshCounter++
        }

        // 타이머 성공 화면 (목표 달성)
        composable(Screen.Success.route) {
            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedSuccessScreen(
                onBack = {
                    // 뒤로 가기: Start 화면으로 이동
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Success.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onResultCheck = {
                    // 타이머 완료 상태 초기화
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
                            val completedActualDays = sharedPref.getFloat("completed_actual_days", 0f).toInt()  // [REFACTOR] Float으로 읽기

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
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { success ->
                                android.util.Log.d("NavGraph", "광고 결과: success=$success")
                                proceedToDetail()
                            }
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

        // 타이머 중단 화면 (포기)
        composable(Screen.GiveUp.route) {
            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedGiveUpScreen(
                onBack = {
                    // 뒤로 가기: Start 화면으로 이동
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
                            android.util.Log.d("NavGraph", "[GiveUp] user_settings 파일 열기 완료")

                            // 포기 기록 데이터 읽기
                            val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                            val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                            val completedTargetDays = sharedPref.getFloat("completed_target_days", 21f)
                            val completedActualDays = sharedPref.getFloat("completed_actual_days", 0f).toInt()
                            val isGiveUp = sharedPref.getBoolean("completed_is_give_up", false)

                            android.util.Log.d("NavGraph", "[GiveUp] 데이터 읽기 완료:")
                            android.util.Log.d("NavGraph", "  - startTime: $completedStartTime")
                            android.util.Log.d("NavGraph", "  - endTime: $completedEndTime")
                            android.util.Log.d("NavGraph", "  - targetDays: $completedTargetDays")
                            android.util.Log.d("NavGraph", "  - actualDays: $completedActualDays")
                            android.util.Log.d("NavGraph", "  - isGiveUp: $isGiveUp")

                            // 데이터 유효성 검증
                            if (completedStartTime > 0 && completedEndTime > 0) {
                                android.util.Log.d("NavGraph", "[GiveUp] 데이터 유효 ✓ -> Result 화면으로 이동")

                                val resultRoute = Screen.Result.createRoute(
                                    startTime = completedStartTime,
                                    endTime = completedEndTime,
                                    targetDays = completedTargetDays,
                                    actualDays = completedActualDays,
                                    isCompleted = false // 포기이므로 미완료
                                )

                                android.util.Log.d("NavGraph", "[GiveUp STEP 4] Result 라우트 생성: $resultRoute")

                                navController.navigate(resultRoute) {
                                    popUpTo(Screen.GiveUp.route) { inclusive = true }
                                    launchSingleTop = true
                                }

                                android.util.Log.d("NavGraph", "[GiveUp STEP 5] Result 화면 이동 완료 ✓")

                                // 임시 데이터 정리
                                try {
                                    sharedPref.edit()
                                        .remove("completed_start_time")
                                        .remove("completed_end_time")
                                        .remove("completed_target_days")
                                        .remove("completed_actual_days")
                                        .remove("completed_is_give_up")
                                        .apply()
                                    android.util.Log.d("NavGraph", "[GiveUp] 임시 데이터 정리 완료")
                                } catch (e: Exception) {
                                    android.util.Log.e("NavGraph", "[GiveUp] 임시 데이터 정리 실패", e)
                                }
                            } else {
                                android.util.Log.e("NavGraph", "[GiveUp ERROR] 데이터 유효하지 않음 ✗")
                                android.util.Log.e("NavGraph", "  startTime=$completedStartTime, endTime=$completedEndTime")
                                android.util.Log.w("NavGraph", "[GiveUp] Records 화면으로 대체 이동")

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
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { success ->
                                android.util.Log.d("NavGraph", "[GiveUp] 광고 결과: success=$success")
                                proceedToDetail()
                            }
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
                    // 새 타이머 시작 로직
                    try {
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)

                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_TIMER_COMPLETED, false)
                            .remove(kr.sweetapps.alcoholictimer.util.constants.Constants.PREF_START_TIME)
                            .apply()
                    } catch (e: Exception) {}

                    navController.navigate(Screen.Start.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // [Refactored] Tab 03: 레벨 화면
        addTab03Graph(navController)

        // [Refactored] Tab 04: 습관 관리 화면
        addTab04Graph(navController)

        // [Refactored] Tab 05: 설정 & About 화면
        addTab05Graph(navController)
    }
}

private fun isHomeRoute(route: String?): Boolean {
    return route == Screen.Start.route ||
           route == Screen.Run.route ||
           route == Screen.Quit.route ||
           route == Screen.Records.route ||
           route == "records_list" // Tab 2 내부 경로
}
