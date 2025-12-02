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
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
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
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.consent.UmpConsentManager as AdsUmpConsentManager

// small noop comment to trigger reindex
// MainActivity integrity check

class MainActivity : BaseActivity() {
    // resume tracking for proper app-open timing
    private var isResumed: Boolean = false
    private var pendingShowOnResume: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // ?�� ?�?�밍 진단: MainActivity 진입 ?�각 기록
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logMainActivityCreate()

        super.onCreate(savedInstanceState)

        // ?�플?�시�?광고가 ?�날 ?�까지 ?��??�는 ?�태
        val holdSplashState = androidx.compose.runtime.mutableStateOf(true)

        // AndroidX SplashScreen: ?�마 ?�플?�시�?holdSplashState가 true???�안 ?��?
        val splash = installSplashScreen()
        // keep on screen while we want to hold the splash (waiting for ad)
        // note: condition called on main thread
        splash.setKeepOnScreenCondition { holdSplashState.value }

        // ?�� AdMob ?�책 준?? 광고가 ?�시?��? ?�을 경우?�만 ?�?�아??
        // 광고가 ?�시 중이�??�?�아??취소?�여 ?�에 ?�이 보이지 ?�도�???
        var timeoutRunnable: Runnable? = null
        timeoutRunnable = Runnable {
            // ?�?�아??발동 ??AppOpen 광고가 ?�시 중인지 ?�인
            val isAppOpenShowing = try {
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isShowingAd()
            } catch (_: Throwable) { false }

            if (isAppOpenShowing) {
                // [수정] 광고가 ?�시 중이�??�?�아?�을 ?�장 (1�????�시 ?�인)
                // AdMob 정책: 광고가 완전히 닫힐 때까지 대기
                android.util.Log.d("MainActivity", "splash timeout deferred - AppOpen ad is showing (AdMob policy)")
                window.decorView.postDelayed(timeoutRunnable!!, 1000)
            } else {
                // [수정] 광고가 표시되지 않음 - 로드 실패 또는 정책에 의해 표시 안 됨
                android.util.Log.d("MainActivity", "splash timeout fired -> releasing holdSplashState (no ad showing)")
                holdSplashState.value = false
            }
        }
        // [수정] 타임아웃을 5초로 증가 (광고 로드 및 표시 시간 고려)
        window.decorView.postDelayed(timeoutRunnable, 5000)

        // Helper to change holdSplashState with logging
        val setHoldSplash: (Boolean) -> Unit = { v ->
            runOnUiThread { android.util.Log.d("MainActivity", "holdSplashState -> $v (thread=${Thread.currentThread().name})"); holdSplashState.value = v
                // When we release the initial splash (v == false), re-enable AppOpen auto-show so
                // subsequent background->foreground transitions may trigger AppOpen ads normally.
                if (!v) {
                    try {
                        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(true)
                        android.util.Log.d("MainActivity", "auto-show re-enabled after splash release")
                    } catch (_: Throwable) {}
                }
            }
        }

        // Use AppOpenAdManager lifecycle handling for foreground shows.
        // Do not disable auto-show or attempt to show manually here; the AppOpenAdManager handles lifecycle-triggered shows.

        // ?�책??비활?�화?????�플?�시�?즉시 ?�제?????�도�?리스???�록
        try {
            AdController.addSplashReleaseListener {
                runOnUiThread {
                    android.util.Log.d("MainActivity", "AdController splashReleaseListener invoked -> release splash")
                    setHoldSplash(false)
                }
            }
        } catch (_: Throwable) {}

        // ?�� AdMob ?�책 준?? AppOpen 광고가 ?�힐 ?�만 Splash ?�제
        // 광고가 ?�시?�는 ?�안 ?�에 ?�이 보이지 ?�도�???
        try {
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener {
                runOnUiThread {
                    android.util.Log.d("MainActivity", "AppOpen ad finished -> releasing splash (AdMob policy)")
                    setHoldSplash(false)
                    // ?�?�아?�도 취소
                    timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
                }
            }
        } catch (_: Throwable) {}

