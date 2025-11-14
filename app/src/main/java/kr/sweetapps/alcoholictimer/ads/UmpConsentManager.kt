package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Minimal UMP gating helper.
 * - Requests consent info update.
 * - If required, loads and shows the consent form.
 * - Calls back with whether we can request ads afterwards.
 */
object UmpConsentManager {
    private const val TAG = "UmpConsentManager"

    /** Returns true if the app must provide a privacy options entry point (EEA regions, etc.). */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            val status = UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus
            status == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (t: Throwable) {
            Log.w(TAG, "isPrivacyOptionsRequired check failed: ${t.message}")
            false
        }
    }

    /** Opens the UMP privacy options form. Safe to call from UI thread. */
    fun showPrivacyOptionsForm(activity: Activity, onClosed: (error: FormError?) -> Unit = {}) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Log.w(TAG, "Privacy options form error: code=${formError.errorCode}, msg=${formError.message}")
                } else {
                    Log.d(TAG, "Privacy options form closed successfully")
                }
                onClosed(formError)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "showPrivacyOptionsForm failed: ${t.message}")
            onClosed(null)
        }
    }

    /**
     * Requests consent flow and returns whether ads can be requested afterwards.
     * Safe to call at activity start; shows form only when required.
     */
    fun requestAndLoadIfRequired(
        activity: Activity,
        tagForUnderAgeOfConsent: Boolean = false,
        onFinished: (canRequestAds: Boolean) -> Unit
    ) {
        Log.d(TAG, "requestAndLoadIfRequired called from ${activity::class.java.simpleName}")
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent)
            .build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        Log.d(TAG, "ConsentInfo status before update: ${consentInformation.consentStatus}")

        fun finishFromInfo(info: ConsentInformation) {
            val canRequest = info.canRequestAds()
            Log.d(TAG, "UMP finished. canRequestAds=$canRequest, status=${info.consentStatus}")
            Log.d(TAG, "Calling AppOpenAdManager.onConsentUpdated($canRequest)")
            // App Open 광고 매니저에 동의 업데이트 전달
            AppOpenAdManager.onConsentUpdated(canRequest)
            onFinished(canRequest)
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Try showing the form if required.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: code=${formError.errorCode}, msg=${formError.message}")
                    }
                    finishFromInfo(consentInformation)
                }
            },
            { error ->
                Log.w(TAG, "requestConsentInfoUpdate error: code=${error.errorCode}, msg=${error.message}")
                finishFromInfo(consentInformation)
            }
        )
    }
}
