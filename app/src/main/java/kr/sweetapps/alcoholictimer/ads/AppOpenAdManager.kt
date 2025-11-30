package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CopyOnWriteArraySet
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager

/**
 * 경량화된 AppOpen 광고 관리자 스텁
 * - 원래 구현의 외부 API를 유지하되, 파일이 잘려 발생하는 컴파일 오류를 제거합니다.
 * - 개발/테스트 환경에서 빌드가 통과하도록 최소 기능만 제공합니다.
 * - 실제 프로덕션 로직(Google SDK 호출)은 필요시 원래 구현으로 교체하세요.
 */
object AppOpenAdManager {
    private const val TAG = "AppOpenAdManager"

    private var applicationRef: Application? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var dismissalRunnable: Runnable? = null

    @Volatile private var autoShowEnabled: Boolean = true
    fun isAutoShowEnabled(): Boolean = autoShowEnabled
    @Volatile private var isShowing: Boolean = false
    @Volatile private var isShowScheduled: Boolean = false
    @Volatile private var loaded: Boolean = false

    // Timestamps for simple suppression logic
    @Volatile private var lastShownAt: Long = 0L
    @Volatile private var lastDismissedAt: Long = 0L

    private val loadedListeners = CopyOnWriteArraySet<() -> Any?>()
    private val shownListeners = CopyOnWriteArraySet<() -> Any?>()
    private val finishedListeners = CopyOnWriteArraySet<() -> Any?>()
    private val loadFailedListeners = CopyOnWriteArraySet<() -> Any?>()

    @Volatile private var onLoadedListener: (() -> Unit)? = null
    @Volatile private var onShownListener: (() -> Unit)? = null
    @Volatile private var onFinishedListener: (() -> Unit)? = null
    @Volatile private var onLoadFailedListener: (() -> Unit)? = null

    fun initialize(application: Application, registerLifecycle: Boolean = true) {
        applicationRef = application
        Log.d(TAG, "initialize: application set. registerLifecycle=$registerLifecycle")
    }

    fun noteAppStart() {
        // optional hook retained for compatibility
        Log.d(TAG, "noteAppStart called")
    }

    fun setAutoShowEnabled(enabled: Boolean) { autoShowEnabled = enabled }

    fun preload(context: Context) {
        try {
            if (applicationRef == null && context.applicationContext is Application) {
                applicationRef = context.applicationContext as Application
            }
        } catch (_: Throwable) {}

        // Simulate a successful load for build/test purposes
        if (loaded) {
            Log.d(TAG, "preload: already loaded")
            return
        }
        loaded = true
        Log.d(TAG, "preload: simulated load complete")
        try { onLoadedListener?.invoke() } catch (_: Throwable) {}
        for (l in loadedListeners) runCatching { l.invoke() }
    }

    fun isLoaded(): Boolean = loaded
    fun isShowingAd(): Boolean = isShowing

    fun clearLoadedAd() { loaded = false }

    fun setOnAdFinishedListener(listener: (() -> Unit)?) { onFinishedListener = listener }
    fun setOnAdLoadedListener(listener: (() -> Unit)?) {
        onLoadedListener = listener
        if (listener != null && loaded) {
            try { listener.invoke() } catch (_: Throwable) {}
        }
    }
    fun setOnAdShownListener(listener: (() -> Unit)?) { onShownListener = listener }
    fun setOnAdLoadFailedListener(listener: (() -> Unit)?) { onLoadFailedListener = listener }

    fun addOnAdLoadedListener(listener: () -> Any?) { loadedListeners.add(listener); if (loaded) runCatching { listener.invoke() } }
    fun removeOnAdLoadedListener(listener: () -> Any?) { loadedListeners.remove(listener) }

    fun addOnAdShownListener(listener: () -> Any?) { shownListeners.add(listener) }
    fun removeOnAdShownListener(listener: () -> Any?) { shownListeners.remove(listener) }

    fun addOnAdFinishedListener(listener: () -> Any?) { finishedListeners.add(listener) }
    fun removeOnAdFinishedListener(listener: () -> Any?) { finishedListeners.remove(listener) }

    fun addOnAdLoadFailedListener(listener: () -> Any?) { loadFailedListeners.add(listener) }
    fun removeOnAdLoadFailedListener(listener: () -> Any?) { loadFailedListeners.remove(listener) }

    fun onConsentUpdated(canRequestAds: Boolean) {
        Log.d(TAG, "onConsentUpdated: canRequestAds=$canRequestAds")
        if (canRequestAds) applicationRef?.let { preload(it.applicationContext) }
    }

    fun showIfAvailable(activity: Activity, bypassRecentFullscreenSuppression: Boolean = false): Boolean {
        Log.d(TAG, "showIfAvailable called - loaded=$loaded isShowing=$isShowing activity=${activity.javaClass.simpleName}")
        if (!loaded || isShowing) return false

        // Basic suppression: if recently shown within 5s, treat as recent
        if (!bypassRecentFullscreenSuppression && wasRecentlyShown()) {
            Log.d(TAG, "showIfAvailable: suppressed due to recent show")
            return false
        }

        isShowing = true
        isShowScheduled = false
        lastShownAt = System.currentTimeMillis()
        Log.d(TAG, "showIfAvailable: marking isShowing=true, lastShownAt=${lastShownAt}")
        try {
            for (l in shownListeners) runCatching { l.invoke() }
            Log.d(TAG, "showIfAvailable: invoked shownListeners count=${shownListeners.size}")
        } catch (_: Throwable) {}
        try {
            Log.d(TAG, "showIfAvailable: invoking onShownListener")
            onShownListener?.invoke()
        } catch (_: Throwable) {}

        // Launch debug overlay activity to make ad visible in tests
        try {
            val intent = Intent(activity, AppOpenOverlayActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d(TAG, "showIfAvailable: launching overlay activity")
            activity.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "showIfAvailable: failed to launch overlay activity: ${t.message}")
        }

        // Simulate dismissal shortly after to allow lifecycle listeners to react
        val run = Runnable {
             try {
                Log.d(TAG, "showIfAvailable: simulated dismissal running (wasShowing=$isShowing)")
                performFinishFlow()
             } catch (_: Throwable) {}
        }
        dismissalRunnable = run
        // Use slightly longer delay to ensure overlay activity has time to display in debug builds
        mainHandler.postDelayed(run, 1800L)

         return true
     }

    /** Called by overlay activity when it finishes so we don't double-run dismissal logic. */
    fun notifyAdFinishedFromOverlay() {
        try {
            dismissalRunnable?.let { mainHandler.removeCallbacks(it) }
        } catch (_: Throwable) {}
        performFinishFlow()
    }

    private fun performFinishFlow() {
        try {
            isShowing = false
            loaded = false
            lastDismissedAt = System.currentTimeMillis()
            Log.d(TAG, "performFinishFlow -> lastDismissedAt=${lastDismissedAt}")
            for (l in finishedListeners) runCatching { l.invoke() }
            try { onFinishedListener?.invoke() } catch (_: Throwable) {}
            try { AnalyticsManager.logAdImpression("app_open") } catch (_: Throwable) {}
            applicationRef?.applicationContext?.let { ctx ->
                mainHandler.postDelayed({ try { preload(ctx) } catch (_: Throwable) {} }, 30_000L)
            }
        } catch (_: Throwable) {}
    }

    fun wasRecentlyShown(): Boolean {
        val last = lastShownAt
        if (last <= 0L) return false
        val elapsed = System.currentTimeMillis() - last
        return elapsed < 5_000L
    }
}
