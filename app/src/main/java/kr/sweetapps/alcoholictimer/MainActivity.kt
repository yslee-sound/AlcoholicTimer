package kr.sweetapps.alcoholictimer

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.core.ui.DrawerMenu
import androidx.navigation.compose.currentBackStackEntryAsState
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen
import kr.sweetapps.alcoholictimer.core.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.core.ads.NativeAdManager
import kr.sweetapps.alcoholictimer.core.ads.UmpConsentManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 진행 중 세션 여부 체크 (NavHost startDestination에만 반영)
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        val startDestinationRoute = if (startTime > 0L && !timerCompleted) Screen.Run.route else Screen.Start.route

        // 광고 동의 후 미리 로드 (전면/네이티브). Compose 배너는 화면에서 자동 로드됨.
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

    // helper: map menu title to route
    fun mapMenuToRoute(menuTitle: String): String {
        return when (menuTitle) {
            context.getString(R.string.drawer_menu_sobriety) -> Screen.Start.route
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
                            kotlinx.coroutines.delay(200)
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
        // 외부 인텐트에서 특정 라우트로 이동 요청 처리
        val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
        LaunchedEffect(Unit) {
            val route = activity.intent?.getStringExtra("route")
            if (route == "run") {
                // NavHost 초기화가 완료된 뒤 안전하게 navigate
                kotlinx.coroutines.delay(50)
                navController.navigate(Screen.Run.route) {
                    popUpTo(0) { inclusive = false }
                    launchSingleTop = true
                }
                // 일회성 처리 후 제거
                activity.intent?.removeExtra("route")
            }
        }

        BaseScaffold(navController = navController) {
            AlcoholicTimerNavGraph(navController, startDestination)
        }
    }
}

@Composable
fun AppContent() { AppContentWithStart(Screen.Start.route) }
