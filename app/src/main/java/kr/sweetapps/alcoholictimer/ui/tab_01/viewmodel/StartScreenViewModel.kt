package kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager
import kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager
import kr.sweetapps.alcoholictimer.data.repository.TimerStateRepository
import kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import java.util.Locale

/**
 * [NEW] StartScreen ViewModel - MVVM 아키텍처 적용
 *
 * 기존 StartScreen.kt에 섞여 있던 비즈니스 로직을 분리하여 관리합니다.
 * - SharedPreferences 데이터 로드/저장
 * - 광고 정책 관리 (AppOpen, Interstitial)
 * - 카운트다운 로직
 * - 타이머 시작 로직
 * - Analytics 이벤트 전송
 * - 네비게이션 로직 통합 관리
 */
class StartScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref: SharedPreferences = application.getSharedPreferences(
        "user_settings",
        Context.MODE_PRIVATE
    )

    // [NEW] 자동 네비게이션 제어 플래그 (Preview나 특정 케이스에서 자동 이동 방지)
    private var gateNavigation: Boolean = false

    // UI 상태 관리
    private val _uiState = MutableStateFlow(StartScreenUiState())
    val uiState: StateFlow<StartScreenUiState> = _uiState.asStateFlow()

    // 네비게이션 이벤트 (One-Time Event)
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    // Snackbar 메시지 이벤트
    private val _snackbarEvent = MutableStateFlow<String?>(null)
    val snackbarEvent: StateFlow<String?> = _snackbarEvent.asStateFlow()

    init {
        loadTimerState()
        checkForPendingSnackbar()
    }

    /**
     * [NEW] 자동 네비게이션 제어 설정
     */
    fun setGateNavigation(gate: Boolean) {
        gateNavigation = gate
    }

    /**
     * [NEW] 타이머 상태 로드 (SharedPreferences)
     *
     * 타이머가 이미 실행 중이라면 자동으로 RunScreen으로 이동하도록 네비게이션 이벤트를 발행합니다.
     */
    private fun loadTimerState() {
        viewModelScope.launch {
            try {
                val startTime = sharedPref.getLong("start_time", 0L)
                val timerCompleted = sharedPref.getBoolean("timer_completed", false)
                val targetDays = sharedPref.getFloat("target_days", 21f).toInt()

                _uiState.update {
                    it.copy(
                        startTime = startTime,
                        timerCompleted = timerCompleted,
                        targetDays = targetDays
                    )
                }

                Log.d(TAG, "Timer state loaded: startTime=$startTime, completed=$timerCompleted, target=$targetDays")

                // [NEW] 타이머가 이미 실행 중이고 gateNavigation이 false라면 자동으로 RunScreen으로 이동
                if (!gateNavigation && startTime != 0L && !timerCompleted) {
                    Log.d(TAG, "Active timer detected -> auto-navigating to RunScreen")
                    _navigationEvent.value = NavigationEvent.NavigateToRun(targetDays)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load timer state", e)
            }
        }
    }

    /**
     * [NEW] 설정 변경 후 Snackbar 표시 체크
     */
    private fun checkForPendingSnackbar() {
        viewModelScope.launch {
            try {
                val pending = sharedPref.getBoolean("settings_applied_snackbar_pending", false)
                if (pending) {
                    sharedPref.edit {
                        putBoolean("settings_applied_snackbar_pending", false)
                    }
                    _snackbarEvent.value = "설정이 반영되어 절약 금액이 업데이트되었습니다! 💰"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check pending snackbar", e)
            }
        }
    }

    /**
     * [NEW] 목표 일수 변경 (뱃지 클릭)
     * 뱃지를 클릭하면 커스텀 입력 모드를 해제하고 해당 뱃지를 선택 상태로 만듭니다.
     */
    fun onBadgeSelected(days: Int) {
        _uiState.update {
            it.copy(
                targetDays = days,
                isCustomInputMode = false // 뱃지 선택 시 커스텀 모드 해제
            )
        }
    }

    /**
     * [NEW] 목표 일수 변경 (직접 입력)
     * 사용자가 입력 필드에 직접 입력하면 커스텀 입력 모드로 전환하고 모든 뱃지 선택을 해제합니다.
     */
    fun onCustomInputChanged(days: Int) {
        _uiState.update {
            it.copy(
                targetDays = days,
                isCustomInputMode = true // 직접 입력 시 커스텀 모드 활성화
            )
        }
    }

    /**
     * [DEPRECATED] 목표 일수 변경 (하위 호환용)
     * 새 코드에서는 onBadgeSelected 또는 onCustomInputChanged를 사용하세요.
     */
    fun onTargetDaysChanged(days: Int) {
        // 기본 동작: 커스텀 입력 모드로 간주
        onCustomInputChanged(days)
    }

    /**
     * [NEW] AppOpen Ad 초기화 (Splash 화면 홀드)
     */
    fun initializeAppOpenAd(context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "AppOpen integration: holding splash and initializing listeners")

                // 광고 로드 상태 추적
                var adShown = false

                val onLoaded = {
                    try {
                        val act = context as? Activity
                        if (act != null && _uiState.value.isSplashHeld && AppOpenAdManager.isLoaded()) {
                            val shown = AppOpenAdManager.showIfAvailable(act)
                            Log.d(TAG, "AppOpen showIfAvailable returned: $shown")
                            if (shown) {
                                adShown = true
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "onAdLoaded handler failed: $t")
                    }
                }

                val onFinished = {
                    try {
                        Log.d(TAG, "AppOpen finished -> releasing splash")
                        _uiState.update { it.copy(isSplashHeld = false) }
                    } catch (t: Throwable) {
                        Log.w(TAG, "onAdFinished handler failed: $t")
                    }
                }

                val onLoadFailed = {
                    try {
                        Log.d(TAG, "AppOpen load failed -> releasing splash immediately")
                        _uiState.update { it.copy(isSplashHeld = false) }
                    } catch (t: Throwable) {
                        Log.w(TAG, "onAdLoadFailed handler failed: $t")
                    }
                }

                AppOpenAdManager.addOnAdLoadedListener(onLoaded)
                AppOpenAdManager.addOnAdFinishedListener(onFinished)
                AppOpenAdManager.addOnAdLoadFailedListener(onLoadFailed)

                // 스플래시 홀드 시작
                _uiState.update { it.copy(isSplashHeld = true) }

                try {
                    AppOpenAdManager.preload(context.applicationContext)
                } catch (t: Throwable) {
                    Log.w(TAG, "preload call failed: $t")
                }

                // 즉시 표시 시도
                try {
                    val act = context as? Activity
                    if (act != null && AppOpenAdManager.isLoaded()) {
                        val shown = AppOpenAdManager.showIfAvailable(act)
                        Log.d(TAG, "Immediate showIfAvailable returned: $shown")
                        if (shown) {
                            adShown = true
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "immediate showIfAvailable failed: $t")
                }

                // 4초 타임아웃 (Google AdMob 권장)
                delay(4000L)
                if (_uiState.value.isSplashHeld) {
                    if (!adShown) {
                        Log.d(TAG, "Safety timeout reached (no ad shown) -> releasing splash")
                        _uiState.update { it.copy(isSplashHeld = false) }
                    } else {
                        Log.d(TAG, "Safety timeout reached but ad is showing -> keep splash active")
                    }
                }

                // 리스너 정리
                AppOpenAdManager.removeOnAdLoadedListener(onLoaded)
                AppOpenAdManager.removeOnAdFinishedListener(onFinished)
                AppOpenAdManager.removeOnAdLoadFailedListener(onLoadFailed)

            } catch (t: Throwable) {
                Log.w(TAG, "AppOpen integration failed: $t")
                _uiState.update { it.copy(isSplashHeld = false) }
            }
        }
    }

    /**
     * [NEW] 타이머 시작 버튼 클릭
     */
    fun onStartButtonClicked(context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "========================================")
                Log.d(TAG, "Timer start button clicked - ad check started")

                val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
                Log.d(TAG, "shouldShowInterstitialAd = $shouldShowAd")

                if (shouldShowAd) {
                    // 전면 광고 표시 시도
                    val activity = context as? Activity
                    if (activity != null) {
                        val adLoaded = InterstitialAdManager.isLoaded()
                        Log.d(TAG, "InterstitialAdManager.isLoaded() = $adLoaded")

                        if (adLoaded) {
                            Log.d(TAG, "✅ Showing interstitial ad")
                            InterstitialAdManager.show(activity) { success ->
                                Log.d(TAG, "Ad callback: success=$success")
                                startCountdown()
                            }
                        } else {
                            Log.d(TAG, "Ad not loaded -> start countdown immediately")
                            startCountdown()
                        }
                    } else {
                        Log.d(TAG, "activity null -> start countdown immediately")
                        startCountdown()
                    }
                } else {
                    Log.d(TAG, "In cooldown -> skip ad and start countdown")
                    startCountdown()
                }

                Log.d(TAG, "========================================")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle start button click", e)
                // 실패 시에도 카운트다운 시작
                startCountdown()
            }
        }
    }

    /**
     * [NEW] 카운트다운 시작 (3 -> 2 -> 1)
     * 각 숫자마다 펄스 애니메이션이 완전히 보이도록 1.2초씩 표시
     */
    private fun startCountdown() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(showCountdown = true, countdownNumber = 3) }

                // 3 - 펄스 애니메이션 완료 대기
                delay(1200L)
                _uiState.update { it.copy(countdownNumber = 2) }

                // 2 - 펄스 애니메이션 완료 대기
                delay(1200L)
                _uiState.update { it.copy(countdownNumber = 1) }

                // 1 - 펄스 애니메이션 완료 대기
                delay(1200L)

                // 타이머 시작
                startTimer()

            } catch (e: Exception) {
                Log.e(TAG, "Countdown failed", e)
                startTimer() // 실패해도 타이머 시작
            }
        }
    }

    /**
     * [NEW] 타이머 시작 로직
     */
    private fun startTimer() {
        viewModelScope.launch {
            try {
                val targetDays = _uiState.value.targetDays

                // Analytics 이벤트 전송
                try {
                    val hadActiveGoal = sharedPref.getLong("start_time", 0L) > 0L
                    AnalyticsManager.logTimerStart(
                        targetDays = targetDays,
                        hadActiveGoal = hadActiveGoal,
                        startTs = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log analytics", e)
                }

                // SharedPreferences에 저장
                val formatted = String.format(Locale.US, "%.6f", targetDays.toFloat()).toFloat()
                sharedPref.edit {
                    putFloat("target_days", formatted)
                    putLong("start_time", System.currentTimeMillis())
                    putBoolean("timer_completed", false)
                }

                // TimerStateRepository 초기화
                try {
                    TimerStateRepository.resetTimer()
                    TimerStateRepository.setStartTime(System.currentTimeMillis())
                    TimerStateRepository.setTimerActive(true)
                    Log.d(TAG, "Timer started: $targetDays days, active: true")
                } catch (t: Throwable) {
                    Log.e(TAG, "Timer state initialization failed", t)
                }

                // Interstitial Ad 미리 로드
                InterstitialAdManager.preload(getApplication<Application>().applicationContext)

                // 네비게이션 이벤트 발행
                _navigationEvent.value = NavigationEvent.NavigateToRun(targetDays)

                Log.d(TAG, "Timer started successfully: $targetDays days")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start timer", e)
            }
        }
    }

    /**
     * [NEW] Snackbar 표시 완료
     */
    fun onSnackbarShown() {
        _snackbarEvent.value = null
    }

    /**
     * [NEW] 네비게이션 완료
     */
    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    /**
     * [NEW] Splash 해제 확인
     */
    fun onSplashReleased() {
        _uiState.update { it.copy(isSplashHeld = false) }
    }

    companion object {
        private const val TAG = "StartScreenViewModel"
    }
}

/**
 * [NEW] StartScreen UI 상태
 */
data class StartScreenUiState(
    val targetDays: Int = 21,
    val startTime: Long = 0L,
    val timerCompleted: Boolean = false,
    val showCountdown: Boolean = false,
    val countdownNumber: Int = 3,
    val isSplashHeld: Boolean = false,
    val isCustomInputMode: Boolean = false // [MANUAL OVERRIDE] 직접 입력 모드 플래그
)

/**
 * [NEW] 네비게이션 이벤트 (One-Time Event)
 */
sealed class NavigationEvent {
    data class NavigateToRun(val targetDays: Int) : NavigationEvent()
}

