package kr.sweetapps.alcoholictimer.ui.main.navigation

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_02.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.AddRecordScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.DetailScreen

/**
 * Tab 02: 기록 관련 네비게이션 그래프
 * - Records List: 기록 목록 (중첩 그래프 내부)
 * - Detail: 기록 상세
 * - Result: 타이머 완료 결과
 * - AllRecords: 모든 기록 보기
 * - AllDiary: 모든 일기 보기
 * - DiaryWrite: 일기 작성
 * - DiaryDetail: 일기 상세
 * - AddRecord: 기록 추가
 */
fun NavGraphBuilder.addTab02Graph(
    navController: NavHostController,
    activity: Activity?,
    context: Context,
    recordsRefreshCounter: Int,
    onRefreshCounterIncrement: () -> Unit
) {
    // Tab 2: 중첩 그래프 (Nested Graph)
    navigation(startDestination = "records_list", route = Screen.Records.route) {

        // 1. 기록 목록 화면
        composable("records_list") {
            val tab02ViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel =
                (activity as? ViewModelStoreOwner)?.let { owner ->
                    viewModel(viewModelStoreOwner = owner)
                } ?: viewModel()

            val pendingRoute by tab02ViewModel.pendingDetailRoute.collectAsState()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            LaunchedEffect(pendingRoute, currentRoute) {
                pendingRoute?.let { route ->
                    if (currentRoute != route) {
                        android.util.Log.d("NavGraph", "[목록] pendingRoute 감지: $route (현재: $currentRoute) -> 자동 이동")
                        navController.navigate(route)
                    } else {
                        android.util.Log.d("NavGraph", "[목록] 이미 해당 route에 있음 -> navigate 건너뜀")
                    }
                }
            }

            kr.sweetapps.alcoholictimer.ui.tab_02.Tab02Screen(
                onNavigateToDetail = { record: SobrietyRecord ->
                    try { AnalyticsManager.logViewRecordDetail(record.id) } catch (_: Throwable) {}
                    val route = Screen.Detail.createRoute(
                        startTime = record.startTime,
                        endTime = record.endTime,
                        targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                        actualDays = kotlin.math.round(record.actualDays).toInt().coerceAtLeast(0),
                        isCompleted = record.isCompleted
                    )
                    navController.navigate(route)
                },
                onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) },
                onNavigateToAllDiaries = { navController.navigate(Screen.AllDiary.route) },
                onAddRecord = { navController.navigate(Screen.DiaryWrite.route) },
                onDiaryClick = { diary ->
                    val route = Screen.DiaryDetail.createRoute(diary.id.toString())
                    navController.navigate(route)
                }
            )
        }

        // 2. 기록 상세 화면
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("startTime") { type = NavType.LongType },
                navArgument("endTime") { type = NavType.LongType },
                navArgument("targetDays") { type = NavType.FloatType },
                navArgument("actualDays") { type = NavType.IntType },
                navArgument("isCompleted") { type = NavType.BoolType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { entry ->
            val args = entry.arguments
            DetailScreen(
                startTime = args?.getLong("startTime") ?: 0L,
                endTime = args?.getLong("endTime") ?: System.currentTimeMillis(),
                targetDays = args?.getFloat("targetDays") ?: 30f,
                actualDays = args?.getInt("actualDays") ?: 0,
                isCompleted = args?.getBoolean("isCompleted") ?: false,
                onBack = { navController.popBackStack() },
                onDeleted = {
                    onRefreshCounterIncrement()
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 3. 타이머 완료 결과 화면
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("startTime") { type = NavType.LongType },
                navArgument("endTime") { type = NavType.LongType },
                navArgument("targetDays") { type = NavType.FloatType },
                navArgument("actualDays") { type = NavType.IntType },
                navArgument("isCompleted") { type = NavType.BoolType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { entry ->
            val tab02ViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel =
                (activity as? ViewModelStoreOwner)?.let { owner ->
                    viewModel(viewModelStoreOwner = owner)
                } ?: viewModel()

            val args = entry.arguments
            DetailScreen(
                startTime = args?.getLong("startTime") ?: 0L,
                endTime = args?.getLong("endTime") ?: System.currentTimeMillis(),
                targetDays = args?.getFloat("targetDays") ?: 30f,
                actualDays = args?.getInt("actualDays") ?: 0,
                isCompleted = args?.getBoolean("isCompleted") ?: false,
                showTopBar = true,
                isResultMode = true,
                onBack = {
                    android.util.Log.d("NavGraph", "[Result] 뒤로 가기 -> Start 화면으로 이동")
                    tab02ViewModel.consumePendingDetailRoute()
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onDeleted = {
                    onRefreshCounterIncrement()
                    android.util.Log.d("NavGraph", "[Result] 삭제 완료 -> Start 화면으로 이동")
                    tab02ViewModel.consumePendingDetailRoute()
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToHome = {
                    android.util.Log.d("NavGraph", "[Result] 다시 시작하기 -> Start")
                    tab02ViewModel.consumePendingDetailRoute()
                    navController.navigate(Screen.Start.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }

    // 4. 기록 추가 화면
    composable(Screen.AddRecord.route) {
        AddRecordScreenComposable(
            onFinished = {
                onRefreshCounterIncrement()
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() }
        )
    }

    // 5. 모든 기록 보기 화면
    composable(
        route = Screen.AllRecords.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        AllRecordsScreen(
            onNavigateBack = {
                if (!navController.popBackStack()) navController.navigate(Screen.Records.route)
            },
            onNavigateToDetail = { record: SobrietyRecord ->
                try { AnalyticsManager.logViewRecordDetail(record.id) } catch (_: Throwable) {}
                val route = Screen.Detail.createRoute(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                    actualDays = kotlin.math.round(record.actualDays).toInt().coerceAtLeast(0),
                    isCompleted = record.isCompleted
                )
                navController.navigate(route)
            },
            onAddRecord = { navController.navigate(Screen.AddRecord.route) }
        )
    }

    // 6. 모든 일기 보기 화면
    composable(
        route = Screen.AllDiary.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        kr.sweetapps.alcoholictimer.ui.tab_02.screens.AllDiaryScreen(
            onNavigateBack = {
                if (!navController.popBackStack()) navController.navigate(Screen.Records.route)
            },
            onOpenDiaryDetail = { diaryId ->
                val route = Screen.DiaryDetail.createRoute(diaryId.toString())
                navController.navigate(route)
            },
            onAddDiary = { navController.navigate(Screen.DiaryWrite.route) }
        )
    }

    // 7. 일기 작성 화면
    composable(
        route = Screen.DiaryWrite.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryWriteScreen(
            onDismiss = {
                onRefreshCounterIncrement()
                navController.popBackStack()
            }
        )
    }

    // 8. 일기 상세보기/수정 화면
    composable(
        route = Screen.DiaryDetail.route,
        arguments = listOf(navArgument("diaryId") { type = NavType.StringType }),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) { backStackEntry ->
        val diaryId = backStackEntry.arguments?.getString("diaryId") ?: return@composable

        kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryWriteScreen(
            diaryId = diaryId.toLongOrNull(),
            onDismiss = {
                val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                val proceedToBack: () -> Unit = {
                    onRefreshCounterIncrement()
                    navController.popBackStack()
                }

                if (shouldShowAd && activity != null) {
                    android.util.Log.d("NavGraph", "[DiaryDetail] 광고 정책 통과 -> 전면 광고 노출")
                    if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                        kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { _ ->
                            proceedToBack()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "[DiaryDetail] 광고 로드 안됨 -> 즉시 뒤로 이동")
                        proceedToBack()
                    }
                } else {
                    android.util.Log.d("NavGraph", "[DiaryDetail] 광고 정책 불통과 -> 즉시 뒤로 이동")
                    proceedToBack()
                }
            }
        )
    }
}


