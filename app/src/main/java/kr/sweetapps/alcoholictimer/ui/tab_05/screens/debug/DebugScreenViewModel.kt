package kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kr.sweetapps.alcoholictimer.core.util.DebugSettings
import kr.sweetapps.alcoholictimer.BuildConfig

data class DebugScreenUiState(
    val switch1: Boolean = false,
    val demoMode: Boolean = false,
    val switch3: Boolean = false,
    val switch4: Boolean = false,
    val switch5: Boolean = false,
)

class DebugScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DebugScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(demoMode = DebugSettings.isDemoModeEnabled(application))
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
                else -> currentState
            }
        }
    }

    fun resetConsent() {
        if (!BuildConfig.DEBUG) return
        val consentInformation = UserMessagingPlatform.getConsentInformation(getApplication())
        consentInformation.reset()
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
                // Analytics test event (use Bundle to avoid deprecated param API)
                try {
                    val b = Bundle().apply { putString("source", "DebugScreen") }
                    Firebase.analytics.logEvent("debug_test_event", b)
                } catch (_: Exception) {
                    // swallow in debug
                }
            }

            4 -> {
                // non-fatal Crashlytics report
                viewModelScope.launch {
                    try {
                        // Ensure collection enabled in debug so reports are uploaded
                        try {
                            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                        } catch (e: Throwable) {
                            Log.w("DebugScreenVM", "Failed to enable Crashlytics collection: ${e.message}")
                        }

                        // Record non-fatal exception
                        FirebaseCrashlytics.getInstance().recordException(Exception("Debug non-fatal from DebugScreen"))
                        Log.d("DebugScreenVM", "Recorded non-fatal exception to Crashlytics")

                        // Attempt to send unsent reports immediately (best-effort)
                        try {
                            // sendUnsentReports can throw; call in try-catch
                            FirebaseCrashlytics.getInstance().sendUnsentReports()
                            Log.d("DebugScreenVM", "Requested sendUnsentReports()")
                        } catch (e: Throwable) {
                            Log.w("DebugScreenVM", "sendUnsentReports failed: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.w("DebugScreenVM", "Crashlytics action failed: ${e.message}")
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
