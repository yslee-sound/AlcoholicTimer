package kr.sweetapps.alcoholictimer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.ui.BaseScaffold
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kr.sweetapps.alcoholictimer.data.supabase.SupabaseProvider
import kr.sweetapps.alcoholictimer.data.supabase.repository.EmergencyPolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.NoticePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.PopupPolicyManager
import kr.sweetapps.alcoholictimer.data.supabase.repository.UpdatePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kr.sweetapps.alcoholictimer.ui.dialogs.OptionalUpdateDialog
import android.os.Build
import androidx.compose.ui.graphics.Color

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
        // In MainActivity we do NOT show AppOpen ads; AppOpen should be displayed only from StartActivity (splash).
        // Keep a no-op listener that simply releases splash if policy disables ads. This avoids showing
        // app-open ads during normal in-app navigation which would violate policy.
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                val policyEnabled = try { kr.sweetapps.alcoholictimer.ads.AdController.isAppOpenEnabled() } catch (_: Throwable) { true }
                if (!policyEnabled) {
                    android.util.Log.d("MainActivity", "Ad loaded but policy disabled -> release splash immediately (MainActivity)")
                    setHoldSplash(false)
                    return@runOnUiThread
                }
                // If ad loaded while in MainActivity, do not present it here. StartActivity (splash) is responsible
                // for showing AppOpen ads. Just cancel the startup timeout and release the splash so app proceeds.
                android.util.Log.d("MainActivity", "Ad loaded in MainActivity -> not showing here (splash-only). Releasing splash if held")
                window.decorView.removeCallbacks(timeoutRunnable)
                setHoldSplash(false)
            }
        }

        // 정책이 비활성화될 때 스플래시를 즉시 해제할 수 있도록 리스너 등록
        try {
            kr.sweetapps.alcoholictimer.ads.AdController.addSplashReleaseListener {
                runOnUiThread {
                    android.util.Log.d("MainActivity", "AdController splashReleaseListener invoked -> release splash")
                    setHoldSplash(false)
                }
            }
        } catch (_: Throwable) {}

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
        // enableEdgeToEdge() // Removed to follow single source strategy for system bars

        // enableEdgeTo-Edge()는 decorFitsSystemWindows를 false로 변경할 수 있으므로
        // 시스템바 배경을 윈도우가 직접 그리도록 유지하려면 true로 재설정합니다.
        // WindowCompat.setDecorFitsSystemWindows(window, true) // Now handled in BaseActivity

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
    val context = LocalContext.current

    // popup policy manager setup
    // Note: Supabase client was removed; use stub repositories matching current constructors
    val emergencyRepo = remember { EmergencyPolicyRepository() }
    val updateRepo = remember { UpdatePolicyRepository(context) }
    val noticeRepo = remember { NoticePolicyRepository() }
    val policyManager = remember { PopupPolicyManager(emergencyRepo, updateRepo, noticeRepo, context) }

    val showOptionalUpdateDialogState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val currentUpdatePolicyState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy?>(null) }

    // decide popup once when UI rendered (after splash)
    androidx.compose.runtime.LaunchedEffect(key1 = holdSplashState.value) {
        if (!holdSplashState.value) {
            try {
                val decision = policyManager.decidePopup(android.os.Build.VERSION.RELEASE ?: "")
                if (decision is PopupDecision.ShowUpdate) {
                    val policy = decision.policy
                    currentUpdatePolicyState.value = policy
                    showOptionalUpdateDialogState.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 테마(윈도우 백그라운드)로 스플래시를 처리하므로, duplicate splash 문제를 방지하기 위해
    // holdSplashState가 true인 동안에는 앱 UI를 렌더하지 않습니다. 테마 스플래시가 보이고,
    // 광고가 끝나면 holdSplashState가 false로 바뀌어 BaseScaffold가 렌더됩니다.
    if (!holdSplashState.value) {
        val contentBg = if (startDestination == kr.sweetapps.alcoholictimer.navigation.Screen.Run.route) Color(0xFFEEEDE9) else null
        BaseScaffold(navController = navController, contentBackground = contentBg) {
            AlcoholicTimerNavGraph(navController, startDestination)
        }
    }

    // show update dialog if present
    if (showOptionalUpdateDialogState.value && currentUpdatePolicyState.value != null) {
        val policy = currentUpdatePolicyState.value!!
        OptionalUpdateDialog(
            isForce = policy.isForceUpdate,
            title = "앱 업데이트",
            description = policy.releaseNotes ?: "",
            updateButtonText = "지금 업데이트",
            laterButtonText = "나중에",
            features = null,
            onUpdateClick = {
                val url = policy.downloadUrl ?: "https://play.google.com/store/apps/details?id=${context.packageName}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            onLaterClick = {
                // record later click
                policyManager.dismissUpdate(policy.targetVersionCode)
                showOptionalUpdateDialogState.value = false
            }
        )
    }
}

@Composable
fun AppContent() { AppContentWithStart(Screen.Start.route) }
