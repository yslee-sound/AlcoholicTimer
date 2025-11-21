package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import kr.sweetapps.alcoholictimer.R

/**
 * Transparent overlay Activity whose sole job is to host/show the App Open ad
 * so that the ad appears visually above the splash screen (which remains visible
 * under this translucent activity).
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

    private val adFinishedListener: (() -> Unit) = {
        Log.d(TAG, "adFinishedListener: ad finished -> finishing overlay")
        // Ensure finish runs on main thread
        mainHandler.post {
            try { finish() } catch (_: Throwable) {}
        }
    }
    private val adLoadFailedListener: (() -> Unit) = {
        Log.w(TAG, "adLoadFailedListener: ad failed to load -> finishing overlay")
        mainHandler.post {
            try { finish() } catch (_: Throwable) {}
        }
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

        // Inflate a simple overlay layout with an optional top 'Continue to app' button.
        try {
            // Use direct setContentView(resource) to avoid inflate(..., null) warning
            setContentView(R.layout.activity_app_open_overlay)

            // No manual top button wiring; ad provides its own 'continue' UI. Overlay layout intentionally
            // contains no top continue button to avoid duplicating ad controls.
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to inflate overlay layout: $t")
        }

        // Register listeners so we can finish when ad ends or fails
        AppOpenAdManager.setOnAdFinishedListener(adFinishedListener)
        AppOpenAdManager.setOnAdLoadFailedListener(adLoadFailedListener)

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
        try {
            mainHandler.removeCallbacks(finishRunnable)
        } catch (_: Throwable) {}
        // Unregister listeners
        try { AppOpenAdManager.setOnAdFinishedListener(null) } catch (_: Throwable) {}
        try { AppOpenAdManager.setOnAdLoadFailedListener(null) } catch (_: Throwable) {}
    }

    override fun onPause() {
        super.onPause()
        // Do not auto-finish here; let ad callbacks control lifecycle.
    }
}
