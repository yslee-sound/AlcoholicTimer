package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import org.json.JSONArray

/**
 * AdPolicyRepository: fetches ad_policy from Supabase REST endpoint and returns the matching policy
 * for the current package. This replaces the previous stub so remote toggles (ad_app_open_enabled)
 * and limits (app_open_max_per_hour/app_open_max_per_day) are respected.
 */
class AdPolicyRepository(private val appId: String = "alcoholictimer") {
    private val SUPABASE_URL = "https://bajurdtglfaiqilnpamt.supabase.co"
    private val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJhanVyZHRnbGZhaXFpbG5wYW10Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE5ODI2NzksImV4cCI6MjA3NzU1ODY3OX0.lqFbkf974wf-uYrY0VFuD7MwCiDF5hKTx-bIbVujfH4"

    @Volatile private var cached: AdPolicy? = null

    suspend fun getPolicy(): AdPolicy? = withContext(Dispatchers.IO) {
        try {
            // Fetch active policies
            val urlStr = "${SUPABASE_URL}/rest/v1/ad_policy?is_active=eq.true&select=*"
            val url = java.net.URL(urlStr)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                addRequestProperty("apikey", SUPABASE_ANON_KEY)
                addRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                addRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            Log.d("AdPolicyRepository", "Supabase response code=$code")
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() > 0) {
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

                        val policy = AdPolicy(
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

                        cached = policy
                        Log.d("AdPolicyRepository", "Chosen policy app_id=${policy.appId} appOpen=${policy.adAppOpenEnabled} hourMax=${policy.appOpenMaxPerHour} dayMax=${policy.appOpenMaxPerDay}")
                        return@withContext policy
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun clearCache() { cached = null }
    fun getCachedPolicy(): AdPolicy? = cached
}
