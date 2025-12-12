package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.Serializable

/**
 * Announcement 모델을 Supabase notice_policy 스키마에 맞게 정의한 최소 스텁입니다.
 * [ROBUST] @Serializable로 안전한 직렬화 지원, 모든 필드에 기본값 설정
 */
@Serializable
data class Announcement(
    val id: Long = 0L,
    val createdAt: String? = null,
    val appId: String? = null,
    val isActive: Boolean = false,
    val title: String? = null,
    val content: String = "",
    val noticeVersion: Int = 1
)
