// [REFACTOR v2.0] 광고 정책 관리자 - 앱 오프닝과 전면광고 쿨타임 분리
package kr.sweetapps.alcoholictimer.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 광고 정책을 관리하는 싱글톤 객체
 *
 * [v2.0 변경사항] 앱 오프닝과 전면광고 쿨타임 분리
 * - 전면 광고: lastInterstitialTime (전면광고 전용)
 * - 앱 오프닝: lastAppOpenTime (별도 관리, AdController에서 처리)
 * - interstitial_interval_sec: "전면광고 간격"으로만 사용
 *
 * - Firebase Remote Config Kill Switch 지원
 * - 디버그 모드에서 쿨타임 오버라이드 가능
 * - 실제 시간(System.currentTimeMillis) 기반 (타이머 배속과 독립)
 */
object AdPolicyManager {
    private const val TAG = "AdPolicyManager"

    // SharedPreferences 키
    private const val PREFS_NAME = "ad_policy_prefs"

    // [v2.0 변경] 전면광고 전용 쿨타임 변수
    private const val KEY_LAST_INTERSTITIAL_TIME_MS = "last_interstitial_time_ms" // 전면광고 전용

    // [DEPRECATED] 이전 통합 키 (하위 호환용, 마이그레이션 후 제거 예정)
    @Deprecated("Use KEY_LAST_INTERSTITIAL_TIME_MS instead")
    private const val KEY_LAST_AD_SHOWN_TIME_MS = "last_ad_shown_time_ms"

    private const val KEY_DEBUG_AD_COOL_DOWN_SECONDS = "debug_ad_cool_down_seconds"
    private const val KEY_DEBUG_COOLDOWN_ENABLED = "debug_cooldown_enabled"
    private const val KEY_DEBUG_AD_FORCE_DISABLED = "debug_ad_force_disabled"

    // Firebase Remote Config 키
    private const val REMOTE_KEY_INTERSTITIAL_INTERVAL = "interstitial_interval_sec"
    private const val REMOTE_KEY_IS_AD_ENABLED = "is_ad_enabled"

    // 기본 정책 값
    private const val DEFAULT_INTERSTITIAL_INTERVAL_SECONDS = 300L // 5분
    private const val DEBUG_DEFAULT_INTERSTITIAL_INTERVAL_SECONDS = 60L // 디버그: 1분

    // [NEW] UI 반응형 상태 관리 (StateFlow)
    private val _isAdEnabledState = MutableStateFlow(true) // 기본값: 광고 활성화
    val isAdEnabledState: StateFlow<Boolean> = _isAdEnabledState.asStateFlow()

