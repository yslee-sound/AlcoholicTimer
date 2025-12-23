package kr.sweetapps.alcoholictimer.data.model

import androidx.annotation.Keep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 앱 공지사항/알림 데이터 모델
 * Firestore 컬렉션: "app_notices"
 */
@Keep
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val title_ko: String = "",
    val content_ko: String = "",
    val title_en: String = "",
    val content_en: String = "",
    val title_ja: String = "",
    val content_ja: String = "",
    val title_id: String = "",
    val content_id: String = "",
    val timestamp: Date? = null,
    val isRead: Boolean = false,
    val type: String = "NOTICE"
) {
    val displayDate: String
        get() {
            if (timestamp == null) return ""
            return try {
                val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                sdf.format(timestamp)
            } catch (e: Exception) {
                ""
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
            return localizedTitle.ifBlank { title }
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

