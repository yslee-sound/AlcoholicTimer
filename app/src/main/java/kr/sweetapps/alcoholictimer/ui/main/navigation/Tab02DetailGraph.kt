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
                val route = Screen.Detail.createRoute(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                    actualDays = round(record.actualDays).toInt().coerceAtLeast(0),
                    isCompleted = record.isCompleted
                )
                navController.navigate(route)
            }
            // [REMOVED] onAddRecord 콜백 제거 - Add Record 기능 삭제 (2025-12-25)
        )
    }

    // 모든 일기 보기 화면 (피드 스타일로 교체 - 2025-12-27)
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
        // [CHANGED] 피드 스타일 화면으로 통합 (2025-12-27)
        val diaryViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel =
            (activity as? androidx.lifecycle.ViewModelStoreOwner)?.let { owner ->
                androidx.lifecycle.viewmodel.compose.viewModel(viewModelStoreOwner = owner)
            } ?: androidx.lifecycle.viewmodel.compose.viewModel()

        kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryDetailFeedScreen(
            targetDiaryId = -1L, // 최신글(최상단)부터 표시
            onBack = {
                // [NEW] 전면광고 표시 후 뒤로가기 (기존 로직 유지)
                val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                val proceedBack: () -> Unit = {
                    // popBackStack 실패 시 Screen.Start로 이동
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Start.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                if (shouldShowAd && activity != null) {
                    android.util.Log.d("NavGraph", "[AllDiary/Feed] 광고 정책 통과 -> 전면 광고 노출")
                    if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                        kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { _ ->
                            proceedBack()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "[AllDiary/Feed] 광고 로드 안됨 -> 즉시 뒤로 이동")
                        proceedBack()
                    }
                } else {
                    android.util.Log.d("NavGraph", "[AllDiary/Feed] 광고 정책 불통과 -> 즉시 뒤로 이동")
                    proceedBack()
                }
            },
            onEditClick = { diaryId ->
                // 일기 수정 화면으로 이동
                val route = Screen.DiaryDetail.createRoute(diaryId.toString())
                navController.navigate(route)
            },
            onDeleteClick = { diaryId ->
                // 일기 삭제 및 토스트 표시
                diaryViewModel.deleteDiary(diaryId)
                android.widget.Toast.makeText(
                    context,
                    "일기가 삭제되었습니다",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            diaryViewModel = diaryViewModel
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
                // [CRITICAL] 신규 작성 후 피드 화면(AllDiary)으로 이동 (2025-12-27)
                onRefreshCounterIncrement()
                navController.popBackStack() // 작성 화면 닫기

                // [NEW] 피드 목록 화면으로 강제 이동하여 저장된 일기를 최신순으로 확인
                navController.navigate(Screen.AllDiary.route) {
                    launchSingleTop = true
                }
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
                // [CRITICAL] 수정 후에도 피드 화면(AllDiary)으로 이동 (2025-12-27)
                onRefreshCounterIncrement()
                navController.popBackStack() // 수정 화면 닫기

                // [NEW] 피드 목록 화면으로 강제 이동하여 수정된 일기를 최신순으로 확인
                navController.navigate(Screen.AllDiary.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}

