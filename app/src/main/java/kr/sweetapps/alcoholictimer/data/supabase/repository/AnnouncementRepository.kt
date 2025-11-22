package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement
import org.json.JSONArray

/**
 * AnnouncementRepository - Supabase의 notice_policy 테이블에서 공지사항을 조회합니다.
 * 네트워크 호출은 간단한 HttpURLConnection을 사용합니다(의존성 최소화).
 */
class AnnouncementRepository(private val context: Context) {
    // BuildConfig 로부터 Supabase 설정을 읽습니다 (app/build.gradle.kts의 buildConfigField 사용)
    private val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_KEY

    private fun toAnnouncement(obj: org.json.JSONObject): Announcement {
        val id = if (obj.has("id")) obj.getLong("id") else 0L
        val createdAt = if (obj.has("created_at") && !obj.isNull("created_at")) obj.getString("created_at") else null
        val appId = if (obj.has("app_id") && !obj.isNull("app_id")) obj.getString("app_id") else null
        val isActive = if (obj.has("is_active")) obj.getBoolean("is_active") else false
        val title = if (obj.has("title") && !obj.isNull("title")) obj.getString("title") else null
        val content = if (obj.has("content") && !obj.isNull("content")) obj.getString("content") else ""
        val noticeVersion = if (obj.has("notice_version")) obj.getInt("notice_version") else 1
        return Announcement(
            id = id,
            createdAt = createdAt,
            appId = appId,
            isActive = isActive,
            title = title,
            content = content,
            noticeVersion = noticeVersion
        )
    }

    private fun getAppId(obj: org.json.JSONObject): String {
        return if (obj.has("app_id") && !obj.isNull("app_id")) obj.getString("app_id") else ""
    }

    /** 모든 활성 공지사항 조회 (최신순) */
    suspend fun getAnnouncements(): Result<List<Announcement>> = kotlin.runCatching {
        withContext(Dispatchers.IO) {
            val urlStr = "${SUPABASE_URL}/rest/v1/notice_policy?is_active=eq.true&select=*&order=created_at.desc"
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
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val list = mutableListOf<Announcement>()
                for (i in 0 until arr.length()) {
                    list.add(toAnnouncement(arr.getJSONObject(i)))
                }
                return@withContext list
            }

            return@withContext emptyList()
        }
    }

    /** 최신 공지사항 1개 조회 (emergency 제외) */
    suspend fun getLatestAnnouncement(): Result<Announcement?> = kotlin.runCatching {
        withContext(Dispatchers.IO) {
            val pkgName = context.packageName
            val urlStr = "${SUPABASE_URL}/rest/v1/notice_policy?is_active=eq.true&select=*&order=created_at.desc&limit=50"
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
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() == 0) return@withContext null

                val candidates = mutableListOf<org.json.JSONObject>()
                for (i in 0 until arr.length()) candidates.add(arr.getJSONObject(i))

                val pkgBase = pkgName.removeSuffix(".debug").removeSuffix(".debug")
                val simpleName = pkgName.substringAfterLast('.')

                val chosenObj = candidates.firstOrNull { getAppId(it) == pkgName }
                    ?: candidates.firstOrNull { getAppId(it) == pkgBase }
                    ?: candidates.firstOrNull { getAppId(it) == simpleName }
                    ?: candidates.firstOrNull()

                return@withContext chosenObj?.let { toAnnouncement(it) }
            }

            return@withContext null
        }
    }

    /** 특정 ID의 공지사항 조회 */
    suspend fun getAnnouncementById(id: Long): Result<Announcement?> = kotlin.runCatching {
        withContext(Dispatchers.IO) {
            val urlStr = "${SUPABASE_URL}/rest/v1/notice_policy?id=eq.$id&select=*"
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
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() == 0) return@withContext null
                return@withContext toAnnouncement(arr.getJSONObject(0))
            }

            return@withContext null
        }
    }

    /** 특정 개수만큼 공지사항 조회 (최신순) */
    suspend fun getRecentAnnouncements(limit: Int = 5): Result<List<Announcement>> = kotlin.runCatching {
        withContext(Dispatchers.IO) {
            val urlStr = "${SUPABASE_URL}/rest/v1/notice_policy?is_active=eq.true&select=*&order=created_at.desc&limit=$limit"
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
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val list = mutableListOf<Announcement>()
                for (i in 0 until arr.length()) list.add(toAnnouncement(arr.getJSONObject(i)))
                return@withContext list
            }

            return@withContext emptyList()
        }
    }

    /** 이 리포지토리에는 emergency를 다루지 않으므로 null을 반환 */
    fun getActiveEmergency(): Result<Announcement?> = kotlin.runCatching { null }
}