    // Firebase Remote Config 인스턴스
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        Firebase.remoteConfig.apply {
            // [FIX] Debug 빌드에서는 fetch interval을 0으로 설정하여 즉시 업데이트
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(
                    if (BuildConfig.DEBUG) 0L // Debug: 즉시 fetch
                    else 3600L // Release: 1시간 캐시
                )
                .build()
            setConfigSettingsAsync(configSettings)

            // 기본값 설정
            setDefaultsAsync(
                mapOf(
                    REMOTE_KEY_INTERSTITIAL_INTERVAL to DEFAULT_INTERSTITIAL_INTERVAL_SECONDS,
                    REMOTE_KEY_IS_AD_ENABLED to true
                )
            )
        }
    }

    /**
     * (v2.0) 전면광고 쿨타임 간격(초)을 반환
     *
     * ⚠️ 주의: 이 값은 "전면광고(Interstitial Ad) 전용"입니다.
     * 앱 오프닝 광고는 별도의 쿨타임을 사용합니다 (AdController 참조)
     *
     * 우선순위 (상위가 우선):
     * 1. (DEBUG ONLY) 디버그 메뉴에서 설정한 커스텀 쿨타임
     * 2. Firebase Remote Config의 "interstitial_interval_sec" 값
     * 3. 기본값 300초 (5분)
     *
     * @param context Context
     * @return 전면광고 쿨타임 간격(초)
     */
    fun getInterstitialIntervalSeconds(context: Context): Long {
        // [1순위] 디버그 모드 커스텀 설정 (최우선)
        if (BuildConfig.DEBUG) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean(KEY_DEBUG_COOLDOWN_ENABLED, false)

                if (isEnabled) {
                    val debugInterval = prefs.getLong(KEY_DEBUG_AD_COOL_DOWN_SECONDS, -1L)
                    if (debugInterval >= 0) {
                        Log.d(TAG, "✅ [1순위] 디버그 커스텀 쿨타임: $debugInterval 초")
                        return debugInterval
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "디버그 설정 로드 실패", t)
            }
        }

        // [2순위] Firebase Remote Config
        try {
            val remoteInterval = remoteConfig.getLong(REMOTE_KEY_INTERSTITIAL_INTERVAL)
            if (remoteInterval > 0) {
                Log.d(TAG, "✅ [2순위] Firebase Remote Config 쿨타임: $remoteInterval 초")
                return remoteInterval
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Remote Config 로드 실패, 기본값 사용", t)
        }

        // [3순위] 기본값 (5분)
        val defaultInterval = if (BuildConfig.DEBUG) {
            DEBUG_DEFAULT_INTERSTITIAL_INTERVAL_SECONDS // 디버그: 1분
        } else {
            DEFAULT_INTERSTITIAL_INTERVAL_SECONDS // 릴리즈: 5분
        }
        Log.d(TAG, "✅ [3순위] 기본 쿨타임: $defaultInterval 초")
        return defaultInterval
    }

    /**
     * (v1.0) 광고 활성화 여부 확인 (Kill Switch)
     *
     * @param context Context
     * @return true이면 광고 표시 가능, false이면 긴급 차단
     */
    fun isAdEnabled(context: Context): Boolean {
        // [예외] 디버그 모드에서 강제 비활성화 설정 확인
        if (BuildConfig.DEBUG) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val forceDisabled = prefs.getBoolean(KEY_DEBUG_AD_FORCE_DISABLED, false)
                if (forceDisabled) {
                    Log.d(TAG, "⚠️ [디버그] 광고 강제 비활성화됨")
                    _isAdEnabledState.value = false // [NEW] StateFlow 업데이트
                    return false
                }
            } catch (t: Throwable) {
                Log.e(TAG, "디버그 광고 설정 확인 실패", t)
            }
        }

        // Firebase Remote Config Kill Switch 확인
        return try {
            val enabled = remoteConfig.getBoolean(REMOTE_KEY_IS_AD_ENABLED)
            Log.d(TAG, "Firebase Kill Switch: is_ad_enabled = $enabled")
            _isAdEnabledState.value = enabled // [NEW] StateFlow 업데이트 (UI 자동 갱신)
            enabled
        } catch (t: Throwable) {
            Log.w(TAG, "Remote Config Kill Switch 확인 실패, 기본값 true 사용", t)
            _isAdEnabledState.value = true // [NEW] StateFlow 업데이트
            true // 기본적으로 광고 활성화
        }
    }

    /**
     * (v2.0) 전면광고 노출 가능 여부를 결정
     *
     * ⚠️ 중요 변경사항:
     * - 오직 "전면광고(Interstitial Ad)"의 마지막 노출 시간만 체크합니다.
     * - 앱 오프닝 광고와는 독립적으로 작동합니다.
     * - 앱 오프닝을 봐도 이 함수의 결과에 영향 없음
     *
     * @param context Context
     * @return true이면 전면광고 노출 가능, false이면 쿨타임 중 또는 Kill Switch 차단
     */
    fun shouldShowInterstitialAd(context: Context): Boolean {
        // 1. Kill Switch 확인
        if (!isAdEnabled(context)) {
            Log.d(TAG, "❌ Kill Switch에 의해 광고 차단됨")
            return false
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 2. 전면광고 쿨타임 간격 가져오기
            val intervalSeconds = getInterstitialIntervalSeconds(context)
            val intervalMillis = intervalSeconds * 1000L

            // 3. [v2.0 변경] 전면광고 마지막 노출 시간만 가져오기
            val lastInterstitialTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME_MS, 0L)
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastInterstitialTime

            // 4. 쿨타임 검사 (전면광고 전용)
            val canShow = elapsedTime >= intervalMillis

            Log.d(TAG, "========================================")
            Log.d(TAG, "[v2.0 전면광고 전용] 광고 노출 가능 여부 확인:")
            Log.d(TAG, "  - 쿨타임 간격: $intervalSeconds 초 (${intervalSeconds / 60}분)")
            Log.d(TAG, "  - 마지막 전면광고 노출: $lastInterstitialTime")
            Log.d(TAG, "  - 현재 시간: $currentTime")
            Log.d(TAG, "  - 경과 시간: ${elapsedTime / 1000} 초")
            Log.d(TAG, "  - 노출 가능: $canShow")
            if (!canShow) {
                val remainingTime = (intervalMillis - elapsedTime) / 1000
                Log.d(TAG, "  - 남은 시간: $remainingTime 초 (${remainingTime / 60}분)")
            }
            Log.d(TAG, "  ⚠️ 참고: 앱 오프닝 광고와는 독립적으로 작동")
            Log.d(TAG, "========================================")

            return canShow
        } catch (t: Throwable) {
            Log.e(TAG, "광고 정책 확인 실패", t)
            return false // 오류 시 안전하게 광고 노출 금지
        }
    }

    /**
     * (v2.0) 전면광고가 성공적으로 표시된 후 호출
     *
     * ⚠️ 중요: 이 함수는 "전면광고(Interstitial Ad) 전용"입니다.
     * - 전면광고의 onAdDismissedFullScreenContent 콜백에서만 호출하세요.
     * - 앱 오프닝 광고에서는 절대 호출하지 마세요!
     *
     * @param context Context
     * @param adType 광고 타입 (로그용, 기본값: "interstitial")
     */
    fun markInterstitialAdShown(context: Context, adType: String = "interstitial") {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()

            // [v2.0] 전면광고 전용 타이머 업데이트
            prefs.edit().putLong(KEY_LAST_INTERSTITIAL_TIME_MS, currentTime).apply()

            Log.d(TAG, "✅ [v2.0 전면광고 전용] [$adType] 광고 표시 완료 - 쿨타임 시작: $currentTime")
            Log.d(TAG, "  ⚠️ 주의: 앱 오프닝 광고 타이머는 별도 관리됨")
        } catch (t: Throwable) {
            Log.e(TAG, "전면광고 쿨타임 시작 실패", t)
        }
    }

    /**
     * [DEPRECATED v2.0] 이전 통합 방식 (하위 호환용)
     * 새 코드에서는 markInterstitialAdShown() 사용 권장
     */
    @Deprecated(
        message = "Use markInterstitialAdShown() for interstitial ads only. App open ads are managed separately in AdController.",
        replaceWith = ReplaceWith("markInterstitialAdShown(context, adType)")
    )
    fun markAdShown(context: Context, adType: String = "unknown") {
        markInterstitialAdShown(context, adType)
    }

    /**
     * [DEPRECATED v1.0] 이전 버전 호환용
     */
    @Deprecated("Use markInterstitialAdShown() instead", ReplaceWith("markInterstitialAdShown(context)"))
    fun markInterstitialShown(context: Context) {
        markInterstitialAdShown(context, "interstitial")
    }

    /**
     * Remote Config 새로고침 (앱 시작 시 호출 권장)
     * [IMPROVED] Fetch 성공 시 자동으로 StateFlow 업데이트하여 UI가 즉시 반응하도록 개선
     *
     * @param context Context (StateFlow 업데이트용)
     * @param onComplete 완료 콜백 (성공 여부)
     */
    fun fetchRemoteConfig(context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            Log.d(TAG, "🔄 Remote Config Fetch 시작...")
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Log.d(TAG, "✅ Remote Config 업데이트 완료: updated=$updated")

                        // [NEW] Fetch 성공 시 즉시 상태 업데이트 (UI 자동 갱신)
                        if (context != null) {
                            val enabled = remoteConfig.getBoolean(REMOTE_KEY_IS_AD_ENABLED)
                            _isAdEnabledState.value = enabled
                            Log.d(TAG, "🔄 StateFlow 업데이트: isAdEnabled = $enabled")
                        }

                        onComplete?.invoke(true)
                    } else {
                        Log.w(TAG, "❌ Remote Config 업데이트 실패: ${task.exception?.message}")
                        onComplete?.invoke(false)
                    }
                }
        } catch (t: Throwable) {
            Log.e(TAG, "❌ Remote Config fetch 실패", t)
            onComplete?.invoke(false)
        }
    }

    /**
     * (디버그) 쿨타임 커스텀 설정 (DEBUG 빌드만)
     */
    fun setDebugCoolDownSeconds(context: Context, seconds: Long) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "릴리즈 빌드에서는 쿨타임 설정 불가")
            return
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_DEBUG_AD_COOL_DOWN_SECONDS, seconds).apply()
            Log.d(TAG, "✅ 디버그 쿨타임 설정: $seconds 초")
        } catch (t: Throwable) {
            Log.e(TAG, "디버그 쿨타임 설정 실패", t)
        }
    }

    /**
     * (디버그) 쿨타임 가져오기 (DEBUG 빌드만)
     */
    fun getDebugCoolDownSeconds(context: Context): Long {
        if (!BuildConfig.DEBUG) return -1L
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getLong(KEY_DEBUG_AD_COOL_DOWN_SECONDS, -1L)
        } catch (t: Throwable) {
            -1L
        }
    }

    /**
     * (디버그) 광고 강제 비활성화 설정 (DEBUG 빌드만)
     */
    fun setDebugAdForceDisabled(context: Context, disabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_DEBUG_AD_FORCE_DISABLED, disabled).apply()
            Log.d(TAG, "✅ 디버그 광고 강제 비활성화: $disabled")
        } catch (t: Throwable) {
            Log.e(TAG, "디버그 광고 설정 실패", t)
        }
    }

    /**
     * (디버그) 광고 강제 비활성화 여부 확인
     */
    fun isDebugAdForceDisabled(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_DEBUG_AD_FORCE_DISABLED, false)
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * 디버그용: 마지막 광고 노출 시간 초기화
     * (v2.0) 전면광고 타이머 초기화
     */
    fun resetLastShownTime(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // (v2.0) 전면광고 타이머 초기화
            prefs.edit().remove(KEY_LAST_INTERSTITIAL_TIME_MS).apply()
            Log.d(TAG, "✅ 전면광고 쿨타임 초기화 완료")
        } catch (t: Throwable) {
            Log.e(TAG, "시간 초기화 실패", t)
        }
    }
}