        // [NEW] 광고 로드 실패 시에도 스플래시 해제 (무한 대기 방지)
        try {
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener {
                runOnUiThread {
                    android.util.Log.d("MainActivity", "AppOpen ad load failed -> releasing splash")
                    // 로드 실패 시 즉시 해제하지 않고 타임아웃이 처리하도록 함
                    // (네트워크 지연 등을 고려)
                }
            }
        } catch (_: Throwable) {}

        // 광고가 ?�제�??�면???��??�는 ?�점???�플?�시�??�제?�여 검?� ?�면 간격???�거
        // Leave ad shown handling to the AppOpenAdManager; keep only the splash timeout to avoid permanent blocking

        // 강제 라이트 모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // [NEW] 타이머 상태 확인 및 UI 전환 로직
        checkTimerStateAndSwitchUI()

        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)

        // [NEW] 타이머 상태에 따라 초기 라우트 결정
        val startDestinationRoute = when {
            timerCompleted -> Screen.Finished.route  // 타이머 완료 시
            startTime > 0L -> Screen.Run.route       // 타이머 진행 중
            else -> Screen.Start.route               // 타이머 설정 전
        }

        val umpConsentManager = (application as MainApplication).umpConsentManager
        umpConsentManager.gatherConsent(this) { canInitializeAds ->
            if (canInitializeAds) {
                // 광고 SDK 초기화 코드
                MobileAds.initialize(this) {}
                InterstitialAdManager.preload(this)

                // [수정] 콜드 스타트 시에도 광고 표시 - 앱 최초 실행 시 노출수 0 문제 해결
                try {
                    android.util.Log.d("MainActivity", "Post-initialize: setting up AppOpen ad for cold start")

                    // 광고 로드 완료 리스너 설정 (콜드 스타트 시 즉시 표시)
                    kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener {
                        runOnUiThread {
                            android.util.Log.d("MainActivity", "AppOpen ad loaded (cold start) -> attempting to show immediately")

                            // 광고가 로드되면 즉시 표시 시도 (콜드 스타트 = 앱 최초 실행)
                            if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded()) {
                                val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(
                                    this,
                                    bypassRecentFullscreenSuppression = true  // 첫 실행이므로 억제 무시
                                )
                                android.util.Log.d("MainActivity", "AppOpen ad show result (cold start): $shown")

                                if (!shown) {
                                    // 광고 표시 실패 시 스플래시 해제 (무한 대기 방지)
                                    android.util.Log.w("MainActivity", "AppOpen ad failed to show (cold start) -> releasing splash")
                                    setHoldSplash(false)
                                }
                            }
                        }
                    }

                    // 광고 로드 시작
                    android.util.Log.d("MainActivity", "Starting AppOpen ad preload for cold start")
                    kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.preload(this)
                } catch (_: Throwable) {
                    android.util.Log.w("MainActivity", "AppOpen setup failed (cold start) -> releasing splash")
                    setHoldSplash(false)  // 오류 시 스플래시 해제
                }
            } else {
                // 동의 없으면 스플래시 즉시 해제
                android.util.Log.d("MainActivity", "User did not consent to ads -> releasing splash")
                setHoldSplash(false)
            }
        }

        android.util.Log.d("MainActivity", "Setting content and entering Compose UI")
        setContent { AppContentWithStart(startDestinationRoute, holdSplashState) }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 앱 타이밍 진단: 최종 리포트 출력
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.printTimingReport()

        // 리스너 제거
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener(null)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener(null)
    }

    // [NEW] 타이머 상태 확인 및 UI 전환 함수
    private fun checkTimerStateAndSwitchUI() {
        try {
            val isFinished = kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.isTimerFinished()
            val isActive = kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.isTimerActive()

            android.util.Log.d("MainActivity", "타이머 상태 확인: isFinished=$isFinished, isActive=$isActive")

            when {
                isFinished -> {
                    // 타이머 만료 상태
                    showFinishedTimerUI()
                }
                isActive -> {
                    // 타이머 작동 중
                    showActiveTimerUI()
                }
                else -> {
                    // 타이머 설정 전 (초기 상태)
                    showInitialSetupUI()
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "타이머 상태 확인 실패", t)
            showInitialSetupUI() // 기본값으로 초기 설정 UI 표시
        }
    }

    // [NEW] 타이머 설정 전 초기 UI 표시
    private fun showInitialSetupUI() {
        android.util.Log.d("MainActivity", "타이머 설정 전 초기 UI 표시: 시작 버튼 활성화")
        // 실제 UI 변경은 Compose에서 상태에 따라 자동으로 처리됨
    }

    // [NEW] 타이머 작동 중 UI 표시
    private fun showActiveTimerUI() {
        android.util.Log.d("MainActivity", "타이머 작동 중 UI 표시: 남은 시간 및 정보 표시")
        // 실제 UI 변경은 Compose에서 상태에 따라 자동으로 처리됨
    }

    // [NEW] 타이머 만료 UI 표시
    private fun showFinishedTimerUI() {
        android.util.Log.d("MainActivity", "타이머 만료 UI 표시: 결과 확인/새 시작 버튼 활성화")
        // 실제 UI 변경은 Compose에서 상태에 따라 자동으로 처리됨
    }

    // [NEW] 타이머 만료 시뮬레이션 (테스트용)
    @Suppress("unused")
    private fun simulateTimerExpiration() {
        android.util.Log.d("MainActivity", "타이머 만료 시뮬레이션 실행")
        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerFinished(true)
    }

    // [NEW] 타이머 리셋 (새 타이머 시작 시)
    @Suppress("unused")
    private fun resetTimer() {
        android.util.Log.d("MainActivity", "타이머 리셋 실행")
        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.resetTimer()
        kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository.setTimerActive(true) // 새 타이머 시작
    }

    // [NEW] 결과 확인 및 기록 (전면 광고 연동)
    @Suppress("unused")
    private fun showResultAndRecord() {
        android.util.Log.d("MainActivity", "결과 확인 버튼 클릭 -> 전면 광고 표시 시도")

        if (kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.isLoaded()) {
            kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager.show(this) { success ->
                if (success) {
                    android.util.Log.d("MainActivity", "광고 종료 -> 결과 기록 화면으로 이동")
                } else {
                    android.util.Log.d("MainActivity", "광고 실패 -> 결과 기록 화면으로 이동")
                }
                // 실제 결과 화면 이동 로직은 여기에 추가
            }
        } else {
            android.util.Log.d("MainActivity", "광고 없음 -> 즉시 결과 기록 화면으로 이동")
            // 실제 결과 화면 이동 로직은 여기에 추가
        }
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        // If ad was loaded earlier while activity wasn't resumed, try to show now
        if (pendingShowOnResume) {
            android.util.Log.d("MainActivity", "onResume: pendingShowOnResume=true -> attempting show")
            pendingShowOnResume = false
            runCatching {
                if (kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded()) {
                    android.util.Log.d("MainActivity", "onResume: ad loaded -> attempting show while keeping splash")
                    val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(this)
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

        // ?�스?�바 appearance 직접 ?�적??코드 ?�거??(BaseActivity?�서 ?�괄 ?�용)
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onStop() {
        super.onStop()

        // ?? ?�기 최적?? AppOpen 광고 ?�리캐싱
        // ?�이 백그?�운?�로 �????�음 AppOpen 광고�?미리 로드
        // ?�과: ?�음 ?�행 ??광고가 ?��? 준비되??즉시 ?�시 가??
        // ?�상 개선: ?�출�?70% ??80% (추�? 10% 개선)
        try {
            android.util.Log.d("MainActivity", "onStop: preloading next AppOpen ad for future use")
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.preload(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "onStop: AppOpen preload failed: ${e.message}")
        }
    }

    // BaseActivity??추상 ?�수 구현
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
            features = listOf(policy.releaseNotes ?: "?�데?�트 ?�내 ?�음"),
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
