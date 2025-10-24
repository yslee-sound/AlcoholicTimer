package com.sweetapps.alcoholictimer.core.ads

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.sweetapps.alcoholictimer.BuildConfig
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 네이티브 광고 로더 및 정책 관리 싱글톤
 * - 디버그: 정책 우회, 항상 허용
 * - 릴리즈: 일일 캡(1-2회), 쿨다운(2분), UMP 동의 게이팅
 */
object NativeAdManager {
    private const val TAG = "NativeAdManager"

    // Google 테스트 네이티브 Advanced 유닛 ID
    private const val GOOGLE_TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    private fun currentUnitId(): String {
        val id = BuildConfig.ADMOB_NATIVE_UNIT_ID
        return if (id.isBlank() || id.contains("REPLACE_WITH_REAL_NATIVE")) {
            GOOGLE_TEST_NATIVE_ID
        } else {
            id
        }
    }

    // 정책 기본값
    private const val DEFAULT_DAILY_CAP = 2 // 뒤로가기 팝업: 하루 최대 2회
    private const val DEFAULT_COOLDOWN_MS = 2 * 60 * 1000L // 2분

    private const val PREFS = "ad_prefs"
    private const val KEY_NATIVE_LAST_SHOWN_MS = "native_last_shown_ms"
    private const val KEY_NATIVE_DAILY_COUNT = "native_daily_count"
    private const val KEY_NATIVE_DAILY_DAY = "native_daily_day"

    private var cachedNativeAd: NativeAd? = null
    private val isLoading = AtomicBoolean(false)

    private fun isPolicyBypassed(): Boolean = BuildConfig.DEBUG

    /**
     * 네이티브 광고 미리 로드
     */
    fun preload(context: Context) {
        if (isLoading.get()) {
            Log.d(TAG, "Already loading, skip")
            return
        }
        if (cachedNativeAd != null) {
            Log.d(TAG, "Ad already loaded, skip")
            return
        }

        isLoading.set(true)
        val unitId = currentUnitId()
        Log.d(TAG, "Loading native ad with unitId=$unitId (debug=${BuildConfig.DEBUG})")

        val adLoader = AdLoader.Builder(context, unitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad loaded successfully")
                cachedNativeAd = nativeAd
                isLoading.set(false)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                    cachedNativeAd = null
                    isLoading.set(false)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * 로드된 네이티브 광고 획득 (소유권 이전)
     * 호출자는 반드시 사용 후 destroy() 호출 필요
     */
    fun acquire(): NativeAd? {
        val ad = cachedNativeAd
        cachedNativeAd = null
        if (ad != null) {
            Log.d(TAG, "Ad acquired, will reload in background")
        }
        return ad
    }

    /**
     * 광고가 로드되어 있는지 확인
     */
    fun isLoaded(): Boolean = cachedNativeAd != null

    /**
     * 정책 체크: 광고 표시가 허용되는지 확인
     * @return true if allowed, false otherwise
     */
    fun canShowAd(context: Context): Boolean {
        if (isPolicyBypassed()) {
            Log.d(TAG, "Policy bypassed (DEBUG)")
            return true
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = currentDayKey()

        // 일일 카운트 체크
        val storedDay = prefs.getString(KEY_NATIVE_DAILY_DAY, "")
        val dailyCount = if (storedDay == todayKey) {
            prefs.getInt(KEY_NATIVE_DAILY_COUNT, 0)
        } else {
            0 // 날짜가 바뀌면 초기화
        }

        if (dailyCount >= DEFAULT_DAILY_CAP) {
            Log.d(TAG, "Daily cap reached ($dailyCount/$DEFAULT_DAILY_CAP)")
            return false
        }

        // 쿨다운 체크
        val lastShown = prefs.getLong(KEY_NATIVE_LAST_SHOWN_MS, 0L)
        val elapsed = now - lastShown
        if (elapsed < DEFAULT_COOLDOWN_MS) {
            val remainSec = (DEFAULT_COOLDOWN_MS - elapsed) / 1000
            Log.d(TAG, "Cooldown active (remain ${remainSec}s)")
            return false
        }

        return true
    }

    /**
     * 광고 표시 후 호출 (정책 카운터 업데이트)
     */
    fun recordShown(context: Context) {
        if (isPolicyBypassed()) {
            Log.d(TAG, "Policy bypassed, skip recording")
            return
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = currentDayKey()

        val storedDay = prefs.getString(KEY_NATIVE_DAILY_DAY, "")
        val dailyCount = if (storedDay == todayKey) {
            prefs.getInt(KEY_NATIVE_DAILY_COUNT, 0)
        } else {
            0
        }

        prefs.edit()
            .putLong(KEY_NATIVE_LAST_SHOWN_MS, now)
            .putInt(KEY_NATIVE_DAILY_COUNT, dailyCount + 1)
            .putString(KEY_NATIVE_DAILY_DAY, todayKey)
            .apply()

        Log.d(TAG, "Recorded show: count=${dailyCount + 1}/$DEFAULT_DAILY_CAP")

        // 바로 재로드 시작
        val appContext = context.applicationContext
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            preload(appContext)
        }, 500)
    }

    private fun currentDayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }
}

