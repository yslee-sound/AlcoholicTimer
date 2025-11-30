package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.core.util.DebugSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.Exception
import kr.sweetapps.alcoholictimer.ads.UmpConsentManager as AdsUmpConsentManager

class UmpConsentManager(private val context: Context) {
    companion object {
        private const val TAG = "UmpConsentManager"
    }

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _isPersonalizedAdsAllowed = MutableStateFlow(false)
    val isPersonalizedAdsAllowed: StateFlow<Boolean> = _isPersonalizedAdsAllowed.asStateFlow()

    // Indicates whether a UMP form (consent or privacy options) is currently being shown
    @Volatile private var formShowing: Boolean = false

    // Prevent concurrent gatherConsent calls: queue callbacks while a gather is in progress
    @Volatile private var isGatheringConsent: Boolean = false
    private val pendingGatherCallbacks = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()

    init {
        updateConsentStatus()
    }

    fun isFormShowing(): Boolean = try { formShowing } catch (_: Throwable) { false }

    fun updateConsentStatus() {
        _isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val purposeConsents = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val hasConsentedToPurpose1 = purposeConsents.isNotEmpty() && purposeConsents[0] == '1'
        
        _isPersonalizedAdsAllowed.value = hasConsentedToPurpose1
        // Persist minimal ads-side flags so ads manager can read them later
        try {
            val sp = context.getSharedPreferences("ump_prefs", Context.MODE_PRIVATE)
            sp.edit { putBoolean("consent_checked", true); putBoolean("last_can_request_ads", hasConsentedToPurpose1) }
            Log.d(TAG, "updateConsentStatus -> wrote ads prefs consent=$hasConsentedToPurpose1")
        } catch (_: Throwable) {}
    }

