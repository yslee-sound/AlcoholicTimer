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

/**
 * 이전 `StartActivity`의 역할을 그대로 옮긴 스플래시 전용 액티비티입니다.
 * 파일명 및 패키지를 `ui.screens`로 이동했습니다.
 */
class SplashScreen : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 기본 초기화
        kr.sweetapps.alcoholictimer.core.util.CurrencyManager.initializeDefaultCurrency(this)

        val skipSplash = intent.getBooleanExtra("skip_splash", false)

        // 스플래시 유지 상태: installSplashScreen에는 AtomicBoolean을 사용하고,
        // Compose에는 MutableState를 동기화하여 setKeepOnScreenCondition의 타이밍 이슈를 방지합니다.
        val holdSplashAtomic = java.util.concurrent.atomic.AtomicBoolean(true)
        val holdSplashState = mutableStateOf(holdSplashAtomic.get())

        fun releaseSplash() {
            try {
                holdSplashAtomic.set(false)
            } catch (_: Throwable) {}
            try {
                holdSplashState.value = false
            } catch (_: Throwable) {}
            android.util.Log.d("SplashScreen", "releaseSplash() called -> atomic=${holdSplashAtomic.get()} compose=${holdSplashState.value}")
        }

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
                    val elapsed = SystemClock.uptimeMillis() - splashStart
                    if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.isLoaded() && holdSplashState.value && elapsed <= AD_WAIT_MS) {
                        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.showIfAvailable(this@SplashScreen)
                        window.decorView.post { applySystemBarAppearance() }
                        return@runOnUiThread
                    }
                    releaseSplash()
                    android.util.Log.d("SplashScreen", "Ad loaded but conditions not met -> releaseSplash() called")
                } catch (t: Throwable) {
                    android.util.Log.w("SplashScreen", "manual show failed: $t")
                    releaseSplash()
                }
            }
        }
        // 광고 사전 로드 (동의 후)
        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            if (canRequest) {
                InterstitialAdManager.preload(this)
                // NativeAdManager.preload(this) // removed: native ads not used
            }
        }

        // 정책 조회 완료 시 스플래시 해제 보장: 정책이 비활성화이면 즉시 release
        try {
            kr.sweetapps.alcoholictimer.ads.AdController.addPolicyFetchListener { policy ->
                runOnUiThread {
                    try {
                        android.util.Log.d("SplashScreen", "Policy fetch listener invoked: policy=${policy?.isActive}")
                        if (policy == null || !policy.isActive) {
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
        // Short timeout runnable
        val shortTimeout = Runnable {
            android.util.Log.d("SplashScreen", "Short splash timeout reached (${AD_WAIT_MS}ms)")
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
            // Do not clear the window background here. Clearing it can cause the
            // system to show its own navigation bar background (semi-transparent/gray)
            // during the transition. Keep the white background as a stable fallback.
        } else {
            launchContent()
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBarAppearance()
        // HomeAdTrigger 호출 제거: NavGraph의 중앙 관찰자에서 홈 그룹 진입을 일괄 처리
        // (StartActivity는 레거시 진입점이며, 진행 중 세션이면 즉시 MainActivity로 이동)
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
        // 리스너 해제
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener(null)
    }

    override fun getScreenTitleResId(): Int = R.string.start_screen_title
    @Deprecated("Use getScreenTitleResId() instead")
    override fun getScreenTitle(): String = getString(R.string.start_screen_title)
}
