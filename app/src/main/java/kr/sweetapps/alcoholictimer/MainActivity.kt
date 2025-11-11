package kr.sweetapps.alcoholictimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.core.ui.DrawerMenu
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // helper: map menu title to route
    fun mapMenuToRoute(menuTitle: String): String {
        return when (menuTitle) {
            context.getString(R.string.drawer_menu_sobriety) -> "start"
            context.getString(R.string.drawer_menu_records) -> "records"
            context.getString(R.string.drawer_menu_level) -> "level"
            context.getString(R.string.drawer_menu_settings) -> "settings"
            context.getString(R.string.drawer_menu_about) -> "about"
            else -> "start"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerMenu(
                    nickname = "",
                    selectedItem = null,
                    onNicknameClick = {
                        scope.launch { drawerState.close() }
                    },
                    onItemSelected = { menuItem ->
                        scope.launch {
                            drawerState.close()
                            // small delay to wait for drawer close animation
                            kotlinx.coroutines.delay(200)
                            val route = mapMenuToRoute(menuItem)
                            if (route == "start") {
                                // decide between start/run depending on saved prefs? default to start
                                navController.navigate("start") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                }
                            } else {
                                navController.navigate(route)
                            }
                        }
                    }
                )
            }
        }
    ) {
        val backStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = backStackEntry?.destination?.route ?: "start"
        val titleRes = when (currentRoute) {
            "start" -> R.string.start_screen_title
            "run" -> R.string.run_title
            "records" -> R.string.records_title
            "all_records" -> R.string.records_title
            "level" -> R.string.level_title
            "settings" -> R.string.settings_title
            "about" -> R.string.about_title
            else -> R.string.start_screen_title
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = titleRes)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(id = R.string.cd_menu))
                        }
                    },
                    actions = {
                        if (currentRoute != "settings") {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(id = R.string.settings_title))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF2C3E50),
                        navigationIconContentColor = Color(0xFF2C3E50),
                        actionIconContentColor = Color(0xFF2C3E50)
                    )
                )
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                BaseScaffold {
                    NavHost(navController = navController, startDestination = "start") {
                        composable("start") { kr.sweetapps.alcoholictimer.feature.start.StartScreen(onStart = { navController.navigate("run") }) }
                        composable("run") { kr.sweetapps.alcoholictimer.feature.run.RunScreenComposable() }
                        composable("records") { kr.sweetapps.alcoholictimer.feature.records.components.RecordsScreen(externalRefreshTrigger = 0, onNavigateToAllRecords = { navController.navigate("all_records") }, onNavigateToDetail = { _ -> /* TODO: navigate to detail route */ }) }
                        composable("all_records") { kr.sweetapps.alcoholictimer.feature.records.components.AllRecordsScreen(onNavigateToDetail = { /* TODO */ }) }
                        composable("level") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Level (todo)") } }
                        composable("settings") { kr.sweetapps.alcoholictimer.feature.settings.SettingsScreen() }
                        composable("about") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("About (todo)") } }
                    }
                }
            }
        }
    }
}
