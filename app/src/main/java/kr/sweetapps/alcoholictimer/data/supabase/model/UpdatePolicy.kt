package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.Serializable

/**
 * Minimal update policy model matching Supabase schema used by the app.
 * [ROBUST] @Serializable로 안전한 직렬화 지원, 모든 필드에 기본값 설정
 */
@Serializable
data class UpdatePolicy(
    val id: Long = 0L,
    val appId: String = "",
    val targetVersionCode: Int = 0,
    val isForceUpdate: Boolean = false,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null
)

