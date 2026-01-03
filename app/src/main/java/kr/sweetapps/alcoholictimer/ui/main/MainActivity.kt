package kr.sweetapps.alcoholictimer.ui.main

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import androidx.compose.ui.res.stringResource
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
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
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.CommunityViewModel
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel

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

    // [NEW] 딥링크 처리를 위한 변수 (2025-12-31)
    private var deepLinkScreenRoute: String? = null
    private var deepLinkNotificationId: Int = 0
    private var deepLinkGroupType: String? = null
    private var deepLinkShowBadgeAnimation: Boolean = false

    // [NEW] 초기화 완료 상태 (2025-12-31)
    // UMP Consent + 알림 권한 + Session Start 완료 시 true로 변경
    // internal로 선언하여 Composable 함수에서 접근 가능하도록 함
    internal val isInitializationComplete = androidx.compose.runtime.mutableStateOf(false)

    // [NEW] Pre-Permission 다이얼로그 표시 상태 (2025-12-31)
    internal val showPermissionDialog = androidx.compose.runtime.mutableStateOf(false)
    private var permissionDialogOnComplete: (() -> Unit)? = null

    // [NEW] 알림 권한 요청 ActivityResultLauncher (2025-12-31)
    // onCreate() 이전에 초기화되어야 하므로 lazy 사용
    // internal로 선언하여 Composable 함수에서 접근 가능하도록 함
    internal val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한 허용됨
            android.util.Log.d("MainActivity", "✅ Notification permission GRANTED")
            kr.sweetapps.alcoholictimer.util.manager.RetentionPreferenceManager.setNotificationPermissionShown(this, true)

            // [NEW] Firebase Analytics 이벤트 전송 (2025-12-31)
            try {
                kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logSettingsChange(
                    settingType = "notification_permission",
                    oldValue = "denied",
                    newValue = "granted"
                )
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to log settings_change", e)
            }
        } else {
            // 권한 거부됨
            android.util.Log.d("MainActivity", "❌ Notification permission DENIED")

            // [NEW] Firebase Analytics 이벤트 전송 (2025-12-31)
            try {
                kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logSettingsChange(
                    settingType = "notification_permission",
                    oldValue = null,
                    newValue = "denied"
                )
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to log settings_change", e)
            }
        }
    }

    /**
     * [NEW] 앱이 이미 실행 중일 때 알림 클릭 처리 (2025-12-31)
     * [UPDATED] 초기화 완료 상태에 따라 대기/즉시 실행 분기 (2025-12-31)
     *
     * 백그라운드나 포그라운드 상태에서 알림을 클릭하면 이 메서드가 호출됨
     * 딥링크가 정상 작동하도록 Intent를 다시 처리
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "📥 onNewIntent called - App already running")

        // 새 Intent를 Activity의 Intent로 설정
        setIntent(intent)

        // 딥링크 처리 (정보 저장 + Analytics)
        handleDeepLinkIntent(intent)

        // [NEW] 초기화 완료 상태에 따른 분기 처리 (2025-12-31)
        val isInitComplete = isInitializationComplete.value
        android.util.Log.d("MainActivity", "🔍 onNewIntent - isInitializationComplete=$isInitComplete")

        if (isInitComplete) {
            // 초기화가 이미 완료된 상태 - NavController도 준비되어 있으므로 안전
            android.util.Log.d("MainActivity", "✅ Initialization already complete - deep link will execute via LaunchedEffect")
            // LaunchedEffect가 deepLinkScreenRoute 변경을 감지하여 자동 실행됨
        } else {
            // 초기화가 아직 진행 중 - 대기 필요
            android.util.Log.d("MainActivity", "⏳ Initialization in progress - deep link will wait")
            android.util.Log.d("MainActivity", "⏳ Navigation will execute after user completes permission dialog")
            // sendSessionStartEvent()에서 isInitializationComplete = true로 변경되면
            // LaunchedEffect가 감지하여 자동으로 딥링크 실행됨
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // [NEW] 앱 시작 시각 기록 (최소 브랜딩 시간 계산용)
        val appStartTimeMs = System.currentTimeMillis()

        // [DEBUG] 초기화 상태 초기값 확인 (2025-12-31)
        android.util.Log.d("MainActivity", "🔵 onCreate START - isInitializationComplete initial value: ${isInitializationComplete.value}")
        android.util.Log.d("MainActivity", "🔵 Deep link navigation is currently BLOCKED until initialization completes")

        // [DEBUG] 로케일 진단 로그 추가 (한국어 리소스 로드 문제 디버깅용)
        try {
            val systemLocale = java.util.Locale.getDefault().language
            @Suppress("DEPRECATION")
            val appResourceLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                resources.configuration.locales[0]?.language ?: "unknown"
            } else {
                resources.configuration.locale?.language ?: "unknown"
            }
            @Suppress("DEPRECATION")
            val allLocales = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                (0 until resources.configuration.locales.size()).joinToString(", ") {
                    resources.configuration.locales[it]?.toString() ?: "null"
                }
            } else {
                resources.configuration.locale?.toString() ?: "unknown"
            }
            android.util.Log.d("LocaleCheck", "========================================")
            android.util.Log.d("LocaleCheck", "System Locale: $systemLocale")
            android.util.Log.d("LocaleCheck", "App Resource Locale: $appResourceLocale")
            android.util.Log.d("LocaleCheck", "All App Locales: [$allLocales]")
            android.util.Log.d("LocaleCheck", "----------------------------------------")
            android.util.Log.d("LocaleCheck", "[Common] app_name: ${getString(R.string.app_name)}")
            android.util.Log.d("LocaleCheck", "[Tab Menu] drawer_menu_more: ${getString(R.string.drawer_menu_more)}")
            android.util.Log.d("LocaleCheck", "[Tab Menu] drawer_menu_sobriety: ${getString(R.string.drawer_menu_sobriety)}")
            android.util.Log.d("LocaleCheck", "[Tab Menu] drawer_menu_records: ${getString(R.string.drawer_menu_records)}")
            android.util.Log.d("LocaleCheck", "[Screen] run_title: ${getString(R.string.run_title)}")
            android.util.Log.d("LocaleCheck", "[Screen] records_title: ${getString(R.string.records_title)}")
            android.util.Log.d("LocaleCheck", "[Button] dialog_confirm: ${getString(R.string.dialog_confirm)}")
            android.util.Log.d("LocaleCheck", "[Button] dialog_cancel: ${getString(R.string.dialog_cancel)}")
            android.util.Log.d("LocaleCheck", "[Profile] profile_edit_title: ${getString(R.string.profile_edit_title)}")
            android.util.Log.d("LocaleCheck", "[Diary] diary_write_title: ${getString(R.string.diary_write_title)}")
            android.util.Log.d("LocaleCheck", "========================================")
        } catch (e: Exception) {
            android.util.Log.e("LocaleCheck", "Failed to log locale info", e)
        }

        // 타이밍 진단: MainActivity 진입 시각 기록
        kr.sweetapps.alcoholictimer.ui.ad.AdTimingLogger.logMainActivityCreate()

        super.onCreate(savedInstanceState)

        // [NEW] SplashScreen에서 광고를 이미 처리했는지 확인
        val isSplashAdShown = intent.getBooleanExtra("is_splash_ad_shown", false)
        if (isSplashAdShown) {
            android.util.Log.d("MainActivity", "⏭️ Splash already handled ad - skipping ad flow")
            // 스플래시 화면 설정만 하고 광고 없이 바로 메인으로 진입
            val holdSplashState = androidx.compose.runtime.mutableStateOf(false) // 즉시 해제
            setTheme(R.style.Theme_AlcoholicTimer)
            setContent {
                val startDestination = when {
                    getSharedPreferences("user_settings", MODE_PRIVATE).getBoolean("timer_completed", false) -> Screen.Success.route
                    getSharedPreferences("user_settings", MODE_PRIVATE).getLong("start_time", 0L) > 0L -> Screen.Run.route
                    else -> Screen.Start.route
                }
                AppContentWithStart(startDestination, holdSplashState)
            }
            return
        }

        // [NEW] Firebase Remote Config 즉시 fetch (Debug에서는 캐시 없이 즉시 업데이트)
        try {
            kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.fetchRemoteConfig(this) { success ->
                android.util.Log.d("MainActivity", "Remote Config fetch completed: success=$success")

                // [테스트용] Fetch 성공 시 값을 로그로 확인 (AdPolicyManager 활용)
                if (success) {
                    val interval = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.getInterstitialIntervalSeconds(this)
                    val isEnabled = kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager.isAdEnabled(this)

                    // 릴리즈 빌드에서 이 로그가 보이면 성공입니다!
                    android.util.Log.d("RemoteConfig_Test", "🔥 [확인] 쿨타임: $interval / 광고ON: $isEnabled")
                }
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
        // [FIX v7] Deadlock 해결: 초기화 조건 제거 (2026-01-03)
        // ============================================================
        val holdSplashState = androidx.compose.runtime.mutableStateOf(true)
        val splash = installSplashScreen()

        // [FIX] Deadlock 해결: 초기화 여부(!isInitializationComplete)는 스플래시 유지 조건에서 제거합니다.
        // 광고 처리가 끝났다면(holdSplashState가 false라면) 스플래시를 걷어내야,
        // 그 뒤에 있는 '알림 권한 다이얼로그'가 사용자에게 보일 수 있습니다.
        splash.setKeepOnScreenCondition {
            // 오직 광고/로딩 대기 상태(holdSplashState)만 확인합니다.
            holdSplashState.value
        }
        android.util.Log.d("MainActivity", "========================================")
        android.util.Log.d("MainActivity", "✅ SplashScreen installed - holdSplash=true")
        android.util.Log.d("MainActivity", "🔓 Splash will release when holdSplashState=false")
        android.util.Log.d("MainActivity", "🛡️ MainActivityContent will show blank screen until isInitComplete=true")
        android.util.Log.d("MainActivity", "========================================")

        // 타이머 상태 확인 (초기 라우트 결정용)
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        val startDestinationRoute = when {
            timerCompleted -> Screen.Success.route
            startTime > 0L -> Screen.Run.route
            else -> Screen.Start.route
        }

        // 강제 라이트 모드 설정
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // [NEW] 알림 채널 생성 (2025-12-31)
        kr.sweetapps.alcoholictimer.util.notification.NotificationChannelManager.createNotificationChannels(this)

        // [NEW] 딥링크 Intent 처리 (2025-12-31)
        handleDeepLinkIntent(intent)

        // [REMOVED] 알림 권한 체크를 UMP 완료 후로 이동 (2025-12-31)
        // 이유: UMP Consent 팝업과 겹치지 않도록 순차 실행

        // [REMOVED] Session Start 이벤트도 모든 초기화 완료 후로 이동 (2025-12-31)

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

            // [FIX] 앱 진입 시 모든 광고 리스너 해제 (뒤늦은 광고 표시 방지)
            try {
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadedListener(null)
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdLoadFailedListener(null)
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdFinishedListener(null)
                kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setOnAdShownListener(null)
            } catch (_: Throwable) {}

            runOnUiThread {
                android.util.Log.d("MainActivity", "========================================")
                android.util.Log.d("MainActivity", "단계 4: 메인 액티비티 진입 (Ad listeners cleared)")
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

                // [FIX] AppOpen auto-show 재활성화 지연 (2초) - 첫 광고 종료 직후 재진입 방지
                window.decorView.postDelayed({
                    try {
                        kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.setAutoShowEnabled(true)
                        android.util.Log.d("MainActivity", "AppOpen auto-show re-enabled (Delayed 2s)")
                    } catch (_: Throwable) {}
                }, 2000)

                // [UPDATED] 조건부 렌더링 setContent (2025-12-31)
                // isInitializationComplete가 true일 때만 AppNavHost 렌더링
                setContent {
                    MainActivityContent(
                        startDestinationRoute = startDestinationRoute,
                        holdSplashState = holdSplashState,
                        activity = this@MainActivity
                    )
                }
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
                // [FIX] Late Show Prevention - 이미 메인으로 진입했다면 늦게 온 광고는 무시 (2025-12-24)
                if (hasProceededToMain) {
                    android.util.Log.w("MainActivity", "⚠️ 광고 로드 완료 (Late Load) -> 이미 메인 진입 상태이므로 표시 차단")
                    return@runOnUiThread
                }

                // [NEW] 초기화 완료 가드 - 권한 팝업 중 광고 차단 (2025-12-31)
                if (!isInitializationComplete.value) {
                    android.util.Log.d("AdGuard", "🛑 초기화 중이라 광고 표시 차단됨 (onAdLoaded)")
                    android.util.Log.d("AdGuard", "🛑 권한 팝업이 완료되기 전까지 광고를 보여주지 않습니다")
                    android.util.Log.d("MainActivity", "⚠️ 초기화 미완료 -> 메인 진입")
                    proceedToMainActivity()
                    return@runOnUiThread
                }

                android.util.Log.d("MainActivity", "✅ 광고 로드 완료 -> 광고 표시 시도")

                // 광고 표시 시도
                val shown = kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager.showIfAvailable(
                    this,
                    bypassRecentFullscreenSuppression = true
                )

                if (shown) {
                    android.util.Log.d("MainActivity", "📺 광고 표시 성공")
                } else {
                    android.util.Log.w("MainActivity", "⚠️ 광고 표시 실패 -> 메인 진입")
                    proceedToMainActivity()
                }
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

            // [FIX v5] 딜레이 제거 - 즉시 알림 권한 체크
            // 이유: UmpConsentManager에서 이미 runOnUiThread로 UI 스레드 보장됨
            android.util.Log.d("MainActivity", "🔔 알림 권한 체크 시작")
            checkAndRequestNotificationPermission {
                // 알림 권한 처리 완료 후 Session Start 이벤트 전송
                android.util.Log.d("MainActivity", "🎯 모든 초기화 완료 - Session Start 이벤트 전송")
                sendSessionStartEvent()
            }

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
                // [NEW] 테스트 기기 설정 (MobileAds.initialize 전에 실행)
                val testDeviceId = try {
                    kr.sweetapps.alcoholictimer.BuildConfig.ADMOB_TEST_DEVICE_ID
                } catch (_: Throwable) { "" }

                if (testDeviceId.isNotBlank()) {
                    try {
                        val requestConfiguration = com.google.android.gms.ads.RequestConfiguration.Builder()
                            .setTestDeviceIds(listOf(testDeviceId))
                            .build()
                        MobileAds.setRequestConfiguration(requestConfiguration)
                        android.util.Log.d("MainActivity", "✅ 테스트 기기 설정 완료: $testDeviceId")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "테스트 기기 설정 실패", e)
                    }
                } else {
                    android.util.Log.d("MainActivity", "테스트 기기 ID 없음 - 일반 모드로 실행")
                }

                // 광고 SDK 초기화
                MobileAds.initialize(this) {
                    android.util.Log.d("MainActivity", "MobileAds initialized successfully")
                }
                // [NEW] 전면광고 제거 결정에 따라 Interstitial 사전 로드 비활성화
                // InterstitialAdManager.preload(this)

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

        // [NEW] 네이티브 광고 캐시 정리 - 메모리 누수 방지 (2025-12-31)
        try {
            kr.sweetapps.alcoholictimer.ui.ad.NativeAdManager.destroyAllAds()
            android.util.Log.d("MainActivity", "Native ad cache cleared")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to clear native ad cache", e)
        }

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

        if (InterstitialAdManager.isLoaded()) {
            InterstitialAdManager.show(this) { success ->
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
                // [NEW] 초기화 완료 가드 - 권한 팝업 중 광고 차단 (2025-12-31)
                if (!isInitializationComplete.value) {
                    android.util.Log.d("AdGuard", "🛑 초기화 중이라 광고 표시 차단됨 (onResume)")
                    android.util.Log.d("AdGuard", "🛑 권한 팝업이 완료되기 전까지 광고를 보여주지 않습니다")
                    return@runCatching
                }

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

        // [REMOVED] 광고 preload 제거 - 광고 표시 중 onStop 호출 시 무한 반복 방지
        // 이유: 앱 오프닝 광고가 뜰 때도 onStop이 호출되어 새 광고를 로드하면,
        // 광고 닫고 돌아올 때 또 광고가 뜨는 무한 루프 발생
    }

    // BaseActivity??추상 ?�수 구현
    @Deprecated("Overrides deprecated API from BaseActivity")
    override fun getScreenTitle(): String = getString(R.string.app_name)

    /**
     * [NEW] 알림 권한 체크 및 Pre-Permission 다이얼로그 표시 (2025-12-31)
     * [UPDATED] 상태 기반으로 변경 - setContent 덮어쓰지 않음 (2025-12-31)
     *
     * @param onComplete 권한 처리 완료 후 호출될 콜백 (Session Start 전송 등)
     */
    private fun checkAndRequestNotificationPermission(onComplete: () -> Unit = {}) {
        val permissionManager = kr.sweetapps.alcoholictimer.util.manager.NotificationPermissionManager
        val retentionPrefs = kr.sweetapps.alcoholictimer.util.manager.RetentionPreferenceManager

        // 권한이 필요하고, 아직 요청하지 않았다면
        if (permissionManager.shouldRequestPermission(this) &&
            !retentionPrefs.isNotificationPermissionShown(this)) {

            android.util.Log.d("MainActivity", "🔔 Notification permission needed - will show Pre-Permission dialog")

            // [UPDATED] 다이얼로그 표시 상태 변경 (2025-12-31)
            permissionDialogOnComplete = onComplete
            showPermissionDialog.value = true

        } else {
            android.util.Log.d("MainActivity", "Notification permission already granted or shown - skipping dialog")

            // [NEW] 다이얼로그가 표시되지 않는 경우에도 즉시 완료 콜백 호출 (2025-12-31)
            onComplete()
        }
    }

    /**
     * [NEW] Pre-Permission 다이얼로그 확인 버튼 처리 (2025-12-31)
     */
    internal fun handlePermissionDialogConfirm() {
        android.util.Log.d("MainActivity", "✅ User confirmed - requesting system permission")

        // 시스템 권한 팝업 요청
        val permissionManager = kr.sweetapps.alcoholictimer.util.manager.NotificationPermissionManager
        permissionManager.requestPermission(requestPermissionLauncher)

        // 다이얼로그 닫기
        showPermissionDialog.value = false

        // [NEW] 완료 콜백 호출 (2025-12-31)
        permissionDialogOnComplete?.invoke()
        permissionDialogOnComplete = null
    }

    /**
     * [NEW] Pre-Permission 다이얼로그 닫기/나중에 버튼 처리 (2025-12-31)
     */
    internal fun handlePermissionDialogDismiss() {
        android.util.Log.d("MainActivity", "⏭️ User dismissed permission dialog")

        // 다이얼로그 닫기
        showPermissionDialog.value = false

        // [NEW] 완료 콜백 호출 (2025-12-31)
        permissionDialogOnComplete?.invoke()
        permissionDialogOnComplete = null
    }

    /**
     * [NEW] Session Start Analytics 이벤트 전송 (2025-12-31)
     * [UPDATED] User Property 설정을 session_start보다 먼저 실행 (2025-12-31)
     * UMP → 알림 권한 처리 완료 후 마지막에 호출
     */
    private fun sendSessionStartEvent() {
        try {
            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            val installTime = sharedPref.getLong("install_time", 0L)

            // 첫 실행이면 설치 시각 저장
            if (installTime == 0L) {
                sharedPref.edit().putLong("install_time", System.currentTimeMillis()).apply()
            }

            val daysSinceInstall = if (installTime > 0) {
                ((System.currentTimeMillis() - installTime) / (24 * 60 * 60 * 1000)).toInt()
            } else {
                0
            }

            val startTime = sharedPref.getLong("start_time", 0L)
            val timerCompleted = sharedPref.getBoolean("timer_completed", false)
            val timerStatus = when {
                timerCompleted -> "completed"
                startTime > 0L -> "active"
                else -> "idle"
            }

            // ============================================================
            // STEP 1: 사용자 그룹 확인 (retention_group 결정)
            // ============================================================
            val retentionPrefs = kr.sweetapps.alcoholictimer.util.manager.RetentionPreferenceManager
            val isTimerRunning = retentionPrefs.isTimerRunning(this)
            val retryCount = retentionPrefs.getRetryCount(this)

            val groupName = when {
                !isTimerRunning && retryCount == 0 -> "group_a_new_user"
                isTimerRunning -> "group_b_active_user"
                !isTimerRunning && retryCount > 0 -> "group_c_resting_user"
                else -> "group_unknown"
            }

            // ============================================================
            // STEP 2: User Property 설정 (session_start보다 먼저!)
            // ============================================================
            android.util.Log.d("MainActivity", "📊 STEP 2: Setting User Property BEFORE session_start")
            kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.setUserProperty("retention_group", groupName)
            android.util.Log.d("AnalyticsCheck", "👤 User Property SET: retention_group = $groupName")

            // ============================================================
            // STEP 3: session_start 이벤트 전송
            // ============================================================
            android.util.Log.d("MainActivity", "📊 STEP 3: Sending session_start event")
            kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logSessionStart(
                isFirstSession = daysSinceInstall == 0,
                daysSinceInstall = daysSinceInstall,
                timerStatus = timerStatus
            )
            android.util.Log.d("MainActivity", "✅ session_start: days=$daysSinceInstall, status=$timerStatus")

            // [NEW] 그룹 A 알림 자동 예약 (2025-12-31)
            // 조건: 타이머 미실행 상태 && retry_count == 0
            try {

                if (!isTimerRunning && retryCount == 0) {
                    kr.sweetapps.alcoholictimer.util.notification.RetentionNotificationManager.scheduleGroupANotifications(this)
                    android.util.Log.d("MainActivity", "✅ Group A scheduled")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to schedule Group A", e)
            }

            // [NEW] 초기화 완료 플래그 설정 (2025-12-31)
            // UMP Consent → 알림 권한 다이얼로그 사용자 응답 → Session Start 모두 완료
            android.util.Log.d("MainActivity", "🚨 DEBUG: Setting isInitializationComplete = TRUE")
            android.util.Log.d("MainActivity", "🚨 DEBUG: Deep link navigation is NOW ENABLED")
            isInitializationComplete.value = true

            // [NEW] MainApplication 플래그도 설정 - App Open Ad 차단 해제 (2025-12-31)
            try {
                kr.sweetapps.alcoholictimer.MainApplication.isMainActivityInitComplete = true
                android.util.Log.d("MainActivity", "🚨 DEBUG: MainApplication.isMainActivityInitComplete = TRUE (App Open Ad allowed)")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to set MainApplication init flag", e)
            }

            android.util.Log.d("MainActivity", "✅ Initialization complete (value=${isInitializationComplete.value})")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to log session_start", e)
            // 오류 발생 시에도 초기화 완료로 처리 (앱 진행 가능하도록)
            android.util.Log.d("MainActivity", "🚨 DEBUG: Exception occurred - setting isInitializationComplete = TRUE anyway")
            isInitializationComplete.value = true
            kr.sweetapps.alcoholictimer.MainApplication.isMainActivityInitComplete = true
        }
    }

    /**
     * [NEW] 딥링크 Intent 처리 (2025-12-31)
     * 알림 클릭 시 전달된 화면 경로 및 정보 저장
     *
     * @param intent Intent
     */
    private fun handleDeepLinkIntent(intent: Intent?) {
        intent?.let {
            deepLinkScreenRoute = it.getStringExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_SCREEN_ROUTE)
            deepLinkNotificationId = it.getIntExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_NOTIFICATION_ID, 0)
            deepLinkGroupType = it.getStringExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_GROUP_TYPE)
            deepLinkShowBadgeAnimation = it.getBooleanExtra(kr.sweetapps.alcoholictimer.util.notification.DeepLinkConstants.EXTRA_SHOW_BADGE_ANIMATION, false)

            if (deepLinkScreenRoute != null) {
                android.util.Log.d("MainActivity", "🔗 Deep link: $deepLinkScreenRoute (Group: $deepLinkGroupType, ID: $deepLinkNotificationId)")

                // [NEW] Analytics 이벤트 전송 (2025-12-31)
                try {
                    kr.sweetapps.alcoholictimer.analytics.AnalyticsManager.logNotificationOpen(
                        notificationId = deepLinkNotificationId,
                        groupType = deepLinkGroupType ?: "unknown",
                        targetScreen = deepLinkScreenRoute ?: "unknown"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to log notification_open", e)
                }
            }
        }
    }

    /**
     * [NEW] 딥링크 네비게이션 실행 (2025-12-31)
     * [UPDATED] 초기화 완료 체크 추가 (2025-12-31)
     * NavController가 준비된 후 호출
     *
     * @param navController NavHostController
     */
    internal fun executeDeepLinkNavigation(navController: androidx.navigation.NavHostController) {
        // [NEW] 초기화 완료 체크 (2025-12-31)
        android.util.Log.d("MainActivity", "🔍 executeDeepLinkNavigation called - isInitComplete=${isInitializationComplete.value}")

        if (!isInitializationComplete.value) {
            android.util.Log.d("MainActivity", "⏳ Deep link navigation BLOCKED - initialization not complete")
            return
        }

        android.util.Log.d("MainActivity", "✅ Initialization verified - checking for deep link route")

        deepLinkScreenRoute?.let { route ->
            android.util.Log.d("MainActivity", "🚀 Deep link route found: $route - executing navigation")

            try {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
                android.util.Log.d("MainActivity", "✅ Navigation to $route completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Navigation to $route failed", e)
            }

            // 한 번 사용 후 초기화
            deepLinkScreenRoute = null
        } ?: run {
            android.util.Log.d("MainActivity", "ℹ️ No deep link route to execute")
        }
    }

    /**
     * [NEW] 배지 애니메이션 표시 여부 반환 (2025-12-31)
     *
     * @return true: 배지 애니메이션 표시
     */
    internal fun shouldShowBadgeAnimation(): Boolean {
        val shouldShow = deepLinkShowBadgeAnimation
        // 한 번 사용 후 초기화
        deepLinkShowBadgeAnimation = false
        return shouldShow
    }
}

