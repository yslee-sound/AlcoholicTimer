package kr.sweetapps.alcoholictimer.ui.ad

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

class AppOpenOverlayActivity : Activity() {
    private val TAG = "AppOpenOverlayActivity"
    private val handler = Handler(Looper.getMainLooper())
    private val displayMillis: Long = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: showing overlay for ${displayMillis}ms")

        // simple full-screen view
        val root = FrameLayout(this)
        root.setBackgroundColor(0xFF000000.toInt())
        val tv = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            text = "AppOpen (debug overlay)"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 200, 0, 0)
        }
        root.addView(tv)
        setContentView(root)

        // finish after displayMillis
        handler.postDelayed({
            try {
                finish()
            } catch (_: Throwable) {}
        }, displayMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            AppOpenAdManager.notifyAdFinishedFromOverlay()
        } catch (_: Throwable) {}
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy: overlay finished")
    }
}

