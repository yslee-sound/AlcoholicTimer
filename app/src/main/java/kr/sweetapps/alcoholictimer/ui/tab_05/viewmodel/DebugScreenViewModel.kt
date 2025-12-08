package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kr.sweetapps.alcoholictimer.util.debug.DebugSettings
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager

data class DebugScreenUiState(
    val switch1: Boolean = false,
    val demoMode: Boolean = false,
    val umpForceEea: Boolean = false,
    val switch3: Boolean = false,
    val switch4: Boolean = false,
    val switch5: Boolean = false,
)

class DebugScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DebugScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                demoMode = DebugSettings.isDemoModeEnabled(application),
                umpForceEea = DebugSettings.isUmpForceEeaEnabled(application)
            )
        }
    }

    fun setSwitch(switchIndex: Int, value: Boolean) {
        _uiState.update { currentState ->
            when (switchIndex) {
                1 -> currentState.copy(switch1 = value)
                2 -> {
                    DebugSettings.setDemoModeEnabled(getApplication(), value)
                    currentState.copy(demoMode = value)
                }
                3 -> currentState.copy(switch3 = value)
                4 -> currentState.copy(switch4 = value)
                5 -> currentState.copy(switch5 = value)
                6 -> {
                    DebugSettings.setUmpForceEeaEnabled(getApplication(), value)
                    // When debug toggle changes, immediately reset primary and ads-side consent and re-request so test effect is immediate
                    if (BuildConfig.DEBUG) {
                        try {
                            // Reset primary UMP manager (if available) and ads-side stored prefs
                            try {
                                val mainApp = getApplication<Application>() as? MainApplication
                                mainApp?.umpConsentManager?.resetConsent(getApplication())
                            } catch (_: Throwable) {}
                        } catch (_: Throwable) {}
                        try {
                            val act = MainApplication.currentActivity
                            if (act != null) {
                                val mainApp = getApplication<Application>() as? MainApplication
                                mainApp?.umpConsentManager?.requestAndLoadIfRequired(act) { can ->
                                    Log.d("DebugScreenVM", "UMP EEA toggle changed -> UMP request finished -> canRequestAds=$can")
                                }
                            } else {
                                Log.d("DebugScreenVM", "UMP EEA toggle changed -> no currentActivity to trigger UMP request")
                            }
                        } catch (_: Throwable) {}
                    }
                    currentState.copy(umpForceEea = value)
                }
                else -> currentState
            }
        }
    }

    // [NEW] 디버그 모드 광고 쿨타임 설정
    fun setDebugAdCoolDown(context: Context, seconds: Long) {
        if (!BuildConfig.DEBUG) return
        try {
            AdPolicyManager.setDebugCoolDownSeconds(context, seconds)
            Log.d("DebugScreenVM", "광고 쿨타임 설정: $seconds 초")
        } catch (t: Throwable) {
            Log.e("DebugScreenVM", "광고 쿨타임 설정 실패", t)
        }
    }

    // [NEW] 디버그 모드 광고 쿨타임 가져오기
    fun getDebugAdCoolDown(context: Context): Long {
        return try {
            AdPolicyManager.getDebugCoolDownSeconds(context)
        } catch (t: Throwable) {
            Log.e("DebugScreenVM", "광고 쿨타임 로드 실패", t)
            -1L
        }
    }

    fun resetConsent() {
        if (!BuildConfig.DEBUG) return
        Log.d("DebugScreenVM", "resetConsent called")
        val consentInformation = UserMessagingPlatform.getConsentInformation(getApplication())
        consentInformation.reset()
        // Ensure ads.UmpConsentManager persisted flags are cleared so ads flow will re-check consent
        try {
            val sp = getApplication<Application>().applicationContext.getSharedPreferences("ump_prefs", Context.MODE_PRIVATE)
            sp.edit { clear() }
            Log.d("DebugScreenVM", "resetConsent: cleared umb_prefs (ump_prefs)")
            // Also clear ads-side manager internal state immediately
            try {
                val mainApp = getApplication<Application>() as? MainApplication
                mainApp?.umpConsentManager?.resetConsent(getApplication())
                Log.d("DebugScreenVM", "Called umpConsentManager.resetConsent")
            } catch (_: Throwable) { Log.d("DebugScreenVM", "umpConsentManager.resetConsent failed") }
            // If there's a foreground activity, trigger ads-side consent re-check immediately
            try {
                val act = MainApplication.currentActivity
                if (act != null) {
                    val mainApp = getApplication<Application>() as? MainApplication
                    mainApp?.umpConsentManager?.requestAndLoadIfRequired(act) { canReq ->
                        Log.d("DebugScreenVM", "UMP request finished after debug reset -> canRequestAds=$canReq")
                    }
                } else {
                    Log.d("DebugScreenVM", "No currentActivity available to trigger UMP request")
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    /**
     * Perform debug action for switches 3..5.
     * Actions are guarded to run only in debug builds.
     * - 3: Analytics test event
     * - 4: non-fatal Crashlytics report
     * - 5: Performance trace (start -> wait -> stop)
     */
    fun performAction(actionIndex: Int) {
        if (!BuildConfig.DEBUG) return

        when (actionIndex) {
            3 -> {
                // [수정] 로그를 명확하게 찍도록 변경
                Log.d("MY_TEST", "Analytics 이벤트 전송 시도 중...") // 시도 로그
                try {
                    val b = Bundle().apply { putString("source", "DebugScreen") }
                    Firebase.analytics.logEvent("debug_test_event", b)
                    Log.d("MY_TEST", "Analytics 이벤트 전송 명령 성공!") // 성공 로그
                } catch (e: Exception) {
                    Log.e("MY_TEST", "Analytics 전송 실패: ${e.message}") // 실패 로그 (에러 내용 확인)
                }
            }

            4 -> {
                // [NEW] Crashlytics 테스트: 버튼 클릭 시에만 일시적으로 활성화하여 전송
                viewModelScope.launch {
                    try {
                        // 1. 일시적으로 수집 활성화 (이 세션에서만)
                        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                        Log.d("DebugScreenVM", "Crashlytics collection enabled for test")

                        // 2. 비치명 예외 기록
                        Firebase.crashlytics.recordException(Exception("Debug non-fatal test from DebugScreen"))
                        Log.d("DebugScreenVM", "Recorded non-fatal exception to Crashlytics")

                        // 3. 즉시 전송 시도 (best-effort)
                        try {
                            Firebase.crashlytics.sendUnsentReports()
                            Log.d("DebugScreenVM", "Requested sendUnsentReports()")
                        } catch (e: Throwable) {
                            Log.w("DebugScreenVM", "sendUnsentReports failed: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.w("DebugScreenVM", "Crashlytics test action failed: ${e.message}")
                    }
                }
            }

            5 -> {
                // Performance trace: run short trace asynchronously
                viewModelScope.launch {
                    try {
                        val perf = Firebase.performance
                        val trace: Trace = perf.newTrace("debug_trace")
                        trace.start()
                        // simulate short work
                        delay(1500)
                        trace.stop()
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }
        }
    }
}
