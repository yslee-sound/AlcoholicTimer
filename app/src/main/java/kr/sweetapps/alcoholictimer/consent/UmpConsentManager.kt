package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.Exception

class UmpConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _isPersonalizedAdsAllowed = MutableStateFlow(false)
    val isPersonalizedAdsAllowed: StateFlow<Boolean> = _isPersonalizedAdsAllowed.asStateFlow()

    init {
        updateConsentStatus()
    }

    fun updateConsentStatus() {
        _isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val purposeConsents = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val hasConsentedToPurpose1 = purposeConsents.isNotEmpty() && purposeConsents[0] == '1'
        
        _isPersonalizedAdsAllowed.value = hasConsentedToPurpose1
    }

    fun gatherConsent(activity: Activity, onConsentGathered: (canInitializeAds: Boolean) -> Unit) {
        val testDeviceId = "44A19A7AB27DC2FEEC73259C8D892E01"
        val debugSettings = ConsentDebugSettings.Builder(context)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(testDeviceId)
            .build()

        val params = if (BuildConfig.DEBUG) {
            ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build()
        } else {
            ConsentRequestParameters.Builder().build()
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { 
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    updateConsentStatus()
                    if (loadAndShowError != null) {
                        Log.w(TAG, "Form load/show error: ${loadAndShowError.errorCode} - ${loadAndShowError.message}")
                        onConsentGathered(false)
                        return@loadAndShowConsentFormIfRequired
                    }
                    onConsentGathered(consentInformation.canRequestAds())
                }
            },
            { 
                requestConsentError ->
                Log.w(TAG, "Consent info update error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                updateConsentStatus()
                onConsentGathered(false)
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onFormError: (Exception) -> Unit) {
        if (consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                updateConsentStatus()
                if (formError != null) {
                    onFormError(Exception("Privacy options form error: ${formError.message}"))
                }
            }
        } else {
            onFormError(Exception("Privacy options form not available."))
        }
    }

    fun resetConsent() {
        consentInformation.reset()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()
        updateConsentStatus()
    }

    companion object {
        private const val TAG = "UmpConsentManager"
    }
}