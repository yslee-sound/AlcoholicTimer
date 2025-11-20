package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log

/**
 * Stubbed AppOpenAdManager (no-op)
 * Backed up original to app/ads_backup/
 */
object AppOpenAdManager {
    private const val TAG = "AppOpenAdManager"
    fun initialize(application: Application, registerLifecycle: Boolean = true) {
        Log.d(TAG, "Stub initialize called")
    }
    fun noteAppStart() {}
    fun setAutoShowEnabled(enabled: Boolean) {}
    fun preload(context: Context) {}
    fun isLoaded(): Boolean = false
    fun isShowingAd(): Boolean = false
    fun clearLoadedAd() {}
    fun setOnAdFinishedListener(listener: (() -> Unit)?) {}
    fun setOnAdLoadedListener(listener: (() -> Unit)?) {}
    fun setOnAdShownListener(listener: (() -> Unit)?) {}
    fun onConsentUpdated(canRequestAds: Boolean) {}

    /**
     * Compatibility shim: shows an app-open ad if one is loaded and appropriate.
     * Stub returns false (no ad shown) so callers can continue normally.
     */
    fun showIfAvailable(activity: Activity): Boolean {
        Log.d(TAG, "showIfAvailable called (stub) - no ad to show")
        return false
    }
}
