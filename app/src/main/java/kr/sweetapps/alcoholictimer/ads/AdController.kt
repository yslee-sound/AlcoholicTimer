package kr.sweetapps.alcoholictimer.ads

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stubbed AdController to satisfy references after removing ad implementation.
 */
object AdController {
    private const val TAG = "AdController"

    // splash release listeners (called when policy forces splash release)
    private val splashReleaseListeners = mutableSetOf<() -> Unit>()

    // policy fetch listeners
    data class Policy(
        val adBannerEnabled: Boolean = false,
        val adInterstitialEnabled: Boolean = false,
        val adAppOpenEnabled: Boolean = true // 기본값을 true로 변경 (앱오프닝 광고 허용)
    )

    private val policyListeners = mutableSetOf<(Policy?) -> Unit>()
    @Volatile private var currentPolicy: Policy? = null

    // runtime flags for UI consumers
    private val interstitialShowing = AtomicBoolean(false)
    private val fullScreenAdShowing = AtomicBoolean(false)

    fun initialize(context: Context) { Log.d(TAG, "Stub initialize")
        // trigger a default (stub) policy to notify listeners so callers can react
        // In real implementation this would be an async network fetch.
        currentPolicy = Policy(adBannerEnabled = false, adInterstitialEnabled = false, adAppOpenEnabled = true)
        notifyPolicyListeners()
    }

    fun isPolicyFetchCompleted(): Boolean = true
    fun isInterstitialEnabled(): Boolean = false
    // 기본적으로 app-open 허용 상태를 반환하도록 변경
    fun isAppOpenEnabled(): Boolean = currentPolicy?.adAppOpenEnabled ?: true
    fun isFullScreenAdShowing(): Boolean = fullScreenAdShowing.get()
    fun setInterstitialShowing(showing: Boolean) { interstitialShowing.set(showing) }
    fun setFullScreenAdShowing(showing: Boolean) { fullScreenAdShowing.set(showing) }
    fun setAppOpenLoading(loading: Boolean) {}
    fun setAppOpenLoaded(loaded: Boolean) {}
    fun setAppOpenLastError(err: String?) {}
    fun setInterstitialLoading(loading: Boolean) {}
    fun setInterstitialLoaded(loaded: Boolean) {}
    fun setInterstitialLastError(err: String?) {}
    fun refreshPolicy(context: Context) {
        // no-op stub; in real implementation this would fetch and update currentPolicy
        currentPolicy = Policy(adBannerEnabled = false, adInterstitialEnabled = false, adAppOpenEnabled = true)
        notifyPolicyListeners()
    }
    fun canShowInterstitial(context: Context): Boolean = false
    fun recordInterstitialShown(context: Context) {}
    fun canShowAppOpen(context: Context): Boolean = false

    /**
     * Registers a listener that will be invoked when external code requests the splash to be released.
     * MainActivity uses this to immediately proceed if ad policy disables showing app-open ads.
     */
    fun addSplashReleaseListener(listener: () -> Unit) {
        synchronized(splashReleaseListeners) { splashReleaseListeners.add(listener) }
    }

    /**
     * Removes a previously registered splash release listener.
     */
    fun removeSplashReleaseListener(listener: () -> Unit) {
        synchronized(splashReleaseListeners) { splashReleaseListeners.remove(listener) }
    }

    /**
     * Trigger all registered splash-release listeners (stub: not called automatically).
     * Kept public so tests or other code can simulate policy change.
     */
    fun triggerSplashRelease() {
        val copy: List<() -> Unit>
        synchronized(splashReleaseListeners) { copy = splashReleaseListeners.toList() }
        for (l in copy) {
            try { l.invoke() } catch (t: Throwable) { Log.w(TAG, "splash listener failed", t) }
        }
    }

    /**
     * Adds a listener that will be called when policy becomes available/changes.
     * Listener is invoked immediately if a policy is already available.
     */
    fun addPolicyFetchListener(listener: (Policy?) -> Unit) {
        synchronized(policyListeners) { policyListeners.add(listener) }
        // deliver current policy synchronously if available
        try {
            listener.invoke(currentPolicy)
        } catch (t: Throwable) {
            Log.w(TAG, "policy listener invoke failed", t)
        }
    }

    /**
     * Removes a previously registered policy fetch listener.
     */
    fun removePolicyFetchListener(listener: (Policy?) -> Unit) {
        synchronized(policyListeners) { policyListeners.remove(listener) }
    }

    private fun notifyPolicyListeners() {
        val copy: List<(Policy?) -> Unit>
        synchronized(policyListeners) { copy = policyListeners.toList() }
        for (l in copy) {
            try { l.invoke(currentPolicy) } catch (t: Throwable) { Log.w(TAG, "policy listener failed", t) }
        }
    }

    // --- New helper accessors used by Compose UI ---
    /** Returns whether banners are enabled by policy (safe, non-blocking) */
    fun isBannerEnabledState(): Boolean = currentPolicy?.adBannerEnabled ?: false

    /** Runtime state: is an interstitial currently shown */
    fun isInterstitialShowingState(): Boolean = interstitialShowing.get()

    /** Runtime state: is a fullscreen ad overlay showing */
    fun isFullScreenAdShowingState(): Boolean = fullScreenAdShowing.get()
}
