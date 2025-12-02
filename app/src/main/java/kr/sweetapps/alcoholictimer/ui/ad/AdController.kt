package kr.sweetapps.alcoholictimer.ui.ad

/*
 * ì£¼ì˜(IMPORTANT):
 * ???Œì¼?€ ???¤í”„??App Open / ?„ì²´?”ë©´) ê´‘ê³ ?€ ë°°ë„ˆ ê´‘ê³  ê°„ì˜ ?íƒœ ?™ê¸°?”ë? ?´ë‹¹?©ë‹ˆ??
 * ë³€ê²½ì‚¬??ë°˜ì˜??: setFullScreenAdShowing(false)ê°€ ?¸ì¶œ?˜ì–´ ?„ì²´?”ë©´ ê´‘ê³ ê°€ ?«í ??
 * ?ë™?¼ë¡œ triggerBannerReload()ë¥??¸ì¶œ??ë°°ë„ˆ ?¬ë¡œ?©ì„ ?¸ë¦¬ê±°í•˜?„ë¡ êµ¬í˜„?˜ì–´ ?ˆìŠµ?ˆë‹¤.
 *
 * ë¬¸ì œ ?¬ë°œ ë°©ì????ˆë‚´:
 *  - ?ì¸: ê³¼ê±°?ëŠ” ?±ì˜¤?„ë‹(?„ì²´?”ë©´) ê´‘ê³ ê°€ ?«í???ë°°ë„ˆ ì»´í¬?ŒíŠ¸??"?¤ì‹œ ë¡œë“œ?˜ë¼"??? í˜¸ê°€ ?„ë‹¬?˜ì? ?Šì•„
 *    ë°°ë„ˆê°€ ê°±ì‹ ?˜ì? ?ŠëŠ” ë¬¸ì œê°€ ?ˆì—ˆ?µë‹ˆ??
 *  - ì§€ê¸ˆì˜ ?™ì‘: ?„ì²´?”ë©´??ë³´ì´?¤ê? ?«íˆ???œê°„??ê°ì????´ë??ìœ¼ë¡?bannerReloadTick??ê°±ì‹ ?˜ë?ë¡?
 *    ?¸ì¶œ ?„ë½?¼ë¡œ ?¸í•œ ë¹?ë°°ë„ˆ ë¬¸ì œ ê°€?¥ì„±???¬ê²Œ ì¤„ì–´?¤ì—ˆ?µë‹ˆ??
 *  - ê¶Œê³ : ?ë™ ?¸ì¶œ???ìš©?˜ì—ˆ?”ë¼?? ?????¤ë¥¸ ê²½ë¡œ?ì„œ ë°°ë„ˆ/ì»¨ì…‰??ë³€?”ê? ë°œìƒ?˜ë©´
 *    ëª…ì‹œ?ìœ¼ë¡?AdController.triggerBannerReload()ë¥??¸ì¶œ?˜ëŠ” ê²ƒì´ ?ˆì „?©ë‹ˆ??
 *  - ë°°ë„ˆ ì¸?êµ¬í˜„: ë°°ë„ˆ ë·?ì»´í¬?ŒíŠ¸??AdController.bannerReloadTick ë¥??µì?ë¹™í•˜??ê°?ë³€ê²???reloadë¥??œë„?´ì•¼ ?©ë‹ˆ??
 *
 * ?”ì•½: setFullScreenAdShowing(false)?ì„œ ?ë™ ?¸ë¦¬ê±°ê? ì¶”ê??˜ì—ˆ?¼ë‹ˆ ?°ì„  ???™ì‘??? ë¢°?˜ë˜,
 * ?¸ë? ?íƒœ ë³€ê²??œì—??triggerBannerReload() ?¸ì¶œ??ê¶Œì¥?©ë‹ˆ??
 */

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.MainApplication
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

    // Reactive StateFlow for full-screen ad visibility: single source of truth for UI
    private val _fullScreenAdShowingFlow = MutableStateFlow(false)
    val fullScreenAdShowingFlow: StateFlow<Boolean> get() = _fullScreenAdShowingFlow

    // Persistence keys for interstitial counters and windows
    private const val PREFS_NAME = "ad_policy_counters"
    private const val KEY_HOUR_WINDOW_START = "hour_window_start"
    private const val KEY_DAY_WINDOW_START = "day_window_start"
    private const val KEY_INTER_HOUR_COUNT = "inter_hour_count"
    private const val KEY_INTER_DAY_COUNT = "inter_day_count"

    @Volatile
    private var appContext: Context? = null

    private val splashReleaseListeners = mutableSetOf<() -> Unit>()
    private val policyListeners = mutableSetOf<(Policy?) -> Unit>()

    // runtime policy snapshot
    @Volatile
    private var currentPolicy: AdPolicy? = null

    // counters for app-open frequency (reset every hour/day as needed)
    private var hourWindowStart = System.currentTimeMillis()
    private var dayWindowStart = System.currentTimeMillis()
    private val shownThisHour = AtomicInteger(0)
    private val shownToday = AtomicInteger(0)

    // track last app-open shown timestamp (ms) for cooldown enforcement
    @Volatile
    private var lastAppOpenShownAt: Long = 0L

    // Interstitial counters (same window semantics)
    private val shownInterstitialThisHour = AtomicInteger(0)
    private val shownInterstitialToday = AtomicInteger(0)

    @Volatile
    private var lastInterstitialShownAt: Long = 0L

    // runtime flags
    private val interstitialShowing = AtomicBoolean(false)
    private val fullScreenAdShowing = AtomicBoolean(false)
    private val fullScreenListeners = mutableSetOf<(Boolean) -> Unit>()

    // ê¸°ë¡: ?„ì²´?”ë©´(Interstitial/AppOpen ?? ê´‘ê³ ê°€ ?«íŒ ë§ˆì?ë§??œê°
    @Volatile
    private var lastFullScreenDismissedAt: Long = 0L

    // ?¸ë??ì„œ ë§ˆì?ë§??„ì²´?”ë©´ ì¢…ë£Œ ?œê°??ì§ˆì˜?????ˆë„ë¡?getter ?œê³µ
    fun getLastFullScreenDismissedAt(): Long = lastFullScreenDismissedAt

    // Ticker to request banner components to retry loading (increment when reload needed)
    private val _bannerReloadTick = MutableStateFlow(0L)
    val bannerReloadTick: StateFlow<Long> get() = _bannerReloadTick

    // Force-hide flag: allow external managers to request banners be hidden immediately
    private val _bannerForceHidden = MutableStateFlow(false)
    val bannerForceHiddenFlow: StateFlow<Boolean> get() = _bannerForceHidden.asStateFlow()

    private val bannerForceHiddenListeners = mutableSetOf<(Boolean) -> Unit>()

    fun setBannerForceHidden(hidden: Boolean) {
        try {
            _bannerForceHidden.value = hidden
            Log.d(TAG, "setBannerForceHidden: $hidden")
        } catch (_: Throwable) {}
        // notify listeners immediately
        val copy: List<(Boolean) -> Unit>
        synchronized(bannerForceHiddenListeners) { copy = bannerForceHiddenListeners.toList() }
        for (l in copy) {
            try { l.invoke(hidden) } catch (_: Throwable) {}
        }
    }

    /**
     * ë°°ë„ˆ ê´‘ê³ ë¥?ê°•ì œë¡?ë³´ì´?„ë¡ ë³µêµ¬
     * - bannerForceHidden??falseë¡??¤ì •
     * - bannerReloadTick??ê°±ì‹ ?˜ì—¬ ë°°ë„ˆ ?¬ë¡œ???¸ë¦¬ê±?
     * ?”§ ?¬ë°œ ë°©ì?: AppOpen/Interstitial ì¢…ë£Œ ??ë°˜ë“œ???¸ì¶œ
     */
    fun ensureBannerVisible(reason: String? = null) {
        try {
            Log.d(TAG, "ensureBannerVisible reason=$reason (current: forceHidden=${_bannerForceHidden.value}, fullScreen=${_fullScreenAdShowingFlow.value})")
        } catch (_: Throwable) {}
        try { setBannerForceHidden(false) } catch (_: Throwable) {}
        try { triggerBannerReload() } catch (_: Throwable) {}
    }

    /**
     * ?š¨ AdMob ?•ì±… ì¤€?? ?„ë©´ê´‘ê³ ?€ ë°°ë„ˆ ê´‘ê³  ê²¹ì¹¨ ë°©ì?
     *
     * ë°°ë„ˆë¥?ì¦‰ì‹œ ?¨ê? (StateFlow + ëª¨ë“  ë¦¬ìŠ¤??ì¦‰ì‹œ ?¸ì¶œ)
     * - show() ?¸ì¶œ ì§ì „???¬ìš©?˜ì—¬ ë°°ë„ˆê°€ ?„ë©´ê´‘ê³  ?„ì— ?˜í??˜ì? ?Šë„ë¡?ë³´ì¥
     *
     * @param reason ?¨ê¸°???´ìœ  (ë¡œê·¸??
     */
    fun hideBannerImmediately(reason: String? = null) {
        try {
            Log.d(TAG, "hideBannerImmediately reason=$reason - forcing GONE immediately")
        } catch (_: Throwable) {}

        // StateFlow ì¦‰ì‹œ ?…ë°?´íŠ¸
        try { _bannerForceHidden.value = true } catch (_: Throwable) {}
        try { _fullScreenAdShowingFlow.value = true } catch (_: Throwable) {}

        // ëª¨ë“  ë¦¬ìŠ¤??ì¦‰ì‹œ ?™ê¸° ?¸ì¶œ (Compose recomposition ?¸ë¦¬ê±?
        val forceHiddenCopy: List<(Boolean) -> Unit>
        synchronized(bannerForceHiddenListeners) { forceHiddenCopy = bannerForceHiddenListeners.toList() }
        for (l in forceHiddenCopy) {
            try { l.invoke(true) } catch (_: Throwable) {}
        }

        val fullScreenCopy: List<(Boolean) -> Unit>
        synchronized(fullScreenListeners) { fullScreenCopy = fullScreenListeners.toList() }
        for (l in fullScreenCopy) {
            try { l.invoke(true) } catch (_: Throwable) {}
        }
    }

    fun addBannerForceHiddenListener(listener: (Boolean) -> Unit) {
        synchronized(bannerForceHiddenListeners) { bannerForceHiddenListeners.add(listener) }
        try { listener.invoke(_bannerForceHidden.value) } catch (_: Throwable) {}
    }

    fun removeBannerForceHiddenListener(listener: (Boolean) -> Unit) {
        synchronized(bannerForceHiddenListeners) { bannerForceHiddenListeners.remove(listener) }
    }

    private val _isPersonalizedAdsAllowed = MutableStateFlow(false)
    val isPersonalizedAdsAllowed: StateFlow<Boolean> = _isPersonalizedAdsAllowed.asStateFlow()

    data class Policy(
        val adBannerEnabled: Boolean = false,
        val adInterstitialEnabled: Boolean = false,
        val adAppOpenEnabled: Boolean = true
    )

    // Note: do NOT add a getFullScreenAdShowingFlow() function ??the property
    // `fullScreenAdShowingFlow` already exposes a JVM getter. Avoid duplicate JVM signature.

    fun initialize(context: Context) {
        Log.d(TAG, "initialize: reading local policy cache first")
        // keep application context for counters persistence
        try {
            appContext = context.applicationContext
        } catch (_: Throwable) {
        }

        // 1ï¸âƒ£ ì¦‰ì‹œ ë¡œì»¬ ìºì‹œ?ì„œ ?•ì±… ?½ê¸° (?™ê¸°, ?¤íŠ¸?Œí¬ ?†ìŒ)
        val repo = AdPolicyRepository(context.packageName, ctx = context.applicationContext)
        val cachedPolicy = try {
            repo.getCachedPolicySync()
        } catch (_: Throwable) {
            null
        }

        if (cachedPolicy != null) {
            // ìºì‹œ???•ì±…???ˆìœ¼ë©?ì¦‰ì‹œ ?ìš©
            currentPolicy = cachedPolicy
            Log.d(TAG, "initialize: loaded cached policy immediately -> appOpen=${cachedPolicy.adAppOpenEnabled} banner=${cachedPolicy.adBannerEnabled}")
            // ì¦‰ì‹œ ë¦¬ìŠ¤?ˆì—ê²??Œë¦¼
            notifyPolicyListeners()
        } else {
            // ìºì‹œê°€ ?†ìœ¼ë©?ê¸°ë³¸ê°??¬ìš© (Debug/Release ëª¨ë‘ DEFAULT_FALLBACK)
            currentPolicy = AdPolicy.DEFAULT_FALLBACK
            Log.d(TAG, "initialize: no cache found, using DEFAULT_FALLBACK -> appOpen=${AdPolicy.DEFAULT_FALLBACK.adAppOpenEnabled} banner=${AdPolicy.DEFAULT_FALLBACK.adBannerEnabled}")
            // ì¦‰ì‹œ ë¦¬ìŠ¤?ˆì—ê²??Œë¦¼
            notifyPolicyListeners()
        }

        val mainApp = context.applicationContext as MainApplication
        val umpManager = mainApp.umpConsentManager

        CoroutineScope(Dispatchers.IO).launch {
            umpManager.isPersonalizedAdsAllowed.collect { isAllowed ->
                if (_isPersonalizedAdsAllowed.value != isAllowed) {
                    _isPersonalizedAdsAllowed.value = isAllowed
                    triggerBannerReload() // NPA status change requires ad reload
                }
            }
        }

        // 2ï¸âƒ£ ë°±ê·¸?¼ìš´?œì—??ìµœì‹  ?•ì±… ê°€?¸ì˜¤ê¸?(ë¹„ë™ê¸? ?¤íŠ¸?Œí¬)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "initialize: fetching latest policy from Supabase in background")
                val res = runCatching { repo.getPolicy() }
                val policy = res.getOrNull()
                if (policy != null) {
                    val policyChanged = currentPolicy?.let {
                        it.adBannerEnabled != policy.adBannerEnabled ||
                        it.adInterstitialEnabled != policy.adInterstitialEnabled ||
                        it.adAppOpenEnabled != policy.adAppOpenEnabled
                    } ?: true

                    currentPolicy = policy
                    Log.d(
                        TAG,
                        "initialize: background fetch complete -> appOpen=${policy.adAppOpenEnabled} banner=${policy.adBannerEnabled} (changed=$policyChanged)"
                    )

                    // ?•ì±…??ë³€ê²½ë˜?ˆìœ¼ë©?ë¦¬ìŠ¤?ˆì—ê²??Œë¦¼
                    if (policyChanged) {
                        notifyPolicyListeners()
                        // ë°°ë„ˆ ?•ì±…??ë³€ê²½ë˜?ˆìœ¼ë©?ë¦¬ë¡œ???¸ë¦¬ê±?
                        if (policy.adBannerEnabled) {
                            triggerBannerReload()
                        }
                    }
                } else {
                    Log.d(TAG, "initialize: background fetch returned null, keeping current policy")
                }
                // load persisted interstitial counters/window start if available
                try {
                    val sp = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sp?.let {
                        hourWindowStart = it.getLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        dayWindowStart = it.getLong(KEY_DAY_WINDOW_START, dayWindowStart)
                        shownInterstitialThisHour.set(it.getInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get()))
                        shownInterstitialToday.set(it.getInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get()))
                        Log.d(
                            TAG,
                            "initialize: loaded persisted counters interHour=${shownInterstitialThisHour.get()} interDay=${shownInterstitialToday.get()} hourWindowStart=$hourWindowStart dayWindowStart=$dayWindowStart"
                        )
                    }
                } catch (_: Throwable) {
                }
                // If policy explicitly disables app-open, inform listeners to release splash etc.
                if (currentPolicy?.adAppOpenEnabled == false) {
                    Log.d(TAG, "initialize: policy disables app-open -> triggering splash release listeners")
                    triggerSplashRelease()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "initialize: background policy fetch failed: $t")
            }
        }
    }

    private fun notifyPolicyListeners() {
        val copy: List<(Policy?) -> Unit>
        synchronized(policyListeners) { copy = policyListeners.toList() }
        for (l in copy) {
            try {
                l.invoke(currentPolicy?.let { Policy(it.adBannerEnabled, it.adInterstitialEnabled, it.adAppOpenEnabled) })
            } catch (t: Throwable) {
                Log.w(TAG, "policy listener failed", t)
            }
        }
    }

    fun addPolicyFetchListener(listener: (Policy?) -> Unit) {
        synchronized(policyListeners) { policyListeners.add(listener) }
        try {
            listener.invoke(currentPolicy?.let { Policy(it.adBannerEnabled, it.adInterstitialEnabled, it.adAppOpenEnabled) })
        } catch (t: Throwable) {
            Log.w(TAG, "policy listener invoke failed", t)
        }
    }

    fun removePolicyFetchListener(listener: (Policy?) -> Unit) {
        synchronized(policyListeners) { policyListeners.remove(listener) }
    }

    fun addSplashReleaseListener(listener: () -> Unit) {
        synchronized(splashReleaseListeners) { splashReleaseListeners.add(listener) }
    }

    fun removeSplashReleaseListener(listener: () -> Unit) {
        synchronized(splashReleaseListeners) { splashReleaseListeners.remove(listener) }
    }

    fun triggerSplashRelease() {
        val copy: List<() -> Unit>
        synchronized(splashReleaseListeners) { copy = splashReleaseListeners.toList() }
        for (l in copy) {
            try {
                l.invoke()
            } catch (t: Throwable) {
                Log.w(TAG, "splash listener failed", t)
            }
        }
    }

    // accessors used by UI
    fun isPolicyFetchCompleted(): Boolean = true // keep existing callers simple
    fun isInterstitialEnabled(): Boolean = currentPolicy?.adInterstitialEnabled ?: false

    // Fail-safe: if policy not yet fetched, treat app-open as disabled so Supabase controls it
    fun isAppOpenEnabled(): Boolean = currentPolicy?.adAppOpenEnabled ?: false
    fun isFullScreenAdShowing(): Boolean = fullScreenAdShowing.get()

    // StateFlow-based accessor for reactive UI frameworks (use getFullScreenAdShowingFlow())
    // (removed duplicate-named accessor to avoid overload conflict)
    fun setInterstitialShowing(showing: Boolean) {
        interstitialShowing.set(showing)
    }

    fun setFullScreenAdShowing(showing: Boolean) {
        // atomically set and get previous state
        val previous = fullScreenAdShowing.getAndSet(showing)
        // update reactive StateFlow
        try {
            _fullScreenAdShowingFlow.value = showing
        } catch (_: Throwable) {
        }

        // ?”§ ?¬ë°œ ë°©ì?: FullScreen???«íˆë©?ë°°ë„ˆë¥??•ì‹¤?˜ê²Œ ë³µêµ¬
        if (previous && !showing) {
            try {
                // record dismissal time for cross-ad suppression logic
                lastFullScreenDismissedAt = System.currentTimeMillis()
            } catch (_: Throwable) {
            }
            try {
                Log.d(TAG, "setFullScreenAdShowing: false -> triggering banner restore")
                // ë°°ë„ˆ ê°•ì œ ë³µêµ¬ (forceHidden ?´ì œ + ?¬ë¡œ???¸ë¦¬ê±?
                ensureBannerVisible("fullScreenDismissed")
            } catch (_: Throwable) {
            }
        }

        // notify listeners of change
        val copy: List<(Boolean) -> Unit>
        synchronized(fullScreenListeners) { copy = fullScreenListeners.toList() }
        for (l in copy) {
            try {
                l.invoke(showing)
            } catch (_: Throwable) {
            }
        }
    }

    // Request banner reload by bumping the tick. Use when consent/state changed after full-screen ad.
    fun triggerBannerReload() {
        try {
            _bannerReloadTick.value = System.currentTimeMillis()
        } catch (_: Throwable) {
        }
    }

    fun addFullScreenShowListener(listener: (Boolean) -> Unit) {
        synchronized(fullScreenListeners) { fullScreenListeners.add(listener) }; try {
            listener.invoke(
                fullScreenAdShowing.get()
            )
        } catch (_: Throwable) {
        }
    }

    fun removeFullScreenShowListener(listener: (Boolean) -> Unit) {
        synchronized(fullScreenListeners) { fullScreenListeners.remove(listener) }
    }

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
            val policyStr =
                if (p == null) "null" else "appId=${p.appId} active=${p.isActive} interEnabled=${p.adInterstitialEnabled} interHourMax=${p.adInterstitialMaxPerHour} interDayMax=${p.adInterstitialMaxPerDay} appOpenHourMax=${p.appOpenMaxPerHour} appOpenDayMax=${p.appOpenMaxPerDay} cooldown=${p.appOpenCooldownSeconds}"
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
                } catch (_: Throwable) {
                }
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
            Log.d(
                TAG,
                "canShowInterstitialNow: policy=appId=${policy.appId} active=${policy.isActive} adInterstitialEnabled=${policy.adInterstitialEnabled} hourMax=${policy.adInterstitialMaxPerHour} dayMax=${policy.adInterstitialMaxPerDay} countsHour=${shownInterstitialThisHour.get()} countsDay=${shownInterstitialToday.get()}"
            )
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
            if (policy == null) {
                // ê°œë°œ ?¸ì˜: Debug ë¹Œë“œ?ì„œ???ê²© ?•ì±…???†ì„ ?Œë„ ?±ì˜¤?ˆì„ ?ˆìš©?˜ì—¬
                // ë¡œì»¬ ?ŒìŠ¤???”ë²„ê¹???ê´‘ê³  ?ë¦„???•ì¸?????ˆë„ë¡??©ë‹ˆ??
                try {
                    val cls = Class.forName(context.packageName + ".BuildConfig")
                    val f = cls.getDeclaredField("DEBUG")
                    val dbg = (f.get(null) as? Boolean) == true
                    if (dbg) {
                        Log.d(TAG, "canShowAppOpen: policy=null but DEBUG build -> allowing app-open for local testing")
                        return true
                    }
                } catch (_: Throwable) {}
                return false
            }
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
            } catch (_: Throwable) {
            }
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
            try {
                lastAppOpenShownAt = System.currentTimeMillis()
            } catch (_: Throwable) {
            }
        } catch (_: Throwable) {
        }
    }

    // Expose cooldown and recent shown check for clients
    fun getAppOpenCooldownSeconds(): Int = currentPolicy?.appOpenCooldownSeconds ?: 60

    // Expose server-controlled minimum gap between any two full-screen ads (seconds).
    fun getMinFullscreenGapSeconds(): Int = currentPolicy?.minFullscreenGapSeconds ?: 30
    fun getLastAppOpenShownAt(): Long = lastAppOpenShownAt
    fun isAppOpenInCooldown(): Boolean {
        try {
            val policy = currentPolicy ?: return false
            val cd = policy.appOpenCooldownSeconds
            if (cd <= 0) return false
            val last = lastAppOpenShownAt
            if (last == 0L) return false
            return (System.currentTimeMillis() - last) < cd * 1000L
        } catch (_: Throwable) {
            return false
        }
    }

    /**
     * Notify controller that a full-screen ad was dismissed right now.
     * This is a defensive helper for ad managers that may experience races
     * where the controller wasn't updated yet. It records the dismissal time
     * and triggers banner reloads as needed.
     */
    fun notifyFullScreenDismissed() {
        try {
            lastFullScreenDismissedAt = System.currentTimeMillis()
        } catch (_: Throwable) {
        }
        try {
            setFullScreenAdShowing(false)
        } catch (_: Throwable) {
        }
        try {
            triggerBannerReload()
        } catch (_: Throwable) {
        }
    }

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
                try {
                    lastInterstitialShownAt = System.currentTimeMillis()
                } catch (_: Throwable) {
                }

                // persist interstitial counters and window starts
                try {
                    val sp = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sp?.edit {
                        putLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        putLong(KEY_DAY_WINDOW_START, dayWindowStart)
                        putInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get())
                        putInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get())
                    }
                } catch (_: Throwable) {
                }

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
                        putLong(KEY_HOUR_WINDOW_START, hourWindowStart)
                        putLong(KEY_DAY_WINDOW_START, dayWindowStart)
                        putInt(KEY_INTER_HOUR_COUNT, shownInterstitialThisHour.get())
                        putInt(KEY_INTER_DAY_COUNT, shownInterstitialToday.get())
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "unreserveInterstitialSlot failed: $t")
        }
    }

}
