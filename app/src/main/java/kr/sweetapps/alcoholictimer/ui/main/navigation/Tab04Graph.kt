package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.ui.main.Screen
import kr.sweetapps.alcoholictimer.ui.tab_04.community.CommunityScreen

/**
 * Tab 04: 커뮤니티 화면 (익명 응원 챌린지)
 * - More: Phase 1 - UI/UX 퍼블리싱 완료
 * - Phase 2: 데이터 연동 예정
 * - Phase 3: 실전 로직 & 수익화 예정
 */
fun NavGraphBuilder.addTab04Graph(navController: NavHostController) {
    composable(Screen.More.route) {
        CommunityScreen()
    }
}

