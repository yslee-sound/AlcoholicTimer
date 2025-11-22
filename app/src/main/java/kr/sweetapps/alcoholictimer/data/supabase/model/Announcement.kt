package kr.sweetapps.alcoholictimer.data.supabase.model

/** Announcement 모델을 Supabase notice_policy 스키마에 맞게 정의한 최소 스텁입니다. */
data class Announcement(
    val id: Long = 0L,
    val createdAt: String? = null,
    val appId: String? = null,
    val isActive: Boolean = false,
    val title: String? = null,
    val content: String = "",
    val noticeVersion: Int = 1
)
