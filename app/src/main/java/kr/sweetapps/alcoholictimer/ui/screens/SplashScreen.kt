package kr.sweetapps.alcoholictimer.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.ui.main.MainActivity
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen

// Added: AdMob AppOpen load/callback (for debug direct loading)
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager


/**
 * Splash screen activity separated from previous `StartActivity`.
 * File has been moved to `ui.screens` package.
 */
class SplashScreen : BaseActivity() {

    // Activity lifecycle state flag
    private var isResumed: Boolean = false
    // Set to true if ad is loaded but Activity is not yet resumed (scheduled to show on resume)
    private var pendingShowOnResume: Boolean = false

    // Splash screen state flag: double tracking (for both installSplashScreen and Compose)
    private val holdSplashAtomic = java.util.concurrent.atomic.AtomicBoolean(true)
    private val holdSplashState = mutableStateOf(holdSplashAtomic.get())

    private fun releaseSplash() {
        // Ad timing diagnosis: record SplashScreen finish time
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logSplashScreenFinish()

        try {
            holdSplashAtomic.set(false)
        } catch (_: Throwable) {}
        try {
            holdSplashState.value = false
        } catch (_: Throwable) {}
        // restore banner visibility when splash released
        try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
        android.util.Log.d("SplashScreen", "releaseSplash() called -> atomic=${holdSplashAtomic.get()} compose=${holdSplashState.value}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("SplashScreen", "🚀 onCreate START")

        // Ad timing diagnosis
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logSplashScreenCreate()

        // [CRITICAL] 디버그 모드에서는 맨 처음에 동의 상태 리셋 (백업 데이터 충돌 방지)
        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            android.util.Log.d("SplashScreen", "🔧 DEBUG: Resetting consent state at start")
            val umpConsentManager = ((application as? kr.sweetapps.alcoholictimer.MainApplication)?.umpConsentManager)
            umpConsentManager?.resetConsent(this)
        }

        // Basic initialization
        CurrencyManager.initializeDefaultCurrency(this)

        val skipSplash = intent.getBooleanExtra("skip_splash", false)
        android.util.Log.d("SplashScreen", "skipSplash=$skipSplash")

        // Install splash screen
        val splash = if (Build.VERSION.SDK_INT >= 31 && !skipSplash) installSplashScreen() else null

        if (Build.VERSION.SDK_INT >= 31 && splash != null) {
            splash.setKeepOnScreenCondition { holdSplashAtomic.get() }
        }

        super.onCreate(savedInstanceState)

        runCatching { window.decorView.setWillNotDraw(false) }

        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // Check if timer is in progress
        val sharedPref = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)

        if (startTime > 0L) {
            android.util.Log.w("SplashScreen", "⚠️ Timer in progress -> skip to MainActivity")
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            i.putExtra("is_splash_ad_shown", true)
            startActivity(i)
            finish()
            return
        }

