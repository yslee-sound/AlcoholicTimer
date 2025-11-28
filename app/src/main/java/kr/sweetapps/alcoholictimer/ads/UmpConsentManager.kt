@file:Suppress("UNUSED_PARAMETER")
package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.ump.ConsentInformation
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * UMP 통합 구현체
 * - 실제 UMP SDK를 사용하여 consent 정보 조회/폼 표시/응답 전달을 수행합니다.
 */
object UmpConsentManager {
    private const val TAG = "UmpConsentManager"

    // Persistent prefs keys
    private const val PREFS_NAME = "ump_prefs"
    private const val KEY_CONSENT_CHECKED = "consent_checked"
    private const val KEY_LAST_CAN_REQUEST = "last_can_request_ads"

    // Indicates whether consent info has been queried at least once (requestConsentInfoUpdate completed)
    @Volatile var consentChecked: Boolean = false
        private set

    // Last known value of canRequestAds after consent check. Default to false to be conservative.
    @Volatile var lastCanRequestAds: Boolean = false
        private set

    // Prevent concurrent updates / queue callbacks
    @Volatile private var isUpdating: Boolean = false
    private val pendingCallbacks = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()

    // External listeners (UI etc.) that want to be notified when consent state changes
    private val consentListeners = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()

    /** Register a listener to be notified when consent (canRequestAds) changes. */
    fun addConsentChangeListener(listener: (Boolean) -> Unit) {
        try { consentListeners.add(listener) } catch (_: Throwable) {}
    }

    /** Unregister a previously registered consent change listener. */
    fun removeConsentChangeListener(listener: (Boolean) -> Unit) {
        try { consentListeners.remove(listener) } catch (_: Throwable) {}
    }

