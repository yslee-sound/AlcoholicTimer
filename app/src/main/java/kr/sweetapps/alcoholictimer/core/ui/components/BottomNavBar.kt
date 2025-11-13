package kr.sweetapps.alcoholictimer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    val icon: ImageVector,
    val labelRes: Int,
    val contentDescriptionRes: Int
)

private val bottomItems: List<BottomItem> = listOf(
    BottomItem(Screen.Start, Icons.Outlined.Home, R.string.drawer_menu_sobriety, R.string.drawer_menu_sobriety),
    BottomItem(Screen.Records, Icons.AutoMirrored.Outlined.ListAlt, R.string.drawer_menu_records, R.string.drawer_menu_records),
    BottomItem(Screen.Level, Icons.Outlined.BarChart, R.string.drawer_menu_level, R.string.drawer_menu_level),
    BottomItem(Screen.Settings, Icons.Outlined.Settings, R.string.drawer_menu_settings, R.string.drawer_menu_settings),
    BottomItem(Screen.About, Icons.Outlined.Info, R.string.drawer_menu_about, R.string.drawer_menu_about)
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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

    // 테마 색상 (앱의 primary 색상 사용)
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryVariant = Color(0xFF8B5CF6) // 보라색 그라데이션

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 40.dp,
                    color = primaryColor
                ),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Container with gradient background when selected
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor,
                                primaryVariant
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = stringResource(id = item.contentDescriptionRes),
                tint = if (isSelected) Color.White else Color(0xFF9CA3AF),
                modifier = Modifier.size(22.dp)
            )
        }

        // Label
        Text(
            text = stringResource(id = item.labelRes),
            fontSize = 11.sp,
            color = if (isSelected) primaryColor else Color(0xFF9CA3AF),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
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