/**
 * [NEW] MainActivity의 최상위 Content (2025-12-31)
 *
 * 초기화 완료 전까지 AppNavHost 렌더링을 완전히 차단
 *
 * @param startDestinationRoute 초기 화면 경로
 * @param holdSplashState Splash 상태
 * @param activity MainActivity 인스턴스
 */
@Composable
private fun MainActivityContent(
    startDestinationRoute: String,
    holdSplashState: androidx.compose.runtime.MutableState<Boolean>,
    activity: MainActivity
) {
    // 초기화 완료 상태 관찰
    val isInitComplete by activity.isInitializationComplete
    val showDialog by activity.showPermissionDialog

    android.util.Log.d("MainActivity", "🔄 MainActivityContent recompose - isInitComplete=$isInitComplete, showDialog=$showDialog")

    // [NEW] Box로 감싸서 다이얼로그가 최상위에 오도록 (2025-12-31)
    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        // [UPDATED] 초기화 중에도 빈 화면(흰색)만 보여주고 로딩 인디케이터 제거 (2026-01-03)
        // 이유: Release 빌드에서 로딩 화면이 다이얼로그를 가리는 문제 해결
        when {
            !isInitComplete -> {
                // 초기화 미완료 - 빈 화면만 표시 (다이얼로그가 보이도록)
                android.util.Log.d("MainActivity", "⏳ Rendering blank screen - waiting for dialog interaction")

                Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.White)
                ) {
                    // 빈 화면만 유지 - 로딩 인디케이터 제거
                }
            }

            else -> {
                // 초기화 완료 - 메인 UI 렌더링
                android.util.Log.d("MainActivity", "✅ Rendering AppNavHost - initialization complete")

                // 타이머 상태에 따른 실제 시작 화면 결정
                val sharedPref = activity.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                val startTime = sharedPref.getLong("start_time", 0L)
                val timerCompleted = sharedPref.getBoolean("timer_completed", false)
                val actualStartDestination = when {
                    timerCompleted -> kr.sweetapps.alcoholictimer.ui.main.Screen.Success.route
                    startTime > 0L -> kr.sweetapps.alcoholictimer.ui.main.Screen.Run.route
                    else -> kr.sweetapps.alcoholictimer.ui.main.Screen.Start.route
                }

                AppContentWithStart(actualStartDestination, holdSplashState)
            }
        }

        // [NEW] Pre-Permission 다이얼로그 - 최상위 레벨에서 표시 (2025-12-31)
        if (showDialog) {
            android.util.Log.d("MainActivity", "🔔 Showing Pre-Permission dialog on top of waiting screen")
            kr.sweetapps.alcoholictimer.ui.components.NotificationPermissionDialog(
                onConfirm = {
                    activity.handlePermissionDialogConfirm()
                },
                onDismiss = {
                    activity.handlePermissionDialogDismiss()
                }
            )
        }
    }
}

