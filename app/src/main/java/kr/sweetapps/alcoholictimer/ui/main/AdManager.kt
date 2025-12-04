package kr.sweetapps.alcoholictimer.ui.main

import kotlin.math.max
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import java.util.TimeZone

interface PreferencesStore {
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
}

class InMemoryPreferencesStore : PreferencesStore {
    private val longs = mutableMapOf<String, Long>()
    private val ints = mutableMapOf<String, Int>()
    override fun getLong(key: String, default: Long): Long = longs[key] ?: default
    override fun putLong(key: String, value: Long) { longs[key] = value }
    override fun getInt(key: String, default: Int): Int = ints[key] ?: default
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun remove(key: String) { longs.remove(key); ints.remove(key) }
}

class AdManager(
    private var policy: AdPolicyConfig,
    private val prefs: PreferencesStore,
    private val timeProvider: TimeProvider
) {
    // ?�태
    var isShowingInterstitialAd: Boolean = false
        private set

    // ?�????
    private val KEY_HOUR_WINDOW_START = "ad_hour_window_start"
    private val KEY_DAY_WINDOW_START = "ad_day_window_start"
    private val KEY_HOUR_COUNT_INTERSTITIAL = "hour_count_interstitial"
    private val KEY_DAY_COUNT_INTERSTITIAL = "day_count_interstitial"
    private val KEY_HOUR_COUNT_APP_OPEN = "hour_count_app_open"
    private val KEY_DAY_COUNT_APP_OPEN = "day_count_app_open"
    private val KEY_LAST_APP_OPEN_TIME = "last_app_open_time"

    private var hourWindowStart: Long = 0L
    private var dayWindowStart: Long = 0L
    private var hourCountInterstitial: Int = 0
    private var dayCountInterstitial: Int = 0
    private var hourCountAppOpen: Int = 0
    private var dayCountAppOpen: Int = 0
    private var lastAppOpenTime: Long = 0L

    init {
        loadFromPrefs()
        ensureWindows()
    }

    private fun startOfHour(millis: Long): Long = millis - (millis % 3_600_000L)
    private fun startOfDay(millis: Long): Long {
        val tzOffset = TimeZone.getDefault().getOffset(millis)
        return (millis + tzOffset) / 86_400_000L * 86_400_000L - tzOffset
    }

    private fun ensureWindows() {
        val now = timeProvider.nowMillis()
        val hStart = startOfHour(now)
        val dStart = startOfDay(now)
        if (hourWindowStart != hStart) {
            hourWindowStart = hStart
            hourCountInterstitial = 0
            hourCountAppOpen = 0
        }
        if (dayWindowStart != dStart) {
            dayWindowStart = dStart
            dayCountInterstitial = 0
            dayCountAppOpen = 0
        }
        saveWindowStarts()
    }

    private fun loadFromPrefs() {
        hourWindowStart = prefs.getLong(KEY_HOUR_WINDOW_START, 0L)
        dayWindowStart = prefs.getLong(KEY_DAY_WINDOW_START, 0L)
        hourCountInterstitial = prefs.getInt(KEY_HOUR_COUNT_INTERSTITIAL, 0)
        dayCountInterstitial = prefs.getInt(KEY_DAY_COUNT_INTERSTITIAL, 0)
        hourCountAppOpen = prefs.getInt(KEY_HOUR_COUNT_APP_OPEN, 0)
        dayCountAppOpen = prefs.getInt(KEY_DAY_COUNT_APP_OPEN, 0)
        lastAppOpenTime = prefs.getLong(KEY_LAST_APP_OPEN_TIME, 0L)
    }

    private fun saveWindowStarts() {
        prefs.putLong(KEY_HOUR_WINDOW_START, hourWindowStart)
        prefs.putLong(KEY_DAY_WINDOW_START, dayWindowStart)
        prefs.putInt(KEY_HOUR_COUNT_INTERSTITIAL, hourCountInterstitial)
        prefs.putInt(KEY_DAY_COUNT_INTERSTITIAL, dayCountInterstitial)
        prefs.putInt(KEY_HOUR_COUNT_APP_OPEN, hourCountAppOpen)
        prefs.putInt(KEY_DAY_COUNT_APP_OPEN, dayCountAppOpen)
        prefs.putLong(KEY_LAST_APP_OPEN_TIME, lastAppOpenTime)
    }

    fun updatePolicy(newPolicy: AdPolicyConfig) {
        policy = newPolicy
    }

    fun canShowAd(adType: AdType): Boolean {
        ensureWindows()
        val now = timeProvider.nowMillis()
        // If a full-screen popup/overlay is active (e.g., emergency/update), never show ads
        try {
            if (AdController.isFullScreenAdShowing()) return false
        } catch (_: Throwable) {}
        // 최상???�책 비활?�화 ??모든 광고 ?�시 불�?
        try {
            if (!policy.is_active) return false
        } catch (_: Throwable) {}
        when (adType) {
            AdType.INTERSTITIAL -> {
                if (!policy.ad_interstitial_enabled) return false
                if (hourCountInterstitial >= policy.ad_interstitial_max_per_hour) return false
                if (dayCountInterstitial >= policy.ad_interstitial_max_per_day) return false
                if (isShowingInterstitialAd) return false
                return true
            }
            AdType.APP_OPEN -> {
                if (!policy.ad_app_open_enabled) return false
                if (hourCountAppOpen >= policy.app_open_max_per_hour) return false
                if (dayCountAppOpen >= policy.app_open_max_per_day) return false
                val cooldownMs = policy.app_open_cooldown_seconds.toLong() * 1000L
                if (lastAppOpenTime > 0 && now - lastAppOpenTime < cooldownMs) return false
                return true
            }
            AdType.BANNER -> {
                return policy.ad_banner_enabled
            }
        }
    }

    fun incrementAdCount(adType: AdType) {
        ensureWindows()
        val now = timeProvider.nowMillis()
        when (adType) {
            AdType.INTERSTITIAL -> {
                hourCountInterstitial = max(0, hourCountInterstitial + 1)
                dayCountInterstitial = max(0, dayCountInterstitial + 1)
            }
            AdType.APP_OPEN -> {
                hourCountAppOpen = max(0, hourCountAppOpen + 1)
                dayCountAppOpen = max(0, dayCountAppOpen + 1)
                lastAppOpenTime = now
            }
            AdType.BANNER -> {
                // 배너??카운?�하지 ?�음
            }
        }
        saveWindowStarts()
    }

    // ?�스?�용 ?�근??
    fun getHourCountInterstitial(): Int = hourCountInterstitial
    fun getDayCountInterstitial(): Int = dayCountInterstitial
    fun getHourCountAppOpen(): Int = hourCountAppOpen
    fun getDayCountAppOpen(): Int = dayCountAppOpen
    fun getLastAppOpenTime(): Long = lastAppOpenTime
}

interface TimeProvider {
    fun nowMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

enum class AdType {
    BANNER, INTERSTITIAL, APP_OPEN
}

data class AdPolicyConfig(
    val is_active: Boolean = true,
    val ad_banner_enabled: Boolean = true,
    val ad_interstitial_enabled: Boolean = true,
    val ad_app_open_enabled: Boolean = true,
    val ad_interstitial_max_per_hour: Int = Int.MAX_VALUE,
    val ad_interstitial_max_per_day: Int = Int.MAX_VALUE,
    val app_open_max_per_hour: Int = Int.MAX_VALUE,
    val app_open_max_per_day: Int = Int.MAX_VALUE,
    val app_open_cooldown_seconds: Int = 0,
    val min_fullscreen_gap_seconds: Int = 0
)