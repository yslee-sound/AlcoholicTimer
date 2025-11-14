package kr.sweetapps.alcoholictimer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager
import androidx.compose.runtime.Composable

// small noop comment to trigger reindex
// MainActivity integrity check

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 스플래시를 광고가 끝날 때까지 유지하는 상태
        val holdSplashState = androidx.compose.runtime.mutableStateOf(true)

        // AndroidX SplashScreen: 테마 스플래시를 holdSplashState가 true인 동안 유지
        val splash = installSplashScreen()
        // keep on screen while we want to hold the splash (waiting for ad)
        // note: condition called on main thread
        splash.setKeepOnScreenCondition { holdSplashState.value }

        // 광고가 표시되지 않을 경우 안전 타임아웃
        val timeoutRunnable = Runnable {
            android.util.Log.d("MainActivity", "splash timeout fired -> releasing holdSplashState")
            holdSplashState.value = false
        }
        window.decorView.postDelayed(timeoutRunnable, 5000)

        // Helper to change holdSplashState with logging
        val setHoldSplash: (Boolean) -> Unit = { v ->
            runOnUiThread { android.util.Log.d("MainActivity", "holdSplashState -> $v (thread=${Thread.currentThread().name})"); holdSplashState.value = v }
        }

        // AppOpenAd 동기화: 자동 라이프사이클 노출은 suppressed 상태로 설계되어 있으므로
        // MainActivity에서 수동으로 광고 로드를 표시하도록 리스너 등록
        android.util.Log.d("MainActivity", "disabling auto-show on AppOpenAdManager")
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(false)
        android.util.Log.d("MainActivity", "auto-show disabled")
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener {
            // 광고가 닫히거나 실패하면 오버레이를 해제 (fallback)
            android.util.Log.d("MainActivity", "onAdFinishedListener invoked")
            kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(true)
            runOnUiThread {
                android.util.Log.d("MainActivity", "Ad finished -> releasing holdSplashState (fallback)")
                setHoldSplash(false)
            }
        }
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                // 광고 로드 성공: 안전 타임아웃 취소 후 수동으로 광고 표시
                android.util.Log.d("MainActivity", "Ad loaded -> manual show requested (cancelling timeout)")
                window.decorView.removeCallbacks(timeoutRunnable)
                // 타임아웃으로 인해 스플래시가 이미 해제되었을 수 있으므로 광고가 표시되기 전까지 스플래시를 다시 유지합니다.
                android.util.Log.d("MainActivity", "holding splash prior to showing ad")
                setHoldSplash(true)
                try {
                    android.util.Log.d("MainActivity", "attempting to show ad via AppOpenAdManager.showIfAvailable")
                    kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.showIfAvailable(this@MainActivity)
                } catch (t: Throwable) {
                    android.util.Log.w("MainActivity", "manual show failed: $t")
                }
            }
        }

        // 광고가 실제로 화면에 나타나는 시점에 스플래시를 해제하여 검은 화면 간격을 제거
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("MainActivity", "onAdShownListener invoked: ad is visible; releasing holdSplashState")
                // 안전 타임아웃 제거
                window.decorView.removeCallbacks(timeoutRunnable)
                setHoldSplash(false)
                // 광고가 나타난 후/사라진 직후 시스템바가 덮어써질 수 있으므로 재적용
                applySystemBarAppearance()
            }
        }

        // 강제 라이트 모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Edge-to-Edge 활성화
        enableEdgeToEdge()

        // enableEdgeToEdge()는 decorFitsSystemWindows를 false로 변경할 수 있으므로
        // 시스템바 배경을 윈도우가 직접 그리도록 유지하려면 true로 재설정합니다.
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 시스템 바 색상/appearance 직접 설정 코드 제거됨 (BaseActivity에서 일괄 적용)

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

        android.util.Log.d("MainActivity", "Setting content and entering Compose UI")
        setContent { AppContentWithStart(startDestinationRoute, holdSplashState) }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 리스너 해제
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdShownListener(null)
    }

    override fun onResume() {
        super.onResume()
        // 시스템바 appearance 직접 재적용 코드 제거됨 (BaseActivity에서 일괄 적용)
    }

    // BaseActivity의 추상 함수 구현
    @Deprecated("Overrides deprecated API from BaseActivity")
    override fun getScreenTitle(): String = getString(R.string.app_name)
}

@Composable
private fun AppContentWithStart(
    startDestination: String,
    holdSplashState: androidx.compose.runtime.MutableState<Boolean> = androidx.compose.runtime.mutableStateOf(false)
) {
    val navController = rememberNavController()
    // 테마(윈도우 백그라운드)로 스플래시를 처리하므로, duplicate splash 문제를 방지하기 위해
    // holdSplashState가 true인 동안에는 앱 UI를 렌더하지 않습니다. 테마 스플래시가 보이고,
    // 광고가 끝나면 holdSplashState가 false로 바뀌어 BaseScaffold가 렌더됩니다.
    if (!holdSplashState.value) {
        BaseScaffold(navController = navController) {
            AlcoholicTimerNavGraph(navController, startDestination)
        }
    }
}

@Composable
fun AppContent() { AppContentWithStart(Screen.Start.route) }
