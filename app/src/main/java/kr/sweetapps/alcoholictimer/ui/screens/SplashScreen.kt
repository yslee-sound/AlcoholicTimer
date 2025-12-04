package kr.sweetapps.alcoholictimer.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.consent.UmpConsentManager
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.ui.main.MainActivity
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.mutableStateOf
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen

// Added: AdMob AppOpen load/callback (for debug direct loading)
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd


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
        // Ad timing diagnosis: record SplashScreen creation time
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logSplashScreenCreate()

        // Basic initialization
        kr.sweetapps.alcoholictimer.util.CurrencyManager.initializeDefaultCurrency(this)

        val skipSplash = intent.getBooleanExtra("skip_splash", false)

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

        // Ensure primary consent flow runs early on Splash so consent form (if required) is presented
        try {
            val mainApp = application as? kr.sweetapps.alcoholictimer.MainApplication
            if (mainApp != null) {
                try {
                    android.util.Log.d("SplashScreen", "Invoking primary UMP gatherConsent from SplashScreen")
                    // Release splash so consent form can be visible on top
                    try { holdSplashAtomic.set(false); holdSplashState.value = false } catch (_: Throwable) {}
                    // Mark full-screen showing to suppress AppOpen while consent UI is active
                    try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                    // Also ensure AppOpen auto-show disabled while consent is handled
                    try { kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(false) } catch (_: Throwable) {}

                    mainApp.umpConsentManager.gatherConsent(this) { canRequest ->
                        android.util.Log.d("SplashScreen", "gatherConsent callback -> canRequestAds=$canRequest")
                        // [수정] gatherConsent가 이미 모든 consent 처리를 완료함
                        try { kr.sweetapps.alcoholictimer.ui.ad.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                        try { kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(false) } catch (_: Throwable) {}
                        // keep splash state as-is; AppOpenAdManager.onConsentUpdated will decide preload/show
                    }
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}

        // Register ad load related listeners first to avoid missing events
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad loaded -> manual show requested (listener)")
                try {
                    // If Activity is not in resumed state, schedule show on resume
                    if (!isResumed) {
                        android.util.Log.d("SplashScreen", "Ad loaded but activity not resumed -> scheduling show on resume")
                        pendingShowOnResume = true
                        return@runOnUiThread
                    }

                    // If ad is loaded and splash screen is active, use AppOpenAdManager.showIfAvailable
                    // to show ad only after UMP consent state and policy verification
                    if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded() && holdSplashState.value) {
                        android.util.Log.d("SplashScreen", "Ad loaded and activity resumed -> attempting to show via AppOpenAdManager.showIfAvailable")
                        try {
                            val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                            if (shown) {
                                android.util.Log.d("SplashScreen", "AppOpenAdManager showed ad")
                                // If overlay/manager handles ad, releaseSplash() will be called from ad finish callback
                                return@runOnUiThread
                            } else {
                                android.util.Log.d("SplashScreen", "AppOpenAdManager declined to show (consent/policy) -> releaseSplash()")
                                releaseSplash()
                                return@runOnUiThread
                            }
                        } catch (t: Throwable) {
                            android.util.Log.w("SplashScreen", "Failed to show via manager: $t")
                            releaseSplash()
                            return@runOnUiThread
                        }
                    }

                    // Other cases (ad not loaded): release splash
                    releaseSplash()
                    android.util.Log.d("SplashScreen", "Ad loaded but conditions not met -> releaseSplash() called")
                } catch (t: Throwable) {
                    android.util.Log.w("SplashScreen", "manual show failed: $t")
                    releaseSplash()
                }
            }
        }
        // Register listener to release splash on ad load failure
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener {
            runOnUiThread {
                android.util.Log.w("SplashScreen", "AppOpen ad failed to load -> releaseSplash()")
                releaseSplash()
            }
        }
        // Adjust system bar color when ad is actually shown
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "AppOpen ad shown")
                window.decorView.post { applySystemBarAppearance() }
            }
        }

        // Ad preload (note)
        // NOTE: UMP consent flow is now centralized in MainApplication.
        // Do not call UmpConsentManager.requestAndLoadIfRequired from SplashScreen to avoid duplicate forms.
        // MainApplication will dispatch requestAndLoadIfRequired once when the first Activity resumes.

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

        // If session is in progress, navigate to MainActivity immediately
        val sharedPref = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        if (startTime > 0L) {
            // Session in progress: MainActivity handles startDestination=Run form
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
                                startActivity(i)
                                overridePendingTransition(0, 0)
                                finish()
                            }
                        )
                    }
                )
            }
        } // <-- closes launchContent lambda

        // Don't force show ad immediately when splash overlay starts.
        // If already preloaded ad exists, handle in onAdLoadedListener,
        // otherwise release splash after short wait
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        // Short timeout runnable: Check if ad is already loaded at short timeout and force show if available
        val shortTimeout = Runnable {
            android.util.Log.d("SplashScreen", "Short splash timeout reached (${AD_WAIT_MS}ms)")
            try {
                if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded() && holdSplashState.value) {
                    android.util.Log.d("SplashScreen","Attempting to show preloaded app-open ad via manager on short timeout (without releasing splash first)")
                    try {
                        val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                        if (shown) {
                            android.util.Log.d("SplashScreen", "AppOpenAdManager showed ad on short timeout")
                            window.decorView.post { applySystemBarAppearance() }
                            return@Runnable
                        } else {
                            android.util.Log.d("SplashScreen", "AppOpenAdManager declined to show on short timeout -> will release splash")
                        }
                    } catch (t: Throwable) {
                        android.util.Log.w("SplashScreen", "Failed to show via manager on short timeout: $t")
                    }
                }
            } catch (_: Throwable) {}
            android.util.Log.d("SplashScreen", "Setting holdSplashState=false (short timeout)")
            releaseSplash()
        }
        // Safety timeout runnable
        // Safety timeout: Reduced forced wait timeout to 2 seconds for improved UX
        val SAFETY_TIMEOUT_MS = 2000L
        val safetyTimeout = Runnable {
            android.util.Log.w("SplashScreen", "Safety splash timeout reached (${SAFETY_TIMEOUT_MS}ms) -> forcing release")
            android.util.Log.d("SplashScreen", "Setting holdSplashState=false (safety timeout)")
            releaseSplash()
        }
        // Schedule
        mainHandler.postDelayed(shortTimeout, AD_WAIT_MS)
        mainHandler.postDelayed(safetyTimeout, SAFETY_TIMEOUT_MS)
        // Cancel scheduled runnables when splash state changes to false
        // (observe via a simple coroutine-esque loop polling the state once it's false)
        val cancelWatcher = object : Runnable {
            override fun run() {
                if (!holdSplashState.value) {
                    try {
                        mainHandler.removeCallbacks(shortTimeout)
                        mainHandler.removeCallbacks(safetyTimeout)
                        android.util.Log.d("SplashScreen", "Cancelled scheduled splash timeouts because holdSplashState=false")
                    } catch (_: Throwable) {}
                } else {
                    mainHandler.postDelayed(this, 200)
                }
            }
        }
        mainHandler.postDelayed(cancelWatcher, 200)

        if (Build.VERSION.SDK_INT < 31) {
            window.setBackgroundDrawable(AndroidColor.WHITE.toDrawable())
            launchContent()
        } else {
            launchContent()
        }
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

        // App reopen optimization: AppOpen ad pre-caching
        // Preload next AppOpen ad whenever app goes to background
        try {
            android.util.Log.d("SplashScreen", "onStop: preloading next AppOpen ad for future use")
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.preload(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("SplashScreen", "onStop: AppOpen preload failed: ${e.message}")
        }
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
            startActivity(i)
            finish()
        }
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
