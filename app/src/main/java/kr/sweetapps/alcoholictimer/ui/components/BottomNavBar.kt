package kr.sweetapps.alcoholictimer.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import kr.sweetapps.alcoholictimer.ui.main.Screen

private data class BottomItem(
    val screen: Screen,
    val iconRes: Int,  // 커스텀 아이콘 drawable 리소스 ID
    val labelRes: Int,
    val contentDescriptionRes: Int,
    val associatedRoutes: Set<String> = setOf(screen.route)
)

private val bottomItems: List<BottomItem> = listOf(
    BottomItem(
        Screen.Start,
        R.drawable.ic_nav_play,
        R.string.drawer_menu_sobriety,
        R.string.drawer_menu_sobriety,
        // [REFACTORED] 1번째 버튼 그룹: 금주시작(Start), 금주 진행(Run), 금주 종료(Quit), 목표 달성(Success), 중단(GiveUp)
        associatedRoutes = setOf(Screen.Start.route, Screen.Run.route, Screen.Quit.route, Screen.Success.route, Screen.GiveUp.route)
    ),
    BottomItem(
        Screen.Records,
        R.drawable.ic_nav_calendardots,
        R.string.drawer_menu_records,
        R.string.drawer_menu_records,
        // [UPDATED] 2번째 버튼 그룹: 금주 기록(Records), 모든 기록(AllRecords), 기록 상세(Detail), 일기 관련
        // [FIXED] "all_diaries"(피드 화면) 포함 - 일기 저장 후 피드로 이동하도록 복원 (2025-12-27)
        associatedRoutes = setOf(
            Screen.Records.route,
            Screen.AllRecords.route,
            Screen.AddRecord.route,
            "detail/",
            "diary_write",      // 일기 작성 화면
            "diary_detail/",    // 일기 상세/수정 화면
            "all_diaries",      // [RESTORED] 피드 화면 (일기 저장 후 이동 목적지)
            Screen.LevelDetail.route  // 레벨 상세 화면 (요약 배너에서 진입)
        )
    ),
    // [REMOVED] Tab 3 (Level) - 이제 레벨은 상세 페이지로만 접근
    BottomItem(
        Screen.More,
        R.drawable.user,
        R.string.drawer_menu_more,
        R.string.drawer_menu_more,
        // [UPDATED] Tab 4는 커뮤니티 화면, About 화면은 설정 버튼으로 진입
        // About 관련 라우트도 Tab 4로 연결 (설정 버튼으로 진입하므로)
        associatedRoutes = setOf(
            Screen.More.route,
            Screen.About.route, // [NEW] About는 Tab 4의 설정 버튼으로 진입
            Screen.AboutLicenses.route,
            Screen.NicknameEdit.route,
            Screen.HabitSettings.route,
            Screen.CurrencySettings.route,
            Screen.Debug.route, // [NEW] 디버그도 Tab 4 그룹
            Screen.Notification.route, // [NEW] 알림도 Tab 4 그룹
            "customer" // [NEW] 고객 지원도 Tab 4 그룹
        )
    )
    // [REMOVED] Tab 5 (About) - 이제 Tab 4의 설정 버튼으로 진입
)

