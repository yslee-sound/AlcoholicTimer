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
import kr.sweetapps.alcoholictimer.ui.screens.DetailScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.LevelScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.components.AllRecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.QuitScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_04.SettingsScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen
import kr.sweetapps.alcoholictimer.feature.profile.NicknameEditScreen
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.ad.HomeAdTrigger
import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import kr.sweetapps.alcoholictimer.feature.addrecord.AddRecordScreenComposable
import kr.sweetapps.alcoholictimer.feature.debug.DebugAdsScreen
import kr.sweetapps.alcoholictimer.ui.tab_04.screens.CurrencyScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DebugScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.policy.DocumentScreen
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen

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
                    // [수정] 타이머 완료 시 Finished 화면으로 이동
                    android.util.Log.d("NavGraph", "타이머 완료 -> Finished 화면으로 이동")
                    navController.navigate(Screen.Finished.route) {
                        popUpTo(Screen.Run.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
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
            kr.sweetapps.alcoholictimer.ui.tab_01.screens.FinishedScreen(
                onResultCheck = {
                    // [수정] 광고 정책 체크 후 전면 광고 노출, 해당 기록 상세 화면으로 이동
                    android.util.Log.d("NavGraph", "결과 확인 클릭 -> 광고 정책 체크")

                    val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                    val proceedToDetail: () -> Unit = {
                        // [수정] 광고 후 완료된 기록의 상세 화면으로 이동
                        try {
                            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                            val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                            val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                            val completedTargetDays = sharedPref.getFloat("completed_target_days", 21f)
                            val completedActualDays = sharedPref.getInt("completed_actual_days", 0)

                            if (completedStartTime > 0 && completedEndTime > 0) {
                                // [중요] 완료 기록 상세 화면으로 이동
                                val route = Screen.Detail.createRoute(
                                    startTime = completedStartTime,
                                    endTime = completedEndTime,
                                    targetDays = completedTargetDays,
                                    actualDays = completedActualDays,
                                    isCompleted = true
                                )

                                android.util.Log.d("NavGraph", "완료 기록 Detail 화면으로 이동: $route")

                                // [중요] 만료 상태 플래그는 true로 유지
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            } else {
                                // 기록 정보가 없으면 Records 화면으로 폴백
                                android.util.Log.w("NavGraph", "완료 기록 없음 -> Records 화면으로 이동")
                                navController.navigate(Screen.Records.route) {
                                    launchSingleTop = true
                                }
                                recordsRefreshCounter++
                            }
                        } catch (t: Throwable) {
                            android.util.Log.e("NavGraph", "결과 확인 실패", t)
                            navController.navigate(Screen.Records.route) {
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

        composable(Screen.Records.route) {
            RecordsScreen(
                externalRefreshTrigger = recordsRefreshCounter,
                onNavigateToAllRecords = { navController.navigate(Screen.AllDiary.route) },
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
                onAddRecord = { navController.navigate(Screen.DiaryWrite.route) } // [수정] 일기 작성 화면으로 연결
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
                onOpenDiaryDetail = { /* TODO: 연결: 상세 편집/보기로 이동 */ }
            )
        }

        // [NEW] 일기 작성 화면
        composable(Screen.DiaryWrite.route) {
            kr.sweetapps.alcoholictimer.ui.diary.DiaryWriteScreen(
                onDismiss = { navController.popBackStack() },
                onSave = { emoji, content, cravingLevel ->
                    android.util.Log.d("NavGraph", "일기 저장: emoji=$emoji, craving=$cravingLevel")

                    // [NEW] SharedPreferences에 일기 저장
                    try {
                        val sharedPref = context.getSharedPreferences("diary_data", android.content.Context.MODE_PRIVATE)
                        val currentDiaries = sharedPref.getString("diaries", "[]") ?: "[]"
                        val diariesArray = org.json.JSONArray(currentDiaries)

                        // 새 일기 객체 생성
                        val newDiary = org.json.JSONObject().apply {
                            put("timestamp", System.currentTimeMillis())
                            put("date", java.text.SimpleDateFormat("M.dd (E)", java.util.Locale.KOREAN).format(java.util.Date()))
                            put("emoji", emoji)
                            put("content", content.ifEmpty { "내용 없음" })
                            put("cravingLevel", cravingLevel)
                        }

                        // 배열 맨 앞에 추가 (최신순)
                        val updatedArray = org.json.JSONArray().apply {
                            put(newDiary)
                            for (i in 0 until diariesArray.length()) {
                                put(diariesArray.getJSONObject(i))
                            }
                        }

                        sharedPref.edit().putString("diaries", updatedArray.toString()).apply()
                        android.util.Log.d("NavGraph", "일기 저장 완료: ${updatedArray.length()}개")

                        // Records 화면 새로고침 트리거
                        recordsRefreshCounter++
                    } catch (e: Exception) {
                        android.util.Log.e("NavGraph", "일기 저장 실패", e)
                    }

                    // [수익화] 저장 후 광고 정책 체크
                    val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                    val proceedToComplete: () -> Unit = {
                        android.util.Log.d("NavGraph", "일기 저장 완료 -> Records 화면으로 복귀")
                        navController.popBackStack()
                    }

                    if (shouldShowAd && activity != null) {
                        android.util.Log.d("NavGraph", "일기 저장 광고 정책 통과 -> 전면 광고 노출")
                        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { success ->
                                android.util.Log.d("NavGraph", "일기 저장 광고 결과: success=$success")
                                proceedToComplete()
                            }
                        } else {
                            android.util.Log.d("NavGraph", "일기 저장 광고 로드 안됨 -> 즉시 완료")
                            proceedToComplete()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "일기 저장 광고 쿨타임 중 -> 즉시 완료")
                        proceedToComplete()
                    }
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) },
                onApplyAndGoHome = {
                    // Navigate to Start (Tab1)
                    try {
                        navController.navigate(Screen.Start.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
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
