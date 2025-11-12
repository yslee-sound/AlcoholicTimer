package kr.sweetapps.alcoholictimer

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.core.ui.DrawerMenu
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen
import kr.sweetapps.alcoholictimer.core.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ads.NativeAdManager
import kr.sweetapps.alcoholictimer.core.ads.UmpConsentManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        val startDestinationRoute = if (startTime > 0L && !timerCompleted) Screen.Run.route else Screen.Start.route

        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            if (canRequest) {
                InterstitialAdManager.preload(this)
                NativeAdManager.preload(this)
            }
        }

        setContent { AppContentWithStart(startDestinationRoute) }
    }
}

@Composable
private fun AppContentWithStart(startDestination: String) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun isRunningNow(): Boolean {
        val sp = context.getSharedPreferences("user_settings", MODE_PRIVATE)
        return sp.getLong("start_time", 0L) > 0L && !sp.getBoolean("timer_completed", false)
    }

    // helper: map menu title to route
    fun mapMenuToRoute(menuTitle: String): String {
        return when (menuTitle) {
            context.getString(R.string.drawer_menu_sobriety) -> if (isRunningNow()) Screen.Run.route else Screen.Start.route
            context.getString(R.string.drawer_menu_records) -> Screen.Records.route
            context.getString(R.string.drawer_menu_level) -> Screen.Level.route
            context.getString(R.string.drawer_menu_settings) -> Screen.Settings.route
            context.getString(R.string.drawer_menu_about) -> Screen.About.route
            else -> Screen.Start.route
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerMenu(
                    nickname = "",
                    selectedItem = null,
                    onNicknameClick = { scope.launch { drawerState.close() } },
                    onItemSelected = { menuItem ->
                        scope.launch {
                            drawerState.close()
                            // small delay to wait for drawer close animation
                            kotlinx.coroutines.delay(120)
                            val route = mapMenuToRoute(menuItem)
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                        }
                    }
                )
            }
        }
    ) {
        BaseScaffold(navController = navController) {
            AlcoholicTimerNavGraph(navController, startDestination)
        }
    }
}

@Composable
fun AppContent() { AppContentWithStart(Screen.Start.route) }
