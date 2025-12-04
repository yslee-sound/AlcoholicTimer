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
import kr.sweetapps.alcoholictimer.MainActivity
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.mutableStateOf
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen

// 추�?: AdMob AppOpen 로드/콜백 (?�버그용 직접 로드)
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd


/**
 * ?�전 `StartActivity`????��??그�?�???�� ?�플?�시 ?�용 ?�티비티?�니??
 * ?�일�?�??�키지�?`ui.screens`�??�동?�습?�다.
 */
class SplashScreen : BaseActivity() {

    // ?�티비티 lifecycle ?�태 ?�래�?
    private var isResumed: Boolean = false
    // 광고가 로드?��?�??�직 Activity가 resume ?�태가 ?�니???�약??경우 true�??�정
    private var pendingShowOnResume: Boolean = false

    // ?�플?�시 ?��? ?�태�??�래???�벨�??�동 (installSplashScreen�?Compose �??�기??
    private val holdSplashAtomic = java.util.concurrent.atomic.AtomicBoolean(true)
    private val holdSplashState = mutableStateOf(holdSplashAtomic.get())

    private fun releaseSplash() {
        // ?�� ?�?�밍 진단: SplashScreen 종료 ?�각 기록
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
        // ?�� ?�?�밍 진단: SplashScreen ?�성 ?�각 기록
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logSplashScreenCreate()

        // 기본 초기??
        kr.sweetapps.alcoholictimer.util.CurrencyManager.initializeDefaultCurrency(this)

        val skipSplash = intent.getBooleanExtra("skip_splash", false)

        val splashStart = SystemClock.uptimeMillis()
        val minShowMillis = 0L // ?�이???�레???�거

        // ?? 개선: AppOpen 광고 로드 ?��??�간 ?�장
        // ?�전: 500ms (?�무 짧아??광고 로드 ?�에 Splash 종료)
        // 개선: 2500ms (AppOpen 광고 로드 ?�료까�? 충분???��?
        // ?�과: 광고 ?�출�?50% ??70% ?�상 (추�? 20% 개선)
        val AD_WAIT_MS = 2500L // 광고 로드�?기다리는 최�? ?�간 (ms)

        val splash = if (Build.VERSION.SDK_INT >= 31 && !skipSplash) installSplashScreen() else null

        if (Build.VERSION.SDK_INT >= 31 && splash != null) {
            // installSplashScreen?� Compose보다 먼�? ?�출?????�으므�?AtomicBoolean???�용
            splash.setKeepOnScreenCondition { holdSplashAtomic.get() }
            // 종료 ?�니메이??리스???�거(기존 ?�이????��)
        }

        super.onCreate(savedInstanceState)

        // DecorView ?�정??
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

        // 광고 로드 관??리스?��? 먼�? ?�록?�여 ?�벤?��? ?�치지 ?�도�???
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad loaded -> manual show requested (listener)")
                try {
                    // 만약 ?�티비티가 resume ?�태가 ?�니?�면 onResume ?�도?�도�??�약
                    if (!isResumed) {
                        android.util.Log.d("SplashScreen", "Ad loaded but activity not resumed -> scheduling show on resume")
                        pendingShowOnResume = true
                        return@runOnUiThread
                    }

                    // 광고가 로드?�어 ?�고 ?�플?�시 ?��? 중이�?AppOpenAdManager.showIfAvailable???�용?�여
                    // UMP ?�의 ?�태?� ?�책 검?��? 거친 ?�에�?광고�??�시?�도�??�니??
                    if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded() && holdSplashState.value) {
                        android.util.Log.d("SplashScreen", "Ad loaded and activity resumed -> attempting to show via AppOpenAdManager.showIfAvailable")
                        try {
                            val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                            if (shown) {
                                android.util.Log.d("SplashScreen", "AppOpenAdManager showed ad")
                                // overlay/manager가 광고�?처리?�면 광고 종료 콜백?�서 releaseSplash()가 ?�출?�니??
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

                    // �??�의 경우(광고 미로???? ?�플?�시 ?�제
                    releaseSplash()
                    android.util.Log.d("SplashScreen", "Ad loaded but conditions not met -> releaseSplash() called")
                } catch (t: Throwable) {
                    android.util.Log.w("SplashScreen", "manual show failed: $t")
                    releaseSplash()
                }
            }
        }
        // 광고 로드 ?�패 ???�플?�시�??�?�록 리스???�록
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener {
            runOnUiThread {
                android.util.Log.w("SplashScreen", "AppOpen ad failed to load -> releaseSplash()")
                releaseSplash()
            }
        }
        // 광고가 ?�제�?보여졌을 ???�스?�바 ?�을 조정
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "AppOpen ad shown")
                window.decorView.post { applySystemBarAppearance() }
            }
        }

        // 광고 ?�전 로드 (?�의 ??
        // NOTE: UMP consent flow is now centralized in MainApplication.
        // Do not call UmpConsentManager.requestAndLoadIfRequired from SplashScreen to avoid duplicate forms.
        // MainApplication will dispatch requestAndLoadIfRequired once when the first Activity resumes.

        // ?�책 조회 ?�료 ???�플?�시 ?�제 보장: ?�책??비활?�화?�면 즉시 release
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
            // ?�책?�로 ?�해 즉시 ?�플?�시 ?�제가 ?�요???��? ?�한 리스?�도 ?�록
            kr.sweetapps.alcoholictimer.ui.ad.AdController.addSplashReleaseListener {
                runOnUiThread {
                    try {
                        android.util.Log.d("SplashScreen", "splashReleaseListener invoked -> releaseSplash()")
                        releaseSplash()
                    } catch (_: Throwable) {}
                }
            }
            // ?��? ?�책 조회가 ?�료???�태?�면 즉시 검?�하???�플?�시 ?�제
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

        // 진행 �??�션?�면 MainActivity�?즉시 ?�동
        val sharedPref = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        if (startTime > 0L) {
            // 진행 �??�션: MainActivity가 startDestination=Run ?�로 처리
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
            overridePendingTransition(0, 0)
            finish()
            return
        }

        // AppOpenAd ?�료/?�패 ???�플?�시 ?�제 ?�리�?
        // ?�동 ?�이?�사?�클 기반 ?�출?� StartActivity?�서 직접 ?�어(?�시 중단)?�니??
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(false)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener {
            // 광고 종료 ???�플?�시�??�제?�되 ?�동 ?�출?� ?�성?�하지 ?�음
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad finished -> releasing holdSplashState (no auto-show)")
                releaseSplash()
                // 광고가 종료?�면 ?�스?�바 ?�형 ?�적??
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
                        // 추후: ?�정 바로가�??�이�???추�? 가??
                    },
                    content = {
                        StartScreen(
                            holdSplashState = holdSplashState,
                            onSplashFinished = {
                                // 광고가 ?�히�?Compose?�서 ?�플?�시가 ?�제?????�출?�니??
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

        // ?�플?�시 overlay가 ?�작????즉시 광고�?강제�??�우지 ?�음.
        // ?�???�전 로드??광고가 ?�으�?onAdLoadedListener?�서 처리?�고,
        // 그렇지 ?�으�?짧�? ?��????�플?�시�??�제?�니??
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        // Short timeout runnable: 짧�? ?�?�아?�에?�는 광고가 ?��? 로드?�는지 ?�인?�고 ?�으�?강제 ?�시
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
        // Safety timeout: 강제 ?��??�?�아?? 2초로 줄여 UX ?�상.
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

        // ?? ?�기 최적?? AppOpen 광고 ?�리캐싱
        // ?�이 백그?�운?�로 �????�음 AppOpen 광고�?미리 로드
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
        // 리스???�제
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener(null)
    }

    override fun getScreenTitleResId(): Int = R.string.start_screen_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.start_screen_title)
}
