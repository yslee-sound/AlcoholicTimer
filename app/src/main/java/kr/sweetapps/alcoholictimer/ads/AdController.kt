package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.supabase.SupabaseProvider
import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository

/**
 * 광고 제어 통합 관리자
 *
 * Supabase의 AdPolicy를 기반으로 광고를 제어합니다.
 * - 배너 광고 ON/OFF
 * - 전면 광고 ON/OFF 및 빈도 제한
 * - 앱 오픈 광고 ON/OFF
 *
 * 사용법:
 * ```kotlin
 * // 초기화 (Application.onCreate)
 * AdController.initialize(context)
 *
 * // 광고 표시 전 체크
 * if (AdController.isBannerEnabled()) {
 *     // 배너 광고 표시
 * }
 *
 * if (AdController.canShowInterstitial()) {
 *     InterstitialAdManager.maybeShowIfEligible(activity) {
 *         // 완료 콜백
 *     }
 * }
 * ```
 */
object AdController {
    private const val TAG = "AdController"
    private const val PREFS_NAME = "ad_controller_prefs"
    private const val KEY_INTERSTITIAL_TIMESTAMPS = "interstitial_timestamps"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var repository: AdPolicyRepository? = null

    // Compose State로 변경하여 UI 자동 업데이트
    private val _cachedPolicy = mutableStateOf<AdPolicy?>(null)
    private val cachedPolicy: AdPolicy?
        get() = _cachedPolicy.value

    private var isInitialized = false

    /**
     * AdController 초기화
     * Application.onCreate()에서 호출하세요.
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        try {
            val client = SupabaseProvider.getClient(context)
            // BuildConfig.APPLICATION_ID를 사용하여 Debug/Release 자동 분기
            val appId = kr.sweetapps.alcoholictimer.BuildConfig.APPLICATION_ID
            repository = AdPolicyRepository(client, appId = appId)

            Log.d(TAG, "🔧 Initializing with app_id: $appId")

            // 백그라운드에서 정책 로드
            scope.launch {
                loadPolicy()
            }

            isInitialized = true
            Log.d(TAG, "✅ AdController initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize AdController", e)
        }
    }

    /**
     * 정책을 Supabase에서 로드합니다.
     */
    private suspend fun loadPolicy() {
        try {
            val result = repository?.getPolicy()
            result?.onSuccess { policy ->
                _cachedPolicy.value = policy // State 업데이트
                if (policy != null) {
                    Log.d(TAG, "📋 AdPolicy loaded:")
                    Log.d(TAG, "  - Active: ${policy.isActive}")
                    Log.d(TAG, "  - Banner: ${policy.adBannerEnabled}")
                    Log.d(TAG, "  - Interstitial: ${policy.adInterstitialEnabled}")
                    Log.d(TAG, "  - App Open: ${policy.adAppOpenEnabled}")
                    Log.d(TAG, "  - Max/hour: ${policy.adInterstitialMaxPerHour}")
                    Log.d(TAG, "  - Max/day: ${policy.adInterstitialMaxPerDay}")
                } else {
                    Log.d(TAG, "⚠️ No AdPolicy found, using defaults")
                }
            }?.onFailure { error ->
                Log.e(TAG, "❌ Failed to load AdPolicy", error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while loading AdPolicy", e)
        }
    }

    /**
     * 정책을 강제로 새로고침합니다.
     */
    fun refreshPolicy(context: Context) {
        scope.launch {
            repository?.clearCache()
            loadPolicy()
        }
    }

    // ===== 배너 광고 =====

    /**
     * 배너 광고 활성화 여부
     */
    fun isBannerEnabled(): Boolean {
        val policy = cachedPolicy
        return if (policy != null && policy.isActive) {
            policy.adBannerEnabled
        } else {
            false // 기본값: 비활성화
        }
    }

    // ===== 전면 광고 =====

    /**
     * 전면 광고 활성화 여부
     */
    fun isInterstitialEnabled(): Boolean {
        val policy = cachedPolicy
        return if (policy != null && policy.isActive) {
            policy.adInterstitialEnabled
        } else {
            false // 기본값: 비활성화
        }
    }

    /**
     * 전면 광고를 표시할 수 있는지 확인 (빈도 제한 포함)
     */
    fun canShowInterstitial(context: Context): Boolean {
        if (!isInterstitialEnabled()) {
            Log.d(TAG, "❌ Interstitial disabled by policy")
            return false
        }

        val policy = cachedPolicy
        val maxPerHour = policy?.adInterstitialMaxPerHour ?: 2
        val maxPerDay = policy?.adInterstitialMaxPerDay ?: 10

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestampsJson = prefs.getString(KEY_INTERSTITIAL_TIMESTAMPS, "[]") ?: "[]"

        try {
            val timestamps = parseTimestamps(timestampsJson)
            val now = System.currentTimeMillis()

            // 오래된 타임스탬프 제거 (24시간 이상)
            val validTimestamps = timestamps.filter { now - it < 24 * 60 * 60 * 1000 }

            // 시간당 제한 체크 (최근 1시간)
            val recentHour = validTimestamps.filter { now - it < 60 * 60 * 1000 }
            if (recentHour.size >= maxPerHour) {
                Log.d(TAG, "❌ Interstitial limit reached: ${recentHour.size}/$maxPerHour per hour")
                return false
            }

            // 일일 제한 체크
            if (validTimestamps.size >= maxPerDay) {
                Log.d(TAG, "❌ Interstitial limit reached: ${validTimestamps.size}/$maxPerDay per day")
                return false
            }

            Log.d(TAG, "✅ Can show interstitial: ${validTimestamps.size}/$maxPerDay (day), ${recentHour.size}/$maxPerHour (hour)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking interstitial limit", e)
            return true // 에러 시 허용
        }
    }

    /**
     * 전면 광고 표시를 기록합니다.
     */
    fun recordInterstitialShown(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestampsJson = prefs.getString(KEY_INTERSTITIAL_TIMESTAMPS, "[]") ?: "[]"
            val timestamps = parseTimestamps(timestampsJson).toMutableList()

            timestamps.add(System.currentTimeMillis())

            // 최근 100개만 유지
            val trimmed = timestamps.takeLast(100)

            prefs.edit().putString(KEY_INTERSTITIAL_TIMESTAMPS, formatTimestamps(trimmed)).apply()
            Log.d(TAG, "📝 Interstitial shown recorded (total: ${trimmed.size})")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording interstitial", e)
        }
    }

    // ===== 앱 오픈 광고 =====

    /**
     * 앱 오픈 광고 활성화 여부
     */
    fun isAppOpenEnabled(): Boolean {
        val policy = cachedPolicy
        return if (policy != null && policy.isActive) {
            policy.adAppOpenEnabled
        } else {
            false // 기본값: 비활성화
        }
    }

    // ===== Helper Methods =====

    private fun parseTimestamps(json: String): List<Long> {
        return try {
            val array = org.json.JSONArray(json)
            List(array.length()) { array.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun formatTimestamps(timestamps: List<Long>): String {
        return org.json.JSONArray(timestamps).toString()
    }

    /**
     * 디버그용: 전면 광고 기록 초기화
     */
    fun clearInterstitialHistory(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_INTERSTITIAL_TIMESTAMPS)
            .apply()
        Log.d(TAG, "🔄 Interstitial history cleared")
    }
}
