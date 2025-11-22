package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.util.Log
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import org.json.JSONArray

/**
 * AdPolicyRepository: fetches ad_policy from Supabase REST endpoint and returns the matching policy
 * for the current package. This replaces the previous stub so remote toggles (ad_app_open_enabled)
 * and limits (app_open_max_per_hour/app_open_max_per_day) are respected.
 */
class AdPolicyRepository(private val appId: String = "alcoholictimer", private val fetcher: Fetcher? = null) {
    private val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_KEY

    @Volatile private var cached: AdPolicy? = null

    // Fetcher 인터페이스: 테스트에서 네트워크 호출을 목킹하기 위해 주입 가능
    interface Fetcher { fun get(url: String): Pair<Int, String?> }

    suspend fun getPolicy(): AdPolicy? = withContext(Dispatchers.IO) {
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
                try { android.util.Log.w("AdPolicyRepository", "fetch failed: $e") } catch (_: Throwable) {}
                Pair(-1, null)
            }

            val (code, body) = codeAndBody
            try { android.util.Log.d("AdPolicyRepository", "Supabase response code=$code bodyLen=${body?.length ?: 0}") } catch (_: Throwable) {}
            val policy = parsePolicyFromJson(body, appId)
            // parsePolicyFromJson now always returns a non-null AdPolicy (DEFAULT_FALLBACK on errors)
            cached = policy
            try { android.util.Log.d("AdPolicyRepository", "Chosen policy app_id=${policy.appId} appOpen=${policy.adAppOpenEnabled} hourMax=${policy.appOpenMaxPerHour} dayMax=${policy.appOpenMaxPerDay}") } catch (_: Throwable) {}
            return@withContext policy
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext AdPolicy.DEFAULT_FALLBACK
    }

    fun clearCache() { cached = null }
    fun getCachedPolicy(): AdPolicy? = cached

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
                        adInterstitialMaxPerHour = adInterstitialMaxPerHour,
                        adInterstitialMaxPerDay = adInterstitialMaxPerDay
                    )
                }
            } catch (e: Exception) {
                try { android.util.Log.w("AdPolicyRepository", "parse failed: $e") } catch (_: Throwable) {}
                return AdPolicy.DEFAULT_FALLBACK
            }
            // fallback: no chosen -> use DEFAULT_FALLBACK
            return AdPolicy.DEFAULT_FALLBACK
          }
      }
  }
