package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_04.screens.CurrencyScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.AboutScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.NicknameEditScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug.DebugScreen
import kr.sweetapps.alcoholictimer.ui.tab_05.screens.policy.DocumentScreen

/**
 * Tab 05: 설정 & About 네비게이션 그래프
 * - About: About 메인 화면
 * - AboutLicenses: 오픈소스 라이선스
 * - Privacy: 개인정보처리방침
 * - NicknameEdit: 닉네임 편집
 * - CurrencySettings: 통화 설정
 * - Debug: 디버그 메뉴
 * - Notification: 알림 목록
 * - Customer: 고객 지원
 */
fun NavGraphBuilder.addTab05Graph(navController: NavHostController) {
    // About 메인 화면
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

    // 오픈소스 라이선스
    composable(Screen.AboutLicenses.route) {
        DocumentScreen(
            resName = "open_source_license",
            onBack = { navController.popBackStack() },
            titleResId = R.string.document_title_open_source
        )
    }

    // 개인정보처리방침
    composable(Screen.Privacy.route) {
        DocumentScreen(
            resName = "privacy_policy_bilingual",
            onBack = { navController.popBackStack() },
            titleResId = R.string.document_title_privacy
        )
    }

    // 닉네임 편집
    composable(Screen.NicknameEdit.route) {
        NicknameEditScreen(
            onDone = { navController.popBackStack() },
            onCancel = { navController.popBackStack() }
        )
    }

    // 통화 설정
    composable(Screen.CurrencySettings.route) {
        CurrencyScreen(onBack = { navController.popBackStack() })
    }

    // 디버그 메뉴
    composable(Screen.Debug.route) {
        DebugScreen(onBack = { navController.popBackStack() })
    }

    // 알림 목록
    composable(Screen.Notification.route) {
        kr.sweetapps.alcoholictimer.ui.tab_05.screens.NotificationListScreen(
            onBack = { navController.popBackStack() }
        )
    }

    // 고객 지원
    composable("customer") {
        kr.sweetapps.alcoholictimer.ui.tab_05.screens.CustomerScreen(
            onBack = { navController.popBackStack() }
        )
    }
}


