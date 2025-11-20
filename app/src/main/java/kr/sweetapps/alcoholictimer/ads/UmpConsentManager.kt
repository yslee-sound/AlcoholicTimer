package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Minimal UMP gating helper stub.
 * - Keeps API surface to avoid changing callers, but delegates to AppOpenAdManager when present.
 */
object UmpConsentManager {
    private const val TAG = "UmpConsentManager"

    /** Returns true if the app must provide a privacy options entry point (EEA regions, etc.). */
    fun isPrivacyOptionsRequired(context: Context): Boolean = false

    /** Opens the UMP privacy options form. Safe to call from UI thread. */
    fun showPrivacyOptionsForm(activity: Activity, onClosed: (Any?) -> Unit = {}) {
        Log.d(TAG, "Stub showPrivacyOptionsForm called")
        onClosed(null)
    }

    /**
     * Requests consent flow and returns whether ads can be requested afterwards.
     * Safe to call at activity start; shows form only when required.
     */
    fun requestAndLoadIfRequired(
        activity: Activity,
        tagForUnderAgeOfConsent: Boolean = false,
        onFinished: (Boolean) -> Unit
    ) {
        Log.d(TAG, "Stub requestAndLoadIfRequired called")
        // call AppOpenAdManager.onConsentUpdated(false) conservatively
        try { AppOpenAdManager.onConsentUpdated(false) } catch (_: Throwable) {}
        onFinished(false)
    }
}

