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
// [FIX] 기존 Screen 직접 import 제거 (또는 안 쓰게 됨)
// import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen (제거 대상)
// import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable (제거 대상)

// [FIX] 새로 만든 래퍼(Wrapper) 컴포저블 import
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
        // [FIX] StartScreen() -> Tab01StartScreen()으로 교체
        // 이제 언어 변경 시 화면이 자동으로 새로고침됩니다.
        Tab01StartScreen(
            gateNavigation = false,
            onStart = { targetDays ->
                val bundle = Bundle()
                bundle.putInt("target_days", targetDays)
                firebaseAnalytics?.logEvent("start_timer", bundle)

                // [REMOVED] 전면광고 로직 제거 - StartScreenViewModel이 이미 처리함
                // ViewModel에서 카운트다운 → 타이머 시작 → NavigationEvent 발행
                // 여기서는 단순히 Run 화면으로 이동만 수행
                navController.navigate(Screen.Run.route) {
                    popUpTo(Screen.Start.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    // Run Screen
    composable(Screen.Run.route) {
        // [FIX] RunScreenComposable() -> Tab01RunScreen()으로 교체
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
        val tab01ViewModel: kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel? =
            (activity as? ViewModelStoreOwner)?.let { owner ->
                viewModel(viewModelStoreOwner = owner)
            }

        QuitScreenComposable(
            onQuitConfirmed = {
                android.util.Log.d("NavGraph", "[Quit] Give up confirmed -> calling ViewModel.giveUpTimer()")
                tab01ViewModel?.giveUpTimer()
            },
            onCancel = { navController.popBackStack() }
        )
    }
}