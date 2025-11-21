package kr.sweetapps.alcoholictimer.ads

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AdPolicyChecker {
    private const val TAG = "AdPolicyChecker"

    data class AdPolicy(
        val enabled: Boolean = true,
        val maxPerHour: Int = Int.MAX_VALUE,
        val maxPerDay: Int = Int.MAX_VALUE
    )

    /**
     * Fetch policy from configured URL (user_settings supabase_policy_url). If not configured or fetch fails,
     * returns defaults which permit interstitials.
     */
    suspend fun fetchPolicy(context: Context): AdPolicy {
        return withContext(Dispatchers.IO) {
            try {
                val sp = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
                val urlString = sp.getString("supabase_policy_url", null)

                if (urlString.isNullOrBlank()) {
                    Log.d(TAG, "No supabase_policy_url configured — returning default policy (allowed)")
                    return@withContext AdPolicy()
                }

                Log.d(TAG, "Fetching ad policy from $urlString")
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                }

                return@withContext try {
                    val code = conn.responseCode
                    if (code != 200) {
                        Log.w(TAG, "Policy fetch returned HTTP $code — returning default policy")
                        AdPolicy()
                    } else {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val enabled = json.optBoolean("ad_interstitial_enabled", true)
                        val maxHour = json.optInt("ad_interstitial_max_per_hour", Int.MAX_VALUE)
                        val maxDay = json.optInt("ad_interstitial_max_per_day", Int.MAX_VALUE)

                        Log.d(TAG, "Policy parsed: enabled=$enabled, maxHour=$maxHour, maxDay=$maxDay")
                        AdPolicy(enabled = enabled, maxPerHour = maxHour, maxPerDay = maxDay)
                    }
                } finally {
                    try { conn.disconnect() } catch (_: Throwable) {}
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Policy fetch failed — returning default policy", t)
                AdPolicy()
            }
        }
    }

    // Backwards-compatible helper
    suspend fun isInterstitialAllowed(context: Context): Boolean = fetchPolicy(context).enabled
}