        // Hide banner during splash
        try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(true) } catch (_: Throwable) {}

        // Define launchContent
        val launchContent = {
            setContent {
                BaseScreen(
                    applyBottomInsets = true,
                    applySystemBars = true,
                    manageBottomAreaExternally = false,
                    showBackButton = false,
                    topBarActions = {},
                    content = {
                        StartScreen(
                            holdSplashState = holdSplashState,
                            onSplashFinished = {
                                val i = Intent(this@SplashScreen, MainActivity::class.java)
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                i.putExtra("is_splash_ad_shown", true)
                                startActivity(i)
                                finish()
                            }
                        )
                    }
                )
            }
        }

        // ============================================================
        // [STANDARD SEQUENCE] 순차 실행 - 동의 → 초기화 → 광고
        // ============================================================
        android.util.Log.d("SplashScreen", "========================================")
        android.util.Log.d("SplashScreen", "[STEP 1] Starting UMP consent")
        android.util.Log.d("SplashScreen", "========================================")

        val umpConsentManager = (application as kr.sweetapps.alcoholictimer.MainApplication).umpConsentManager

        // STEP 1: UMP 동의 (SDK가 알아서 처리, 완료될 때까지 대기)
        umpConsentManager.gatherConsent(this) { canRequestAds ->
            android.util.Log.d("SplashScreen", "✅ UMP consent completed: canRequestAds=$canRequestAds")

            if (!canRequestAds) {
                // 동의 없음 -> 광고 없이 메인 진입
                android.util.Log.w("SplashScreen", "User did not consent -> proceed without ads")
                runOnUiThread {
                    launchContent()
                    releaseSplash()
                }
                return@gatherConsent
            }

            // STEP 2: MobileAds 초기화 (동의 완료 후에만)
            // [FIX] 백그라운드 스레드에서 실행하여 ANR 방지 (v1.1.9)
            android.util.Log.d("SplashScreen", "========================================")
            android.util.Log.d("SplashScreen", "[STEP 2] Initializing MobileAds SDK (background)")
            android.util.Log.d("SplashScreen", "========================================")

            try {
                // [FIX] Dispatchers.IO에서 초기화하여 메인 스레드 블로킹 방지
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.google.android.gms.ads.MobileAds.initialize(this@SplashScreen) {
                        android.util.Log.d("SplashScreen", "✅ MobileAds initialized (background)")

                        // STEP 3: 광고 로드 및 표시 (메인 스레드에서 실행)
                        runOnUiThread {
                            android.util.Log.d("SplashScreen", "========================================")
                            android.util.Log.d("SplashScreen", "[STEP 3] Loading and showing ad")
                            android.util.Log.d("SplashScreen", "========================================")

                            loadAndShowAd(launchContent)
                        }
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("SplashScreen", "❌ MobileAds init failed", t)
                runOnUiThread {
                    launchContent()
                    releaseSplash()
                }
            }
        }
    }

    /**
     * 광고 로드 및 표시 (단순 방식)
     */
    private fun loadAndShowAd(launchContent: () -> Unit) {
        // [FIX] 타임아웃을 취소하기 위해 핸들러와 러너블을 변수로 선언
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var timeoutRunnable: Runnable? = null

        // 1. 타임아웃 로직 정의 (5초 뒤 실행될 내용)
        timeoutRunnable = Runnable {
            if (holdSplashAtomic.get()) {
                android.util.Log.w("SplashScreen", "⏱️ Timeout (5s) -> Force proceed")

                // 혹시 로드는 됐는데 show가 안 된 상태일 수 있으니 마지막 체크
                if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded()) {
                    val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                    if (!shown) {
                        launchContent()
                        releaseSplash()
                    }
                } else {
                    // 로드 안 됐으면 그냥 이동
                    launchContent()
                    releaseSplash()
                }
            }
        }

        // 2. 광고 로드 성공 리스너
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "✅ Ad loaded -> showing immediately")

                val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                if (!shown) {
                    // 보여주기 실패하면 이동
                    launchContent()
                    releaseSplash()
                    // [FIX] 알람 해제
                    timeoutRunnable?.let { handler.removeCallbacks(it) }
                }
            }
        }

        // 3. 광고 보여주기 시작 리스너 (가장 중요!)
        // [FIX] 광고가 눈에 보이는 순간, 5초 타임아웃을 취소해야 함!
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "👁️ Ad is showing -> Cancel timeout")
                timeoutRunnable?.let { handler.removeCallbacks(it) }
            }
        }

        // 4. 광고 로드 실패 리스너
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener {
            runOnUiThread {
                android.util.Log.w("SplashScreen", "❌ Ad load failed -> proceed to main")
                launchContent()
                releaseSplash()
                // [FIX] 알람 해제
                timeoutRunnable?.let { handler.removeCallbacks(it) }
            }
        }

        // 5. 광고 종료(닫기) 리스너
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "📺 Ad finished -> proceed to main")
                releaseSplash()
                applySystemBarAppearance()
            }
        }

        // 6. 로드 시작 및 타임아웃 가동
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.preload(this@SplashScreen)

        // [FIX] 5초 뒤에 타임아웃 실행 예약
        handler.postDelayed(timeoutRunnable!!, 5000)
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        applySystemBarAppearance()
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener(null)
    }

    override fun getScreenTitleResId(): Int = R.string.start_screen_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.start_screen_title)
}
