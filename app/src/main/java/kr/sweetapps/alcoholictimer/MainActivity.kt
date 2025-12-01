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
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kr.sweetapps.alcoholictimer.data.supabase.repository.EmergencyPolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.NoticePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.PopupPolicyManager
import kr.sweetapps.alcoholictimer.data.supabase.repository.UpdatePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kr.sweetapps.alcoholictimer.ui.dialogs.OptionalUpdateDialog
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy
import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement
import kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy
import kr.sweetapps.alcoholictimer.ui.dialogs.AnnouncementDialog
import kr.sweetapps.alcoholictimer.ui.dialogs.EmergencyRedirectDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.google.android.gms.ads.MobileAds
import kr.sweetapps.alcoholictimer.ads.AdController
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager as AdsUmpConsentManager

// small noop comment to trigger reindex
// MainActivity integrity check

class MainActivity : BaseActivity() {
    // resume tracking for proper app-open timing
    private var isResumed: Boolean = false
    private var pendingShowOnResume: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 📊 타이밍 진단: MainActivity 진입 시각 기록
        kr.sweetapps.alcoholictimer.ads.AdTimingLogger.logMainActivityCreate()

        super.onCreate(savedInstanceState)

        // 스플래시를 광고가 끝날 때까지 유지하는 상태
        val holdSplashState = androidx.compose.runtime.mutableStateOf(true)

        // AndroidX SplashScreen: 테마 스플래시를 holdSplashState가 true인 동안 유지
        val splash = installSplashScreen()
        // keep on screen while we want to hold the splash (waiting for ad)
        // note: condition called on main thread
        splash.setKeepOnScreenCondition { holdSplashState.value }

        // 🚨 AdMob 정책 준수: 광고가 표시되지 않을 경우에만 타임아웃
        // 광고가 표시 중이면 타임아웃 취소하여 뒤에 앱이 보이지 않도록 함
        var timeoutRunnable: Runnable? = null
        timeoutRunnable = Runnable {
            // 타임아웃 발동 시 AppOpen 광고가 표시 중인지 확인
            val isAppOpenShowing = try {
                kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.isShowingAd()
            } catch (_: Throwable) { false }

            if (isAppOpenShowing) {
                // 광고가 표시 중이면 타임아웃을 연장 (1초 후 다시 확인)
                android.util.Log.d("MainActivity", "splash timeout deferred - AppOpen ad is showing")
                window.decorView.postDelayed(timeoutRunnable!!, 1000)
            } else {
                // 광고가 없으면 Splash 해제
                android.util.Log.d("MainActivity", "splash timeout fired -> releasing holdSplashState")
                holdSplashState.value = false
            }
        }
        window.decorView.postDelayed(timeoutRunnable, 3000) // 초기 타임아웃 3초로 단축

        // Helper to change holdSplashState with logging
        val setHoldSplash: (Boolean) -> Unit = { v ->
            runOnUiThread { android.util.Log.d("MainActivity", "holdSplashState -> $v (thread=${Thread.currentThread().name})"); holdSplashState.value = v
                // When we release the initial splash (v == false), re-enable AppOpen auto-show so
                // subsequent background->foreground transitions may trigger AppOpen ads normally.
                if (!v) {
                    try {
                        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setAutoShowEnabled(true)
                        android.util.Log.d("MainActivity", "auto-show re-enabled after splash release")
                    } catch (_: Throwable) {}
                }
            }
        }

        // Use AppOpenAdManager lifecycle handling for foreground shows.
        // Do not disable auto-show or attempt to show manually here; the AppOpenAdManager handles lifecycle-triggered shows.

        // 정책이 비활성화될 때 스플래시를 즉시 해제할 수 있도록 리스너 등록
        try {
            AdController.addSplashReleaseListener {
                runOnUiThread {
                    android.util.Log.d("MainActivity", "AdController splashReleaseListener invoked -> release splash")
                    setHoldSplash(false)
                }
            }
        } catch (_: Throwable) {}

        // 🚨 AdMob 정책 준수: AppOpen 광고가 닫힐 때만 Splash 해제
        // 광고가 표시되는 동안 뒤에 앱이 보이지 않도록 함
        try {
            kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener {
                runOnUiThread {
                    android.util.Log.d("MainActivity", "AppOpen ad finished -> releasing splash")
                    setHoldSplash(false)
                    // 타임아웃도 취소
                    timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
                }
            }
        } catch (_: Throwable) {}

        // 광고가 실제로 화면에 나타나는 시점에 스플래시를 해제하여 검은 화면 간격을 제거
        // Leave ad shown handling to the AppOpenAdManager; keep only the splash timeout to avoid permanent blocking

        // 강제 라이트 모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        val startDestinationRoute = if (startTime > 0L && !timerCompleted) Screen.Run.route else Screen.Start.route

        val umpConsentManager = (application as MainApplication).umpConsentManager
        umpConsentManager.gatherConsent(this) { canInitializeAds ->
            if (canInitializeAds) {
                // 광고 SDK 초기화 코드
                MobileAds.initialize(this) {}
                InterstitialAdManager.preload(this)
                // Ensure AppOpen preload runs after MobileAds initialization / consent
                try {
                    android.util.Log.d("MainActivity", "Post-initialize: preloading AppOpen via AppOpenAdManager")
                    kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.preload(this)
                } catch (_: Throwable) { android.util.Log.w("MainActivity", "AppOpen preload failed post-initialize") }
                // Ads-side consent proxy: ensure ads-side manager has checked consent and loads if allowed
                try { AdsUmpConsentManager.requestAndLoadIfRequired(this) { _ -> } } catch (_: Throwable) {}
            }
        }

