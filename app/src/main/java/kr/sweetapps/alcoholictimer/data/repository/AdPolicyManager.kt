// [NEW] 광고 정책 관리자 - 쿨타임 제어 통합
package kr.sweetapps.alcoholictimer.data.repository

import android.content.Context
import android.util.Log
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * 광고 정책을 통합 관리하는 싱글톤 객체
 * - 전면 광고 쿨타임 관리
 * - 디버그 모드에서 쿨타임 오버라이드 가능
 * - 마지막 광고 노출 시간 추적
 */
object AdPolicyManager {
    private const val TAG = "AdPolicyManager"

    // SharedPreferences 키
    private const val PREFS_NAME = "ad_policy_prefs"
    private const val KEY_LAST_INTERSTITIAL_TIME_MS = "last_interstitial_time_ms"
    private const val KEY_DEBUG_AD_COOL_DOWN_SECONDS = "debug_ad_cool_down_seconds"

    // 기본 정책 값 (릴리즈용)
    private const val DEFAULT_INTERSTITIAL_INTERVAL_SECONDS = 1800L // 30분

    /**
     * 전면 광고 쿨타임 간격(초)을 반환
     *
     * 우선순위:
     * 1. 디버그 모드 오버라이드 값 (DEBUG 빌드만)
     * 2. Firebase Remote Config 값 (추후 구현)
     * 3. 하드코딩된 기본값 (1800초 = 30분)
     *
     * @param context Context
     * @return 쿨타임 간격(초)
     */
    fun getInterstitialIntervalSeconds(context: Context): Long {
        // [1단계] 디버그 모드 오버라이드 확인 (DEBUG 빌드만)
        if (BuildConfig.DEBUG) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val debugInterval = prefs.getLong(KEY_DEBUG_AD_COOL_DOWN_SECONDS, -1L)

                if (debugInterval > 0) {
                    Log.d(TAG, "디버그 모드 쿨타임 오버라이드: $debugInterval 초")
                    return debugInterval
                }
            } catch (t: Throwable) {
                Log.e(TAG, "디버그 쿨타임 로드 실패", t)
            }
        }

        // [2단계] Firebase Remote Config (추후 구현)
        // TODO: Firebase Remote Config에서 값 가져오기

        // [3단계] 기본값 반환
        Log.d(TAG, "기본 쿨타임 사용: $DEFAULT_INTERSTITIAL_INTERVAL_SECONDS 초 (30분)")
        return DEFAULT_INTERSTITIAL_INTERVAL_SECONDS
    }

    /**
     * 전면 광고 노출 가능 여부를 결정
     *
     * @param context Context
     * @return true이면 광고 노출 가능, false이면 쿨타임 중
     */
    fun shouldShowInterstitialAd(context: Context): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. 쿨타임 간격 가져오기
            val intervalSeconds = getInterstitialIntervalSeconds(context)
            val intervalMillis = intervalSeconds * 1000L

            // 2. 마지막 노출 시간 가져오기
            val lastShownTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME_MS, 0L)
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastShownTime

            // 3. 쿨타임 검사
            val canShow = elapsedTime >= intervalMillis

            Log.d(TAG, "전면 광고 노출 가능 여부 확인:")
            Log.d(TAG, "  - 쿨타임 간격: $intervalSeconds 초")
            Log.d(TAG, "  - 마지막 노출: $lastShownTime")
            Log.d(TAG, "  - 경과 시간: ${elapsedTime / 1000} 초")
            Log.d(TAG, "  - 노출 가능: $canShow")

            // 4. 노출 가능하면 마지막 노출 시간 업데이트
            if (canShow) {
                prefs.edit().putLong(KEY_LAST_INTERSTITIAL_TIME_MS, currentTime).apply()
                Log.d(TAG, "마지막 노출 시간 업데이트: $currentTime")
            }

            return canShow
        } catch (t: Throwable) {
            Log.e(TAG, "광고 정책 확인 실패", t)
            return false // 오류 시 안전하게 광고 노출 금지
        }
    }

    /**
     * 디버그 모드 쿨타임 설정 (DEBUG 빌드만)
     *
     * @param context Context
     * @param seconds 쿨타임 간격(초), 0 이하면 기본값 사용
     */
    fun setDebugCoolDownSeconds(context: Context, seconds: Long) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "릴리즈 빌드에서는 쿨타임 설정 불가")
            return
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_DEBUG_AD_COOL_DOWN_SECONDS, seconds).apply()
            Log.d(TAG, "디버그 쿨타임 설정 완료: $seconds 초")
        } catch (t: Throwable) {
            Log.e(TAG, "디버그 쿨타임 설정 실패", t)
        }
    }

    /**
     * 디버그 모드 쿨타임 가져오기 (DEBUG 빌드만)
     *
     * @param context Context
     * @return 설정된 쿨타임(초), 설정되지 않았으면 -1
     */
    fun getDebugCoolDownSeconds(context: Context): Long {
        if (!BuildConfig.DEBUG) {
            return -1L
        }

        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getLong(KEY_DEBUG_AD_COOL_DOWN_SECONDS, -1L)
        } catch (t: Throwable) {
            Log.e(TAG, "디버그 쿨타임 로드 실패", t)
            -1L
        }
    }

    /**
     * 마지막 광고 노출 시간 초기화 (테스트용)
     */
    fun resetLastShownTime(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_LAST_INTERSTITIAL_TIME_MS).apply()
            Log.d(TAG, "마지막 광고 노출 시간 초기화 완료")
        } catch (t: Throwable) {
            Log.e(TAG, "시간 초기화 실패", t)
        }
    }
}

