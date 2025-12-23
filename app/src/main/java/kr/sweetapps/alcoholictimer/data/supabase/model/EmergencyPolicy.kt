package kr.sweetapps.alcoholictimer.data.supabase.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * EmergencyPolicy model aligned with Supabase schema (docs/release-test/supabase_schema.md).
 * [ROBUST] @Serializable로 안전한 직렬화 지원, 모든 필드에 기본값 설정
 * [FIX] @Keep 추가 - 릴리즈 빌드 난독화 방지 (2025-12-23)
 */
@Keep
@Serializable
data class EmergencyPolicy(
    val id: Long = 0L,
    val createdAt: String? = null,
    val appId: String? = null,
    val isActive: Boolean = false,
    val content: String = "",
    val redirectUrl: String? = null,
    val buttonText: String? = null,
    val isDismissible: Boolean = false
)