        android.util.Log.d("MainActivity", "Setting content and entering Compose UI")
        setContent { AppContentWithStart(startDestinationRoute, holdSplashState) }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 📊 타이밍 진단: 최종 리포트 출력
        kr.sweetapps.alcoholictimer.ads.AdTimingLogger.printTimingReport()

        // 리스너 해제
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdLoadedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.setOnAdShownListener(null)
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        // If ad was loaded earlier while activity wasn't resumed, try to show now
        if (pendingShowOnResume) {
            android.util.Log.d("MainActivity", "onResume: pendingShowOnResume=true -> attempting show")
            pendingShowOnResume = false
            runCatching {
                if (kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.isLoaded()) {
                    android.util.Log.d("MainActivity", "onResume: ad loaded -> attempting show while keeping splash")
                    val shown = kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.showIfAvailable(this)
                    android.util.Log.d("MainActivity", "onResume: showIfAvailable returned=$shown")
                    if (shown) {
                        window.decorView.post { applySystemBarAppearance() }
                      } else {
                        android.util.Log.d("MainActivity", "onResume: ad not shown -> release splash")
                        // ensure splash isn't stuck
                        runOnUiThread { /* no-op; the show listener will release splash or fallback will handle */ }
                      }
                }
            }
        }

        // 시스템바 appearance 직접 재적용 코드 제거됨 (BaseActivity에서 일괄 적용)
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onStop() {
        super.onStop()

        // 🚀 장기 최적화: AppOpen 광고 프리캐싱
        // 앱이 백그라운드로 갈 때 다음 AppOpen 광고를 미리 로드
        // 효과: 다음 실행 시 광고가 이미 준비되어 즉시 표시 가능
        // 예상 개선: 노출률 70% → 80% (추가 10% 개선)
        try {
            android.util.Log.d("MainActivity", "onStop: preloading next AppOpen ad for future use")
            kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.preload(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "onStop: AppOpen preload failed: ${e.message}")
        }
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

    // repositories & manager
    val emergencyRepo = remember { EmergencyPolicyRepository(context) }
    val updateRepo = remember { UpdatePolicyRepository(context) }
    val noticeRepo = remember { NoticePolicyRepository(context) }
    val policyManager = remember { PopupPolicyManager(emergencyRepo, updateRepo, noticeRepo, context) }

    val showUpdateDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val currentUpdatePolicy = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<UpdatePolicy?>(null) }
    val showNoticeDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val currentNotice = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Announcement?>(null) }
    val showEmergencyDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val currentEmergency = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<EmergencyPolicy?>(null) }

    // decide once after splash hidden
    androidx.compose.runtime.LaunchedEffect(key1 = holdSplashState.value) {
        if (!holdSplashState.value) {
            try {
                val decision = try { policyManager.decidePopup(android.os.Build.VERSION.RELEASE ?: "") } catch (e: Exception) { e.printStackTrace(); PopupDecision.None }
                when (decision) {
                    is PopupDecision.ShowEmergency -> {
                        currentEmergency.value = decision.policy
                        showEmergencyDialog.value = true
                    }
                    is PopupDecision.ShowUpdate -> {
                        val pol = decision.policy
                        currentUpdatePolicy.value = pol
                        showUpdateDialog.value = true
                    }
                    is PopupDecision.ShowNotice -> {
                        val ann = decision.announcement
                        currentNotice.value = ann
                        showNoticeDialog.value = true
                    }
                    else -> { /* none */ }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // main content
    if (!holdSplashState.value) {
        val contentBg = if (startDestination == Screen.Run.route) Color(0xFFEEEDE9) else null
        BaseScaffold(navController = navController, contentBackground = contentBg) {
            AlcoholicTimerNavGraph(navController, startDestination)
        }
    }

    // update dialog
    if (showUpdateDialog.value && currentUpdatePolicy.value != null) {
        val policy = currentUpdatePolicy.value!!
        OptionalUpdateDialog(
            isForce = policy.isForceUpdate,
            title = stringResource(id = R.string.update_dialog_title),
            features = listOf(policy.releaseNotes ?: "업데이트 안내 없음"),
            updateButtonText = stringResource(id = R.string.update_dialog_update),
            laterButtonText = stringResource(id = R.string.update_dialog_later),
            onUpdateClick = {
                val url = policy.downloadUrl ?: "https://play.google.com/store/apps/details?id=${context.packageName}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            onLaterClick = {
                policyManager.dismissUpdate(policy.targetVersionCode)
                showUpdateDialog.value = false
            }
        )
    }

    // notice dialog (use project AnnouncementDialog design)
    if (showNoticeDialog.value && currentNotice.value != null) {
        val ann = currentNotice.value!!
        AnnouncementDialog(
            announcement = ann,
            onDismiss = {
                try {
                    val prefs = context.getSharedPreferences("popup_prefs", android.content.Context.MODE_PRIVATE)
                    val key = "last_notice_version_${context.packageName}"
                    prefs.edit().putInt(key, ann.noticeVersion).apply()
                } catch (_: Throwable) {}
                showNoticeDialog.value = false
            }
        )
    }

    // emergency dialog
    if (showEmergencyDialog.value && currentEmergency.value != null) {
        val em = currentEmergency.value!!
        EmergencyRedirectDialog(
            title = stringResource(id = R.string.emergency_title_default),
            description = em.content,
            newAppPackage = em.appId ?: context.packageName,
            redirectUrl = em.redirectUrl,
            buttonText = em.buttonText?.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.dialog_confirm),
            isDismissible = em.isDismissible,
            onDismiss = { showEmergencyDialog.value = false }
        )
    }
}

@Composable
fun AppContent() { AppContentWithStart(Screen.Start.route) }
