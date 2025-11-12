package kr.sweetapps.alcoholictimer

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
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
    BaseScaffold(navController = navController) {
        AlcoholicTimerNavGraph(navController, startDestination)
    }
}

@Composable
fun AppContent() { AppContentWithStart(Screen.Start.route) }
