package kr.sweetapps.alcoholictimer.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

    NavigationBar(modifier = modifier) {
        bottomItems.forEach { item ->
            val selected = isDestinationSelected(currentDestination, item.screen)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.contentDescriptionRes)
                    )
                },
                label = { Text(text = stringResource(id = item.labelRes)) }
            )
        }
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