    fun gatherConsent(activity: Activity, onConsentGathered: (canInitializeAds: Boolean) -> Unit) {
        // Only apply ConsentDebugSettings when running a DEBUG build and the debug toggle is enabled.
        val params = try {
            val builder = ConsentRequestParameters.Builder()
            if (BuildConfig.DEBUG && DebugSettings.isUmpForceEeaEnabled(context)) {
                val debugBuilder = ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                // BuildConfig.UMP_TEST_DEVICE_HASH가 비어있지 않다면 테스트 기기 해시를 추가
                // Collect hashes from BuildConfig and from persisted ump_prefs (if SDK recorded one)
                try {
                    val hashSet = mutableSetOf<String>()
                    try {
                        val cfg = try { BuildConfig.UMP_TEST_DEVICE_HASH } catch (_: Throwable) { "" }
                        if (!cfg.isNullOrBlank()) {
                            cfg.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { hashSet.add(it) }
                        }
                    } catch (_: Throwable) {}
                    try {
                        val sp = context.getSharedPreferences("ump_prefs", Context.MODE_PRIVATE)
                        val stored = sp.getString("test_device_hash", "") ?: ""
                        if (stored.isNotBlank()) hashSet.add(stored)
                    } catch (_: Throwable) {}

                    for (h in hashSet) {
                        try { debugBuilder.addTestDeviceHashedId(h) } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
                 builder.setConsentDebugSettings(debugBuilder.build())
             }
             builder.build()
         } catch (_: Throwable) {
             ConsentRequestParameters.Builder().build()
         }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { 
                // Deduplicate concurrent gather requests: queue callbacks while gathering
                if (isGatheringConsent) {
                    try { pendingGatherCallbacks.add(onConsentGathered) } catch (_: Throwable) {}
                    return@requestConsentInfoUpdate
                }
                isGatheringConsent = true
                // If a form is available, loadAndShowConsentFormIfRequired may show it. Mark full-screen state to prevent AppOpen overlap.
                try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                // Mark internal flag to indicate form is showing
                formShowing = true
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    // After form dismissed, re-query consent info so consentInformation reflects latest response
                    try {
                        consentInformation.requestConsentInfoUpdate(activity, params,
                            {
                                // success: update local state and sync ads proxy
                                try { updateConsentStatus() } catch (_: Throwable) {}
                                try {
                                    val can = try { consentInformation.canRequestAds() } catch (_: Throwable) { false }
                                    AdsUmpConsentManager.applyExternalConsent(activity.applicationContext, can)
                                } catch (_: Throwable) {}
                                // Clear full-screen flag now that form handling and sync complete
                                try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                                // clear internal flag
                                formShowing = false
                                // notify queued gather callers and reset gathering flag
                                try { for (cb in pendingGatherCallbacks) runCatching { cb.invoke(try { consentInformation.canRequestAds() } catch (_: Throwable) { false }) } } catch (_: Throwable) {}
                                pendingGatherCallbacks.clear()
                                isGatheringConsent = false
                                if (loadAndShowError != null) {
                                    Log.w(TAG, "Form load/show error: ${loadAndShowError.errorCode} - ${loadAndShowError.message}")
                                    onConsentGathered(false)
                                    return@requestConsentInfoUpdate
                                }
                                onConsentGathered(try { consentInformation.canRequestAds() } catch (_: Throwable) { false })
                            },
                            { requestError ->
                                // refresh failed: still clear flags and return conservative result
                                Log.w(TAG, "Post-form consent info update failed: ${requestError.message}")
                                try { updateConsentStatus() } catch (_: Throwable) {}
                                try {
                                    val can = try { consentInformation.canRequestAds() } catch (_: Throwable) { false }
                                    AdsUmpConsentManager.applyExternalConsent(activity.applicationContext, can)
                // notify queued callers
                try { for (cb in pendingGatherCallbacks) runCatching { cb.invoke(false) } } catch (_: Throwable) {}
                pendingGatherCallbacks.clear()
                isGatheringConsent = false
                                } catch (_: Throwable) {}
                                try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                                formShowing = false
        // When successful path completes, we clear pendingGatherCallbacks in handlePostFormDismiss to ensure callers are notified there.
                                onConsentGathered(false)
                            }
                        )
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to refresh consent info after form: ${t.message}")
                        try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                        formShowing = false
                        onConsentGathered(false)
                    }
                }
            },
            {
                requestConsentError ->
                Log.w(TAG, "Consent info update error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                updateConsentStatus()
                try {
                    val can = try { consentInformation.canRequestAds() } catch (_: Throwable) { false }
                    AdsUmpConsentManager.applyExternalConsent(activity.applicationContext, can)
                } catch (_: Throwable) {}
                onConsentGathered(false)
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onFormError: (Exception) -> Unit) {
        if (consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
            try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
            formShowing = true
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                // After privacy options form dismissed, re-query consent info and sync ads proxy
                try {
                    val paramsBuilder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
                    val paramsLocal = try {
                        if (BuildConfig.DEBUG && DebugSettings.isUmpForceEeaEnabled(activity.applicationContext)) {
                            val dbg = ConsentDebugSettings.Builder(activity).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA).build()
                            paramsBuilder.setConsentDebugSettings(dbg).build()
                        } else paramsBuilder.build()
                    } catch (_: Throwable) { paramsBuilder.build() }

                    consentInformation.requestConsentInfoUpdate(activity, paramsLocal,
                        {
                            // success: update local state and sync ads proxy
                            try { updateConsentStatus() } catch (_: Throwable) {}
                            try {
                                val can = try { consentInformation.canRequestAds() } catch (_: Throwable) { false }
                                AdsUmpConsentManager.applyExternalConsent(activity.applicationContext, can)
                            } catch (_: Throwable) {}
                            try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                            formShowing = false
                            // If the privacy options form reported an error, surface it to caller
                            if (formError != null) {
                                try { onFormError(Exception("Privacy options form error: ${formError.message}")) } catch (_: Throwable) {}
                            }
                        },
                        { err ->
                            // failure: still attempt to update local state conservatively
                            try { updateConsentStatus() } catch (_: Throwable) {}
                            try {
                                val can = try { consentInformation.canRequestAds() } catch (_: Throwable) { false }
                                AdsUmpConsentManager.applyExternalConsent(activity.applicationContext, can)
                            } catch (_: Throwable) {}
                            try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                            formShowing = false
                            if (formError != null) {
                                try { onFormError(Exception("Privacy options form error: ${formError.message}")) } catch (_: Throwable) {}
                            } else {
                                try { onFormError(Exception("Privacy options post-update failed: ${err.message}")) } catch (_: Throwable) {}
                            }
                        }
                    )
                } catch (t: Throwable) {
                    try { kr.sweetapps.alcoholictimer.ads.AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
                    formShowing = false
                    try { onFormError(Exception("Privacy options form handling failed: ${t.message}")) } catch (_: Throwable) {}
                }
            }
        } else {
            try { onFormError(Exception("Privacy options form not available.")) } catch (_: Throwable) {}
        }
    }

    // 새로 추가: Primary consent manager에서 동의 상태를 강제로 초기화하여 디버그/테스트 시 재요청이 가능하도록 합니다.
    fun resetConsent() {
        try {
            // 기본(앱) SharedPreferences에 저장된 IAB TC 데이터 제거
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit { remove("IABTCF_PurposeConsents") }
        } catch (_: Throwable) {}

        try {
            // ads 측에서 사용하는 ump_prefs도 보수적으로 재설정
            val sp = context.getSharedPreferences("ump_prefs", Context.MODE_PRIVATE)
            sp.edit { putBoolean("consent_checked", false); putBoolean("last_can_request_ads", false) }
        } catch (_: Throwable) {}

        try {
            // 내부 상태 갱신
            updateConsentStatus()
        } catch (_: Throwable) {}
    }

}
