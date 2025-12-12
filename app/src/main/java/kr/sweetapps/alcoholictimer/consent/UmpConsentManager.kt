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
import kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager

/**
 * UMP 통합 구현체
 * - 실제 UMP SDK를 사용하여 consent 정보 조회/폼 표시/응답 전달을 실행합니다
 * - [SIMPLIFIED] 상용 배포용 표준 UMP 로직만 유지
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

            // [REMOVED] 무단 광고 표시 코드 제거 - MainActivity만 광고 표시 제어
            // 이유: 앱 화면 위에 광고가 덮어씌워지는 정책 위반 방지

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

        // [NEW] UMP 디버그 설정 (테스트 기기 ID가 있을 때만)
        try {
            val paramsBuilder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)

            // Debug 빌드에서 테스트 기기 ID가 존재하면 디버그 설정 적용
            if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                val testDeviceId = kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH

                if (testDeviceId.isNotBlank()) {
                    val debugSettings = com.google.android.ump.ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // 유럽 강제
                        .addTestDeviceHashedId(testDeviceId) // local.properties에서 가져온 ID
                        .build()

                    paramsBuilder.setConsentDebugSettings(debugSettings)
                    android.util.Log.d(TAG, "✅ 유럽(EEA) 테스트 모드 강제 적용 (Device: $testDeviceId)")
                } else {
                    android.util.Log.d(TAG, "ℹ️ 일반 모드 작동 (UMP_TEST_DEVICE_HASH 없음 - local.properties 확인)")
                }
            }

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

    // [REMOVED] resetConsent 및 모든 테스트 메서드 제거 - 상용 배포에 불필요

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

    // [NEW] MainActivity/SplashScreen에서 사용하는 gatherConsent 메서드
    // Google 권장 표준 흐름: Request Info Update -> Load and Show Form If Required
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        try {
            val paramsBuilder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)

            // Debug 빌드에서 테스트 기기 ID가 존재하면 디버그 설정 적용
            if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                val testDeviceId = kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH

                if (testDeviceId.isNotBlank()) {
                    val debugSettings = ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .addTestDeviceHashedId(testDeviceId)
                        .build()

                    paramsBuilder.setConsentDebugSettings(debugSettings)
                    Log.d(TAG, "✅ 유럽(EEA) 테스트 모드 강제 적용 (Device: $testDeviceId)")
                } else {
                    Log.d(TAG, "ℹ️ 일반 모드 작동 (UMP_TEST_DEVICE_HASH 없음)")
                }
            }

            val params = paramsBuilder.build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            // STEP 1: Request consent info update
            Log.d(TAG, "========================================")
            Log.d(TAG, "STEP 1: Requesting consent info update")
            Log.d(TAG, "========================================")

            consentInformation.requestConsentInfoUpdate(activity, params,
                { // onSuccess
                    Log.d(TAG, "✅ Consent info update successful")

                    // STEP 2: Load and show consent form if required (핵심 단계!)
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "STEP 2: Loading and showing consent form if required")
                    Log.d(TAG, "========================================")

                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.e(TAG, "❌ Consent form error: ${formError.message}")
                        } else {
                            Log.d(TAG, "✅ Consent form completed or not required")
                        }

                        // STEP 3: Check final consent status
                        val status = consentInformation.consentStatus
                        val canRequest = status == ConsentInformation.ConsentStatus.OBTAINED ||
                                       status == ConsentInformation.ConsentStatus.NOT_REQUIRED

                        Log.d(TAG, "========================================")
                        Log.d(TAG, "STEP 3: Final consent status")
                        Log.d(TAG, "Status: $status, canRequestAds: $canRequest")
                        Log.d(TAG, "========================================")

                        // Save and notify
                        consentChecked = true
                        lastCanRequestAds = canRequest
                        saveToPrefs(activity.applicationContext, canRequest)
                        applyExternalConsent(activity.applicationContext, canRequest)

                        onComplete(canRequest)
                    }
                },
                { formError -> // onFailure
                    Log.e(TAG, "❌ Consent info update failed: ${formError?.message}")
                    consentChecked = true
                    lastCanRequestAds = false
                    saveToPrefs(activity.applicationContext, false)
                    onComplete(false)
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "❌ gatherConsent failed: ${t.message}", t)
            onComplete(false)
        }
    }

    // [NEW] 테스트용: 동의 상태 강제 리셋 (개발 중에만 사용)
    fun resetConsent(context: Context) {
        if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            Log.w(TAG, "⚠️ resetConsent called in non-debug build - ignoring")
            return
        }

        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(context)
            consentInformation.reset()

            // Clear local state
            consentChecked = false
            lastCanRequestAds = false
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().clear().apply()

            Log.d(TAG, "🔄 Consent state reset successfully (DEBUG mode)")
        } catch (t: Throwable) {
            Log.e(TAG, "❌ Failed to reset consent: ${t.message}", t)
        }
    }
}
