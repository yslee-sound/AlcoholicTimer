package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy
import org.json.JSONArray

/**
 * Implementation of UpdatePolicyRepository that fetches the active policy from Supabase REST
 * endpoint. The Supabase project URL and anon key are configurable here. This is a small,
 * dependency-free HTTP implementation using java.net.HttpURLConnection and org.json.
 */
class UpdatePolicyRepository(private val context: Context) {
    // TODO: move these values to secure config (BuildConfig or resources). Using constants
    // here because this repo previously used a stub.
    private val SUPABASE_URL = "https://bajurdtglfaiqilnpamt.supabase.co"
    private val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJhanVyZHRnbGZhaXFpbG5wYW10Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE5ODI2NzksImV4cCI6MjA3NzU1ODY3OX0.lqFbkf974wf-uYrY0VFuD7MwCiDF5hKTx-bIbVujfH4"

    /**
     * Suspend function that attempts to fetch the currently active update policy from Supabase.
     * Returns UpdatePolicy or null if none available or on error. This executes the network call
     * on the IO dispatcher.
     */
    suspend fun getActivePolicy(): UpdatePolicy? = withContext(Dispatchers.IO) {
        try {
            val pkgName = context.packageName
            // Build REST query: fetch all active policies and match locally because app_id values
            // in Supabase may not match the Android package exactly (debug variants, simple ids, etc).
            val urlStr = "${SUPABASE_URL}/rest/v1/update_policy?is_active=eq.true&select=*"
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
            android.util.Log.d("UpdatePolicyRepo", "Supabase response code=$code")
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                // Supabase returns a JSON array
                val arr = JSONArray(text)
                if (arr.length() > 0) {
                    // Try to find a matching policy by app_id heuristics
                    val candidates = mutableListOf<org.json.JSONObject>()
                    for (i in 0 until arr.length()) {
                        candidates.add(arr.getJSONObject(i))
                    }

                    // helper to extract string safely
                    fun getAppId(obj: org.json.JSONObject): String {
                        return if (obj.has("app_id") && !obj.isNull("app_id")) obj.getString("app_id") else ""
                    }

                    val pkgBase = pkgName.removeSuffix(".debug").removeSuffix(".debug")
                    val simpleName = pkgName.substringAfterLast('.')

                    // prefer exact matches
                    var chosen: org.json.JSONObject? = null
                    chosen = candidates.firstOrNull { getAppId(it) == pkgName }
                        ?: candidates.firstOrNull { getAppId(it) == pkgBase }
                        ?: candidates.firstOrNull { getAppId(it) == simpleName }
                        ?: candidates.firstOrNull() // fallback to first active

                    if (chosen != null) {
                        val obj = chosen
                        android.util.Log.d("UpdatePolicyRepo", "Chosen policy app_id=${getAppId(obj)} target=${if (obj.has("target_version_code")) obj.getInt("target_version_code") else "?"}")
                        val id = if (obj.has("id")) obj.getLong("id") else 0L
                        val appId = getAppId(obj).ifBlank { pkgName }
                        val targetVersionCode = if (obj.has("target_version_code")) obj.getInt("target_version_code") else 0
                        val isForce = if (obj.has("is_force_update")) obj.getBoolean("is_force_update") else false
                        val releaseNotes = if (obj.has("release_notes") && !obj.isNull("release_notes")) obj.getString("release_notes") else null
                        val downloadUrl = if (obj.has("download_url") && !obj.isNull("download_url")) obj.getString("download_url") else null

                        val policy = UpdatePolicy(
                            id = id,
                            appId = appId,
                            targetVersionCode = targetVersionCode,
                            isForceUpdate = isForce,
                            releaseNotes = releaseNotes,
                            downloadUrl = downloadUrl
                        )
                        android.util.Log.d("UpdatePolicyRepo", "Returning network policy id=${policy.id} releaseNotes=${policy.releaseNotes?.take(80)}")
                        return@withContext policy
                    }
                }
            }

            // if we reach here, no policy found or non-2xx
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Development fallback: when running a debug build return a sample policy so UI can be exercised
        try {
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    (pkgInfo.longVersionCode).toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode
                }
                return@withContext UpdatePolicy(
                    id = 1L,
                    appId = context.packageName,
                    targetVersionCode = currentVersionCode + 1,
                    isForceUpdate = false,
                    releaseNotes = "- 버그 수정\n- 안정성 향상",
                    downloadUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }
}
