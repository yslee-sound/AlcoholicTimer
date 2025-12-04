@file:Suppress("UNUSED_PARAMETER")
// [NEW] 클린 아키텍처 리팩토링: UmpConsentManager를 consent 패키지로 이동
package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import androidx.core.content.edit
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.util.debug.DebugSettings
import kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager

/**
 * UMP 통합 구현체
 * - 실제 UMP SDK를 사용하여 consent 정보 조회/폼 표시/응답 전달을 실행합니다
 */
class UmpConsentManager(private val context: Context) {
    private val TAG = "UmpConsentManager"

    // Persistent prefs keys
    private val PREFS_NAME = "ump_prefs"
    private val KEY_CONSENT_CHECKED = "consent_checked"
    private val KEY_LAST_CAN_REQUEST = "last_can_request_ads"

    // Indicates whether consent info has been queried at least once (requestConsentInfoUpdate completed)
    @Volatile var consentChecked: Boolean = false
        private set

    // Last known value of canRequestAds after consent check. Default to false to be conservative.
    @Volatile var lastCanRequestAds: Boolean = false
        private set

    // [NEW] StateFlow for personalized ads consent status
    private val _isPersonalizedAdsAllowed = MutableStateFlow(false)
    val isPersonalizedAdsAllowed: StateFlow<Boolean> = _isPersonalizedAdsAllowed.asStateFlow()

    // [NEW] StateFlow for privacy options required status
    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    // [NEW] 생성자 초기화 블록에서 자동으로 prefs 로드
    init {
        loadFromPrefs(context)
    }

    // Prevent concurrent updates / queue callbacks
    @Volatile private var isUpdating: Boolean = false
    private val pendingCallbacks = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()

    // External listeners (UI etc.) that want to be notified when consent state changes
    private val consentListeners = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()

    // ?�로 추�?: ?�재 ?�의 ?�이 ?�면???�시 중인지 ?��?. AppOpen 광고가 ?�의 ???�에 ?�시?�는 것을 방�??�기 ?�해 ?�용.
    @Volatile private var formShowing: Boolean = false

