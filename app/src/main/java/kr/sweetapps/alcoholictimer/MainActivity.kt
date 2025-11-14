package kr.sweetapps.alcoholictimer

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 강제 라이트 모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Edge-to-Edge 활성화
        enableEdgeToEdge()

        // 시스템 바 색상을 흰색으로 설정
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE

        // 시스템 바 아이콘을 어두운 색으로 설정 (라이트 배경용)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        val startDestinationRoute = if (startTime > 0L && !timerCompleted) Screen.Run.route else Screen.Start.route

        android.util.Log.d("MainActivity", "About to call UmpConsentManager.requestAndLoadIfRequired")
        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            android.util.Log.d("MainActivity", "UMP callback: canRequest=$canRequest")
            if (canRequest) {
                InterstitialAdManager.preload(this)
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
