package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * Repository that fetches active emergency policy from Supabase REST endpoint.
 */
class EmergencyPolicyRepository(private val context: Context) {
    private val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_KEY

    suspend fun getActivePolicy(): EmergencyPolicy? = withContext(Dispatchers.IO) {
        // [DISABLED] Supabase 팝업 기능 비활성화 - Firebase로 이전 예정
        // 앱 크래시 방지를 위해 항상 null 반환 (표시할 팝업 없음)
        android.util.Log.d("EmergencyRepo", "Emergency popup disabled - returning null")
        return@withContext null

        /* [원본 코드 주석 처리 - 재사용 대비]
        try {
            val pkgName = context.packageName
            val urlStr = "${SUPABASE_URL}/rest/v1/emergency_policy?is_active=eq.true&select=*"
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
            android.util.Log.d("EmergencyRepo", "Supabase response code=$code")
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() > 0) {
                    val candidates = mutableListOf<org.json.JSONObject>()
                    for (i in 0 until arr.length()) candidates.add(arr.getJSONObject(i))

                    fun getAppId(obj: org.json.JSONObject): String {
                        return if (obj.has("app_id") && !obj.isNull("app_id")) obj.getString("app_id") else ""
                    }

                    val pkgBase = pkgName.removeSuffix(".debug").removeSuffix(".debug")
                    val simpleName = pkgName.substringAfterLast('.')

                    var chosen: org.json.JSONObject? = null
                    chosen = candidates.firstOrNull { getAppId(it) == pkgName }
                        ?: candidates.firstOrNull { getAppId(it) == pkgBase }
                        ?: candidates.firstOrNull { getAppId(it) == simpleName }

                    if (chosen == null) {
                        android.util.Log.d("EmergencyRepo", "No matching emergency_policy found for pkg=$pkgName; candidates=${candidates.map { getAppId(it) }}")
                    }

                    if (chosen != null) {
                        val obj = chosen
                        val id = if (obj.has("id")) obj.getLong("id") else 0L
                        val createdAt = if (obj.has("created_at") && !obj.isNull("created_at")) obj.getString("created_at") else null
                        val appId = getAppId(obj).ifBlank { pkgName }
                        val isActive = if (obj.has("is_active")) obj.getBoolean("is_active") else false
                        val content = if (obj.has("content") && !obj.isNull("content")) obj.getString("content") else ""
                        val redirectUrl = if (obj.has("redirect_url") && !obj.isNull("redirect_url")) obj.getString("redirect_url") else null
                        val buttonText = if (obj.has("button_text") && !obj.isNull("button_text")) obj.getString("button_text") else null
                        val isDismissible = if (obj.has("is_dismissible")) obj.getBoolean("is_dismissible") else false

                        return@withContext EmergencyPolicy(
                            id = id,
                            createdAt = createdAt,
                            appId = appId,
                            isActive = isActive,
                            content = content,
                            redirectUrl = redirectUrl,
                            buttonText = buttonText,
                            isDismissible = isDismissible
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
        */
    }
}
