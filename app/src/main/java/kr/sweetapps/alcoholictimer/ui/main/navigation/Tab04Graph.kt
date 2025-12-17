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
 *
 * [UPDATE] 우측 상단 설정 버튼으로 Tab 5 (About/설정) 화면 진입
 */
fun NavGraphBuilder.addTab04Graph(navController: NavHostController) {
    composable(Screen.More.route) {
        CommunityScreen(
            onSettingsClick = {
                // [NEW] 설정 버튼 클릭 시 About (Tab 5) 화면으로 이동
                navController.navigate(Screen.About.route)
            }
        )
    }
}

