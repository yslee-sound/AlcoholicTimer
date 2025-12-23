package kr.sweetapps.alcoholictimer.data.supabase.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Announcement 모델
 */
@Keep
@Serializable
data class Announcement(
    val id: Long = 0L,
    val createdAt: String? = null,
    val appId: String? = null,
    val isActive: Boolean = false,
    val title: String? = null,
    val content: String = "",
    val title_ko: String = "",
    val content_ko: String = "",
    val title_en: String = "",
    val content_en: String = "",
    val title_ja: String = "",
    val content_ja: String = "",
    val title_id: String = "",
    val content_id: String = "",
    val noticeVersion: Int = 1
) {
    val displayDate: String
        get() {
            if (createdAt.isNullOrBlank()) return ""
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                val date = inputFormat.parse(createdAt.substring(0, 19))
                date?.let { outputFormat.format(it) } ?: ""
            } catch (e: Exception) {
                createdAt.take(10).replace("-", ".")
            }
        }

    val displayTitle: String
        get() {
            val language = Locale.getDefault().language
            val localizedTitle = when (language) {
                "ko" -> title_ko
                "ja" -> title_ja
                "in", "id" -> title_id
                else -> title_en
            }.ifBlank { title_en }
            return localizedTitle.ifBlank { title ?: "" }
        }

    val displayContent: String
        get() {
            val language = Locale.getDefault().language
            val localizedContent = when (language) {
                "ko" -> content_ko
                "ja" -> content_ja
                "in", "id" -> content_id
                else -> content_en
            }.ifBlank { content_en }
            val rawContent = localizedContent.ifBlank { content }
            // [FIX] "\\n" 텍스트를 실제 줄바꿈 문자로 변환 (2025-12-23)
            return rawContent.replace("\\n", "\n")
        }
}

