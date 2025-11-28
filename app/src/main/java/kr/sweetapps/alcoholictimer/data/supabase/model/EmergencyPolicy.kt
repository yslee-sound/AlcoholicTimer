package kr.sweetapps.alcoholictimer.data.supabase.model

/**
 * EmergencyPolicy model aligned with Supabase schema (docs/release-test/supabase_schema.md).
 */
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
