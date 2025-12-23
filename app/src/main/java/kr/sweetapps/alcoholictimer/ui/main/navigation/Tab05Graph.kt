package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.CustomerScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.NotificationListScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.NicknameEditScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.settings.ProfileEditScreen
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab05ViewModel

/**
 * Tab 05: 설정 & About 서브 화면 네비게이션 그래프
 * [UPDATE] About 메인 화면은 AppNavHost에서 처리 (애니메이션 적용)
 * [UPDATE] HabitSettings도 AppNavHost에서 처리 (슬라이드 애니메이션 적용)
 * [UPDATE] CurrencySettings도 AppNavHost에서 처리 (슬라이드 애니메이션 적용)
 * [UPDATE] Privacy도 AppNavHost에서 처리 (슬라이드 애니메이션 적용)
 * [UPDATE] AboutLicenses도 AppNavHost에서 처리 (슬라이드 애니메이션 적용)
 * [UPDATE] Debug도 AppNavHost에서 처리 (슬라이드 애니메이션 적용) - 2025-12-19
 * [NEW] ProfileEdit: 프로필 전체 편집 (2025-12-23)
 * - NicknameEdit: 닉네임 편집
 * - Notification: 알림 목록
 * - Customer: 고객 지원
 */
fun NavGraphBuilder.addTab05Graph(navController: NavHostController) {
    // [REMOVED] About 메인 화면 - AppNavHost에서 슬라이드 애니메이션과 함께 처리
    // [REMOVED] AboutLicenses - AppNavHost에서 슬라이드 애니메이션과 함께 처리
    // [REMOVED] Privacy - AppNavHost에서 슬라이드 애니메이션과 함께 처리
    // [REMOVED] CurrencySettings - AppNavHost에서 슬라이드 애니메이션과 함께 처리
    // [REMOVED] HabitSettings - AppNavHost에서 슬라이드 애니메이션과 함께 처리
    // [REMOVED] Debug - AppNavHost에서 슬라이드 애니메이션과 함께 처리 (2025-12-19)

    // [NEW] 프로필 전체 편집 (2025-12-23)
    composable(Screen.ProfileEdit.route) {
        val viewModel: Tab05ViewModel = viewModel()
        ProfileEditScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }

    // 닉네임 편집
    composable(Screen.NicknameEdit.route) {
        NicknameEditScreen(
            onDone = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }


    // 알림 목록
    composable(Screen.Notification.route) {
        NotificationListScreen(
            onBack = { navController.popBackStack() }
        )
    }

    // 고객 지원
    composable("customer") {
        CustomerScreen(
            onBack = { navController.popBackStack() }
        )
    }
}


