package kr.sweetapps.alcoholictimer.ui.main

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.google.android.gms.ads.MobileAds
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.common.BaseScaffold
// Navigation imports (now in ui.main package)
// Note: Screen and AppNavHost are now in the same package
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.data.supabase.repository.EmergencyPolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.NoticePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.repository.PopupPolicyManager
import kr.sweetapps.alcoholictimer.data.supabase.repository.UpdatePolicyRepository
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy
import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement
import kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy
import kr.sweetapps.alcoholictimer.ui.dialogs.OptionalUpdateDialog
import kr.sweetapps.alcoholictimer.ui.dialogs.AnnouncementDialog
import kr.sweetapps.alcoholictimer.ui.dialogs.EmergencyRedirectDialog
import kr.sweetapps.alcoholictimer.consent.UmpConsentManager as AdsUmpConsentManager

// small noop comment to trigger reindex
// MainActivity integrity check

class MainActivity : BaseActivity() {
    // resume tracking for proper app-open timing
    private var isResumed: Boolean = false
    private var pendingShowOnResume: Boolean = false

    // [NEW] 메인 진입 플래그 - 중복 호출 방지
    @Volatile
    private var hasProceededToMain: Boolean = false

    // [NEW] 광고 로드 리스너 실행 플래그 - 무한 중첩 방지
    @Volatile
    private var hasHandledInitialAdLoad: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 타이밍 진단: MainActivity 진입 시각 기록
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logMainActivityCreate()

        super.onCreate(savedInstanceState)

        // [NEW] Firebase Remote Config 즉시 fetch (Debug에서는 캐시 없이 즉시 업데이트)
        try {
            kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.fetchRemoteConfig(this) { success ->
                android.util.Log.d("MainActivity", "Remote Config fetch completed: success=$success")
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Remote Config fetch failed", t)
        }

        // [중요] 앱 오프닝 광고 자동 표시 비활성화 (수동으로 제어하여 중첩 방지)
        try {
            kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(false)
            android.util.Log.d("MainActivity", "AppOpen auto-show DISABLED - preventing duplicate show")
        } catch (_: Throwable) {}

        // ============================================================
        // 스플래시 화면 설정 (AndroidX SplashScreen)
        // ============================================================
        val holdSplashState = androidx.compose.runtime.mutableStateOf(true)
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { holdSplashState.value }
        android.util.Log.d("MainActivity", "========================================")
        android.util.Log.d("MainActivity", "SplashScreen installed - holdSplashState=true")
        android.util.Log.d("MainActivity", "========================================")

        // 타이머 상태 확인 (초기 라우트 결정용)
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        val startDestinationRoute = when {
            timerCompleted -> Screen.Finished.route
            startTime > 0L -> Screen.Run.route
            else -> Screen.Start.route
        }

        // 강제 라이트 모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 타이머 상태 확인 및 UI 전환 로직
        checkTimerStateAndSwitchUI()

        // ============================================================
        // 4단계: 메인 액티비티 진입 함수 (광고 완료 또는 실패 시만 호출)
        // ============================================================
        val proceedToMainActivity: () -> Unit = proceedToMain@{
            // [중요] 중복 호출 방지
            if (hasProceededToMain) {
                android.util.Log.w("MainActivity", "proceedToMainActivity already called - skipping duplicate call")
                return@proceedToMain
            }
            hasProceededToMain = true

            runOnUiThread {
                android.util.Log.d("MainActivity", "========================================")
                android.util.Log.d("MainActivity", "단계 4: 메인 액티비티 진입")
                android.util.Log.d("MainActivity", "호출 스택 추적: ${Thread.currentThread().stackTrace.take(5).joinToString()}")
                android.util.Log.d("MainActivity", "========================================")

                // [FIX] 스플래시 화면 해제 전에 테마 변경 (검은색 배경 문제 해결)
                try {
                    setTheme(R.style.Theme_AlcoholicTimer)
                    android.util.Log.d("MainActivity", "Theme changed to Theme.AlcoholicTimer")
                } catch (t: Throwable) {
                    android.util.Log.e("MainActivity", "Failed to change theme", t)
                }

                holdSplashState.value = false
                android.util.Log.d("MainActivity", "Splash released - entering Compose UI")

                // AppOpen auto-show 재활성화 (백그라운드 복귀 시 자동 표시)
                try {
                    kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(true)
                    android.util.Log.d("MainActivity", "AppOpen auto-show re-enabled")
                } catch (_: Throwable) {}

                setContent { AppContentWithStart(startDestinationRoute, holdSplashState) }
            }
        }

        // ============================================================
        // 안전 타임아웃 (4초) - 광고 로딩 중이면 무한 연장
        // ============================================================
        var isUmpConsentCompleted = false  // UMP 동의 확인 완료 플래그
        var timeoutRunnable: Runnable? = null
        timeoutRunnable = Runnable {
            val isAppOpenShowing = try {
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isShowingAd()
            } catch (_: Throwable) { false }
            val isAppOpenLoading = try {
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoading()
            } catch (_: Throwable) { false }

            if (!isUmpConsentCompleted) {
                // [NEW] UMP 동의 확인 중 - 타임아웃 연장
                android.util.Log.d("MainActivity", "Timeout deferred - UMP consent in progress")
                window.decorView.postDelayed(timeoutRunnable!!, 1000)
            } else if (isAppOpenShowing) {
                android.util.Log.d("MainActivity", "Timeout deferred - AppOpen ad is showing")
                window.decorView.postDelayed(timeoutRunnable!!, 1000)
            } else if (isAppOpenLoading) {
                android.util.Log.d("MainActivity", "Timeout deferred - AppOpen ad is loading")
                window.decorView.postDelayed(timeoutRunnable!!, 1000)
            } else {
                android.util.Log.w("MainActivity", "Timeout fired (4s) - no ad showing/loading -> proceed to main")
                proceedToMainActivity()
            }
        }
        window.decorView.postDelayed(timeoutRunnable, 4000)

        // ============================================================
        // 광고 리스너 설정 (광고 로드 전에 미리 설정 - 중요!)
        // ============================================================
        // 광고 로드 완료 리스너 설정
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener {
            runOnUiThread {
                // [중요] 첫 광고 로드 시에만 실행 - 무한 중첩 방지
                if (hasHandledInitialAdLoad) {
                    android.util.Log.d("MainActivity", "광고 로드 완료 (이미 처리됨) - 스킵 (무한 중첩 방지)")
                    return@runOnUiThread
                }
                hasHandledInitialAdLoad = true

                android.util.Log.d("MainActivity", "========================================")
                android.util.Log.d("MainActivity", "단계 3: 광고 로드 완료 -> 광고 표시 시도")
                android.util.Log.d("MainActivity", "========================================")

                if (!kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.isLoaded()) {
                    android.util.Log.w("MainActivity", "Ad not loaded -> proceed to main")
                    proceedToMainActivity()
                    return@runOnUiThread
                }

                // ============================================================
                // 3단계: 광고 표시 (Sequential Step 3)
                // ============================================================
                val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(
                    this,
                    bypassRecentFullscreenSuppression = true
                )
                android.util.Log.d("MainActivity", "광고 표시 시도 결과: shown=$shown")

                if (!shown) {
                    // 광고 표시 실패 - 메인으로 이동
                    android.util.Log.w("MainActivity", "Ad failed to show -> proceed to main")
                    proceedToMainActivity()
                }
                // 광고 표시 성공 시 - onAdDismissedFullScreenContent에서 메인으로 이동
            }
        }

        // 광고 로드 실패 리스너 설정
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener {
            runOnUiThread {
                android.util.Log.w("MainActivity", "AppOpen ad load failed -> proceed to main")
                proceedToMainActivity()
            }
        }

        // 광고 표시 완료 리스너 설정 (사용자가 광고를 닫았을 때)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener {
            runOnUiThread {
                android.util.Log.d("MainActivity", "AppOpen ad dismissed by user -> proceed to main")
                // 타임아웃 취소
                timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
                proceedToMainActivity()
            }
        }