    // Load persisted state if available
    private fun loadFromPrefs(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            consentChecked = sp.getBoolean(KEY_CONSENT_CHECKED, false)
            lastCanRequestAds = sp.getBoolean(KEY_LAST_CAN_REQUEST, false)
            Log.d(TAG, "loadFromPrefs -> consentChecked=$consentChecked lastCanRequest=$lastCanRequestAds")
        } catch (_: Throwable) {}
    }

    private fun saveToPrefs(context: Context, canRequest: Boolean) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean(KEY_CONSENT_CHECKED, true); putBoolean(KEY_LAST_CAN_REQUEST, canRequest) }
            Log.d(TAG, "saveToPrefs -> consentChecked=true lastCanRequest=$canRequest")
        } catch (_: Throwable) {}
    }

    /** Initialize from persisted prefs. Call from Application.onCreate after primary manager is constructed. */
    fun initialize(context: Context) {
        loadFromPrefs(context)
    }

    fun addConsentChangeListener(listener: (Boolean) -> Unit) {
        try { consentListeners.add(listener) } catch (_: Throwable) {}
    }
    fun removeConsentChangeListener(listener: (Boolean) -> Unit) {
        try { consentListeners.remove(listener) } catch (_: Throwable) {}
    }

    /** 새로운 API: UMP 동의 폼이 현재 표시 중인지 반환합니다. */
    fun isFormShowing(): Boolean {
        try {
            // Prefer primary manager's flag if available
            val app = try { MainApplication.currentActivity?.application as? MainApplication } catch (_: Throwable) { null }
            if (app != null) {
                try {
                    return app.umpConsentManager.isFormShowing()
                } catch (_: Throwable) {}
            }
            // Fallback to internal flag
            return try { formShowing } catch (_: Throwable) { false }
        } catch (_: Throwable) {
            return false
        }
    }

    fun showPrivacyOptionsForm(activity: Activity, onClosed: (Any?) -> Unit = {}) {
        // Prefer primary manager if available
        try {
            val app = activity.application as? MainApplication
            if (app != null) {
                try {
                    app.umpConsentManager.showPrivacyOptionsForm(activity) { err -> onClosed(err) }
                    return
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}

        // Fallback: use UMP privacy options form if available
        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            if (consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                // showPrivacyOptionsForm (UMP) will load+show the privacy options form; callback receives an optional FormError
                UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError: FormError? ->
                    try {
                        if (formError != null) onClosed(formError) else onClosed(null)
                    } catch (t: Throwable) { onClosed(t) }
                }
            } else {
                onClosed(null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "showPrivacyOptionsForm fallback failed: ${t.message}")
            onClosed(null)
        }
    }

    /** Apply an external consent decision into ads manager state and notify listeners. */
    fun applyExternalConsent(context: Context, canRequest: Boolean) {
        try {
            consentChecked = true
            lastCanRequestAds = canRequest
            saveToPrefs(context, canRequest)

            // [NEW] Update personalized ads consent status
            try {
                val consentInfo = UserMessagingPlatform.getConsentInformation(context)
                val isPersonalized = canRequest && (consentInfo.consentStatus == com.google.android.ump.ConsentInformation.ConsentStatus.OBTAINED)
                _isPersonalizedAdsAllowed.value = isPersonalized

                // Update privacy options required status
                val privacyOptionsRequired = try {
                    consentInfo.privacyOptionsRequirementStatus == com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                } catch (_: Throwable) { false }
                _isPrivacyOptionsRequired.value = privacyOptionsRequired

                Log.d(TAG, "applyExternalConsent -> isPersonalizedAdsAllowed=$isPersonalized privacyOptionsRequired=$privacyOptionsRequired (canRequest=$canRequest consentStatus=${consentInfo.consentStatus})")
            } catch (_: Throwable) {
                _isPersonalizedAdsAllowed.value = false
                _isPrivacyOptionsRequired.value = false
            }

            try {
                val debugMode = try {
                    val cls = Class.forName(context.packageName + ".BuildConfig")
                    val f = cls.getDeclaredField("DEBUG")
                    (f.get(null) as? Boolean) == true
                } catch (_: Throwable) { false }
                if (canRequest || debugMode) {
                    runCatching { InterstitialAdManager.preload(context.applicationContext) }
                    android.util.Log.d(TAG, "applyExternalConsent -> triggered InterstitialAdManager.preload (canRequest=$canRequest debug=$debugMode)")
                }
            } catch (_: Throwable) {}
            try { AppOpenAdManager.onConsentUpdated(canRequest) } catch (_: Throwable) {}
            // Try to show AppOpen soon after consent applied if an activity is available.
            try {
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.postDelayed({
                    try {
                        val act = try { MainApplication.currentActivity } catch (_: Throwable) { null }
                        if (act != null) {
                            val cls = try { act.javaClass.simpleName } catch (_: Throwable) { "" }
                            if (cls != "SplashScreen" && cls != "AppOpenOverlayActivity") {
                                try {
                                    val shown = runCatching { AppOpenAdManager.showIfAvailable(act) }.getOrDefault(false)
                                    android.util.Log.d(TAG, "applyExternalConsent -> attempted post-consent AppOpen showIfAvailable returned=$shown")
                                } catch (_: Throwable) {}
                            }
                        }
                    } catch (_: Throwable) {}
                }, 350L)
             } catch (_: Throwable) {}

             try {
                 val mainHandler = Handler(Looper.getMainLooper())
                 mainHandler.post { for (l in consentListeners) runCatching { l.invoke(canRequest) } }
             } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    /** Request consent info update from UMP SDK. */
    fun requestConsentInfoUpdate(activity: Activity, onComplete: (Boolean) -> Unit) {
        // Skip if already updating
        if (isUpdating) {
            Log.w(TAG, "requestConsentInfoUpdate skipped: already updating")
            onComplete(false)
            return
        }

        // Mark as updating
        isUpdating = true

        // Clear pending callbacks
        pendingCallbacks.clear()

        // Add initial callback to pending
        pendingCallbacks.add(onComplete)

        // Use UMP to request consent info update. Honor debug geography if debug setting enabled.
        try {
            // Build debug settings if forced via DebugSettings
            val paramsBuilder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
            try {
                val isDebugBuild = try {
                    val cls = Class.forName(activity.packageName + ".BuildConfig")
                    val f = cls.getDeclaredField("DEBUG")
                    (f.get(null) as? Boolean) == true
                } catch (_: Throwable) { false }
                if (isDebugBuild && DebugSettings.isUmpForceEeaEnabled(activity.applicationContext)) {
                    val debugBuilder = ConsentDebugSettings.Builder(activity.applicationContext)
                    // Force EEA geography
                    debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    paramsBuilder.setConsentDebugSettings(debugBuilder.build())
                }
            } catch (_: Throwable) {}

            val params = paramsBuilder.build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            consentInformation.requestConsentInfoUpdate(activity, params,
                {
                    // onSuccess
                    try {
                        consentChecked = true
                        val status = consentInformation.consentStatus
                        // Treat NOT_REQUIRED and OBTAINED as allowing ad requests.
                        // NOT_REQUIRED: UMP indicates consent dialog not required for this user/region -> ads may be requested.
                        // OBTAINED: user granted consent for personalized ads.
                        val canRequest = try {
                            status == ConsentInformation.ConsentStatus.OBTAINED ||
                            status == ConsentInformation.ConsentStatus.NOT_REQUIRED
                        } catch (_: Throwable) {
                            // Fallback conservative: only allow when explicitly obtained
                            status == ConsentInformation.ConsentStatus.OBTAINED
                        }
                         lastCanRequestAds = canRequest
                         saveToPrefs(activity.applicationContext, canRequest)
                         for (cb in pendingCallbacks) { try { cb.invoke(canRequest) } catch (_: Throwable) {} }
                    } catch (t: Throwable) {
                        for (cb in pendingCallbacks) { try { cb.invoke(false) } catch (_: Throwable) {} }
                    }
                    isUpdating = false
                },
                { formError: FormError? ->
                    try {
                        for (cb in pendingCallbacks) { try { cb.invoke(false) } catch (_: Throwable) {} }
                    } catch (_: Throwable) {}
                    Log.e(TAG, "requestConsentInfoUpdate failed: ${formError?.message}")
                    isUpdating = false
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "requestConsentInfoUpdate failed: ${t.message}")
            for (cb in pendingCallbacks) { try { cb.invoke(false) } catch (_: Throwable) {} }
            isUpdating = false
        }
    }

    /** Deprecated: Use requestConsentInfoUpdate instead */
    @Deprecated(
        message = "Use requestConsentInfoUpdate instead",
        replaceWith = ReplaceWith("requestConsentInfoUpdate(activity, onComplete)"))
    fun requestConsentUpdate(activity: Activity, onComplete: (Boolean) -> Unit) {
        requestConsentInfoUpdate(activity, onComplete)
    }

    /** Show the consent form if applicable. */
    fun showConsentForm(activity: Activity, onClosed: (Boolean) -> Unit) {
        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.UNKNOWN) {
                UserMessagingPlatform.loadConsentForm(activity,
                    { consentForm ->
                        try {
                            consentForm?.show(activity) { formError: FormError? -> onClosed(formError == null) }
                        } catch (t: Throwable) { onClosed(false) }
                    },
                    { loadError: FormError? ->
                        onClosed(false)
                    }
                )
            } else {
                onClosed(consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "showConsentForm failed: ${t.message}")
            onClosed(false)
        }
    }

    /** Deprecated: Use showConsentForm instead */
    @Deprecated(
        message = "Use showConsentForm instead",
        replaceWith = ReplaceWith("showConsentForm(activity, onClosed)"))
    fun show(activity: Activity, onClosed: (Boolean) -> Unit) {
        showConsentForm(activity, onClosed)
    }

    /** Force the consent status to be unknown (re-prompt). */
    fun resetConsent(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean(KEY_CONSENT_CHECKED, false) }
            Log.d(TAG, "resetConsent -> consentChecked=false")
        } catch (_: Throwable) {}
    }

    /** For testing: force the consent status and request ad loading permission. */
    fun testSetConsent(context: Context) {
        try {
            consentChecked = true
            lastCanRequestAds = true
            saveToPrefs(context, true)
            try { InterstitialAdManager.preload(context.applicationContext) } catch (_: Throwable) {}
            try { AppOpenAdManager.onConsentUpdated(true) } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    /** For testing: clear consent and request ad loading permission. */
    fun testClearConsent(context: Context) {
        try {
            consentChecked = false
            lastCanRequestAds = false
            saveToPrefs(context, false)
        } catch (_: Throwable) {}
    }

    /** For testing: force the consent status and deny request ad loading permission. */
    fun testSetConsentDenied(context: Context) {
        try {
            consentChecked = true
            lastCanRequestAds = false
            saveToPrefs(context, false)
        } catch (_: Throwable) {}
    }

    /** For testing: trigger a consent form show. */
    fun testShowConsentForm(activity: Activity) {
        try {
            UserMessagingPlatform.loadConsentForm(activity,
                { consentForm ->
                    Log.d(TAG, "testShowConsentForm loaded successfully")
                    try { consentForm?.show(activity) { formError: FormError? -> Log.d(TAG, "testShowConsentForm shown formError=${formError?.message}") } } catch (_: Throwable) {}
                },
                { loadError: FormError? -> Log.d(TAG, "testShowConsentForm completed with error: ${loadError?.message}") }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "testShowConsentForm failed: ${t.message}")
        }
    }

    /** For testing: print the current consent status. */
    fun testPrintConsentStatus(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val checked = sp.getBoolean(KEY_CONSENT_CHECKED, false)
            val lastCanRequest = sp.getBoolean(KEY_LAST_CAN_REQUEST, false)
            Log.d(TAG, "testPrintConsentStatus -> consentChecked=$checked lastCanRequest=$lastCanRequest")
        } catch (_: Throwable) {}
    }

    /** For testing: force the privacy options requirement status. */
    fun testSetPrivacyOptionsRequired(context: Context, required: Boolean) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean("privacy_options_required", required) }
            Log.d(TAG, "testSetPrivacyOptionsRequired -> required=$required")
        } catch (_: Throwable) {}
    }

    /** For testing: clear the privacy options requirement status. */
    fun testClearPrivacyOptionsRequired(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { remove("privacy_options_required") }
            Log.d(TAG, "testClearPrivacyOptionsRequired")
        } catch (_: Throwable) {}
    }

    /** For testing: force the debug settings. */
    fun testSetDebugSettings(context: Context, enabled: Boolean) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean("debug_settings_enabled", enabled) }
            Log.d(TAG, "testSetDebugSettings -> enabled=$enabled")
        } catch (_: Throwable) {}
    }

    /** For testing: clear the debug settings. */
    fun testClearDebugSettings(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { remove("debug_settings_enabled") }
            Log.d(TAG, "testClearDebugSettings")
        } catch (_: Throwable) {}
    }

    /** For testing: force the consent status to confirmed. */
    fun testConfirmConsent(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean(KEY_CONSENT_CHECKED, true); putBoolean(KEY_LAST_CAN_REQUEST, true) }
            Log.d(TAG, "testConfirmConsent -> consentChecked=true lastCanRequest=true")
        } catch (_: Throwable) {}
    }

    /** For testing: force the consent status to denied. */
    fun testDenyConsent(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean(KEY_CONSENT_CHECKED, true); putBoolean(KEY_LAST_CAN_REQUEST, false) }
            Log.d(TAG, "testDenyConsent -> consentChecked=true lastCanRequest=false")
        } catch (_: Throwable) {}
    }

    /** For testing: simulate a consent form error. */
    fun testSimulateConsentFormError(activity: Activity) {
        try {
            UserMessagingPlatform.loadConsentForm(activity,
                { consentForm -> Log.d(TAG, "testSimulateConsentFormError loaded form") },
                { loadError: FormError? -> Log.d(TAG, "testSimulateConsentFormError completed with error: ${loadError?.message}") }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "testSimulateConsentFormError failed: ${t.message}")
        }
    }

    /** For testing: simulate a privacy options form error. */
    fun testSimulatePrivacyOptionsFormError(activity: Activity) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError: FormError? ->
                Log.d(TAG, "testSimulatePrivacyOptionsFormError completed with error: ${formError?.message}")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "testSimulatePrivacyOptionsFormError failed: ${t.message}")
        }
    }

    /** For testing: force the form showing state. */
    fun testSetFormShowing(context: Context, showing: Boolean) {
        try {
            formShowing = showing
            Log.d(TAG, "testSetFormShowing -> showing=$showing")
        } catch (_: Throwable) {}
    }

    /** For testing: clear the form showing state. */
    fun testClearFormShowing(context: Context) {
        try {
            formShowing = false
            Log.d(TAG, "testClearFormShowing")
        } catch (_: Throwable) {}
    }

    /** For testing: force the consent status to unknown. */
    fun testSetConsentUnknown(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit { putBoolean(KEY_CONSENT_CHECKED, false) }
            Log.d(TAG, "testSetConsentUnknown -> consentChecked=false")
        } catch (_: Throwable) {}
    }

    /** For testing: trigger a consent info update. */
    fun testRequestConsentInfoUpdate(activity: Activity) {
        try {
            requestConsentInfoUpdate(activity) { result ->
                Log.d(TAG, "testRequestConsentInfoUpdate completed with result: $result")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "testRequestConsentInfoUpdate failed: ${t.message}")
        }
    }

    /** For testing: show the consent form. */
    fun testShowConsent(activity: Activity) {
        try {
            showConsentForm(activity) { result ->
                Log.d(TAG, "testShowConsent completed with result: $result")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "testShowConsent failed: ${t.message}")
        }
    }

    /** For testing: show the privacy options form. */
    fun testShowPrivacyOptions(activity: Activity) {
        try {
            showPrivacyOptionsForm(activity) { result ->
                Log.d(TAG, "testShowPrivacyOptions completed with result: $result")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "testShowPrivacyOptions failed: ${t.message}")
        }
    }

    /** For testing: print the debug settings. */
    fun testPrintDebugSettings(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = sp.getBoolean("debug_settings_enabled", false)
            Log.d(TAG, "testPrintDebugSettings -> enabled=$enabled")
        } catch (_: Throwable) {}
    }

    /** For testing: print the privacy options requirement status. */
    fun testPrintPrivacyOptionsRequired(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val required = sp.getBoolean("privacy_options_required", false)
            Log.d(TAG, "testPrintPrivacyOptionsRequired -> required=$required")
        } catch (_: Throwable) {}
    }

    /** Ensure ads-side consent is requested and, if allowed, ads are loaded. */
    fun requestAndLoadIfRequired(activity: android.app.Activity, onComplete: (Boolean) -> Unit = {}) {
        try {
            // If we've already checked consent, immediately notify and ensure any required loading is triggered
            if (consentChecked) {
                try {
                    // trigger preload if allowed
                    if (lastCanRequestAds) runCatching { InterstitialAdManager.preload(activity.applicationContext) }
                } catch (_: Throwable) {}
                try { onComplete(lastCanRequestAds) } catch (_: Throwable) {}
                return
            }

            // Otherwise request consent info update and act on the result
            requestConsentInfoUpdate(activity) { canRequest ->
                try { applyExternalConsent(activity.applicationContext, canRequest) } catch (_: Throwable) {}
                try { onComplete(canRequest) } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            try { onComplete(false) } catch (_: Throwable) {}
        }
    }

    // [NEW] MainActivity에서 사용하는 gatherConsent 메서드 (requestAndLoadIfRequired의 alias)
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        requestAndLoadIfRequired(activity, onComplete)
    }
}
