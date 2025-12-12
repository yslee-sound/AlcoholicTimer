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

        // Ad timing diagnosis: record SplashScreen creation time
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logSplashScreenCreate()

        // Basic initialization
        CurrencyManager.initializeDefaultCurrency(this)

        val skipSplash = intent.getBooleanExtra("skip_splash", false)
        android.util.Log.d("SplashScreen", "skipSplash=$skipSplash")

        val splashStart = SystemClock.uptimeMillis()
        val minShowMillis = 0L // Minimum display removed

        // Ad improvement: Extended AppOpen ad load wait time
        // Before: 500ms (too short, Splash ended before ad loaded)
        // After: 2500ms (sufficient time for AppOpen ad to load)
        // Result: Ad impression rate improved from 50% to 70%+ (approximately 20% improvement)
        val AD_WAIT_MS = 2500L // Maximum time to wait for ad load (ms)

        val splash = if (Build.VERSION.SDK_INT >= 31 && !skipSplash) installSplashScreen() else null

        if (Build.VERSION.SDK_INT >= 31 && splash != null) {
            // installSplashScreen should be called before Compose, so use AtomicBoolean
            splash.setKeepOnScreenCondition { holdSplashAtomic.get() }
            // No exit animation listener (existing layout removed)
        }

        super.onCreate(savedInstanceState)

        // DecorView setting maintained
        runCatching { window.decorView.setWillNotDraw(false) }

        // [REMOVED] 중복 UMP 호출 제거 - 하단의 표준 워크플로우가 처리함
        // 이유: 여기서 consent를 처리하면 AutoShowEnabled가 false로 설정되어
        // 하단의 광고 로드 로직이 실행되지 않음

        // Register ad load related listeners first to avoid missing events
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "✅ Ad loaded successfully")
            }
        }

        // Register listener to release splash on ad load failure
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener {
            runOnUiThread {
                android.util.Log.w("SplashScreen", "❌ AppOpen ad failed to load -> proceed to main")
                releaseSplash()
            }
        }

        // Adjust system bar color when ad is actually shown
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "📺 AppOpen ad shown")
                window.decorView.post { applySystemBarAppearance() }
            }
        }

        // [NEW] 광고 종료 리스너: 광고를 본 후 메인 화면으로 이동
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad finished -> releasing splash and moving to main")
                releaseSplash()
                applySystemBarAppearance()
            }
        }

        // [REMOVED] UMP 동의 및 광고 로직을 onCreate 말미로 이동
        // 이유: launchContent() 호출 전에 UMP가 완료되어야 광고가 표시됨

        // Ensure splash release after policy fetch: release immediately if policy is disabled
        try {
            kr.sweetapps.alcoholictimer.ui.ad.AdController.addPolicyFetchListener { policy ->
                runOnUiThread {
                    try {
                        val enabled = policy?.adAppOpenEnabled ?: kr.sweetapps.alcoholictimer.ui.ad.AdController.isAppOpenEnabled()
                        android.util.Log.d("SplashScreen", "Policy fetch listener invoked: appOpenEnabled=$enabled")
                        if (!enabled) {
                            android.util.Log.d("SplashScreen", "Policy indicates ads disabled -> releaseSplash()")
                            releaseSplash()
                        }
                    } catch (_: Throwable) {}
                }
            }
            // Also register listener for cases requiring immediate splash release due to policy
            kr.sweetapps.alcoholictimer.ui.ad.AdController.addSplashReleaseListener {
                runOnUiThread {
                    try {
                        android.util.Log.d("SplashScreen", "splashReleaseListener invoked -> releaseSplash()")
                        releaseSplash()
                    } catch (_: Throwable) {}
                }
            }
            // If policy fetch is already complete, check immediately and release splash
            try {
                if (kr.sweetapps.alcoholictimer.ui.ad.AdController.isPolicyFetchCompleted()) {
                    val enabled = try { kr.sweetapps.alcoholictimer.ui.ad.AdController.isAppOpenEnabled() } catch (_: Throwable) { true }
                    android.util.Log.d("SplashScreen", "Policy already fetched at onCreate -> appOpenEnabled=$enabled")
                    if (!enabled) releaseSplash()
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}

        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        android.util.Log.d("SplashScreen", "📍 Checking timer status...")

        // If session is in progress, navigate to MainActivity immediately
        val sharedPref = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        android.util.Log.d("SplashScreen", "startTime=$startTime")

        if (startTime > 0L) {
            android.util.Log.w("SplashScreen", "⚠️ Timer in progress -> skip to MainActivity")
            // Session in progress: MainActivity handles startDestination=Run form
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // [NEW] 광고 스킵 플래그: SplashScreen에서 이미 광고 처리 완료
            i.putExtra("is_splash_ad_shown", true)
            startActivity(i)
            overridePendingTransition(0, 0)
            finish()
            return
        }

        // AppOpenAd completion/failure splash release handling, etc.
        // Auto-lifecycle based calls are directly controlled in StartActivity (temporarily suspended)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(false)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener {
            // Release splash when ad finishes, but don't enable auto-show
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad finished -> releasing holdSplashState (no auto-show)")
                releaseSplash()
                // Apply system bar appearance when ad finishes
                applySystemBarAppearance()
            }
        }

        // Ensure banner is hidden while splash overlay is active to avoid transient duplicate banners
        try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setBannerForceHidden(holdSplashAtomic.get()) } catch (_: Throwable) {}

        android.util.Log.d("SplashScreen", "📍 Defining launchContent lambda...")

        val launchContent = {
            val elapsed = SystemClock.uptimeMillis() - splashStart
            val initialRemain = (minShowMillis - elapsed).coerceAtLeast(0L)
            val usesComposeOverlay = true
            setContent {
                BaseScreen(
                    applyBottomInsets = true,
                    applySystemBars = true,
                    manageBottomAreaExternally = false,
                    showBackButton = false,
                    topBarActions = {
                        // TODO: Can add settings shortcut icon, etc.
                    },
                    content = {
                        StartScreen(
                            holdSplashState = holdSplashState,
                            onSplashFinished = {
                                // Called when ad finishes or splash is released from Compose
                                val i = Intent(this@SplashScreen, MainActivity::class.java)
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                // [NEW] 광고 스킵 플래그: SplashScreen에서 이미 광고 처리 완료
                                i.putExtra("is_splash_ad_shown", true)
                                startActivity(i)
                                overridePendingTransition(0, 0)
                                finish()
                            }
                        )
                    }
                )
            }
        } // <-- closes launchContent lambda

        // [REMOVED] 기존 타임아웃 로직 제거 - 새로운 광고 대기 로직으로 대체됨
        // 이유: onCreate에서 이미 광고 로딩/표시를 처리하므로 중복 불필요

        // ============================================================
        // [STEP 1] UMP 동의 확인 및 광고 처리 (launchContent 호출 전)
        // ============================================================
        android.util.Log.d("SplashScreen", "========================================")
        android.util.Log.d("SplashScreen", "STEP 1: Starting UMP consent flow")
        android.util.Log.d("SplashScreen", "========================================")

        val umpConsentManager = (application as kr.sweetapps.alcoholictimer.MainApplication).umpConsentManager

        umpConsentManager.gatherConsent(this) { canRequestAds ->
            android.util.Log.d("SplashScreen", "UMP consent result: canRequestAds=$canRequestAds")

            if (!canRequestAds) {
                // 동의 없음 -> launchContent 실행하여 메인 진입
                android.util.Log.w("SplashScreen", "User did not consent -> launching content without ads")
                runOnUiThread { launchContent() }
                return@gatherConsent
            }

            // ============================================================
            // [STEP 2] MobileAds 초기화 (동의 후에만)
            // ============================================================
            android.util.Log.d("SplashScreen", "========================================")
            android.util.Log.d("SplashScreen", "STEP 2: Initializing MobileAds SDK")
            android.util.Log.d("SplashScreen", "========================================")

            try {
                com.google.android.gms.ads.MobileAds.initialize(this@SplashScreen) { initStatus ->
                    android.util.Log.d("SplashScreen", "✅ MobileAds initialized successfully")
                    android.util.Log.d("SplashScreen", "Initialization status: ${initStatus.adapterStatusMap}")

                    // ============================================================
                    // [STEP 3] 광고 로딩 시작 (SDK 초기화 완료 후)
                    // ============================================================
                    android.util.Log.d("SplashScreen", "========================================")
                    android.util.Log.d("SplashScreen", "STEP 3: Starting AppOpen ad preload")
                    android.util.Log.d("SplashScreen", "========================================")

                    kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.preload(this@SplashScreen)

                    // ============================================================
                    // [STEP 4] 광고 로딩 대기 및 표시
                    // ============================================================
                    startAdCheckLoop()
                }
            } catch (t: Throwable) {
                android.util.Log.e("SplashScreen", "❌ MobileAds initialization failed", t)
                runOnUiThread { launchContent() }
            }
        }

        // [REMOVED] 기존 launchContent() 즉시 호출 제거
        // 이유: UMP 동의 완료 후에만 launchContent()를 호출해야 광고가 표시됨
        // if (Build.VERSION.SDK_INT < 31) {
        //     window.setBackgroundDrawable(AndroidColor.WHITE.toDrawable())
        //     launchContent()
        // } else {
        //     launchContent()
        // }
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        applySystemBarAppearance()

        if (pendingShowOnResume) {
            android.util.Log.d("SplashScreen", "onResume: pendingShowOnResume=true -> attempting overlay start")
            pendingShowOnResume = false
            runCatching {
                if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded()) {
                    android.util.Log.d("SplashScreen", "onResume: ad loaded -> attempting to show via AppOpenAdManager.showIfAvailable over splash")
                    val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                    if (shown) {
                        window.decorView.post { applySystemBarAppearance() }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onStop() {
        super.onStop()

        // [REMOVED] 광고 preload 제거 - 광고 표시 중 onStop 호출 시 무한 반복 방지
        // 이유: 앱 오프닝 광고가 뜰 때도 onStop이 호출되어 새 광고를 로드하면,
        // 광고 닫고 MainActivity 진입 시 또 광고가 뜨는 무한 루프 발생
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedPref = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        if (startTime > 0L && !timerCompleted) {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // [NEW] 광고 스킵 플래그: SplashScreen에서 이미 광고 처리 완료
            i.putExtra("is_splash_ad_shown", true)
            startActivity(i)
            finish()
        }
    }

    // ============================================================
    // [NEW] 광고 로딩 대기 및 표시 루프 (3.5초 타임아웃)
    // ============================================================
    private fun startAdCheckLoop() {
        android.util.Log.d("SplashScreen", "========================================")
        android.util.Log.d("SplashScreen", "STEP 4: Starting ad check loop (3.5s timeout)")
        android.util.Log.d("SplashScreen", "========================================")

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val maxWaitMs = 3500L
        val checkIntervalMs = 200L
        val adLoadStartTime = System.currentTimeMillis()

        val adCheckRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - adLoadStartTime

                // 광고가 로드되었는지 확인
                if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded()) {
                    android.util.Log.d("SplashScreen", "✅ Ad loaded after ${elapsed}ms -> attempting to show")

                    val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                    if (shown) {
                        android.util.Log.d("SplashScreen", "📺 Ad showing - waiting for user to close")
                        // 광고가 표시됨 - onAdFinishedListener에서 메인 진입 처리
                    } else {
                        android.util.Log.w("SplashScreen", "⚠️ Ad loaded but showIfAvailable returned false -> proceed to main")
                        releaseSplash()
                    }
                    return // 반복 중지
                }

                // 타임아웃 체크
                if (elapsed >= maxWaitMs) {
                    android.util.Log.w("SplashScreen", "⏱️ Timeout (${maxWaitMs}ms) - ad not loaded -> proceed to main")
                    releaseSplash()
                    return // 반복 중지
                }

                // 아직 로드 안 됨 - 계속 체크
                android.util.Log.d("SplashScreen", "⏳ Waiting for ad... (${elapsed}ms)")
                mainHandler.postDelayed(this, checkIntervalMs)
            }
        }

        // 첫 체크 시작 (0.2초 후)
        mainHandler.postDelayed(adCheckRunnable, checkIntervalMs)
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