        // 광고 표시 시작 리스너 설정 (타임아웃 취소용)
        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener {
            runOnUiThread {
                android.util.Log.d("MainActivity", "AppOpen ad shown -> canceling timeout")
                timeoutRunnable?.let { window.decorView.removeCallbacks(it) }
            }
        }

        // ============================================================
        // 1단계: UMP 동의 확인 시작 (Sequential Step 1)
        // ============================================================
        android.util.Log.d("MainActivity", "========================================")
        android.util.Log.d("MainActivity", "단계 1: UMP 동의 확인 시작")
        android.util.Log.d("MainActivity", "========================================")

        val umpConsentManager = (application as MainApplication).umpConsentManager
        umpConsentManager.gatherConsent(this) { canInitializeAds ->
            // [중요] UMP 동의 확인 완료 표시
            isUmpConsentCompleted = true
            android.util.Log.d("MainActivity", "단계 1 완료: UMP 동의 확인 결과 = $canInitializeAds")

            if (!canInitializeAds) {
                // 동의 없음 - 즉시 메인으로 이동
                android.util.Log.w("MainActivity", "User did not consent to ads -> skip ads, proceed to main")
                proceedToMainActivity()
                return@gatherConsent
            }

            // ============================================================
            // 2단계: 광고 SDK 초기화 및 광고 로드 (Sequential Step 2)
            // ============================================================
            android.util.Log.d("MainActivity", "========================================")
            android.util.Log.d("MainActivity", "단계 2: 광고 SDK 초기화 및 광고 로드")
            android.util.Log.d("MainActivity", "========================================")

            try {
                // 광고 SDK 초기화
                MobileAds.initialize(this) {
                    android.util.Log.d("MainActivity", "MobileAds initialized successfully")
                }
                InterstitialAdManager.preload(this)

                // 광고 로드 시작 (리스너는 이미 설정됨)
                android.util.Log.d("MainActivity", "Starting AppOpen ad preload...")
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.preload(this)

            } catch (t: Throwable) {
                android.util.Log.e("MainActivity", "Error during ad setup", t)
                proceedToMainActivity()
            }
        }
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
            AppNavHost(navController, startDestination)
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
