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
 * Tab 05: 설정 & About 서브 화면 네비게이션 그래프
 * [UPDATE] About 메인 화면은 AppNavHost에서 처리 (애니메이션 적용)
 * - AboutLicenses: 오픈소스 라이선스
 * - Privacy: 개인정보처리방침
 * - NicknameEdit: 닉네임 편집
 * - CurrencySettings: 통화 설정
 * - HabitSettings: 습관 설정
 * - Debug: 디버그 메뉴
 * - Notification: 알림 목록
 * - Customer: 고객 지원
 */
fun NavGraphBuilder.addTab05Graph(navController: NavHostController) {
    // [REMOVED] About 메인 화면 - AppNavHost에서 슬라이드 애니메이션과 함께 처리

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

    // [NEW] 습관 설정 (기존 Tab04의 HabitScreen을 독립 화면으로 이동)
    composable(Screen.HabitSettings.route) {
        kr.sweetapps.alcoholictimer.ui.tab_04.HabitSettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateCurrencySettings = { navController.navigate(Screen.CurrencySettings.route) }
        )
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


