package kr.sweetapps.alcoholictimer.core.ui.components

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
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.navigation.Screen

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
        // 1번째 버튼 그룹: 금주시작(Start), 금주 진행(Run), 금주 종료(Quit)
        associatedRoutes = setOf(Screen.Start.route, Screen.Run.route, Screen.Quit.route)
    ),
    BottomItem(
        Screen.Records,
        R.drawable.ic_nav_calendardots,
        R.string.drawer_menu_records,
        R.string.drawer_menu_records,
        // 2번째 버튼 그룹: 금주 기록(Records), 모든 기록(AllRecords), 기록 상세(Detail)
        // 포함: 기록 추가(add_record)도 같은 그룹으로 처리
        associatedRoutes = setOf(Screen.Records.route, Screen.AllRecords.route, Screen.AddRecord.route, "detail/")
    ),
    BottomItem(
        Screen.Level,
        R.drawable.ic_nav_medal,
        R.string.drawer_menu_level,
        R.string.drawer_menu_level
    ),
    BottomItem(
        Screen.Settings,
        R.drawable.ic_nav_gearsix,
        R.string.drawer_menu_settings,
        R.string.drawer_menu_settings
    ),
    BottomItem(
        Screen.About,
        R.drawable.ic_nav_user,
        R.string.drawer_menu_about,
        R.string.drawer_menu_about,
        associatedRoutes = setOf(Screen.About.route, Screen.AboutLicenses.route, Screen.NicknameEdit.route, Screen.CurrencySettings.route)
    )
)

@Composable
fun BottomNavBar(navController: NavHostController, modifier: Modifier = Modifier) {
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
                                if (!selected) {
                                    navController.navigate(item.screen.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                indication = ripple(
                    bounded = true,
                    radius = 28.dp,
                    color = Color.Gray
                ),
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
