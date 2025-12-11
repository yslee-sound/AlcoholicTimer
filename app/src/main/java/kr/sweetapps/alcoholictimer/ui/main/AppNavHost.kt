@file:Suppress("UNUSED_IMPORT", "UNUSED_VARIABLE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package kr.sweetapps.alcoholictimer.ui.main

import android.os.Bundle
// ...existing imports...
import androidx.navigation.navArgument
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.DetailScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.LevelScreen // [FIX] Tab03의 ViewModel 사용 LevelScreen으로 변경
import kr.sweetapps.alcoholictimer.ui.tab_02.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.QuitScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_04.SettingsScreen
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen
import java.util.Map.entry

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

    // [REMOVED] 타이머 완료 네비게이션 리스너를 MainActivity로 이동하여 전역 처리
    // 이제 사용자가 어느 화면(Tab 1, 2, 3)에 있든 타이머 완료 시 DetailScreen으로 이동

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
                onCompletedNavigateToDetail = { /* [REMOVED] 중복 네비게이션 방지 - MainActivity에서 전역 처리 */ },
                onRequireBackToStart = {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Run.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // [NEW] 타이머 완료 화면
        composable(Screen.Finished.route) {
            // [REMOVED] ViewModel 제거 - pending route 로직 불필요

            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedScreen(
                onResultCheck = {
                    // [FIX] Reset timer completion state when user checks result
                    // This prevents FinishedScreen from showing again when returning to Tab 1
                    android.util.Log.d("NavGraph", "결과 확인 클릭 -> 타이머 완료 상태 초기화")

                    try {
                        // Reset timer completion flag
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)

                        // Also clear SharedPreferences
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
                        // [FIX] 직통 연결: 복잡한 중간 과정 없이 즉시 결과 화면으로 이동
                        try {
                            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                            val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                            val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                            val completedTargetDays = sharedPref.getFloat("completed_target_days", 21f)
                            val completedActualDays = sharedPref.getInt("completed_actual_days", 0)

                            if (completedStartTime > 0 && completedEndTime > 0) {
                                val resultRoute = Screen.Result.createRoute(
                                    startTime = completedStartTime,
                                    endTime = completedEndTime,
                                    targetDays = completedTargetDays,
                                    actualDays = completedActualDays,
                                    isCompleted = true
                                )

                                android.util.Log.d("NavGraph", "[직통] Finished -> Result 즉시 이동: $resultRoute")

                                // [FIX] 중간 단계(Tab 2) 없이 즉시 결과 화면으로 이동
                                navController.navigate(resultRoute) {
                                    // 완료 화면(Finished)은 스택에서 제거하여 뒤로 가기 시 다시 안 나오게 함
                                    popUpTo(Screen.Finished.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                // 기록 정보가 없으면 Records 화면으로 폴백
                                android.util.Log.w("NavGraph", "완료 기록 없음 -> Records 화면으로 이동")
                                navController.navigate(Screen.Records.route) {
                                    popUpTo(Screen.Finished.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                                recordsRefreshCounter++
                            }
                        } catch (t: Throwable) {
                            android.util.Log.e("NavGraph", "결과 확인 실패", t)
                            navController.navigate(Screen.Records.route) {
                                popUpTo(Screen.Finished.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    if (shouldShowAd && activity != null) {
                        android.util.Log.d("NavGraph", "광고 정책 통과 -> 전면 광고 노출")
                        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { success ->
                                android.util.Log.d("NavGraph", "광고 결과: success=$success -> Detail 화면으로 이동")
                                proceedToDetail()
                            }
                        } else {
                            android.util.Log.d("NavGraph", "광고 로드 안됨 -> 즉시 Detail 화면으로 이동")
                            proceedToDetail()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "광고 쿨타임 중 or activity null -> 즉시 Detail 화면으로 이동")
                        proceedToDetail()
                    }
                },
                onNewTimerStart = {
                    // [중요] 새 타이머 시작 버튼 - 만료 상태 해제 (유일한 해제 경로)
                    android.util.Log.d("NavGraph", "새 타이머 시작 -> 만료 상태 해제 및 Start 화면으로 이동")

                    // 만료 상태 해제 (이 버튼이 유일한 해제 경로)
                    kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(false)
                    kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(false)

                    android.util.Log.d("NavGraph", "만료 상태 해제 완료: isFinished=false")

                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Finished.route) { inclusive = true }
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
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }


        // [FIX] Tab 2를 위한 중첩 그래프 (Nested Graph) 생성
        // route: 탭의 경로 (Screen.Records.route)
        // startDestination: 이 탭의 첫 화면 (내부 경로 "records_list")
        // [목록, Detail, Result] 3개가 모두 하나의 그룹으로 묶여 탭 전환 시 상태 유지됨
        navigation(startDestination = "records_list", route = Screen.Records.route) {

            // 1. 기록 목록 화면 (내부 경로 "records_list" 사용)
            composable("records_list") {
                // [FIX] Activity Scope ViewModel을 사용하여 pending route 감지
                val tab02ViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel = if (activity != null) {
                    androidx.lifecycle.viewmodel.compose.viewModel(viewModelStoreOwner = activity as androidx.lifecycle.ViewModelStoreOwner)
                } else {
                    androidx.lifecycle.viewmodel.compose.viewModel()
                }
                val pendingRoute by tab02ViewModel.pendingDetailRoute.collectAsState()

                // [FIX] 현재 route 가져오기 (무한 루프 방지용)
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                // [핵심] pendingRoute가 있고, 현재 route와 다를 때만 navigate
                LaunchedEffect(pendingRoute, currentRoute) {
                    pendingRoute?.let { route ->
                        if (currentRoute != route) {
                            android.util.Log.d("NavGraph", "[목록] pendingRoute 감지: $route (현재: $currentRoute) -> 자동 이동")
                            navController.navigate(route)
                            // [중요] 소비하지 않음! 탭 복귀 시에도 유지되도록
                        } else {
                            android.util.Log.d("NavGraph", "[목록] 이미 해당 route에 있음 -> navigate 건너뜀")
                        }
                    }
                }

                // [UPDATED] Tab02Screen을 통해 Room DB 데이터 연결
                kr.sweetapps.alcoholictimer.ui.tab_02.Tab02Screen(
                    onNavigateToDetail = { record: SobrietyRecord ->
                        // Analytics: 기록 상세 보기 이벤트 전송
                        try { AnalyticsManager.logViewRecordDetail(record.id) } catch (_: Throwable) {}
                        val route = Screen.Detail.createRoute(
                            startTime = record.startTime,
                            endTime = record.endTime,
                            targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                            actualDays = record.actualDays.coerceAtLeast(0),
                            isCompleted = record.isCompleted
                        )
                        navController.navigate(route)
                    },
                    onNavigateToAllRecords = { navController.navigate(Screen.AllRecords.route) },
                    onNavigateToAllDiaries = { navController.navigate(Screen.AllDiary.route) },
                    onAddRecord = { navController.navigate(Screen.DiaryWrite.route) },
                    onDiaryClick = { diary -> // DiaryEntity 사용
                        val route = Screen.DiaryDetail.createRoute(diary.id.toString())
                        navController.navigate(route)
                    }
                )
            }

            // 2. 기록 상세 화면 (Tab 2의 일부 - 과거 기록 열람용)
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
                    onBack = {
                        // [FIX] 같은 그래프 내에서의 이동이므로 기본 backStack 처리
                        navController.popBackStack()
                    },
                    onDeleted = {
                        recordsRefreshCounter = recordsRefreshCounter + 1
                        // 삭제 후 목록으로 복귀
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

            // 3. [NEW] 타이머 완료 결과 화면 (전체 화면 모드)
            // 하단 네비게이션바 가려짐, 오른쪽에서 왼쪽으로 슬라이드 인 애니메이션
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
                    // [NEW] 오른쪽에서 왼쪽으로 슬라이드 인
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 300,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                },
                exitTransition = {
                    // [NEW] 왼쪽으로 슬라이드 아웃 (뒤로 가기 시)
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 300,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                }
            ) { entry ->
                // [FIX] ViewModel 가져오기 (pending route clear용)
                val tab02ViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel = if (activity != null) {
                    androidx.lifecycle.viewmodel.compose.viewModel(viewModelStoreOwner = activity as androidx.lifecycle.ViewModelStoreOwner)
                } else {
                    androidx.lifecycle.viewmodel.compose.viewModel()
                }

                val args = entry.arguments
                val startTime = args?.getLong("startTime") ?: 0L
                val endTime = args?.getLong("endTime") ?: System.currentTimeMillis()
                val targetDays = args?.getFloat("targetDays") ?: 30f
                val actualDays = args?.getInt("actualDays") ?: 0
                val isCompleted = args?.getBoolean("isCompleted") ?: false

                // [REMOVED] 뒤로 가기 차단 제거 - 한 번에 닫히도록 수정

                // [NEW] 전체 화면 (Dialog 오버레이 제거)
                DetailScreen(
                    startTime = startTime,
                    endTime = endTime,
                    targetDays = targetDays,
                    actualDays = actualDays,
                    isCompleted = isCompleted,
                    showTopBar = true,        // 타이틀바 표시 (결과 모드)
                    isResultMode = true,      // 결과 모드 활성화
                    onBack = {
                        // [FIX] 뒤로 가기 -> 먼저 화면 이동 후 상태 정리
                        android.util.Log.d("NavGraph", "[Result] 뒤로 가기 -> popBackStack 먼저 실행")
                        navController.popBackStack()
                        tab02ViewModel.consumePendingDetailRoute()
                    },
                    onDeleted = {
                        recordsRefreshCounter = recordsRefreshCounter + 1
                        // 삭제 후에도 먼저 popBackStack 실행 후 pendingRoute clear
                        android.util.Log.d("NavGraph", "[Result] 삭제 완료 -> popBackStack 후 pendingRoute clear")
                        navController.popBackStack()
                        tab02ViewModel.consumePendingDetailRoute()
                    },
                    onNavigateToHome = {
                        // [NEW] 다시 시작하기 버튼 -> Tab 1 (Start) 홈으로
                        android.util.Log.d("NavGraph", "[Result] 다시 시작하기 -> Tab 1 (Start)")
                        tab02ViewModel.consumePendingDetailRoute()
                        navController.navigate(Screen.Start.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
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
                    // Analytics: 기록 상세 보기 이벤트 전송 (AllRecords 출처)
                    try { AnalyticsManager.logViewRecordDetail(record.id) } catch (_: Throwable) {}
                    val route = Screen.Detail.createRoute(
                        startTime = record.startTime,
                        endTime = record.endTime,
                        targetDays = record.targetDays.toFloat().coerceAtLeast(1f),
                        actualDays = record.actualDays.coerceAtLeast(0),
                        isCompleted = record.isCompleted
                    )
                    navController.navigate(route)
                },
                onAddRecord = { navController.navigate(Screen.AddRecord.route) } // [NEW] 기록 추가 기능 연결
            )
        }

        // [NEW] 모든 일기 보기 화면 (AllDiary)
        composable(Screen.AllDiary.route) {
            kr.sweetapps.alcoholictimer.ui.tab_02.screens.AllDiaryScreen(
                onNavigateBack = { if (!navController.popBackStack()) navController.navigate(Screen.Records.route) },
                onOpenDiaryDetail = { diaryId -> // [UPDATED] diaryId (Long) 받음
                    val route = Screen.DiaryDetail.createRoute(diaryId.toString())
                    navController.navigate(route)
                }
            )
        }

        // [NEW] 일기 작성 화면 (Room DB 기반)
        composable(Screen.DiaryWrite.route) {
            kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryWriteScreen(
                onDismiss = {
                    // Records 화면 새로고침 트리거
                    recordsRefreshCounter++
                    navController.popBackStack()
                }
            )
        }

        // [NEW] 일기 상세보기/수정 화면 (Room DB 기반)
        composable(
            route = Screen.DiaryDetail.route,
            arguments = listOf(navArgument("diaryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val diaryId = backStackEntry.arguments?.getString("diaryId") ?: return@composable


            // [UPDATED] Room DB 기반 DiaryWriteScreen (ViewModel 사용)
            kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryWriteScreen(
                diaryId = diaryId?.toLongOrNull(), // String -> Long 변환
                onDismiss = {
                    // Records 화면 새로고침 트리거
                    recordsRefreshCounter++
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Level.route) {
            LevelScreen(onNavigateBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(Screen.Start.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }

        composable(Screen.More.route) {
            SettingsScreen(
                onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) },
                onApplyAndGoHome = {
                    // Navigate to Start (Tab1) and recreate Start so its LaunchedEffect runs
                    try {
                        navController.navigate(Screen.Start.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } catch (_: Throwable) {}
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateLicenses = { navController.navigate(Screen.AboutLicenses.route) },
                onNavigatePrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateEditNickname = { navController.navigate(Screen.NicknameEdit.route) },
                onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) },
                onNavigateDebug = { navController.navigate(Screen.Debug.route) },
                onNavigateNotification = { navController.navigate(Screen.Notification.route) },
                onNavigateCustomer = { navController.navigate("customer") },
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
            CurrencyScreen(onBack = { navController.popBackStack() })
        }


        composable(Screen.Debug.route) {
            DebugScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Notification.route) {
            kr.sweetapps.alcoholictimer.ui.tab_05.screens.NotificationListScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // [REMOVED] DetailScreen은 이제 Tab 2 navigation 그래프 안에 있음

        // Customer Screen
        composable("customer") {
            kr.sweetapps.alcoholictimer.ui.tab_05.screens.CustomerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun isHomeRoute(route: String?): Boolean {
    return route == Screen.Start.route ||
           route == Screen.Run.route ||
           route == Screen.Quit.route ||
           route == Screen.Records.route ||
           route == "records_list" // [추가] Tab 2 내부 경로
}
