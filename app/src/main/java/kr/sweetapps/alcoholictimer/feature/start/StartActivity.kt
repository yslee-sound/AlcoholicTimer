package kr.sweetapps.alcoholictimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.NativeAdManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.util.AppUpdateManager
import kr.sweetapps.alcoholictimer.core.util.Constants
import kr.sweetapps.alcoholictimer.MainActivity
import android.graphics.Color as AndroidColor

class StartActivity : BaseActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 기본 초기화
        kr.sweetapps.alcoholictimer.core.util.CurrencyManager.initializeDefaultCurrency(this)

        val splashStart = SystemClock.uptimeMillis()
        val minShowMillis = 0L // 페이드/딜레이 제거
        val splash = if (Build.VERSION.SDK_INT >= 31) installSplashScreen() else null

        if (Build.VERSION.SDK_INT >= 31) {
            splash?.setKeepOnScreenCondition { false } // 즉시 제거
            // 종료 애니메이션 리스너 제거(기존 페이드 삭제)
        }

        super.onCreate(savedInstanceState)

        // 시스템바 설정 (추후 테마로 이전 가능)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = AndroidColor.WHITE
        window.navigationBarColor = AndroidColor.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isAppearanceLightNavigationBars = true
            }
        }

        // DecorView 안정화
        runCatching { window.decorView.setWillNotDraw(false) }

        // 광고 사전 로드 (동의 후)
        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            if (canRequest) {
                InterstitialAdManager.preload(this)
                NativeAdManager.preload(this)
            }
        }
        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // 진행 중 세션이면 MainActivity로 즉시 이동
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        if (startTime > 0L) {
            // 진행 중 세션: MainActivity가 startDestination=Run 으로 처리
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
            finish()
            return
        }

        val skipSplash = intent.getBooleanExtra("skip_splash", false)
        appUpdateManager = AppUpdateManager(this)

        val launchContent = {
            val elapsed = SystemClock.uptimeMillis() - splashStart
            val initialRemain = (minShowMillis - elapsed).coerceAtLeast(0L)
            val usesComposeOverlay = false
            setContent {
                BaseScreen(
                    applyBottomInsets = true,
                    applySystemBars = true,
                    manageBottomAreaExternally = false,
                    showBackButton = false,
                    topBarActions = {
                        // 추후: 설정 바로가기 아이콘 등 추가 가능
                    },
                    content = {
                        StartScreenWithUpdate(
                            appUpdateManager = appUpdateManager,
                            initialMinRemainMillis = if (skipSplash) 0L else initialRemain,
                            usesComposeOverlay = usesComposeOverlay,
                            onSplashFinished = { /* keep watermark background */ }
                        )
                    }
                )
            }
        } // <-- closes launchContent lambda

        if (Build.VERSION.SDK_INT < 31) {
            window.setBackgroundDrawable(AndroidColor.WHITE.toDrawable())
            launchContent()
            window.decorView.post { window.setBackgroundDrawable(null) }
        } else {
            launchContent()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        if (startTime > 0L && !timerCompleted) {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
            finish()
        }
    }

    override fun getScreenTitleResId(): Int = R.string.start_screen_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.start_screen_title)
}
