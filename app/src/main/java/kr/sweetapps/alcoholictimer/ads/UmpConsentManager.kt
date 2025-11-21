package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Minimal UMP gating helper stub.
 * - Keeps API surface to avoid changing callers, but delegates to AppOpenAdManager when present.
 *
 * NOTE: For local testing we optimistically allow ad requests by returning `onFinished(true)`.
 * In production replace this with a real UMP flow.
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
        Log.d(TAG, "Stub requestAndLoadIfRequired called (test-mode: allowing ads)")
        // Inform AppOpenAdManager about consent state (optimistic test-mode=true)
        try { AppOpenAdManager.onConsentUpdated(true) } catch (_: Throwable) {}
        // Notify caller that ads can be requested so preload() will run in SplashScreen
        onFinished(true)
    }
}
