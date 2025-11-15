package kr.sweetapps.alcoholictimer.core.ui.components

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
import kr.sweetapps.alcoholictimer.navigation.Screen

private data class BottomItem(
    val screen: Screen,
    val iconRes: Int,  // 커스텀 아이콘 drawable 리소스 ID
    val labelRes: Int,
    val contentDescriptionRes: Int
)

private val bottomItems: List<BottomItem> = listOf(
    BottomItem(
        Screen.Start,
        R.drawable.ic_nav_play,
        R.string.drawer_menu_sobriety,
        R.string.drawer_menu_sobriety
    ),
    BottomItem(
        Screen.Records,
        R.drawable.ic_nav_calendardots,
        R.string.drawer_menu_records,
        R.string.drawer_menu_records
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
        R.string.drawer_menu_about
    )
)

@Composable
fun BottomNavBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(kr.sweetapps.alcoholictimer.constants.UiConstants.BOTTOM_NAV_BAR_HEIGHT),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // 좌우 패딩 증가 (8dp → 16dp)
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 중앙을 기준으로 아이템 그룹을 배치하기 위해 Box로 감싼 내부 Row를 사용합니다.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(kr.sweetapps.alcoholictimer.constants.UiConstants.BOTTOM_NAV_ITEM_GAP),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomItems.forEach { item ->
                        val selected = isDestinationSelected(currentDestination, item.screen)
                        BottomNavItem(
                            item = item,
                            isSelected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            .size(kr.sweetapps.alcoholictimer.constants.UiConstants.BOTTOM_NAV_ITEM_SIZE) // 고정 크기로 레이아웃 안정화
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
            modifier = Modifier.size(kr.sweetapps.alcoholictimer.constants.UiConstants.BOTTOM_NAV_ICON_SIZE)
        )
    }
}

private fun isDestinationSelected(current: NavDestination?, screen: Screen): Boolean {
    val route = current?.route ?: return screen == Screen.Start
    // Run 화면은 Home(Start) 탭으로 귀속
    return when (screen) {
        Screen.Start -> route == Screen.Start.route || route == Screen.Run.route
        else -> route == screen.route
    }
}
