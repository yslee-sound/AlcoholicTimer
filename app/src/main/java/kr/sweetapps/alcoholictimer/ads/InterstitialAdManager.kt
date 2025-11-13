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
    private const val DEFAULT_COOLDOWN_MS = 2 * 60 * 1000L // 2 minutes

    private const val PREFS = "ad_prefs"
    private const val KEY_LAST_SHOWN_MS = "interstitial_last_shown_ms"
    private const val KEY_DAILY_COUNT = "interstitial_daily_count"
    private const val KEY_DAILY_DAY = "interstitial_daily_day"

    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)
    private val hasShownThisColdStart = AtomicBoolean(false)

    // simple listeners to notify callers when load succeeds/fails
    private val loadListeners = mutableListOf<(Boolean) -> Unit>()

    private fun isPolicyBypassed(): Boolean = BuildConfig.DEBUG

    fun preload(context: Context) {
        if (isLoading.get()) return
        if (interstitialAd != null) return
        isLoading.set(true)
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
                    Log.d(TAG, "onAdLoaded")
                    // notify listeners
                    notifyLoadListeners(true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading.set(false)
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
        // Daily cap
        if (state.dailyCount >= DEFAULT_DAILY_CAP) {
            return false to "dailycap"
        }
        // Cooldown
        val now = System.currentTimeMillis()
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
        if (!bypass && hasShownThisColdStart.get()) {
            Log.d(TAG, "Blocked: already shown this cold start")
            return false
        }
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
                Log.d(TAG, "onAdShowedFullScreenContent")
                if (!bypass) {
                    // 표시 성공 기록은 릴리즈 정책에서만 반영
                    recordShown(activity)
                }
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent")
                interstitialAd = null
                hasShownThisColdStart.set(true)
                onDismiss?.invoke()
                // 다음 기회 대비 즉시 프리로드
                preload(activity.applicationContext)
            }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "onAdFailedToShowFullScreenContent: $adError")
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

    fun resetColdStartGate() {
        hasShownThisColdStart.set(false)
    }
}

object AdHelpers {
    /** 전면광고가 표시되면 onAfterShown, 아니면 fallback 실행 */
    fun showOr(activity: Activity, fallback: () -> Unit) {
        val showed = InterstitialAdManager.maybeShowIfEligible(activity) { fallback() }
        if (!showed) fallback()
    }

    /** 전면 미로드 시 짧게 프리로드 시도 후 fallback */
    fun preloadThenShowOr(activity: Activity, timeoutMs: Long = 1200, fallback: () -> Unit) {
        if (InterstitialAdManager.isLoaded()) { showOr(activity, fallback); return }
        var handled = false
        InterstitialAdManager.addLoadListener { success ->
            if (!handled) {
                handled = true
                if (success) showOr(activity, fallback) else fallback()
            }
        }
        InterstitialAdManager.preload(activity.applicationContext)
        activity.window?.decorView?.postDelayed({ if (!handled) { handled = true; fallback() } }, timeoutMs)
    }
}
