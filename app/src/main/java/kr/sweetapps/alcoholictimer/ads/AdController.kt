package kr.sweetapps.alcoholictimer.ads

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Central ad policy controller. Fetches AdPolicy from Supabase via AdPolicyRepository
 * and enforces app-open enable/limit semantics used by AppOpen flow.
 */
@Suppress("unused", "UNUSED_PARAMETER")
object AdController {
    private const val TAG = "AdController"

    // Persistence keys for interstitial counters and windows
    private const val PREFS_NAME = "ad_policy_counters"
    private const val KEY_HOUR_WINDOW_START = "hour_window_start"
    private const val KEY_DAY_WINDOW_START = "day_window_start"
    private const val KEY_INTER_HOUR_COUNT = "inter_hour_count"
    private const val KEY_INTER_DAY_COUNT = "inter_day_count"

    @Volatile private var appContext: Context? = null

    private val splashReleaseListeners = mutableSetOf<() -> Unit>()
    private val policyListeners = mutableSetOf<(Policy?) -> Unit>()

    // runtime policy snapshot
    @Volatile private var currentPolicy: AdPolicy? = null

    // counters for app-open frequency (reset every hour/day as needed)
    private var hourWindowStart = System.currentTimeMillis()
    private var dayWindowStart = System.currentTimeMillis()
    private val shownThisHour = AtomicInteger(0)
    private val shownToday = AtomicInteger(0)

    // track last app-open shown timestamp (ms) for cooldown enforcement
    @Volatile private var lastAppOpenShownAt: Long = 0L

    // Interstitial counters (same window semantics)
    private val shownInterstitialThisHour = AtomicInteger(0)
    private val shownInterstitialToday = AtomicInteger(0)
    @Volatile private var lastInterstitialShownAt: Long = 0L

    // runtime flags
    private val interstitialShowing = AtomicBoolean(false)
    private val fullScreenAdShowing = AtomicBoolean(false)
    private val fullScreenListeners = mutableSetOf<(Boolean) -> Unit>()

    data class Policy(
        val adBannerEnabled: Boolean = false,
        val adInterstitialEnabled: Boolean = false,
        val adAppOpenEnabled: Boolean = true
    )

