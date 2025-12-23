package kr.sweetapps.alcoholictimer.ui.main.navigation

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.AllRecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.AddRecordScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.DetailScreen
import kotlin.math.round

/**
 * Tab 02 - 상세/전체 화면 (Root 레벨용)
 * - Detail: 기록 상세
 * - Result: 타이머 완료 결과
 * - AddRecord: 기록 추가
 * - AllRecords: 모든 기록 보기
 * - AllDiary: 모든 일기 보기
 * - DiaryWrite: 일기 작성
 * - DiaryDetail: 일기 상세
 */
fun NavGraphBuilder.addTab02DetailGraph(
    navController: NavHostController,
    activity: Activity?,
    context: Context,
    onRefreshCounterIncrement: () -> Unit
) {
    // 기록 상세 화면
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

    // 타이머 완료 결과 화면
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
                android.util.Log.d("NavGraph", "[Result] 뒤로가기 -> home")
                tab02ViewModel.consumePendingDetailRoute()
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true } // [FIX] home으로 이동하며 스택 초기화
                    launchSingleTop = true
                }
            },
            onDeleted = {
                onRefreshCounterIncrement()
                android.util.Log.d("NavGraph", "[Result] 삭제 완료 -> Start 화면으로 이동")
                tab02ViewModel.consumePendingDetailRoute()
                navController.navigate(Screen.Start.route) {
                    popUpTo(Screen.Start.route) { inclusive = true } // [FIX] Start 화면으로 정리
                    launchSingleTop = true
                }
            },
            onNavigateToHome = {
                android.util.Log.d("NavGraph", "[Result] 다시 시작하기 -> home")
                tab02ViewModel.consumePendingDetailRoute()
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true } // [FIX] home으로 이동하며 스택 초기화
                    launchSingleTop = true
                }
            }
        )
    }

    // 기록 추가 화면
    composable(Screen.AddRecord.route) {
        AddRecordScreenComposable(
            onFinished = {
                onRefreshCounterIncrement()
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() }
        )
    }

    // 모든 기록 보기 화면
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
                // [NEW] 전면광고 표시 후 뒤로가기
                val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                val proceedBack: () -> Unit = {
                    // [FIX] popBackStack 실패 시 Screen.Start로 이동 (탭2의 records_list로 돌아감)
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Start.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                if (shouldShowAd && activity != null) {
                    android.util.Log.d("NavGraph", "[AllRecords] 광고 정책 통과 -> 전면 광고 노출")
                    if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                        kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { _ ->
                            proceedBack()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "[AllRecords] 광고 로드 안됨 -> 즉시 뒤로 이동")
                        proceedBack()
                    }
                } else {
                    android.util.Log.d("NavGraph", "[AllRecords] 광고 정책 불통과 -> 즉시 뒤로 이동")
                    proceedBack()
                }
            },
            onNavigateToDetail = { record: SobrietyRecord ->
                try { AnalyticsManager.logViewRecordDetail(record.id) } catch (_: Throwable) {}
                val route = Screen.Detail.createRoute(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                    actualDays = round(record.actualDays).toInt().coerceAtLeast(0),
                    isCompleted = record.isCompleted
                )
                navController.navigate(route)
            },
            onAddRecord = { navController.navigate(Screen.AddRecord.route) }
        )
    }

    // 모든 일기 보기 화면
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
                // [NEW] 전면광고 표시 후 뒤로가기
                val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                val proceedBack: () -> Unit = {
                    // [FIX] popBackStack 실패 시 Screen.Start로 이동 (탭2의 records_list로 돌아감)
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Start.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                if (shouldShowAd && activity != null) {
                    android.util.Log.d("NavGraph", "[AllDiary] 광고 정책 통과 -> 전면 광고 노출")
                    if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                        kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { _ ->
                            proceedBack()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "[AllDiary] 광고 로드 안됨 -> 즉시 뒤로 이동")
                        proceedBack()
                    }
                } else {
                    android.util.Log.d("NavGraph", "[AllDiary] 광고 정책 불통과 -> 즉시 뒤로 이동")
                    proceedBack()
                }
            },
            onOpenDiaryDetail = { diaryId ->
                val route = Screen.DiaryDetail.createRoute(diaryId.toString())
                navController.navigate(route)
            },
            onAddDiary = { navController.navigate(Screen.DiaryWrite.route) }
        )
    }

    // 일기 작성 화면
    composable(
        route = Screen.DiaryWrite.route,
        arguments = listOf(
            navArgument("selectedDate") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        ),
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
        val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
        val selectedDate = selectedDateString?.toLongOrNull()

        kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryWriteScreen(
            selectedDate = selectedDate, // [FIX] 선택된 날짜 전달 (2025-12-22)
            onDismiss = {
                onRefreshCounterIncrement()
                navController.popBackStack()
            }
        )
    }

    // 일기 상세보기/수정 화면
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
        val diaryIdLong = diaryId.toLongOrNull()

        kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryWriteScreen(
            diaryId = diaryIdLong,
            onDismiss = {
                // [FIX] 수정 모드일 때는 상세 화면으로 복귀, 새 작성일 때는 탭2로 복귀 (2025-12-23)
                onRefreshCounterIncrement()

                if (diaryIdLong != null) {
                    // 수정 모드: 현재 화면을 pop하고 Tab02로 돌아감 (상세 화면이 다시 표시됨)
                    navController.popBackStack()
                } else {
                    // 새 작성 모드: 그냥 뒤로가기
                    navController.popBackStack()
                }
            }
        )
    }
}

