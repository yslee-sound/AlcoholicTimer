package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Stubbed InterstitialAdManager: no-op implementation that preserves API
 */
object InterstitialAdManager {
    private const val TAG = "InterstitialAdManager"
    fun preload(context: Context) { Log.d(TAG, "Stub preload called") }
    fun addLoadListener(listener: (Boolean) -> Unit) { /* no-op */ }
    fun isLoaded(): Boolean = false
    fun clearLoadedAd() {}
    fun maybeShowIfEligible(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean {
        Log.d(TAG, "maybeShowIfEligible called - forcing debug interstitial for testing")
        return try {
            forceShowDebug(activity, onDismiss)
        } catch (t: Throwable) {
            Log.e(TAG, "maybeShowIfEligible failed", t)
            false
        }
    }
    fun isShowingAd(): Boolean = false
    fun resetColdStartGate() {}

    // Added to satisfy MainApplication.noteAppStart() call
    fun noteAppStart() { Log.d(TAG, "noteAppStart called (stub)") }

    // DEBUG: force-show a simple dialog that simulates an interstitial ad for local testing.
    // Returns true if dialog shown.
    fun forceShowDebug(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean {
        return try {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Debug Interstitial")
                .setMessage("This simulates an interstitial ad for testing. Press Close to continue.")
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ ->
                    try { onDismiss?.invoke() } catch (_: Throwable) {}
                }
                .show()
            Log.d(TAG, "forceShowDebug: shown")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "forceShowDebug failed", t)
            false
        }
    }
}
