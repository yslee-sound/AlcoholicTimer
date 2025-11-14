package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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

    // 전면광고 표시 중 배너 일시 숨김 상태
    private val _isInterstitialShowing = mutableStateOf(false)

    // 전체(full-screen) 광고(앱오픈/전면)를 나타내는 단일 소스 오브 트루스
    private val _isFullScreenAdShowing = mutableStateOf(false)

    // 광고 로드 상태 및 실패 이유 통합 관리
    private val _appOpenLoading = mutableStateOf(false)
    private val _appOpenLoaded = mutableStateOf(false)
    private val _appOpenLastError = mutableStateOf<String?>(null)

    private val _interstitialLoading = mutableStateOf(false)
    private val _interstitialLoaded = mutableStateOf(false)
    private val _interstitialLastError = mutableStateOf<String?>(null)

    /**
     * 전면광고 표시 상태 읽기 (Composable에서 사용)
     */
    @Composable
    fun isInterstitialShowingState(): Boolean {
        return _isInterstitialShowing.value
    }

    /**
     * 전체(full-screen) 광고가 표시 중인지 여부 (Composable에서 사용)
     */
    @Composable
    fun isFullScreenAdShowingState(): Boolean {
        return _isFullScreenAdShowing.value
    }

    /**
     * 전체(full-screen) 광고가 표시 중인지 여부 (비-Composable)
     */
    fun isFullScreenAdShowing(): Boolean {
        return _isFullScreenAdShowing.value
    }

    /**
     * 전체(full-screen) 광고 표시 상태 설정(앱 내부에서만 사용)
     */
    fun setFullScreenAdShowing(showing: Boolean) {
        _isFullScreenAdShowing.value = showing
        Log.d(TAG, "Full-screen ad showing state set: $showing")
    }

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

                    // 정책이 비활성화된 경우 이미 로드된 광고를 제거
                    if (!policy.isActive) {
                        try {
                            kr.sweetapps.alcoholictimer.ads.InterstitialAdManager.clearLoadedAd()
                        } catch (_: Throwable) { Log.w(TAG, "Failed to clear interstitial on policy change") }
                        try {
                            kr.sweetapps.alcoholictimer.ads.AppOpenAdManager.clearLoadedAd()
                        } catch (_: Throwable) { Log.w(TAG, "Failed to clear app-open on policy change") }
                    }
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
     * 배너 광고 활성화 여부 (Composable에서 사용)
     */
    @Composable
    fun isBannerEnabledState(): Boolean {
        if (_isInterstitialShowing.value) {
            return false
        }
        val policy = _cachedPolicy.value  // State 직접 읽기로 구독
        return when {
            policy == null -> true // 문서 기준: 정책 미존재 -> 기본 허용
            !policy.isActive -> false // 정책이 존재하고 비활성화 되어 있으면 전체 차단
            else -> policy.adBannerEnabled
        }
    }

    /**
     * 배너 광고 활성화 여부 (비-Composable에서 사용)
     */
    fun isBannerEnabled(): Boolean {
        if (_isInterstitialShowing.value) {
            Log.d(TAG, "Banner disabled: interstitial is showing")
            return false
        }
        val policy = cachedPolicy
        return when {
            policy == null -> true // 문서 기준: 정책 미존재 -> 기본 허용
            !policy.isActive -> false // 정책이 존재하고 비활성화 되어 있으면 전체 차단
            else -> policy.adBannerEnabled
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
        return when {
            policy == null -> true // 문서 기준: 정책 미존재 -> 기본 허용
            !policy.isActive -> false // 정책이 존재하고 비활성화 되어 있으면 전체 차단
            else -> policy.adAppOpenEnabled
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

    /**
     * 전면광고 표시 시작 시 호출 (배너 숨김)
     */
    internal fun setInterstitialShowing(showing: Boolean) {
        _isInterstitialShowing.value = showing
        Log.d(TAG, "Interstitial showing state: $showing")
    }

    // App Open getters/setters
    @Composable
    fun isAppOpenLoadingState(): Boolean = _appOpenLoading.value

    fun isAppOpenLoading(): Boolean = _appOpenLoading.value

    fun setAppOpenLoading(loading: Boolean) {
        _appOpenLoading.value = loading
        if (loading) {
            _appOpenLastError.value = null
            _appOpenLoaded.value = false
        }
        Log.d(TAG, "AppOpen loading state: $loading")
    }

    fun setAppOpenLoaded(loaded: Boolean) {
        _appOpenLoaded.value = loaded
        if (loaded) _appOpenLoading.value = false
        Log.d(TAG, "AppOpen loaded state: $loaded")
    }

    fun setAppOpenLastError(message: String?) {
        _appOpenLastError.value = message
        if (message != null) {
            _appOpenLoaded.value = false
            _appOpenLoading.value = false
        }
        Log.d(TAG, "AppOpen last error: $message")
    }

    fun getAppOpenLastError(): String? = _appOpenLastError.value

    // Interstitial getters/setters
    @Composable
    fun isInterstitialLoadingState(): Boolean = _interstitialLoading.value

    fun isInterstitialLoading(): Boolean = _interstitialLoading.value

    fun setInterstitialLoading(loading: Boolean) {
        _interstitialLoading.value = loading
        if (loading) {
            _interstitialLastError.value = null
            _interstitialLoaded.value = false
        }
        Log.d(TAG, "Interstitial loading state: $loading")
    }

    fun setInterstitialLoaded(loaded: Boolean) {
        _interstitialLoaded.value = loaded
        if (loaded) _interstitialLoading.value = false
        Log.d(TAG, "Interstitial loaded state: $loaded")
    }

    fun setInterstitialLastError(message: String?) {
        _interstitialLastError.value = message
        if (message != null) {
            _interstitialLoaded.value = false
            _interstitialLoading.value = false
        }
        Log.d(TAG, "Interstitial last error: $message")
    }

    fun getInterstitialLastError(): String? = _interstitialLastError.value
}
