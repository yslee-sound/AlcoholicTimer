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
    fun maybeShowIfEligible(activity: Activity, onDismiss: (() -> Unit)? = null): Boolean { return false }
    fun isShowingAd(): Boolean = false
    fun resetColdStartGate() {}

    // Added to satisfy MainApplication.noteAppStart() call
    fun noteAppStart() { Log.d(TAG, "noteAppStart called (stub)") }
}