@Composable
fun BottomNavBar(
    navController: NavHostController,
    rootNavController: NavHostController? = null, // [NEW] Success 화면 이동용
    modifier: Modifier = Modifier
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentRoute = currentDestination?.route ?: backStackEntry?.destination?.id?.toString() ?: "<null>"

    // 먼저 각 아이템의 매칭 결과를 계산하여, 가장 먼저 매칭되는 인덱스만 선택하도록 결정합니다.
    val matchedIndex = bottomItems.indexOfFirst { isDestinationSelected(currentRoute, currentDestination, it) }
    // Debug: 어떤 route가 선택되었는지 확인용 로그 (선택 인덱스)
    Log.d("BottomNavBar", "currentRoute=$currentRoute destId=${backStackEntry?.destination?.id} selectedIndex=$matchedIndex")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(UiConstants.BOTTOM_NAV_BAR_HEIGHT),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // reduced horizontal padding for small screens
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 중앙을 기준으로 아이템 그룹을 배치하기 위해 Box로 감싼 내부 Row를 사용합니다.
            // Use SpaceEvenly so items never overflow on narrow screens or with large fonts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Give each nav item equal weight so they spread across the full width.
                bottomItems.forEachIndexed { index, item ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val selected = index == matchedIndex
                        BottomNavItem(
                            item = item,
                            isSelected = selected,
                            onClick = {
                                // [FIX v17] 탭별 네비게이션 로직 완전 독립화 (2026-01-03)
                                when (index) {
                                    // 탭 1 (Timer): 만료 상태 확인 로직
                                    0 -> {
                                        val isFinished = kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.isTimerFinished()
                                        Log.d("BottomNavBar", "탭 1 클릭: isFinished=$isFinished")

                                        if (isFinished) {
                                            // Success 화면으로 강제 이동
                                            if (rootNavController != null && currentRoute != Screen.Success.route) {
                                                rootNavController.navigate(Screen.Success.route) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else {
                                            // 타이머 시작 시간 확인
                                            val startTime = kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.getStartTime()
                                            val targetRoute = if (startTime > 0) Screen.Run.route else Screen.Start.route

                                            if (currentRoute != targetRoute) {
                                                navController.navigate(targetRoute) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                }
                                            }
                                        }
                                    }

                                    // 탭 2 (Records): selected 체크 후 navigate
                                    1 -> {
                                        android.util.Log.d("BottomNavBar", "🔵 탭 2 클릭 - selected: $selected, currentRoute: $currentRoute")
                                        if (!selected) {
                                            android.util.Log.d("BottomNavBar", "➡️ 탭 2로 이동 중...")
                                            navController.navigate(Screen.Records.route) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            }
                                        } else {
                                            android.util.Log.d("BottomNavBar", "✋ 이미 탭 2 - navigate 스킵")
                                        }
                                    }

                                    // 다른 탭들: selected 체크 후 navigate
                                    else -> {
                                        if (!selected) {
                                            navController.navigate(item.screen.route) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    item: BottomItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 아이콘 색상 - 선택됨: 검은색, 비활성: 연한 회색
    val iconColor = if (isSelected) Color.Black else Color(0xFFBBBBBB)

    Box(
        modifier = Modifier
            .size(UiConstants.BOTTOM_NAV_ITEM_SIZE) // 고정 크기로 레이아웃 안정화
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                // use default indication (platform-dependent ripple)
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 커스텀 아이콘 (stroke 색상으로 선택/비선택 구분)
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = stringResource(id = item.contentDescriptionRes),
            tint = iconColor,
            modifier = Modifier.size(UiConstants.BOTTOM_NAV_ICON_SIZE)
        )
    }
}

private fun isDestinationSelected(currentRoute: String?, current: NavDestination?, item: BottomItem): Boolean {
    // 우선 currentRoute 문자열 자체에서 간단 검사
    if (currentRoute != null) {
        val cr = currentRoute
        // associatedRoutes 검사: 정확 일치 또는 prefix(예: "detail/")만 허용
        val assocMatch = item.associatedRoutes.any { ar -> if (ar.endsWith("/")) cr.startsWith(ar) else cr == ar }
        if (assocMatch) {
            Log.d("BottomNavBar", "match reason for ${item.screen.route}: associatedMatch (currentRoute=$cr, associated=${item.associatedRoutes})")
            return true
        }
    }

    // fallback: NavDestination의 parent를 따라 올라가며 route 검사
    var dest: NavDestination? = current
    while (dest != null) {
        val r = dest.route
        if (r != null) {
            val assocMatchParent = item.associatedRoutes.any { ar -> if (ar.endsWith("/")) r.startsWith(ar) else r == ar }
            if (assocMatchParent) {
                Log.d("BottomNavBar", "match reason for ${item.screen.route}: parentAssociatedMatch (parentRoute=$r, associated=${item.associatedRoutes})")
                return true
            }
        }
        dest = dest.parent
    }

    val defaultMatch = item.screen == Screen.Start && (currentRoute == null || currentRoute == "<null>")
    if (defaultMatch) Log.d("BottomNavBar", "match reason for ${item.screen.route}: defaultStartMatch")
    return defaultMatch
}
