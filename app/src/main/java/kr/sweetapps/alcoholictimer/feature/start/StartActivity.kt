package kr.sweetapps.alcoholictimer.feature.start

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ads.AppOpenAdManager
import kr.sweetapps.alcoholictimer.ads.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.core.util.AppUpdateManager
import kr.sweetapps.alcoholictimer.core.util.Constants
import kr.sweetapps.alcoholictimer.MainActivity
import android.graphics.Color as AndroidColor
import kr.sweetapps.alcoholictimer.ads.HomeAdTrigger
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowManager

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

        // DecorView 안정화
        runCatching { window.decorView.setWillNotDraw(false) }

        // 광고 사전 로드 (동의 후)
        UmpConsentManager.requestAndLoadIfRequired(this) { canRequest ->
            if (canRequest) {
                InterstitialAdManager.preload(this)
                // NativeAdManager.preload(this) // removed: native ads not used
            }
        }
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

        val skipSplash = intent.getBooleanExtra("skip_splash", false)
        appUpdateManager = AppUpdateManager(this)

        // Compose로 전달할 스플래시 유지 상태 (초기 true: 스플래시는 광고가 끝날 때까지 유지)
        val holdSplashState = mutableStateOf(true)

        // AppOpenAd 완료/실패 시 스플래시 해제 트리거
        // 자동 라이프사이클 기반 노출은 StartActivity에서 직접 제어(일시 중단)합니다.
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(false)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener {
            // 광고 종료 시 자동 노출을 다시 허용하고 스플래시를 해제
            kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(true)
            runOnUiThread {
                android.util.Log.d("StartActivity", "Ad finished -> releasing holdSplashState")
                holdSplashState.value = false
                // 광고가 종료되면 시스템바 외형 재적용
                applySystemBarAppearance()
            }
        }
        // 광고가 로드되면 수동으로 표시하도록 리스너 등록
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                android.util.Log.d("StartActivity", "Ad loaded -> manual show requested")
                try {
                    kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.showIfAvailable(this@StartActivity)
                    // 광고가 나타난 직후 시스템바가 변경될 수 있으므로 재적용 예약
                    window.decorView.post { applySystemBarAppearance() }
                } catch (t: Throwable) {
                    android.util.Log.w("StartActivity", "manual show failed: $t")
                }
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
                        StartScreenWithUpdate(
                            appUpdateManager = appUpdateManager,
                            initialMinRemainMillis = if (skipSplash) 0L else initialRemain,
                            usesComposeOverlay = usesComposeOverlay,
                            holdSplashState = holdSplashState,
                            onSplashFinished = {
                                // 광고가 닫히고 Compose에서 스플래시가 해제될 때 호출됩니다.
                                val i = Intent(this@StartActivity, MainActivity::class.java)
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                startActivity(i)
                                finish()
                            }
                        )
                    }
                )
            }
        } // <-- closes launchContent lambda

        // 스플래시 overlay가 시작될 때 광고를 바로 띄움 (리스너 등록 및 auto-show 비활성화 후 호출)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.showIfAvailable(this)

        // 광고가 없거나 실패 등으로 콜백이 불리지 않을 경우를 대비한 안전 타임아웃
        // 직접 Activity 전환하지 말고 holdSplashState를 해제하여 Compose에서 onSplashFinished가 호출되게 함
        window.decorView.postDelayed({
            holdSplashState.value = false
        }, 2000) // 광고가 없거나 실패 시 2초 후 자동 진입

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
