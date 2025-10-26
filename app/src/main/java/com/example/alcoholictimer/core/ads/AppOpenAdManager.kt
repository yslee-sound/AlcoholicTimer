package com.sweetapps.alcoholictimer.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
    private var isAdFullyLoaded = false  // 광고가 완전히 로드되어 표시 가능한 상태

    private var currentActivity: Activity? = null
    private var isShowingAd = false

    // 콜드 스타트 플래그 (앱 프로세스 시작 후 첫 foreground 전환)
    private var isColdStart = true

    // 포그라운드 전환 시 광고 표시를 한 번만 시도하기 위한 플래그
    private var shouldShowAdOnResume = false

    // Handler for delayed ad display
    private val handler = Handler(Looper.getMainLooper())
    private var adShowRunnable: Runnable? = null

    companion object {
        private const val TAG = "AppOpenAdManager"

        // Google 테스트 앱 오프닝 광고 ID
        private const val GOOGLE_TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

        // 광고 유효 시간 (4시간)
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L

        // 정책 기본값
        private const val DEFAULT_DAILY_CAP = 5  // 일일 최대 5회
        private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L  // 5분 쿨다운

        // Activity 안정화 대기 시간 (500ms)
        private const val AD_SHOW_DELAY_MS = 500L

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
        val available = appOpenAd != null && wasLoadTimeLessThanNHoursAgo() && isAdFullyLoaded
        Log.d(TAG, "isAdAvailable: $available (ad=${appOpenAd != null}, timeValid=${wasLoadTimeLessThanNHoursAgo()}, fullyLoaded=$isAdFullyLoaded)")
        return available
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

    /** 디버그 모드에서만 토스트 메시지 표시 */
    private fun showDebugToast(message: String) {
        if (BuildConfig.DEBUG) {
            handler.post {
                Toast.makeText(application, "🎯 앱오프닝: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 광고 로드 */
    fun loadAd() {
        // 이미 로딩 중이면 스킵
        if (isLoadingAd) {
            Log.d(TAG, "Skipping load: already loading")
            return
        }

        // 유효한 광고가 이미 있으면 스킵
        if (isAdAvailable()) {
            Log.d(TAG, "Skipping load: valid ad already available")
            return
        }

        isLoadingAd = true
        isAdFullyLoaded = false  // 로딩 시작하면 플래그 초기화
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
                    isAdFullyLoaded = true  // 광고가 완전히 로드되어 표시 가능
                    Log.d(TAG, "App open ad loaded successfully at ${Date()} - Ready to show")
                    showDebugToast("📥 광고 로드 완료 - 다음 복귀 시 표시됩니다")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    isAdFullyLoaded = false
                    Log.w(TAG, "App open ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                    showDebugToast("❌ 광고 로드 실패 (${loadAdError.code})")
                }
            }
        )
    }

    /** 광고 표시 */
    private fun showAdIfAvailable(activity: Activity) {
        // 이미 광고 표시 중이면 스킵 (이중 체크)
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing")
            showDebugToast("❌ 실패: 이미 광고 표시 중 (이중 체크)")
            return
        }

        // 광고가 없으면 새로 로드 (이중 체크)
        if (!isAdAvailable()) {
            Log.d(TAG, "Ad is not available, loading new ad")
            showDebugToast("❌ 실패: 광고 로드 안 됨 (이중 체크, 로드 시작)")
            loadAd()
            return
        }

        // 디버그 모드가 아닐 때만 정책 체크
        if (!isPolicyBypassed()) {
            val (pass, reason) = passesPolicy()
            if (!pass) {
                Log.d(TAG, "Blocked by policy: $reason")
                val reasonText = when(reason) {
                    "dailycap" -> "일일 제한 (5회) 초과"
                    "cooldown" -> "쿨다운 (5분) 미충족"
                    else -> "정책 차단: $reason"
                }
                showDebugToast("❌ 실패: $reasonText")
                return
            }
        } else {
            Log.d(TAG, "Policy bypassed (debug): showing ad")
            // 성공 토스트는 광고가 실제로 표시될 때만 (onAdShowedFullScreenContent)
        }

        val ad = appOpenAd ?: return

        // 광고 표시 시작 시간 기록
        val adShowStartTime = System.currentTimeMillis()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                val showDelay = System.currentTimeMillis() - adShowStartTime
                isShowingAd = true
                Log.d(TAG, "App open ad showed full screen content (delay: ${showDelay}ms)")

                // 정상적으로 표시됨
                showDebugToast("✅ 광고 표시 중...")

                // 정상 표시된 경우에만 노출 기록 (디버그에서는 기록 안 함)
                if (!isPolicyBypassed()) {
                    recordShown()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                val totalDisplayTime = System.currentTimeMillis() - adShowStartTime
                appOpenAd = null
                isAdFullyLoaded = false
                isShowingAd = false

                Log.d(TAG, "App open ad dismissed (total display time: ${totalDisplayTime}ms)")

                // 광고가 정상적으로 표시되었는지 체크
                if (totalDisplayTime < 500) {
                    // 너무 빨리 닫힌 광고 - 비정상으로 간주
                    Log.w(TAG, "Ad dismissed too quickly (${totalDisplayTime}ms), marking ad as invalid")
                    showDebugToast("⚠️ 광고 오류 (${totalDisplayTime}ms) - 즉시 재로드")
                }

                // 정상이든 비정상이든 즉시 다음 광고 로드 (일관성 유지)
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isAdFullyLoaded = false
                isShowingAd = false

                Log.w(TAG, "App open ad failed to show: ${adError.message} (code: ${adError.code})")

                val errorMessage = when {
                    adError.message?.contains("Timeout", ignoreCase = true) == true -> {
                        "타임아웃 - 네트워크를 확인하세요"
                    }
                    adError.code == 1 -> {
                        "광고 로드 실패 - 인터넷 연결 확인"
                    }
                    else -> {
                        "광고 오류 (${adError.code})"
                    }
                }

                showDebugToast("❌ $errorMessage")

                // 실패한 광고는 즉시 재로드
                loadAd()
            }
        }

        // 광고 표시 시도
        Log.d(TAG, "Calling ad.show() on activity: ${activity.javaClass.simpleName}")
        ad.show(activity)
    }

    /** 앱이 포그라운드로 전환될 때 호출 (DefaultLifecycleObserver) */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        Log.d(TAG, "ProcessLifecycle.onStart() - isColdStart=$isColdStart")

        // 이전에 예약된 광고 표시 작업 취소
        cancelPendingAdShow()

        // 콜드 스타트인 경우 플래그만 해제하고 광고를 표시하지 않음
        if (isColdStart) {
            Log.d(TAG, "First onStart after cold start - resetting flag, will NOT show ad")
            showDebugToast("❌ 실패: 콜드 스타트 (스플래시 충돌 방지)")
            isColdStart = false
            shouldShowAdOnResume = false
            return
        }

        // 콜드 스타트가 아닌 경우, Activity가 Resume될 때 광고를 표시하도록 플래그 설정
        Log.d(TAG, "Not cold start - setting flag to show ad on next activity resume")
        shouldShowAdOnResume = true
    }

    /** 앱이 백그라운드로 전환될 때 호출 */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "ProcessLifecycle.onStop()")
        shouldShowAdOnResume = false
        cancelPendingAdShow()
    }

    /** 예약된 광고 표시 작업 취소 */
    private fun cancelPendingAdShow() {
        adShowRunnable?.let {
            handler.removeCallbacks(it)
            Log.d(TAG, "Cancelled pending ad show")
        }
        adShowRunnable = null
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

            // 플래그가 있든 없든 무조건 체크하고 토스트 표시
            if (shouldShowAdOnResume) {
                Log.d(TAG, "Activity resumed with shouldShowAdOnResume=true, checking ad availability")
                shouldShowAdOnResume = false

                // 광고 표시 전 상태 확인 및 토스트 표시
                if (isShowingAd) {
                    showDebugToast("❌ 실패: 이미 광고 표시 중")
                    return
                }

                if (!isAdAvailable()) {
                    showDebugToast("❌ 실패: 광고 로드 안 됨 (로드 시작)")
                    loadAd()
                    return
                }

                // 상태 확인 통과 - 광고 표시 예약 (토스트는 실제 표시 시도 시점에)
                // Activity가 완전히 안정될 때까지 짧은 딜레이 후 광고 표시
                adShowRunnable = Runnable {
                    Log.d(TAG, "Delayed ad show triggered")
                    currentActivity?.let { act ->
                        if (!isShowingAd) {
                            showAdIfAvailable(act)
                        } else {
                            Log.d(TAG, "Ad already showing, skipping")
                            showDebugToast("❌ 실패: 이미 광고 표시 중 (딜레이 후)")
                        }
                    }
                    adShowRunnable = null
                }
                handler.postDelayed(adShowRunnable!!, AD_SHOW_DELAY_MS)
            } else {
                // 플래그가 없는 경우 - 정상적인 상황들:
                // 1. 콜드 스타트 직후 (이미 onStart에서 토스트 표시함)
                // 2. 앱 내부 네비게이션 (다른 Activity 복귀)
                // 3. 화면 회전 등
                Log.d(TAG, "Activity resumed but shouldShowAdOnResume=false (normal for cold start or internal navigation)")
                // 토스트 표시하지 않음 - 정상 동작
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // 사용자가 빠르게 나간 경우 예약된 광고 표시 취소
        if (!isShowingAd) {
            cancelPendingAdShow()
        }
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}

