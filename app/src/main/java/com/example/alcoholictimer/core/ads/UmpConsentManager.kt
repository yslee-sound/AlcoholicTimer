package com.sweetapps.alcoholictimer.core.ads

import android.app.Activity
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

    /**
     * Requests consent flow and returns whether ads can be requested afterwards.
     * Safe to call at activity start; shows form only when required.
     */
    fun requestAndLoadIfRequired(
        activity: Activity,
        tagForUnderAgeOfConsent: Boolean = false,
        onFinished: (canRequestAds: Boolean) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent)
            .build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        fun finishFromInfo(info: ConsentInformation) {
            val canRequest = info.canRequestAds()
            Log.d(TAG, "UMP finished. canRequestAds=$canRequest, status=${info.consentStatus}")
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

