package com.sweetapps.alcoholictimer.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.sweetapps.alcoholictimer.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App Open Ad Manager
 *
 * 앱이 백그라운드에서 포그라운드로 전환될 때 전면 광고를 표시합니다.
 * - 콜드 스타트 시에는 표시하지 않음 (스플래시 화면과의 충돌 방지)
 * - 일일 노출 횟수 제한 및 쿨다운 적용
 */
class AppOpenAdManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0

    private var currentActivity: Activity? = null
    private var isShowingAd = false

    // 콜드 스타트 플래그 (앱 프로세스 시작 후 첫 foreground 전환)
    private var isColdStart = true

    companion object {
        private const val TAG = "AppOpenAdManager"

        // Google 테스트 앱 오프닝 광고 ID
        private const val GOOGLE_TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

        // 광고 유효 시간 (4시간)
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L

        // 정책 기본값
        private const val DEFAULT_DAILY_CAP = 5  // 일일 최대 5회
        private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L  // 5분 쿨다운

        // SharedPreferences
        private const val PREFS = "ad_prefs"
        private const val KEY_LAST_SHOWN_MS = "app_open_last_shown_ms"
        private const val KEY_DAILY_COUNT = "app_open_daily_count"
        private const val KEY_DAILY_DAY = "app_open_daily_day"
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun currentUnitId(): String {
        val id: String = BuildConfig.ADMOB_APP_OPEN_UNIT_ID
        return if (id.isBlank() || id.contains("REPLACE_WITH_REAL_APP_OPEN")) {
            GOOGLE_TEST_APP_OPEN_ID
        } else {
            id
        }
    }

    /** 광고가 유효한지 확인 (4시간 이내 로드된 광고) */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = Date().time - loadTime
        return dateDifference < AD_TIMEOUT_MS
    }

    private fun currentDayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun getPrefs() = application.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    private data class PolicyState(
        val dailyCount: Int,
        val dayKey: String,
        val lastShownMs: Long
    )

    private fun readPolicyState(): PolicyState {
        val sp = getPrefs()
        val day = sp.getString(KEY_DAILY_DAY, null)
        val today = currentDayKey()
        val count = if (day == today) sp.getInt(KEY_DAILY_COUNT, 0) else 0
        val lastMs = sp.getLong(KEY_LAST_SHOWN_MS, 0L)
        return PolicyState(count, today, lastMs)
    }

    private fun writePolicyState(update: PolicyState) {
        getPrefs().edit {
            putString(KEY_DAILY_DAY, update.dayKey)
            putInt(KEY_DAILY_COUNT, update.dailyCount)
            putLong(KEY_LAST_SHOWN_MS, update.lastShownMs)
        }
    }

    private fun passesPolicy(): Pair<Boolean, String?> {
        val state = readPolicyState()

        // 일일 노출 횟수 제한
        if (state.dailyCount >= DEFAULT_DAILY_CAP) {
            return false to "dailycap"
        }

        // 쿨다운 체크
        val now = System.currentTimeMillis()
        val since = now - state.lastShownMs
        if (state.lastShownMs > 0L && since < DEFAULT_COOLDOWN_MS) {
            return false to "cooldown"
        }

        return true to null
    }

    private fun recordShown() {
        val prev = readPolicyState()
        val newState = prev.copy(
            dailyCount = prev.dailyCount + 1,
            lastShownMs = System.currentTimeMillis()
        )
        writePolicyState(newState)
    }

    private fun isPolicyBypassed(): Boolean = BuildConfig.DEBUG

    /** 광고 로드 */
    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        val unitId = currentUnitId()

        Log.d(TAG, "Loading app open ad with unitId=$unitId (debug=${BuildConfig.DEBUG})")

        AppOpenAd.load(
            application,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(TAG, "App open ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.w(TAG, "App open ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /** 광고 표시 */
    private fun showAdIfAvailable(activity: Activity) {
        // 이미 광고 표시 중이면 스킵
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing")
            return
        }

        // 광고가 없으면 새로 로드
        if (!isAdAvailable()) {
            Log.d(TAG, "Ad is not available, loading new ad")
            loadAd()
            return
        }

        // 콜드 스타트 시에는 표시하지 않음
        if (isColdStart) {
            Log.d(TAG, "Skipping ad on cold start")
            return
        }

        // 디버그 모드가 아닐 때만 정책 체크
        if (!isPolicyBypassed()) {
            val (pass, reason) = passesPolicy()
            if (!pass) {
                Log.d(TAG, "Blocked by policy: $reason")
                return
            }
        } else {
            Log.d(TAG, "Policy bypassed (debug): showing ad")
        }

        val ad = appOpenAd ?: return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "App open ad showed full screen content")

                // 디버그 모드가 아닐 때만 노출 기록
                if (!isPolicyBypassed()) {
                    recordShown()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                Log.d(TAG, "App open ad dismissed")

                // 다음 광고 미리 로드
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.w(TAG, "App open ad failed to show: ${adError.message}")

                // 다음 광고 미리 로드
                loadAd()
            }
        }

        ad.show(activity)
    }

    /** 앱이 포그라운드로 전환될 때 호출 (DefaultLifecycleObserver) */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let { activity ->
            showAdIfAvailable(activity)
        }

        // 첫 번째 foreground 전환 이후에는 콜드 스타트가 아님
        if (isColdStart) {
            isColdStart = false
        }
    }

    /** 콜드 스타트 플래그 리셋 (Application.onCreate에서 호출) */
    fun resetColdStart() {
        isColdStart = true
    }

    // ActivityLifecycleCallbacks 구현
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}

