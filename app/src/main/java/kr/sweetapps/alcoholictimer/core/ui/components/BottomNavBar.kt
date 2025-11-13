package kr.sweetapps.alcoholictimer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.navigation.Screen

private data class BottomItem(
    val screen: Screen,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
    val labelRes: Int,
    val contentDescriptionRes: Int
)

private val bottomItems: List<BottomItem> = listOf(
    BottomItem(
        Screen.Start,
        Icons.Filled.Home,
        Icons.Outlined.Home,
        R.string.drawer_menu_sobriety,
        R.string.drawer_menu_sobriety
    ),
    BottomItem(
        Screen.Records,
        Icons.AutoMirrored.Filled.ListAlt,
        Icons.AutoMirrored.Outlined.ListAlt,
        R.string.drawer_menu_records,
        R.string.drawer_menu_records
    ),
    BottomItem(
        Screen.Level,
        Icons.Filled.BarChart,
        Icons.Outlined.BarChart,
        R.string.drawer_menu_level,
        R.string.drawer_menu_level
    ),
    BottomItem(
        Screen.Settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        R.string.drawer_menu_settings,
        R.string.drawer_menu_settings
    ),
    BottomItem(
        Screen.About,
        Icons.Filled.Info,
        Icons.Outlined.Info,
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
            .height(80.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // 좌우 패딩을 16dp로 줄여서 아이콘을 더 넓게 펼침
            horizontalArrangement = Arrangement.SpaceBetween, // 양 끝에 배치하여 최대한 넓게 펼침
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
            .size(56.dp) // 고정 크기로 레이아웃 안정화
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
        // 아이콘 - 선택 시 filled, 미선택 시 outlined
        Icon(
            imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
            contentDescription = stringResource(id = item.contentDescriptionRes),
            tint = iconColor,
            modifier = Modifier.size(32.dp) // 아이콘 크기 증가 (28dp → 32dp)
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

