package kr.sweetapps.alcoholictimer.ui.main.navigation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.firebase.analytics.FirebaseAnalytics
import kr.sweetapps.alcoholictimer.ui.main.Screen
// [FIX] 기존 Screen 직접 호출 제거 -> 래퍼(Tab01...) 호출로 변경
import kr.sweetapps.alcoholictimer.ui.tab_01.Tab01StartScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.Tab01RunScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.QuitScreenComposable

/**
 * Tab 01: 금주 타이머 관련 네비게이션 그래프
 * - Start: 타이머 시작 화면
 * - Run: 타이머 진행 화면
 * - Quit: 포기 확인 화면
 */
fun NavGraphBuilder.addTab01Graph(
    navController: NavHostController,
    activity: Activity?,
    context: Context,
    firebaseAnalytics: FirebaseAnalytics?
) {
    // Start Screen
    composable(Screen.Start.route) {
        // [FIX] StartScreen -> Tab01StartScreen으로 교체
        // 이제 Tab01.kt에 있는 key(configuration) 로직이 작동하여 언어 변경 시 새로고침됩니다.
        Tab01StartScreen(
            gateNavigation = false,
            onStart = { targetDays ->
                val bundle = Bundle()
                bundle.putInt("target_days", targetDays)
                firebaseAnalytics?.logEvent("start_timer", bundle)

                // 광고 정책 체크
                val shouldShowAd = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.shouldShowInterstitialAd(context)

                val proceedToRun: () -> Unit = {
                    navController.navigate(Screen.Run.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                if (shouldShowAd && activity != null) {
                    android.util.Log.d("NavGraph", "[Start] 광고 정책 통과 -> 전면 광고 노출")
                    if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
                        kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(activity) { success ->
                            android.util.Log.d("NavGraph", "[Start] 광고 결과: success=$success")
                            proceedToRun()
                        }
                    } else {
                        android.util.Log.d("NavGraph", "[Start] 광고 로드 안됨 -> 즉시 Run으로 이동")
                        proceedToRun()
                    }
                } else {
                    android.util.Log.d("NavGraph", "[Start] 광고 정책 불통과 (쿨타임/조건) -> 즉시 Run으로 이동")
                    proceedToRun()
                }
            }
        )
    }

    // Run Screen
    composable(Screen.Run.route) {
        // [FIX] RunScreenComposable -> Tab01RunScreen으로 교체
        Tab01RunScreen(
            onRequestQuit = {
                navController.navigate(Screen.Quit.route) { launchSingleTop = true }
            },
            onCompletedNavigateToDetail = { /* MainActivity에서 전역 처리 */ },
            onRequireBackToStart = {
                navController.navigate(Screen.Start.route) {
                    popUpTo(Screen.Run.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    // Quit Screen
    composable(Screen.Quit.route) {
        // Activity Scope ViewModel 가져오기
        val tab01ViewModel: kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel? =
            (activity as? ViewModelStoreOwner)?.let { owner ->
                viewModel(viewModelStoreOwner = owner)
            }

        QuitScreenComposable(
            onQuitConfirmed = {
                // 포기 확인 시 ViewModel의 giveUpTimer() 호출
                android.util.Log.d("NavGraph", "[Quit] Give up confirmed -> calling ViewModel.giveUpTimer()")
                tab01ViewModel?.giveUpTimer()
            },
            onCancel = { navController.popBackStack() }
        )
    }
}