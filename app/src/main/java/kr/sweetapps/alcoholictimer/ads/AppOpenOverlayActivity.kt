package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager

/**
 * Transparent overlay Activity whose sole job is to host/show the App Open ad
 * so that the ad appears visually above the splash screen (which remains visible
 * under this translucent activity).
 *
 * Flow:
 * - SplashScreen keeps the splash visible.
 * - When an app-open ad is ready, SplashScreen starts this overlay activity.
 * - The overlay activity calls AppOpenAdManager.showIfAvailable(this).
 *   - If the ad is shown, AppOpenAdManager's callbacks will notify listeners (SplashScreen)
 *     and the overlay activity will simply wait for the ad to finish.
 *   - If no ad is available or showing failed, this activity finishes immediately
 *     to allow SplashScreen to continue.
 */
class AppOpenOverlayActivity : Activity() {
    private val TAG = "AppOpenOverlayActivity"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val fallbackFinishDelayMs = 1500L
    private val finishRunnable = Runnable {
        try {
            Log.d(TAG, "finishRunnable: no ad shown -> finishing overlay")
            finish()
        } catch (_: Throwable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Ensure the window is translucent/transparent and doesn't dim underlying splash
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Don't allow this activity to appear in recents
            try { if (intent != null) { setFinishOnTouchOutside(false) } } catch (_: Throwable) {}
        } catch (t: Throwable) {
            Log.w(TAG, "onCreate window setup failed: $t")
        }

        // Try to show a preloaded app-open ad. If not shown, finish quickly.
        mainHandler.post {
            try {
                val shown = runCatching { AppOpenAdManager.showIfAvailable(this) }.getOrDefault(false)
                Log.d(TAG, "Attempted showIfAvailable from overlay -> returned=$shown")
                if (!shown) {
                    // If ad could not be shown, schedule immediate finish
                    finish()
                    return@post
                }
                // If ad is shown, schedule a safety finish in case callbacks are not invoked
                mainHandler.postDelayed(finishRunnable, fallbackFinishDelayMs * 10)
            } catch (t: Throwable) {
                Log.w(TAG, "showIfAvailable call failed: $t")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { mainHandler.removeCallbacks(finishRunnable) } catch (_: Throwable) {}
    }

    override fun onPause() {
        super.onPause()
        // If the overlay loses focus but is still running, we don't want it to hold the app.
        // However do not auto-finish here; let ad callbacks control lifecycle.
    }
}