@Composable
private fun AppContentWithStart(
    startDestination: String,
    holdSplashState: androidx.compose.runtime.MutableState<Boolean> = androidx.compose.runtime.mutableStateOf(false)
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val communityViewModel = viewModel<CommunityViewModel>()

    // [REMOVED] 알림 권한 요청 로직을 MainActivity.onCreate()로 이동 (2025-12-31)
    // 이유: 앱 시작 시 즉시 권한을 확인하고 다이얼로그를 표시하기 위함

    // [NEW] 공유 버튼 클릭 시 커뮤니티 글쓰기 화면으로 이동
    fun navigateToCommunityWithDraft(draftContent: String) {
        communityViewModel.setDraftContent(draftContent)
        navController.navigate("community") {
            popUpTo("community") { inclusive = true }
        }
    }

    // [NEW] 전역 타이머 완료 네비게이션 리스너 (Activity Scope ViewModel)
    val activity = context as? MainActivity
    val tab01ViewModel: Tab01ViewModel? = activity?.let {
        viewModel<Tab01ViewModel>(viewModelStoreOwner = it)
    }

    // [NEW] 딥링크 네비게이션 실행 (2025-12-31)
    // [UPDATED] 초기화 완료 상태만 감지하도록 수정 (2025-12-31)
    // isInitializationComplete가 false → true로 변할 때만 실행됨
    LaunchedEffect(activity?.isInitializationComplete?.value) {
        val isInitComplete = activity?.isInitializationComplete?.value ?: false

        android.util.Log.d("MainActivity", "🔍 LaunchedEffect triggered - isInitComplete=$isInitComplete")

        if (isInitComplete) {
            android.util.Log.d("MainActivity", "✅ Initialization complete detected - checking for deep link")
            activity?.executeDeepLinkNavigation(navController)
        } else {
            android.util.Log.d("MainActivity", "⏳ Initialization not complete yet - navigation blocked")
        }
    }

    // [REFACTORED] 타이머 완료/중단 시 전역 네비게이션 처리
    LaunchedEffect(tab01ViewModel) {
        tab01ViewModel?.navigationEvent?.collect { event ->
            when (event) {
                is Tab01ViewModel.NavigationEvent.NavigateToSuccess -> {
                    android.util.Log.d("MainActivity", "🎉 [Global] Timer finished! Navigating to Success screen")

                    // Success 화면으로 이동
                    navController.navigate(Screen.Success.route) {
                        popUpTo(Screen.Start.route) { inclusive = false }
                        launchSingleTop = true
                    }

                    android.util.Log.d("MainActivity", "Navigation to SuccessScreen completed")
                }
                is Tab01ViewModel.NavigationEvent.NavigateToGiveUp -> {
                    android.util.Log.d("MainActivity", "🍃 [Global] Timer gave up! Navigating to GiveUp screen")

                    // GiveUp 화면으로 이동
                    navController.navigate(Screen.GiveUp.route) {
                        popUpTo(Screen.Start.route) { inclusive = false }
                        launchSingleTop = true
                    }

                    android.util.Log.d("MainActivity", "Navigation to GiveUpScreen completed")
                }
                is Tab01ViewModel.NavigationEvent.NavigateToDetail -> {
                    android.util.Log.d("MainActivity", "📊 Navigating to Detail screen")

                    // DetailScreen으로 직접 이동
                    val route = Screen.Detail.createRoute(
                        startTime = event.startTime,
                        endTime = event.endTime,
                        targetDays = event.targetDays,
                        actualDays = event.actualDays,
                        isCompleted = true
                    )

                    navController.navigate(route) {
                        popUpTo(0) { inclusive = false }
                        launchSingleTop = true
                    }

                    android.util.Log.d("MainActivity", "Navigation to Detail completed")
                }
            }
        }
    }

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
        AppNavHost(navController, startDestination)
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
