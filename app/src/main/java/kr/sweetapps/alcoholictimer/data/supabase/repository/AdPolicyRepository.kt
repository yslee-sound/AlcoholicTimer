package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import org.json.JSONArray

// Persistent cache keys
private const val PREFS_NAME = "ad_policy_repo_prefs"
private const val KEY_CACHED_JSON = "cached_policy_json"
private const val KEY_CACHED_TS = "cached_policy_ts"
// TTL for persistent cache (default 24 hours)
private const val DEFAULT_CACHE_TTL_MS: Long = 24L * 60L * 60L * 1000L

/**
 * AdPolicyRepository: fetches ad_policy from Supabase REST endpoint and returns the matching policy
 * for the current package. This replaces the previous stub so remote toggles (ad_app_open_enabled)
 * and limits (app_open_max_per_hour/app_open_max_per_day) are respected.
 */
class AdPolicyRepository(
    private val appId: String = "alcoholictimer",
    private val fetcher: Fetcher? = null,
    private val ctx: Context? = null,
    // allow configuring TTL for tests / rapid invalidation
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS
) {
    // Use reflection to read BuildConfig fields so static analysis in this environment
    // doesn't fail when generated BuildConfig is not present.
    private fun getBuildConfigString(name: String, default: String): String {
        return try {
            val cls = Class.forName("kr.sweetapps.alcoholictimer.BuildConfig")
            val field = cls.getField(name)
            (field.get(null) as? String) ?: default
        } catch (_: Throwable) {
            default
        }
    }

    private val SUPABASE_URL: String = getBuildConfigString("SUPABASE_URL", "https://your-project.supabase.co")
    private val SUPABASE_ANON_KEY: String = getBuildConfigString("SUPABASE_KEY", "your-anon-key")

    @Volatile private var cached: AdPolicy? = null

    init {
        // Restore cached policy from SharedPreferences if available and not expired
        try {
            val c = ctx
            if (c != null) {
                val sp = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = sp.getString(KEY_CACHED_JSON, null)
                val ts = try { sp.getLong(KEY_CACHED_TS, 0L) } catch (_: Throwable) { 0L }
                if (!json.isNullOrBlank()) {
                    val now = System.currentTimeMillis()
                    val valid = (ts > 0L) && (now - ts <= cacheTtlMs)
                    if (valid) {
                        cached = parsePolicyFromJson(json, appId)
                        try { Log.d("AdPolicyRepository", "Restored cached policy app_id=${cached?.appId} (age=${now - ts}ms)") } catch (_: Throwable) {}
                    } else {
                        try { Log.d("AdPolicyRepository", "Cached policy expired or missing ts (age=${if (ts>0) System.currentTimeMillis()-ts else "na"}) - ignoring") } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    // Fetcher 인터페이스: 테스트에서 네트워크 호출을 목킹하기 위해 주입 가능
    interface Fetcher { fun get(url: String): Pair<Int, String?> }

    suspend fun getPolicy(): AdPolicy = withContext(Dispatchers.IO) {
        try {
            // If a fetcher is provided (test/mocking), use it
            val urlStr = "${SUPABASE_URL}/rest/v1/ad_policy?is_active=eq.true&select=*"
            val codeAndBody: Pair<Int, String?> = try {
                fetcher?.get(urlStr) ?: run {
                    val url = java.net.URL(urlStr)
                    val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        addRequestProperty("apikey", SUPABASE_ANON_KEY)
                        addRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                        addRequestProperty("Accept", "application/json")
                    }
                    val responseCode = conn.responseCode
                    val text = if (responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
                    Pair(responseCode, text)
                }
            } catch (e: Exception) {
                try { Log.w("AdPolicyRepository", "fetch failed: $e") } catch (_: Throwable) {}
                // On transient network/DNS failure, prefer previously cached policy if available
                return@withContext cached ?: AdPolicy.DEFAULT_FALLBACK
            }

            val (code, body) = codeAndBody
            try { Log.d("AdPolicyRepository", "Supabase response code=$code bodyLen=${body?.length ?: 0}") } catch (_: Throwable) {}

            // If server returned empty/null body, keep using cached policy when available
            if (body.isNullOrBlank()) {
                cached?.let {
                    try { Log.d("AdPolicyRepository", "Empty response from Supabase; using cached policy app_id=${it.appId}") } catch (_: Throwable) {}
                    return@withContext it
                }
            }

            val policy = parsePolicyFromJson(body, appId)
            // parsePolicyFromJson now always returns a non-null AdPolicy (DEFAULT_FALLBACK on errors)
            cached = policy
            // persist cached JSON for future runs
            try {
                val c = ctx
                if (c != null) {
                    val sp = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val now = System.currentTimeMillis()
                    sp.edit { putString(KEY_CACHED_JSON, body); putLong(KEY_CACHED_TS, now) }
                    try { Log.d("AdPolicyRepository", "Persisted policy to prefs app_id=${policy.appId} ts=$now") } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
            try { Log.d("AdPolicyRepository", "Chosen policy app_id=${policy.appId} appOpen=${policy.adAppOpenEnabled} hourMax=${policy.appOpenMaxPerHour} dayMax=${policy.appOpenMaxPerDay}") } catch (_: Throwable) {}
            return@withContext policy
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Prefer cached policy on unexpected errors
        return@withContext cached ?: AdPolicy.DEFAULT_FALLBACK
    }

    fun clearCache() { cached = null }
    fun getCachedPolicy(): AdPolicy? = cached
    fun clearPersistentCache() {
        try { ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.let { sp -> sp.edit { remove(KEY_CACHED_JSON); remove(KEY_CACHED_TS) } } } catch (_: Throwable) {}
    }

    companion object {
        /**
         * JSON 파싱 로직을 분리하여 단위 테스트로 다양한 응답을 검증할 수 있게 함.
         * - empty/invalid JSON -> 안전한 기본(비활성) 정책을 반환
         */
        fun parsePolicyFromJson(jsonText: String?, appId: String): AdPolicy {
            try {
                if (jsonText.isNullOrBlank()) return AdPolicy.DEFAULT_FALLBACK
                val arr = JSONArray(jsonText)
                if (arr.length() == 0) return AdPolicy.DEFAULT_FALLBACK

                val candidates = mutableListOf<org.json.JSONObject>()
                for (i in 0 until arr.length()) candidates.add(arr.getJSONObject(i))

                fun getAppId(obj: org.json.JSONObject): String {
                    return if (obj.has("app_id") && !obj.isNull("app_id")) obj.getString("app_id") else ""
                }

                val pkgBase = appId.removeSuffix(".debug").removeSuffix(".debug")
                val simpleName = appId.substringAfterLast('.')

                val chosen = candidates.firstOrNull { getAppId(it) == appId }
                    ?: candidates.firstOrNull { getAppId(it) == pkgBase }
                    ?: candidates.firstOrNull { getAppId(it) == simpleName }

                if (chosen != null) {
                    val obj = chosen
                    val id = if (obj.has("id")) obj.getLong("id") else 0L
                    val aId = getAppId(obj).ifBlank { appId }
                    val isActive = if (obj.has("is_active")) obj.getBoolean("is_active") else true
                    val adAppOpenEnabled = if (obj.has("ad_app_open_enabled")) obj.getBoolean("ad_app_open_enabled") else true
                    val adInterstitialEnabled = if (obj.has("ad_interstitial_enabled")) obj.getBoolean("ad_interstitial_enabled") else false
                    val adBannerEnabled = if (obj.has("ad_banner_enabled")) obj.getBoolean("ad_banner_enabled") else false
                    val appOpenMaxPerHour = if (obj.has("app_open_max_per_hour")) obj.getInt("app_open_max_per_hour") else 2
                    val appOpenMaxPerDay = if (obj.has("app_open_max_per_day")) obj.getInt("app_open_max_per_day") else 15
                    val appOpenCooldownSeconds = if (obj.has("app_open_cooldown_seconds")) obj.getInt("app_open_cooldown_seconds") else 60
                    val minFullscreenGapSeconds = if (obj.has("min_fullscreen_gap_seconds")) obj.getInt("min_fullscreen_gap_seconds") else 30
                    val adInterstitialMaxPerHour = if (obj.has("ad_interstitial_max_per_hour")) obj.getInt("ad_interstitial_max_per_hour") else 3
                    val adInterstitialMaxPerDay = if (obj.has("ad_interstitial_max_per_day")) obj.getInt("ad_interstitial_max_per_day") else 20

                    return AdPolicy(
                        id = id,
                        appId = aId,
                        isActive = isActive,
                        adAppOpenEnabled = adAppOpenEnabled,
                        adInterstitialEnabled = adInterstitialEnabled,
                        adBannerEnabled = adBannerEnabled,
                        appOpenMaxPerHour = appOpenMaxPerHour,
                        appOpenMaxPerDay = appOpenMaxPerDay,
                        appOpenCooldownSeconds = appOpenCooldownSeconds,
                        minFullscreenGapSeconds = minFullscreenGapSeconds,
                        adInterstitialMaxPerHour = adInterstitialMaxPerHour,
                        adInterstitialMaxPerDay = adInterstitialMaxPerDay
                    )
                }
            } catch (e: Exception) {
                try { Log.w("AdPolicyRepository", "parse failed: $e") } catch (_: Throwable) {}
                return AdPolicy.DEFAULT_FALLBACK
            }
            // fallback: no chosen -> use DEFAULT_FALLBACK
            return AdPolicy.DEFAULT_FALLBACK
        }
    }
}
