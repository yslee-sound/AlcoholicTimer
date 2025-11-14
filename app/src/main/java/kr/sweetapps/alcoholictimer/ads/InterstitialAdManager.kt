package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kr.sweetapps.alcoholictimer.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object InterstitialAdManager {
    private const val TAG = "InterstitialAdManager"

    // Google's sample interstitial ad unit ID for testing (fallback only)
    private const val GOOGLE_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private fun currentUnitId(): String {
        val id: String = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
        return if (id.isBlank() || id.contains("REPLACE_WITH_REAL_INTERSTITIAL")) GOOGLE_TEST_INTERSTITIAL_ID else id
    }

    // Policy defaults (can be wired to Remote Config later)
    private const val DEFAULT_DAILY_CAP = 3
    private const val DEFAULT_COOLDOWN_MS = 60 * 1000L // 60 seconds
    private const val INITIAL_PROTECTION_MS = 60 * 1000L // 60 seconds after cold start

    private const val PREFS = "ad_prefs"
    private const val KEY_LAST_SHOWN_MS = "interstitial_last_shown_ms"
    private const val KEY_DAILY_COUNT = "interstitial_daily_count"
    private const val KEY_DAILY_DAY = "interstitial_daily_day"

    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)

    // Deprecated: cold start once-per-session gate (disabled)
    @Deprecated("Cold start one-time gate is disabled. Use initial protection window instead.")
    private val hasShownThisColdStart = AtomicBoolean(false)

    // Initial protection: record app start time to block interstitials briefly after cold start
    private var appStartMs: Long = 0L
    fun noteAppStart() { appStartMs = System.currentTimeMillis() }

    // simple listeners to notify callers when load succeeds/fails
    private val loadListeners = mutableListOf<(Boolean) -> Unit>()

    private fun isPolicyBypassed(): Boolean = BuildConfig.DEBUG

    fun preload(context: Context) {
        if (isLoading.get()) return
        if (interstitialAd != null) return
        isLoading.set(true)
        try { AdController.setInterstitialLoading(true) } catch (_: Throwable) {}
        val adRequest = AdRequest.Builder().build()
        val unitId = currentUnitId()
        Log.d(TAG, "Loading interstitial with unitId=$unitId (debug=${BuildConfig.DEBUG})")
        InterstitialAd.load(
            context,
            unitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading.set(false)
                    try { AdController.setInterstitialLoaded(true); AdController.setInterstitialLoading(false); AdController.setInterstitialLastError(null) } catch (_: Throwable) {}
                    Log.d(TAG, "onAdLoaded")
                    // notify listeners
                    notifyLoadListeners(true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading.set(false)
                    try { AdController.setInterstitialLastError(error.toString()); AdController.setInterstitialLoading(false) } catch (_: Throwable) {}
                    Log.w(TAG, "onAdFailedToLoad: $error")
                    notifyLoadListeners(false)
                }
            }
        )
    }

    private fun notifyLoadListeners(success: Boolean) {
        val copy = ArrayList(loadListeners)
        loadListeners.clear()
        for (l in copy) {
            try { l(success) } catch (t: Throwable) { Log.w(TAG, "Listener threw: $t") }
        }
    }

    fun addLoadListener(listener: (Boolean) -> Unit) {
        loadListeners.add(listener)
    }

    fun isLoaded(): Boolean = interstitialAd != null

    private fun currentDayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private data class PolicyState(
        val dailyCount: Int,
        val dayKey: String,
        val lastShownMs: Long
    )

    private fun readPolicyState(context: Context): PolicyState {
        val sp = getPrefs(context)
        val day = sp.getString(KEY_DAILY_DAY, null)
        val today = currentDayKey()
        val count = if (day == today) sp.getInt(KEY_DAILY_COUNT, 0) else 0
        val lastMs = sp.getLong(KEY_LAST_SHOWN_MS, 0L)
        return PolicyState(count, today, lastMs)
    }

    private fun writePolicyState(context: Context, update: PolicyState) {
        getPrefs(context).edit()
            .putString(KEY_DAILY_DAY, update.dayKey)
            .putInt(KEY_DAILY_COUNT, update.dailyCount)
            .putLong(KEY_LAST_SHOWN_MS, update.lastShownMs)
            .apply()
    }

    private fun passesPolicy(context: Context): Pair<Boolean, String?> {
        val state = readPolicyState(context)
        // Initial protection after cold start
        val now = System.currentTimeMillis()
        if (appStartMs > 0L) {
            val sinceStart = now - appStartMs
            if (sinceStart < INITIAL_PROTECTION_MS) {
                return false to "initial_protection"
            }
        }
        // Daily cap
        if (state.dailyCount >= DEFAULT_DAILY_CAP) {
            return false to "dailycap"
        }
        // Cooldown after shown
        val since = now - state.lastShownMs
        if (state.lastShownMs > 0L && since < DEFAULT_COOLDOWN_MS) {
            return false to "cooldown"
        }
        return true to null
    }

    private fun recordShown(context: Context) {
        val prev = readPolicyState(context)
        val newState = prev.copy(
            dailyCount = prev.dailyCount + 1,
            lastShownMs = System.currentTimeMillis()
        )
        writePolicyState(context, newState)
    }

    fun maybeShowIfEligible(
        activity: Activity,
        onDismiss: (() -> Unit)? = null
    ): Boolean {
        val bypass = isPolicyBypassed()
        // Cold start once-per-session gate disabled
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Blocked: ad not loaded${if (bypass) " (debug: will just return)" else ""}")
            return false
        }
        if (activity.isFinishing || activity.isDestroyed) {
            Log.d(TAG, "Blocked: invalid activity state")
            return false
        }
        if (!bypass) {
            val (pass, reason) = passesPolicy(activity)
            if (!pass) {
                Log.d(TAG, "Blocked by policy: $reason")
                return false
            }
        } else {
            Log.d(TAG, "Policy bypassed (debug): always show if loaded")
        }
        // 콜백 주입
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowing.set(true)
                Log.d(TAG, "onAdShowedFullScreenContent")
                AdController.setInterstitialShowing(true)  // 배너 숨김
                AdController.setFullScreenAdShowing(true)
                if (!bypass) {
                    // 표시 성공 기록은 릴리즈 정책에서만 반영
                    recordShown(activity)
                }
            }
            override fun onAdDismissedFullScreenContent() {
                isShowing.set(false)
                Log.d(TAG, "onAdDismissedFullScreenContent")
                AdController.setInterstitialShowing(false)  // 배너 다시 표시
                AdController.setFullScreenAdShowing(false)
                try { AdController.setInterstitialLoaded(false); AdController.setInterstitialLastError(null) } catch (_: Throwable) {}
                interstitialAd = null
                onDismiss?.invoke()
                // 다음 기회 대비 즉시 프리로드
                preload(activity.applicationContext)
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                isShowing.set(false)
                Log.w(TAG, "onAdFailedToShowFullScreenContent: $adError")
                AdController.setInterstitialShowing(false)  // 실패 시에도 배너 복구
                AdController.setFullScreenAdShowing(false)
                try { AdController.setInterstitialLastError(adError.toString()) } catch (_: Throwable) {}
                interstitialAd = null
                onDismiss?.invoke()
                // 실패 시에도 다음 기회 대비 프리로드
                preload(activity.applicationContext)
            }
        }
        Handler(Looper.getMainLooper()).post {
            try {
                ad.show(activity)
            } catch (t: Throwable) {
                Log.w(TAG, "Show failed: $t")
                interstitialAd = null
                onDismiss?.invoke()
                preload(activity.applicationContext)
            }
        }
        return true
    }

    // 광고가 현재 표시 중인지 여부 반환
    fun isShowingAd(): Boolean = isShowing.get()

    @Deprecated("No-op: Cold start one-time gate removed.")
    fun resetColdStartGate() { /* no-op */ }
}