    fun initialize(context: Context) {
        Log.d(TAG, "initialize: loading policy from Supabase repo")
        // keep application context for counters persistence
        try { appContext = context.applicationContext } catch (_: Throwable) {}
        // async fetch policy and keep cached snapshot
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AdPolicyRepository(context.packageName)
                val res = runCatching { repo.getPolicy() }
                val policy = res.getOrNull()
                if (policy != null) {
                    currentPolicy = policy
                    Log.d(TAG, "initialize: policy loaded: appOpen=${policy.adAppOpenEnabled} hourMax=${policy.appOpenMaxPerHour} dayMax=${policy.appOpenMaxPerDay}")
                } else {
                    Log.d(TAG, "initialize: no policy returned from repo; using defaults (treat as disabled)")
                }
                // load persisted interstitial counters/window start if available
                try {
                    val sp = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sp?.let {
                        hourWindowStart = it.getLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        dayWindowStart = it.getLong(KEY_DAY_WINDOW_START, dayWindowStart)
                        shownInterstitialThisHour.set(it.getInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get()))
                        shownInterstitialToday.set(it.getInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get()))
                        Log.d(TAG, "initialize: loaded persisted counters interHour=${shownInterstitialThisHour.get()} interDay=${shownInterstitialToday.get()} hourWindowStart=$hourWindowStart dayWindowStart=$dayWindowStart")
                    }
                } catch (_: Throwable) {}
                notifyPolicyListeners()
                // If policy explicitly disables app-open, inform listeners to release splash etc.
                if (currentPolicy?.adAppOpenEnabled == false) {
                    Log.d(TAG, "initialize: policy disables app-open -> triggering splash release listeners")
                    triggerSplashRelease()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "initialize: failed to fetch policy: $t")
            }
        }
    }

    private fun notifyPolicyListeners() {
        val copy: List<(Policy?) -> Unit>
        synchronized(policyListeners) { copy = policyListeners.toList() }
        for (l in copy) {
            try { l.invoke(currentPolicy?.let { Policy(it.adBannerEnabled, it.adInterstitialEnabled, it.adAppOpenEnabled) }) } catch (t: Throwable) { Log.w(TAG, "policy listener failed", t) }
        }
    }

    fun addPolicyFetchListener(listener: (Policy?) -> Unit) {
        synchronized(policyListeners) { policyListeners.add(listener) }
        try { listener.invoke(currentPolicy?.let { Policy(it.adBannerEnabled, it.adInterstitialEnabled, it.adAppOpenEnabled) }) } catch (t: Throwable) { Log.w(TAG, "policy listener invoke failed", t) }
    }

    fun removePolicyFetchListener(listener: (Policy?) -> Unit) { synchronized(policyListeners) { policyListeners.remove(listener) } }

    fun addSplashReleaseListener(listener: () -> Unit) { synchronized(splashReleaseListeners) { splashReleaseListeners.add(listener) } }
    fun removeSplashReleaseListener(listener: () -> Unit) { synchronized(splashReleaseListeners) { splashReleaseListeners.remove(listener) } }

    fun triggerSplashRelease() {
        val copy: List<() -> Unit>
        synchronized(splashReleaseListeners) { copy = splashReleaseListeners.toList() }
        for (l in copy) { try { l.invoke() } catch (t: Throwable) { Log.w(TAG, "splash listener failed", t) } }
    }

    // accessors used by UI
    fun isPolicyFetchCompleted(): Boolean = true // keep existing callers simple
    fun isInterstitialEnabled(): Boolean = currentPolicy?.adInterstitialEnabled ?: false
    // Fail-safe: if policy not yet fetched, treat app-open as disabled so Supabase controls it
    fun isAppOpenEnabled(): Boolean = currentPolicy?.adAppOpenEnabled ?: false
    fun isFullScreenAdShowing(): Boolean = fullScreenAdShowing.get()
    fun setInterstitialShowing(showing: Boolean) { interstitialShowing.set(showing) }
    fun setFullScreenAdShowing(showing: Boolean) {
        fullScreenAdShowing.set(showing)
        // notify listeners of change
        val copy: List<(Boolean) -> Unit>
        synchronized(fullScreenListeners) { copy = fullScreenListeners.toList() }
        for (l in copy) { try { l.invoke(showing) } catch (_: Throwable) {} }
    }

    fun addFullScreenShowListener(listener: (Boolean) -> Unit) { synchronized(fullScreenListeners) { fullScreenListeners.add(listener) }; try { listener.invoke(fullScreenAdShowing.get()) } catch (_: Throwable) {} }
    fun removeFullScreenShowListener(listener: (Boolean) -> Unit) { synchronized(fullScreenListeners) { fullScreenListeners.remove(listener) } }

    // Added compatibility/state helpers used by UI callers (preserve old names expected by Compose files)
    fun isBannerEnabledState(): Boolean = currentPolicy?.adBannerEnabled ?: false
    fun isInterstitialShowingState(): Boolean = interstitialShowing.get()
    fun isFullScreenAdShowingState(): Boolean = fullScreenAdShowing.get()

    // Stable API names expected across codebase
    fun isBannerEnabled(): Boolean = currentPolicy?.adBannerEnabled ?: false
    fun isInterstitialShowingNow(): Boolean = interstitialShowing.get()

    fun refreshPolicy(context: Context) {
        initialize(context)
    }

    // Debug helper: return a compact snapshot of policy and counters
    fun debugSnapshot(): String {
        return try {
            val p = currentPolicy
            val policyStr = if (p == null) "null" else "appId=${p.appId} active=${p.isActive} interEnabled=${p.adInterstitialEnabled} interHourMax=${p.adInterstitialMaxPerHour} interDayMax=${p.adInterstitialMaxPerDay} appOpenHourMax=${p.appOpenMaxPerHour} appOpenDayMax=${p.appOpenMaxPerDay} cooldown=${p.appOpenCooldownSeconds}"
            "AdController(snapshot: policy=$policyStr interCountsHour=${shownInterstitialThisHour.get()} interCountsDay=${shownInterstitialToday.get()} appCountsHour=${shownThisHour.get()} appCountsDay=${shownToday.get()} hourWindowStart=$hourWindowStart dayWindowStart=$dayWindowStart lastInterstitialAt=$lastInterstitialShownAt lastAppOpenAt=$lastAppOpenShownAt)"
        } catch (t: Throwable) {
            "AdController(debugSnapshot failed: $t)"
        }
    }

    // Ensure hour/day windows and counters are reset when their interval elapses.
    private fun resetWindowsIfNeeded() {
        try {
            val now = System.currentTimeMillis()
            var persistNeeded = false
            synchronized(this) {
                // hourly window expired
                if (now - hourWindowStart >= 60 * 60 * 1000L) {
                    hourWindowStart = now
                    shownThisHour.set(0)
                    shownInterstitialThisHour.set(0)
                    persistNeeded = true
                }
                // daily window expired
                if (now - dayWindowStart >= 24 * 60 * 60 * 1000L) {
                    dayWindowStart = now
                    shownToday.set(0)
                    shownInterstitialToday.set(0)
                    persistNeeded = true
                }
            }
            if (persistNeeded) {
                try {
                    val sp = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sp?.edit {
                        putLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        putLong(KEY_DAY_WINDOW_START, dayWindowStart)
                        putInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get())
                        putInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get())
                    }
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.w(TAG, "resetWindowsIfNeeded failed: $t")
        }
    }

    // Interstitial frequency control
    fun canShowInterstitial(context: Context): Boolean = canShowInterstitialNow()

    /** Context-free check usable in unit tests and internal callers */
    fun canShowInterstitialNow(): Boolean {
        try {
            resetWindowsIfNeeded()
            val policy = currentPolicy ?: run {
                Log.d(TAG, "canShowInterstitialNow: no policy loaded -> deny")
                return false
            }
            Log.d(TAG, "canShowInterstitialNow: policy=appId=${policy.appId} active=${policy.isActive} adInterstitialEnabled=${policy.adInterstitialEnabled} hourMax=${policy.adInterstitialMaxPerHour} dayMax=${policy.adInterstitialMaxPerDay} countsHour=${shownInterstitialThisHour.get()} countsDay=${shownInterstitialToday.get()}")
            if (!policy.isActive) return false
            if (!policy.adInterstitialEnabled) return false
            if (policy.adInterstitialMaxPerHour >= 0 && shownInterstitialThisHour.get() >= policy.adInterstitialMaxPerHour) return false
            if (policy.adInterstitialMaxPerDay >= 0 && shownInterstitialToday.get() >= policy.adInterstitialMaxPerDay) return false
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "canShowInterstitialNow evaluation failed: $t")
            return false
        }
    }

    /** Test helper: set policy snapshot (used by unit tests) */
    fun setPolicyForTest(policy: AdPolicy?) {
        currentPolicy = policy
        // reset counters when changing policy
        shownInterstitialThisHour.set(0)
        shownInterstitialToday.set(0)
        shownThisHour.set(0)
        shownToday.set(0)
        lastAppOpenShownAt = 0L
        lastInterstitialShownAt = 0L
    }

    fun canShowAppOpen(context: Context): Boolean {
        try {
            resetWindowsIfNeeded()
            val policy = currentPolicy
            // Fail-safe: if policy not yet fetched, do NOT show app-open
            if (policy == null) return false
            if (!policy.isActive) return false
            if (!policy.adAppOpenEnabled) return false
            if (policy.appOpenMaxPerHour >= 0 && shownThisHour.get() >= policy.appOpenMaxPerHour) return false
            if (policy.appOpenMaxPerDay >= 0 && shownToday.get() >= policy.appOpenMaxPerDay) return false
            // Cooldown (server-controlled)
            try {
                val cdSec = policy.appOpenCooldownSeconds
                if (cdSec > 0) {
                    val now = System.currentTimeMillis()
                    val last = lastAppOpenShownAt
                    if (last > 0 && now - last < cdSec * 1000L) return false
                }
            } catch (_: Throwable) {}
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "canShowAppOpen evaluation failed: $t")
            return false
        }
    }

    fun recordAppOpenShown(context: Context) {
        try {
            resetWindowsIfNeeded()
            shownThisHour.incrementAndGet()
            shownToday.incrementAndGet()
            try { lastAppOpenShownAt = System.currentTimeMillis() } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    // Expose cooldown and recent shown check for clients
    fun getAppOpenCooldownSeconds(): Int = currentPolicy?.appOpenCooldownSeconds ?: 60
    fun getLastAppOpenShownAt(): Long = lastAppOpenShownAt
    fun isAppOpenInCooldown(): Boolean {
        try {
            val policy = currentPolicy ?: return false
            val cd = policy.appOpenCooldownSeconds
            if (cd <= 0) return false
            val last = lastAppOpenShownAt
            if (last == 0L) return false
            return (System.currentTimeMillis() - last) < cd * 1000L
        } catch (_: Throwable) { return false }
    }

    // no-op setters preserved for compatibility
    fun setAppOpenLoading(loading: Boolean) {}
    fun setAppOpenLoaded(loaded: Boolean) {}
    fun setAppOpenLastError(err: String?) {}
    fun setInterstitialLoading(loading: Boolean) {}
    fun setInterstitialLoaded(loaded: Boolean) {}
    fun setInterstitialLastError(err: String?) {}

    // Reserve/unreserve interstitial slot API used by ad managers. Reservation is atomic
    // w.r.t. policy counters: reserve increments counters if limits allow; unreserve
    // decrements counters in case a reserved ad failed to show.
    fun reserveInterstitialSlot(): Boolean {
        try {
            synchronized(this) {
                resetWindowsIfNeeded()
                val policy = currentPolicy ?: run {
                    Log.d(TAG, "reserveInterstitialSlot: no policy -> deny")
                    return false
                }
                if (!policy.isActive) return false
                if (!policy.adInterstitialEnabled) return false
                if (policy.adInterstitialMaxPerHour >= 0 && shownInterstitialThisHour.get() >= policy.adInterstitialMaxPerHour) return false
                if (policy.adInterstitialMaxPerDay >= 0 && shownInterstitialToday.get() >= policy.adInterstitialMaxPerDay) return false

                shownInterstitialThisHour.incrementAndGet()
                shownInterstitialToday.incrementAndGet()
                try { lastInterstitialShownAt = System.currentTimeMillis() } catch (_: Throwable) {}

                // persist interstitial counters and window starts
                try {
                    val sp = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sp?.edit {
                        putLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        putLong(KEY_DAY_WINDOW_START, dayWindowStart)
                        putInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get())
                        putInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get())
                    }
                } catch (_: Throwable) {}

                return true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "reserveInterstitialSlot failed: $t")
            return false
        }
    }

    fun unreserveInterstitialSlot() {
        try {
            synchronized(this) {
                if (shownInterstitialThisHour.get() > 0) shownInterstitialThisHour.decrementAndGet()
                if (shownInterstitialToday.get() > 0) shownInterstitialToday.decrementAndGet()
                try {
                    val sp = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sp?.edit {
                        putInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get())
                        putInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get())
                        putLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        putLong(KEY_DAY_WINDOW_START, dayWindowStart)
                    }
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.w(TAG, "unreserveInterstitialSlot failed: $t")
        }
    }
}