    // 새로 추가: 현재 동의 폼이 화면에 표시 중인지 여부. AppOpen 광고가 동의 폼 위에 표시되는 것을 방지하기 위해 사용.
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
            sp.edit().putBoolean(KEY_CONSENT_CHECKED, true).putBoolean(KEY_LAST_CAN_REQUEST, canRequest).apply()
            Log.d(TAG, "saveToPrefs -> consentChecked=true lastCanRequest=$canRequest")
        } catch (_: Throwable) {}
    }

    // Finish update: persist and notify queued callbacks
    private fun finishUpdate(activity: Activity, canRequest: Boolean) {
        try {
            consentChecked = true
            lastCanRequestAds = canRequest
            saveToPrefs(activity.applicationContext, canRequest)
            // 동의 허용 시(또는 디버그 모드) 인터스티셜 미리로드 시도
            try {
                val debugMode = try { kr.sweetapps.alcoholictimer.BuildConfig.DEBUG } catch (_: Throwable) { false }
                if (canRequest || debugMode) {
                    runCatching { InterstitialAdManager.preload(activity.applicationContext) }
                    android.util.Log.d(TAG, "finishUpdate -> triggered InterstitialAdManager.preload (canRequest=$canRequest debug=$debugMode)")
                }
            } catch (_: Throwable) {}
            // notify AppOpenAdManager and queued callers
            try { AppOpenAdManager.onConsentUpdated(canRequest) } catch (_: Throwable) {}
            // notify external listeners (UI) about consent change on main thread
            try {
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                mainHandler.post {
                    try { for (l in consentListeners) runCatching { l.invoke(canRequest) } } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
             for (cb in pendingCallbacks) runCatching { cb.invoke(canRequest) }
        } finally {
            pendingCallbacks.clear()
            isUpdating = false
        }
    }

    /** Returns true if the app must provide a privacy options entry point (EEA regions, etc.). */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(context)
            // 정책 권고: privacyOptionsRequirementStatus가 REQUIRED인 경우에만 앱 내 진입점(Privacy Options) 노출 권장
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (t: Throwable) {
            Log.w(TAG, "isPrivacyOptionsRequired failed: ${t.message}")
            false
        }
    }

    /** 새로운 API: 현재 동의 폼이 표시 중인지 반환합니다. */
    fun isFormShowing(): Boolean = try { formShowing } catch (_: Throwable) { false }

    /** Opens the UMP privacy options form. Safe to call from UI thread. */
    fun showPrivacyOptionsForm(activity: Activity, onClosed: (Any?) -> Unit = {}) {
        // Ensure that consent info is up-to-date before attempting to load the form.
        // Some devices/regions require requestConsentInfoUpdate() before isConsentFormAvailable becomes true.
        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            // If form already available, load immediately.
            if (consentInformation.isConsentFormAvailable) {
                loadAndShowForm(activity, onClosed)
                return
            }

            // Otherwise request consent info update first, then try to load form.
            val paramsBuilder = com.google.android.ump.ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
            if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                try {
                    val dbg = com.google.android.ump.ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    // If a test device hash was registered in prefs, add it so debug builds show the form
                    try {
                        val sp = activity.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val testHash = sp.getString("test_device_hash", null)
                        if (!testHash.isNullOrBlank()) {
                            try { dbg.addTestDeviceHashedId(testHash) } catch (_: Throwable) {}
                        }
                    } catch (_: Throwable) {}
                    paramsBuilder.setConsentDebugSettings(dbg.build())
                    Log.d(TAG, "Applied ConsentDebugSettings for debug geography (EEA).")
                } catch (_: Throwable) {}
            }

            val params = paramsBuilder.build()

            consentInformation.requestConsentInfoUpdate(activity, params,
                {
                    // success - try loading form if available
                    try {
                        if (consentInformation.isConsentFormAvailable) {
                            loadAndShowForm(activity, onClosed)
                        } else {
                            Log.d(TAG, "showPrivacyOptionsForm: consent form not available after info update")
                            onClosed(null)
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "showPrivacyOptionsForm processing failed: ${t.message}")
                        onClosed(null)
                    }
                },
                { formError ->
                    Log.w(TAG, "showPrivacyOptionsForm: requestConsentInfoUpdate failed: ${formError.message}")
                    onClosed(null)
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "showPrivacyOptionsForm failed: ${t.message}")
            onClosed(null)
        }
    }

    // Helper: load consent form and show it, with flags and error handling
    private fun loadAndShowForm(activity: Activity, onClosed: (Any?) -> Unit) {
        try {
            UserMessagingPlatform.loadConsentForm(activity,
                { consentForm ->
                    Log.d(TAG, "showPrivacyOptionsForm: loaded, showing")
                    formShowing = true
                    try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                    consentForm.show(activity) {
                        Log.d(TAG, "showPrivacyOptionsForm: dismissed")
                        // After the form is dismissed, re-query consent info and call finishUpdate to persist and notify UI
                        try {
                            val info = UserMessagingPlatform.getConsentInformation(activity)
                            val finalCanRequest = try { info.canRequestAds() } catch (_: Throwable) { false }
                            Log.d(TAG, "showPrivacyOptionsForm: dismissed -> canRequestAds=$finalCanRequest")
                            // Ensure we persist and notify UI listeners
                            finishUpdate(activity, finalCanRequest)
                        } catch (_: Throwable) {
                            // Fallback: clear flags and notify caller
                            formShowing = false
                            try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                        }
                        // clear formShowing & restore ad controller (finishUpdate may also do this)
                        formShowing = false
                        try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                        onClosed(null)
                    }
                },
                { formError ->
                    Log.w(TAG, "showPrivacyOptionsForm: load error=${formError.message}")
                    try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    onClosed(null)
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "loadAndShowForm failed: ${t.message}")
            try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
            onClosed(null)
        }
    }

    /**
     * Requests consent flow and returns whether ads can be requested afterwards.
     * AdvertisingId 조회는 디버그에서만 시도하며 별도 스레드 + 타임아웃으로 수행해 앱 시작 지연을 방지합니다.
     */
    fun requestAndLoadIfRequired(
        activity: Activity,
        tagForUnderAgeOfConsent: Boolean = false,
        onFinished: (Boolean) -> Unit
    ) {
        Log.d(TAG, "requestAndLoadIfRequired start (debug=${BuildConfig.DEBUG})")
        // load persisted state first if available
        loadFromPrefs(activity.applicationContext)

        // If we already checked, return cached result immediately
        if (consentChecked) {
            Log.d(TAG, "requestAndLoadIfRequired: already checked -> returning cached=$lastCanRequestAds")
            runCatching { onFinished(lastCanRequestAds) }
            return
        }

        // Deduplicate concurrent callers: queue callback and return if an update is already in progress
        pendingCallbacks.add(onFinished)
        if (isUpdating) {
            Log.d(TAG, "requestAndLoadIfRequired: update already in progress -> queued callback")
            return
        }
        isUpdating = true

        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            val paramsBuilder = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent)

            if (BuildConfig.DEBUG) {
                val debugBuilder = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                // 테스트 디바이스 해시를 추가하려면 addTestDeviceHashedId("HASH") 사용
                paramsBuilder.setConsentDebugSettings(debugBuilder.build())
                // If a test device hash was registered in prefs, add it so debug builds show the form
                try {
                    val sp = activity.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val testHash = sp.getString("test_device_hash", null)
                    if (!testHash.isNullOrBlank()) {
                        try { debugBuilder.addTestDeviceHashedId(testHash) } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
                Log.d(TAG, "Applied ConsentDebugSettings for debug geography (EEA).")
            }

            val params = paramsBuilder.build()

            consentInformation.requestConsentInfoUpdate(activity, params,
                {
                    // success
                    try {
                        val canRequest = try { consentInformation.canRequestAds() } catch (_: Throwable) { true }
                        Log.d(TAG, "requestConsentInfoUpdate success. isConsentFormAvailable=${consentInformation.isConsentFormAvailable}, canRequestAds=$canRequest")

                        if (consentInformation.isConsentFormAvailable) {
                            UserMessagingPlatform.loadConsentForm(activity,
                                { consentForm ->
                                    Log.d(TAG, "consentForm loaded, showing")
                                    // 폼 표시 전 플래그 설정
                                    formShowing = true
                                    try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                                    consentForm.show(activity) {
                                        val finalCanRequest = try { consentInformation.canRequestAds() } catch (_: Throwable) { true }
                                        Log.d(TAG, "consentForm dismissed. canRequestAds=$finalCanRequest")
                                        // 폼이 닫혔으므로 플래그 클리어
                                        formShowing = false
                                        try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                                        finishUpdate(activity, finalCanRequest)
                                    }
                                },
                                { formError ->
                                    Log.w(TAG, "loadConsentForm error: ${formError.message}")
                                    val curCanRequest = try { consentInformation.canRequestAds() } catch (_: Throwable) { false }
                                    formShowing = false
                                    try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                                    finishUpdate(activity, curCanRequest)
                                }
                            )
                        } else {
                            // No form to show; continue with current consent state
                            finishUpdate(activity, canRequest)
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "requestAndLoadIfRequired processing failed: ${t.message}")
                        try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                        finishUpdate(activity, true)
                    }
                },
                { formError ->
                    // failure
                    Log.w(TAG, "requestConsentInfoUpdate error: ${formError.message}")
                    val fallback = try { consentInformation.canRequestAds() } catch (_: Throwable) { true }
                    try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    finishUpdate(activity, fallback)
                }
            )

        } catch (t: Throwable) {
            Log.w(TAG, "requestAndLoadIfRequired failed: ${t.message}")
            // unexpected failure: mark as checked and default conservatively to false (no personalized)
            try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
            finishUpdate(activity, false)
        }
    }
}
