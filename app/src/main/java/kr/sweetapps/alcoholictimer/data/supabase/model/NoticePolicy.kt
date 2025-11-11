package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 일반 공지사항 정책 데이터 모델
 */
@Serializable
data class NoticePolicy(
    @SerialName("id")
    val id: String,

    @SerialName("is_active")
    val isActive: Boolean,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String,

    @SerialName("button_text")
    val buttonText: String,

    @SerialName("action_url")
    val actionUrl: String? = null,

    @SerialName("priority")
    val priority: Int = 0,

    @SerialName("show_once")
    val showOnce: Boolean = false,

    @SerialName("target_version_min")
    val targetVersionMin: String? = null,

    @SerialName("target_version_max")
    val targetVersionMax: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null
)

