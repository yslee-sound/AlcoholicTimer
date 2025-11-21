package kr.sweetapps.alcoholictimer.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.constants.Constants
import kr.sweetapps.alcoholictimer.MainActivity
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.mutableStateOf

// 추가: AdMob AppOpen 로드/콜백 (디버그용 직접 로드)
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd


/**
 * 이전 `StartActivity`의 역할을 그대로 옮긴 스플래시 전용 액티비티입니다.
 * 파일명 및 패키지를 `ui.screens`로 이동했습니다.
 */
class SplashScreen : BaseActivity() {

    // 액티비티 lifecycle 상태 플래그
    private var isResumed: Boolean = false
    // 광고가 로드됐지만 아직 Activity가 resume 상태가 아니라 예약된 경우 true로 설정
    private var pendingShowOnResume: Boolean = false

    // 스플래시 유지 상태를 클래스 레벨로 이동 (installSplashScreen과 Compose 간 동기화)
    private val holdSplashAtomic = java.util.concurrent.atomic.AtomicBoolean(true)
    private val holdSplashState = mutableStateOf(holdSplashAtomic.get())

    private fun releaseSplash() {
        try {
            holdSplashAtomic.set(false)
        } catch (_: Throwable) {}
        try {
            holdSplashState.value = false
        } catch (_: Throwable) {}
        android.util.Log.d("SplashScreen", "releaseSplash() called -> atomic=${holdSplashAtomic.get()} compose=${holdSplashState.value}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 기본 초기화
        kr.sweetapps.alcoholictimer.core.util.CurrencyManager.initializeDefaultCurrency(this)

        val skipSplash = intent.getBooleanExtra("skip_splash", false)

        val splashStart = SystemClock.uptimeMillis()
        val minShowMillis = 0L // 페이드/딜레이 제거
        val AD_WAIT_MS = 500L // 광고 로드를 기다리는 최대 시간 (ms)
        val splash = if (Build.VERSION.SDK_INT >= 31 && !skipSplash) installSplashScreen() else null

        if (Build.VERSION.SDK_INT >= 31 && splash != null) {
            // installSplashScreen은 Compose보다 먼저 호출될 수 있으므로 AtomicBoolean을 사용
            splash.setKeepOnScreenCondition { holdSplashAtomic.get() }
            // 종료 애니메이션 리스너 제거(기존 페이드 삭제)
        }

        super.onCreate(savedInstanceState)

        // DecorView 안정화
        runCatching { window.decorView.setWillNotDraw(false) }

        // 광고 로드 관련 리스너를 먼저 등록하여 이벤트를 놓치지 않도록 함
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad loaded -> manual show requested (listener)")
                try {
                    // 만약 액티비티가 resume 상태가 아니라면 onResume 시도하도록 예약
                    if (!isResumed) {
                        android.util.Log.d("SplashScreen", "Ad loaded but activity not resumed -> scheduling show on resume")
                        pendingShowOnResume = true
                        return@runOnUiThread
                    }

                    // 광고가 로드되어 있고 스플래시 유지 중이면 오버레이 액티비티를 시작하여
                    // 광고가 스플래시 위에 표시되도록 합니다.
                    if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.isLoaded() && holdSplashState.value) {
                        android.util.Log.d("SplashScreen", "Ad loaded and activity resumed -> starting AppOpenOverlayActivity to show ad over splash")
                        try {
                            val intent = Intent(this@SplashScreen, kr.sweetapps.alcoholictimer.ads.AppOpenOverlayActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            startActivity(intent)
                            // overlay가 광고를 처리하면 광고 종료 콜백에서 releaseSplash()가 호출됩니다.
                            return@runOnUiThread
                        } catch (t: Throwable) {
                            android.util.Log.w("SplashScreen", "Failed to start overlay activity: $t")
                            releaseSplash()
                            return@runOnUiThread
                        }
                    }

                    // 그 외의 경우(광고 미로드 등) 스플래시 해제
                    releaseSplash()
                    android.util.Log.d("SplashScreen", "Ad loaded but conditions not met -> releaseSplash() called")
                } catch (t: Throwable) {
                    android.util.Log.w("SplashScreen", "manual show failed: $t")
                    releaseSplash()
                }
            }
        }
        // 광고 로드 실패 시 스플래시를 풀도록 리스너 등록
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadFailedListener {
            runOnUiThread {
                android.util.Log.w("SplashScreen", "AppOpen ad failed to load -> releaseSplash()")
                releaseSplash()
            }
        }
        // 광고가 실제로 보여졌을 때 시스템바 등을 조정
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("SplashScreen", "AppOpen ad shown")
                window.decorView.post { applySystemBarAppearance() }
            }
        }

        // 개발(디버그) 모드에서는 동의 여부와 관계없이 로컬 매니저를 강제 preload 하여
        // 오버레이 동작을 손쉽게 테스트할 수 있도록 합니다.
        try {
            val debugMode = try { kr.sweetapps.alcoholictimer.BuildConfig.DEBUG } catch (_: Throwable) { false }
            if (debugMode) {
                android.util.Log.d("SplashScreen", "DEBUG build: forcing AppOpenAdManager.preload for testing")
                runCatching { kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.preload(this) }

                // 디버그 전용 직접 AppOpen 로드/표시: 테스트 단위 ID 사용
                try {
                    val TEST_APP_OPEN_UNIT = "ca-app-pub-3940256099942544/9257395921"
                    val adRequest = AdRequest.Builder().build()
                    AppOpenAd.load(
                        this,
                        TEST_APP_OPEN_UNIT,
                        adRequest,
                        AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                        object : AppOpenAd.AppOpenAdLoadCallback() {
                            override fun onAdLoaded(ad: AppOpenAd) {
                                android.util.Log.d("SplashScreen", "DEBUG AppOpen ad loaded (direct). Showing ad now.")
                                try {
                                    // fullScreen callbacks
                                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                        override fun onAdDismissedFullScreenContent() {
                                            android.util.Log.d("SplashScreen", "DEBUG direct AppOpen dismissed -> releaseSplash()")
                                            runOnUiThread { releaseSplash() }
                                        }

                                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                            android.util.Log.w("SplashScreen", "DEBUG direct AppOpen failed to show: $adError -> releaseSplash()")
                                            runOnUiThread { releaseSplash() }
                                        }

                                        override fun onAdShowedFullScreenContent() {
                                            android.util.Log.d("SplashScreen", "DEBUG direct AppOpen showed")
                                        }
                                    }

                                    runOnUiThread {
                                        if (holdSplashState.value) {
                                            // Activity가 resume 상태일 때만 직접 show 시도
                                            if (isResumed) {
                                                // 스플래시를 유지한 채로 광고를 먼저 띄웁니다.
                                                val shown = try {
                                                    // keep direct show for debug path (overlay activity is not used here)
                                                    ad.show(this@SplashScreen)
                                                    true
                                                } catch (t: Throwable) {
                                                    android.util.Log.w("SplashScreen", "DEBUG direct ad show failed: $t")
                                                    false
                                                }
                                                if (!shown) {
                                                    // 실패하면 스플래시 해제
                                                    releaseSplash()
                                                } else {
                                                    // 성공하면 release는 광고의 dismiss 콜백에서 수행
                                                    window.decorView.post { applySystemBarAppearance() }
                                                }
                                                return@runOnUiThread
                                            } else {
                                                android.util.Log.d("SplashScreen", "DEBUG direct ad loaded but activity not resumed -> pendingShowOnResume=true")
                                                pendingShowOnResume = true
                                            }
                                        } else {
                                            releaseSplash()
                                        }
                                    }
                                } catch (t: Throwable) {
                                    android.util.Log.w("SplashScreen", "DEBUG AppOpen onAdLoaded handler error: $t")
                                    releaseSplash()
                                }
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                android.util.Log.w("SplashScreen", "DEBUG AppOpen failed to load (direct): $loadAdError -> releaseSplash()")
                                releaseSplash()
                            }
                        }
                    )
                } catch (t: Throwable) {
                    android.util.Log.w("SplashScreen", "DEBUG AppOpen direct load error: $t")
                    releaseSplash()
                }
            }
        } catch (_: Throwable) {}
        // 광고 사전 로드 (동의 후)
        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            try {
                val debugMode = try { kr.sweetapps.alcoholictimer.BuildConfig.DEBUG } catch (_: Throwable) { false }
                if (canRequest) {
                    InterstitialAdManager.preload(this)
                    runCatching { kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.preload(this) }
                } else {
                    // 동의가 아직 없어도, 디버그 모드에서는 테스트용 preload 시도
                    if (debugMode) {
                        android.util.Log.d("SplashScreen", "UMP denied but DEBUG: forcing preload for testing")
                        InterstitialAdManager.preload(this)
                        runCatching { kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.preload(this) }
                    } else {
                        android.util.Log.d("SplashScreen", "UMP consent=false -> not preloading ads until consent")
                    }
                }
            } catch (_: Throwable) {}
        }

        // 정책 조회 완료 시 스플래시 해제 보장: 정책이 비활성화이면 즉시 release
        try {
            kr.sweetapps.alcoholictimer.ads.AdController.addPolicyFetchListener { policy ->
                runOnUiThread {
                    try {
                        val enabled = policy?.adAppOpenEnabled ?: kr.sweetapps.alcoholictimer.ads.AdController.isAppOpenEnabled()
                        android.util.Log.d("SplashScreen", "Policy fetch listener invoked: appOpenEnabled=$enabled")
                        if (!enabled) {
                            android.util.Log.d("SplashScreen", "Policy indicates ads disabled -> releaseSplash()")
                            releaseSplash()
                        }
                    } catch (_: Throwable) {}
                }
            }
            // 정책으로 인해 즉시 스플래시 해제가 필요할 때를 위한 리스너도 등록
            kr.sweetapps.alcoholictimer.ads.AdController.addSplashReleaseListener {
                runOnUiThread {
                    try {
                        android.util.Log.d("SplashScreen", "splashReleaseListener invoked -> releaseSplash()")
                        releaseSplash()
                    } catch (_: Throwable) {}
                }
            }
            // 이미 정책 조회가 완료된 상태라면 즉시 검사하여 스플래시 해제
            try {
                if (kr.sweetapps.alcoholictimer.ads.AdController.isPolicyFetchCompleted()) {
                    val enabled = try { kr.sweetapps.alcoholictimer.ads.AdController.isAppOpenEnabled() } catch (_: Throwable) { true }
                    android.util.Log.d("SplashScreen", "Policy already fetched at onCreate -> appOpenEnabled=$enabled")
                    if (!enabled) releaseSplash()
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}

        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // 진행 중 세션이면 MainActivity로 즉시 이동
        val sharedPref = getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        if (startTime > 0L) {
            // 진행 중 세션: MainActivity가 startDestination=Run 으로 처리
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
            overridePendingTransition(0, 0)
            finish()
            return
        }

        // AppOpenAd 완료/실패 시 스플래시 해제 트리거
        // 자동 라이프사이클 기반 노출은 StartActivity에서 직접 제어(일시 중단)합니다.
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(false)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener {
            // 광고 종료 시 자동 노출을 다시 허용하고 스플래시를 해제
            kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(true)
            runOnUiThread {
                android.util.Log.d("SplashScreen", "Ad finished -> releasing holdSplashState")
                releaseSplash()
                // 광고가 종료되면 시스템바 외형 재적용
                applySystemBarAppearance()
            }
        }

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
                        // 추후: 설정 바로가기 아이콘 등 추가 가능
                    },
                    content = {
                        StartScreen(
                            holdSplashState = holdSplashState,
                            onSplashFinished = {
                                // 광고가 닫히고 Compose에서 스플래시가 해제될 때 호출됩니다.
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

        // 스플래시 overlay가 시작될 때 즉시 광고를 강제로 띄우지 않음.
        // 대신 사전 로드된 광고가 있으면 onAdLoadedListener에서 처리하고,
        // 그렇지 않으면 짧은 대기 후 스플래시를 해제합니다.
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        // Short timeout runnable: 짧은 타임아웃에서는 광고가 이미 로드됐는지 확인하고 있으면 강제 표시
        val shortTimeout = Runnable {
            android.util.Log.d("SplashScreen", "Short splash timeout reached (${AD_WAIT_MS}ms)")
            try {
                if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.isLoaded() && holdSplashState.value) {
                    android.util.Log.d("SplashScreen","Attempting to start overlay for preloaded app-open ad on short timeout (without releasing splash first)")
                    try {
                        val intent = Intent(this@SplashScreen, kr.sweetapps.alcoholictimer.ads.AppOpenOverlayActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        startActivity(intent)
                        window.decorView.post { applySystemBarAppearance() }
                        return@Runnable
                    } catch (t: Throwable) {
                        android.util.Log.w("SplashScreen", "Failed to start overlay on short timeout: $t")
                    }
                }
            } catch (_: Throwable) {}
            android.util.Log.d("SplashScreen", "Setting holdSplashState=false (short timeout)")
            releaseSplash()
        }
        // Safety timeout runnable
        val SAFETY_TIMEOUT_MS = 3000L
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
                if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.isLoaded()) {
                    android.util.Log.d("SplashScreen", "onResume: ad loaded -> starting overlay activity to show ad over splash")
                    val intent = Intent(this@SplashScreen, kr.sweetapps.alcoholictimer.ads.AppOpenOverlayActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    window.decorView.post { applySystemBarAppearance() }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
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
            overridePendingTransition(0, 0)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 리스너 해제
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadFailedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdShownListener(null)
    }

    override fun getScreenTitleResId(): Int = R.string.start_screen_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.start_screen_title)
}
